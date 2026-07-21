# Extensão — Módulo Parkour

## Como Estender o Módulo Parkour

O módulo Parkour foi projetado para ser extensível. Abaixo estão as
principais formas de adicionar funcionalidades.

## Adicionar Novo Tipo de Gatilho

Atualmente apenas `SIGN_INTERACT` é usado. Para adicionar `REGION_ENTER`:

1. Crie um listener Fabric para `UseBlockCallback` ou similar.
2. Detecte quando o jogador entra em uma região.
3. Dispare o gatilho via `engine.activateTrigger()`.

```java
public class RegionListener {
    public void onPlayerMove(ServerPlayer player, ServerLevel level) {
        BlockPos pos = player.blockPosition();
        String binding = level.dimension().location() + ";"
            + pos.getX() + ";" + pos.getY() + ";" + pos.getZ();

        // Buscar gatilhos com esse binding
        BigBangEventos.engine().definitions().forEach(def -> {
            def.triggers().stream()
                .filter(t -> t.type() == TriggerType.REGION_ENTER
                    && t.binding().orElse("").equals(binding))
                .findFirst()
                .ifPresent(t -> engine.activateTrigger(def.id(), t.id(),
                    player.getUUID(), player.getGameProfile().getName(),
                    permissions, effects));
        });
    }
}
```

## Adicionar Power-Ups

Crie um serviço de power-ups que escuta eventos de checkpoint:

```java
public class ParkourPowerUpService {
    private final Map<String, PowerUp> powerUps = new HashMap<>();

    public void registerPowerUp(String id, PowerUp powerUp) {
        powerUps.put(id, powerUp);
    }

    public void onCheckpointReached(UUID playerId, UUID sessionId,
                                     String checkpointId) {
        // Verificar se checkpoint tem power-up
        PowerUp pu = powerUps.get(checkpointId);
        if (pu != null) {
            pu.apply(playerId);
        }
    }
}

public interface PowerUp {
    void apply(UUID playerId);
    void remove(UUID playerId);
}
```

## Adicionar Múltiplas Tentativas

Modifique `ParkourSessionService` para suportar tentativas:

```java
public class ParkourAttemptService {
    private final Map<String, List<Attempt>> attempts = new HashMap<>();

    public record Attempt(int checkpoint, int falls, long elapsedMillis) {}

    public void registerAttempt(UUID playerId, UUID sessionId, Attempt attempt) {
        String key = playerId + ":" + sessionId;
        attempts.computeIfAbsent(key, k -> new ArrayList<>()).add(attempt);
    }

    public Optional<Attempt> bestAttempt(UUID playerId, UUID sessionId) {
        String key = playerId + ":" + sessionId;
        return attempts.getOrDefault(key, List.of()).stream()
            .min(Comparator.comparingLong(Attempt::elapsedMillis));
    }
}
```

## Adicionar Ranking por Categoria

Estenda `RankingStrategy` para classificar por quedas:

```java
public class ParkourRankingStrategy implements RankingStrategy {
    @Override
    public List<EventParticipant> rank(EventSession session) {
        List<EventParticipant> result = new ArrayList<>(session.participants());

        result.sort((a, b) -> {
            // 1º: quem concluiu primeiro (por tempo)
            int timeCmp = Long.compare(
                a.finishedAt().orElse(Instant.MAX).toEpochMilli(),
                b.finishedAt().orElse(Instant.MAX).toEpochMilli());
            if (timeCmp != 0) return timeCmp;

            // 2º: menos quedas
            int fallsA = Integer.parseInt(
                a.data().getOrDefault("falls", "0"));
            int fallsB = Integer.parseInt(
                b.data().getOrDefault("falls", "0"));
            return Integer.compare(fallsA, fallsB);
        });

        for (int i = 0; i < result.size(); i++) {
            result.get(i).position(i + 1);
        }
        return List.copyOf(result);
    }
}
```

## Adicionar GUI

Use Fabric API para criar telas:

```java
// Exemplo: abrir tela de seleção de checkpoint (admin)
public void openCheckpointScreen(ServerPlayer player,
                                  List<String> checkpoints) {
    // Usar net.minecraft.world.inventory.MenuProvider
    // ou pacotes de GUI como "libgui" se disponível
}
```

## Adicionar Notificações

Estenda `TriggerEffects` para enviar notificações customizadas:

```java
public class ParkourTriggerEffects implements TriggerEffects {
    private final MinecraftServer server;

    @Override
    public void message(UUID player, String message) {
        ServerPlayer p = server.getPlayerList().getPlayer(player);
        if (p != null) {
            p.sendSystemMessage(Component.literal("[Parkour] " + message));
        }
    }

    @Override
    public void executeConsole(String command) {
        server.getCommands().performPrefixedCommand(
            server.createCommandSourceStack(), command);
    }

    // Novos métodos
    public void sendTitle(UUID player, String title, String subtitle) {
        ServerPlayer p = server.getPlayerList().getPlayer(player);
        if (p != null) {
            p.sendSystemMessage(Component.literal(title));
        }
    }
}
```

## Integração com Outros Mods

### Com BigBangEssentials

```java
// Exemplo: enviar recompensa via BigBangEssentials
public void giveReward(UUID playerId, String rewardId) {
    // Se BigBangEssentials estiver disponível:
    // BigBangEssentials.api().rewards().give(playerId, rewardId);
}
```

### Com Economia

```java
// Exemplo: dar moedas ao completar
public void giveCoins(UUID playerId, int amount) {
    // Usar API de economia se disponível
    // VaultAPI ou similar
}
```

## Boas Práticas ao Estender

1. **Nunca modifique o core.** Toda extensão fica no módulo ou em outro
   módulo separado.
2. **Use listeners Fabric.** O Fabric API permite escutar qualquer evento
   sem modificar o BigBangEventos.
3. **Mantenha compatibilidade.** Novas funcionalidades não devem quebrar
   eventos parkour existentes.
4. **Documente.** Atualize este documento com novas capacidades.
5. **Teste.** Cada extensão deve ter seus próprios testes.
