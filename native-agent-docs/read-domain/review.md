# CAP-READ-DOMAIN review ledger

Append-only. Review rounds are tied to exact content digests. Acceptance targets remain PLANNED until S3.

## Current state

- Workflow state: SPEC_DRAFT (revision 1 after SPEC_INVALID in Round 1); S1 structure check PASS; fresh review of revision 1 not run
- Latest round: 1 (review), revision 1 (address)
- Spec digest: `e55de1e816059c94bbfe84fa9f38c4555bcfadec4aeef77a777faa86d5717989`
- Product digest at spec basis: `2edba8b1e45db4f53d2f2dad7ba290a0067f5287a30244f2e6bdad80e7c05bae`
- Open Blockers: none recorded (RD-R1-001 and RD-R1-002 addressed in revision 1, not yet re-reviewed)
- Open Majors: none recorded (RD-R1-003 to RD-R1-010 addressed in revision 1, not yet re-reviewed)
- Gate order: review after none; first slice
- Next permitted action: fresh independent specification review of the exact revision 1 digest (EV-SPEC-REVIEW, `workflow.md` SPEC_VALID gate 5) by a reviewer that did not author the split or revision 1

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

## Round 1 — independent specification review

- Reviewer: separate tool run by the owner from `.agent-work/native-agent-docs/read-domain/spec-review-prompt.md`; the result does not name the reviewer model
- Spec digest: `0d0655d37508c38347bbc8810a726eecca5eb369949a653540dba6b62a5c536b`; basis verified by the reviewer (`basis_verified: true`)
- Result: `.agent-work/native-agent-docs/read-domain/spec-review-r1.json` (`5c53a3d61aaec36513e1640b5b31ad091ecd32695a4eef739598c7836f774a92`)
- Platform facts: 3 checked, 3 CONFIRMED (run package holds Kotlin coroutine driver contracts; CallAdmission owns Effect execution; ToolOutcome is the terminal envelope)
- Verdict: SPEC_INVALID

### Findings

- **RD-R1-001 — Blocker, open.** R1 depends normatively on later slices: nineteen decision REFs, an adapter package in AC-018 and adapter declarations in EV-DESIGN-SOURCE.
- **RD-R1-002 — Blocker, open.** Argument rejection has no closed names, no precedence and no whitespace-only rule; the matrix attributes rejection to the codec.
- **RD-R1-003 — Major, open.** AC-033 does not prove defaults, truncated flags, the production cap, TOO_MANY_RESULTS without a page, or UTF-8 byte and UTF-16 unit counting.
- **RD-R1-004 — Major, open.** AC-012 does not separate issue, reuse and lookup recency, and a registry with colliding local counters passes.
- **RD-R1-005 — Major, open.** The operation matrix is not a complete registry transition table; the null matrix omits predicate and callback inputs.
- **RD-R1-006 — Major, open.** SR-014 issuance by get_symbol_info and read_file has no R1 acceptance.
- **RD-R1-007 — Major, open.** AC-018 allows the whole run package, uses a hand-listed class set and inspects an adapter package that R1 does not have.
- **RD-R1-008 — Major, open.** AC-023 does not prove IDE-state independence or the CacheGeneration prefix clause of SR-022.
- **RD-R1-009 — Major, open.** SR-023 assigns session wiring, smart pointer release and Effect cancellation to R1 against its non-goals.
- **RD-R1-010 — Major, open.** AC-032 proves comparators only; equal outcomes and fresh read handles and page tokens have no acceptance.

## Revision 1 — address Round 1

- Author: Claude Code session (Opus 5) that authored the split, run from the `address-plan-review-comments` skill; this revision is not a review receipt
- Spec digest before: `0d0655d37508c38347bbc8810a726eecca5eb369949a653540dba6b62a5c536b`; after: `e55de1e816059c94bbfe84fa9f38c4555bcfadec4aeef77a777faa86d5717989`
- Mechanical command: `python3 scripts/native-spec/check_slice.py check --spec native-agent-docs/<slice>/spec.json --planned-targets` for all five read slices; result PASS, zero findings each
- Owner decisions (2026-09-15): R1 keeps only platform-free values, registry and comparators, and adapter clauses move to their owning slices; `argument_rejection` first failing check in fixed order; `whitespace_query` valid and untrimmed; `read_handle_issue` (owned by read-pipeline) new read handle on every read_file and get_symbol_info call
- Labeled normalizations without an owner question (review suggested resolutions): assumption 5 (argument names, reason set, check order, Java identifier rule, int lines, line and byte counting, registry identity, predicate order, offered payload release, exception and release order)
- Sibling slices changed (drafts, no review run): read-pipeline, read-text-search, read-symbols, read-references; each ledger records the move
- Platform fact for the moved smart pointer clause (ij-search): `SmartPointerManager.removePointer` at SmartPointerManager.java:86; `SmartPointerManagerImpl.getPointersNumber` (@TestOnly) at SmartPointerManagerImpl.java:230-235
- Stage count and dependencies unchanged; scope narrowed by owner decision

### Dispositions

- **RD-R1-001 — Addressed.** Decisions keep only product REFs, `result_collection_cap`, `argument_rejection` and `whitespace_query`; SR-001 and SR-002 no longer cite later requirements; AC-018 and EV-DESIGN-SOURCE cover read domain classes only; scope states that run and lifecycle dependencies set gate order only.
- **RD-R1-002 — Addressed.** SR-001 defines the rejection algebra (argument names, reasons), argument order per operation, check order, defaults and the whitespace rule; matrix rows name the factory, not the codec; AC-001 adds multi-failure and whitespace cases.
- **RD-R1-003 — Addressed.** SR-002 states counting rules and leaves collection to adapter slices; AC-033 adds default limit, truncated flags, production cap, cap 1, TOO_MANY_RESULTS without a page, 3-byte against 4-byte UTF-8 texts and surrogate-pair excerpts.
- **RD-R1-004 — Addressed.** SR-012 embeds registry identity and defines issue-or-reuse; AC-012 tests lookup, reuse and issue recency as separate transitions, wrong-kind lookup without refresh, predicate order and a colliding identifier from registry B.
- **RD-R1-005 — Addressed.** Operation matrix has rows for argument construction, issue below and at capacity, issue-or-reuse match, no match and throw, disposed issue, live and stale lookup, disposed lookup and both dispose phases; null matrix adds entry kind, match predicate, dispose and comparators.
- **RD-R1-006 — Addressed.** SR-014 keeps only the read handle value; issuance on every call is in read-pipeline SR-003 and read-symbols SR-008, with AC-030 and AC-008 extended.
- **RD-R1-007 — Addressed.** SR-021 and AC-018 allow only java.* and read domain types, forbid java.lang.reflect, and discover a nonempty complete class set; adapter layering moved to read-pipeline SR-028 and AC-036.
- **RD-R1-008 — Addressed.** SR-022 keeps deep immutability and value equality, AC-023 adds equality and unmodifiable collections; IDE-state independence moved to read-references SR-027 and AC-019.
- **RD-R1-009 — Addressed.** SR-023 is the platform-free dispose contract with release order and exception rule; new AC-035 proves it; smart pointer release moved to read-symbols SR-011 and AC-011; Effect cancellation stays in read-pipeline SR-019.
- **RD-R1-010 — Addressed.** SR-024 keeps comparators; the equal-outcome rule moved to read-pipeline SR-015 (AC-030); fresh page tokens are asserted in read-text-search AC-016 and fresh read handles in read-symbols AC-008.

Address status: COMPLETE
Plan status: READY_FOR_IMPLEMENTATION_REVIEW
Final gate required: TARGETED_REVIEW
Blocking findings remaining: 0
Deferred findings: 0

Next permitted action at this basis: fresh exact-digest specification review by a separate reviewer (`workflow.md` SPEC_VALID gate 5 overrides the skill's targeted gate).
