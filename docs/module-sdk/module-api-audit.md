# Module API Audit — BigBangEventos

## Date
2026-07-21

## What modules can already do

| Capability | Status | API |
|---|---|---|
| Register EventType | OK | `EventTypeRegistry.register()` |
| Custom validate | OK | `EventType.validate()` |
| Lifecycle hooks | OK | `onSessionCreated`, `onRegistrationOpen`, `onSessionStart`, `onSessionFinish`, `onSessionCancel` |
| Type-specific config | Partial | `EventDefinition.typeSettings()` Map<String, Object> |
| Create/modify definitions | OK | Via `BigBangEventosApi` or `EventEngine` |
| Read sessions | OK | Via `BigBangEventosApi` |
| Register commands | OK | Via Fabric `CommandRegistrationCallback` |
| Player messages/titles | OK | Via `PlatformPlayerService` |
| Teleport | OK | Via `PlatformTeleportService` |
| Scheduler | OK | Via `PlatformScheduler` |
| Snapshot/restore | OK | Via `SnapshotService` / `PlayerRestoreService` |
| Per-participant data | OK | `EventParticipant.data()` Map<String,String> |
| Persistence of own data | OK | Module manages its own YAML/files (not in core storage) |
| Fabric listeners | OK | Via Fabric API directly |
| Sign callback | OK | Via `FabricEvents` + `TriggerType.SIGN_INTERACT` |

## What depends on internal classes

| Dependency | Internal class | Gap |
|---|---|---|
| `EventEngine` bootstrap | Direct field access | No reason to expose. Modules use API. |
| `EventEngine.definition()` | Package-private | Exists and is used by API. Acceptable. |
| `EventParticipant` mutation | Internal package | For Parkour timers/falls, module needs its own data layer. |
| `ConditionType` / `ActionType` | Enum, not extensible | Cannot add new enum values. Module must implement own condition/action evaluation. |

## Gaps found

### 1. No module registration API
No `BigBangEventModule` interface, no `EventModuleContext`, no Fabric entrypoint.

### 2. Condition/Action/Trigger/Ranking enums are closed
`ConditionType`, `ActionType`, `TriggerType`, `Rankings` are enums. Modules cannot extend them.
- **Mitigation**: `TriggerService` default cases return pass-through responses. Modules add own services.
- **For Parkour**: All checkpoint/fall/completion logic lives in Parkour services, not core triggers.

### 3. No API versioning
No module API version string. No compatibility check.

### 4. `EventDefinition.typeSettings()` is weakly typed
`Map<String, Object>` with no schema validation.

### 5. No isolated persistence
Core storage is `synchronized LocalEventStorage`. Modules should not use it directly.
- **Mitigation**: Modules manage their own files in config directory.

### 6. No module isolation on failure
`EventEngine` has no try/catch per module. A crashing module hook could block core.
- **Mitigation**: Core wraps module lifecycle calls in try/catch.

### 7. No participant data API beyond Map<String,String>
Parkour needs `startedAt`, `finishedAt`, `elapsedMillis`, `falls`, `completedCheckpointIds`, etc.
- **Mitigation**: Parkour stores its own data model separately, keyed by `playerId+sessionId`.

## Changes to implement

1. Create `BigBangEventModule` interface in `api/`
2. Create `EventModuleContext` interface in `api/`
3. Create Fabric entrypoint `bigbangeventos:event_module`
4. Add module discovery in `BigBangEventosMod`
5. Add API version constant
6. Add version validation on module load
7. Add try/catch around module lifecycle
8. Document `api/` as public, everything else as internal

## Changes deliberately postponed

- Extensible `ConditionType`/`ActionType` — would require enum → class refactor. Modules evaluate their own logic.
- Per-module permission tree — base permission system works.
- Module classloader isolation — Fabric Loader handles this.
- Dependency injection — modules access services through context.
- Hot-reload modules — not in scope for initial release.
- Module-specific storage API — modules use their own file access.
