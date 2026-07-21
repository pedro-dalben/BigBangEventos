# Comandos

## Registro via Fabric

Use o `CommandRegistrationCallback` padrão do Fabric API para adicionar
comandos.

```java
CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
    dispatcher.register(Commands.literal("meuevento")
        .requires(source -> source.hasPermission(2))
        .then(Commands.literal("iniciar")
            .executes(ctx -> {
                ctx.getSource().sendSuccess(
                    () -> Component.literal("Iniciado!"), false);
                return 1;
            }))
    );
});
```

## Extensão da Árvore /evento

O comando `/evento` fica registrado pelo core. Módulos podem estender
a árvore se o core expuser um ponto de extensão.

Atualmente, módulos criam comandos separados (ex: `/parkour`). Em versões
futuras, um ponto de extensão permitirá adicionar subcomandos a `/evento`.

## Comandos Recomendados por Tipo

Para cada novo tipo de evento, considere:

| Comando | Finalidade |
|---------|-----------|
| `/meuevento` | Comando raiz do módulo |
| `/meuevento info` | Status do evento ativo |
| `/meuevento top` | Ranking atual |
| `/meuevento checkpoints` | Listar checkpoints |
| `/meuevento admin reset <player>` | Resetar jogador (admin) |

## Acesso a Serviços

Dentro do executor do comando, acesse o core via `BigBangEventos`:

```java
import com.pedrodalben.bigbangeventos.BigBangEventos;

// Obter engine
var engine = BigBangEventos.engine();

// Obter API
var api = BigBangEventos.api();
```

## Envio de Mensagens

```java
// Mensagem no chat
ctx.getSource().sendSuccess(
    () -> Component.literal("[Parkour] Mensagem"), false);

// Mensagem direta ao jogador
ServerPlayer player = ctx.getSource().getPlayerOrException();
player.sendSystemMessage(Component.literal("Mensagem"));
```

## Comandos com Argumentos

```java
Commands.literal("tp")
    .then(Commands.argument("jogador", EntityArgument.player())
        .executes(ctx -> {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "jogador");
            // ...
            return 1;
        }));
```

## Permissões

Use `requires(source -> source.hasPermission(2))` para comandos de admin.
Para comandos de jogador, não use `requires`.

## Exemplo

Veja `EventoCommand.java` no core (`command/EventoCommand.java`) como
referência de estrutura de comandos Fabric com Brigadier.
