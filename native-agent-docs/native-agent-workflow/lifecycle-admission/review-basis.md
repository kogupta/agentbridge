# Frozen lifecycle review basisCurrent branch: native-agent-workflow

## .agent-work/native-agent-workflow/plan.md
sha256: 25d9c121ceb4a46f69734a06b32911dba44fa82e1f38ebb1b6444d9c4be4c7c7
```
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

```

## .agent-work/native-agent-workflow/lifecycle-admission/spec.json
sha256: 63bb188d7a0d2d6658b74a5aada3b1c09bbe61ecbb08345e81762dd678ef8404
```
{
  "format_version": 1,
  "feature": "CAP-LIFECYCLE-ADMISSION",
  "scope": "Reusable Java lifecycle/admission owner for synchronous effects invoked at their actual execution boundary. Not the provider loop, tool codec, session service or UI integration. Kotlin dispatch integration follows when the native driver exists; no coroutine wrapper is needed by this synchronous core.",
  "decisions": {
    "language": "JAVA_21",
    "coroutines": "KOTLIN_AT_ASYNC_BOUNDARY_ONLY",
    "state_owner": "ONE_MONITOR_PER_LIFECYCLE_INSTANCE",
    "completion": "EXPLICIT_DRIVER_FINISH_ACK",
    "call_ids": "UNIQUE_PER_RUN",
    "reflection": "NONE_REQUIRED",
    "nullness": "CONSTRUCTOR_CHECKS_NO_NEW_CHECKER",
    "effect_start": "LINEARIZED_ADMISSION_AT_ACTUAL_EXECUTION_ENTRY",
    "receipt_identity": "STAGE_FEATURE_SPEC_DESIGN_SOURCE_CONFIG_EVIDENCE_HASHES"
  },
  "requirements": [
    {
      "id": "LC-001",
      "statement": "Only one run may own a lifecycle instance; a busy or closing/closed instance cannot admit another run.",
      "acceptance": [
        "AC-001"
      ]
    },
    {
      "id": "LC-002",
      "statement": "Each batch is a nonempty immutable ordered collection of distinct nonblank CallIds; malformed batches fail construction before admission.",
      "acceptance": [
        "AC-002"
      ]
    },
    {
      "id": "LC-003",
      "statement": "Only the next pending call in the current batch may start; at most one effect is executing. Another batch cannot replace an unsettled batch.",
      "acceptance": [
        "AC-003"
      ]
    },
    {
      "id": "LC-004",
      "statement": "Stop and effect-start admission are linearized under the same monitor at execute entry inside the actual queued runnable. Stop winning prevents callback invocation. Admission winning counts as already started; no enqueue or suspension is allowed between admission and synchronous callback invocation.",
      "acceptance": [
        "AC-004",
        "AC-013"
      ]
    },
    {
      "id": "LC-005",
      "statement": "Stop cancels every pending call, retains executing/completed dispositions, and blocks further effects and new runs. Already-started work may finish; finishRun requires a settled batch and explicit driver cleanup acknowledgement.",
      "acceptance": [
        "AC-005"
      ]
    },
    {
      "id": "LC-006",
      "statement": "Each batch call has exactly one terminal status. Effect has no return value: normal return records COMPLETED; RuntimeException or Error records FAILED_AFTER_START before rethrowing the same object. Lifecycle state retains only the status; no public complete method or automatic replay exists.",
      "acceptance": [
        "AC-006"
      ]
    },
    {
      "id": "LC-007",
      "statement": "Run and batch handles are opaque identity capabilities; handles from another owner or previous generation cannot start, finish, stop, or overwrite current work. Lifecycle and batch snapshots contain no capability handles. Previously returned immutable snapshots remain readable; a stale handle cannot query a replaced batch.",
      "acceptance": [
        "AC-007"
      ]
    },
    {
      "id": "LC-008",
      "statement": "close is idempotent: IDLE closes immediately; RUNNING or STOPPING enters CLOSING, cancels pending work and preserves an active run until finishRun acknowledges settlement; CLOSING and CLOSED never reopen.",
      "acceptance": [
        "AC-008"
      ]
    },
    {
      "id": "LC-009",
      "statement": "Domain values, transitions and outcomes contain no PSI, JSON, coroutine, provider or unconstrained generic result objects. Effect is value-free and execution success is a closed variant. Expected rejection uses operation-local closed outcomes; programmer nulls fail before state mutation.",
      "acceptance": [
        "AC-009"
      ]
    },
    {
      "id": "LC-010",
      "statement": "No monitor is held while an effect callback runs. Stop must return while a callback is blocked. State inspection returns immutable snapshots, never owner internals.",
      "acceptance": [
        "AC-010"
      ]
    },
    {
      "id": "LC-011",
      "statement": "A RUNNING run can accept multiple settled batches and must have unique CallIds across that run. beginBatch validates every call and performs no mutation on rejection, including a mixed fresh/reused candidate. Stop/close cancel pending calls; unfinished batches cannot be silently discarded by finishRun.",
      "acceptance": [
        "AC-011"
      ]
    },
    {
      "id": "LC-012",
      "statement": "Every stage is gated by canonical requirement and evidence IDs for exact frozen inputs. Each invariant has a type/API-first audit row, and each receipt binds stage, feature, spec/design/source hashes, configuration identity and evidence result hashes.",
      "acceptance": [
        "AC-012"
      ]
    }
  ],
  "acceptance": [
    {
      "id": "AC-001",
      "when": "Start twice; finish the first run; start again.",
      "then": "Second start is BUSY; later start owns a new handle.",
      "target": "RunLifecycleTest#singleRunOwnership"
    },
    {
      "id": "AC-002",
      "when": "Construct empty, duplicate-ID and null-containing batches; mutate the input list after valid construction.",
      "then": "Invalid values rejected; admitted order unaffected by caller mutation.",
      "target": "RunLifecycleTest#validatedImmutableBatch"
    },
    {
      "id": "AC-003",
      "when": "Attempt call B before A, call B while A is blocked, and execute A concurrently from two threads using barriers.",
      "then": "B never invokes; exactly one A callback enters; the losing A attempt is ALREADY_EXECUTING; after A settles, B executes once and another A attempt is ALREADY_TERMINAL.",
      "target": "RunLifecycleTest#orderedExclusiveExecution"
    },
    {
      "id": "AC-004",
      "when": "Submit an effect invocation to an executor behind a gate; request Stop before releasing the gate.",
      "then": "Callback counter remains zero; all pending calls are CANCELLED_BEFORE_START; no effect begins after Stop wins.",
      "target": "RunLifecycleTest#stopBeforeQueuedEffect"
    },
    {
      "id": "AC-005",
      "when": "Admit A, block inside its callback, then Stop and attempt finish/start; release A.",
      "then": "Stop returns without callback release; no B runs; finish/start are rejected until A settles; A is COMPLETED or FAILED_AFTER_START; explicit finish releases the run.",
      "target": "RunLifecycleTest#stopDuringEffect"
    },
    {
      "id": "AC-006",
      "when": "Execute value-free effects once with normal return, RuntimeException and AssertionError; attempt each again.",
      "then": "One terminal status each; thrown object is rethrown unchanged after FAILED_AFTER_START accounting; no replay or nullable result exists.",
      "target": "RunLifecycleTest#terminalAccounting"
    },
    {
      "id": "AC-007",
      "when": "Use handles across owners and after starting a new generation; capture an old snapshot, replace the batch, then query with the old handle.",
      "then": "No current state changes; captured snapshot remains readable and immutable; stale handle query returns STALE_BATCH and exposes no capability.",
      "target": "RunLifecycleTest#staleHandleIsolation"
    },
    {
      "id": "AC-008",
      "when": "Stop an active run, close it, close again, settle its active effect and acknowledge cleanup.",
      "then": "Phase is STOPPING then CLOSING then CLOSED; pending calls cancel, active outcome is retained, close is idempotent, and start remains rejected forever.",
      "target": "RunLifecycleTest#closeDrainsWithoutReopening"
    },
    {
      "id": "AC-009",
      "when": "Inspect public signatures/dependencies and pass null to CallId, CallBatch.of, beginBatch, execute, stop, finishRun and batchSnapshot, recording snapshots before each mutating call.",
      "then": "No forbidden dependency or generic effect result; each null fails before state mutation; no lifecycle or batch snapshot changes.",
      "target": "RunLifecycleTest#nullInputDoesNotMutate"
    },
    {
      "id": "AC-010",
      "when": "Block callback; Stop and query from another thread; inspect earlier snapshot after later updates.",
      "then": "Stop/query return without callback release; earlier snapshot is unchanged.",
      "target": "RunLifecycleTest#nonblockingStopAndImmutableSnapshots"
    },
    {
      "id": "AC-011",
      "when": "Finish with calls pending; settle a batch; submit [freshB, reusedA]; then submit freshB in a new batch and finish normally.",
      "then": "Pending finish rejects; mixed batch rejects without poisoning freshB; freshB later executes; normal finish transitions RUNNING to IDLE and a new run can start.",
      "target": "RunLifecycleTest#multipleBatchesPreserveIdentity"
    },
    {
      "id": "AC-012",
      "when": "Remove an audit binding and alter a frozen source hash in disposable copies.",
      "then": "Bounded artifact/gate check rejects each; valid design shape alone does not mark behavior evidence passed.",
      "target": "check_slice.py"
    },
    {
      "id": "AC-013",
      "when": "Run execute through the real AWT EventQueue behind a latch; exercise Stop-before-runnable and admission-before-Stop orderings separately.",
      "then": "The first ordering invokes no callback; the second ordering records an executing callback and Stop returns without waiting; the owner cannot start another run until settlement.",
      "target": "lifecycle-admission-awt-smoke"
    }
  ],
  "type_safety_audit": [
    {
      "requirement": "LC-001",
      "invalid": "Idle plus an active run, or two active runs",
      "type_api_prevention": "Final RunLifecycle owner with private state and distinct final RunHandle; no public active-state constructor",
      "residual_runtime_obligation": "Synchronized startRun and owner-key identity checks serialize competing starts and reject foreign handles",
      "justification": "Java aliases do not enforce single ownership; no per-state wrapper is allowed to become stale authority"
    },
    {
      "requirement": "LC-002",
      "invalid": "Empty/duplicate/null/mutated call list",
      "type_api_prevention": "CallId and private CallBatch factory; List.copyOf plus nonempty/distinct validation",
      "residual_runtime_obligation": "Constructor validation of externally supplied values",
      "justification": "Java has no built-in nonempty/unique list type; one domain wrapper is simpler than pervasive checks"
    },
    {
      "requirement": "LC-003",
      "invalid": "Execute wrong call or two effects",
      "type_api_prevention": "Private BatchState cursor/status map and no public completion mutator",
      "residual_runtime_obligation": "Cursor/admission checked under monitor on every attempt",
      "justification": "Sequential ordering involves changing shared state, not just a parameter type"
    },
    {
      "requirement": "LC-004",
      "invalid": "Scheduled token grants effect after Stop",
      "type_api_prevention": "No public admission token; execute owns admission and immediate callback invocation",
      "residual_runtime_obligation": "Stop and execute share one monitor and linearization point",
      "justification": "No affine references or atomic world transition in Java; tests cover both race orderings"
    },
    {
      "requirement": "LC-005",
      "invalid": "Stopping admits work or falsely claims rollback",
      "type_api_prevention": "Stopping owns run state; finish returns Unsettled while work remains",
      "residual_runtime_obligation": "Driver invokes finish only after its own resources settle; this component accounts effects only",
      "justification": "Type state cannot force an external platform operation to terminate"
    },
    {
      "requirement": "LC-006",
      "invalid": "Duplicate completion or contradictory outcome flags",
      "type_api_prevention": "Value-free Effect; one terminal CallStatus; Executed and Rejected are disjoint closed variants; no public complete operation",
      "residual_runtime_obligation": "Normal return records COMPLETED; RuntimeException/Error records FAILED_AFTER_START and rethrows the same object; current executing identity is checked and terminal recording occurs once",
      "justification": "An effect failure is temporal/external and must be observed"
    },
    {
      "requirement": "LC-007",
      "invalid": "CallId/RunHandle/BatchHandle mixup or forged public capability",
      "type_api_prevention": "Distinct final handle types with package-private construction; LifecycleSnapshot and ordered BatchSnapshot contain no handles",
      "residual_runtime_obligation": "Private owner-key identity and current-handle checks reject foreign/replaced handles; captured snapshots are inert values",
      "justification": "Package-local construction alone cannot prove ownership or freshness"
    },
    {
      "requirement": "LC-008",
      "invalid": "Closed with new admission or lost executing work",
      "type_api_prevention": "LifecyclePhase distinguishes STOPPING/CLOSING/CLOSED; close has no reopen transition",
      "residual_runtime_obligation": "close cancels pending work; finish settles Closing",
      "justification": "External running effect can outlive content; no unconditional liveness claim"
    },
    {
      "requirement": "LC-009",
      "invalid": "Generic JSON/PSI/null/protocol flags in core",
      "type_api_prevention": "Value-free Effect, non-generic closed result algebras, ordered handle-free snapshots, no forbidden type imports, null-check constructors",
      "residual_runtime_obligation": "Null-check every public argument before state mutation; inspect public signatures/dependencies; no static whole-program nullness claim",
      "justification": "No JSpecify/NullAway requested; Java nullable references remain a stated limitation"
    },
    {
      "requirement": "LC-010",
      "invalid": "Caller mutates internal state or Stop waits on arbitrary callback",
      "type_api_prevention": "Immutable ordered snapshots; owner state, map and capability handles never exposed",
      "residual_runtime_obligation": "Release monitor before callback; capture snapshot under monitor",
      "justification": "Types do not prove lock duration or scheduling"
    },
    {
      "requirement": "LC-011",
      "invalid": "Call IDs collide across accepted batches",
      "type_api_prevention": "CallBatch validates local uniqueness; run-private accepted ID set and atomic batch installation",
      "residual_runtime_obligation": "Reject reuse before admitting batch; finish requires settled state",
      "justification": "Identity uniqueness is relative to accumulated history"
    },
    {
      "requirement": "LC-012",
      "invalid": "Green stage without concrete evidence or type audit",
      "type_api_prevention": "Fixed requirement/audit/evidence/stage records and exact receipt identity fields",
      "residual_runtime_obligation": "Check referential coverage and hashes; reviewer judges prevention adequacy",
      "justification": "Structural checks cannot prove arbitrary semantic enforcement"
    }
  ],
  "audit_dimensions": [
    "sum_types",
    "state_payloads",
    "validated_values",
    "constructor_authority",
    "transitions",
    "argument_result_correlation",
    "collection_invariants",
    "aliasing_ownership",
    "protocol_progression",
    "visibility",
    "nullness"
  ],
  "non_goals": [
    "Provider/stream/history assembly and wire validation",
    "Actual PSI mutation algorithms, handles, rename and undo",
    "Tool-window registration or UI callback delivery",
    "Coroutine driver/service ownership implementation",
    "Generic workflow/packet/graph infrastructure",
    "Donor deletion or dependency migration",
    "Unconditional termination of arbitrary callbacks"
  ],
  "assumptions": [
    "Caller constructs one lifecycle owner per native session; project service uniqueness is a later integration obligation.",
    "Effect callbacks are synchronous; no work is queued or suspended after admission before the callback boundary.",
    "The driver calls finishRun only after other owned resources settle. This component cannot discover detached work.",
    "External callbacks eventually return if eventual settlement is claimed. Safety is required even when they do not."
  ],
  "evidence": [
    {
      "id": "EV-SOURCE",
      "kind": "source",
      "description": "Current source-manifest hashes, branch identity, and cited donor/Pi ranges; reissued whenever source basis changes."
    },
    {
      "id": "EV-STRUCTURE",
      "kind": "command",
      "description": "Strict parse plus identity/reference/audit/matrix/null-boundary/stage-binding checks; expected invalid-copy rejection."
    },
    {
      "id": "EV-DESIGN-COMPILE",
      "kind": "command",
      "description": "Successful javac --release 21 compilation of source-backed declarations under plugin-core/src/main/java/.../nativeagent/lifecycle/; command output and classpath identity retained."
    },
    {
      "id": "EV-REVIEW",
      "kind": "review",
      "description": "Fresh exact-basis review by openai-codex/gpt-5.6-sol:medium with all findings dispositioned and targeted final gate passed."
    },
    {
      "id": "EV-VCS",
      "kind": "tool",
      "description": "Current branch native-agent-workflow, authorized IDE VCS capability and nonoverlapping diff"
    },
    {
      "id": "EV-FOCUSED",
      "kind": "command",
      "description": "Exact RunLifecycleTest test methods named by AC-001 through AC-011"
    },
    {
      "id": "EV-SMOKE",
      "kind": "command",
      "description": "Real AWT EventQueue scenario; callback loses/wins Stop; no model/provider claim"
    },
    {
      "id": "EV-BUILD",
      "kind": "tool",
      "description": "IDE build_project success on changed production basis"
    },
    {
      "id": "EV-SURFACE",
      "kind": "tool",
      "description": "Semantic API/allowed-file inspection and no forbidden core dependencies"
    },
    {
      "id": "EV-DESIGN-SOURCE",
      "kind": "source",
      "description": "Hashes and extracted public/package-visible signatures for all source-backed lifecycle declarations."
    },
    {
      "id": "EV-RECEIPT",
      "kind": "command",
      "description": "Exact-basis receipt identity check rejects altered spec, design, declaration/source, configuration or evidence result hashes."
    }
  ],
  "stages": [
    {
      "id": "S1",
      "entry": [
        {
          "requirements": [
            "LC-012"
          ],
          "evidence": [
            "EV-SOURCE"
          ]
        }
      ],
      "exit": [
        {
          "requirements": [
            "LC-001",
            "LC-002",
            "LC-003",
            "LC-004",
            "LC-005",
            "LC-006",
            "LC-007",
            "LC-008",
            "LC-009",
            "LC-010",
            "LC-011",
            "LC-012"
          ],
          "evidence": [
            "EV-STRUCTURE",
            "EV-DESIGN-SOURCE",
            "EV-DESIGN-COMPILE",
            "EV-RECEIPT"
          ]
        }
      ],
      "outcome": "Normalized type-audited design; no implementation claim"
    },
    {
      "id": "S2",
      "depends_on": [
        "S1"
      ],
      "entry": [
        {
          "requirements": [
            "LC-012"
          ],
          "evidence": [
            "EV-STRUCTURE",
            "EV-DESIGN-SOURCE",
            "EV-DESIGN-COMPILE",
            "EV-RECEIPT"
          ]
        }
      ],
      "exit": [
        {
          "requirements": [
            "LC-001",
            "LC-002",
            "LC-003",
            "LC-004",
            "LC-005",
            "LC-006",
            "LC-007",
            "LC-008",
            "LC-009",
            "LC-010",
            "LC-011",
            "LC-012"
          ],
          "evidence": [
            "EV-REVIEW",
            "EV-STRUCTURE",
            "EV-DESIGN-SOURCE",
            "EV-DESIGN-COMPILE",
            "EV-RECEIPT"
          ]
        }
      ],
      "outcome": "Independent final gate passes on exact frozen basis"
    },
    {
      "id": "S3",
      "depends_on": [
        "S2"
      ],
      "entry": [
        {
          "requirements": [
            "LC-012"
          ],
          "evidence": [
            "EV-REVIEW",
            "EV-STRUCTURE",
            "EV-DESIGN-SOURCE",
            "EV-DESIGN-COMPILE",
            "EV-RECEIPT",
            "EV-VCS",
            "EV-SOURCE"
          ]
        }
      ],
      "exit": [
        {
          "requirements": [
            "LC-001",
            "LC-002",
            "LC-003",
            "LC-004",
            "LC-005",
            "LC-006",
            "LC-007",
            "LC-008",
            "LC-009",
            "LC-010",
            "LC-011"
          ],
          "evidence": [
            "EV-FOCUSED",
            "EV-SMOKE",
            "EV-BUILD",
            "EV-SURFACE",
            "EV-RECEIPT"
          ]
        },
        {
          "requirements": [
            "LC-012"
          ],
          "evidence": [
            "EV-STRUCTURE",
            "EV-REVIEW",
            "EV-VCS",
            "EV-RECEIPT"
          ]
        }
      ],
      "outcome": "Implemented/verified component; commit only via authorized IDE VCS"
    }
  ],
  "open_decisions": [],
  "operation_matrix": [
    {
      "operation": "startRun",
      "phase": "IDLE",
      "result": "Started",
      "next_phase": "RUNNING"
    },
    {
      "operation": "startRun",
      "phase": "RUNNING|STOPPING",
      "result": "Rejected(BUSY)",
      "next_phase": "unchanged"
    },
    {
      "operation": "startRun",
      "phase": "CLOSING|CLOSED",
      "result": "Rejected(CLOSED)",
      "next_phase": "unchanged"
    },
    {
      "operation": "beginBatch",
      "phase": "RUNNING",
      "precondition": "current run; no unsettled batch; no CallId already accepted",
      "result": "Begun",
      "next_phase": "RUNNING with new current batch"
    },
    {
      "operation": "beginBatch",
      "phase": "RUNNING",
      "precondition": "any rejected precondition",
      "result": "Rejected(exact matching reason)",
      "next_phase": "unchanged"
    },
    {
      "operation": "beginBatch",
      "phase": "STOPPING|CLOSING",
      "result": "Rejected(RUN_NOT_ACCEPTING_BATCHES)",
      "next_phase": "unchanged"
    },
    {
      "operation": "beginBatch",
      "phase": "IDLE|CLOSED or foreign/old run",
      "result": "Rejected(STALE_RUN)",
      "next_phase": "unchanged"
    },
    {
      "operation": "execute",
      "phase": "RUNNING",
      "precondition": "current batch; next pending call",
      "result": "Executed or thrown callback; terminal status recorded",
      "next_phase": "RUNNING or Stop/close phase set concurrently"
    },
    {
      "operation": "execute",
      "phase": "RUNNING",
      "precondition": "foreign/replaced batch",
      "result": "Rejected(STALE_BATCH)",
      "next_phase": "unchanged"
    },
    {
      "operation": "execute",
      "phase": "RUNNING",
      "precondition": "unknown/already executing/already terminal/out of order call",
      "result": "Rejected(exact matching reason)",
      "next_phase": "unchanged"
    },
    {
      "operation": "execute",
      "phase": "STOPPING|CLOSING|CLOSED",
      "result": "Rejected(RUN_NOT_ACCEPTING_EFFECTS) after current-batch identity check",
      "next_phase": "unchanged"
    },
    {
      "operation": "stop",
      "phase": "RUNNING",
      "result": "Acknowledged; transition STOPPING and cancel pending",
      "next_phase": "STOPPING"
    },
    {
      "operation": "stop",
      "phase": "STOPPING|CLOSING",
      "result": "Idempotent Acknowledged",
      "next_phase": "unchanged"
    },
    {
      "operation": "stop",
      "phase": "IDLE|CLOSED or foreign/old run",
      "result": "Rejected(STALE_RUN)",
      "next_phase": "unchanged"
    },
    {
      "operation": "finishRun",
      "phase": "RUNNING|STOPPING",
      "precondition": "no current batch or current batch settled",
      "result": "Finished",
      "next_phase": "IDLE"
    },
    {
      "operation": "finishRun",
      "phase": "CLOSING",
      "precondition": "no current batch or current batch settled",
      "result": "Finished",
      "next_phase": "CLOSED"
    },
    {
      "operation": "finishRun",
      "phase": "RUNNING|STOPPING|CLOSING",
      "precondition": "current batch unsettled",
      "result": "Rejected(BATCH_UNSETTLED)",
      "next_phase": "unchanged"
    },
    {
      "operation": "finishRun",
      "phase": "IDLE|CLOSED or foreign/old run",
      "result": "Rejected(STALE_RUN)",
      "next_phase": "unchanged"
    },
    {
      "operation": "close",
      "phase": "IDLE",
      "result": "Closed snapshot",
      "next_phase": "CLOSED"
    },
    {
      "operation": "close",
      "phase": "RUNNING|STOPPING",
      "result": "Closing snapshot and cancel pending",
      "next_phase": "CLOSING"
    },
    {
      "operation": "close",
      "phase": "CLOSING|CLOSED",
      "result": "Idempotent snapshot",
      "next_phase": "unchanged"
    },
    {
      "operation": "batchSnapshot",
      "phase": "any",
      "precondition": "current batch handle",
      "result": "Available(immutable ordered snapshot)",
      "next_phase": "unchanged"
    },
    {
      "operation": "batchSnapshot",
      "phase": "any",
      "precondition": "foreign/replaced/old batch handle",
      "result": "Rejected(STALE_BATCH)",
      "next_phase": "unchanged"
    }
  ],
  "receipt_identity": {
    "fields": [
      "stage",
      "feature",
      "spec_hash",
      "design_hash",
      "declaration_hashes",
      "configuration_identity",
      "evidence_hashes",
      "status"
    ],
    "volatile_fields_excluded_from_digest": [
      "timestamp"
    ]
  },
  "null_boundary_matrix": [
    {
      "operation": "CallId(String)",
      "null_input": "value",
      "expected": "NullPointerException; no owner state exists"
    },
    {
      "operation": "CallBatch.of(List<CallId>)",
      "null_input": "list or element",
      "expected": "NullPointerException before CallBatch exists"
    },
    {
      "operation": "beginBatch(RunHandle, CallBatch)",
      "null_input": "run or batch",
      "expected": "NullPointerException; lifecycle snapshot unchanged"
    },
    {
      "operation": "execute(BatchHandle, CallId, Effect)",
      "null_input": "batch, call or effect",
      "expected": "NullPointerException; lifecycle/batch snapshot unchanged"
    },
    {
      "operation": "stop(RunHandle)",
      "null_input": "run",
      "expected": "NullPointerException; lifecycle snapshot unchanged"
    },
    {
      "operation": "finishRun(RunHandle)",
      "null_input": "run",
      "expected": "NullPointerException; lifecycle snapshot unchanged"
    },
    {
      "operation": "batchSnapshot(BatchHandle)",
      "null_input": "handle",
      "expected": "NullPointerException; lifecycle/batch snapshot unchanged"
    }
  ]
}

```

## .agent-work/native-agent-workflow/lifecycle-admission/design.md
sha256: 2cc5edd5d451cc7bf5ca6cbe1eea8175ada4ef377538b7a7f3aa4589fd088aa7
```
# Lifecycle/admission design

## Design status

Draft pending fresh independent review. The declarations are installed in the real `plugin-core/src/main/java/com/github/catatafishen/agentbridge/nativeagent/lifecycle/` source tree on branch `native-agent-workflow`; IntelliJ must inspect and compile these exact files. The former ignored design-src copy is not part of the basis and is removed to prevent duplicate implementations.

## Scope

`RunLifecycle` owns one session-local lifecycle and one active run. It accepts validated ordered call batches and executes one synchronous effect at a time at the actual effect-entry boundary. It reports immutable snapshots and operation-local rejection outcomes.

It does not own provider streaming, tool argument decoding, PSI, JSON, coroutine scopes, Swing content, process handlers, or platform writes. A later Kotlin adapter may schedule the Java owner from an IntelliJ-owned scope; it must call this API and must not duplicate its state machine.

## Java surface

| Type | Role | Construction/visibility rule |
|---|---|---|
| `CallId` | Nonblank call identity | Public record validates non-null/nonblank value. |
| `CallBatch` | Nonempty immutable distinct ordered calls | Final class; only `of(List<CallId>)`; snapshots input. |
| `RunHandle` | Opaque run capability | Final class; constructor package-private; owner identity private; exposes no observation payload. |
| `BatchHandle` | Opaque batch capability | Final class; constructor package-private; owner/run identity private. |
| `LifecyclePhase` | Closed finite lifecycle phase set | Enum. |
| `CallStatus` | Closed call status set | Enum; transient and terminal statuses are explicit; only terminal statuses satisfy `isTerminal()`. |
| `LifecycleSnapshot` | Read-only lifecycle view | Sealed interface; active payload contains phase only and no capability handle. |
| `CallSnapshot` | Immutable call/status pair | Public record validates both components. |
| `BatchSnapshot` | Read-only ordered status view | Record copies a nonempty ordered list; it exposes no capability handle or owner mutation. |
| `BatchObservation` | Explicit presence/absence of current batch | Sealed variants; no nullable snapshot. |
| `Effect` | Value-free synchronous callback boundary | Functional interface; effect is called only after admission linearizes and cannot return a foreign result object. |
| `RunLifecycle` | Single owner and transition authority | Final class; all state mutations synchronized; no public state mutation or completion operation. |

`RunLifecycle` result algebras are nested sealed interfaces with named variants:

- `StartRunResult.Started` or `.Rejected(StartRejection)`;
- `BeginBatchResult.Begun` or `.Rejected(BatchRejection)`;
- `ExecutionResult.Executed` or `.Rejected(ExecutionRejection)`;
- `StopResult.Acknowledged` or `.Rejected(StopRejection)`;
- `FinishRunResult.Finished` or `.Rejected(FinishRejection)`.

No generic error envelope, status string, nullable success payload or caller-supplied capability token exists in the domain surface.

## State and transitions

The owner stores one `LifecyclePhase`, current `RunHandle`, at most one current `BatchState`, an owner-private identity key and a run-local accepted-call set.

- `IDLE -> RUNNING`: `startRun()` creates a fresh handle and clears prior accepted IDs.
- `RUNNING -> STOPPING`: `stop(currentRun)` marks all pending calls cancelled while leaving no monitor held across effects.
- `RUNNING -> CLOSING`: `close()` marks pending calls cancelled while an active run exists.
- `STOPPING -> CLOSING`: `close()` preserves the active run and moves it into closing; it can never later become IDLE.
- `RUNNING -> IDLE`: `finishRun(currentRun)` after no batch or a settled current batch.
- `STOPPING -> IDLE`: `finishRun(currentRun)` only after the current batch is settled.
- `CLOSING -> CLOSED`: `finishRun(currentRun)` only after the current batch is settled.
- `IDLE -> CLOSED`: `close()` is immediate because no effect is active.
- `CLOSED`: `startRun`, batch admission, effect execution and later close cannot reopen or mutate the owner.

The public API intentionally does not expose `BatchState`, its cursor, its status map or an effect-completion operation. `Effect.execute()` returns no value; normal return records `COMPLETED`, while `RuntimeException`/`Error` records `FAILED_AFTER_START` and rethrows the same object. Lifecycle state retains only statuses, never callback results or throwables.

## Admission algorithm

`execute(batch, call, effect)` performs these steps:

1. Validate non-null method arguments before acquiring state.
2. Synchronize on the owner.
3. Reject foreign/stale handles, non-running phases, unknown calls, terminal calls, and calls other than the first unsettled call.
4. Change the selected call from `PENDING` to `EXECUTING` while still holding the monitor.
5. Release the monitor before invoking the callback.
6. Invoke the callback immediately; no scheduling or suspension occurs between admission and callback invocation.
7. Reacquire the monitor to record the terminal disposition.

`stop()` uses the same monitor. If it acquires the monitor first, the pending call is cancelled and later `execute()` is rejected. If `execute()` acquires it first, the call is `EXECUTING` before `stop()` can change phase; Stop returns without waiting for the callback. No monitor is held during callback execution.

This establishes safety. It does not assert that an arbitrary callback eventually returns. The driver must call `finishRun()` after owned external work settles; `finishRun()` rejects while an executing call remains unsettled.

## Batch and run rules

`CallBatch.of` establishes nonempty, distinct, ordered immutable calls. `RunLifecycle.beginBatch` rejects a reused `CallId` anywhere in the current run, not only in the immediately previous batch. A second batch is rejected while the previous batch is unsettled. The API permits a second batch after the previous batch settles and while the run remains `RUNNING`.

`stop()` and `close()` cancel pending calls in the current batch. They do not invent a result for an executing call. A terminal snapshot is returned as an immutable value; it remains readable after the owner advances or closes. A stale `BatchHandle` cannot query a replaced batch, so the owner retains no historical batch map.

## Operation-by-phase matrix

Handle identity is checked before phase-specific rejection when an operation accepts a handle. A rejected operation never mutates state. `close` is the only idempotent lifecycle operation.

| Operation | Phase/precondition | Result and next phase |
|---|---|---|
| `startRun` | `IDLE` | `Started`; `RUNNING` |
| `startRun` | `RUNNING`/`STOPPING` | `Rejected(BUSY)`; unchanged |
| `startRun` | `CLOSING`/`CLOSED` | `Rejected(CLOSED)`; unchanged |
| `beginBatch` | current run, `RUNNING`, no unsettled current batch, all IDs fresh | `Begun`; `RUNNING` |
| `beginBatch` | current run but any precondition fails | exact rejection (`PREVIOUS_BATCH_UNSETTLED` or `CALL_ID_ALREADY_ACCEPTED`); unchanged |
| `beginBatch` | stale/foreign run or `IDLE`/`CLOSED` | `Rejected(STALE_RUN)`; unchanged |
| `beginBatch` | `STOPPING`/`CLOSING` | `Rejected(RUN_NOT_ACCEPTING_BATCHES)`; unchanged |
| `execute` | current batch, `RUNNING`, first pending call | `Executed` or callback throwable; terminal status recorded |
| `execute` | stale/replaced batch | `Rejected(STALE_BATCH)`; unchanged |
| `execute` | current batch but stopped/closing | `Rejected(RUN_NOT_ACCEPTING_EFFECTS)`; unchanged |
| `execute` | unknown, executing, terminal or later call | exact rejection; unchanged |
| `stop` | current run in `RUNNING` | `Acknowledged`; `STOPPING`, pending calls cancelled |
| `stop` | current run in `STOPPING`/`CLOSING` | idempotent `Acknowledged`; unchanged |
| `stop` | stale/foreign run or no current run | `Rejected(STALE_RUN)`; unchanged |
| `finishRun` | current run, no batch or settled batch, `RUNNING`/`STOPPING` | `Finished`; `IDLE` |
| `finishRun` | current run, no batch or settled batch, `CLOSING` | `Finished`; `CLOSED` |
| `finishRun` | current run with unsettled batch | `Rejected(BATCH_UNSETTLED)`; unchanged |
| `finishRun` | stale/foreign run or no current run | `Rejected(STALE_RUN)`; unchanged |
| `close` | `IDLE` | closed snapshot; `CLOSED` |
| `close` | `RUNNING`/`STOPPING` | closing snapshot; `CLOSING` |
| `close` | `CLOSING`/`CLOSED` | idempotent snapshot; unchanged |
| `batchSnapshot` | current batch handle | `Available(immutable ordered snapshot)`; unchanged |
| `batchSnapshot` | stale/foreign/replaced handle | `Rejected(STALE_BATCH)`; unchanged |

`beginBatch` performs every validation before installing the next `BatchState` or mutating `acceptedCallIds`; a rejected mixed batch cannot poison a later fresh call ID.

## Type-safety audit binding

| Requirement | Invalid representation/operation | Type/API prevention | Residual runtime obligation | Justification |
|---|---|---|---|---|
| LC-001 | Two active runs or start after close | One final owner; no public active-state constructor | Synchronize `startRun`; check phase/handle identity | Java cannot enforce linear ownership statically. |
| LC-002 | Empty/duplicate/null/mutable batch | `CallId`, private `CallBatch` constructor, `List.copyOf`, distinct check | Validate foreign input at construction | A general nonempty unique-list type would add more machinery than this slice needs. |
| LC-003 | Out-of-order or concurrent effects | Private batch cursor/dispositions; no completion mutator | Monitor checks next unsettled call and execution status | Ordering depends on mutable transition history. |
| LC-004 | Stop loses to queued effect or effect starts after Stop | No public admission token; execute owns entry | Same monitor linearizes Stop and actual callback entry | Java has no affine/linear capability type; callback invocation is external. |
| LC-005 | New work after Stop or false settlement | `STOPPING`/`CLOSING` are separate phases; `finishRun` requires settled batch | Driver owns external resource settlement | The owner cannot statically control arbitrary external work. |
| LC-006 | Duplicate completion or result flag combinations | No public completion method; one terminal disposition per call | normal return records COMPLETED; RuntimeException/Error records FAILED_AFTER_START and rethrows the same object; duplicate execute rejects | Callback failure and scheduling are runtime events. |
| LC-007 | Foreign/old capability changes current state or observation grants authority | Distinct final handle types; private owner key; snapshots contain neither handle | Identity checks at every operation; stale snapshot query rejects | Java aliases remain forgeable only through package code; owner key is inaccessible and snapshots are inert. |
| LC-008 | Closed owner reopens or loses active work | `Closed` snapshot and explicit close transitions | Idempotent close and cleanup acknowledgement | External running effect can outlive content; no unconditional liveness claim. |
| LC-009 | JSON/PSI/coroutine/provider objects in core | API contains only Java domain types; no generic envelope | Boundary/dependency inspection; constructor null checks; no static whole-program nullness claim | No JSpecify/NullAway is introduced; Java nullable references remain a stated limitation. |
| LC-010 | Stop blocks on callback or caller mutates state | Snapshot records and private state; monitor scope ends before callback | Concurrency test with blocked callback | Lock duration is a behavioral property, not represented by Java types. |
| LC-011 | Call IDs collide across accepted batches | Run-private accepted ID set and immutable batch | Reject reuse before batch admission | Uniqueness is relative to run history. |
| LC-012 | Gate passes without audit/evidence | Canonical IDs in `spec.json`; S1/S2/S3 evidence bindings | Exact-basis review and evidence checks | Structural tooling cannot judge whether a chosen type/API truly enforces semantics. |

Rejected alternatives: a generic `Result<E,A>`, universal effect token, mutable context bag, generic typestate/effects framework, duplicate Quint/Alloy transition model, and a permanent generated implementation. They add abstraction or duplicate truth without eliminating a residual Java aliasing/external-world obligation in this slice.

## Entry and exit evidence

- **S1 entry:** `EV-SOURCE`; user-selected Java/Kotlin boundary; W1–W8.
- **S1 exit:** all LC requirements present; `EV-STRUCTURE`, `EV-DESIGN-SOURCE`, `EV-DESIGN-COMPILE` and `EV-RECEIPT` from the source-backed declarations and exact-basis receipt.
- **S2 entry:** S1 exit remains valid and exact basis is frozen.
- **S2 exit:** `EV-REVIEW`, `EV-STRUCTURE`, `EV-DESIGN-SOURCE`, `EV-DESIGN-COMPILE` and `EV-RECEIPT`; zero open Blockers/Majors; reviewer confirms every audit row, matrix row and contract.
- **S3 entry:** current S2 receipt, source/design/compile receipt, clean authorized branch and allowed surface.
- **S3 exit:** `EV-FOCUSED`, `EV-SMOKE`, `EV-BUILD`, `EV-SURFACE` and `EV-RECEIPT`; LC-012 retains review/structure/review/VCS evidence.

A design compile is not behavior proof. The source-backed declarations are the only design basis; the temporary ignored declaration copy is deleted before the fresh review. A unit test is not actual AWT/IntelliJ lifecycle proof. A passing S3 does not qualify provider, UI, PSI or donor cutover.

```

## .agent-work/native-agent-workflow/lifecycle-admission/review-state.md
sha256: 2dddedde7847cf40993eaada51e134f97a97af54830aa2f20161e2ee6ad7beb6
```
# Lifecycle/admission review state

## Peer agreement

- Primary reviewer: `openai-codex/gpt-5.6-sol`, reasoning effort `medium`, configuration role `plan`.
- `omp config get modelRoles --json` confirms `plan = openai-codex/gpt-5.6-sol:medium`.
- Additional independent peer: none, explicitly selected by user.
- Author/coordinator: current session; not eligible to approve its own design.

## Frozen basis

- Basis files: `spec.json`, `design.md`, `design-src/**/*.java`, and the revised plan sections defining W1–W8 and S1–S3.
- Canonical requirements: LC-001 through LC-012.
- Acceptance criteria: AC-001 through AC-012.
- Type-safety audit: one row for each LC requirement; dimensions include sum types, state payloads, validated values, constructor authority, transitions, argument/result correlation, collection invariants, aliasing/ownership, protocol progression, visibility and nullness.
- Evidence IDs: EV-SOURCE, EV-STRUCTURE, EV-DESIGN-COMPILE, EV-REVIEW, EV-VCS, EV-FOCUSED, EV-SMOKE, EV-BUILD, EV-SURFACE.
- Stage contracts: S1 normalize/design, S2 independent review/freeze, S3 implementation/qualification.
- Source basis: current project branch is still `master`; the eight pre-existing untracked entries are unrelated to this lifecycle basis. Design declarations compile with `javac --release 21` in the ignored design workspace. No production source has been installed.

## Review status

Review status: IN_PROGRESS
Reviewer checklist: INCOMPLETE
Frozen basis: CURRENT
Peer agreement: openai-codex/gpt-5.6-sol:medium + none
Peer exchanges used: 0
Open Blockers: unknown; independent review running
Open Majors: unknown; independent review running
Deferred Minors/Nits: 0
Verification: PARTIAL (design declarations compile; structural binding check passes; behavioral tests not run)
Review rounds: 1
Stop reason: awaiting independent review output
Next permitted action: address reviewer findings; do not install production source until review passes

## Review round 1 — independent findings

Reviewer: `openai-codex/gpt-5.6-sol:medium` (configured `plan` role). Basis at review launch was stale relative to the later branch/source installation, so B-004 and M-005 are accepted independently of that timing issue. The remaining findings are valid design corrections.

- **B-001 — Addressed:** replace generic `Effect<T>` and `ExecutionResult.Executed<T>` with a value-free `Effect` and non-generic `ExecutionResult.Executed`; lifecycle retains only `CallStatus`. Update LC-006/LC-009, AC-006/AC-009 and the audit.
- **B-002 — Addressed:** remove `RunHandle` from `LifecycleSnapshot`; snapshots expose phase only. Remove `BatchHandle` from `BatchSnapshot`; use ordered `CallSnapshot` values. Stale handles cannot query replaced batches.
- **B-003 — Addressed:** document normal `RUNNING -> IDLE` completion and `STOPPING + close -> CLOSING`; add close-after-stop acceptance.
- **B-004 — Addressed:** source-backed declarations now exist under `plugin-core/src/main/java/.../nativeagent/lifecycle/`; compile/surface evidence is reissued after correction.
- **M-001 — Addressed:** add the complete public operation-by-phase matrix and precedence to the design/spec.
- **M-002 — Addressed:** retain transactional `beginBatch` validation before any accepted-ID mutation; add mixed fresh/reused ID regression.
- **M-003 — Addressed:** add same-call concurrent execution regression using barriers.
- **M-004 — Addressed:** normal return and throwable accounting are explicit; completion rechecks current owner/batch/call and preserves the original throwable if accounting itself fails.
- **M-005 — Addressed:** add exact receipt identity fields and current source/design/compile hashes to the frozen basis. Re-review is required after these changes.
- **M-006 — Addressed:** enumerate all nullable public boundaries and before/after snapshot assertions.
- **M-007 — Addressed:** choose minimal semantics: captured snapshots remain readable; stale handles cannot query after replacement.
- **N-001 — Addressed:** rename `CallDisposition` to `CallStatus`.
- **N-002 — Accepted with clarification:** package-private handle constructors can create inert handles, but the private owner key is inaccessible; prose now says owner identity, not package visibility, establishes acceptance.
- **N-003 — Addressed:** `BatchSnapshot` exposes an immutable ordered list of `CallSnapshot`.
- **N-004 — Addressed:** focused tests and separately launched AWT smoke have distinct evidence IDs and scenarios.

Review status: FINDINGS_READY_FOR_ADDRESS
Reviewer checklist: COMPLETE
Frozen basis: INVALIDATED
Peer agreement: openai-codex/gpt-5.6-sol:medium + none
Peer exchanges used: 0
Open Blockers: 0 after addressed corrections; fresh review required
Open Majors: 0 after addressed corrections; fresh review required
Deferred Minors/Nits: 0
Verification: PARTIAL (source-backed declarations compile before final correction; recompile and focused behavior tests required)
Review rounds: 1
Stop reason: review basis changed; targeted final gate cannot reuse this review
Next permitted action: update canonical spec/design/receipts, recompile, obtain fresh independent review, then implement focused tests

```

## plugin-core/src/main/java/com/github/catatafishen/agentbridge/nativeagent/lifecycle/BatchHandle.java
sha256: 0baabc1fc7fec913f5a187dd1c34039c45cc2429b0b3b9bae264b0797f6dfd75
```
package com.github.catatafishen.agentbridge.nativeagent.lifecycle;

public final class BatchHandle {
    private final Object ownerKey;
    private final RunHandle run;

    BatchHandle(Object ownerKey, RunHandle run) {
        this.ownerKey = ownerKey;
        this.run = run;
    }

    boolean belongsTo(Object expectedOwnerKey) {
        return ownerKey == expectedOwnerKey;
    }

    RunHandle run() {
        return run;
    }
}

```

## plugin-core/src/main/java/com/github/catatafishen/agentbridge/nativeagent/lifecycle/BatchObservation.java
sha256: 2a59676a6625140bbeeaf7ebe2fa5c95c692d345a04f786bbe9161af9dec0316
```
package com.github.catatafishen.agentbridge.nativeagent.lifecycle;

import java.util.Objects;

public sealed interface BatchObservation {
    record Present(BatchSnapshot snapshot) implements BatchObservation {
        public Present {
            Objects.requireNonNull(snapshot, "snapshot");
        }
    }

    record Absent() implements BatchObservation {
    }
}

```

## plugin-core/src/main/java/com/github/catatafishen/agentbridge/nativeagent/lifecycle/BatchSnapshot.java
sha256: fdbfb04ab3805aa8b1a53bf5b6519950f39a0a6a7f77896d703db019ff4b07f9
```
package com.github.catatafishen.agentbridge.nativeagent.lifecycle;

import java.util.List;
import java.util.Objects;

public record BatchSnapshot(List<CallSnapshot> calls) {
    public BatchSnapshot {
        List<CallSnapshot> snapshot = List.copyOf(Objects.requireNonNull(calls, "calls"));
        if (snapshot.isEmpty()) {
            throw new IllegalArgumentException("BatchSnapshot must not be empty");
        }
        calls = snapshot;
    }

    public boolean isSettled() {
        return calls.stream().allMatch(call -> call.status().isTerminal());
    }
}

```

## plugin-core/src/main/java/com/github/catatafishen/agentbridge/nativeagent/lifecycle/CallBatch.java
sha256: 9286d1d3b6c87dcebf763cc9efa95918345cb2410ccfffe18887d5fb7790786b
```
package com.github.catatafishen.agentbridge.nativeagent.lifecycle;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public final class CallBatch {
    private final List<CallId> calls;

    private CallBatch(List<CallId> calls) {
        this.calls = calls;
    }

    public static CallBatch of(List<CallId> calls) {
        Objects.requireNonNull(calls, "calls");
        List<CallId> snapshot = List.copyOf(calls);
        if (snapshot.isEmpty()) {
            throw new IllegalArgumentException("CallBatch must not be empty");
        }
        if (new HashSet<>(snapshot).size() != snapshot.size()) {
            throw new IllegalArgumentException("CallBatch CallIds must be distinct");
        }
        return new CallBatch(snapshot);
    }

    public List<CallId> calls() {
        return calls;
    }
}

```

## plugin-core/src/main/java/com/github/catatafishen/agentbridge/nativeagent/lifecycle/CallId.java
sha256: 1093cc9b7ecf1a1d597d695b4ae1a9619edeb3800ed678e798b8de1ecc6724e7
```
package com.github.catatafishen.agentbridge.nativeagent.lifecycle;

import java.util.Objects;

public record CallId(String value) {
    public CallId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("CallId must not be blank");
        }
    }
}

```

## plugin-core/src/main/java/com/github/catatafishen/agentbridge/nativeagent/lifecycle/CallSnapshot.java
sha256: b92c49011ca875410652452c6fb08d1b3aee669d03e59931bb449108b6b81ccb
```
package com.github.catatafishen.agentbridge.nativeagent.lifecycle;

import java.util.Objects;

public record CallSnapshot(CallId call, CallStatus status) {
    public CallSnapshot {
        Objects.requireNonNull(call, "call");
        Objects.requireNonNull(status, "status");
    }
}

```

## plugin-core/src/main/java/com/github/catatafishen/agentbridge/nativeagent/lifecycle/CallStatus.java
sha256: 22e39f808f5e70c13e4191532e3795485871fcf8c3865ab8000518cdb27eb6fa
```
package com.github.catatafishen.agentbridge.nativeagent.lifecycle;

public enum CallStatus {
    PENDING,
    EXECUTING,
    COMPLETED,
    FAILED_AFTER_START,
    CANCELLED_BEFORE_START;

    public boolean isTerminal() {
        return switch (this) {
            case COMPLETED, FAILED_AFTER_START, CANCELLED_BEFORE_START -> true;
            case PENDING, EXECUTING -> false;
        };
    }
}

```

## plugin-core/src/main/java/com/github/catatafishen/agentbridge/nativeagent/lifecycle/Effect.java
sha256: 676d6f87ffc987b72a04be0b161f13d1cadda2438960222933078ea98f7b2c11
```
package com.github.catatafishen.agentbridge.nativeagent.lifecycle;

@FunctionalInterface
public interface Effect {
    void execute();
}

```

## plugin-core/src/main/java/com/github/catatafishen/agentbridge/nativeagent/lifecycle/LifecyclePhase.java
sha256: 467a478ba26ef5bb5ffb2f0dc12b985bbd3b7bd73e3dc4346c3b943f3ccbf564
```
package com.github.catatafishen.agentbridge.nativeagent.lifecycle;

public enum LifecyclePhase {
    IDLE,
    RUNNING,
    STOPPING,
    CLOSING,
    CLOSED
}

```

## plugin-core/src/main/java/com/github/catatafishen/agentbridge/nativeagent/lifecycle/LifecycleSnapshot.java
sha256: a506c28ccc375545c7c66a2818a019ca5c798af466ad8fb086ac1ecef6ee3a10
```
package com.github.catatafishen.agentbridge.nativeagent.lifecycle;

import java.util.Objects;

public sealed interface LifecycleSnapshot {
    LifecyclePhase phase();

    record Idle() implements LifecycleSnapshot {
        @Override
        public LifecyclePhase phase() {
            return LifecyclePhase.IDLE;
        }
    }

    record Active(LifecyclePhase phase) implements LifecycleSnapshot {
        public Active {
            Objects.requireNonNull(phase, "phase");
            if (phase != LifecyclePhase.RUNNING
                && phase != LifecyclePhase.STOPPING
                && phase != LifecyclePhase.CLOSING) {
                throw new IllegalArgumentException("Active snapshot requires an active phase");
            }
        }
    }

    record Closed() implements LifecycleSnapshot {
        @Override
        public LifecyclePhase phase() {
            return LifecyclePhase.CLOSED;
        }
    }
}

```

## plugin-core/src/main/java/com/github/catatafishen/agentbridge/nativeagent/lifecycle/RunHandle.java
sha256: b0cc0ab302e140debdf5e94989bef85ded86789d373ee1a2037c5134ceec7213
```
package com.github.catatafishen.agentbridge.nativeagent.lifecycle;

public final class RunHandle {
    private final Object ownerKey;

    RunHandle(Object ownerKey) {
        this.ownerKey = ownerKey;
    }

    boolean belongsTo(Object expectedOwnerKey) {
        return ownerKey == expectedOwnerKey;
    }
}

```

## plugin-core/src/main/java/com/github/catatafishen/agentbridge/nativeagent/lifecycle/RunLifecycle.java
sha256: beff8dc4313213af3415fd2a9739214d983fcb5991ec65f7b83f0c66828537d8
```
package com.github.catatafishen.agentbridge.nativeagent.lifecycle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class RunLifecycle {
    private final Object ownerKey = new Object();
    private final Set<CallId> acceptedCallIds = new HashSet<>();

    private LifecyclePhase phase = LifecyclePhase.IDLE;
    private RunHandle currentRun;
    private BatchState currentBatch;

    public synchronized StartRunResult startRun() {
        if (phase != LifecyclePhase.IDLE) {
            return new StartRunResult.Rejected(startRejection());
        }
        currentRun = new RunHandle(ownerKey);
        currentBatch = null;
        acceptedCallIds.clear();
        phase = LifecyclePhase.RUNNING;
        return new StartRunResult.Started(currentRun);
    }

    public synchronized BeginBatchResult beginBatch(RunHandle run, CallBatch batch) {
        Objects.requireNonNull(run, "run");
        Objects.requireNonNull(batch, "batch");
        if (isStaleRun(run)) {
            return new BeginBatchResult.Rejected(BatchRejection.STALE_RUN);
        }
        if (phase != LifecyclePhase.RUNNING) {
            return new BeginBatchResult.Rejected(BatchRejection.RUN_NOT_ACCEPTING_BATCHES);
        }
        if (currentBatch != null && currentBatch.hasUnsettledCalls()) {
            return new BeginBatchResult.Rejected(BatchRejection.PREVIOUS_BATCH_UNSETTLED);
        }
        if (batch.calls().stream().anyMatch(acceptedCallIds::contains)) {
            return new BeginBatchResult.Rejected(BatchRejection.CALL_ID_ALREADY_ACCEPTED);
        }

        currentBatch = new BatchState(new BatchHandle(ownerKey, run), batch.calls());
        acceptedCallIds.addAll(batch.calls());
        return new BeginBatchResult.Begun(currentBatch.handle);
    }

    public ExecutionResult execute(BatchHandle batch, CallId call, Effect effect) {
        Objects.requireNonNull(batch, "batch");
        Objects.requireNonNull(call, "call");
        Objects.requireNonNull(effect, "effect");

        synchronized (this) {
            ExecutionRejection rejection = rejectExecution(batch, call);
            if (rejection != null) {
                return new ExecutionResult.Rejected(rejection);
            }
            currentBatch.markExecuting(call);
        }

        try {
            effect.execute();
            complete(batch, call, CallStatus.COMPLETED);
            return new ExecutionResult.Executed();
        } catch (RuntimeException | Error failure) {
            recordFailure(batch, call, failure);
            throw failure;
        }
    }

    public synchronized StopResult stop(RunHandle run) {
        Objects.requireNonNull(run, "run");
        if (isStaleRun(run)) {
            return new StopResult.Rejected(StopRejection.STALE_RUN);
        }
        if (phase == LifecyclePhase.RUNNING) {
            phase = LifecyclePhase.STOPPING;
            cancelPending();
        } else if (phase != LifecyclePhase.STOPPING && phase != LifecyclePhase.CLOSING) {
            return new StopResult.Rejected(StopRejection.RUN_NOT_ACTIVE);
        }
        return new StopResult.Acknowledged(snapshot(), currentBatchObservation());
    }

    public synchronized FinishRunResult finishRun(RunHandle run) {
        Objects.requireNonNull(run, "run");
        if (isStaleRun(run)) {
            return new FinishRunResult.Rejected(FinishRejection.STALE_RUN);
        }
        if (currentBatch != null && currentBatch.hasUnsettledCalls()) {
            return new FinishRunResult.Rejected(FinishRejection.BATCH_UNSETTLED);
        }

        phase = phase == LifecyclePhase.CLOSING ? LifecyclePhase.CLOSED : LifecyclePhase.IDLE;
        currentRun = null;
        currentBatch = null;
        acceptedCallIds.clear();
        return new FinishRunResult.Finished(snapshot());
    }

    public synchronized LifecycleSnapshot close() {
        if (phase == LifecyclePhase.CLOSED || phase == LifecyclePhase.CLOSING) {
            return snapshot();
        }
        if (phase == LifecyclePhase.IDLE) {
            phase = LifecyclePhase.CLOSED;
        } else {
            phase = LifecyclePhase.CLOSING;
            cancelPending();
        }
        return snapshot();
    }

    public synchronized LifecycleSnapshot snapshot() {
        return switch (phase) {
            case IDLE -> new LifecycleSnapshot.Idle();
            case RUNNING, STOPPING, CLOSING -> new LifecycleSnapshot.Active(phase);
            case CLOSED -> new LifecycleSnapshot.Closed();
        };
    }

    public synchronized BatchSnapshotResult batchSnapshot(BatchHandle handle) {
        Objects.requireNonNull(handle, "handle");
        if (isStaleBatch(handle)) {
            return new BatchSnapshotResult.Rejected(BatchSnapshotRejection.STALE_BATCH);
        }
        return new BatchSnapshotResult.Available(currentBatch.snapshot());
    }

    private void complete(BatchHandle batch, CallId call, CallStatus status) {
        synchronized (this) {
            currentBatch.ensureCurrent(batch, call);
            currentBatch.markTerminal(call, status);
        }
    }

    private void recordFailure(BatchHandle batch, CallId call, Throwable failure) {
        try {
            complete(batch, call, CallStatus.FAILED_AFTER_START);
        } catch (RuntimeException | Error accountingFailure) {
            if (accountingFailure != failure) {
                failure.addSuppressed(accountingFailure);
            }
        }
    }

    private StartRejection startRejection() {
        return switch (phase) {
            case RUNNING, STOPPING -> StartRejection.BUSY;
            case CLOSING, CLOSED -> StartRejection.CLOSED;
            case IDLE -> throw new IllegalStateException("Idle run should have started");
        };
    }

    private ExecutionRejection rejectExecution(BatchHandle batch, CallId call) {
        if (isStaleBatch(batch)) {
            return ExecutionRejection.STALE_BATCH;
        }
        if (phase != LifecyclePhase.RUNNING) {
            return ExecutionRejection.RUN_NOT_ACCEPTING_EFFECTS;
        }
        CallStatus target = currentBatch.status(call);
        if (target == null) {
            return ExecutionRejection.UNKNOWN_CALL;
        }
        if (target == CallStatus.EXECUTING) {
            return ExecutionRejection.ALREADY_EXECUTING;
        }
        if (target.isTerminal()) {
            return ExecutionRejection.ALREADY_TERMINAL;
        }
        CallId next = currentBatch.nextUnsettled();
        if (next == null || !next.equals(call)) {
            return ExecutionRejection.OUT_OF_ORDER;
        }
        return null;
    }

    private boolean isStaleRun(RunHandle run) {
        return !run.belongsTo(ownerKey) || run != currentRun;
    }

    private boolean isStaleBatch(BatchHandle batch) {
        return !batch.belongsTo(ownerKey) || isStaleRun(batch.run())
            || currentBatch == null || currentBatch.handle != batch;
    }

    private void cancelPending() {
        if (currentBatch != null) {
            currentBatch.cancelPending();
        }
    }

    private BatchObservation currentBatchObservation() {
        return currentBatch == null
            ? new BatchObservation.Absent()
            : new BatchObservation.Present(currentBatch.snapshot());
    }

    public enum StartRejection { BUSY, CLOSED }
    public enum BatchRejection { STALE_RUN, RUN_NOT_ACCEPTING_BATCHES, PREVIOUS_BATCH_UNSETTLED, CALL_ID_ALREADY_ACCEPTED }
    public enum ExecutionRejection { STALE_BATCH, RUN_NOT_ACCEPTING_EFFECTS, UNKNOWN_CALL, ALREADY_EXECUTING, ALREADY_TERMINAL, OUT_OF_ORDER }
    public enum StopRejection { STALE_RUN, RUN_NOT_ACTIVE }
    public enum FinishRejection { STALE_RUN, BATCH_UNSETTLED }
    public enum BatchSnapshotRejection { STALE_BATCH }

    public sealed interface StartRunResult {
        record Started(RunHandle run) implements StartRunResult { public Started { Objects.requireNonNull(run); } }
        record Rejected(StartRejection reason) implements StartRunResult { public Rejected { Objects.requireNonNull(reason); } }
    }

    public sealed interface BeginBatchResult {
        record Begun(BatchHandle batch) implements BeginBatchResult { public Begun { Objects.requireNonNull(batch); } }
        record Rejected(BatchRejection reason) implements BeginBatchResult { public Rejected { Objects.requireNonNull(reason); } }
    }

    public sealed interface ExecutionResult {
        record Executed() implements ExecutionResult { }
        record Rejected(ExecutionRejection reason) implements ExecutionResult { public Rejected { Objects.requireNonNull(reason); } }
    }

    public sealed interface StopResult {
        record Acknowledged(LifecycleSnapshot lifecycle, BatchObservation batch) implements StopResult {
            public Acknowledged {
                Objects.requireNonNull(lifecycle);
                Objects.requireNonNull(batch);
            }
        }
        record Rejected(StopRejection reason) implements StopResult { public Rejected { Objects.requireNonNull(reason); } }
    }

    public sealed interface FinishRunResult {
        record Finished(LifecycleSnapshot lifecycle) implements FinishRunResult { public Finished { Objects.requireNonNull(lifecycle); } }
        record Rejected(FinishRejection reason) implements FinishRunResult { public Rejected { Objects.requireNonNull(reason); } }
    }

    public sealed interface BatchSnapshotResult {
        record Available(BatchSnapshot snapshot) implements BatchSnapshotResult {
            public Available { Objects.requireNonNull(snapshot); }
        }
        record Rejected(BatchSnapshotRejection reason) implements BatchSnapshotResult {
            public Rejected { Objects.requireNonNull(reason); }
        }
    }

    private static final class BatchState {
        private final BatchHandle handle;
        private final List<CallId> order;
        private final Map<CallId, CallStatus> statuses;

        private BatchState(BatchHandle handle, List<CallId> calls) {
            this.handle = handle;
            this.order = List.copyOf(calls);
            this.statuses = new LinkedHashMap<>();
            calls.forEach(call -> statuses.put(call, CallStatus.PENDING));
        }

        private CallStatus status(CallId call) {
            return statuses.get(call);
        }

        private void markExecuting(CallId call) {
            mark(call, CallStatus.EXECUTING);
        }

        private void markTerminal(CallId call, CallStatus status) {
            if (!status.isTerminal() || statuses.get(call) != CallStatus.EXECUTING) {
                throw new IllegalStateException("Call is not executing");
            }
            mark(call, status);
        }

        private void ensureCurrent(BatchHandle expectedHandle, CallId call) {
            if (handle != expectedHandle || statuses.get(call) != CallStatus.EXECUTING) {
                throw new IllegalStateException("Executing call is no longer current");
            }
        }

        private void mark(CallId call, CallStatus status) {
            if (!statuses.containsKey(call)) {
                throw new IllegalStateException("Unknown accepted call");
            }
            statuses.put(call, status);
        }

        private CallId nextUnsettled() {
            return order.stream()
                .filter(call -> !statuses.get(call).isTerminal())
                .findFirst()
                .orElse(null);
        }

        private void cancelPending() {
            order.stream()
                .filter(call -> statuses.get(call) == CallStatus.PENDING)
                .forEach(call -> statuses.put(call, CallStatus.CANCELLED_BEFORE_START));
        }

        private boolean hasUnsettledCalls() {
            return statuses.values().stream().anyMatch(status -> !status.isTerminal());
        }

        private BatchSnapshot snapshot() {
            List<CallSnapshot> snapshot = new ArrayList<>(order.size());
            order.forEach(call -> snapshot.add(new CallSnapshot(call, statuses.get(call))));
            return new BatchSnapshot(snapshot);
        }
    }
}

```

## plugin-core/src/test/java/com/github/catatafishen/agentbridge/nativeagent/lifecycle/RunLifecycleTest.java
sha256: 84ee30c5014041723d597851789620166556433de32286d3c0668f82b7621744
```
package com.github.catatafishen.agentbridge.nativeagent.lifecycle;

import org.junit.jupiter.api.Test;

import java.awt.EventQueue;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunLifecycleTest {
    @Test
    void singleRunOwnership() {
        RunLifecycle lifecycle = new RunLifecycle();
        RunHandle first = startedRun(lifecycle);

        RunLifecycle.StartRunResult busy = lifecycle.startRun();
        assertEquals(new RunLifecycle.StartRunResult.Rejected(RunLifecycle.StartRejection.BUSY), busy);
        assertInstanceOf(RunLifecycle.FinishRunResult.Finished.class, lifecycle.finishRun(first));
        assertInstanceOf(RunLifecycle.StartRunResult.Started.class, lifecycle.startRun());
    }

    @Test
    void validatedImmutableBatch() {
        CallId first = new CallId("first");
        CallId duplicate = new CallId("duplicate");
        assertThrows(IllegalArgumentException.class, () -> CallBatch.of(List.of()));
        assertThrows(NullPointerException.class, () -> CallBatch.of(null));
        assertThrows(IllegalArgumentException.class, () -> CallBatch.of(List.of(duplicate, duplicate)));
        assertThrows(NullPointerException.class, () -> CallBatch.of(Arrays.asList(first, null)));

        var input = new java.util.ArrayList<>(List.of(first));
        CallBatch batch = CallBatch.of(input);
        input.clear();
        assertEquals(List.of(first), batch.calls());
        assertThrows(UnsupportedOperationException.class, () -> batch.calls().clear());
    }

    @Test
    void orderedExclusiveExecutionAndDuplicateAdmission() throws Exception {
        RunLifecycle lifecycle = new RunLifecycle();
        RunHandle run = startedRun(lifecycle);
        CallId first = new CallId("first");
        CallId second = new CallId("second");
        BatchHandle batch = begunBatch(lifecycle, run, first, second);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger firstEntries = new AtomicInteger();

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<RunLifecycle.ExecutionResult> firstExecution = executor.submit(
                () -> lifecycle.execute(batch, first, () -> {
                    firstEntries.incrementAndGet();
                    entered.countDown();
                    await(release);
                }));
            assertTrue(entered.await(5, TimeUnit.SECONDS));

            RunLifecycle.ExecutionResult duplicate = lifecycle.execute(batch, first, firstEntries::incrementAndGet);
            RunLifecycle.ExecutionResult outOfOrder = lifecycle.execute(batch, second, firstEntries::incrementAndGet);
            assertEquals(new RunLifecycle.ExecutionResult.Rejected(RunLifecycle.ExecutionRejection.ALREADY_EXECUTING), duplicate);
            assertEquals(new RunLifecycle.ExecutionResult.Rejected(RunLifecycle.ExecutionRejection.OUT_OF_ORDER), outOfOrder);

            release.countDown();
            assertInstanceOf(RunLifecycle.ExecutionResult.Executed.class, firstExecution.get(5, TimeUnit.SECONDS));
            assertInstanceOf(RunLifecycle.ExecutionResult.Executed.class,
                lifecycle.execute(batch, second, firstEntries::incrementAndGet));
            assertEquals(2, firstEntries.get());
            assertEquals(new RunLifecycle.ExecutionResult.Rejected(RunLifecycle.ExecutionRejection.ALREADY_TERMINAL),
                lifecycle.execute(batch, first, firstEntries::incrementAndGet));
        }
    }

    @Test
    void stopBeforeQueuedEffect() throws Exception {
        RunLifecycle lifecycle = new RunLifecycle();
        RunHandle run = startedRun(lifecycle);
        CallId call = new CallId("queued");
        BatchHandle batch = begunBatch(lifecycle, run, call);
        CountDownLatch queueGate = new CountDownLatch(1);
        AtomicInteger entries = new AtomicInteger();

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            executor.submit(() -> await(queueGate));
            Future<RunLifecycle.ExecutionResult> queued = executor.submit(
                () -> lifecycle.execute(batch, call, entries::incrementAndGet));
            RunLifecycle.StopResult stopped = lifecycle.stop(run);
            queueGate.countDown();
            assertEquals(0, entries.get());
            assertEquals(LifecyclePhase.STOPPING, stoppedPhase(stopped));
            assertEquals(new RunLifecycle.ExecutionResult.Rejected(RunLifecycle.ExecutionRejection.RUN_NOT_ACCEPTING_EFFECTS),
                queued.get(5, TimeUnit.SECONDS));
            assertEquals(CallStatus.CANCELLED_BEFORE_START, batchStatus(lifecycle, batch, call));
        }
    }

    @Test
    void stopDuringEffect() throws Exception {
        RunLifecycle lifecycle = new RunLifecycle();
        RunHandle run = startedRun(lifecycle);
        CallId first = new CallId("first");
        CallId second = new CallId("second");
        BatchHandle batch = begunBatch(lifecycle, run, first, second);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<RunLifecycle.ExecutionResult> executing = executor.submit(
                () -> lifecycle.execute(batch, first, () -> {
                    entered.countDown();
                    await(release);
                }));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            assertInstanceOf(RunLifecycle.StopResult.Acknowledged.class, lifecycle.stop(run));
            assertEquals(new RunLifecycle.StartRunResult.Rejected(RunLifecycle.StartRejection.BUSY), lifecycle.startRun());
            assertEquals(new RunLifecycle.FinishRunResult.Rejected(RunLifecycle.FinishRejection.BATCH_UNSETTLED), lifecycle.finishRun(run));
            release.countDown();
            assertInstanceOf(RunLifecycle.ExecutionResult.Executed.class, executing.get(5, TimeUnit.SECONDS));
            assertEquals(CallStatus.COMPLETED, batchStatus(lifecycle, batch, first));
            assertEquals(CallStatus.CANCELLED_BEFORE_START, batchStatus(lifecycle, batch, second));
            assertInstanceOf(RunLifecycle.FinishRunResult.Finished.class, lifecycle.finishRun(run));
        }
    }

    @Test
    void terminalAccounting() {
        assertTerminalStatus(() -> { });
        RuntimeException runtime = new RuntimeException("runtime");
        assertTerminalStatus(CallStatus.FAILED_AFTER_START, () -> { throw runtime; }, runtime);
        AssertionError error = new AssertionError("error");
        assertTerminalStatus(CallStatus.FAILED_AFTER_START, () -> { throw error; }, error);
    }

    @Test
    void staleHandleIsolation() {
        RunLifecycle firstLifecycle = new RunLifecycle();
        RunLifecycle secondLifecycle = new RunLifecycle();
        RunHandle firstRun = startedRun(firstLifecycle);
        RunHandle secondRun = startedRun(secondLifecycle);
        CallId firstCall = new CallId("first");
        BatchHandle firstBatch = begunBatch(firstLifecycle, firstRun, firstCall);

        assertEquals(new RunLifecycle.BeginBatchResult.Rejected(RunLifecycle.BatchRejection.STALE_RUN),
            secondLifecycle.beginBatch(firstRun, CallBatch.of(List.of(new CallId("foreign")))));
        lifecycleExecute(firstLifecycle, firstBatch, firstCall);
        BatchSnapshot captured = availableSnapshot(firstLifecycle.batchSnapshot(firstBatch));
        CallId secondCall = new CallId("second");
        BatchHandle secondBatch = begunBatch(firstLifecycle, firstRun, secondCall);
        assertEquals(new RunLifecycle.BatchSnapshotResult.Rejected(RunLifecycle.BatchSnapshotRejection.STALE_BATCH),
            firstLifecycle.batchSnapshot(firstBatch));
        assertEquals(List.of(new CallSnapshot(firstCall, CallStatus.COMPLETED)), captured.calls());
        assertNotNull(secondRun);
        assertNotNull(secondBatch);
    }

    @Test
    void closeDrainsWithoutReopening() throws Exception {
        RunLifecycle lifecycle = new RunLifecycle();
        RunHandle run = startedRun(lifecycle);
        CallId active = new CallId("active");
        CallId pending = new CallId("pending");
        BatchHandle batch = begunBatch(lifecycle, run, active, pending);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<RunLifecycle.ExecutionResult> execution = executor.submit(
                () -> lifecycle.execute(batch, active, () -> {
                    entered.countDown();
                    await(release);
                }));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            lifecycle.stop(run);
            assertEquals(LifecyclePhase.CLOSING, lifecycle.close().phase());
            assertEquals(LifecyclePhase.CLOSING, lifecycle.close().phase());
            release.countDown();
            execution.get(5, TimeUnit.SECONDS);
            assertEquals(LifecyclePhase.CLOSED, finishedPhase(lifecycle.finishRun(run)));
            assertEquals(new RunLifecycle.StartRunResult.Rejected(RunLifecycle.StartRejection.CLOSED), lifecycle.startRun());
        }
    }

    @Test
    void nullInputDoesNotMutate() {
        RunLifecycle lifecycle = new RunLifecycle();
        assertThrows(NullPointerException.class, () -> new CallId(null));
        assertThrows(NullPointerException.class, () -> lifecycle.beginBatch(null, CallBatch.of(List.of(new CallId("x")))));
        assertThrows(NullPointerException.class, () -> lifecycle.execute(null, new CallId("x"), () -> { }));
        assertThrows(NullPointerException.class, () -> lifecycle.stop(null));
        assertThrows(NullPointerException.class, () -> lifecycle.finishRun(null));
        assertThrows(NullPointerException.class, () -> lifecycle.batchSnapshot(null));
        assertEquals(new LifecycleSnapshot.Idle(), lifecycle.snapshot());
    }

    @Test
    void nonblockingStopAndImmutableSnapshots() throws Exception {
        RunLifecycle lifecycle = new RunLifecycle();
        RunHandle run = startedRun(lifecycle);
        CallId call = new CallId("call");
        BatchHandle batch = begunBatch(lifecycle, run, call);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<RunLifecycle.ExecutionResult> execution = executor.submit(() -> lifecycle.execute(batch, call, () -> {
                entered.countDown();
                await(release);
            }));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            BatchSnapshot before = availableSnapshot(lifecycle.batchSnapshot(batch));
            assertEquals(CallStatus.EXECUTING, before.calls().getFirst().status());
            assertEquals(LifecyclePhase.STOPPING, stoppedPhase(lifecycle.stop(run)));
            release.countDown();
            execution.get(5, TimeUnit.SECONDS);
            assertEquals(CallStatus.EXECUTING, before.calls().getFirst().status());
            assertEquals(CallStatus.COMPLETED, batchStatus(lifecycle, batch, call));
        }
    }

    @Test
    void multipleBatchesPreserveIdentity() {
        RunLifecycle lifecycle = new RunLifecycle();
        RunHandle run = startedRun(lifecycle);
        CallId first = new CallId("first");
        CallId second = new CallId("second");
        CallId fresh = new CallId("fresh");
        BatchHandle firstBatch = begunBatch(lifecycle, run, first, second);
        assertEquals(new RunLifecycle.FinishRunResult.Rejected(RunLifecycle.FinishRejection.BATCH_UNSETTLED), lifecycle.finishRun(run));
        lifecycleExecute(lifecycle, firstBatch, first);
        lifecycleExecute(lifecycle, firstBatch, second);
        assertInstanceOf(RunLifecycle.FinishRunResult.Finished.class, lifecycle.finishRun(run));

        RunHandle nextRun = startedRun(lifecycle);
        BatchHandle nextBatch = begunBatch(lifecycle, nextRun, fresh);
        lifecycleExecute(lifecycle, nextBatch, fresh);
        assertEquals(new RunLifecycle.BeginBatchResult.Rejected(RunLifecycle.BatchRejection.CALL_ID_ALREADY_ACCEPTED),
            lifecycle.beginBatch(nextRun, CallBatch.of(List.of(fresh, first))));
        BatchHandle freshBatch = begunBatch(lifecycle, nextRun, new CallId("new"));
        lifecycleExecute(lifecycle, freshBatch, new CallId("new"));
        assertEquals(LifecyclePhase.IDLE, finishedPhase(lifecycle.finishRun(nextRun)));
        assertNotNull(nextBatch);
    }

    @Test
    void awtAdmissionSmoke() throws Exception {
        RunLifecycle lifecycle = new RunLifecycle();
        RunHandle run = startedRun(lifecycle);
        CallId call = new CallId("awt");
        BatchHandle batch = begunBatch(lifecycle, run, call);
        CountDownLatch edtBlocked = new CountDownLatch(1);
        CountDownLatch releaseEdt = new CountDownLatch(1);
        EventQueue.invokeLater(() -> {
            edtBlocked.countDown();
            await(releaseEdt);
        });
        assertTrue(edtBlocked.await(5, TimeUnit.SECONDS));
        AtomicInteger entries = new AtomicInteger();
        FutureTaskResult queued = new FutureTaskResult();
        EventQueue.invokeLater(() -> queued.set(lifecycle.execute(batch, call, entries::incrementAndGet)));
        RunLifecycle.StopResult stopBefore = lifecycle.stop(run);
        releaseEdt.countDown();
        assertEquals(0, entries.get());
        assertEquals(new RunLifecycle.ExecutionResult.Rejected(RunLifecycle.ExecutionRejection.RUN_NOT_ACCEPTING_EFFECTS), queued.get());
        assertEquals(CallStatus.CANCELLED_BEFORE_START, batchStatus(lifecycle, batch, call));
        assertEquals(LifecyclePhase.STOPPING, stoppedPhase(stopBefore));

        RunLifecycle admittedLifecycle = new RunLifecycle();
        RunHandle admittedRun = startedRun(admittedLifecycle);
        CallId admittedCall = new CallId("admitted");
        BatchHandle admittedBatch = begunBatch(admittedLifecycle, admittedRun, admittedCall);
        CountDownLatch effectEntered = new CountDownLatch(1);
        CountDownLatch releaseEffect = new CountDownLatch(1);
        FutureTaskResult admittedResult = new FutureTaskResult();
        EventQueue.invokeLater(() -> admittedResult.set(admittedLifecycle.execute(admittedBatch, admittedCall, () -> {
            effectEntered.countDown();
            await(releaseEffect);
        })));
        assertTrue(effectEntered.await(5, TimeUnit.SECONDS));
        assertEquals(LifecyclePhase.STOPPING, stoppedPhase(admittedLifecycle.stop(admittedRun)));
        releaseEffect.countDown();
        assertEquals(new RunLifecycle.ExecutionResult.Executed(), admittedResult.get());
        assertEquals(CallStatus.COMPLETED, batchStatus(admittedLifecycle, admittedBatch, admittedCall));
    }

    private static void assertTerminalStatus(Effect effect) {
        assertTerminalStatus(CallStatus.COMPLETED, effect, null);
    }

    private static void assertTerminalStatus(CallStatus expected, Effect effect, Throwable thrown) {
        RunLifecycle lifecycle = new RunLifecycle();
        RunHandle run = startedRun(lifecycle);
        CallId call = new CallId("call");
        BatchHandle batch = begunBatch(lifecycle, run, call);
        if (thrown == null) {
            assertInstanceOf(RunLifecycle.ExecutionResult.Executed.class, lifecycle.execute(batch, call, effect));
        } else {
            Throwable observed = assertThrows(thrown.getClass(), () -> lifecycle.execute(batch, call, effect));
            assertSame(thrown, observed);
        }
        assertEquals(expected, batchStatus(lifecycle, batch, call));
        RunLifecycle.ExecutionResult duplicate = lifecycle.execute(batch, call, () -> { });
        assertEquals(new RunLifecycle.ExecutionResult.Rejected(RunLifecycle.ExecutionRejection.ALREADY_TERMINAL), duplicate);
    }

    private static RunHandle startedRun(RunLifecycle lifecycle) {
        return ((RunLifecycle.StartRunResult.Started) lifecycle.startRun()).run();
    }

    private static BatchHandle begunBatch(RunLifecycle lifecycle, RunHandle run, CallId... calls) {
        return ((RunLifecycle.BeginBatchResult.Begun) lifecycle.beginBatch(run, CallBatch.of(List.of(calls)))).batch();
    }

    private static void lifecycleExecute(RunLifecycle lifecycle, BatchHandle batch, CallId call) {
        assertInstanceOf(RunLifecycle.ExecutionResult.Executed.class, lifecycle.execute(batch, call, () -> { }));
    }

    private static BatchSnapshot availableSnapshot(RunLifecycle.BatchSnapshotResult result) {
        return ((RunLifecycle.BatchSnapshotResult.Available) result).snapshot();
    }

    private static CallStatus batchStatus(RunLifecycle lifecycle, BatchHandle batch, CallId call) {
        return availableSnapshot(lifecycle.batchSnapshot(batch)).calls().stream()
            .filter(snapshot -> snapshot.call().equals(call))
            .findFirst()
            .orElseThrow()
            .status();
    }

    private static LifecyclePhase stoppedPhase(RunLifecycle.StopResult result) {
        return ((RunLifecycle.StopResult.Acknowledged) result).lifecycle().phase();
    }

    private static LifecyclePhase finishedPhase(RunLifecycle.FinishRunResult result) {
        return ((RunLifecycle.FinishRunResult.Finished) result).lifecycle().phase();
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Latch was not released");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }

    private static final class FutureTaskResult {
        private final CountDownLatch done = new CountDownLatch(1);
        private volatile RunLifecycle.ExecutionResult result;

        private synchronized void set(RunLifecycle.ExecutionResult result) {
            this.result = result;
            done.countDown();
        }

        private RunLifecycle.ExecutionResult get() throws InterruptedException {
            assertTrue(done.await(5, TimeUnit.SECONDS));
            return result;
        }
    }
}

```
