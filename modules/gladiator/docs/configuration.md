# Gladiator Configuration

All settings are stored in the event's `typeSettings` under the `gladiator` map.

## Modes

- `gladiator.mode`: FREE_FOR_ALL | LAST_PLAYER_STANDING (default: FREE_FOR_ALL)

## Rounds

- `gladiator.rounds.total`: number of rounds (default: 1)
- `gladiator.rounds.time-limit-seconds`: time limit per round (default: 600)
- `gladiator.rounds.countdown-seconds`: countdown before start (default: 10)

## Combat

- `gladiator.combat.pvp-enabled`: allow PvP (default: true)
- `gladiator.combat.friendly-fire`: allow friendly fire (default: false)
- `gladiator.combat.fall-damage`: fall damage enabled (default: true)
- `gladiator.combat.environment-damage`: environment damage (default: true)
- `gladiator.combat.void-eliminates`: void kills player (default: true)
- `gladiator.combat.score-per-kill`: points per kill (default: 1)
- `gladiator.combat.score-limit`: score limit for FFA win, 0 = disabled (default: 0)

## Lives

- `gladiator.lives.initial`: starting lives (default: 3)
- `gladiator.lives.maximum`: max lives cap (default: 3)

## Respawn

- `gladiator.respawn.policy`: NONE | DELAYED | AT_TEAM_SPAWN | AT_PERSONAL_SPAWN (default: DELAYED)
- `gladiator.respawn.delay-seconds`: respawn delay (default: 5)
- `gladiator.respawn.invulnerability-seconds`: invulnerability after respawn (default: 3)

## Elimination

- `gladiator.elimination.become-spectator`: eliminated players become spectators (default: true)

## Arena

- `gladiator.arena.out-of-bounds-policy`: IGNORE | TELEPORT_BACK | ELIMINATE (default: ELIMINATE)

## Ranking

- `gladiator.ranking.strategy`: SURVIVAL_THEN_KILLS | KILLS_DESCENDING | SCORE_DESCENDING | SURVIVAL_ORDER (default: SURVIVAL_THEN_KILLS)

## Validation

Critical errors block event start:
- Missing lobby, entrance, exit, or arena
- Invalid mode
- LPS requires lives > 0
- Negative values for lives, time, delay, etc.
- Invalid ranking strategy
