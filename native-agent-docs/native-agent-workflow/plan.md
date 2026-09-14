# IntelliJ-native agent: constrained implementation workflow

## Status and authority

Planning deliverable only. No production implementation, dependency removal, compiler upgrade, or formal-model verification is performed by this plan-writing session.

This plan designs the workflow requested in `.agent-work/thought-process.md:374-407`. It is not a claim that the native agent's complete feature specification or Java API is already frozen. The executable stages below establish that workflow and qualify it on one real feature. The product delivery sequence at the end is a roadmap whose feature packets must pass these gates before implementation.

Inputs: `.agent-work/thought-process.md`; `.agent-work/native-agent-mvp/{phased-scope,plan,feedback,handoff}.md`; current source; the user's follow-up requesting mostly Java records/sealed interfaces with minimal Kotlin for coroutines.

Preserve the older artifacts. They are evidence and migration inputs, not concurrent authorities for new implementation packets. Stage 1 assigns every relevant old requirement a retained, superseded, excluded, or unresolved disposition. An unresolved decision blocks the affected feature and its dependents, not unrelated research.

Observed branch: `master`, with eight untracked entries and no tracked modifications reported by IDE VCS. Do not change branches or commit during this planning session. Proposed implementation branch: `native-agent-workflow`. Create it through IDE VCS before implementing the workflow. Preserve unrelated work. Current source hashes are recorded in `source-manifest.json` beside this plan; historical hashes are not reused.

## Issue and current behavior

The existing documents leave conflicting choices available to an implementation model:

- The old scope includes fifteen tools and `run_command`; feedback defers the command escape hatch (`phased-scope.md:79-96`; `feedback.md:184-224`).
- The old handoff says "reviewed MVP" while separately requiring fresh review (`handoff.md:5-23`). It is not approval.
- The old design puts parsed JSON and generic status/code results in the domain; feedback rejects that boundary (`plan.md:96-110`; `feedback.md:473-513`).
- Scope says Java 21; feedback prefers Kotlin everywhere in new native-agent code (`phased-scope.md:58-63`; `feedback.md:228-238`). The current user selects a narrower mixed-language direction.
- Session ownership and provider sequencing require the explicit reconciliations in `feedback.md:48-73`.

Current source also limits what can be claimed:

| Evidence | Observed fact | Design consequence |
|---|---|---|
| `settings.gradle.kts:1-7` | Five included subprojects, not the three in older project prose. | Derive module inventory; do not hard-code an old module census. |
| `gradle/wrapper/gradle-wrapper.properties:3` | Wrapper uses Gradle 9.7.1. | Do not plan from the old Gradle 8.x description. |
| `build.gradle.kts:170-185` | Java source/target 21 and JUnit Platform. | Preserve runtime compatibility; distinguish compiler JVM from target bytecode. |
| `plugin-core/build.gradle.kts:7-25,107-118,508-519` | Mixed Java/Kotlin source root; Jupiter/Vintage; default test task excludes integration tag. | Reuse source roots and actual runners; unit success is not platform integration success. |
| `gradle.properties:34-39` | Configured SDK 2026.1.3; documented minimum runtime 2025.3. | A Java 25-only product is a separate compatibility decision. |
| `plugin-core/src/main/java/com/github/catatafishen/agentbridge/psi/tools/editing/EditingTool.java:52-78,110-147` | Deferred formatting and first/nearest same-name selection. | Do not expose donor resolution as exact identity or donor formatting as completed work. |
| `plugin-core/src/main/java/com/github/catatafishen/agentbridge/psi/tools/editing/ReplaceSymbolBodyTool.java:85-143` | EDT work is queued; line-based replacement; caller waits 15 seconds. | Timeout is not effect cancellation. Native admission must be checked at actual effect start. |
| Pi `packages/agent/src/agent-loop.ts:226-240,379-403` | Truncated calls become errors, never execute; loop may continue. | Preserve non-execution; explicitly specify native end-run behavior rather than silently inheriting retry. |
| Pi `packages/agent/src/agent-loop.ts:409-485,607-674` | Sequential mode exists; each call is validated immediately before its execution; abort breaks between calls. | Whole-response prevalidation and complete Stop accounting are stronger native decisions, not Pi guarantees. |

Pi root: `/home/muku/depot/personal/cli-tools/pi`. Main located `runLoop` using `codeq pi sym runLoop` and confirmed the cited local ranges. A helper's differently numbered source citations were not used as the frozen basis. A donor research helper failed to start; main performed the cited donor reads. No independent review is claimed.

## Global invariants

W1. Each normative decision has one canonical key and value. Feature files reference shared decisions; they cannot override them.

W2. A feature cannot enter implementation without valid specification and design receipts for the exact frozen inputs. Missing, malformed, stale, or unavailable evidence fails closed.

W3. Every normative behavior has acceptance criteria. Every invariant has an enforcement strategy before SPEC_VALID and a concrete enforcement binding before DESIGN_VALID.

W4. Mechanical checks claim only their supported semantics. Prose review is not proof; a passing model is not a proof of Java/IntelliJ behavior.

W5. Java owns domain representation and transition policy. Kotlin owns coroutine execution and platform coroutine interoperability. JSON, PSI, coroutine types and provider SDK objects do not enter the Java domain.

W6. No workflow stage changes donor behavior or starts donor subtraction. Product mutations require their own gated feature packet and real effect/lifecycle proof.

W7. A frozen surface can change only by reopening its design/spec gate. Implementation agents may choose local algorithms, not new state semantics, public abstractions or dependencies.

W8. DESIGN_VALID requires a representation/operation audit for every invariant: invalid representation or operation → type/API prevention → residual runtime obligation → justification. TEST or RUNTIME alone is not an acceptable binding when a simple type, constructor, ownership or visibility change can eliminate the invalid case.

## Decisions for this plan

| Key | Chosen value / authority |
|---|---|
| SPEC_FORMAT | Strict JSON, one shared decisions file plus one file per feature. Standard-library tooling; no YAML coercions, merge keys or schema-library dependency. |
| VALIDATOR | Small Python 3 standard-library CLI under `scripts/native-spec/`; development-only, not shipped in the plugin. Existing `scripts/issue-fixer.py` establishes that Python tooling is not a new runtime language for the product. |
| FORMAL_TOOL_DEFAULT | None. Use a targeted Quint spike only if effect-admission interleavings remain materially uncertain after explicit state/API design. |
| CORE_LANGUAGE | Java records, sealed interfaces, exhaustive switches and named outcomes; user-requested minimal Kotlin for coroutines. |
| BYTECODE_BASELINE | Java/JVM 21 while retaining IntelliJ 2025.3–2026.1 runtime compatibility. No preview features. |
| NULLNESS_TOOLING | Do not introduce JSpecify/NullAway. Retain existing JetBrains annotations and explicit constructor/boundary validation. No build/source references were found in the searched module sources/build configuration. This is not a transitive-dependency audit. |
| SESSION_OWNER | At most one native session per project; native tool-window content owns it. Content close invalidates handles, requests cancellation and suppresses UI delivery; already-started effects still need accounting. |
| TOOL_EXECUTION | Sequential. Validate complete wire structure and all call schemas before any tool effect; mutable-world preconditions remain per-operation checks. |
| COMMAND_ESCAPE_HATCH | Excluded from initial semantic MVP; re-entry requires an observed dogfood task that native operations cannot complete. No unused command API. |
| PROVIDER_SEQUENCING | Provider work may proceed after shared contracts freeze; live integration qualification waits for real native tools. |
| DONOR_SUBTRACTION | Only after explicit dogfood Go; before persistence or provider expansion. |

These are proposed workflow/product decisions, not assertions of implemented behavior. Stage 1 moves them into canonical JSON and thereafter this explanatory table is non-normative, replaced by references rather than maintained as a second decision registry.

### Java core with a small Kotlin coroutine layer

Use Java for IDs, accepted messages, typed tool arguments, operation outcomes, provider DTOs/codecs where practical, pure transition policy, PSI operation helpers, and ordinary Swing UI. Use Kotlin where suspend calls and structured lifetime ownership actually simplify execution: the run driver, service/content scope wiring, cancellable waits and platform dispatch.

The Java core computes a domain-specific next action from validated events/state. The Kotlin driver awaits the requested operation and feeds its named result back. Keep the next-action set closed and specific to this loop; it is not a generic effects interpreter or reusable orchestration framework. Do not encode the transition rules again in Kotlin.

Java-facing UI entry points are ordinary start/stop/dispose methods and immutable event delivery. Java does not call raw suspend methods or manipulate Continuation. Do not convert every method into CompletableFuture merely to bridge languages. Use a future bridge only at a genuine async JDK boundary such as HttpClient, with explicit cancellation and resource-close handling.

An IntelliJ-injected service scope owns child content/run work. Content close cancels only that child's work, not the whole project service scope. Scope cancellation alone does not suppress an already queued Java EDT runnable: the admission boundary remains mandatory. Do not use GlobalScope, block EDT with runBlocking, catch-and-swallow cancellation, or bundle another coroutines runtime. Exact supported dispatch/read/write APIs and PlatformApiCompat wrappers are verified during feature design.

Java records are not deep immutability or non-null proofs. Validate components and take immutable snapshots at ownership boundaries. Public records cannot hide public canonical construction; proof-bearing values that require restricted construction should be final classes with controlled factories, or package-private records where sufficient. Java references are not affine capabilities: a token's single-use/admission semantics require runtime synchronization, not a "consumed" type name. A symbol with zero references is a valid rename candidate; do not copy feedback's illustrative nonempty-usage-list constraint.

Java 25 is not blocked by nullness tools. [JetBrains' platform table](https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html#platformVersions) lists 2025.3/2026.1 on Java 21 and 2026.2 on Java 25. A newer build JDK with an older release target differs from using Java 25 source/API/bytecode. Do not raise the minimum IDE version merely to modernize the compiler. A 2026.2+-only decision would reopen the compatibility feature and build/verifier matrix. Coroutines do not require that upgrade.

## Normalized artifacts

During design, keep artifacts under `.agent-work/native-agent-workflow/`. Once approved for implementation, promote canonical inputs to tracked `spec/native-agent/`; keep generated packets/reports under `.agent-work/`. Ignored-only specifications cannot serve as durable CI input.

Maintain only four categories:

1. `decisions.json`: format version; shared decision definitions with stable key, primitive type, allowed values/range, selected value, rationale and evidence IDs; shared invariant definitions; evidence records. No feature-level override or implicit default.
2. `features/<id>.json`: identity, observable contracts, shared references, feature-local invariants, optional state/event table, dependencies, non-goals and acceptance scenarios.
3. `design/<id>.json`: links from requirements to enforcement targets; allowed files/packages; frozen public surface references; exact validation commands/scenarios. It references code rather than repeating field/method signatures in prose.
4. Gate receipts and generated implementation packets: disposable outputs tied to content hashes, not editable claims of completion.

Feature fields:

| Field | Required content |
|---|---|
| `id`, `title` | Stable ID and short descriptive title; filenames match IDs. |
| `evidence` | IDs resolving to repository + path + source symbol/range + hash, or requirement-origin evidence. Record reference behavior versus intentional deviation. |
| `depends_on` | Feature IDs; directed acyclic feature dependency graph. Runtime state loops are not dependency cycles. |
| `decision_refs` | Shared keys only. Local decisions are defined once under their feature and cannot shadow global keys. |
| `contracts` | Stable behavior ID, trigger/input, externally observable output, ordered failure/precedence cases and acceptance IDs. No prose spread across multiple authoritative sections. |
| `invariants` | Stable ID, canonical statement or supported structured constraint, assumptions, enforcement categories and acceptance references. Shared invariants referenced, never copied. |
| `machine` | Optional initial state, states, terminal/quiescent classifications, events, unique state/event/case outcomes and explicit rejected/ignored events. Each case names its disjoint input condition. |
| `acceptance` | Stable ID, given/when/then, negative case where meaningful, automated/manual mode, observable oracle. No successful status without actual evidence. |
| `non_goals` | Concrete exclusions; re-entry condition where useful. |
| `open_decisions` | Explicit entries during draft; must be empty for SPEC_VALID. |

Inputs/outputs at spec time describe domain concepts, not a second copy of Java DTO fields or JSON schemas. Design binds them to actual declarations. Derive tool schemas from the boundary's chosen definition where practical; do not require a generic schema/code generator.

### Representative feature, not a frozen API

`CAP-EFFECT-ADMISSION` references the session, sequential-execution and effect-accounting decisions. Its boundary is effect start, not request scheduling.

Required acceptance scenarios:

- A queued write loses to Stop: no mutation occurs even if its EDT runnable later executes.
- A write wins admission before Stop: retain its real completed/failed-after-start outcome; no following effect starts; Send remains disabled until settlement.
- Content closes while a callback is queued: no UI update reaches disposed content; accounting still records an already-started effect.
- New Session cannot begin while the previous run is stopping.

No unconditional "Stop always finishes" invariant. If a noninterruptible platform operation never returns, the agent cannot truthfully report that it settled. Liveness assumes eventual operation completion and eventual scheduling of cleanup. Safety must hold without those progress assumptions.

## What spec-lint mechanically checks

One CLI, fixed project-specific rules; no plugin system, expression evaluator, solver embedding or repair mode. Proposed commands are future tooling, not commands that exist today:

- `python3 scripts/native-spec/spec_lint.py spec --root spec/native-agent --feature CAP-EFFECT-ADMISSION`
- `python3 scripts/native-spec/spec_lint.py design --root spec/native-agent --feature CAP-EFFECT-ADMISSION`
- `python3 scripts/native-spec/spec_lint.py packet --root spec/native-agent --feature CAP-EFFECT-ADMISSION --out .agent-work/native-agent-workflow/packet.json`

All commands emit machine-readable diagnostics with rule code, feature/JSON pointer, related IDs and message. Sort findings deterministically. Exit 0 only on the requested gate's success, 1 for invalid inputs/findings, 2 for tool/environment failure. Neither nonzero result advances a gate. Reject unknown format versions/fields and malformed review receipts; never silently ignore them.

Each command gates the selected feature and its transitive dependencies. Parse all files and check global ID/key uniqueness and reference integrity so a draft cannot shadow an approved definition. Apply readiness requirements (resolved decisions, complete acceptance/enforcement, review receipts) only to the selected closure. Unrelated future-feature drafts do not block the pilot. The structural-only report available in Stage 2 is not a SPEC_VALID receipt; Stage 3 adds receipt eligibility.

Mechanical rules:

- Strict parsing, including duplicate JSON object-key rejection before dictionary construction; reject non-finite numbers. Check field types, required values and IDs. Do not use a parser's last-value-wins behavior.
- Global ID uniqueness across all definitions; no duplicate decision keys even if values match; typed allowed values/ranges and no local shadowing.
- Resolve every feature, decision, invariant, evidence, transition and acceptance reference. Missing dependencies or evidence fail.
- Reject feature dependency cycles. Check declared enforcement categories against TYPE/API/ARCHITECTURE/TEST/RUNTIME/MODEL-CHECKER.
- Every behavior requires acceptance; every invariant requires an enforcement strategy. Every reference has an owner. Non-goals do not need artificial tests.
- For finite tables: known states/events, one initial state, unambiguous row keys, coverage of declared event/case combinations, no unintended outgoing terminal transitions, structural reachability from initial state, and a path to declared quiescent/terminal states where required. Explicit ignore/reject rows are outcomes, not missing transitions.
- Direct structured contradiction checks only: incompatible required values for the same decision, empty allowed-value intersection, inconsistent min/max, identical state/event/case with competing outcomes. Support only these finite forms. No ad hoc predicate strings claimed as checked logic.
- Reject unresolved decision entries and placeholder-only normative fields. Scan normative strings for explicit TODO/TBD markers, not quoted historical source. Hidden ambiguity in ordinary prose belongs to review.
- Report validation coverage: structural checks versus semantic-review obligations. An abstract graph path does not prove its guards are satisfiable or that every execution terminates. Unchecked guard overlap, arithmetic, concurrency and liveness cannot receive a mechanical pass label.

Use strict Python validation functions as the single schema implementation initially. Do not maintain an independent handwritten JSON Schema in addition. A schema export is optional only if generated from that same definition and needed by an editor.

## Semantic reviewer contract

A fresh-context reviewer receives the exact feature dependency closure, shared decisions, relevant reference excerpts and mechanical report. It reads all normative clauses in that closure, not a sample. Its only job is to identify contradictions, omitted outcomes, overlapping cases, ownership ambiguity, infeasible requirements and spec-to-evidence mismatches.

Each finding contains stable ID, severity (blocking/nonblocking), feature/requirement IDs, cited conflicting locations or missing case, concrete witness/scenario, and the decision needed. Proposed repairs may be separate suggestions; reviewer never modifies the spec or changes authority. Uncertainty about required semantics is blocking. Do not downgrade a blocking finding to get a green receipt.

A receipt records input digest, reviewer identity, reviewed IDs, findings and dispositions. A distinct review must approve the final changed basis; a clean review of an earlier hash cannot be carried forward. Author self-audit is useful but does not satisfy this receipt. A missing reviewer/tool is BLOCKED, not approved by default.

The control flow is deterministic; an LLM's discovery process is not. Claims are limited to "mechanical checks pass and no unresolved blocking findings on this basis", never "the specification is mathematically consistent" unless a precisely scoped formal check actually establishes that claim.

## SPEC_VALID gate

For a feature and its complete dependency closure:

1. Strict parsing and all supported spec-lint rules pass.
2. Every selected normative decision is resolved; no unresolved authority conflict remains.
3. Reference evidence is readable and matches its frozen hash, or its changed source has been re-reviewed and rebased.
4. Every behavior has acceptance criteria; every invariant has an enforcement strategy and explicit assumptions.
5. Semantic review covers this exact closure/digest; zero unresolved blocking findings.
6. Any explicitly required model check has a successful, scope-qualified report; counterexample, timeout, unsupported property or unknown result blocks.

Output: SPEC_VALID receipt, closure digest and coverage report. Existing production symbols are not required yet. A promised future enforcement mechanism is enough here only if its concrete binding is resolved at DESIGN_VALID.

## Invariant-to-enforcement mapping

The names below are design targets, not existing repository symbols.

| Invariant | Primary mechanism | Necessary residual check |
|---|---|---|
| Waiting-for-tools has work | Java state variant holding a validated nonempty batch | Reject empty construction; accepted empty response takes a different transition. |
| Distinct run/call/session IDs | Separate Java records/value classes | Validate source values and uniqueness where identity is allocated/accepted. |
| Incomplete provider output cannot execute | Boundary produces either rejected response or validated complete response; driver never receives streamed argument fragments | Protocol test includes syntactically valid but length-truncated arguments. |
| JSON/PSI/coroutines stay outside domain | Test-scoped ArchUnit dependency rule over compiled native-domain classes | Include signatures and implementation dependencies; compiler alone does not forbid imports. |
| One active provider request | State-specific request admission and serial driver ownership | Controlled overlapping callback/Send scenario; no duplicate request. |
| Stop prevents not-started effects | One synchronized admission/start boundary shared with Stop | Deterministic queued-EDT race regression; a Java token alone is insufficient. |
| Stale inspection cannot authorize edit | Typed inspected identity plus adapter revalidation at write boundary | Change document after inspection; reject without a wrong-target edit. |
| Every accepted call has one terminal result before continuation | Batch ledger keyed by typed CallId; named unstarted cancellation outcome | Duplicate/late completion rejected; Stop accounts for remaining calls and real started outcomes. |
| Expected failures remain actionable | Operation-local sealed outcomes | Invalid input/ambiguity/pending diagnostics are not swallowed as success or generic InternalError. |
| Null cannot enter a constructed core value | Explicit constructor checks, immutable collections, JetBrains annotations | Boundary regression for plausible nullable external input; no whole-program null-safety claim. |

Add ArchUnit only as a test dependency when the native domain exists; it is not a plugin runtime dependency. Freeze exact package rules in that feature's design. A runtime-reflection exclusion needs direct-code/dependency review as well as static checks; do not claim ArchUnit can prove the absence of reflection inside arbitrary third-party internals.

## DESIGN_VALID gate

Input: SPEC_VALID receipt; actual Java/Kotlin declaration sources; feature design bindings; relevant donor signatures/calls; acceptance oracles.

Requirements:

1. Spec receipt and dependency closure remain current.
2. Every behavior/decision/invariant maps to concrete types, method signatures, architecture rules, runtime assertions or acceptance scenarios. Multiple mechanisms can defend one invariant; avoid duplicate normative definitions.
3. Resolve actual symbols through IntelliJ/LSP with overload identity, not regex. Record source file hashes and exact signatures in a generated surface report. A missing symbol is failure, not permission to invent it during implementation.
4. New declarations compile in an isolated design source directory under `.agent-work/` before installation into production. Use real value constructors/interfaces, no fake implementations, throwing method stubs or dummy provider/tool classes. Async interop declarations must also compile against the selected SDK/Kotlin setup; javac alone does not establish Kotlin/IDE compatibility.
5. A fresh reviewer checks both directions: each requirement is enforced sufficiently; each substantial design semantic has a requirement basis. Special scrutiny: constructor visibility, mutable collections, nullable references, capability reuse, cancellation and ownership.
   The audit must cover sum types, state-specific payloads, distinct validated values, constructor authority, legal transitions, argument/result correlation, collection invariants, aliasing/ownership, protocol progression, package visibility and nullness. Record rejected stronger alternatives and why they add complexity without eliminating an actual invalid case. Do not claim that Java references are non-null or affine, that records are deeply immutable, or that inspection evidence proves future external state.
6. Enumerate allowed edit files/packages, frozen public declarations, dependencies, required tests/scenarios and commands. No unresolved selection of JSON stack, executor ownership or platform API in an implementation packet.
7. Current focused compilation and design review evidence exist. Behavioral tests not implementable before the production behavior exists are explicitly PLANNED, not PASSED. They must run before DONE.

Output: DESIGN_VALID receipt over spec digest, design declarations, enforcement binding and environment/build configuration. Declarations move into production once during the implementation slice; delete design copies after the move and rebind to production paths. Do not keep two sets of Java interfaces in sync indefinitely.

The Python checker verifies receipt structure, coverage and hashes. It does not implement a Java parser or claim that a referenced test proves an invariant. Symbol resolution/compilation/review provide those separate pieces of evidence.

## Frozen implementation packet

Generate, do not author a second prose handoff. Include:

- Feature ID, target observable change, spec/design digests and approved gate receipts.
- Canonical decisions/invariants/acceptance cases for the transitive feature closure, included once each.
- Actual frozen Java/Kotlin declarations and the signatures of directly referenced dependencies.
- Required reference excerpts, intentional Pi deviations and the donor helpers to reuse or explicitly avoid.
- Allowed edits, new files approved by design, forbidden dependencies, frozen signatures and exact targeted verification commands/manual scenarios.
- Relevant implementation bodies and tests needed for the task; do not recursively embed every library implementation or the repository.
- Stop rule: missing dependency, semantic ambiguity, incompatible API or needed surface change yields a structured blocker and returns to the appropriate gate. No guessing.

Derive code closure using IDE symbol references/implementations plus build dependencies. A short human-curated list of dynamic/platform edges is allowed when indexing cannot establish them; mark why it is needed. Capture current editor content through read_batch; reconcile unsaved edits before producing a disk-hash freeze. Missing/partial index results fail packet completion where semantic identity matters.

Packet output is reproducible for identical inputs: stable ordering, no volatile timestamps in its digest. Recompute inputs at consumption time. Recheck frozen surface and allowed diff before verification/commit; ordinary implementation-body changes are allowed, public-surface changes are not. Reports may have timestamps outside the identity payload.

Keep a local immutable before-snapshot for allowed-diff comparison; do not build a durable graph database. Compiler/extracted signature checks catch surface drift. Do not trust a whole-file hash to distinguish an allowed body change from a signature change.

## Workflow states and deterministic failure routing

| Current state/event | Next state | Required action |
|---|---|---|
| New/revised requirement | SPEC_DRAFT | Normalize decisions and affected dependency closure. |
| SPEC_DRAFT + invalid structure/contradiction | SPEC_INVALID | Report exact rules/findings; no design generation. |
| SPEC_INVALID + explicit revision | SPEC_DRAFT | Re-run spec checks/review on new basis. |
| SPEC_DRAFT + SPEC_VALID gate passes | SPEC_VALID | Freeze receipt; permit design. |
| SPEC_VALID + begin design | DESIGN_DRAFT | Create declarations and enforcement bindings. |
| DESIGN_DRAFT + missing/insufficient enforcement | DESIGN_INVALID | Repair design only if spec remains unchanged. |
| DESIGN_INVALID + explicit revision | DESIGN_DRAFT | Recompile/review changed design closure. |
| DESIGN_DRAFT + DESIGN_VALID gate passes | DESIGN_VALID | Freeze surface; packet may be emitted. |
| DESIGN_VALID + matching packet consumed | IMPLEMENTING | Bounded edits only. |
| IMPLEMENTING + required spec policy missing | SPEC_INVALID | Invalidate affected design/packet; do not choose policy. |
| IMPLEMENTING + required API/surface change | DESIGN_INVALID | Keep spec if unchanged; re-design/review affected closure. |
| IMPLEMENTING + bounded change ready | VERIFYING | Execute contract tests, architecture checks, build and actual-surface smoke. |
| VERIFYING + implementation defect | IMPLEMENTING | Fix within frozen contract and rerun affected evidence. |
| VERIFYING + spec/design defect | SPEC_INVALID / DESIGN_INVALID | Classify by ownership, invalidate downstream receipts. |
| VERIFYING + all required evidence passes | DONE | Record exact evidence and semantic commit through IDE VCS. |
| Any stage + missing environment/reviewer/live access | Same stage, BLOCKED status | Preserve resume point; no successful receipt. Retry only after prerequisite restored. |
| Any successful stage + relevant upstream hash changes | Earliest affected draft stage | Invalidate dependent receipts and re-run gates; unrelated feature receipts remain valid. |

Persist receipts as evidence, not a mutable magic `status=valid` flag. A CLI derives current gate eligibility from inputs and receipts. A crash/partial write must not produce a successful receipt: write result atomically only after completion. No workflow server, queue or orchestration service.

## Quint versus Alloy

Both are viable specification languages. Neither removes the need for feature IDs, evidence, API bindings, review or implementation conformance.

| Choice | Useful here | Limitation / decision |
|---|---|---|
| Strict JSON + fixed lint | Decision consistency, reference integrity, acceptance/enforcement coverage and small state tables | Cannot decide arbitrary predicates or prove concurrent implementation behavior. Default. |
| Quint | Explicit transition actions and invariants for Stop/admission/settlement interleavings | Extra model plus checker toolchain. Prefer for a narrowly justified operational state-machine spike. |
| Alloy 6 | Relational constraints among sessions, handles, calls/results, ownership and dependency rules; also temporal behavior | Not merely a static diagram tool. Small fixed ownership rules are cheaper in Java/lint; use when combinations of relations are the hard problem. |

[Quint documentation](https://quint.sh/docs/model-checkers) distinguishes Apalache's bounded checking (`--max-steps`, default 10) from TLC's explicit finite-state exploration, including temporal properties. [Its overview](https://quint.sh/docs/what-does-quint-do) states temporal support is partial and the simulator does not support temporal properties. Random simulation is not exhaustive checking. Select/pin version and backend before claiming a liveness gate.

[Practical Alloy](https://practicalalloy.github.io/) is a guide to Alloy, not a different language. Alloy 6 supports mutable state and temporal formulas ([behavioral modeling](https://practicalalloy.github.io/chapters/behavioral-modeling/index.html)). `run` searches for satisfying instances; `check` searches for counterexamples within configured scope ([commands](https://practicalalloy.github.io/chapters/structural-topics/topics/commands/index.html)). Always record atom scopes, integer bounds where relevant, temporal bounds and solver mode; "no counterexample" is not an unqualified proof for all sizes. [Fairness guidance](https://practicalalloy.github.io/chapters/behavioral-topics/topics/fairness/index.html) matters for cancellation settlement.

If a formal spike is authorized, constrain it to one run, a bounded set of calls, Stop, queued/start/completion events and content disposal. Do not model PSI trees, OAuth, UI rendering or an entire agent. Require a reachable positive scenario as a non-vacuity check, and a deliberately broken admission rule that produces the expected counterexample. Check assumptions do not exclude the very race being tested. Translate useful counterexample traces into implementation regressions.

Do not maintain the same transition table independently in JSON, Quint and Java. The default keeps the implementation as the only executable agent. A temporary formal model is a design experiment; archive its scoped evidence and keep the resulting regression, not a permanent normative twin. If ongoing formal verification proves necessary, reopen this architecture and designate one authoritative transition model with an explicit conformance mechanism. No automatic promotion from a spike to a permanent framework.

Recommendation: no formal tooling in the initial workflow stages. A concrete Stop race already justifies careful design and testing, but not automatically a permanent model. Use Quint first for unresolved interleavings; Alloy if the hard question is relational consistency. Do not adopt both.

## Derive rather than maintain

Derive module/dependency inventories from Gradle; declarations, references, implementations and callers from IntelliJ; frozen signatures from semantic extraction/compiler-visible declarations; test outcomes from runner reports; packet inputs from feature references plus code closure; touched files from actual mutation results.

Maintain only decisions, behavioral requirements, assumptions, acceptance oracles and the mapping of requirements to enforcement. Code cannot derive user intent. An index cannot prove reflective/dynamic registration closure or actual disposal behavior; those need targeted runtime evidence. No permanent graph, duplicate AST/index, spec-to-Java code generator or separate model of every class.

## Semantic stages

The previous six-stage workflow-tooling prerequisite is withdrawn. Qualify the workflow by designing and implementing one real lifecycle/admission slice. Build only the small validation checks that slice uses. No separate packet generator, workflow server, predicate language or persistent duplicate state machine is a prerequisite.

Canonical slice requirements, type-safety audit, stage contracts and evidence identifiers live in `lifecycle-admission/spec.json` beside this plan. Concrete API/ownership design lives in `lifecycle-admission/design.md`. Stage checks reference those requirement/evidence IDs; they must not restate independent versions of requirements.

### Stage 1 — Normalize and design lifecycle/admission

**Purpose:** establish an executable, type-audited contract for lifecycle ownership, sequential effect admission and terminal accounting. **Depends on:** none.

**Preconditions:** user-requested Java/Kotlin split and W1–W8; source evidence available. No prior approval is inferred.

**Expected scope:** this plan and `lifecycle-admission/` specification, design, audit and evidence artifacts; no installed product implementation.

**Implementation:** define the smallest coherent slice, its observable outcomes and allowed API; enumerate invalid representations and operations; bind every requirement to enforcement and acceptance evidence. Reuse source facts in the evidence table. Resolve Stop-before-start, Stop-after-start, content close, stale callbacks, repeated completion and next-run admission without leaving the implementation model policy choices.

**Tests:** execute strict JSON/reference/coverage checks on the real slice; review positive and negative scenario traces. Add no general-purpose specification tooling.

**Completion contract:** Outcome: normalized slice and concrete design ready for independent review. Invariants: W1–W8. Artifacts and evidence: slice stage `S1` canonical entry/exit bindings. No product behavior is marked implemented.

**Commit:** design artifacts are included with the corresponding implementation semantic commit; ignored session artifacts are not falsely claimed committed.

### Stage 2 — Independently review and freeze the slice

**Purpose:** close design ambiguity before production edits. **Depends on:** Stage 1.

**Preconditions:** canonical `S1` exit conditions pass; review model agreement recorded; frozen spec/design/source basis.

**Expected scope:** append-only slice review/disposition ledger and bounded design corrections.

**Implementation:** independent review covers every requirement, audit row, state/event outcome, stage contract and acceptance scenario. Resolve blockers in one address pass; targeted final review checks corrections and their affected neighbors. Any material design change invalidates affected evidence. No author self-approval.

**Tests:** repeat slice checks on final basis; verify expected rejection for a missing audit binding or stale evidence. Review does not count as behavioral execution.

**Completion contract:** Outcome: SPEC_VALID and DESIGN_VALID only on a current passing basis with no open blockers/majors. Invariants: W1–W8. Artifacts and evidence: canonical stage `S2` bindings and actual review results.

**Commit:** no production commit in this stage.

### Stage 3 — Implement and qualify the lifecycle/admission slice

**Purpose:** implement the reviewed contract and prove the actual concurrency/lifecycle behavior. **Depends on:** Stage 2.

**Preconditions:** canonical `S2` exit evidence current; required branch/VCS and IDE capabilities available; approved edit surface unchanged; no conflicting user edits.

**Expected scope:** reviewed native lifecycle/admission Java classes, the minimal Kotlin coroutine adapter if required by the frozen slice, focused tests and corresponding specification. No donor deletion, live provider, new chat UI or unrelated runtime dependency.

**Implementation:** follow the frozen APIs and ownership design. Apply effects through their real admission boundary, never merely check before enqueueing. Keep Java domain policy separate from coroutine/platform dispatch. Retain completed effects and block new-run admission until owned work settles. Do not introduce placeholder provider/tool implementations.

**Tests:** run the exact focused tests and runtime scenarios named by canonical stage `S3`, including queued-effect cancellation, already-started effect settlement and subsequent-run exclusion. Compile/build through the existing IDE/Gradle configuration. No full product or live-provider claim from this slice.

**Completion contract:** Outcome: usable, tested lifecycle/admission component satisfying every slice acceptance criterion. Invariants: W1–W8 and slice requirements. Artifacts and evidence: canonical `S3` bindings, actual test/build/smoke outputs and allowed-surface inspection. Missing evidence blocks completion.

**Commit:** `feat: enforce native run lifecycle and effect admission`, through repository-authorized IDE VCS only after required verification passes. No push.

## Product delivery roadmap after workflow qualification

Each row becomes a feature closure with SPEC_VALID and DESIGN_VALID before code. This roadmap is not permission to implement from the older handoff or from this table alone.

| Milestone | Product behavior | Required exit evidence |
|---|---|---|
| Baseline | Current donor build/test/package and retained-operation closure recorded | Actual existing runners; no invented successful baseline. |
| Domain and run driver | Java algebra and minimal Kotlin driver; sequential continuation; accepted/provisional distinction; complete Stop accounting | Platform-free transition/contract tests, coroutine lifetime smoke, invalid/truncated batch never executes. |
| Native semantic reads | Bounded discovery, exact opaque identity, unsaved editor reads and references without MCP/HTTP | Overload and stale-handle fixtures through real IDE operations. |
| Native guarded mutations | Exact method replacement, localized text, create-only file, semantic rename, undo, effect admission and owned formatting | Wrong/stale target rejected; same-line neighbor preserved; zero-usage rename supported; Stop queue race and undo through actual IDE. |
| Native verification | Focused diagnostics, real build and targeted test terminal results | Pending is not clean; targeted failing/passing test and busy/cancel lifecycle. |
| Codex auth/transport | Supported subscription login, secure credentials, strict response assembly and provider history continuity | Protocol fixtures plus real supported login and real native tool continuation; no inferred service/client authorization from Pi's license. |
| Native UI | Swing/editor session, draft, Send/Stop, content lifetime and disposal | Actual Swing/editor interaction, not JCEF/browser screenshots; no stale UI callbacks. |
| Dogfood decision | Compare same real Java tasks against Pi + idea-facade | Overload edit, cross-file rename, real defect/test task; correctness and interventions; explicit Go/Adjust/Stop. |
| Cutover and subtraction | Native registrations only; remove unreachable donor runtime and packaging | Dependency/registration census plus startup/archive evidence; build-green semantic deletion commits. |

After shared domain contracts freeze, provider and tool implementation can run independently, with a single integration owner for shared APIs/build/plugin registrations. No live provider qualification before real tools. Command execution re-enters only through its recorded dogfood requirement. Persistence, providers and other expansion wait until after subtraction.

## Risk / test matrix

| Risk | Gate / evidence |
|---|---|
| JSON silently replaces conflicting decision | S1 strict duplicate-key and reference checks on the real slice. |
| Structural checks are mistaken for semantic proof | S2 independent review and explicit coverage limitations. |
| Unsatisfiable guards make graph look safe | S2 positive/negative scenario traces; formal spike only if needed. |
| Changed shared rule leaves stale approval | S2/S3 frozen-input evidence checks. |
| A Java type falsely claims proof or single use | S1 mandatory type-safety audit; S2 review; S3 residual runtime tests. |
| Kotlin duplicates policy or loses scope ownership | S2 boundary review and S3 lifetime/Stop evidence. |
| Implementation changes the approved API | S3 semantic surface and allowed-file inspection. |
| Tests pass but real dispatch behavior is wrong | S3 actual queued-operation smoke; fixture claims scoped accurately. |
| Formal model is vacuous or diverges from code | If introduced: positive scenario, deliberately broken rule, bounds and implementation regression. |
| Workflow tooling delays product learning | No independent tooling stages; retain only checks exercised by this slice. |

## Final verification and boundaries

For this planning session: source reads, current codeq lookup, official-document inspection, source manifest and structural artifact checks only. No spec-lint implementation, independent plan approval, model checker, build or native behavior has been run.

For the first slice, execute only the checks, review and focused behavior evidence named by canonical S1–S3. The earlier proposed `scripts/native-spec/` CLI commands describe possible later automation, not prerequisites or implemented tooling. Reuse concrete evidence by ID; never maintain a second entry/exit checklist with duplicated normative conditions.

For later native production slices: use IDE `run_tests` with exact selected tests; default `:plugin-core:test` excludes integration-tagged tests, so select the appropriate real platform/integration runner explicitly. Run IDE `build_project` before ending a code-change turn and before each semantic commit. Final product qualification includes unit tests, appropriate IDE integration tests, `:plugin-core:buildPlugin`, compatibility verification against declared minimum/current IDEs, real native UI startup and the roadmap scenarios. Packaging/verifier task availability and exact invocation are recorded from the actual selected build before qualification; no fabricated `platformTest` task.

Use IDE VCS diff/status for allowed-file and whitespace/conflict checks (repository policy forbids shell git). The slice design names the exact approved source/test files. Planning artifacts remain under `.agent-work/native-agent-workflow/`; production implementation begins only after S2. Preserve historical inputs and unrelated work. No dependency removals or pushes.

## Deliberately not built

No generic specification language, arbitrary predicate evaluator, solver framework, durable state-machine twin, production code generation, workflow server, event store, graph database, duplicate PSI index, all-repository context collector, reflection-based registration, generic effects runtime, coroutine wrapper for every Java method, mandatory JSpecify/NullAway, or Java 25 compatibility break without a product decision. No formal tool is a prerequisite merely because it can express the problem.

No unresolved user choice blocks this workflow plan. Java 21 compatibility is retained conservatively; a later request to drop older IDEs reopens that decision. Full product details are intentionally resolved per feature before implementation, never left for the implementation model to guess.

## Workflow contract

Plan status: COMPLETE_FOR_REVIEW
Issue: explicit native-agent workflow planning request
Base: master; current scoped source hashes in source-manifest.json
Implementation branch: native-agent-workflow (proposed, not created)
Evidence manifest: COMPLETE for this workflow plan's source claims; not a full donor cutover census
Stages: 3 (real slice first; previous six tooling stages withdrawn)
Invariants: COMPLETE (W1–W8)
Risk/test matrix: COMPLETE
Final verification: COMPLETE as a prescribed procedure; execution not claimed
Independent review: NOT_RUN
Product SPEC_VALID / DESIGN_VALID: NOT_ISSUED

## Plan revision — real slice before tooling

User-directed correction: added W8 and its mandatory type/API-first audit; replaced the six workflow-infrastructure stages with S1 design, S2 independent review and S3 implementation/qualification. Stage entry and exit checks now reference the slice's canonical requirement/evidence bindings. Previous artifact-check results are stale and must not be used to approve this revision. Product implementation remains gated on actual S2 evidence, not this planning status.
