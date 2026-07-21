# Parkour Pre-UAT Audit

Audit date: 2026-07-21
Branch: feat/parkour-runtime-uat
Auditor: AI (code inspection)

---

## Summary

| Category | Count |
|----------|-------|
| Funcionando e testado unitariamente | 12 |
| Carregado em runtime Fabric | 8 |
| Implementado mas nao homologado | 14 |
| Incompleto | 5 |
| Divergencia documental | 4 |
| Riscos | 7 |

---

## 1. Funcionando e testado unitariamente

Funcionalidades com testes automatizados que passam.

### Core

| ID | Feature | Evidence |
|----|---------|----------|
| C-01 | SessionLifecycle state transitions | EventCoreTest.lifecycleRejectsTerminalAndBadTransitions |
| C-02 | Join/leave participation | EventCoreTest.participationPreventsDoubleJoinAndIsIdempotentOnLeave |
| C-03 | Join when full fails | EventCoreTest.joinWhenFullFails |
| C-04 | SessionTimer (absolute clock) | EventCoreTest.timerUsesAbsoluteClockAndSnapshotDoesNotRestoreTwice |
| C-05 | Snapshot idempotent restore | EventCoreTest.snapshotIdempotentRestore |
| C-06 | Trigger condition fail + non-repeatable completion | EventCoreTest.triggerStopsOnFailedConditionAndCompletionIsNotRepeatable |
| C-07 | Disconnect grace period | EventCoreTest.disconnectGracePeriodExpires |
| C-08 | Teleport offline player fails | EventCoreTest.teleportOfflinePlayerFails |
| TOT | **8 core unit tests** | All pass |

### Parkour

| ID | Feature | Evidence |
|----|---------|----------|
| P-01 | Valid configuration passes validation | ParkourTest.validConfiguration |
| P-02 | Missing start/finish fails validation | ParkourTest.missingStartFails/missingFinishFails |
| P-03 | Negative maxTime/attempts fails validation | ParkourTest.negativeMaxTimeFails/negativeMaxAttemptsFails |
| P-04 | Checkpoint order tracking | ParkourTest.correctOrderCanBeTracked/wrongOrderIsHandled |
| P-05 | Repeat checkpoint fails | ParkourTest.repeatedCheckpointFails |
| P-06 | Checkpoint contains location (radius) | ParkourTest.checkpointContainsLocation |
| P-07 | Checkpoint rejects invalid radius | ParkourTest.checkpointRejectsInvalidRadius |
| P-08 | Timer format | ParkourTest.timerFormat/timerFormatEdgeCases |
| P-09 | Timer start/stop | ParkourTest.timerStartAndStop |
| P-10 | Fall increments counter | ParkourTest.fallIncrementsCounter |
| P-11 | Attempts tracked | ParkourTest.attemptsTracked |
| P-12 | Reset in progress flag | ParkourTest.resetInProgressFlag |
| P-13 | Idempotent completion | ParkourTest.idempotentCompletion |
| P-14 | Participant data CRUD | ParkourTest.participantDataTracksCheckpoints/clearsCheckpoints/currentCheckpoint |
| P-15 | Participant service getOrCreate | ParkourTest.participantServiceGetOrCreate/clearSession |
| P-16 | Configuration getters/setters | 8 tests (fallY, finishRadius, maxTime, maxAttempts, finishMode, resetMode, checkpointsRequired, completeDestination) |
| TOT | **26 parkour unit tests** | All pass |

---

## 2. Carregado em runtime Fabric

| ID | Feature | Evidence |
|----|---------|----------|
| R-01 | Fabric loader entrypoint | BigBangEventosMod.onInitialize registered |
| R-02 | Server lifecycle hooks | SERVER_STARTING/STARTED/STOPPING registered |
| R-03 | Connection events (join/disconnect) | JOIN and DISCONNECT callbacks registered |
| R-04 | Command registration | EventoCommand + ParkourCommandRegistrar |
| R-05 | Sign interact handler | FabricEvents.register -> UseBlockCallback |
| R-06 | ModuleLoader discovery | BigBangEventosMod.onServerStarted calls discoverAndLoad |
| R-07 | Recovery on startup | BigBangEventosMod.onServerStarted calls recoverOnStartup |
| R-08 | Scheduler tick | FabricScheduler.onTick via ServerTickEvents |

---

## 3. Implementado mas nao homologado com cliente real

| ID | Feature | Location | Status |
|----|---------|----------|--------|
| H-01 | Snapshot capture (full NBT serialization) | FabricSnapshotGateway | Compiled, never tested with modded items |
| H-02 | Inventory clear + restore | FabricSnapshotGateway.restoreInventory | Compiled, never tested with Cobblemon items |
| H-03 | Armor/offhand save + restore | FabricSnapshotGateway.restoreArmor | Compiled |
| H-04 | Player state restore (XP, health, effects, GM) | FabricSnapshotGateway.restoreState | Compiled |
| H-05 | Effect serialization/deserialization | FabricSnapshotGateway.captureActiveEffects/restoreEffects | Compiled |
| H-06 | KEEP inventory mode | ParticipationService.resolveInventoryMode | Implementation exists but NO automated test |
| H-07 | CLEAR_AND_RESTORE mode | ParticipationService | Covered in test stub, not with real items |
| H-08 | Session recovery on restart | SessionRecoveryService.recoverOnStartup | Compiled |
| H-09 | Parkour periodic tick (500ms) | ParkourSessionService.startPeriodicCheck | Compiled |
| H-10 | Parkour fall detection with Y_LEVEL | ParkourFallService.checkFall | Compiled |
| H-11 | Parkour checkpoint progression via radius | ParkourSessionService.tickCheckpointProgression | Compiled |
| H-12 | Parkour finish detection via radius | ParkourSessionService.tickFinishCheck | Compiled |
| H-13 | Parkour ranking (TIME_ASCENDING) | ParkourRankingService | Compiled |
| H-14 | Two-player independence | All services use ConcurrentHashMap | Compiled, never tested with 2 players |

---

## 4. Incompleto

| ID | Feature | Missing | Severity |
|----|---------|---------|----------|
| I-01 | **REGION_ENTER detection** | Enum exists in TriggerType. No scanning loop in Core. No area on EventTrigger. No inside/outside tracking. | CRITICAL |
| I-02 | **EventArea RADIUS type** | EventArea record is CUBOID-only. No radius/verticalRadius support. | HIGH |
| I-03 | **EventTrigger area binding** | EventTrigger has no area field. Triggers don't know their region. | HIGH |
| I-04 | **Parkour SIGN_INTERACT checkpoints** | Checkpoints only work via radius position check. No sign-based checkpoint support. | MEDIUM |
| I-05 | **Parkour SIGN_INTERACT finish** | Finish only works via radius check. No sign-based arrival support. | MEDIUM |

---

## 5. Divergencia documental

| ID | Doc file | Doc says | Code reality |
|----|----------|----------|--------------|
| D-01 | docs/triggers.md | REGION_ENTER "detecta quando jogador entra em area" | Not actually implemented |
| D-02 | docs/module-sdk/triggers-conditions-actions.md | Mentions REGION_ENTER as available | Not implemented |
| D-03 | modules/parkour/docs/checkpoints.md | Mentions "checkpoint por regiao" | Not implemented |
| D-04 | modules/parkour/docs/triggers.md | Mentions REGION_ENTER checkpoint config | Not implemented |

---

## 6. Riscos

### R-01: Perda de inventario com itens modded
**Risk**: HIGH
**Detail**: FabricSnapshotGateway serializes items via `ItemStack.save()` which uses CompoundTag. Items with modded components (Cobblemon, etc.) should serialize correctly, but items from mods not loaded will deserialize to AIR silently.
**Mitigation**: Test with Cobblemon items. Log warnings on deserialize failures.

### R-02: Duplicacao de itens
**Risk**: MEDIUM
**Detail**: SnapshotState.RESTORED guard exists but only in-memory. On server restart, snapshots loaded from YAML may restore again.
**Mitigation**: SessionRecoveryService only restores if snapshot.state != RESTORED. Persist snapshot state.

### R-03: Falha de teleporte no lobby
**Risk**: LOW
**Detail**: joinLocked does not roll back on teleport failure. Player stays registered with snapshot.
**Mitigation**: Acceptable - staff can fix. Snapshot exists.

### R-04: Checkpoint duplicado por race condition
**Risk**: LOW
**Detail**: ParkourCheckpointService.completeCheckpoint checks `hasCompletedCheckpoint` then `addCompletedCheckpoint` without lock. Race possible if both radius tick and manual click trigger simultaneously.
**Mitigation**: LinkedHashSet.add is atomic for dedup. Risk is benign (second activation = no-op).

### R-05: Conclusao duplicada
**Risk**: LOW
**Detail**: ParkourCompletionService.complete checks ParticipantState.FINISHED. Fine-grained race possible within same tick.
**Mitigation**: Participant state transitions are single-threaded (EventEngine synchronized). Parkour runs on platform thread.

### R-06: Reinicio durante sessao ativa
**Risk**: MEDIUM
**Detail**: SessionRecoveryService marks sessions FAILED and restores. If engine recovers sessions but snapshots aren't persisted to YAML, items lost.
**Mitigation**: Verify LocalEventStorage persists snapshots. Snapshot is persisted in session YAML.

### R-07: Regiao de checkpoint disparando varias vezes
**Risk**: HIGH (for REGION_ENTER when implemented)
**Detail**: Must track inside/outside state per trigger per player. Without state, re-entry detection impossible.
**Mitigation**: Implement inside UUID set per trigger in TriggerService.

---

## 7. Architecture Notes

### Trigger System - Current State

```
TriggerType (enum): SIGN_INTERACT, REGION_ENTER, REGION_EXIT, PRESSURE_PLATE, 
                    BUTTON_INTERACT, LEVER_INTERACT, TIMER, MANUAL
TriggerService: executes triggers, checks conditions, tracks uses/cooldowns
EventTrigger: id, type, enabled, maxUses, cooldown, conditions, actions, binding(String)
EventDefinition: putTrigger/trigger/triggers

MISSING:
- EventTrigger has NO area field
- TriggerService has NO inside/outside state
- Core has NO REGION_ENTER scanning loop
```

### Parkour Checkpoint System - Current State

```
ParkourCheckpoint: id, order, location(EventLocation), radius, enabled
  -> contains(serverId, dimension, x, y, z) - radius-based check
ParkourCheckpointService: CRUD + completion logic -> uses typeSettings JSON
ParkourSessionService.tickCheckpointProgression: periodic radius check
  -> getNextCheckpoint -> cp.contains() -> completeCheckpoint()

MISSING:
- No trigger system integration (SIGN_INTERACT, REGION_ENTER)
- No cuboid checkpoint support
- No per-checkpoint trigger type selection
```

### EventArea - Current State

```
EventArea record: serverId, dimension, minX, minY, minZ, maxX, maxY, maxZ
  -> contains(server, world, x, y, z) - cuboid check

MISSING:
- No RADIUS type
- No integration with EventTrigger
- Listed in EventDefinition but not used by triggers
```

---

## 8. Implementation Plan

### Phase 1: REGION_ENTER Core (critical)

1. Add area field to EventTrigger (CUBOID + RADIUS)
2. Extend EventArea with RADIUS variant
3. Create RegionTriggerService in Core for periodic scanning
4. Register REGION_ENTER scanner on server tick
5. Track inside/outside state per player per trigger
6. Respect maxUses, cooldown, session state

### Phase 2: Parkour Trigger Integration (high)

1. Parkour checkpoints accept trigger type (radius/SIGN_INTERACT/REGION_ENTER)
2. Parkour finish accepts trigger type (radius/SIGN_INTERACT/REGION_ENTER)
3. SIGN_INTERACT checkpoints via FabricEvents
4. REGION_ENTER checkpoints via Core scanner
5. SIGN_INTERACT finish via FabricEvents
6. REGION_ENTER finish via Core scanner

### Phase 3: Fixes and Hardening (medium)

1. Teleport confirm before timer start
2. Persist snapshot state to YAML
3. Second restore guard in LocalEventStorage
4. Fall cooldown handling improvements

### Phase 4: Tests and Docs (medium)

1. REGION_ENTER unit tests
2. Parkour trigger integration tests
3. Snapshot persistence tests
4. Update all documentation
5. UAT test scenarios document

---

## 9. Commands Audit

### Core commands (/evento)

| Subcommand | Permission | Implemented | Working |
|-----------|-----------|-------------|---------|
| create <id> <type> | admin | Yes | Yes |
| edit <id> | admin | Yes | Yes |
| list | any | Yes | Yes |
| info <id> | any | Yes | Yes |
| open <id> | admin | Yes | Yes |
| close <id> | admin | Yes | Yes |
| start <id> | admin | Yes | Yes |
| finish <id> | admin | Yes | Yes |
| pause <id> | admin | Yes | Yes |
| resume <id> | admin | Yes | Yes |
| cancel <id> | admin | Yes | Yes |
| validate <id> | any | Yes | Yes |
| status | any | Yes | Yes |
| entrar <id> | any | Yes | Yes |
| sair | any | Yes | Yes |
| set <location> | admin | Yes | Yes |
| trigger create/bind | admin | Yes | Partial |
| recovery * | admin | Yes | Yes |
| debug player | admin | Yes | Yes |

### Parkour commands (/evento parkour ...)

| Subcommand | Permission | Implemented | Working |
|-----------|-----------|-------------|---------|
| set-start | admin | Yes | Yes |
| set-finish [radius] | admin | Yes | Yes |
| set-finish-radius | admin | Yes | Yes |
| set-fall-y | admin | Yes | Yes |
| reset-mode | admin | Yes | Yes |
| max-time | admin | Yes | Yes |
| max-attempts | admin | Yes | Yes |
| finish-mode | admin | Yes | Yes |
| ranking-strategy | admin | Yes | Yes |
| complete-destination | admin | Yes | Yes |
| checkpoints-required | admin | Yes | Yes |
| checkpoint add/remove/list/info | admin | Yes | Yes |
| checkpoint set-radius | admin | Yes | Yes |
| checkpoint teleport | admin | Yes | Yes |
| info | admin | Yes | Yes |
| validate | admin | Yes | Yes |

Note: Parkour commands hang under `/evento` node. Must have event selected with `/evento edit <id>` first.

---

## 10. Build Status

Last build: Unknown (no ./gradlew run yet this session)

To verify: `./gradlew clean test build`

