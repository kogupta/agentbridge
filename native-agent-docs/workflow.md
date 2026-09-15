# Native agent — implementation workflow

This file defines how a feature from `product.md` becomes code: principles, gates, reviewer contract, failure routing, the feature slice index and the lifecycle slice stages. Product decisions live only in `product.md`; this file references them and never restates them.

## Goal

**Reduce the design/search space presented to the implementation LLM as aggressively as practical.** The central metric is: how many meaningful design decisions remain available to the implementation model? Drive it close to zero. The implementation model decides local algorithmic details, not architecture or domain semantics.

The goal is not elaborate formal specification, documentation, code generation or process ceremony. Reject machinery whose maintenance cost exceeds how much it constrains the implementation model. Optimize for correctness, determinism, type safety, small search space, minimal duplication, minimal maintenance and compact implementation.

Principles:

1. Spec-driven development.
2. Type-driven design.
3. Unsound boundary, type-safe core.
4. Make illegal states unrepresentable, where that stays simple and legible. No type gymnastics to claim stronger typing.
5. Deterministic workflows and state transitions.
6. Minimal implementation freedom.
7. Check internal specification consistency before implementation.
8. Inconsistency or unresolved ambiguity fails the tool/workflow; the LLM never guesses.
9. Prefer compiler, type, test and architecture enforcement over prose instructions.
10. Never maintain two parallel implementations of the same truth.

```text
reference behavior / requirement
            ↓
     normalized feature spec
            ↓
     CONSISTENCY GATE (SPEC_VALID)
            ↓
      type/API design
            ↓
    SPEC ↔ DESIGN GATE (DESIGN_VALID)
            ↓
  frozen implementation surface
            ↓
    implementation LLM
            ↓
compiler / tests / architecture checks
```

## Workflow invariants

W1. Each normative decision has one canonical key and value. Features reference shared decisions and cannot override them.

W2. A feature cannot enter implementation without valid specification and design receipts for the exact frozen inputs. Missing, malformed, stale or unavailable evidence fails closed.

W3. Every normative behavior has acceptance criteria. Every invariant has an enforcement strategy before SPEC_VALID and a concrete binding before DESIGN_VALID.

W4. Mechanical checks claim only their supported semantics. Prose review is not proof. A passing model is not proof of Java/IntelliJ behavior.

W5. Java owns domain representation and transition policy. Kotlin owns coroutine execution and platform coroutine interop. JSON, PSI, coroutine types and provider SDK objects do not enter the Java domain (`product.md` CORE_LANGUAGE).

W6. No workflow stage changes product behavior outside its gated feature packet. Product mutations need real effect/lifecycle proof.

W7. A frozen surface changes only by reopening its design/spec gate. Implementation agents choose local algorithms, not new state semantics, public abstractions or dependencies.

W8. DESIGN_VALID requires a representation/operation audit for every invariant: invalid representation or operation → type/API prevention → residual runtime obligation → justification. TEST or RUNTIME alone is not acceptable when a simple type, constructor, ownership or visibility change eliminates the invalid case.

## Artifacts

Tracked under `native-agent-docs/`. One directory per feature:

- `<feature>/spec.json`: strict JSON. Identity, requirements, acceptance, type-safety audit, operation matrix, null boundaries, evidence IDs, stage bindings, non-goals, assumptions, `open_decisions` (must be empty for SPEC_VALID).
- `<feature>/design.md`: actual Java surface, ownership, algorithm and rejected alternatives. It references `spec.json` rows and code; it does not copy requirement text, matrices or signatures.
- `<feature>/review.md`: append-only review ledger with current state at the top.

Generated receipts and packets are disposable outputs tied to content hashes, kept under the ignored `.agent-work/`, never editable completion claims. Regenerate source hashes (`EV-SOURCE`) from current paths; do not maintain hand-written manifests.

When a second feature exists, move shared decisions from `product.md` into `decisions.json` (key, primitive type, allowed values, selected value, rationale, evidence IDs; no feature-level override or implicit default). Until then, `product.md` Decisions is the registry.

Deferred by owner decision (2026-09-15): two features exist, but the migration waits. The frozen `lifecycle-admission` and `domain-run-driver` specs reference `REF:product.md#KEY`, and moving the registry would change their digests and reopen their gates. New specs use the same `REF:product.md#KEY` form. Migrate all specs together when a gate reopens for another reason or the owner schedules the migration.

Feature fields when the format generalizes:

| Field | Content |
|---|---|
| `id`, `title` | Stable ID; directory name matches. |
| `evidence` | Repository + path + symbol/range + hash, or requirement-origin evidence. Reference behavior vs intentional deviation. |
| `depends_on` | Feature IDs; acyclic. Runtime state loops are not dependency cycles. |
| `decision_refs` | Shared keys only. Local decisions defined once, never shadowing global keys. |
| `contracts` | Behavior ID, trigger/input, observable output, ordered failure/precedence cases, acceptance IDs. |
| `invariants` | ID, canonical statement, assumptions, enforcement categories, acceptance references. |
| `machine` | Optional initial state, states, terminal/quiescent classification, events, unique state/event/case outcomes, explicit rejected/ignored events. |
| `acceptance` | ID, given/when/then, negative case where meaningful, automated/manual mode, observable oracle. |
| `non_goals` | Concrete exclusions, re-entry condition where useful. |

Spec inputs/outputs describe domain concepts, not a copy of Java DTO fields or JSON schemas. Design binds them to declarations.

## Enforcement categories

Every invariant names at least one: `TYPE`, `API`, `ARCHITECTURE`, `TEST`, `RUNTIME`, `MODEL-CHECKER`. Prefer the strongest simple mechanism nearest the code. An invariant with no enforcement location is a design failure.

| Invariant (design target, not existing symbols) | Primary mechanism | Residual check |
|---|---|---|
| Waiting-for-tools has work | State variant holding a validated nonempty batch | Reject empty construction; empty response takes another transition |
| Distinct run/call/session IDs | Separate value types | Validate source values and uniqueness where allocated |
| Incomplete provider output cannot execute | Boundary yields rejected or validated complete response | Protocol test with syntactically valid length-truncated arguments |
| JSON/PSI/coroutines outside domain | Test-scoped ArchUnit rule over compiled domain classes | Include signatures and implementation dependencies |
| One active provider request | State-specific admission and serial driver ownership | Overlapping callback/Send scenario |
| Stop prevents not-started effects | One synchronized admission boundary shared with Stop | Queued-EDT race regression; a token alone is insufficient |
| Stale inspection cannot authorize edit | Typed inspected identity plus adapter revalidation | Change document after inspection; reject without wrong-target edit |
| One terminal result per call before continuation | Batch ledger keyed by CallId; named unstarted cancellation | Duplicate/late completion rejected |
| Expected failures stay actionable | Operation-local sealed outcomes | Not swallowed as success or generic error |
| No null in constructed core values | Constructor checks, immutable collections, annotations | Boundary regression for plausible nullable input |

Add ArchUnit only as a test dependency once the domain exists. ArchUnit cannot prove absence of reflection inside third-party internals.

## Mechanical checks (spec-lint)

Future tooling, not a prerequisite for the current slice. One small Python 3 standard-library CLI under `scripts/native-spec/`, development-only. Fixed project rules: no plugin system, expression evaluator, solver or repair mode. Diagnostics carry rule code, JSON pointer, related IDs and message, sorted deterministically. Exit 0 on gate success, 1 for findings, 2 for tool failure. Reject unknown format versions/fields.

A check gates the selected feature and its transitive dependencies but parses all files for global uniqueness and references.

- Strict parsing: duplicate object keys rejected before dictionary construction; non-finite numbers rejected.
- Global ID uniqueness; no duplicate decision keys even with equal values; typed allowed values; no shadowing.
- Every feature, decision, invariant, evidence, transition and acceptance reference resolves.
- No feature dependency cycles; enforcement categories from the fixed set.
- Every behavior has acceptance; every invariant has enforcement.
- Finite tables: known states/events, one initial state, unambiguous keys, coverage of declared combinations, no unintended terminal outgoing transitions, reachability, path to quiescent/terminal states where required.
- Direct structured contradictions only: incompatible required values, empty allowed-value intersection, inconsistent min/max, same state/event/case with competing outcomes.
- Unresolved decisions and explicit TODO/TBD markers in normative strings fail.
- Report coverage: structural checks vs semantic-review obligations. Graph reachability does not prove guard satisfiability, concurrency or liveness.

The Python validation functions are the single schema implementation. No separate handwritten JSON Schema.

## Semantic reviewer contract

A fresh-context reviewer receives the exact feature closure, shared decisions, reference excerpts and mechanical report, and reads every normative clause. Its only job is to find contradictions, omitted outcomes, overlapping cases, ownership ambiguity, infeasible requirements and spec-to-evidence mismatches. It never modifies the spec.

Each finding has an ID, severity (blocking/nonblocking), requirement IDs, cited locations or missing case, a concrete witness and the decision needed. Uncertainty about required semantics is blocking. A receipt records input digest, reviewer identity, reviewed IDs, findings and dispositions. A clean review of an earlier hash does not carry forward. Author self-audit does not satisfy the receipt. A missing reviewer is BLOCKED, not approved.

Claims are limited to "mechanical checks pass and no unresolved blocking findings on this basis".

## SPEC_VALID gate

For the feature and its dependency closure:

1. Strict parsing and all supported checks pass.
2. Every selected decision is resolved; no authority conflict remains.
3. Reference evidence is readable and matches its frozen hash, or changed source was re-reviewed.
4. Every behavior has acceptance; every invariant has enforcement and explicit assumptions.
5. Semantic review covers this exact digest with zero unresolved blocking findings.
6. Any required model check has a scope-qualified success; counterexample, timeout or unknown result blocks.

## DESIGN_VALID gate

1. Spec receipt and closure remain current.
2. Every behavior, decision and invariant maps to concrete types, signatures, architecture rules, runtime assertions or acceptance scenarios.
3. Symbols are resolved through IntelliJ with overload identity, not regex. A missing symbol is failure.
4. Declarations compile against the real build (Java and, where used, Kotlin). No fake implementations, throwing stubs or dummy providers.
5. A fresh reviewer checks both directions: each requirement is enforced; each design semantic has a requirement basis. The audit covers sum types, state payloads, validated values, constructor authority, transitions, argument/result correlation, collection invariants, aliasing/ownership, protocol progression, visibility and nullness. Record rejected stronger alternatives. Do not claim non-null or affine references, deep record immutability, or that inspection proves future external state.
6. Allowed files/packages, frozen declarations, dependencies, tests and commands are enumerated. No unresolved JSON stack, executor ownership or platform API choice.
7. Behavioral tests not implementable yet are PLANNED, not PASSED, and run before DONE.

Declarations move into production once; there is never a second copy of the interfaces.

## Implementation packet

Generate the packet; do not author a second prose handoff. It contains:

- feature ID, target change, spec/design digests and gate receipts;
- canonical decisions, invariants and acceptance for the closure, each once;
- frozen declarations and signatures of direct dependencies;
- reference excerpts and intentional Pi deviations;
- allowed edits, approved new files, forbidden dependencies, exact verification commands and manual scenarios;
- relevant bodies and tests only, derived from IDE references/implementations plus build dependencies, with a short curated list of dynamic edges the index cannot see;
- stop rule: missing dependency, ambiguity, incompatible API or needed surface change returns a structured blocker to the owning gate.

Packet output is reproducible: stable ordering, no timestamps in the digest. Compare the frozen surface by extracted signatures, not whole-file hashes.

Progress report after each semantic stage: source changes; observable behavior implemented; exact tests/commands/scenarios and outcomes; manual gates not exercised and why; commit identity; next stage or exact blocker.

## Workflow states

| State / event | Next | Action |
|---|---|---|
| New or revised requirement | SPEC_DRAFT | Normalize decisions and closure |
| SPEC_DRAFT + invalid structure/contradiction | SPEC_INVALID | Report rules/findings; no design |
| SPEC_INVALID + revision | SPEC_DRAFT | Rerun checks and review |
| SPEC_DRAFT + gate passes | SPEC_VALID | Freeze receipt |
| SPEC_VALID + begin design | DESIGN_DRAFT | Declarations and bindings |
| DESIGN_DRAFT + insufficient enforcement | DESIGN_INVALID | Repair design if spec unchanged |
| DESIGN_INVALID + revision | DESIGN_DRAFT | Recompile and review changed closure |
| DESIGN_DRAFT + gate passes | DESIGN_VALID | Freeze surface; emit packet |
| DESIGN_VALID + packet consumed | IMPLEMENTING | Bounded edits only |
| IMPLEMENTING + spec policy missing | SPEC_INVALID | Invalidate design/packet; do not choose policy |
| IMPLEMENTING + surface change needed | DESIGN_INVALID | Redesign affected closure |
| IMPLEMENTING + change ready | VERIFYING | Contract tests, architecture checks, build, smoke |
| VERIFYING + implementation defect | IMPLEMENTING | Fix within frozen contract |
| VERIFYING + spec/design defect | SPEC_INVALID / DESIGN_INVALID | Classify by ownership; invalidate downstream receipts |
| VERIFYING + all evidence passes | DONE | Record evidence; semantic commit |
| Any + missing environment/reviewer/access | Same state, BLOCKED | Preserve resume point; no receipt |
| Any success + upstream hash change | Earliest affected draft | Invalidate dependent receipts only |

Receipts are evidence, not a mutable `status=valid` flag. Write results atomically after completion. No workflow server, queue or orchestration service.

## Formal tools

Default: none (`product.md` FORMAL_TOOL_DEFAULT). Strict JSON plus fixed checks cover decisions, references, coverage and small state tables. Quint fits operational Stop/admission interleavings; Alloy 6 fits relational ownership constraints. Adopt at most one, only for a concrete question typing and testing cannot answer cheaply.

If a spike is authorized: one run, bounded calls, Stop, queued/start/completion events, content disposal. No PSI, OAuth or UI. Require a reachable positive scenario and a deliberately broken rule that yields the expected counterexample. Record bounds, backend and solver mode; "no counterexample" is not an unbounded proof. Quint's Apalache is bounded (`--max-steps`, default 10) and its simulator does not check temporal properties ([Quint docs](https://quint.sh/docs/model-checkers)). Alloy `check` searches counterexamples within scope ([Practical Alloy](https://practicalalloy.github.io/chapters/structural-topics/topics/commands/index.html)). Keep the resulting regression test, archive the model, and never keep a permanent normative twin of the transition table.

## Derive rather than maintain

Derive modules and dependencies from Gradle; declarations, references, implementations and callers from IntelliJ; frozen signatures from compiler-visible declarations; test outcomes from runner reports; touched files from actual mutations. Maintain only decisions, requirements, assumptions, acceptance oracles and requirement-to-enforcement mapping. No permanent graph, duplicate index, spec-to-Java generator or model of every class.

## Feature slices

A slice's stage bindings are in its `spec.json`. Its state is only the Current state section of its `review.md`.

| Slice | Roadmap milestone | Code |
|---|---|---|
| `lifecycle-admission/` | 0 | `plugin-core/.../nativeagent/lifecycle/` |
| `domain-run-driver/` | 1 | `plugin-core/.../nativeagent/run/` (Java and Kotlin) |
| `semantic-reads/` | 2 | Superseded by the five read slices below; no code |
| `read-domain/` | 2 (R1) | Not installed (specification draft) |
| `read-pipeline/` | 2 (R2) | Not installed (specification draft) |
| `read-text-search/` | 2 (R3) | Not installed (specification draft) |
| `read-symbols/` | 2 (R4) | Not installed (specification draft) |
| `read-references/` | 2 (R5) | Not installed (specification draft) |

## Lifecycle/admission slice stages

Canonical requirements, audit, stage bindings and evidence IDs are in `lifecycle-admission/spec.json`. Stage checks reference those IDs and never restate them. Tooling beyond what this slice uses is not built.

### S1 — Normalize and design

Establish an executable, type-audited contract for lifecycle ownership, sequential admission and terminal accounting. Resolve Stop-before-start, Stop-after-start, content close, stale callbacks, repeated completion and next-run admission without leaving policy choices. Run strict JSON/reference/coverage checks on the real slice. Exit: `spec.json` stage S1.

### S2 — Independent review and freeze

Review covers every requirement, audit row, state/event outcome, stage contract and acceptance scenario. Resolve blockers in one address pass; a targeted final review checks corrections and neighbors. Any material design change invalidates affected evidence. No author self-approval. Exit: `spec.json` stage S2 with no open Blockers or Majors.

### S3 — Implement and qualify

Implement the frozen API. Apply effects through the real admission boundary, never a pre-enqueue check. Retain completed effects and block new runs until owned work settles. No placeholder provider/tool code. Run the focused tests and smoke named by `spec.json` stage S3. Commit: `feat: enforce native run lifecycle and effect admission`, through repository-authorized IDE VCS after verification, no push.

## Verification rules

- Execute builds through the project wrapper (`./gradlew`); the default `:plugin-core:test` task runs JUnit Platform.
- Run the exact selected tests for a slice, then the module build before each semantic commit.
- Final product qualification: unit tests, IDE integration tests through their real task, `:plugin-core:buildPlugin`, compatibility verification against declared minimum and current IDEs, real native UI startup and the roadmap scenarios. Record the actual task names from the build; never invent a task.
- Record environment failures; never suppress checks.

## Deliberately not built

No generic specification language, predicate evaluator, solver framework, durable state-machine twin, production code generation, workflow server, event store, graph database, duplicate PSI index, all-repository context collector, reflection-based registration, generic effects runtime, coroutine wrapper for every Java method, mandatory JSpecify/NullAway, or Java 25 compatibility break without a product decision.
