# Development

## Project shape

`plugin-core` is the only product module. It contains the IntelliJ-native agent. The former MCP server, ACP clients, JCEF chat frontend and external-agent modules are removed from this branch.

## Requirements

- JDK 21.
- Gradle 9.7.1. `mtk` is the preferred local wrapper when installed.
- IntelliJ IDEA 2025.3 or newer for plugin sandbox work.

## Build

```bash
mtk gradle :plugin-core:buildPlugin
mtk gradle :plugin-core:test
```

The repository's Gradle wrapper JAR is currently absent. Use the cached Gradle 9.7.1 distribution or restore the wrapper before using `./gradlew`. Gradle 8.x is incompatible with the IntelliJ Platform Gradle Plugin version in this repository.

## Native design

The implementation boundary is explicit:

```text
external protocol / JSON / PSI / platform callbacks
                       ↓ validate
Java records, sealed results and lifecycle transitions
                       ↓ explicit effect boundary
IntelliJ platform operations
```

Keep external values out of the domain core. Prefer types and constructors over runtime flags. Use runtime checks only for ownership, mutable-world races and platform lifecycle behavior that Java cannot encode.

## Verification

Run focused lifecycle tests before broader plugin verification. A passing Java compile does not prove AWT or IntelliJ lifecycle behavior. A plugin build does not prove provider integration or user-interface behavior.

Use IntelliJ VCS operations for branch, staging and commit changes when available. Keep semantic commits small and conventional.
