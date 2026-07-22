# Competitive Module API (API 3)

API 3 introduces competitive event infrastructure: teams, rounds, combat,
lives, elimination, respawn, and spectators.

## Services available in EventModuleContext

| Method | Returns | API |
|--------|---------|-----|
| `teams()` | `TeamService` | 3 |
| `rounds()` | `RoundService` | 3 |
| `combat()` | `CombatService` | 3 |
| `lives()` | `LifeService` | 3 |
| `eliminations()` | `EliminationService` | 3 |
| `respawns()` | `RespawnService` | 3 |
| `spectators()` | `SpectatorService` | 3 |

## Compatibility

| Module API | Core API 3 |
|------------|-----------|
| 1          | Compatible |
| 2          | Compatible |
| 3          | Compatible |

Parkour (API 1) and Treasure Hunt (API 2) continue loading without changes.
Non-competitive events do not use any competitive service.

## Teams

- `TeamService`: create/remove teams, assign/remove players, random/balanced assignment, score, elimination
- `TeamDefinition`: defines team template (ID, name, color, limits, spawn)
- `SessionTeam`: runtime team in a session
- `TeamAssignmentMode`: MANUAL, RANDOM, BALANCED, PLAYER_CHOICE
- `TeamStatus`: ACTIVE, ELIMINATED, FINISHED, DISQUALIFIED

## Rounds

- `RoundService`: prepare, startCountdown, start, finish, cancel, fail, advance
- One active round per session
- `SessionRound`: tracks state, timing, scores per participant/team
- Round state machine: WAITING → PREPARING → COUNTDOWN → ACTIVE → FINISHING → FINISHED

## Combat

- `CombatService.onDeath()`: process death, handle lives, check elimination
- `LifeService`: initialize, add, remove, set, get lives
- `EliminationService`: eliminate player (idempotent), triggers events
- `CombatRuleSet`: PvP, friendly fire, fall damage, void, environment, OOB policy

## Respawn

- `RespawnService`: schedule/execute respawn with configurable policy and delay
- Policies: NONE, IMMEDIATE, DELAYED, AT_TEAM_SPAWN, AT_PERSONAL_SPAWN, NEXT_ROUND
- Invulnerability window after respawn

## Spectators

- `SpectatorService`: make/remove spectator, non-interacting observers
- `SpectatorReason`: ELIMINATED, MANUAL, ROUND_FINISHED, LATE_JOIN, STAFF

## Domain Events

Located in `DomainEventBus`. New events:
- `TeamEvents`: TeamCreated, PlayerAssignedToTeam, TeamScoreChanged, TeamEliminated
- `RoundEvents`: RoundPrepared, RoundStarted, RoundFinished, RoundFailed, RoundCancelled
- `CombatEvents`: ParticipantDamaged, ParticipantDeath, ParticipantKill, ParticipantLifeChanged, ParticipantRespawnScheduled, ParticipantRespawned, ParticipantEliminated, ParticipantBecameSpectator, ParticipantLeftSpectator

## Conditions (Trigger System)

New ConditionType values:
- `PLAYER_HAS_TEAM`, `PLAYER_TEAM_IS`, `TEAM_IS_ACTIVE`, `TEAM_IS_ELIMINATED`, `TEAM_SCORE_AT_LEAST`
- `ROUND_IS_ACTIVE`, `ROUND_NUMBER_IS`, `ROUND_TIME_REMAINING_AT_MOST`
- `PLAYER_LIVES_AT_LEAST`, `PLAYER_LIVES_EQUALS`, `PLAYER_IS_ELIMINATED`, `PLAYER_IS_SPECTATOR`
- `ACTIVE_PARTICIPANTS_AT_MOST`, `ACTIVE_TEAMS_AT_MOST`

## Actions (Trigger System)

New ActionType values:
- `ASSIGN_PLAYER_TO_TEAM`, `ADD_TEAM_SCORE`, `SET_TEAM_SCORE`, `ELIMINATE_TEAM`
- `START_ROUND`, `FINISH_ROUND`, `ADVANCE_ROUND`
- `ADD_PLAYER_LIFE`, `REMOVE_PLAYER_LIFE`, `SET_PLAYER_LIVES`
- `MAKE_SPECTATOR`, `REMOVE_SPECTATOR`
