# Arquitetura — Módulo Parkour

## Visão Geral

O módulo Parkour estende o BigBangEventos registrando o tipo de evento
`parkour` e fornecendo serviços específicos para gerenciar checkpoints,
cronômetro, quedas e conclusão.

```
┌──────────────────────────────────────────┐
│  ParkourModule (BigBangEventModule)      │
│  ┌─────────────┐  ┌───────────────────┐  │
│  │ParkourEventType│  │ParkourCommand    │  │
│  │ (EventType)   │  │ (/parkour)       │  │
│  └─────────────┘  └───────────────────┘  │
│  ┌────────────────────────────────────┐   │
│  │ ParkourSessionService              │   │
│  │  - cronômetro individual           │   │
│  │  - contagem de quedas              │   │
│  │  - gerenciamento de estado         │   │
│  └────────────────────────────────────┘   │
│  ┌────────────────────────────────────┐   │
│  │ ParkourCheckpointService           │   │
│  │  - validação de ordem             │   │
│  │  - registro de checkpoint         │   │
│  └────────────────────────────────────┘   │
│  ┌────────────────────────────────────┐   │
│  │ ParkourFallService                 │   │
│  │  - detecção de queda              │   │
│  │  - teleporte de volta             │   │
│  └────────────────────────────────────┘   │
│  ┌────────────────────────────────────┐   │
│  │ ParkourDataStore                   │   │
│  │  - persistência YAML              │   │
│  │  - cache em memória               │   │
│  └────────────────────────────────────┘   │
└──────────────────────────────────────────┘
         │
         ▼ (usa)
┌──────────────────────────────────────────┐
│  BigBangEventos Core (APIs públicas)     │
│  EventTypeRegistry, BigBangEventosApi,   │
│  PlatformScheduler, PlatformTeleport     │
└──────────────────────────────────────────┘
```

## Fluxo de Sessão

```
Jogador faz /evento entrar <id>
  → ParticipationService.join()
    → SnapshotService.prepare() (CLEAR_AND_RESTORE)
    → Teleporta para ENTRANCE

Admin faz /evento start <id>
  → ParkourEventType.onSessionStart()
    → Inicia cronômetro individual para cada participante

Jogador clica placa checkpoint_1
  → FabricEvents detecta clique
  → core TriggerService executa gatilho
  → ParkourSessionService registra checkpoint
  → ParkourCheckpointService valida ordem

Jogador cai do trajeto (altura > threshold)
  → ParkourFallService detecta no tick
  → Teleporta para último checkpoint
  → Incrementa contador de quedas

Jogador clica placa finish
  → TriggerService executa PLAYER_COMPLETE
  → ParkourEventType.onSessionFinish() calcula ranking

Admin faz /evento finish <id>
  → core finaliza sessão
  → ParkourEventType.onSessionFinish()
  → Restaura inventário (EXIT location)
```

## Serviços

### ParkourSessionService

Gerencia o estado da sessão parkour:
- Mapa `playerId+sessionId → ParkourPlayerData`
- Iniciar/parar cronômetro individual
- Incrementar quedas
- Verificar conclusão

### ParkourCheckpointService

- Mantém lista de checkpoints da definição do evento
- Valida ordem (strict vs any)
- Teleporta jogador para checkpoint específico

### ParkourFallService

- Escuta tick do servidor
- Verifica distância vertical dos jogadores ativos
- Se excede threshold, teleporta para último checkpoint
- Pula jogadores que já concluíram

### ParkourDataStore

- Persiste dados em `config/bigbangeventos/modules/parkour/players/`
- Cache em `ConcurrentHashMap`
- Salvamento em `onDisable` + periódico

## Dependências Entre Serviços

```
ParkourFallService
  → ParkourSessionService (obter último checkpoint)
  → PlatformTeleportService (teleportar)
  → PlatformScheduler (agendar verificação)

ParkourCheckpointService
  → EventDefinition.triggers (obter checkpoints)
  → PlatformTeleportService (teleportar admin)

ParkourSessionService
  → ParkourDataStore (persistir estado)
  → Rankings.TIME_ASCENDING (calcular ranking)
```
