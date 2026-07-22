# Competitive Core Architecture Audit — Phase 3

## Overview

Audit of BigBangEventos codebase to identify reuse opportunities, gaps, and
architecture for competitive event infrastructure (teams, rounds, combat,
lives, elimination, respawn, spectators) and Gladiator module.

Audit date: 2026-07-22
Branch: feat/competitive-core-gladiator
Core API: 2 (target: 3)
Schema: 2 (target: 3)

---

## 1. Reusable Structures

### Session & Participant Models
- `EventSession` (session/EventSession.java): has participants map, objective progress, stage progress, typed data. Will add team membership, round tracking, spectator set.
- `EventParticipant` (participant/EventParticipant.java): has state, score, typedData. Will add combat stats, lives, elimination state.
- `ParticipantState` enum: REGISTERED, WAITING, ACTIVE, PAUSED, FINISHED, ELIMINATED, DISQUALIFIED, LEFT, DISCONNECTED, RESTORE_PENDING, RESTORED. ELIMINATED already exists - reuse.
- `SessionState` enum: CREATED, REGISTRATION_OPEN, REGISTRATION_CLOSED, COUNTDOWN, RUNNING, PAUSED, FINISHING, FINISHED, CANCELLED, FAILED. No changes needed.

### Lifecycle
- `SessionLifecycle` (lifecycle/SessionLifecycle.java): state machine transitions. Reuse as-is. Rounds will add independent sub-lifecycle.
- `SessionRecoveryService` (core/SessionRecoveryService.java): marks unfinished as FAILED on startup. Will extend to mark active rounds as FAILED.

### Typed Data System
- `DataContainer`, `DataKey`, `DataCodec`, `InMemoryDataContainer` (data/). Reuse for TeamDataContainer.
- `TypedDataService` (data/TypedDataService.java): participant() and session() accessors. Add team() method.

### Domain Event Bus
- `DomainEventBus` (domain/DomainEventBus.java): subscribe/publish pattern. Reuse for all new competitive domain events.
- `ObjectiveEvents` (domain/ObjectiveEvents.java): existing events. Add `CompetitiveEvents` or extend with team/round/combat events.

### Trigger Service
- `TriggerService` (trigger/TriggerService.java): switch-based condition/action evaluation. Extend with new ConditionTypes and ActionTypes.
- `ConditionType`, `ActionType` enums: add competitive types.
- `TriggerExecutionContext`: reuse.

### Event Areas & Locations
- `EventArea` (Cuboid, Radius), `EventLocation`, `LocationName`. Reuse for arena bounds, spectator area, spawns.
- `LocationName`: new values for spectator spawns.

### Ranking
- `RankingStrategy` interface, `Rankings` enum. Add new strategies (SURVIVAL_THEN_KILLS etc).

### Persistence
- `LocalEventStorage` (persistence/LocalEventStorage.java): YAML with schema-version field. Add schema 3 support with rounds, teams, combat data.

### Module SDK
- `BigBangEventModule`, `EventModuleContext`, `ModuleLoader`. API 3 adds teams(), rounds(), combat(), lives(), elimination(), respawns(), spectators() to context.
- Parkour (API 1) and Treasure Hunt (API 2) continue loading.

### Fabric Runtime
- `BigBangEventosMod` (fabric/BigBangEventosMod.java): event hooks. Add Fabric damage/kill/respawn event capture.
- `FabricEvents` (fabric/FabricEvents.java): sign interaction. Add combat event handlers.
- `FabricPlayerService`, `FabricTeleportService`, `FabricSnapshotGateway`, `FabricScheduler`: reuse.

### Parkour Module
- Structure validates the module SDK works for API 1 modules.

### Treasure Hunt Module
- Validates API 2 module and DomainEventBus integration pattern. The event-driven architecture (subscribe to domain events) will be the model for Gladiator.

### Objectives & Stages
- `ObjectiveScope`: PARTICIPANT, SESSION, TEAM. TEAM exists but not implemented.
- `ObjectiveService`: currently rejects TEAM scope. Will implement.
- `ObjectiveProgress`: scope field, playerId nullable for SESSION scope.
- `StageService`, `SessionStageProgress`: reuse.

---

## 2. Gaps Found

### Teams
- No team model or service exists.
- No team data container.
- No team assignment (manual, random, balanced).
- ObjectiveScope.TEAM not implemented (ObjectiveService lines 19, 42 reject it).

### Rounds
- No round model or service exists.
- No round lifecycle separate from session lifecycle.
- No round persistence.

### Combat
- No combat state tracking per participant.
- No lives system (deaths field on EventParticipant is for parkour falls, not combat).
- No kill/death registration.
- No elimination reason tracking beyond ParticipantState enum.
- No damage attribution.
- No friendly fire logic.
- No out-of-bounds enforcement.

### Respawn
- No respawn policy or service.
- No invulnerability system.
- No spawn selection logic.
- SnapshotService exists but should NOT be used for respawn (only for join/leave/finish).

### Spectators
- `EventSession` has `spectators` Set<UUID> field but no spectator service.
- No spectator behavior enforcement.
- No follow-player functionality.

### Combat Rules
- No CombatRuleSet model.
- No centralized combat filtering (pvp enabled, friendly fire, blocked commands).
- No Fabric event handlers for damage/attack/death.

### Module SDK API 3
- `EventModuleContext` needs new methods for team, round, combat, life, elimination, respawn, spectator services.
- `BigBangEventosApi` needs new accessors.
- `ModuleLoader.DefaultModuleContext` needs wiring.

### Persistence Schema 3
- No schema migration logic.
- Schema version not tracked as constant.
- Session YAML lacks teams, rounds, combat data.

### Commands
- No team, round, combat, or spectator diagnostic commands.

### Conditions/Actions
- No competitive conditions (PLAYER_HAS_TEAM, ROUND_IS_ACTIVE, PLAYER_IS_ELIMINATED etc).
- No competitive actions (ASSIGN_PLAYER_TO_TEAM, START_ROUND, ELIMINATE_PLAYER etc).

---

## 3. Public Classes Needed

| Class | Package | Purpose |
|-------|---------|---------|
| TeamDefinition | definition/team/ | Template/definition of a team |
| SessionTeam | session/team/ | Runtime team in a session |
| TeamMembership | session/team/ | Player-team association |
| TeamStatus | session/team/ | ACTIVE, ELIMINATED, FINISHED, DISQUALIFIED |
| TeamAssignmentMode | session/team/ | MANUAL, RANDOM, BALANCED, PLAYER_CHOICE |
| TeamService | core/team/ | Team operations |
| TeamDataKeys | data/ | DataKeys for team typed data |
| TeamDataContainer | data/ | Proxy for team data |

| RoundDefinition | definition/round/ | Template of a round |
| SessionRound | session/round/ | Runtime round in a session |
| RoundState | session/round/ | WAITING, PREPARING, COUNTDOWN, ACTIVE, FINISHING, FINISHED, CANCELLED, FAILED |
| RoundFinishReason | session/round/ | LAST_PARTICIPANT, LAST_TEAM, TIME_LIMIT, SCORE_LIMIT, STAFF, NO_PARTICIPANTS, ERROR |
| RoundService | core/round/ | Round lifecycle operations |

| ParticipantCombatState | participant/combat/ | Combat stats per participant |
| CombatStatistics | participant/combat/ | Kills, deaths, assists |
| CombatRuleSet | definition/combat/ | PvP, friendly fire, fall damage, etc. |
| LifeService | core/combat/ | Life operations |
| EliminationService | core/combat/ | Elimination logic |
| EliminationReason | participant/combat/ | Enum of elimination reasons |
| DamageAttribution | participant/combat/ | Killer, source, timestamp |

| RespawnPolicy | model/respawn/ | NONE, IMMEDIATE, DELAYED, AT_TEAM_SPAWN, AT_PERSONAL_SPAWN, NEXT_ROUND |
| RespawnService | core/respawn/ | Respawn scheduling and execution |
| RespawnRequest | core/respawn/ | Pending respawn |
| RespawnResult | core/respawn/ | Result of respawn attempt |

| EventSpectatorState | participant/spectator/ | Spectator state |
| SpectatorService | core/spectator/ | Spectator operations |
| SpectatorReason | participant/spectator/ | ELIMINATED, MANUAL, ROUND_FINISHED, LATE_JOIN, STAFF |

| CompetitiveEvents | domain/ | All competitive domain events |

| EventLoadout | definition/loadout/ | Kit/loadout (minimal scope) |
| LoadoutService | core/loadout/ | Loadout application |
| LoadoutApplicationMode | core/loadout/ | NONE, REPLACE, ADD |

---

## 4. Internal Classes (no public exposure)

| Class | Reason |
|-------|--------|
| TeamMembership (if simple record) | Accessed only through TeamService |
| In-memory team storage within TeamService | Implementation detail |
| DamageAttribution | Internal combat detail |
| RespawnRequest | Internal scheduling |
| Round deadline logic | Inside RoundService |

---

## 5. Integration with Current Lifecycle

### No changes to SessionState
Rounds are orthogonal sub-lifecycles:
```
Session RUNNING
├── Round 1 FINISHED
├── Round 2 FINISHED
└── Round 3 ACTIVE
```

Round lifecycle hooks into EventEngine:
- `EventEngine.onTick()` → `RoundService.onTick()` for deadline enforcement
- `EventEngine.start()` → module's session start → prepare first round
- `EventEngine.pause()` → pause active round
- `EventEngine.resume()` → resume active round (countdown restart or preserve)
- `EventEngine.finish()` → finish active round → session finish
- `EventEngine.cancel()` → cancel active round → session cancel

Teams are created during registration and assigned before countdown.
Combat begins when round becomes ACTIVE.
Spectators set when participant is eliminated or manually toggled.

### EventType callbacks
Add optional interface methods for competitive events:
- `onRoundPrepared(session, round)`
- `onRoundStarted(session, round)`
- `onRoundFinished(session, round)`
- `onPlayerKilled(session, kill, victim)`
- `onPlayerEliminated(session, player)`

Default no-op in EventType interface.

---

## 6. Strategy for Deaths and Respawn

### Death flow (Fabric integration)
1. `ServerLivingEntityEvents.ALLOW_DAMAGE` → pre-filter (session check, friendly fire)
2. `ServerLivingEntityEvents.AFTER_DEATH` → capture death, process kill
3. Check if death is in competitive session
4. Deduplicate (same death tick/player)
5. Identify killer (attacker from damage source)
6. Register death (increment deaths, remove life)
7. Register kill if valid (increment kills, update score)
8. Check elimination (lives <= 0)
9. If eliminated: mark ELIMINATED, set spectator, emit events
10. If not eliminated: schedule respawn
11. Verify win condition

### Respawn flow
1. Delay (configured seconds)
2. Select spawn (personal or random available)
3. Teleport player to spawn
4. Apply invulnerability (configured seconds)
5. Clear respawn pending
6. Do NOT restore snapshot

### Snapshot policy
- Snapshot captured on join (existing) — NOT during respawn
- Snapshot restored on: leave, finish, cancel, recovery
- Respawn preserves current inventory

---

## 7. Strategy for Spectators

### State
- `EventSession.spectators` Set<UUID> already exists
- Add `EventParticipant.spectatorReason`, `spectatorFollowTarget`
- Add `SpectatorService` for CRUD and behavior enforcement

### Behavior enforcement (Fabric mixin or event handler)
On spectator:
- `GameType.SPECTATOR` (capture original GT in snapshot)
- Block damage to/from spectator
- Block interaction, block break/place, item drop/pickup
- Block competitive trigger activation
- `/evento gladiator spectate <player>` → follow mode
- On follow target disconnect/death/elimination → return to spectator area

### Restoration
- Restore from snapshot only on event finish/cancel/leave
- NOT on spectate toggle (spectator should keep event inventory)

---

## 8. Strategy for Persistence

### Schema 3 changes
`EventSession` YAML additions:
```yaml
schema-version: 3
# ...existing fields...
teams:
  - team-id: uuid
    team-definition-id: red
    members: [uuid, uuid]
    score: 0
    status: ACTIVE
    created-at: timestamp
rounds:
  - round-id: uuid
    number: 1
    state: ACTIVE
    started-at: timestamp
    deadline: timestamp
    participant-scores: {uuid: 0}
    team-scores: {red: 0}
participant-combat:
  - player-id: uuid
    lives: 3
    kills: 0
    deaths: 0
    eliminated: false
```

### Schema compatibility
- Schema 1: loaded as-is (no teams/rounds/combat data → defaults)
- Schema 2: loaded as-is
- Schema 3: full competitive support
- `LocalEventStorage.load()` checks `schema-version` field, defaults to 1 if absent

### Migration
- No automatic migration between schemas
- Schema 3 sessions without teams/rounds = empty lists (safe defaults)
- Existing Parkour/Treasure Hunt events remain schema 1 or 2

---

## 9. Strategy for Thread Safety

- All EventEngine methods are `synchronized` — reuse this pattern
- TeamService, RoundService, CombatService, RespawnService: per-engine services, synchronized at engine level
- SpectatorService: per-engine, synchronized
- Respawn scheduler uses FabricScheduler (tick-based, single-thread)
- Fabric event handlers run on server thread → safe
- DomainEventBus: publishes synchronously when on server thread, queues when off

---

## 10. Available Fabric Events

| Event | Class | Purpose |
|-------|-------|---------|
| ALLOW_DAMAGE | ServerLivingEntityEvents | Pre-damage filter (friendly fire, invulnerability) |
| AFTER_DEATH | ServerLivingEntityEvents | Death capture |
| ENTITY_RESPAWN | ServerPlayerEvents.AFTER_RESPAWN | Respawn detection |
| ALLOW_BLOCK_BREAK | PlayerBlockBreakEvents | Block break in arena |
| USE_BLOCK | UseBlockCallback | Interaction (already used for signs) |
| USE_ITEM | UseItemCallback | Item use in arena |
| DROP_ITEM | ServerPlayerEntity.DROP_ITEM | Drop prevention |
| PICKUP_ITEM | ProjectileImpactCallback | Not ideal; use PlayerInventory.INSERT or mixin |
| COMMAND | CommandRegistrationCallback | Already used |
| TICK | ServerTickEvents.START_SERVER_TICK | Already used |
| PLAYER_JOIN | ServerPlayConnectionJoinEvent | Reconnect |
| PLAYER_DISCONNECT | ServerPlayConnectionDisconnectEvent | Disconnect |
| CHANGE_DIMENSION | ServerPlayerEntity.CHANGE_DIMENSION | Dimension change detection |

### Mixin requirements
Only if Fabric API lacks callback:
1. **Item drop prevention for spectators** — if `ServerPlayerEntity.dropItem` has no callback
2. **Block interaction prevention for spectators** — if `PlayerBlockBreakEvents` fires before spectator check
3. **Entity interaction prevention** — if `ServerPlayNetworkHandler` mixin needed

Keep mixins minimal and documented.

---

## 11. Inventory and Snapshot Risks

### Risks
1. **Respawn with snapshot restore**: If a player is restored from snapshot on respawn, they lose ALL items gained during the event. Respawn MUST NOT restore snapshot.
2. **Spectator with event items**: If a spectator keeps event items, they could pass them to active players. Policy: clear inventory when becoming spectator, or keep but block drop.
3. **Friendly fire item transfer**: Players could drop items for teammates. Blocked by `allowItemDrop` rule.
4. **Disconnect during combat**: Grace period preserves state. If player disconnects mid-combat and dies during grace, they should be treated as disconnected (no kill credit).
5. **Death during snapshot**: Should not happen (snapshot taken on join, before combat).

### Mitigation
- Respawn: clear pending state, teleport, apply invulnerability. No snapshot restore.
- Spectator: clear inventory or use `GameType.SPECTATOR` which prevents interaction.
- Grace period: treat as disconnected, not dead. If killed during grace, no kill credit.
- Death deduplication: track death per player+round+tick combination.

---

## 12. Compatibility with Existing Modules

| Module | API | Impact | Action |
|--------|-----|--------|--------|
| parkour | 1 | None | Continues loading |
| treasure_hunt | 2 | None | Continues loading |
| generic | - | None | Core built-in, unchanged |

### No behavioral changes
- Non-competitive events never create teams, rounds, or combat state
- `EventEngine` checks event type before competitive processing
- Combat handlers filter: "is this a competitive session with combat enabled?"
- Conditions/Actions default behavior unchanged

### API 3 compatibility with API 1 and 2 modules
- `EventModuleContext` uses default methods (throws UnsupportedOperationException)
- API 3 adds new methods with defaults → API 1/2 modules don't see them
- `ModuleLoader.API_VERSION` = 3
- `BigBangEventosApi.API_VERSION` = 3
- Parkour (req API 1) → 1 <= 3 → loads
- Treasure Hunt (req API 2) → 2 <= 3 → loads

---

## 13. Gladiator Module Strategy

### Module identity
- ID: `bigbangeventos_gladiator`
- Type: `gladiator`
- API: 3
- Package: `br.com.bigbangcraft.eventos.gladiator`

### Modes (MVP)
- `FREE_FOR_ALL`: everyone vs everyone, respawn, score = kills, time or score limit
- `LAST_PLAYER_STANDING`: lives, elimination, last alive wins

### Architecture
- `GladiatorSessionService`: main lifecycle (subscribe to domain events)
- `GladiatorRoundService`: round management (start/finish per mode)
- `GladiatorCombatService`: death/kill processing
- `GladiatorCompletionService`: win condition checking
- `GladiatorRankingService`: ranking strategies
- `GladiatorValidator`: config validation
- `GladiatorCommandRegistrar`: commands
- `GladiatorConfiguration`: typeSettings keys
- `GladiatorMessages`: string constants

### Reuse
- TeamService (for TEAM_DEATHMATCH future)
- RoundService (for round lifecycle)
- LifeService, EliminationService, RespawnService, SpectatorService
- CombatRuleSet
- DomainEventBus (subscribe to CompetitiveEvents)

### Data flow
```
Player death (Fabric)
  → FabricEvents.onDeath()
  → EventEngine.onPlayerDeath(eventId, player, killer, source)
  → CompetitiveEvents.ParticipantDeathEvent published
  → GladiatorCombatService.subscribe(death)
    → register stats
    → remove life
    → if lives <= 0: eliminate
    → if eliminated: make spectator, check win
    → else: schedule respawn
  → GladiatorCompletionService.subscribe(elimination)
    → if FFA and score >= limit: finish
    → if LPS and active <= 1: finish
  → RoundService.finish() if win condition met
  → SessionLifecycle.finish() via EventEngine
```

---

## 14. Summary: Implementation Order

1. Version bump (0.3.0-SNAPSHOT, API 3, Schema 3)
2. Core competitive models (teams, rounds, combat, respawn, spectator)
3. Core competitive services
4. TeamData + ObjectiveScope.TEAM
5. Domain events
6. Runtime Fabric capture (death, damage, respawn)
7. Conditions + Actions
8. Diagnostic commands
9. Module SDK API 3
10. Gladiator module
11. Tests
12. Documentation
13. Build, merge, push
