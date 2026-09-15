# CAP-READ-PIPELINE review ledger

Append-only. Review rounds are tied to exact content digests. Acceptance targets remain PLANNED until S3.

## Current state

- Workflow state: SPEC_DRAFT; S1 structure check PASS; fresh review not run
- Latest round: 0
- Spec digest: `4bd9a8f7acdd8981dd2cd607e6684a85e00cac2924ebc8d851459e23b7c51040` (changed by read-domain revision 1)
- Product digest at spec basis: `2edba8b1e45db4f53d2f2dad7ba290a0067f5287a30244f2e6bdad80e7c05bae` (Milestone slices section added with this split)
- Open Blockers: none recorded
- Open Majors: none recorded
- Gate order: review after read-domain S1
- Next permitted action: fresh independent specification review of the exact digest (EV-SPEC-REVIEW) by a reviewer that did not author the split

## Round 0 — split from CAP-SEMANTIC-READS

- Source: `semantic-reads/spec.json` revision 2 (`13183292b057ec708e01744089e3b3e82455cfc77112b3e5e54d82bad5687586`), history in `semantic-reads/review.md`
- Owner decision (2026-09-15): split roadmap milestone 2 into five slices (read-domain, read-pipeline, read-text-search, read-symbols, read-references)
- Author: Claude Code session (Opus 5) that reviewed Round 2 and authored revision 2 and this split; not eligible to review
- Requirements: SR-003, SR-004, SR-005, SR-015, SR-016, SR-019, SR-020, SR-026
- Acceptance: AC-002, AC-004, AC-014, AC-015, AC-017, AC-024, AC-030
- Carried: Round 2 dispositions SR-R2-001, SR-R2-002, SR-R2-009 (file scope), SR-R2-010, SR-R2-015, SR-R2-016, SR-R2-018 carried into SR-004, SR-016, SR-019, SR-020 and SR-026
- Local decisions are owned by exactly one slice; other slices reference them as `REF:<slice>/spec.json#<key>`
- Mechanical command: `python3 scripts/native-spec/check_slice.py check --spec native-agent-docs/read-pipeline/spec.json --planned-targets`; result PASS, zero findings
- Verdict: none; review pending

## Change — clauses moved by read-domain revision 1

- Cause: read-domain specification review Round 1 and owner decisions 2026-09-15; see `read-domain/review.md` Revision 1
- Spec digest before: `3bd189e6fe9dcdcecb5ff8daff795d609aa7b16cbfa44d39a9a9feaf747d48bd`; after: `4bd9a8f7acdd8981dd2cd607e6684a85e00cac2924ebc8d851459e23b7c51040`
- Decision `read_handle_issue` added (owner decision): new read handle on every call; SR-003 states it
- SR-015 adds the equal-outcome rule moved from read-domain SR-024; AC-030 repeats read_file and find_file
- SR-028 and AC-036 added: adapter layering and no MCP, HTTP or reflection, moved from read-domain SR-021; audit row and stage bindings added
- Requirement ownership map in assumption 1 adds read-pipeline SR-028
- Mechanical command: `python3 scripts/native-spec/check_slice.py check --spec native-agent-docs/read-pipeline/spec.json --planned-targets`; result PASS, zero findings
- Not a review receipt; review still pending
