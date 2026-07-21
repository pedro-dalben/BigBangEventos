# Arquitetura — BigBangEventos

## Camada de Domínio (pura, sem Minecraft)

### Definições e Sessões
- `EventDefinition` — configuração persistente de evento
- `EventSession` — uma execução de evento
- `EventParticipant` — jogador dentro de uma sessão
- `EventLocation` — localização nomeada (LOBBY, ENTRANCE, EXIT, etc.)
- `EventType` — interface para tipos de evento

### Serviços de Domínio
- `SessionLifecycle` — única porta de transição de estado
- `ParticipationService` — join/leave transacional com snapshot e rollback
- `ParticipantCompletionService` — conclusão idempotente
- `TriggerService` — execução de gatilhos
- `SnapshotService` — captura e gerenciamento de snapshots
- `PlayerRestoreService` — restauração idempotente com tracking de componentes
- `DisconnectService` — grace period, desconexão/reconexão
- `SessionRecoveryService` — recuperação pós-reinício

### Plataforma (interfaces)
- `PlatformPlayerService` — operações de jogador (find, sendMessage, captureLocation)
- `PlatformTeleportService` — teleporte de jogadores
- `PlatformScheduler` — scheduling na thread do servidor
- `SnapshotGateway` — captura/restauração de estado de jogador
- `EventStorage` — persistência (definições, sessões, snapshots)
- `PermissionChecker` — verificação de permissão
- `TriggerEffects` — efeitos de trigger (mensagem, comando)

## Camada Fabric (implementações)

- `FabricPlayerService` — implementa PlatformPlayerService via MinecraftServer
- `FabricTeleportService` — teleporte via ServerPlayer.teleportTo()
- `FabricScheduler` — scheduler baseado em tick do servidor
- `FabricSnapshotGateway` — serialização de inventário via NBT
- `LocalEventStorage` — persistência YAML com escrita atômica
- `BigBangEventosMod` — entrypoint Fabric com listeners de conexão/desconexão/shutdown

## Fluxo de Dados

```
Comando/Listener → EventEngine (facade)
  → ParticipationService.join()
    → SnapshotService.prepare()
      → SnapshotGateway.capture()  [Fabric: serializa inventário]
      → SnapshotGateway.clearInventory()
    → PlatformTeleportService.teleport()
    → EventStorage.saveSession()
```

## Regras Arquiteturais

1. Domínio não depende de Minecraft (sem ServerPlayer, ServerWorld, ItemStack)
2. `EventEngine` é a única fachada — comandos/listeners nunca tocam storage diretamente
3. `SessionLifecycle` é a única porta de transição de estado
4. Snapshots são persistidos antes de qualquer alteração no jogador
5. Restauração é idempotente (RESTORED ignora chamadas repetidas)
6. Locks por playerId + sessionId (não lock global)
7. Toda operação Minecraft na thread do servidor
8. Clock injetável para testabilidade
