# Semantic reads review ledger

Append-only. Review rounds are tied to exact content digests. Acceptance targets remain PLANNED until S3.

## Current state

- Workflow state: SPEC_INVALID; Round 2 review of revision 1 found 12 Majors; S1 structure check PASS
- Latest round: 2
- Spec digest: `4394865678ea7ebaf7be478eca3836e2b7c6829d41cb9a2bb0dbd88126db25d3`
- Product digest at spec basis: `3ca1c39fe951862e79371b80581bb929df246430dc524ce0015d93e9e6f2624a`
- Open Blockers: none
- Open Majors: 12 (SR-R2-001 through SR-R2-012)
- Open Minors: 11 (SR-R2-013 through SR-R2-023)
- Reviewer selection: Round 2 ran in a fresh-context Claude Code session (Opus 5) from prompt `.agent-work/native-agent-docs/semantic-reads/spec-review-prompt-r2.md`, not in an external separate tool
- Next permitted action: resolve owner decisions for the Round 2 Majors, revise, and obtain a fresh exact-digest review

## Round 0 — specification draft

- Feature: `CAP-SEMANTIC-READS`, roadmap milestone 2
- Mechanical command: `python3 scripts/native-spec/check_slice.py check --spec native-agent-docs/semantic-reads/spec.json --planned-targets`
- Mechanical result: PASS, zero findings
- Owner decisions recorded in `spec.json` decisions (2026-09-15): index wait 30 seconds then `INDEX_NOT_READY`; an edited handle re-resolves and reports current state; page tokens bind to the PSI modification count and return `STALE_PAGE`; `search_text` is literal, case-sensitive, with 500-character excerpts
- Scope decisions (2026-09-15): typed domain and IntelliJ adapters only, JSON codec and LLM envelope excluded; lifecycle S3 block recorded as an assumption; `decisions.json` migration deferred (`workflow.md` Artifacts)
- Reference research: IntelliJ Community and Pi facts from `ij-search` and `pi-search` answers, recorded as agent-cited ranges in EV-SOURCE; not yet reread line by line
- Verdict: none; review pending

## Round 1 — independent specification review

- Reviewer: Claude Code session (Opus 5) that did not author the spec, run from the review prompt
- Spec digest: `6d39c09b696c8637d20cd81e307d3f6000dcda92ffdfd7d7fda47f545c5b7d97`; basis digests for `review.md`, `product.md` and `workflow.md` verified
- Result: `.agent-work/native-agent-docs/semantic-reads/spec-review-r1.json` (`7f74a305fac7aa2c5356c9ac226127d0d505bda48da26a4c2b0716ff555e99c8`)
- Platform facts: 13 checked; 3 WRONG (ReferencesSearch dumb-mode behavior without read access, PSI modification count scope, public bounded smart-mode wait)
- Verdict: SPEC_INVALID

### Findings

- **SR-R1-001 — Blocker, open.** SR-019 leaves the lifecycle disposition of a read cancelled after admission, and the position of the index wait relative to admission, undecided.
- **SR-R1-002 — Blocker, open.** SR-017 page staleness depends on the PSI modification count, which ignores uncommitted edits and changes on dumb-mode transitions.
- **SR-R1-003 — Blocker, open.** SR-016 assumes every index-backed read throws `IndexNotReadyException`; reference search outside read access blocks without a bound.
- **SR-R1-004 — Major, open.** Paging residuals contradict (limit plus one vs sort all); discovery has no collection cap.
- **SR-R1-005 — Major, open.** Path base, library path form and path-outcome precedence are undefined.
- **SR-R1-006 — Major, open.** LIBRARIES scope meaning, generated-source handling and path-suffix matching are undefined.
- **SR-R1-007 — Major, open.** Declaration range, signature, owner and declaration kinds are undefined.
- **SR-R1-008 — Major, open.** Reference-set policy (Javadoc, imports, overriders, non-Java files) is open.
- **SR-R1-009 — Major, open.** read_file defaults, line model and oversize get_symbol_info source are undefined.
- **SR-R1-010 — Major, open.** search_text match, excerpt and explicit-file outcomes are undefined.
- **SR-R1-011 — Major, open.** Registry ownership contradicts the platform-free domain rule; page-token storage is undefined.
- **SR-R1-012 — Major, open.** Global failure precedence and several matrix combinations are missing.
- **SR-R1-013 — Major, open.** A cancelled read computation is not required to settle before the Effect returns.
- **SR-R1-014 — Major, open.** AC-019 cannot fail for a wrong read implementation.
- **SR-R1-015 — Major, open.** AC-014 and AC-015 lack an injectable wait bound, dumb-mode mechanism and observable EDT oracle.
- **SR-R1-016 — Major, open.** Several requirement clauses and the roadmap "incomplete" proof have no direct acceptance.
- **SR-R1-017 — Major, open.** Outcome equality in SR-024 conflicts with fresh read handles and eviction; sort comparison is unstated.
- **SR-R1-018 through SR-R1-023 — Minor, open.** Handle payload vs product, hint rule scope, outline in dumb mode, weak audit residuals, optional-argument nullness, weak AC-002/AC-003 oracles.

Next permitted action at this basis: resolve owner decisions, revise, and obtain a fresh exact-digest review.

## Revision 1 — address Round 1

- Author: Claude Code session (Opus 5), the Round 1 reviewer session; this revision is not a review receipt
- Spec digest before: `6d39c09b696c8637d20cd81e307d3f6000dcda92ffdfd7d7fda47f545c5b7d97`; after: `4394865678ea7ebaf7be478eca3836e2b7c6829d41cb9a2bb0dbd88126db25d3`
- Mechanical command: `python3 scripts/native-spec/check_slice.py check --spec native-agent-docs/semantic-reads/spec.json --planned-targets`; result PASS, zero findings
- Owner decisions (2026-09-15): `read_cancellation` wait and read inside the admitted Effect, cancellation throws, FAILED_AFTER_START; `libraries_scope` project plus attached library sources; `reference_set` every resolved PsiReference including imports, Javadoc and non-Java files, strict method search; `result_collection_cap` 10,000 then TOO_MANY_RESULTS
- Labeled normalizations: assumptions 9–12 (bounds, terms, generated-source scope, fail-closed completeness)

### Dispositions

- **SR-R1-001 — Addressed.** SR-019 places the wait and read inside the Effect, throws on cancellation and records FAILED_AFTER_START; assumption 3 restated; AC-015 asserts the status and outcome.
- **SR-R1-002 — Addressed.** SR-015 runs every computation with documents committed; SR-017 states dumb-mode transitions give STALE_PAGE; AC-016 holds smart mode and adds unsaved-edit cases.
- **SR-R1-003 — Addressed.** SR-016 requires index-backed calls inside the smart-mode read computation and excludes `DumbService.waitForSmartMode(long)`; assumption 5 corrected.
- **SR-R1-004 — Addressed.** SR-002 adds the injected 10,000 collection cap and TOO_MANY_RESULTS; SR-002 and SR-024 audit rows agree; AC-022 added.
- **SR-R1-005 — Addressed.** SR-004 defines the base directory, normalization, check order and rendered path forms; AC-002 extended.
- **SR-R1-006 — Addressed.** SR-005 defines LIBRARIES, generated sources and segment suffix matching; SR-006 and SR-009 state generated handling; AC-004 extended.
- **SR-R1-007 — Addressed.** SR-025 defines range, signature, owner and containing declaration; SR-006 lists kinds and restriction semantics; AC-005, AC-008 and AC-021.
- **SR-R1-008 — Addressed.** SR-009 states the reference set and reference kinds; AC-009 rewritten.
- **SR-R1-009 — Addressed.** SR-003 states defaults, span rejection, line model and byte counting; SR-008 adds SOURCE_TOO_LARGE; AC-001, AC-002 and AC-008.
- **SR-R1-010 — Addressed.** SR-010 states match, excerpt window, recursion, file-scope outcome and skipped-file count; SR-001 bounds queries; AC-010 extended.
- **SR-R1-011 — Addressed.** SR-012 and SR-021 make the registry generic over an adapter payload and store page tokens as bounded entries; SR-011 compares pointers in the adapter.
- **SR-R1-012 — Addressed.** SR-026 adds global precedence; the operation matrix adds admission, path, language, index-loss, content-close and project cases; AC-024 added.
- **SR-R1-013 — Addressed.** SR-019 requires computation termination before the Effect ends and registration only after success; AC-015 asserts it.
- **SR-R1-014 — Addressed.** AC-023 proves SR-022 immutability; AC-019 is labeled a prefix regression with a test renderer.
- **SR-R1-015 — Addressed.** SR-016 injects the wait bound; AC-014 uses 200 ms and the platform dumb-mode utility; AC-015 uses a latch executor and thread assertions.
- **SR-R1-016 — Addressed.** AC-002, AC-016, AC-020, AC-021 and AC-024 cover the missing clauses and the roadmap incomplete proof.
- **SR-R1-017 — Addressed.** SR-024 states comparison, absent-value order and the read-handle exception to equality.
- **SR-R1-018 — Addressed.** SR-011 states the product handle payload; assumption 11 labels handle reuse.
- **SR-R1-019 — Addressed.** SR-020 applies the catalog-only hint rule to every hinted outcome; assumption 9 labels LINE_EXCEEDS_BYTE_CAP.
- **SR-R1-020 — Addressed.** SR-007 and SR-025 compute outlines and signatures without type resolution.
- **SR-R1-021 — Addressed.** SR-014, SR-018 and SR-022 audit rows revised; SR-025 and SR-026 rows added.
- **SR-R1-022 — Addressed.** Null boundary rows state absence through overloads or empty variants.
- **SR-R1-023 — Addressed.** AC-002 has an exact truncation oracle; AC-003 inserts a new unsaved method.

Next permitted action at this basis: fresh exact-digest specification review.

## Round 2 — independent specification review

- Reviewer: fresh-context Claude Code session (Opus 5), run from `.agent-work/native-agent-docs/semantic-reads/spec-review-prompt-r2.md`; IntelliJ and Pi facts came from `ij-search` and `pi-search` answers
- Spec digest: `4394865678ea7ebaf7be478eca3836e2b7c6829d41cb9a2bb0dbd88126db25d3`; basis digests for `review.md`, `product.md` and `workflow.md` verified
- Mechanical command rerun: `python3 scripts/native-spec/check_slice.py check --spec native-agent-docs/semantic-reads/spec.json --planned-targets`; result PASS, zero findings
- Result: `.agent-work/native-agent-docs/semantic-reads/spec-review-r2.json` (`5ed53275093dacbd49807a65d2f313ed87376c60b8ac93b125119f68ef94bf22`)
- Platform facts: 17 checked; 1 WRONG (assumption 7: a smart pointer can restore to a replacement declaration); 2 UNVERIFIED (strict method search semantics, symlinked content root lookup)
- Round 1 dispositions: no Round 1 finding reopened; new findings are in neighboring clauses
- Verdict: SPEC_INVALID

### Findings

- **SR-R2-001 — Major, open.** Mapping of CallAdmission Limited and Rejected to ToolOutcome has no owner; the ADMISSION matrix row is wrong for the frozen driver.
- **SR-R2-002 — Major, open.** A read that completes after registry disposal, without run cancellation, has no defined outcome.
- **SR-R2-003 — Major, open.** A handle or token identifier of the wrong kind has no defined outcome; no typed per-kind key.
- **SR-R2-004 — Major, open.** Page token reuse after use (replayable or single-use) is undefined.
- **SR-R2-005 — Major, open.** Next page during INDEXING with a changed count matches both STALE_PAGE and INDEX_NOT_READY rows.
- **SR-R2-006 — Major, open.** Catalog status, owner and depth of members of anonymous classes, local classes and enum-constant bodies are undefined.
- **SR-R2-007 — Major, open.** Non-type owner handle and restriction-versus-scope or generated-source outcomes are missing.
- **SR-R2-008 — Major, open.** Reference range, excerpt anchor and CODE/IMPORT/DOC classification are undefined.
- **SR-R2-009 — Major, open.** search_text offset base and the outcome for a too-large file scope are undefined.
- **SR-R2-010 — Major, open.** Canonical comparison of roots for symlinked project paths is undefined.
- **SR-R2-011 — Major, open.** AC-009 does not discriminate strict search; IMPORT, non-Java and generated references lack acceptance.
- **SR-R2-012 — Major, open.** LIBRARIES symbol candidates depend on library-source filtering that the spec does not state; no LIBRARIES acceptance for symbols or references.
- **SR-R2-013 through SR-R2-023 — Minor, open.** Page-token mismatch step, signature edge cases, cancellation listener and throwable type, READ_FAILED categories, query validation, write-action restarts, weak oracles, missing matrix rows, audit wording, assumption 7, fallback hint and find_references target identity.

Next permitted action at this basis: resolve owner decisions, revise, and obtain a fresh exact-digest review.
