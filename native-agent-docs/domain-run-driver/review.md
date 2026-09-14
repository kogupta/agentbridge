# Domain and run driver review ledger

Append-only. Review rounds are tied to exact content digests. Acceptance targets remain PLANNED until S3.

## Current state

- Workflow state: S3 CLOSED — Round 12 targeted gate FINAL_GATE_PASS (Round 13); closure commit on `native-agent-workflow`
- Latest round: 13
- Spec digest: `3c55cfb664920e0c6105dfdfb98744b0bc6aacc4d6e19af512b802fab313031d` (unchanged)
- Design digest: `031f8436e88d06dd70fc696e03cc72afedb5da7fd52fca6e3b25ff101cdb1ae3` (unchanged)
- Product digest at spec basis: `d095eab1a03cdebaa5ba35d23103f293b9cc3cffab5461df8311812ac82b787d`
- S2 receipt digest: `772f701e2225e3d003a600f4037901d493aef599d236a5bc8f3557bdcdce928a` (re-emitted after the IR-009 generator fix; `s2_receipt.py verify` PASS). Receipt `5667013c7450930729c944a0d2613f78f46e17c3bd8f1694ac81e92acddd8094` is superseded; its declaration hashes are identical.
- Open Blockers: 0
- Open Majors: 0
- Deferred Minors/Nits: 2 (IR-005, IR-006)
- Implementation reviewer selection: external separate tool; independent peer: none (Round 9 ledger)
- Next permitted action: none for this feature; later features start from their own specification gate

## Round 1 — full specification review

- Reviewer: independent `reviewer` agent, `DomainSpecReviewer`
- Spec digest: `861ca9569807f43120da30e088f0df121e3c867826280b141c82733d621d9ee9`
- Mechanical check: PASS, zero findings
- Checker regression selftest: PASS, 24/24
- Verdict: SPEC_INVALID

### Findings

- **R1-B-001 — Addressed.** DR-011 did not determine behavior for `Retry-After` above ten seconds. Product item 13 and DR-011 now state that negative or above-cap values are not clamped or automatically waited and produce a terminal rate-limit outcome retaining the requested wait when available.
- **R1-B-002 — Addressed.** DR-005 did not determine whether prevalidated rejection calls entered the lifecycle batch. DR-005 and its audit row now require every accepted call in the batch and settle rejection calls through a synchronous lifecycle accounting callback that invokes no tool.
- **R1-m-001 — Addressed.** DR-010 now enumerates transient transport/EOF, eligible 429, and 5xx retry categories and the non-retryable categories.
- **R1-m-002 — Addressed.** The operation matrix now has `ownedResourcesSettled` from `STOPPING` to `IDLE` when no admitted call remains.
- **R1-n-001 — Addressed.** An assumption maps domain result/state names to frozen lifecycle `CANCELLED_BEFORE_START` and `CLOSED`, and distinguishes the pre-admission run-limit outcome.

Next permitted action at this basis: revise and obtain a fresh exact-digest review.

## Round 2 — targeted final specification review

- Reviewer: independent fresh `reviewer` agent, `DomainSpecFinal`
- Spec digest: `40fff0150bb2bd0d063998725bd40cd24c5705765b24cc7cc4c8c3a4b5b6aae9`
- Product digest: `58ac3058ba9b463cc88ed3a1900aec97be6e8219c22c6095f6575fd6372c7e58`
- Mechanical command: `python3 scripts/native-spec/check_slice.py check --spec native-agent-docs/domain-run-driver/spec.json --planned-targets`
- Mechanical result: PASS, zero findings
- Coverage: DR-001 through DR-016, AC-001 through AC-014, all audit rows, operation-matrix rows, null boundaries, assumptions, non-goals, evidence, and S1–S3 bindings
- Prior findings: all resolved
- Open Blockers: 0
- Open Majors: 0
- Verdict: SPEC_VALID

### Deferred finding

- **R2-N-001 — Design binding required.** DR-012 counts tool invocations. A prevalidated rejection crosses lifecycle accounting but invokes no tool. The design must bind the counter to actual executable-tool admission, not accounting-only rejection settlement.

Next permitted action: S2 design and frozen declarations.

## Round 3 — full design review

- Reviewer: independent `reviewer` agent, `DomainDesignReviewer`
- Spec digest: `40fff0150bb2bd0d063998725bd40cd24c5705765b24cc7cc4c8c3a4b5b6aae9`
- Design digest: `8420d46858981eccc73e46dd29bb2c3d8c8bf182d27135cfb1ce0087b7e5fd48`
- Local declaration compile: Java 21 PASS; Kotlin JVM 21 PASS
- Verdict: DESIGN_INVALID

### Findings

- **R3-B-001 — Addressed.** Stop after admission attempted to append later cancellation results before the executing call's result. Cancellation results now remain pending in lifecycle state until the earlier admitted result is appended, then drain in source order.
- **R3-M-001 — Addressed.** Driver Stop cancelled its child from a stale REQUESTING observation. Stop now uses only the synchronized session/lifecycle boundary and never cancels the child.
- **R3-M-002 — Addressed.** Terminal provider failures could exit while still REQUESTING. Every terminal failure now uses `stopIfCurrent` and `finish`.
- **R3-M-003 — Addressed.** Public Stop could race completed cleanup and throw on a stale run. `stopIfCurrent` returns a race-tolerant result.
- **R3-M-004 — Addressed.** A stale atomic driver holder could reject a valid newly admitted run. `RunSession.start` is the sole authority and the holder replacement cannot let the old completion clear the new holder.
- **R3-M-005 — Addressed.** Design text described a pre-admission `Limited` step absent from source. The design now binds the check inside lifecycle accounting immediately before the tool effect.
- **R3-m-001 — Addressed.** Rejected accounting admission now stops instead of repeating the same call.
- **R3-m-002 — Addressed.** Stop records lifecycle state before aggregating cancellation-listener and resource-close failures.
- **R3-m-003 — Addressed.** Parent cancellation now has a scope-owned watcher; the single already-owned tool execution and result capture use `NonCancellable`.

Next permitted action at this basis: correct findings and obtain a fresh exact-basis review.

## Round 4 — corrected design review

- Reviewer: independent fresh `reviewer` agent, `DomainDesignFinal`
- Design digest: `8658a0d76178d3b274ec272f5e764c679fb232782a48d68c8297d3b015e1eaf7`
- Prior findings: all resolved
- Open Blockers: 0
- Open Majors: 0
- Verdict: semantic PASS; two Minor corrections required before freeze

### Findings

- **R4-m-001 — Addressed.** Active disposal now aggregates cancellation-listener and resource-close failures after lifecycle close and phase transition.
- **R4-m-002 — Addressed.** The declaration table now names `RunLimits.Budget` and `RunTimeSource`; the Kotlin surface lists the injected `RetryPolicy`.

## Round 5 — targeted design freeze review

- Reviewer: independent fresh `reviewer` agent, `DomainDesignFreeze`
- Spec digest: `40fff0150bb2bd0d063998725bd40cd24c5705765b24cc7cc4c8c3a4b5b6aae9`
- Design digest: `8d0c1fac8faa133526ba30e8fb028f343d2012f413b17ebbc74a3ba3e36a124b`
- `RunSession.java` digest: `ee5d45fddfa8bc142f814fac734009044911786e76dad45c759cc10b579f69d3`
- Local Java 21 compile: PASS
- Local Kotlin JVM 21 compile: PASS
- Prior findings: all resolved
- Open Blockers: 0
- Open Majors: 0
- Semantic verdict: PASS
- Remaining gate: real `./gradlew :plugin-core:classes` result and exact-basis receipt

Next permitted action: obtain external Gradle compile evidence; do not mark DESIGN_VALID before it passes.

## Round 6 — external compile and design freeze

- User-executed command: `./gradlew :plugin-core:classes`
- Result: BUILD SUCCESSFUL in 1s; `:plugin-core:compileKotlin`, `:plugin-core:compileJava`, and `:plugin-core:classes` completed
- Preserved evidence: `.agent-work/native-agent-docs/domain-run-driver/classes.log`
- S2 receipt: `.agent-work/native-agent-docs/domain-run-driver/s2-receipt.json`
- Receipt digest verification: PASS, `63bded2a1fe0e82313b3e6568601a4cb3ad25852ae845f74e209f67c89973ffa`
- Frozen declarations: 13 files under the allowed Java and Kotlin run packages
- Open Blockers: 0
- Open Majors: 0
- Verdict: DESIGN_VALID

Next permitted action: S3 bounded implementation and qualification.

## Round 7 — reopened specification review (prompt cache)

The product authority added decision `PROMPT_CACHE`, invariant I13, the Prompt-cache stability rules/runtime model/diagnostic/telemetry/tests, and Milestone 1 cache requirements (`CacheGeneration`, `CacheReset`, dispatch-time prefix checking, prefix tests, and a greater-than-90-percent structural-reuse benchmark). The frozen specification was reopened and extended with DR-017–DR-020 and AC-015–AC-018.

- Reviewer: independent `reviewer` agent, `PromptCacheSpecReview`
- Product digest at review: `d095eab1a03cdebaa5ba35d23103f293b9cc3cffab5461df8311812ac82b787d`
- Mechanical command: `python3 scripts/native-spec/check_slice.py check --spec native-agent-docs/domain-run-driver/spec.json --planned-targets`
- Mechanical result: PASS, zero findings
- Initial verdict: PASS with four nonblocking clarifications
- Clarifications applied:
  - a prefix violation settles the run to `IDLE` before the exception propagates;
  - the violation diagnostic includes the affected field;
  - `ModelChange` labels a replacement session rather than an in-place reset;
  - the benchmark is pinned to 20 dispatches with an explicit aggregate formula (total common-prefix bytes divided by total previous-request bytes, first two dispatches excluded) and named path coverage.
- Targeted re-review on spec digest `3c55cfb664920e0c6105dfdfb98744b0bc6aacc4d6e19af512b802fab313031d`: PASS, no new blockers
- Verdict: SPEC_VALID on the cache-expanded specification basis

## Round 8 — full prompt-cache design review

- Reviewer: independent `reviewer` agent, `PromptCacheDesignReview`
- Frozen inputs: spec `3c55cfb664920e0c6105dfdfb98744b0bc6aacc4d6e19af512b802fab313031d`, design `12aca9b6e2f3d42333a56638913c648829bb31fa83415789ec9858a218daa131`
- New declarations: `CacheField`, `CacheRequest`, `CachePrefixViolation`, `CachePrefixGuard`, `CacheGeneration`, `ModelItemRenderer`; `ProviderRequest` extended with `HistorySnapshot` and `CacheRequest`; `NativeRunDriver` constructor extended with `CacheGeneration` and `ModelItemRenderer`
- Direct compile: Java 21 PASS; Kotlin JVM 21 PASS
- Prior findings: all resolved
- Open Blockers: 0
- Open Majors: 0
- Verdict: DESIGN_VALID PASS with two nits

### Findings

- **R8-n-001 — Addressed.** The design described the guard success result as an observation tuple with ratio; the source exposes `structuralReuse()` as a derived method. The design wording is aligned to the immutable byte-count observation with the derived ratio accessor.
- **R8-n-002 — Addressed.** `CacheGeneration.requestFor` rebuilt the complete `CacheRequest` from all parts after each append — correct but quadratic across a long session and contrary to the design's byte-extension statement. `CacheRequest.extend` now extends the previous byte array and component ranges with only newly appended parts.

## Round 9 — targeted design gate after nit correction

- Reviewer: independent fresh `reviewer` agent, `PromptCacheDesignTargeted`
- Digests verified at review: design `031f8436e88d06dd70fc696e03cc72afedb5da7fd52fca6e3b25ff101cdb1ae3`, `CacheRequest.java` `7241db268b421209637b538634f6d77330e31eb3fa884a52e9da3710cc7cac08`, `CacheGeneration.java` `f8b2676d2ded5eb61f73d3d3156295e1142a9cfdff7686404f13fa1a8ad5c06b`, spec `3c55cfb664920e0c6105dfdfb98744b0bc6aacc4d6e19af512b802fab313031d`
- R8-n-001: RESOLVED (design wording matches `CachePrefixGuard.Observation` with derived `structuralReuse()`)
- R8-n-002: RESOLVED (`CacheRequest.extend` reuses the previous aggregate bytes and component ranges; retry identity retained; deep immutability and null boundaries verified)
- New findings: one Nit, `TG-1-Nit` — a renderer contract violation for a later message inside one append batch leaves earlier same-batch messages admitted while `current` lags their bytes. Assessed as an unreachable divergence (extend cannot break the prefix; the violation propagates through the driver's terminal invariant-failure path) and as pre-existing reviewed incremental-admission behavior, not introduced by the correction. Accepted as nonblocking.
- Open Blockers: 0
- Open Majors: 0
- Verdict: PASS; design basis `031f8436e88d06dd70fc696e03cc72afedb5da7fd52fca6e3b25ff101cdb1ae3` cleared for refreeze

Agent-executable verification on the corrected basis (log: `.agent-work/native-agent-docs/domain-run-driver/rebuild-and-test.log`):

- `javac --release 21` over the lifecycle and run packages: PASS
- `kotlinc -jvm-target 21` on JDK 21: PASS
- Java tests compile: PASS; Kotlin tests compile with `-Xfriend-paths`: PASS
- Disposable focused launcher (whole run package): 22 tests found, 22 started, 22 successful, 0 failed (RunDomainTest 6, NativeRunDriverTest 10, DomainArchitectureTest 1, PromptCacheTest 4, coroutine smoke 1)
- Separately selected coroutine smoke `driverLifetimeIsBoundToParentScope`: 1 found, 1 successful, 0 failed
- `check_slice.py check --spec … --test-dir <java run tests> --test-dir <kotlin run tests>` with real target enforcement: PASS, zero findings

Remaining gate for the cache basis: user-executed `./gradlew :plugin-core:classes` and a reissued exact-basis S2 receipt.

### Implementation-review ledger

- Primary implementation reviewer: external separate tool; frozen prompt at `.agent-work/native-agent-docs/domain-run-driver/implementation-review-prompt.md`
- Independent peer: none (user selection)
- No agent-side implementation-review peer will be spawned.

## Round 10 — external qualification and S2 refreeze on the cache basis

User-executed Gradle evidence on the exact corrected basis (design `031f8436…`, spec `3c55cfb…`; logs under `.agent-work/native-agent-docs/domain-run-driver/`):

- `./gradlew :plugin-core:classes`: BUILD SUCCESSFUL in 32s; `compileKotlin`, `compileJava`, `classes` executed fresh (`classes-gradle.log`)
- Focused tests `RunDomainTest` + `NativeRunDriverTest` + `DomainArchitectureTest`: 17 found, 17 PASSED, 0 failed (`focused-gradle.log`; EV-FOCUSED)
- Prompt-cache gate `PromptCacheTest`: 4 found, 4 PASSED, 0 failed, including the pinned 20-dispatch benchmark (`cache-gradle.log`; EV-PREFIX)
- Separately selected coroutine smoke `driverLifetimeIsBoundToParentScope`: 1 found, 1 PASSED, 0 failed (`smoke-gradle.log`; EV-SMOKE)
- `./gradlew :plugin-core:build :plugin-core:buildPlugin`: BUILD SUCCESSFUL in 6s; full module suite 35 found, 35 PASSED, 0 failed — 13 frozen lifecycle regression tests plus the 22 run-package tests (`build-gradle.log`; EV-BUILD)

S2 receipt reissued for the 19-declaration cache-expanded basis: digest `6f8f1581334367c7b5620d0ef03e2f1113a00e2ab8ed218170e8060a0b7863dc`, `s2_receipt.py verify` PASS. The Round 6 receipt (`63bded2a…`, 13 declarations) is superseded.

Disposable launchers removed: permanent Gradle test runs now cover every test the disposable launchers executed (17 focused + 4 cache + 1 smoke = 22).

- Verdict: DESIGN_VALID refrozen on the cache-expanded basis; S3 exit evidence satisfied for EV-FOCUSED, EV-PREFIX, EV-SMOKE, EV-BUILD
- Remaining S3 evidence: EV-IMPLEMENTATION-REVIEW (external separate tool, prompt frozen at `.agent-work/native-agent-docs/domain-run-driver/implementation-review-prompt.md`)

Next permitted action: external implementation review, findings resolution with one targeted gate if Blockers/Majors appear, then closure commit with the configured identity.

## Round 11 — external implementation review and address pass

External reviewer (separate tool) on the frozen prompt basis: basis verified, verdict FAIL with three Majors, two Minors, and one Nit.

### Findings

- [x] **IR-001 — Major, DR-018.** `CacheGeneration.requestFor` added tail messages to `accepted` before the full tail rendered. A renderer failure on a later tail item silently dropped the earlier items' bytes from every later request in the generation.
- [x] **IR-002 — Major, DR-015.** `NativeRunDriver.submit` into an already cancelled scope started the session run, but the child body never ran. The session stayed `REQUESTING`, `stop` returned `NoActiveRun`, and every later Send returned `BUSY`.
- [x] **IR-003 — Major, AC-017.** The AC-017 target tested only a HEAD divergence at guard level. No test covered an INPUT item index diagnostic or the driver path (no transport, run settles `IDLE`, exception propagates).
- [x] **IR-004 — Minor, AC-016/AC-018.** AC-016 test lacked tool round, Stop, truncation, rejected terminal output, and explicit reset. No test counted renderer calls. Prompt witness 16 overstated what the tests detect.
- [x] **IR-005 — Minor, DR-017.** `CacheGeneration.replacement` throws instead of returning the operation-matrix `Rejected(BUSY)`. The idle check is not atomic with a later start. Nothing binds a generation to a session.
- [x] **IR-006 — Nit.** `check_slice.py` target discovery keys methods by file stem only and accepts `private fun` Kotlin tests.

### Resolution

- **IR-001 — Resolved (class A).** `requestFor` renders the whole tail into a local part list, extends and verifies, and only then adds the tail to `accepted` and replaces `current`. Regression test `PromptCacheTest#rendererFailureAdmitsNoPartialTail`: fails on the pre-fix source (reconstructed digest `f8b2676d…`, `array lengths differ, expected: <4193> but was: <4122>`), passes on the fix.
- **IR-002 — Resolved (class A).** `Active` records whether `drive` started. The job completion handler settles a run whose body never ran through `stopIfCurrent` and `finishStopped`. The user message stays accepted, as for Stop during a request. Regression test `NativeRunDriverTest#submitIntoCancelledScopeSettlesIdle`: fails on the pre-fix source (reconstructed digest `4ca69b66…`, timeout waiting for `IDLE`), passes on the fix.
- **IR-003 — Resolved (class B).** `PromptCacheTest#prefixViolationReportsFirstDivergence` now also asserts an INPUT divergence: component `INPUT`, field `item`, item index 1, offset 6, lengths 7/7, reused 6, and no content in the message. New `NativeRunDriverTest#prefixViolationStopsBeforeTransportAndSettlesIdle` raises a real guard violation during request preparation and asserts zero transport calls, phase `IDLE`, unchanged history, `NoActiveRun`, and propagation of `CachePrefixViolation` to the scope handler. Mutation check: with Stop/finish removed from the driver failure catch, the test fails (`expected: <IDLE> but was: <STOPPING>`). Recorded fact: through `CacheGeneration`, the guard compares `extend(current, tail)` with `current`, so a production divergence is unreachable by construction. The guard stays as the dispatch-time defense required by DR-019. Spec and AC targets are unchanged.
- **IR-004 — Resolved in part (class B), residual deferred.** `dispatchRejectsNonPrefixAndRetryReusesBytes` now drives a `RunSession` through a rejected terminal turn, a length-truncated call round, an executed tool round, and a Stop with cancelled calls. It asserts exact byte extension after each, one render per accepted item, and an explicit reset that compares against its own head and refuses cross-generation comparison. The benchmark also asserts one render per item. Deferred: the AC-018 benchmark still runs at generation level, not through `NativeRunDriver` with a fake provider. Residual risk: a driver that sent bytes other than `cache.requestFor` output would not reduce the measured ratio. Witness 16 is corrected in the re-review prompt.
- **IR-005 — Deferred (Minor).** A sealed `Rejected(BUSY)` result or session binding changes the frozen public `CacheGeneration` surface and the design, and this slice has no production caller of `replacement`. Residual risk: the operation-matrix row `resetGeneration` in an active phase is enforced by exception, and the idle check can race a concurrent start. The session-owner integration feature must decide the result type and session binding.
- **IR-006 — Deferred (Nit).** No duplicate test-class stems exist under `plugin-core/src/test`, and no `@Test` Kotlin method is `private`, so no current target resolves wrongly. The support script is outside the run-package fix scope.

### Changed files (Round 11)

| Path | Digest |
|---|---|
| `plugin-core/src/main/java/com/github/catatafishen/agentbridge/nativeagent/run/CacheGeneration.java` | `032d27f1c45e189701e4221ef7732aafe0b998f4ea44996607893d34102cc481` |
| `plugin-core/src/main/kotlin/com/github/catatafishen/agentbridge/nativeagent/run/NativeRunDriver.kt` | `b86d6a95e43b78691e698508930869d71e84244481e6bec87684d4b32356c001` |
| `plugin-core/src/test/java/com/github/catatafishen/agentbridge/nativeagent/run/PromptCacheTest.java` | `a0f52b2b7484a060b3e6cead242fcc841a7eab76f1d982ae615b8e831b9e8fd5` |
| `plugin-core/src/test/kotlin/com/github/catatafishen/agentbridge/nativeagent/run/NativeRunDriverTest.kt` | `b9b65684e6ecc9c24aec5a1e141379254c5479a9e0200a1456c74baeaf408907` |

### Verification (agent-executable; logs under `.agent-work/native-agent-docs/domain-run-driver/round11/`)

- `compile-and-test.sh`: `javac --release 21` and `kotlinc -jvm-target 21` (JDK 21.0.5-tem) for production and tests PASS; whole run package 25 found, 25 succeeded, 0 failed (`fixed-run.log`)
- Pre-fix reconstruction (`prefix-src`, digests equal to the frozen manifest): 25 found, 23 succeeded, 2 failed — exactly the IR-001 and IR-002 regression tests (`prefix-run.log`)
- Mutant without driver failure settlement: `prefixViolationStopsBeforeTransportAndSettlesIdle` 1 found, 0 succeeded, 1 failed (`mutant-run.log`)
- Separately selected smoke `driverLifetimeIsBoundToParentScope`: 1 found, 1 succeeded (`smoke-run.log`)
- `NativeRunDriverTest` repeated five times: 12/12 each run (`driver-repeat.log`)
- `check_slice.py check --spec … --test-dir <java> --test-dir <kotlin>`: PASS, zero findings (`check-slice.log`)
- `s2_receipt.py verify`: FAIL, `declaration_hashes changed since receipt` (expected; re-emit after user Gradle)
- Trailing-whitespace scan on changed files: clean; `git status --short`: only the pre-existing feature paths
- Not run by the agent (session policy: user-executed Gradle): `:plugin-core:classes`, focused tests, prompt-cache gate, smoke, `:plugin-core:build :plugin-core:buildPlugin`

Address status: COMPLETE
Implementation review status: FINAL_GATE_FAIL
Reviewer checklist: INCOMPLETE
Frozen basis: CURRENT
Open Blockers: 0
Open Majors: 0
Deferred Minors/Nits: 2
Verification: PASS
Final gate: FAIL
Stop reason: targeted gate not yet run; user-executed Gradle evidence and S2 receipt re-emit pending for the corrected declaration hashes
Next permitted action: TARGETED_GATE

## Round 12 — Round 11 targeted gate result and basis correction

Targeted gate (external reviewer; gate basis archived verbatim as `.agent-work/native-agent-docs/domain-run-driver/implementation-review-prompt-round11.md`): basis docs verified; dispositions IR-001 to IR-004 RESOLVED, IR-005/IR-006 deferrals accepted; verdict FINAL_GATE_FAIL on two new Blockers. Full JSON: `round12/gate-round11.json`.

### Findings

- **IR-007 — Blocker, BASIS_GAP.** Four production declarations marked unchanged (`CachePrefixGuard`, `CallAdmission`, `RetryPolicy`, `RunSession`) were modified at 2026-09-15 00:48:10 — after the Round 11 manifest and every agent verification log, before the user Gradle runs and the receipt re-emit — while the gate prompt, this ledger, and the manifest asserted their Round 10 digests. The diff against the `round11/prefix-src` snapshot was a behavior-preserving style sweep (record-pattern `instanceof` conversions in `CallAdmission`/`RunSession`, removal of the redundant `previousBytes < 0` term in `CachePrefixGuard.Observation`, a no-op assignment drop and reformat in `RetryPolicy`), but unmanifested: the agent evidence and the user Gradle/receipt evidence were produced on two different trees. The origin of the 00:48:10 edit is not attributable from the artifacts.
- **IR-008 — Blocker, BASIS_GAP (EVIDENCE_MISSING, s2_receipt row).** The re-emitted receipt's `declaration_hashes` contained the swept digests, contradicting the gate manifest, and EV-DESIGN-SOURCE still bound `rebuild-and-test.log` from the Round 10 tree.

### Resolution

- **IR-007 — Resolved by revert.** The four files were restored byte-exactly from the `round11/prefix-src` snapshot; all four sha256 digests equal the manifest rows and the full 19-file production manifest matches exactly. The swept versions are archived at `round12/rejected-style-sweep/`. A revert restores the frozen surface without reopening the design gate; adopting the sweep instead would enlarge the reviewed diff without an owner decision.
- **IR-008 — Resolved on the agent side.** `rebuild-and-test.sh` was re-run on the corrected tree (disposable launchers recreated under `.agent-work/domain-run-driver-test-launcher/`), regenerating `rebuild-and-test.log` with ALL STEPS PASSED so EV-DESIGN-SOURCE binds the final tree; the Round 10 log is archived at `round12/rebuild-and-test-round10.log`; `check-slice-full.json` regenerated. The S2 receipt must be re-emitted after the user re-runs the Gradle evidence; the current `s2-receipt.json` still binds the swept tree and is STALE.

### Verification (agent-executable; logs under `round12/`)

- `compile-and-test.sh` (javac `--release 21`, kotlinc `-jvm-target 21`, JDK 21.0.5-tem): whole run package 25 found, 25 succeeded, 0 failed (`fixed-run.log`)
- Separately selected smoke `driverLifetimeIsBoundToParentScope`: 1 found, 1 succeeded (`smoke-run.log`)
- `NativeRunDriverTest` five repeated runs: 12/12 each (`driver-repeat.log`)
- `check_slice.py check --spec … --test-dir <java> --test-dir <kotlin>`: PASS, zero findings (`check-slice.log`)
- `rebuild-and-test.sh`: ALL STEPS PASSED — focused launcher 25/25, smoke launcher 1/1 (`rebuild-and-test.log`)
- Not run by the agent (session policy: user-executed Gradle): the five Gradle evidence commands (`round12/gradle-evidence.sh`)

Address status: COMPLETE (agent side)
Frozen basis: CURRENT (19 production declarations equal the manifest; four files reverted to frozen digests)
Open Blockers: 0
Open Majors: 0
Deferred Minors/Nits: 2 (IR-005, IR-006)
Next permitted action: user-executed Gradle (`round12/gradle-evidence.sh`), S2 receipt re-emit and verify, then TARGETED_GATE (Round 12)

### S2 receipt re-emission (after the Round 12 Gradle evidence)

- User-executed Gradle via `round12/gradle-evidence.sh` (logs 2026-09-15 01:27:23 to 01:27:43 +0530, all later than the Round 12 ledger write at 01:17:11): `classes` BUILD SUCCESSFUL in 20s with `compileKotlin`/`compileJava` executed; focused 19/19 (6 + 12 + 1); cache 5/5; smoke 1/1; `build`/`buildPlugin` BUILD SUCCESSFUL with 38/38 (13 lifecycle + 25 run package); zero failures across all five logs
- S2 receipt re-emitted and verified: digest `5667013c7450930729c944a0d2613f78f46e17c3bd8f1694ac81e92acddd8094`, `s2_receipt.py verify` PASS with zero mismatches; `declaration_hashes` equal the 19-file manifest; EV-DESIGN-COMPILE binds the fresh `classes-gradle.log`; EV-DESIGN-SOURCE binds the corrected-tree `rebuild-and-test.log`

## Round 13 — Round 12 targeted gate result and closure

Targeted gate on the Round 12 prompt (`.agent-work/native-agent-docs/domain-run-driver/implementation-review-prompt.md`). Independence note: the gate ran in the session that wrote the Round 11 fixes; it did not write the Round 12 revert.

- Basis: spec, design, `review.md` (`223c83e2…`), and `product.md` digests verified; all 25 manifest paths match; the run packages hold exactly 24 files (19 production, 5 tests)
- Pre-gate evidence: classes, focused 19/19, cache 5/5, smoke 1/1, build 38/38, direct-compile log 25/25 + 1/1, S2 receipt `5667013c…` verify PASS — all PASS, with mtime order restore 01:11:22 < direct-compile log 01:12:35 < ledger 01:17:11 < Gradle 01:27:23–01:27:43 < receipt 01:28:52 < ledger 01:29:17
- Independent check: direct compile and test of the current tree outside the repository, 25 found, 25 succeeded; `check_slice.py check` PASS
- **IR-007 — RESOLVED.** The four files are byte-identical to `round11/prefix-src`; the archived sweep differs only in behavior-preserving record-pattern conversions, an unreachable validation term, compact-constructor self-assignment drops, and one reformat.
- **IR-008 — RESOLVED.** Receipt declaration hashes equal the manifest; EV-DESIGN-COMPILE binds the fresh `classes-gradle.log`; ordering evidence attributes all evidence to the corrected tree.
- Verdict: FINAL_GATE_PASS, one new Minor

### Findings

- [x] **IR-009 — Minor, NEW_EVIDENCE.** `s2_receipt.py` recorded EV-DESIGN-SOURCE with the direct-compile log name only and no content hash, so a replaced log containing `ALL STEPS PASSED` still verified.

### Resolution

- **IR-009 — Resolved.** `build_evidence` adds `log_sha256` of `rebuild-and-test.log` to `EV-DESIGN-SOURCE.direct_compile`. The receipt was re-emitted: digest `772f701e2225e3d003a600f4037901d493aef599d236a5bc8f3557bdcdce928a`, `direct_compile.log_sha256` `e26aadaa183d01dd915ed22b5f7676377bbeb8e6ab01bb417cab0de93bb47c93` equals the log. Declaration hashes are unchanged from `5667013c…`. Regression proof: the previous receipt fails `verify` under the new generator (`evidence EV-DESIGN-SOURCE payload hash changed`); an in-memory substitution of the Round 10 log content (which also contains `ALL STEPS PASSED`) makes `verify` fail with the same mismatch; the clean tree verifies with zero mismatches. The manifest was re-hashed before re-emission: 25 of 25 match.

Address status: COMPLETE
Implementation review status: FINAL_GATE_PASS
Reviewer checklist: COMPLETE
Frozen basis: CURRENT
Open Blockers: 0
Open Majors: 0
Deferred Minors/Nits: 2
Verification: PASS
Final gate: PASS
Stop reason: all Blockers and Majors resolved and gate-verified; IR-009 resolved with regression proof; IR-005 and IR-006 deferred with accepted reasons
Next permitted action: MERGE
