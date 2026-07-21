# Threading

## Regra Fundamental

TODO acesso a APIs do Minecraft DEVE ocorrer na **server thread** (thread
principal do servidor). Acessar o mundo, jogadores, entidades ou
inventários de outra thread causa crashes e corrupção de dados.

## PlatformScheduler

O core fornece `PlatformScheduler` (acessível via `EventModuleContext`)
para agendar tarefas de forma thread-safe.

```java
PlatformScheduler scheduler = ctx.scheduler();
```

### Métodos

| Método | Descrição |
|--------|-----------|
| `isServerThread()` | Retorna true se estamos na server thread |
| `executeOnServerThread(Runnable)` | Executa Runnable na server thread |
| `schedule(Delay, Runnable)` | Executa depois de um atraso |
| `scheduleRepeating(Interval, Runnable)` | Executa repetidamente |

### Exemplos

```java
// Verificar thread atual
if (scheduler.isServerThread()) {
    // seguro acessar Minecraft
} else {
    scheduler.executeOnServerThread(() -> {
        // seguro acessar Minecraft
    });
}

// Agendar tarefa com delay
ScheduledHandle handle = scheduler.schedule(
    Duration.ofSeconds(10),
    () -> { /* código na server thread */ }
);

// Tarefa repetitiva
ScheduledHandle repeating = scheduler.scheduleRepeating(
    Duration.ofSeconds(30),
    () -> { /* código na server thread */ }
);

// Cancelar
handle.cancel();
```

## Tarefas Assíncronas e Callbacks

Se você usa APIs assíncronas (ex: HTTP requests, cálculos pesados),
sempre volte para a server thread antes de tocar no Minecraft:

```java
// Em uma thread qualquer:
CompletableFuture.supplyAsync(() -> {
    // trabalho pesado (não Minecraft)
    return resultado;
}).thenAccept(resultado -> {
    // Voltar para server thread
    scheduler.executeOnServerThread(() -> {
        // Agora seguro acessar Minecraft
        ServerPlayer player = server.getPlayerList()
            .getPlayer(playerId);
        if (player != null) {
            player.sendSystemMessage(
                Component.literal("Resultado: " + resultado));
        }
    });
});
```

## Tarefas Demoradas

Nunca execute tarefas demoradas (IO, rede, cálculo pesado) na server
thread. Isso vai travar o servidor (lag). Use threads separadas para
trabalho pesado:

```java
// Java standard: nova thread
new Thread(() -> {
    // IO, rede, cálculo
    String result = fazerAlgoDemorado();
    // Voltar para server thread para tocar Minecraft
    scheduler.executeOnServerThread(() -> {
        usarResultadoNoMinecraft(result);
    });
}).start();
```

## Acesso a Dados do Módulo

Dados gerenciados pelo módulo (caches, arquivos) podem ser acessados de
qualquer thread, desde que:

1. Use estruturas thread-safe (`ConcurrentHashMap`, `synchronized`).
2. Não toque em Minecraft fora da server thread.

## Resumo

| Operação | Thread |
|----------|--------|
| Acesso a `EventDefinition` | Qualquer (objetos imutáveis) |
| Acesso a `EventParticipant.data()` | Server thread (pode ser chamado de hooks do core que já estão na server thread) |
| Comandos | Server thread (Fabric já garante) |
| Listeners de bloco/entidade | Server thread |
| Listeners de tick | Server thread |
| IO (arquivos, rede) | Thread separada |
| Cálculo pesado | Thread separada |
| Acesso a `ServerPlayer`, `ServerLevel`, `ItemStack` | Server thread APENAS |
| Mensagens para jogador | Server thread |
| Teleporte | Server thread |
| Modificar inventário | Server thread |
