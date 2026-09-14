#!/usr/bin/env python3
"""Structure check and S1 receipt for the lifecycle/admission slice.

Standard library only. Development tooling; not shipped in the plugin.

  check                   strict parse + reference/coverage rules on spec.json          (EV-STRUCTURE)
  receipt [--out PATH]    check, javac --release 21, javap signatures, hashed receipt   (EV-DESIGN-COMPILE,
                                                                                         EV-DESIGN-SOURCE, EV-RECEIPT)
  verify --receipt PATH   recompute the receipt and reject any changed identity field   (EV-RECEIPT)
  selftest                prove check/verify reject a removed audit row and altered hashes (AC-012)

Exit codes: 0 success, 1 findings or mismatch, 2 tool/environment failure.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import re
import shutil
import subprocess
import sys
import tempfile
from datetime import datetime, timezone
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
SPEC = "native-agent-docs/lifecycle-admission/spec.json"
DESIGN = "native-agent-docs/lifecycle-admission/design.md"
SOURCE_DIR = "plugin-core/src/main/java/com/github/catatafishen/agentbridge/nativeagent/lifecycle"
TEST_DIR = "plugin-core/src/test/java/com/github/catatafishen/agentbridge/nativeagent/lifecycle"
CONFIG_FILES = [
    "settings.gradle.kts",
    "build.gradle.kts",
    "plugin-core/build.gradle.kts",
    "gradle.properties",
    "gradle/wrapper/gradle-wrapper.properties",
]
DEFAULT_RECEIPT = ".agent-work/native-agent-docs/lifecycle-admission/s1-receipt.json"
STAGE = "S1"
RECEIPT_IDENTITY = ["stage", "feature", "spec_hash", "design_hash", "declaration_hashes",
                    "configuration_identity", "evidence_hashes", "status"]
REQUIRED_TOP = {
    "format_version": int, "feature": str, "scope": str, "decisions": dict, "requirements": list,
    "acceptance": list, "type_safety_audit": list, "audit_dimensions": list, "non_goals": list,
    "assumptions": list, "evidence": list, "stages": list, "open_decisions": list,
    "operation_matrix": list, "receipt_identity": dict, "null_boundary_matrix": list,
}


class ToolError(Exception):
    pass


# ---------------------------------------------------------------- strict parsing

def _reject_duplicates(pairs):
    seen = {}
    for key, value in pairs:
        if key in seen:
            raise ValueError(f"duplicate object key {key!r}")
        seen[key] = value
    return seen


def _reject_constant(name):
    raise ValueError(f"non-finite number {name}")


def load_strict(path: Path):
    text = path.read_text(encoding="utf-8")
    return json.loads(text, object_pairs_hook=_reject_duplicates, parse_constant=_reject_constant)


# ---------------------------------------------------------------- structure rules

def check_spec(root: Path) -> list[dict]:
    findings: list[dict] = []

    def fail(code, pointer, message):
        findings.append({"rule": code, "pointer": pointer, "message": message})

    try:
        spec = load_strict(root / SPEC)
    except (OSError, ValueError) as error:
        fail("S001", "/", f"strict parse failed: {error}")
        return findings

    for key, kind in REQUIRED_TOP.items():
        if not isinstance(spec.get(key), kind):
            fail("S003", f"/{key}", f"missing or not {kind.__name__}")
    unknown = sorted(set(spec) - set(REQUIRED_TOP))
    for key in unknown:
        fail("S003", f"/{key}", "unknown top-level field")
    if findings:
        return findings
    if spec["format_version"] != 1:
        fail("S003", "/format_version", "unsupported format version")

    def ids(section):
        seen = set()
        for index, item in enumerate(spec[section]):
            item_id = item.get("id") if isinstance(item, dict) else None
            if not isinstance(item_id, str) or not item_id:
                fail("S003", f"/{section}/{index}/id", "missing id")
            elif item_id in seen:
                fail("S004", f"/{section}/{index}/id", f"duplicate id {item_id}")
            else:
                seen.add(item_id)
        return seen

    requirement_ids = ids("requirements")
    acceptance_ids = ids("acceptance")
    evidence_ids = ids("evidence")
    stage_ids = ids("stages")
    for overlap in sorted((requirement_ids & acceptance_ids) | (requirement_ids & evidence_ids)
                          | (acceptance_ids & evidence_ids)):
        fail("S004", "/", f"id {overlap} used in more than one section")

    referenced_acceptance = set()
    for index, requirement in enumerate(spec["requirements"]):
        if not str(requirement.get("statement", "")).strip():
            fail("S003", f"/requirements/{index}/statement", "empty statement")
        refs = requirement.get("acceptance")
        if not isinstance(refs, list) or not refs:
            fail("R001", f"/requirements/{index}/acceptance", "requirement has no acceptance")
            continue
        for ref in refs:
            referenced_acceptance.add(ref)
            if ref not in acceptance_ids:
                fail("R001", f"/requirements/{index}/acceptance", f"unresolved acceptance {ref}")
    for missing in sorted(acceptance_ids - referenced_acceptance):
        fail("R002", "/acceptance", f"acceptance {missing} is not referenced by any requirement")

    test_methods = set()
    for test_file in sorted((root / TEST_DIR).glob("*.java")):
        for method in re.findall(r"void\s+(\w+)\s*\(", test_file.read_text(encoding="utf-8")):
            test_methods.add(f"{test_file.stem}#{method}")
    for index, acceptance in enumerate(spec["acceptance"]):
        for field in ("when", "then", "target"):
            if not str(acceptance.get(field, "")).strip():
                fail("S003", f"/acceptance/{index}/{field}", "empty field")
        target = str(acceptance.get("target", ""))
        script = target.split()[0] if target else ""
        if "#" in target:
            if target not in test_methods:
                fail("T002", f"/acceptance/{index}/target", f"test method {target} not found")
        elif not (root / script).is_file():
            fail("T002", f"/acceptance/{index}/target", f"target {script!r} does not exist")

    audit_fields = ("invalid", "type_api_prevention", "residual_runtime_obligation", "justification")
    audit_counts: dict[str, int] = {}
    for index, row in enumerate(spec["type_safety_audit"]):
        requirement = row.get("requirement")
        audit_counts[requirement] = audit_counts.get(requirement, 0) + 1
        if requirement not in requirement_ids:
            fail("A001", f"/type_safety_audit/{index}/requirement", f"unresolved requirement {requirement}")
        for field in audit_fields:
            if not str(row.get(field, "")).strip():
                fail("A002", f"/type_safety_audit/{index}/{field}", "empty audit field")
    for requirement in sorted(requirement_ids):
        if audit_counts.get(requirement, 0) != 1:
            fail("A001", "/type_safety_audit",
                 f"requirement {requirement} has {audit_counts.get(requirement, 0)} audit rows, expected 1")
    if not spec["audit_dimensions"]:
        fail("A003", "/audit_dimensions", "no audit dimensions")

    exited = set()
    for index, stage in enumerate(spec["stages"]):
        for dependency in stage.get("depends_on", []):
            if dependency not in stage_ids:
                fail("G002", f"/stages/{index}/depends_on", f"unresolved stage {dependency}")
        for gate in ("entry", "exit"):
            for binding_index, binding in enumerate(stage.get(gate, [])):
                pointer = f"/stages/{index}/{gate}/{binding_index}"
                for ref in binding.get("requirements", []):
                    if ref not in requirement_ids:
                        fail("G001", pointer, f"unresolved requirement {ref}")
                    elif gate == "exit":
                        exited.add(ref)
                for ref in binding.get("evidence", []):
                    if ref not in evidence_ids:
                        fail("G001", pointer, f"unresolved evidence {ref}")
    for requirement in sorted(requirement_ids - exited):
        fail("G003", "/stages", f"requirement {requirement} is in no stage exit")

    matrix_keys = set()
    for index, row in enumerate(spec["operation_matrix"]):
        for field in ("operation", "phase", "result", "next_phase"):
            if not str(row.get(field, "")).strip():
                fail("M001", f"/operation_matrix/{index}/{field}", "empty matrix field")
        key = (row.get("operation"), row.get("phase"), row.get("precondition"))
        if key in matrix_keys:
            fail("M002", f"/operation_matrix/{index}", f"duplicate matrix case {key}")
        matrix_keys.add(key)

    for index, row in enumerate(spec["null_boundary_matrix"]):
        for field in ("operation", "null_input", "expected"):
            if not str(row.get(field, "")).strip():
                fail("N001", f"/null_boundary_matrix/{index}/{field}", "empty null-boundary field")

    for field in RECEIPT_IDENTITY:
        if field not in spec["receipt_identity"].get("fields", []):
            fail("S003", "/receipt_identity/fields", f"missing receipt field {field}")

    if spec["open_decisions"]:
        fail("D001", "/open_decisions", "open decisions must be empty")

    def scan(value, pointer):
        if isinstance(value, dict):
            for key, child in value.items():
                scan(child, f"{pointer}/{key}")
        elif isinstance(value, list):
            for index, child in enumerate(value):
                scan(child, f"{pointer}/{index}")
        elif isinstance(value, str) and re.search(r"\b(TODO|TBD)\b", value):
            fail("T001", pointer, "unresolved TODO/TBD marker")
        elif isinstance(value, float) and not math.isfinite(value):
            fail("S002", pointer, "non-finite number")

    scan(spec, "")
    findings.sort(key=lambda finding: (finding["rule"], finding["pointer"], finding["message"]))
    return findings


# ---------------------------------------------------------------- receipt

def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha256_file(path: Path) -> str:
    return sha256_bytes(path.read_bytes())


def run_tool(command: list[str], cwd: Path | None = None) -> subprocess.CompletedProcess:
    if shutil.which(command[0]) is None:
        raise ToolError(f"{command[0]} not found on PATH")
    return subprocess.run(command, cwd=cwd, capture_output=True, text=True)


def build_receipt(root: Path) -> dict:
    findings = check_spec(root)
    structure = {"command": "check_slice.py check", "findings": findings, "status": "PASS" if not findings else "FAIL"}

    sources = sorted((root / SOURCE_DIR).glob("*.java"))
    if not sources:
        raise ToolError(f"no sources under {SOURCE_DIR}")
    javac_version = run_tool(["javac", "-version"])
    with tempfile.TemporaryDirectory() as classes:
        compile_command = ["javac", "--release", "21", "-d", classes] + [str(path) for path in sources]
        compiled = run_tool(compile_command)
        compile_result = {
            "command": "javac --release 21 -d <tmp> " + " ".join(str(p.relative_to(root)) for p in sources),
            "exit_code": compiled.returncode,
            "output": (compiled.stdout + compiled.stderr).strip(),
            "status": "PASS" if compiled.returncode == 0 else "FAIL",
        }
        signatures = ""
        if compiled.returncode == 0:
            class_names = sorted(
                str(path.relative_to(classes))[:-len(".class")].replace("/", ".")
                for path in Path(classes).rglob("*.class"))
            javap = run_tool(["javap", "-package", "-cp", classes] + class_names)
            if javap.returncode != 0:
                raise ToolError(f"javap failed: {javap.stderr.strip()}")
            signatures = javap.stdout
    signature_result = {
        "command": "javap -package <all compiled lifecycle classes>",
        "signatures": signatures,
        "status": "PASS" if signatures else "FAIL",
    }

    evidence = {"EV-STRUCTURE": structure, "EV-DESIGN-COMPILE": compile_result, "EV-DESIGN-SOURCE": signature_result}
    receipt = {
        "stage": STAGE,
        "feature": load_strict(root / SPEC)["feature"],
        "spec_hash": sha256_file(root / SPEC),
        "design_hash": sha256_file(root / DESIGN),
        "declaration_hashes": {str(path.relative_to(root)): sha256_file(path) for path in sources},
        "configuration_identity": {
            "files": {name: sha256_file(root / name) for name in CONFIG_FILES if (root / name).is_file()},
            "javac": (javac_version.stdout + javac_version.stderr).strip(),
            "release": "21",
        },
        "evidence_hashes": {key: sha256_bytes(canonical(value)) for key, value in evidence.items()},
        "status": "PASS" if all(value["status"] == "PASS" for value in evidence.values()) else "FAIL",
        "evidence": evidence,
    }
    receipt["digest"] = sha256_bytes(canonical({field: receipt[field] for field in RECEIPT_IDENTITY}))
    receipt["timestamp"] = datetime.now(timezone.utc).isoformat(timespec="seconds")
    return receipt


def canonical(value) -> bytes:
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode("utf-8")


def verify_receipt(root: Path, receipt_path: Path) -> list[dict]:
    stored = load_strict(receipt_path)
    mismatches = []
    missing = [field for field in RECEIPT_IDENTITY + ["digest"] if field not in stored]
    if missing:
        return [{"field": field, "message": "missing from receipt"} for field in missing]
    if stored["digest"] != sha256_bytes(canonical({field: stored[field] for field in RECEIPT_IDENTITY})):
        mismatches.append({"field": "digest", "message": "receipt content does not match its digest"})
    current = build_receipt(root)
    for field in RECEIPT_IDENTITY:
        if stored[field] != current[field]:
            mismatches.append({"field": field, "message": "changed since receipt", "diff": diff(stored[field], current[field])})
    if current["status"] != "PASS":
        mismatches.append({"field": "status", "message": "current basis does not pass"})
    return mismatches


def diff(old, new):
    if isinstance(old, dict) and isinstance(new, dict):
        return sorted(key for key in set(old) | set(new) if old.get(key) != new.get(key))
    return {"receipt": old, "current": new}


# ---------------------------------------------------------------- selftest

def copy_basis(root: Path, target: Path) -> None:
    paths = [SPEC, DESIGN, str(Path(__file__).resolve().relative_to(REPO_ROOT))]
    paths += [name for name in CONFIG_FILES if (root / name).is_file()]
    paths += [str(p.relative_to(root)) for p in (root / SOURCE_DIR).glob("*.java")]
    paths += [str(p.relative_to(root)) for p in (root / TEST_DIR).glob("*.java")]
    for relative in paths:
        destination = target / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(root / relative, destination)


def selftest(root: Path) -> list[dict]:
    cases = []

    def record(name, passed, detail):
        cases.append({"case": name, "status": "PASS" if passed else "FAIL", "detail": detail})

    with tempfile.TemporaryDirectory() as scratch:
        copy = Path(scratch)
        copy_basis(root, copy)

        intact = check_spec(copy)
        record("intact basis passes check", not intact, intact)

        spec = load_strict(copy / SPEC)
        spec["type_safety_audit"] = [row for row in spec["type_safety_audit"] if row["requirement"] != "LC-004"]
        (copy / SPEC).write_text(json.dumps(spec, indent=2) + "\n", encoding="utf-8")
        removed = check_spec(copy)
        record("removed audit binding is rejected",
               any(f["rule"] == "A001" and "LC-004" in f["message"] for f in removed), removed)
        shutil.copy2(root / SPEC, copy / SPEC)

        receipt = build_receipt(copy)
        receipt_path = copy / "receipt.json"
        receipt_path.write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
        clean = verify_receipt(copy, receipt_path)
        record("unchanged basis verifies", not clean, clean)

        source = sorted((copy / SOURCE_DIR).glob("*.java"))[0]
        original = source.read_text(encoding="utf-8")
        source.write_text(original + "\n// altered\n", encoding="utf-8")
        altered = verify_receipt(copy, receipt_path)
        record("altered frozen source hash is rejected",
               any(m["field"] == "declaration_hashes" for m in altered), altered)
        source.write_text(original, encoding="utf-8")

        design = copy / DESIGN
        design.write_text(design.read_text(encoding="utf-8") + "\naltered\n", encoding="utf-8")
        record("altered design hash is rejected",
               any(m["field"] == "design_hash" for m in verify_receipt(copy, receipt_path)), "design_hash")
        shutil.copy2(root / DESIGN, design)

        tampered = dict(receipt, spec_hash="0" * 64)
        receipt_path.write_text(json.dumps(tampered, indent=2) + "\n", encoding="utf-8")
        record("edited receipt is rejected",
               any(m["field"] == "digest" for m in verify_receipt(copy, receipt_path)), "digest")

        record("valid design shape alone does not mark behavior evidence passed",
               not {"EV-FOCUSED", "EV-SMOKE", "EV-BUILD"} & set(receipt["evidence_hashes"]),
               sorted(receipt["evidence_hashes"]))
    return cases


# ---------------------------------------------------------------- main

def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--root", type=Path, default=REPO_ROOT, help="repository root")
    commands = parser.add_subparsers(dest="command", required=True)
    commands.add_parser("check")
    receipt_parser = commands.add_parser("receipt")
    receipt_parser.add_argument("--out", type=Path)
    verify_parser = commands.add_parser("verify")
    verify_parser.add_argument("--receipt", type=Path, required=True)
    commands.add_parser("selftest")
    args = parser.parse_args(argv)
    root = args.root.resolve()

    try:
        if args.command == "check":
            findings = check_spec(root)
            print(json.dumps({"status": "PASS" if not findings else "FAIL", "findings": findings}, indent=2))
            return 0 if not findings else 1
        if args.command == "receipt":
            receipt = build_receipt(root)
            out = args.out or root / DEFAULT_RECEIPT
            out.parent.mkdir(parents=True, exist_ok=True)
            partial = out.with_suffix(out.suffix + ".partial")
            partial.write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
            partial.replace(out)
            summary = {key: receipt[key] for key in ("stage", "feature", "status", "digest", "evidence_hashes")}
            print(json.dumps({"receipt": str(out), **summary}, indent=2))
            return 0 if receipt["status"] == "PASS" else 1
        if args.command == "verify":
            mismatches = verify_receipt(root, args.receipt)
            print(json.dumps({"status": "PASS" if not mismatches else "FAIL", "mismatches": mismatches}, indent=2))
            return 0 if not mismatches else 1
        cases = selftest(root)
        print(json.dumps({"status": "PASS" if all(c["status"] == "PASS" for c in cases) else "FAIL",
                          "cases": [{k: c[k] for k in ("case", "status")} for c in cases]}, indent=2))
        return 0 if all(c["status"] == "PASS" for c in cases) else 1
    except ToolError as error:
        print(json.dumps({"status": "ERROR", "message": str(error)}), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
