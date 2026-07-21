# Arquitetura — Module SDK

## Visão Geral

BigBangEventos é dividido em duas camadas:

1. **Core** (`com.pedrodalben.bigbangeventos`) — motor de eventos, sessões,
   participantes, gatilhos, ranking, snapshots, persistência.
2. **Módulos** — mods Fabric separados que registram tipos de evento
   personalizados usando apenas APIs públicas.

## Core vs Módulos

```
┌──────────────────────────────────────────┐
│              BigBangEventos Core         │
│  ┌──────────┐  ┌──────────┐  ┌────────┐ │
│  │ EventEngine│  │ Trigger  │  │ Ranking│ │
│  │ SessionLC  │  │ Service  │  │  strs  │ │
│  │ PartSvc    │  │ Storage  │  │ Snaps  │ │
│  └──────────┘  └──────────┘  └────────┘ │
├──────────────────────────────────────────┤
│         API Pública (api/)               │
│  BigBangEventosApi                       │
│  BigBangEventModule (interface)          │
│  EventModuleContext (interface)          │
│  OperationResult                         │
├──────────────────────────────────────────┤
│         Módulo Externo (ex: parkour)      │
│  ┌──────────┐  ┌──────────┐             │
│  │EventType │  │ Comandos │             │
│  │ own data │  │ own sched│             │
│  └──────────┘  └──────────┘             │
└──────────────────────────────────────────┘
```

## Pacotes Públicos (API)

Módulos só podem usar:

| Pacote | Uso |
|--------|-----|
| `com.pedrodalben.bigbangeventos.api` | Interfaces públicas do módulo |
| `com.pedrodalben.bigbangeventos.eventtype` | `EventType`, `EventTypeRegistry` |
| `com.pedrodalben.bigbangeventos.definition` | `EventDefinition`, `EventLocation`, `EventArea`, `LocationName` |
| `com.pedrodalben.bigbangeventos.session` | `EventSession`, `SessionState` |
| `com.pedrodalben.bigbangeventos.participant` | `EventParticipant`, `ParticipantState`, `CompletionMode` |
| `com.pedrodalben.bigbangeventos.trigger` | `EventTrigger`, `TriggerType`, `ConditionType`, `ActionType`, `TriggerAction`, `TriggerExecutionContext` |
| `com.pedrodalben.bigbangeventos.ranking` | `RankingStrategy`, `Rankings` |
| `com.pedrodalben.bigbangeventos.validation` | `ValidationResult`, `ValidationLevel`, `ValidationIssue` |
| `com.pedrodalben.bigbangeventos.platform` | `PlatformScheduler`, `PlatformPlayerService`, `PlatformTeleportService`, `StoredLocation` |
| `com.pedrodalben.bigbangeventos.snapshot` | `SnapshotService`, `PlayerSnapshot`, `InventoryMode`, `SnapshotState` |

## Pacotes InternOS (proibidos para módulos)

| Pacote | Motivo |
|--------|--------|
| `com.pedrodalben.bigbangeventos.core` | Implementação interna do motor |
| `com.pedrodalben.bigbangeventos.fabric` | Código específico Fabric |
| `com.pedrodalben.bigbangeventos.persistence` | Armazenamento interno (`EventStorage`) |
| `com.pedrodalben.bigbangeventos.lifecycle` | `SessionLifecycle` — transições de estado |
| `com.pedrodalben.bigbangeventos.timer` | Timer interno |

## Ciclo de Vida do Módulo

Ver `module-lifecycle.md` para detalhes.

1. `onLoad` — módulo carregado, config básica
2. `onEnable` — tipos, comandos, listeners registrados
3. `onDisable` — limpeza, salvamento

## Fluxo de Dados

```
Jogador → Comando/EventoFabric
  → BigBangEventosApi (público)
    → EventEngine (interno, chamado via API)
      → SessionLifecycle / ParticipationService / TriggerService
        → EventStorage
        → PlatformServices (interfaces)
          → Implementações Fabric
```

Módulos interagem com o core APENAS via `BigBangEventosApi` e
`EventModuleContext`. Nunca acessam `EventEngine` diretamente.
