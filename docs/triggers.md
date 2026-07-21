# Gatilhos (Triggers)

## Tipos Suportados

| Tipo | Status | Descricao |
|------|--------|-----------|
| SIGN_INTERACT | Funcional | Clique em placa |
| REGION_ENTER | Funcional | Entrada em area definida |
| REGION_EXIT | Implementado | Saida de area definida |
| PRESSURE_PLATE | Placeholder | Placa de pressao |
| BUTTON_INTERACT | Placeholder | Botao |
| LEVER_INTERACT | Placeholder | Alavanca |
| TIMER | Placeholder | Temporizador |
| MANUAL | Funcional | Ativacao por comando/admin |

## REGION_ENTER

### Area Types

Cada trigger REGION_ENTER pode ter uma area associada:

**CUBOID**
```
serverId, dimension, minX, minY, minZ, maxX, maxY, maxZ
```

**RADIUS**
```
serverId, dimension, centerX, centerY, centerZ, radius, verticalRadius
```

### Semantica

- Executa apenas na transicao OUTSIDE → INSIDE
- Nao executa continuamente enquanto dentro
- Ao sair e reentrar, pode executar novamente
- Respeita maxUses, cooldown, condicoes de sessao
- Scanner executa a cada 2 ticks (configuravel)
- State tracking por trigger+player

### Configuracao

```yaml
region-triggers:
  enabled: true
  check-interval-ticks: 2
```

### Cleanup

Estado de inside/outside limpo quando:
- Sessao finaliza/cancela/falha
- Jogador sai do evento
- Jogador desconecta
- Trigger removido

## Criacao de Gatilho

```bash
# Criar trigger SIGN_INTERACT
/evento trigger create chegada SIGN_INTERACT

# Vincular a placa
/evento trigger bind chegada
# (clique na placa)

# Criar trigger REGION_ENTER
/evento trigger create cp1_region REGION_ENTER
# (configure area via API/typeSettings)

# Listar triggers
/evento trigger list
```

## Condicoes

| Condicao | Descricao |
|----------|-----------|
| PLAYER_IS_PARTICIPANT | Jogador participa do evento |
| EVENT_IS_RUNNING | Sessao em estado RUNNING |
| PLAYER_NOT_FINISHED | Jogador ainda nao concluiu |
| PLAYER_HAS_PERMISSION | Permissao `bigbangeventos.player.trigger` |

## Acoes

| Acao | Descricao |
|------|-----------|
| SEND_MESSAGE | Enviar mensagem ao jogador |
| EXECUTE_COMMAND | Executar comando no console |
| ADD_POINTS | Adicionar pontos ao participante |
| PLAYER_COMPLETE | Marcar participante como concluido |
| TELEPORT | Teleporte (delegado a plataforma) |
| ENABLE_TRIGGER | Habilitar outro gatilho |
| DISABLE_TRIGGER | Desabilitar outro gatilho |
| EVENT_FINISH | Encerrar evento |

## Bridge para Modulos (EventType)

Modulos (Parkour, etc.) recebem callback `onTriggerFired` quando um
gatilho e ativado para um evento do seu tipo.

O Parkour usa triggerId para roteamento:
- triggerId == "finish" → completar parkour
- triggerId == checkpointId → completar checkpoint

Exemplo:
```bash
# Trigger para checkpoint
/evento trigger create cp1 SIGN_INTERACT
/evento trigger bind cp1

# Trigger para chegada
/evento trigger create finish REGION_ENTER
# Area configurada via API
```

---

## Nao implementado nesta versao

- GUI para configuracao de triggers
- Area editor visual
- Triggers em cadeia (um trigger ativar outro)
- Trigger conditions customizadas por modulo
