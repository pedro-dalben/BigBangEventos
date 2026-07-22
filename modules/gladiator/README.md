# BigBangEventos Gladiator

Gladiator competitive event module for BigBangEventos.

## Modes

- **FREE_FOR_ALL**: everyone vs everyone, respawn enabled, kills = score. Round ends on time limit, score limit, or staff finish.
- **LAST_PLAYER_STANDING**: each player has lives. Last alive wins. Eliminated players become spectators.

## Quick Start

```
/evento create mygladiator gladiator
/evento edit mygladiator
/evento set lobby
/evento set entrance
/evento set exit
/evento set spectator
/evento set respawn
/evento area pos1
/evento area pos2 (or area radius myarena 10)
/evento open mygladiator
/evento start mygladiator
```

## Configuration

Stored in typeSettings under `gladiator` key. See `docs/configuration.md`.

## Commands

- `/evento gladiator mode <FREE_FOR_ALL|LAST_PLAYER_STANDING>`
- `/evento gladiator lives <amount>`
- `/evento gladiator round-time <seconds>`
- `/evento gladiator info`

## Dependencies

- BigBangEventos >= 0.3.0 (API 3)
- Fabric API
- Minecraft 1.21.1
