We are designing an LLM-driven development workflow for an IntelliJ-native minimal coding-agent harness, roughly matching Pi's feature set.

The primary goal is NOT elaborate formal specification, documentation, code generation, or process ceremony.

The goal is:

**Reduce the design/search space presented to the implementation LLM as aggressively as practical.**

The important principles are:

1. Spec-driven development
2. Type-driven design
3. Unsound boundary / type-safe core
4. Make illegal states unrepresentable
5. Deterministic workflows and state transitions
6. Minimal implementation freedom
7. Internal specification consistency must be checked before implementation
8. Inconsistency or unresolved ambiguity must cause a tool/workflow failure, not an LLM guess
9. Prefer compiler/type/test/architecture enforcement over prose instructions
10. Avoid maintaining two parallel implementations of the same truth

## Desired workflow

Use this basic pipeline:

```
reference behavior / requirement
            ↓
     normalized feature spec
            ↓
     CONSISTENCY GATE
            ↓
      type/API design
            ↓
    SPEC ↔ DESIGN GATE
            ↓
  frozen implementation surface
            ↓
    implementation LLM
            ↓
compiler/tests/architecture checks
```

The implementation LLM should normally work only after the design surface has been constrained.

## 1. Specification

Do not treat a large Markdown document as the authoritative specification.

The spec should be a small normalized set of facts for each feature.

Each feature should contain approximately:

* stable feature ID
* reference evidence, where applicable
* externally observable behavior
* inputs / outputs
* decisions
* invariants
* relevant states/events/transitions
* dependencies
* explicit non-goals
* acceptance criteria

Normative facts should have one canonical definition.

Do not independently redefine the same rule in multiple prose sections.

For example, do not allow:

```
§5.4: tools execute sequentially
§7.2: independent tools execute concurrently
```

Instead there should be one canonical decision such as:

```
TOOL_EXECUTION_CONCURRENCY = SEQUENTIAL
```

Other artifacts may reference it but must not redefine it.

## 2. Consistency is a hard gate

Before type design or implementation begins, mechanically check as much specification consistency as possible.

Failures must STOP the workflow.

Do not allow the LLM to "interpret", reconcile, or choose between inconsistent requirements.

At minimum detect:

* duplicate/conflicting decisions
* duplicate IDs
* undefined references
* invalid state/event/transition references
* impossible or malformed transition graphs
* forbidden dependency cycles
* unreachable states when inappropriate
* missing terminal paths when required
* feature/invariant without an enforcement strategy
* requirement without acceptance criteria where acceptance is expected
* directly contradictory structured invariants
* unresolved TODO/TBD/ambiguous normative decisions

For semantic contradictions that cannot be mechanically established, a separate LLM review may be used as a semantic linter.

Its job is only to identify:

* mutually incompatible requirements
* two different outcomes for the same state/event
* missing behavior
* ambiguous ownership
* underspecified decisions

It must not silently repair them.

Any material inconsistency becomes an explicit failed result requiring the design/spec phase to resolve it.

## 3. Type-driven design

After the spec passes consistency checks, consume it into the implementation design.

Use:

* sealed interfaces
* records/value types
* exhaustive switches
* constrained constructors
* non-empty collection types where meaningful
* state-specific types
* capability-oriented interfaces
* package/module visibility
* explicit transition APIs

Follow:

```
UNSOUND BOUNDARY
       ↓ validation/parsing
   TYPE-SAFE CORE
```

Treat these as unsound boundaries:

* LLM output
* JSON
* provider SDKs
* IntelliJ platform callbacks
* filesystem data
* persisted sessions
* MCP/external tool responses
* user-entered unstructured values

Raw strings, JSON nodes, nullable SDK objects, maps, etc. should not leak into the core when a validated domain type can represent them.

The implementation core should make illegal states unrepresentable where doing so remains simple and legible.

Do not perform type gymnastics merely to claim stronger typing.

## 4. Every invariant needs an enforcement location

For every invariant, explicitly classify its enforcement:

```
TYPE
API
ARCHITECTURE
TEST
RUNTIME
MODEL-CHECKER
```

Prefer the strongest simple mechanism nearest to the implementation.

Examples:

```
"WaitingForTools always has at least one ToolCall"
    → TYPE

"agent-core cannot depend on IntelliJ UI"
    → ARCHITECTURE / ArchUnit

"stopped agent ignores/rejects further events"
    → TYPE/API if practical, otherwise TEST

"only one active model request"
    → state model + TEST/runtime assertion

"cancellation always terminates under all asynchronous interleavings"
    → potentially MODEL-CHECKER if this becomes genuinely difficult
```

No normative invariant may remain as prose with no identified enforcement mechanism.

Failure to map an invariant to enforcement is a design failure.

## 5. Spec-to-design consistency

After producing types/interfaces/tests, review whether every relevant spec constraint has actually been represented.

For every important decision/invariant, answer:

```
Which Java type/interface/test/architecture rule enforces this?
```

If there is no answer, fail the design gate.

Conversely, flag major design semantics that have appeared in the code model but have no basis in the specification.

The goal is not traceability bureaucracy.

The goal is to ensure that the LLM cannot quietly invent architecture while translating prose into code.

## 6. Freeze the implementation surface

Once the design is approved, the implementation model should receive a narrow task.

Prefer tasks like:

```
Implement DefaultToolExecutor.

Relevant types:
  ToolExecutor
  ToolCall
  ToolResult
  AgentState.WaitingForTools

Required behavior:
  CAP-023
  INV-017
  INV-021

Required tests:
  ToolExecutorContractTest

Allowed package:
  agent.core.tool

Public types/interfaces are frozen.

Do not:
  add new abstractions
  alter the state model
  modify unrelated packages
  change specification decisions
```

Rather than:

```
Implement tool execution according to the design document.
```

The implementation model should fill holes inside an already-shaped system.

## 7. Determinism

The development workflow itself should be deterministic.

Use explicit states such as:

```
SPEC_DRAFT
SPEC_INVALID
SPEC_VALID
DESIGN_DRAFT
DESIGN_INVALID
DESIGN_VALID
IMPLEMENTING
VERIFYING
DONE
```

Transitions should have explicit preconditions and postconditions.

Example:

```
SPEC_DRAFT → SPEC_VALID
```

only if:

```
spec validator passes
semantic review has no unresolved blocking findings
```

A failed gate should transition to a defined failure/revision state.

Do not encode:

```
if something seems questionable, ask the coding LLM to use judgment
```

for normative design questions.

## 8. Do not build unnecessary formal machinery

Do not start by creating:

* a generic specification language
* a second executable implementation of the agent
* automatic production-code generation from the spec
* a giant knowledge graph
* a theorem prover integration
* a permanent duplicate state-machine model

Use ordinary mechanisms first:

* normalized YAML/JSON/org/spec artifact if needed
* a small validator/linter
* Java compiler
* Java type system
* tests
* ArchUnit/module dependency checks
* IntelliJ semantic/index information

Only introduce Quint/TLA+/Alloy/SMT/model checking for a concrete problem that ordinary typing and testing cannot cheaply answer, such as difficult concurrency, cancellation, streaming, or liveness/interleaving properties.

## 9. Graphs

A graph representation is useful only where it provides leverage.

Prefer deriving architectural graphs from the actual code/IntelliJ index rather than manually maintaining another graph.

Useful derived relations include:

```
type → references
interface → implementations
package → dependencies
method → callers
test → subject
module → dependency
```

Use these graphs to retrieve the minimal code/spec closure relevant to the current task.

Do not maintain a parallel graph just because graphs are elegant.

## 10. LLM context reduction

When assigning an implementation task, construct the smallest context that is closed under the relevant dependencies.

Include:

* target capability
* relevant decisions
* relevant invariants
* relevant domain types
* relevant interfaces
* allowed transitions
* relevant tests
* directly referenced implementation files
* required reference behavior

Exclude unrelated features and architecture prose.

The central metric is:

**How many meaningful design decisions remain available to the implementation model?**

Try to make that number close to zero.

The implementation LLM should mainly decide local algorithmic details, not architecture or domain semantics.

## Task for this session

Using these principles, design the smallest practical workflow/tooling architecture for this project.

Do NOT jump into implementation.

Specifically determine:

1. What the normalized feature-spec artifact should contain.
2. What can be mechanically checked by a small `spec-lint` tool.
3. What must still be checked by an LLM semantic reviewer.
4. How invariants map into Java types/APIs/tests/ArchUnit/runtime checks.
5. What constitutes the SPEC_VALID gate.
6. What constitutes the DESIGN_VALID gate.
7. What exact context should be given to the implementation LLM.
8. How failures return deterministically to the appropriate earlier stage.
9. Which parts can be derived from IntelliJ indexes/code rather than manually maintained.
10. What we should deliberately NOT build.

Keep the design minimal.

Reject machinery whose maintenance cost exceeds the amount by which it constrains the implementation LLM.

Optimize for:

```
correctness
determinism
type safety
small search space
minimal duplication
minimal maintenance burden
compact implementation
```

