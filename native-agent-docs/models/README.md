# Quint models

Layered Quint models of native-agent interactions between independent owners. They are tracked evidence, not normative spec text: `native-agent-docs/product.md` is the product authority, and `native-agent-docs/workflow.md` (Formal tools) sets the rules. The plan that produced them is `native-agent-docs/plans/semantic-reads-preparation.md`.

Read this file before you change, check or review a model.

## Modules

| Module | Content |
|---|---|
| `agent_interface.qnt` | ToolEffect interface views and the pure relation `legalEffectTransition` |
| `agent_coarse.qnt` | One `RunSession` with its `RunLifecycle`, the `NativeRunDriver` loop, the provider and tool effects. Each action comment names the Java method that it follows. |
| `read_execution.qnt` | One read lifecycle: smart wait, write-action restart, dumb mode, document edits, compute, abort, registration, registry disposal, settle |
| `read_refinement.qnt` | Projects `read_execution` state onto the interface views and checks every step with `REF_R` |

## Check the models

```bash
native-agent-docs/models/check.sh all      # or: fast | verify
```

The script finds its targets in the model files. Raw output goes to `.agent-work/evidence/phase2/<name>.txt`, and `summary.tsv` has one row per check. The script exits 1 when any check is BAD.

| Declaration | Check | Expected |
|---|---|---|
| `val INV_*`, `val LEM_*`, `val REF_*` | `quint run` (simulator) and `quint verify --backend=tlc` | holds |
| `val W_*` | `quint verify --invariant="not(W_*)" --backend=tlc` | violation (the scenario is reachable) |
| `action step_mut_<X>` | `quint verify --step=step_mut_<X> --invariant=<X> --backend=tlc` | violation (the invariant detects the fault) |
| `action stepCorpus` | `quint run --mbt --out-itf` | ITF replay corpus in `.agent-work/itf/` |

## Rules

1. **Use TLC for `verify`.** Every model is finite, so TLC checks the complete reachable state space in seconds. Apalache (the Quint default) did not finish 12 steps on `agent_coarse`, and its result is bounded. This is an owner decision of 2026-09-15.
2. **Bound every counter with a guard.** A variable without a bound makes the state space infinite, and TLC then runs without end.
3. **Put each mutant in the base module** as `mut_<name>` plus `action step_mut_<X> = any { step, mut_<name> }`. Do not write a mutant module that imports the base with `import ... as B`: Quint 0.32.0 cannot flatten those names for TLC or Apalache (`QNT404 Name 'B::...' not found`), so the check crashes.
4. **A witness or mutant passes only on violation text.** A crash also exits non-zero. `check.sh` requires `is violated`, `[violation]` or `found a counterexample` in the output.
5. **Write `x' = (a or b)`, not `x' = a or b`.** The backends compile the second form as `(x' = a) or b` and report a missing assignment.
6. **Witnesses need TLC, not the simulator.** The seeded simulator missed three witnesses that TLC reaches.
7. **Do not name anything `run` or `keys`.** `run` is a Quint keyword, and `keys` is a built-in.
8. **Mark scheduler-only actions.** `stepCorpus` is a subset of `step` for replay traces only. Invariants, witnesses and mutants use `step`.

## Conventions

- `agent_coarse` holds its state in one record `s`. `prev` is the pre-state of the last step, and every action, mutants included, sets `prev' = s`. Transition invariants compare `prev` with `s`.
- A batch in `agent_coarse` has at most three calls, held in the fixed slots `slot0`, `slot1` and `slot2`. Call key = `10*run + localId`.
- `read_execution` assigns every variable in every action.
- ITF traces label state 0 with the step action name (for example `stepCorpus`), not `init`.

## Navigation

IntelliJ resolves Quint symbols by position. Use `mcp__idea-facade__symbol` with `op: references` or `op: definition` and `file`, `line`, `column`. This also works across modules (`B::INV_C4`). Name search (`find op=symbol`) and `semantic_search` return nothing for `.qnt`. Find the position with `search_text` and `filePattern: "*.qnt"` first.

## Tracker

- `tracker/tracker.py scan-models` records every `action`, `var` and `val` of each module with its digest and `file:line`. See `tracker/README.md`.
- Review findings on a model use `finding add --target-kind model --target-id <module>`. A later scan of that module makes the finding stale.
- The review of a model must come from a different model or person than the author. Record it with `review open`, `finding add` and `review close`, not in a Markdown or JSON file.
