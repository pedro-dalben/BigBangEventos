# Parkour — Triggers

## Integracao com o Sistema de Gatilhos

O Parkour integra-se ao sistema de gatilhos do Core via `onTriggerFired`.

Quando um gatilho e ativado para um evento do tipo `parkour`, o Parkour
recebe o callback e roteia pela triggerId:

| triggerId | Acao |
|-----------|------|
| `"finish"` | Completar o parkour (calcular tempo, ranking, teleport) |
| `"<checkpointId>"` | Completar checkpoint especifico |

## Tipos de Deteccao Suportados

### RADIUS (padrao — nao usa trigger system)

Checkpoints e chegada detectados por proximidade ao ponto central.
Configurado via comandos `/evento parkour checkpoint add <id> <radius>`.

### SIGN_INTERACT

Checkpoint ou chegada ativados ao clicar em placa.

Configuracao:
```bash
# 1. Selecionar evento
/evento edit parkour_test

# 2. Criar trigger SIGN_INTERACT com nome do checkpoint
/evento trigger create cp1 SIGN_INTERACT

# 3. Vincular a placa
/evento trigger bind cp1
# (clicar na placa)

# 4. Para chegada
/evento trigger create finish SIGN_INTERACT
/evento trigger bind finish
# (clicar na placa de chegada)
```

### REGION_ENTER

Checkpoint ou chegada ativados ao entrar em area definida.

Configuracao:
```bash
# 1. Criar trigger REGION_ENTER
/evento trigger create cp2_region REGION_ENTER

# 2. Configurar area via API/typeSettings (exemplo conceitual)
# Area tipo CUBOID: serverId, dimension, minX, minY, minZ, maxX, maxY, maxZ
# Area tipo RADIUS: serverId, dimension, centerX, centerY, centerZ, radius, verticalRadius

# 3. Para chegada
/evento trigger create finish REGION_ENTER
# (configurar area da linha de chegada)
```

## Comportamento

- Gatilho so ativa se sessao em RUNNING
- Gatilho so ativa se jogador e participante ativo
- Gatilho respeita maxUses e cooldown do Core
- Checkpoint nao duplica (completeCheckpoint idempotente)
- Conclusao nao duplica (ParticipantState.FINISHED)
- Fora de ordem: checkpoint ignorado silenciosamente

## Checkpoints Existentes (RADIUS)

Checkpoints configurados via `/evento parkour checkpoint add` usam deteccao
por raio no tick periodico (500ms). Nao usam o sistema de triggers.

Checkpoints por SIGN_INTERACT ou REGION_ENTER sao adicionais e podem
coexistir com checkpoints RADIUS.

## Fluxo de Ativacao

```
SIGN_INTERACT:
  Player clica placa
  → FabricEvents detecta
  → engine.activateTrigger()
  → TriggerService verifica condicoes (EVENT_IS_RUNNING, PLAYER_IS_PARTICIPANT...)
  → TriggerService executa acoes
  → engine chama EventType.onTriggerFired()
  → ParkourSessionService.onTriggerFired()
  → Roteia para checkpoint ou finish

REGION_ENTER:
  Player se move
  → RegionTriggerService.onTick() (a cada 2 ticks)
  → Verifica posicao contra areas dos triggers
  → Detecta OUTSIDE→INSIDE
  → engine.activateTrigger()
  → (mesmo fluxo acima)
```
