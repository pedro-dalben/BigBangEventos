package br.com.bigbangcraft.eventos.gladiator;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.pedrodalben.bigbangeventos.BigBangEventos;
import com.pedrodalben.bigbangeventos.definition.EventDefinition;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public final class GladiatorCommandRegistrar {
    private GladiatorCommandRegistrar() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var gladiator = Commands.literal("evento").then(Commands.literal("gladiator"));

        dispatcher.register(gladiator.then(Commands.literal("mode")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("mode", StringArgumentType.word())
                        .executes(ctx -> {
                            String mode = StringArgumentType.getString(ctx, "mode");
                            return setMode(ctx.getSource(), mode);
                        }))
        ));

        dispatcher.register(gladiator.then(Commands.literal("lives")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 99))
                        .executes(ctx -> {
                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                            return setLives(ctx.getSource(), amount);
                        }))
        ));

        dispatcher.register(gladiator.then(Commands.literal("round-time")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("seconds", IntegerArgumentType.integer(10, 3600))
                        .executes(ctx -> {
                            int sec = IntegerArgumentType.getInteger(ctx, "seconds");
                            return setRoundTime(ctx.getSource(), sec);
                        }))
        ));

        dispatcher.register(gladiator.then(Commands.literal("info")
                .executes(ctx -> showInfo(ctx.getSource()))));
    }

    private static int setMode(CommandSourceStack source, String mode) {
        if (!mode.equals("FREE_FOR_ALL") && !mode.equals("LAST_PLAYER_STANDING")) {
            source.sendFailure(Component.literal("§cModo inválido. Use: FREE_FOR_ALL, LAST_PLAYER_STANDING"));
            return 0;
        }
        EventDefinition def = getSelected(source);
        if (def == null) return 0;
        settings(def).put("mode", mode);
        BigBangEventos.engine().save(def);
        source.sendSuccess(() -> Component.literal("§aModo definido: " + mode), true);
        return 1;
    }

    private static int setLives(CommandSourceStack source, int amount) {
        EventDefinition def = getSelected(source);
        if (def == null) return 0;
        settings(def).put("lives.initial", amount);
        settings(def).put("lives.maximum", amount);
        BigBangEventos.engine().save(def);
        source.sendSuccess(() -> Component.literal("§aVidas definidas: " + amount), true);
        return 1;
    }

    private static int setRoundTime(CommandSourceStack source, int seconds) {
        EventDefinition def = getSelected(source);
        if (def == null) return 0;
        settings(def).put("rounds.time-limit-seconds", seconds);
        BigBangEventos.engine().save(def);
        source.sendSuccess(() -> Component.literal("§aTempo de rodada definido: " + seconds + "s"), true);
        return 1;
    }

    private static int showInfo(CommandSourceStack source) {
        EventDefinition def = getSelected(source);
        if (def == null) return 0;
        Map<String, Object> s = settings(def);
        source.sendSuccess(() -> Component.literal("§6=== Gladiator Info ==="), false);
        source.sendSuccess(() -> Component.literal("§7Modo: §f" + s.getOrDefault("mode", "FREE_FOR_ALL")), false);
        source.sendSuccess(() -> Component.literal("§7Vidas: §f" + s.getOrDefault("lives.initial", 3)), false);
        source.sendSuccess(() -> Component.literal("§7Tempo rodada: §f" + s.getOrDefault("rounds.time-limit-seconds", 600) + "s"), false);
        source.sendSuccess(() -> Component.literal("§7Score per kill: §f" + s.getOrDefault("combat.score-per-kill", 1)), false);
        return 1;
    }

    private static EventDefinition getSelected(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return null;
        var session = BigBangEventos.engine().sessionByPlayer(player.getUUID()).orElse(null);
        if (session != null) return BigBangEventos.engine().definition(session.eventId()).orElse(null);
        var defs = BigBangEventos.engine().definitions().stream().filter(d -> d.type().equals("gladiator")).toList();
        return defs.isEmpty() ? null : defs.get(0);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> settings(EventDefinition def) {
        Map<String, Object> s = (Map<String, Object>) def.typeSettings().get("gladiator");
        if (s == null) {
            s = new java.util.LinkedHashMap<>();
            def.typeSetting("gladiator", s);
        }
        return s;
    }
}
