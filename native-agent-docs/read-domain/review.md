# CAP-READ-DOMAIN review ledger

Append-only. Review rounds are tied to exact content digests. Acceptance targets remain PLANNED until S3.

## Current state

- Workflow state: SPEC_DRAFT; S1 structure check PASS; fresh review not run
- Latest round: 0
- Spec digest: `0d0655d37508c38347bbc8810a726eecca5eb369949a653540dba6b62a5c536b`
- Product digest at spec basis: `2edba8b1e45db4f53d2f2dad7ba290a0067f5287a30244f2e6bdad80e7c05bae` (Milestone slices section added with this split)
- Open Blockers: none recorded
- Open Majors: none recorded
- Gate order: review after none; first slice
- Next permitted action: fresh independent specification review of the exact digest (EV-SPEC-REVIEW) by a reviewer that did not author the split

## Round 0 — split from CAP-SEMANTIC-READS

- Source: `semantic-reads/spec.json` revision 2 (`13183292b057ec708e01744089e3b3e82455cfc77112b3e5e54d82bad5687586`), history in `semantic-reads/review.md`
- Owner decision (2026-09-15): split roadmap milestone 2 into five slices (read-domain, read-pipeline, read-text-search, read-symbols, read-references)
- Author: Claude Code session (Opus 5) that reviewed Round 2 and authored revision 2 and this split; not eligible to review
- Requirements: SR-001, SR-002, SR-012, SR-014, SR-021, SR-022, SR-023, SR-024
- Acceptance: AC-001, AC-012, AC-018, AC-023, AC-029, AC-032, AC-033
- Carried: Round 2 dispositions SR-R2-003, SR-R2-004 (token domain) and SR-R2-021 carried into SR-012, SR-014 and SR-024
- Local decisions are owned by exactly one slice; other slices reference them as `REF:<slice>/spec.json#<key>`
- Mechanical command: `python3 scripts/native-spec/check_slice.py check --spec native-agent-docs/read-domain/spec.json --planned-targets`; result PASS, zero findings
- Verdict: none; review pending
