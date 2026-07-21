# Gatilhos — Módulo Parkour

## Gatilhos do Core Usados

O módulo Parkour usa os seguintes `TriggerType` do core:

| TriggerType | Uso no Parkour |
|-------------|----------------|
| `SIGN_INTERACT` | Checkpoints e chegada (padrão) |
| `REGION_ENTER` | Alternativa: entrar em região = checkpoint (futuro) |
| `TIMER` | Limite de tempo por checkpoint (futuro) |
| `MANUAL` | Disparado por comando (/parkour admin) |

## Configuração de Gatilhos no Parkour

### Checkpoint (SIGN_INTERACT)

```yaml
triggers:
  checkpoint_1:
    type: SIGN_INTERACT
    enabled: true
    binding: "minecraft:overworld;100;65;10"
    max-uses: 1
    cooldown: 0s
    conditions:
      - EVENT_IS_RUNNING
      - PLAYER_IS_PARTICIPANT
      - PLAYER_NOT_FINISHED
    actions:
      - type: SEND_MESSAGE
        arguments:
          message: "§aCheckpoint 1/5 alcançado!"
      - type: SEND_TITLE
        arguments:
          title: "§aCheckpoint 1"
          subtitle: "§7Faltam 4"
      - type: ADD_POINTS
        arguments:
          amount: "10"
```

### Chegada (SIGN_INTERACT) — Finish

```yaml
  finish:
    type: SIGN_INTERACT
    enabled: true
    binding: "minecraft:overworld;130;80;10"
    max-uses: 1
    conditions:
      - EVENT_IS_RUNNING
      - PLAYER_IS_PARTICIPANT
      - PLAYER_NOT_FINISHED
      - PLAYER_HAS_ALL_CHECKPOINTS   # Requer todos checkpoints
    actions:
      - type: SEND_MESSAGE
        arguments:
          message: "§6§lPARABÉNS! Você completou o parkour!"
      - type: SEND_TITLE
        arguments:
          title: "§a§lCOMPLETOU!"
          subtitle: "§7Tempo: %d segundos"
      - type: PLAY_SOUND
        arguments:
          sound: "minecraft:entity.player.levelup"
          volume: "1.0"
          pitch: "1.0"
      - type: PLAYER_COMPLETE
        arguments: {}
      - type: EVENT_FINISH
        arguments: {}  # Opcional: finaliza evento se todos completaram
```

## Avaliação Customizada pelo Módulo

O módulo Parkour adiciona lógica além das condições do core:

### Verificação de Ordem

Antes de executar as ações do checkpoint, o módulo verifica se o
checkpoint pode ser ativado:

```java
// Hook no listener de trigger
public void onTriggerActivate(EventTrigger trigger,
                               TriggerExecutionContext ctx) {
    boolean canActivate = false;

    if (trigger.id().startsWith("checkpoint_")) {
        canActivate = checkpointService.validateCheckpoint(
            ctx.playerId(), ctx.session().id(),
            trigger.id(), checkpointOrder);
    } else if (trigger.id().equals("finish")) {
        canActivate = checkpointService.isReadyToFinish(
            ctx.playerId(), ctx.session().id());
    }

    if (!canActivate) {
        ctx.effects().message(ctx.playerId(),
            "§cVocê não pode ativar este checkpoint agora.");
    }
}
```

### Detecção de Queda

O módulo escuta o tick do servidor e verifica a altura dos jogadores:

```java
// No tick listener
public void onTick(MinecraftServer server) {
    for (ParkourSession session : activeSessions.values()) {
        for (Map.Entry<UUID, ParkourPlayerData> entry :
                 session.getActivePlayers().entrySet()) {
            UUID playerId = entry.getKey();
            ParkourPlayerData data = entry.getValue();

            ServerPlayer player = server.getPlayerList()
                .getPlayer(playerId);
            if (player == null) continue;

            double startY = data.getStartPosition().y;
            double currentY = player.getY();

            if (startY - currentY > fallThreshold) {
                handleFall(player, data);
            }
        }
    }
}
```

## Vinculação

### Via Comando

```bash
/evento trigger bind checkpoint_1
# Clique na placa
```

### Via Código

```java
trigger.binding("minecraft:overworld;100;65;10");
```

## Gatilhos Futuros

| Trigger | Status | Descrição |
|---------|--------|-----------|
| `REGION_ENTER` | Planejado | Ativar checkpoint ao entrar em região |
| `TIMER` | Planejado | Limite de tempo por seção |
| `PRESSURE_PLATE` | Planejado | Alternativa a placas |
