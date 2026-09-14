# AgentBridge Native Agent

AgentBridge is an IntelliJ Platform plugin for a small native coding agent.

The current branch is an intentional cutover baseline:

- Java 21 domain model with records and sealed interfaces.
- IntelliJ-native execution surface under `plugin-core`.
- No MCP server or HTTP bridge.
- No ACP client runtime.
- No JCEF/web chat frontend.
- No external-agent client registry.
- No persistent conversation database, memory subsystem, hooks or sandbox runtime.

The native agent is being rebuilt incrementally. The lifecycle/admission slice is the first source-backed component. It is not yet a complete provider, tool catalog or user-facing chat workflow.

## Requirements

- IntelliJ IDEA 2025.3 or newer.
- JDK 21.
- Gradle 9.7.1 for local builds. The repository wrapper JAR must be restored before `./gradlew` can be used; CI provisions Gradle directly.

## Build and test

```bash
mtk gradle :plugin-core:buildPlugin
mtk gradle :plugin-core:test
```

If `mtk` is unavailable, use an installed Gradle 9.x with JDK 21. Gradle 8.x is not supported by the IntelliJ Platform Gradle Plugin used here.

## Project structure

```text
agentbridge/
├── plugin-core/       # Native IntelliJ plugin
├── buildSrc/          # Gradle build helpers
├── scripts/           # Development and release scripts
└── .agent-work/       # Ignored planning and review artifacts
```

## Development direction

The native implementation keeps an unsound shell around a type-safe core:

```text
provider / JSON / PSI / platform callbacks
             ↓ validate and resolve
Java domain values and transitions
             ↓ explicit adapter effects
IntelliJ platform side effects
```

The implementation process is defined in `.agent-work/thought-process.md` and `.agent-work/native-agent-workflow/plan.md`. The lifecycle feature contract is in `.agent-work/native-agent-workflow/lifecycle-admission/`.

The donor runtime was deliberately removed on this branch. New provider, semantic-tool, verification and Swing UI capabilities will be added only after their typed contracts and focused behavioral evidence exist.
