# Agent Instructions

## Source hierarchy

- Follow system and user instructions first. This file defines repository-specific defaults.
- Read the applicable feature specification, design, review ledger, and build files before changing their implementation.
- Do not claim a file, symbol, configuration, test result, or tool behavior without reading or executing the relevant evidence.
- Treat user observations as facts. Do not rerun a check merely to dispute them.

## Workspace and tooling

- Keep temporary files, plans, generated evidence, and disposable probes under `.agent-work/`. Do not write to `/tmp`, home directories, or outside the repository unless the user explicitly requests it.
- When `idea-facade` is registered, read `skill://jb-code-nav` before investigating code structure, symbol identity, references, call graphs, implementations, dependency source, refactors, diagnostics, tests, or builds.
- Call `idea_facade_status` before relying on IDEA Facade capability. Do not pass a project path; the server binds the project.
- Prefer IDEA Facade composite tools for IntelliJ-aware reads, searches, refactors, diagnostics, tests, and builds. Use CLI Git/GitHub or OMP native Git when the facade has no suitable Git operation.
- A failed or incomplete IDEA Facade response is an infrastructure defect, not evidence that the repository code is wrong. Report unexpected facade behavior with the exact call, expected response, actual response, and workaround. Then use the narrowest safe fallback.
- A test request that reports zero discovered tests is not a passing test result.
- Use the normal project wrapper command when no suitable IDE runner exists. Do not invent build or test task names.
- Batch independent reads and checks. Use the lightest verification that establishes the needed fact.

## Design and domain model

- Prefer JetBrains platform APIs over custom implementations. If IntelliJ already owns OS detection, shell behavior, VCS, project model, SDK lookup, threading, or an IDE action, use or wrap that API.
- MCP tools bridge IntelliJ actions. Do not invent raw JDBC, subprocess, filesystem-scanning, or similar substitutes for available platform behavior. Disable unavailable IDE capabilities explicitly.
- Treat JSON, PSI, callbacks, coroutines, provider SDK objects, network data, and UI events as unsound boundaries. Validate and normalize them once before they enter the domain.
- Keep the internal domain strongly typed: separate value types for distinct identifiers, validated constructors, immutable collections, sealed outcomes or state variants when they remove illegal states, and state-specific operations where simple. Prefer type/API prevention over tests or runtime checks when it removes an invalid state cheaply.
- Java owns domain representation and transition policy. Kotlin owns coroutine execution and platform coroutine interop. Do not leak JSON, PSI, coroutine, or provider-SDK types into the Java domain.
- One requirement, decision, transition table, and implementation path has one canonical authority. Do not create parallel models, compatibility aliases, or duplicate implementations.
- Fix root causes. Do not hide unexpected values with plausible defaults, swallowed errors, or silent fallbacks.
- Keep callbacks, I/O, network access, and heavy computation off the UI thread. Avoid whole-file PSI text/range work on large files when a targeted API exists. Cache only where repeated work justifies it.

## UI and testable logic

- UI classes coordinate UI. Any logic independent of Swing, JCEF, or IntelliJ UI APIs belongs in a small, cohesive domain, formatter, parser, builder, or calculator class.
- Do not pass UI components into extracted logic. Pass immutable data.
- Test extracted decision logic directly. Tests must cover observable behavior, boundaries, precedence, and real failures; do not pin implementation plumbing or prose.
- Do not create god utility classes or section-comment banners to conceal an oversized UI class.

## Change workflow

- Work on the current feature branch. Do not commit directly to `master`. If an earlier PR from this session is unmerged, extend that branch unless the user explicitly approves a separate branch.
- Keep each change coherent. Update every direct caller, test, contract, and documentation claim affected by a cutover.
- Before reversing an explicit setting, ignored path, disabled feature, deleted file, or commented block, inspect its blame and introducing commit.
- After code changes, run focused tests and the affected build. Before a PR or push, run the relevant unit tests. Record actual commands and outcomes.
- Resolve every PR review thread with an evidence-backed reply and an explicit resolution. Do not leave addressed threads pending.
- Preserve the repository-configured Git author and committer identity. Never substitute a model, coding agent, CLI, tool, dependency author, or invented bot identity. Do not add `Co-authored-by` trailers for tools or dependencies. If a hook rejects or rewrites identity, stop and report the mismatch.
- Use the repository's configured GitHub bot path for agent-created PRs or comments. Do not substitute a personal token when bot credentials are required.
- Rebase branches onto the target branch; do not create merge commits.

## Native-agent workflow

- `native-agent-docs/product.md` is the product authority. `native-agent-docs/workflow.md` defines the feature gates.
- A feature enters implementation only after its exact specification and design evidence pass. Frozen surfaces change only by reopening the affected spec or design gate.
- Every normative behavior needs an acceptance criterion and enforcement location. Distinguish mechanical evidence from semantic review; a passing parser, model, or build proves only its stated scope.
- For lifecycle work, preserve the single admission boundary, sequential effect accounting, terminal outcomes, cancellation semantics, ownership identity, and immutable observations documented in the lifecycle feature packet.

## MCP tool changes

- Tool descriptions state what the tool does, when to use it, its result shape, important parameters, and caveats.
- Return actionable errors prefixed with `Error:` or `Error (exit N):`. Validate preconditions and include enough response context to avoid follow-up calls.
- Set read-only, destructive, idempotent, and open-world annotations accurately. Remote-ref Git operations fetch through the shared throttled helper.

## Prose and reasoning

- Write direct, evidence-backed prose. Do not use filler, generic praise, rhetorical questions, false certainty, or AI stock phrases.
- Do not manually wrap prose to an arbitrary column.
- State constraints, decisions, risks, and verification results. Do not restate the request.
- Ask a focused question when a missing requirement, authority, or destructive choice cannot be resolved from the repository and user context. Do not fabricate an answer.
