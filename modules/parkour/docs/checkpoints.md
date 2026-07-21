# Checkpoints — Módulo Parkour

## O que são

Checkpoints são pontos de passagem obrigatórios no percurso do parkour.
Cada checkpoint é um gatilho (`EventTrigger`) do tipo `SIGN_INTERACT`
vinculado a uma placa específica.

## Como Criar

### Via Comando

```bash
# Criar gatilho
/evento trigger create checkpoint_1 sign_interact

# Vincular a placa
/evento trigger bind checkpoint_1
# Clique na placa desejada

# Repetir para cada checkpoint:
/evento trigger create checkpoint_2 sign_interact
/evento trigger bind checkpoint_2

# Criar gatilho de chegada
/evento trigger create finish sign_interact
/evento trigger bind finish
```

### Via YAML

```yaml
triggers:
  checkpoint_1:
    type: SIGN_INTERACT
    enabled: true
    binding: "minecraft:overworld;100;65;10"
    max-uses: 1
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
```

## Ordem dos Checkpoints

### Strict (padrão)

Os checkpoints devem ser ativados em ordem crescente. O jogador não pode
pular checkpoints.

- `checkpoint_2` só funciona se `checkpoint_1` foi completado.
- Condição extra: `PLAYER_HAS_CHECKPOINT(N-1)` (avaliada pelo módulo).

### Any

O jogador pode ativar checkpoints em qualquer ordem.

- Útil para parkours abertos onde a ordem não importa.
- Apenas o checkpoint `finish` é verificado.

## Validação de Checkpoint

O módulo Parkour valida cada checkpoint via `ParkourCheckpointService`:

```java
public class ParkourCheckpointService {
    private final ParkourDataStore dataStore;

    public boolean validateCheckpoint(UUID playerId, UUID sessionId,
                                      String checkpointId, String orderMode) {
        ParkourPlayerData data = dataStore.get(playerId, sessionId);

        if (orderMode.equals("strict")) {
            int checkpointNum = extractNumber(checkpointId);
            int lastCompleted = data.getLastCheckpoint();

            // Só permite se é o próximo
            if (checkpointNum != lastCompleted + 1) {
                return false;
            }
        }

        // Marcar checkpoint como completo
        data.addCompletedCheckpoint(checkpointId);
        data.setLastCheckpoint(checkpointNum);

        // Se é o último, marcar como pronto para finalizar
        if (isLastCheckpoint(checkpointId)) {
            data.setReadyToFinish(true);
        }

        return true;
    }

    private int extractNumber(String checkpointId) {
        return Integer.parseInt(checkpointId.replace("checkpoint_", ""));
    }
}
```

## Checkpoint de Chegada (finish)

O gatilho `finish` é especial: quando ativado, o módulo marca o jogador
como completo e registra o tempo final.

```java
// No ParkourSessionService
public void finishPlayer(UUID playerId, UUID sessionId) {
    ParkourPlayerData data = dataStore.get(playerId, sessionId);
    data.setFinishedAt(System.currentTimeMillis());

    long elapsed = data.elapsedMillis();

    engine.complete(eventId, playerId, CompletionMode.FIRST_FINISHER);
}
```

## Persistência

Dados de checkpoint são persistidos em
`config/bigbangeventos/modules/parkour/players/{playerId}_{sessionId}.yml`:

```yaml
playerId: uuid
sessionId: uuid
startedAt: 1712345678000
finishedAt: 1712345723000
lastCheckpoint: 3
falls: 2
completedCheckpoints:
  - checkpoint_1
  - checkpoint_2
  - checkpoint_3
readyToFinish: false
```

## Dados no Participante (core)

O módulo também armazena dados simples no `EventParticipant.data()`:

```java
participant.data("checkpoint", "3");      // último checkpoint
participant.data("falls", "2");            // quedas
participant.data("status", "active");     // active | finished | fell
```

## Boas Práticas

1. Sempre use `max-uses: 1` em checkpoints para evitar re-ativação.
2. Coloque placas em locais visíveis e de fácil acesso.
3. Teste a ordem dos checkpoints antes de abrir o evento.
4. Configure `fall-distance-threshold` de acordo com o percurso.
5. Use `SEND_TITLE` ou `SEND_MESSAGE` para feedback visual ao jogador.
6. Para percursos longos, adicione checkpoints a cada 10-20 blocos.
