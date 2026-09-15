# CAP-READ-TEXT-SEARCH review ledger

Append-only. Review rounds are tied to exact content digests. Acceptance targets remain PLANNED until S3.

## Current state

- Workflow state: SPEC_DRAFT; S1 structure check PASS; fresh review not run
- Latest round: 0
- Spec digest: `02af5dfdf1190020f34395cb9439b8e5585cb37be8f8d0d7c94599e47a612de8` (changed by read-domain revision 1)
- Product digest at spec basis: `2edba8b1e45db4f53d2f2dad7ba290a0067f5287a30244f2e6bdad80e7c05bae` (Milestone slices section added with this split)
- Open Blockers: none recorded
- Open Majors: none recorded
- Gate order: review after read-pipeline S1
- Next permitted action: fresh independent specification review of the exact digest (EV-SPEC-REVIEW) by a reviewer that did not author the split

## Round 0 — split from CAP-SEMANTIC-READS

- Source: `semantic-reads/spec.json` revision 2 (`13183292b057ec708e01744089e3b3e82455cfc77112b3e5e54d82bad5687586`), history in `semantic-reads/review.md`
- Owner decision (2026-09-15): split roadmap milestone 2 into five slices (read-domain, read-pipeline, read-text-search, read-symbols, read-references)
- Author: Claude Code session (Opus 5) that reviewed Round 2 and authored revision 2 and this split; not eligible to review
- Requirements: SR-010, SR-017
- Acceptance: AC-010, AC-016, AC-022
- Carried: Round 2 dispositions SR-R2-004, SR-R2-005, SR-R2-009 and SR-R2-013 carried into SR-010 and SR-017
- Local decisions are owned by exactly one slice; other slices reference them as `REF:<slice>/spec.json#<key>`
- Mechanical command: `python3 scripts/native-spec/check_slice.py check --spec native-agent-docs/read-text-search/spec.json --planned-targets`; result PASS, zero findings
- Verdict: none; review pending

## Change — clauses moved by read-domain revision 1

- Cause: read-domain specification review Round 1 and owner decisions 2026-09-15; see `read-domain/review.md` Revision 1
- Spec digest before: `a78a0b562966da1e57108d9c0d2d29f3fead01f9b66b664afd8cc5371a6f3fce`; after: `02af5dfdf1190020f34395cb9439b8e5585cb37be8f8d0d7c94599e47a612de8`
- AC-016 asserts a new page token for every page result with more results, moved from read-domain SR-024
- Requirement ownership map in assumption 1 adds read-pipeline SR-028
- Mechanical command: `python3 scripts/native-spec/check_slice.py check --spec native-agent-docs/read-text-search/spec.json --planned-targets`; result PASS, zero findings
- Not a review receipt; review still pending
