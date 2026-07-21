# Gatilhos, Condições e Ações

## Visão Geral

Gatilhos associam uma ação do jogador (clicar numa placa, entrar numa
região, etc.) a uma sequência de condições e ações definidas na
configuração do evento.

Módulos podem criar gatilhos na definição do evento via código ou
configuração YAML.

## Trigger Types

| Tipo | Descrição |
|------|-----------|
| `SIGN_INTERACT` | Jogador clica em placa vinculada |
| `REGION_ENTER` | Jogador entra em área definida |
| `REGION_EXIT` | Jogador sai de área definida |
| `PRESSURE_PLATE` | Jogador aciona placa de pressão |
| `BUTTON_INTERACT` | Jogador clica em botão |
| `LEVER_INTERACT` | Jogador interage com alavanca |
| `TIMER` | Dispara após um tempo |
| `MANUAL` | Disparado via comando ou código |

## Condições (ConditionType)

| Condição | Descrição |
|----------|-----------|
| `PLAYER_IS_PARTICIPANT` | Jogador está participando |
| `EVENT_IS_RUNNING` | Evento está em execução |
| `PLAYER_NOT_FINISHED` | Jogador não concluiu |
| `PLAYER_AT_STAGE` | Jogador está em estágio específico |
| `PLAYER_HAS_CHECKPOINT` | Jogador tem checkpoint específico |
| `PLAYER_HAS_ALL_CHECKPOINTS` | Jogador tem todos checkpoints |
| `PLAYER_HAS_ITEM` | Jogador tem item no inventário |
| `PLAYER_HAS_PERMISSION` | Jogador tem permissão |
| `PREVIOUS_TRIGGER_COMPLETED` | Gatilho anterior foi executado |
| `MAX_USES_NOT_REACHED` | Limite de usos não atingido |
| `COOLDOWN_READY` | Cooldown do gatilho passou |

## Ações (ActionType)

| Ação | Descrição |
|------|-----------|
| `SEND_MESSAGE` | Envia mensagem ao jogador |
| `SEND_TITLE` | Envia título na tela |
| `PLAY_SOUND` | Toca som |
| `TELEPORT` | Teleporta jogador |
| `EXECUTE_COMMAND` | Executa comando no console |
| `ADD_POINTS` | Adiciona pontos |
| `REMOVE_POINTS` | Remove pontos |
| `COMPLETE_STAGE` | Completa estágio |
| `COMPLETE_CHECKPOINT` | Marca checkpoint como completo |
| `PLAYER_COMPLETE` | Marca jogador como concluído |
| `EVENT_FINISH` | Finaliza evento |
| `ELIMINATE_PLAYER` | Elimina jogador |
| `ENABLE_TRIGGER` | Habilita gatilho |
| `DISABLE_TRIGGER` | Desabilita gatilho |
| `GIVE_ITEM` | Dá item ao jogador |
| `REMOVE_ITEM` | Remove item do jogador |

## Criar Gatilho via Código

```java
EventTrigger trigger = new EventTrigger("chegada", TriggerType.SIGN_INTERACT);
trigger.addCondition(ConditionType.EVENT_IS_RUNNING);
trigger.addCondition(ConditionType.PLAYER_NOT_FINISHED);
trigger.addAction(new TriggerAction(ActionType.SEND_MESSAGE,
    Map.of("message", "Você completou o percurso!")));
trigger.addAction(new TriggerAction(ActionType.ADD_POINTS,
    Map.of("amount", "100")));
trigger.addAction(new TriggerAction(ActionType.PLAYER_COMPLETE, Map.of()));
trigger.maxUses(1);
definition.putTrigger(trigger);
```

## Vincular a uma Placa

Use o comando `/evento trigger bind <nome>` e clique na placa desejada.

O binding associa a string `dimensão;x;y;z` ao gatilho. O core detecta
o clique em placa (via `UseBlockCallback`) e executa o gatilho vinculado.

## Custom Evaluation

Módulos que precisam de condições ou ações customizadas (não cobertas
pelos enums) devem implementar sua própria lógica de avaliação:

```java
// No lugar de usar core triggers, crie seu próprio serviço
public class MeuServicoTrigger {
    public boolean avaliarCondicaoCustomizada(EventParticipant p) {
        return p.data().getOrDefault("fase", "1").equals("2");
    }

    public void executarAcaoCustomizada(ServerPlayer player) {
        // Lógica específica do módulo
    }
}
```

## Execução via API

```java
BigBangEventos.engine().activateTrigger(
    "meu_evento",      // eventId
    "chegada",         // triggerId
    playerId,          // UUID do jogador
    playerName,        // Nome do jogador
    permissions,       // PermissionChecker
    effects            // TriggerEffects
);
```

## Trigger Effects

`TriggerEffects` permite enviar mensagens e executar comandos:

```java
TriggerEffects effects = new TriggerEffects() {
    public void message(UUID player, String message) {
        // envia mensagem
    }
    public void executeConsole(String command) {
        // executa comando
    }
};
```
