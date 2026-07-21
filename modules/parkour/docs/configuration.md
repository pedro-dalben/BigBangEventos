# Configuração — Módulo Parkour

## Configuração Global

Arquivo: `config/bigbangeventos/modules/parkour/config.yml`

```yaml
# Configuração global do módulo parkour
fall-check-interval-ticks: 5       # A cada quantos ticks verificar queda
default-fall-distance-threshold: 10 # Distância padrão para detectar queda
auto-save-interval-minutes: 5       # Intervalo de salvamento automático
max-concurrent-sessions: 10         # Máximo de sessões simultâneas
```

## Configuração por Evento (typeSettings)

Definida via `EventDefinition.typeSettings()` ou no YAML do evento.

```yaml
# Exemplo: config/bigbangeventos/events/meu_parkour.yml
type-settings:
  parkour:
    fall-distance-threshold: 8          # Distância vertical para queda
    exit-area-teleport: true            # Teleportar se sair da área
    teleport-to-last-checkpoint: true   # Voltar ao último checkpoint na queda
    count-falls: true                   # Contar quedas
    allowed-checkpoint-order: "strict"  # strict | any
    allow-flight: false                 # Permitir voo
    auto-start-timer: true              # Iniciar cronômetro ao entrar
    max-time-seconds: 600               # Tempo máximo (0 = ilimitado)
    min-players: 1                      # Mínimo de jogadores
    max-players: 20                     # Máximo de jogadores
    completion-title: "§aParabéns!"     # Título ao completar
    completion-subtitle: "Você completou o parkour!" # Subtítulo
    fall-message: "§cVocê caiu!"        # Mensagem ao cair
    checkpoint-message: "§aCheckpoint %d alcançado!" # Mensagem de checkpoint
    use-fall-detection: true            # Ativar detecção de queda automática
    use-area-teleport: true             # Ativar teleporte ao sair da área
    checkpoint-order: "strict"          # strict = ordem numérica, any = qualquer ordem
    lives: 0                            # Vidas (0 = infinitas)
    reset-on-complete: true             # Resetar checkpoint ao completar
```

## Descrição dos Campos

| Campo | Tipo | Padrão | Descrição |
|-------|------|--------|-----------|
| `fall-distance-threshold` | int | 10 | Distância vertical em blocos para considerar queda |
| `exit-area-teleport` | boolean | true | Teleportar jogador se sair da área definida |
| `teleport-to-last-checkpoint` | boolean | true | Teleportar para último checkpoint ao cair |
| `count-falls` | boolean | true | Incrementar contador de quedas |
| `allowed-checkpoint-order` | enum | "strict" | "strict" = ordem obrigatória, "any" = qualquer ordem |
| `allow-flight` | boolean | false | Permitir que jogador use voo |
| `auto-start-timer` | boolean | true | Iniciar cronômetro automaticamente ao entrar na área |
| `max-time-seconds` | int | 0 | Tempo máximo em segundos (0 = ilimitado) |
| `completion-title` | string | "§aParabéns!" | Título exibido ao completar |
| `completion-subtitle` | string | "" | Subtítulo ao completar |
| `fall-message` | string | "§cVocê caiu!" | Mensagem ao cair |
| `checkpoint-message` | string | "§aCheckpoint %d alcançado!" | Mensagem ao atingir checkpoint |
| `use-fall-detection` | boolean | true | Ativar detecção de queda |
| `lives` | int | 0 | Número de vidas (0 = infinitas) |
| `reset-on-complete` | boolean | true | Resetar progresso ao completar |
| `checkpoint-order` | enum | "strict" | strict = deve seguir ordem numérica; any = pode clicar qualquer checkpoint |

## Enums

### CheckpointOrder

| Valor | Descrição |
|-------|-----------|
| `strict` | Checkpoints devem ser ativados em ordem crescente (1, 2, 3...) |
| `any` | Jogador pode ativar checkpoints em qualquer ordem |

### CompletionTriggerType (uso interno)

| Valor | Descrição |
|-------|-----------|
| `CHECKPOINT_FINISH` | Último checkpoint = conclusão |
| `SEPARATE_TRIGGER` | Gatilho separado "finish" para conclusão |

## Exemplo de Evento YAML Completo

```yaml
schema-version: 1
id: meu_parkour
type: parkour
server: cobbleverse
display-name: "Parkour da Nascente"
description: "Percurso de parkour pela nascente do rio"
enabled: true
min-players: 1
max-players: 10
locations:
  LOBBY:
    dimension: minecraft:overworld
    x: 100.0
    y: 64.0
    z: 0.0
    yaw: 0.0
    pitch: 0.0
  ENTRANCE:
    dimension: minecraft:overworld
    x: 100.0
    y: 64.0
    z: 10.0
    yaw: 90.0
    pitch: 0.0
  EXIT:
    dimension: minecraft:overworld
    x: 100.0
    y: 64.0
    z: -10.0
    yaw: -90.0
    pitch: 0.0
triggers:
  checkpoint_1:
    type: SIGN_INTERACT
    enabled: true
    binding: "minecraft:overworld;100;65;10"
    conditions:
      - EVENT_IS_RUNNING
      - PLAYER_IS_PARTICIPANT
      - PLAYER_NOT_FINISHED
    actions:
      - type: SEND_MESSAGE
        arguments:
          message: "§aCheckpoint 1 alcançado!"
      - type: ADD_POINTS
        arguments:
          amount: "10"
  checkpoint_2:
    type: SIGN_INTERACT
    enabled: true
    binding: "minecraft:overworld;110;70;10"
    conditions:
      - EVENT_IS_RUNNING
      - PLAYER_IS_PARTICIPANT
      - PLAYER_NOT_FINISHED
    actions:
      - type: SEND_MESSAGE
        arguments:
          message: "§aCheckpoint 2 alcançado!"
      - type: ADD_POINTS
        arguments:
          amount: "10"
  finish:
    type: SIGN_INTERACT
    enabled: true
    binding: "minecraft:overworld;120;75;10"
    conditions:
      - EVENT_IS_RUNNING
      - PLAYER_IS_PARTICIPANT
      - PLAYER_NOT_FINISHED
    actions:
      - type: SEND_MESSAGE
        arguments:
          message: "§6§lVocê completou o parkour!"
      - type: SEND_TITLE
        arguments:
          title: "§aParabéns!"
          subtitle: "Tempo: %d segundos"
      - type: PLAYER_COMPLETE
        arguments: {}
```
