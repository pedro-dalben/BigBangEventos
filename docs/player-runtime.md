# Player Runtime — BigBangEventos

## Arquitetura

```
┌─────────────────────────────────────────┐
│              EventEngine (Facade)        │
├─────────────────────────────────────────┤
│  ParticipationService   SnapshotService │
│  PlayerRestoreService   DisconnectSvc   │
│  SessionRecoveryService                 │
├─────────────────────────────────────────┤
│  PlatformPlayerService  (interface)     │
│  PlatformTeleportService (interface)    │
│  PlatformScheduler      (interface)     │
│  SnapshotGateway        (interface)     │
├─────────────────────────────────────────┤
│  FabricPlayerService    (impl)          │
│  FabricTeleportService  (impl)          │
│  FabricScheduler        (impl)          │
│  FabricSnapshotGateway  (impl)          │
└─────────────────────────────────────────┘
```

## Fluxo de Entrada

```
1. Validar evento e sessão
2. Validar jogador online
3. Verificar vaga disponível
4. Criar participante provisório
5. Capturar snapshot (via SnapshotGateway)
6. Limpar inventário (se CLEAR_AND_RESTORE)
7. Teleportar para LOBBY
8. Confirmar participação ativa
9. Persistir sessão
```

Se falhar em qualquer etapa:
- Remover participante
- Restaurar snapshot se inventário foi alterado
- Liberar vaga

## Fluxo de Saída

```
1. Validar participação
2. Marcar saída em andamento
3. Restaurar snapshot:
   a. Restaurar inventário
   b. Restaurar armadura
   c. Restaurar offhand
   d. Restaurar estado (vida, fome, XP, etc.)
   e. Teleportar (saída ou local original)
4. Marcar estado final
5. Remover participante
6. Persistir
```

## Ordem do Snapshot

1. Capturar localização original
2. Serializar inventário principal
3. Serializar armadura
4. Serializar mão secundária
5. Capturar estado (vida, fome, XP, gamemode, etc.)
6. Capturar efeitos ativos
7. Criar PlayerSnapshot com estado CAPTURED
8. Limpar inventário (se modo CLEAR_AND_RESTORE)
9. Confirmar persistência

## Ordem da Restauração

1. Adquirir lock por snapshot
2. Verificar estado atual (ignorar se RESTORED)
3. Marcar RESTORING
4. Restaurar inventário → marcar INVENTORY
5. Restaurar armadura → marcar ARMOR
6. Restaurar offhand → marcar OFFHAND
7. Restaurar estado → marcar EXPERIENCE, HEALTH, HUNGER, EFFECTS, GAME_MODE, FLIGHT_STATE, MISC_STATE
8. Teleportar → marcar LOCATION
9. Marcar RESTORED
10. Registrar auditoria

## Modos de Inventário

### KEEP
- Inventário não é alterado
- Snapshot salva localização e estado
- Ao sair, não sobrescreve mudanças legítimas

### CLEAR_AND_RESTORE (padrão)
- Captura inventário completo
- Limpa inventário, armadura, mão secundária
- Restaura ao sair
- Não duplica itens

### EVENT_KIT (não implementado)
- Retorna erro de configuração

### ISOLATED (não implementado)
- Retorna erro de configuração

## Thread Safety

- Toda operação com Minecraft na thread do servidor
- `FabricScheduler.isServerThread()` verifica antes de executar
- Ações assíncronas enfileiradas para próximo tick
- Scheduler baseado em tick do servidor
- Locks por `playerId` e `sessionId` (não lock global)

## Grace Period

- Padrão: 120 segundos
- Durante grace period: participação reservada, snapshot ativo
- Expiração: participante desclassificado, snapshot pendente
- Reconexão: restauração pendente executada primeiro

## Limitações

- Safe teleport não implementado
- Dados client-only não capturados
- Mods com serialização não-NBT podem perder dados
- Apenas política CONTINUE para cronômetro durante desconexão
- Apenas GRACE_PERIOD e DISQUALIFY para desconexão
