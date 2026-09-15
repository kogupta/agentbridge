# CAP-READ-SYMBOLS review ledger

Append-only. Review rounds are tied to exact content digests. Acceptance targets remain PLANNED until S3.

## Current state

- Workflow state: SPEC_DRAFT; S1 structure check PASS; fresh review not run
- Latest round: 0
- Spec digest: `285e99ae5022c58b2a390f676120c7b5d50f5dca0a84c0cf854f5515ce0c88c7` (changed by read-domain revision 1)
- Product digest at spec basis: `2edba8b1e45db4f53d2f2dad7ba290a0067f5287a30244f2e6bdad80e7c05bae` (Milestone slices section added with this split)
- Open Blockers: none recorded
- Open Majors: none recorded
- Gate order: review after read-pipeline S1
- Next permitted action: fresh independent specification review of the exact digest (EV-SPEC-REVIEW) by a reviewer that did not author the split

## Round 0 — split from CAP-SEMANTIC-READS

- Source: `semantic-reads/spec.json` revision 2 (`13183292b057ec708e01744089e3b3e82455cfc77112b3e5e54d82bad5687586`), history in `semantic-reads/review.md`
- Owner decision (2026-09-15): split roadmap milestone 2 into five slices (read-domain, read-pipeline, read-text-search, read-symbols, read-references)
- Author: Claude Code session (Opus 5) that reviewed Round 2 and authored revision 2 and this split; not eligible to review
- Requirements: SR-006, SR-007, SR-008, SR-011, SR-013, SR-018, SR-025
- Acceptance: AC-003, AC-005, AC-007, AC-008, AC-011, AC-013, AC-021, AC-025, AC-031
- Carried: Round 2 dispositions SR-R2-006, SR-R2-007, SR-R2-012 (symbols), SR-R2-014, SR-R2-017 (names), SR-R2-022 and SR-R2-023 (hint) carried into SR-006, SR-007, SR-013 and SR-025
- Local decisions are owned by exactly one slice; other slices reference them as `REF:<slice>/spec.json#<key>`
- Mechanical command: `python3 scripts/native-spec/check_slice.py check --spec native-agent-docs/read-symbols/spec.json --planned-targets`; result PASS, zero findings
- Verdict: none; review pending

## Change — clauses moved by read-domain revision 1

- Cause: read-domain specification review Round 1 and owner decisions 2026-09-15; see `read-domain/review.md` Revision 1
- Spec digest before: `7375e477d1ce23fee70a33f3cd111722aa60ae5c37d523187226b5473ff36f93`; after: `285e99ae5022c58b2a390f676120c7b5d50f5dca0a84c0cf854f5515ce0c88c7`
- Decision REF `read_handle_issue` added; SR-008 issues a new read handle on every call; AC-008 repeats get_symbol_info
- SR-011 names registry issue-or-reuse and smart pointer release with SmartPointerManager.removePointer, moved from read-domain SR-023; AC-011 checks the pointer count after disposal
- Requirement ownership map in assumption 1 adds read-pipeline SR-028
- Mechanical command: `python3 scripts/native-spec/check_slice.py check --spec native-agent-docs/read-symbols/spec.json --planned-targets`; result PASS, zero findings
- Not a review receipt; review still pending
