package br.com.bigbangcraft.eventos.pokegladiator;

import com.pedrodalben.bigbangeventos.api.module.EventModuleContext;
import com.pedrodalben.bigbangeventos.definition.EventDefinition;
import com.pedrodalben.bigbangeventos.session.EventSession;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import static net.minecraft.commands.Commands.literal;

import java.util.Map;

public final class PokeGladiatorCommandRegistrar {
    private PokeGladiatorCommandRegistrar() {}

    public static void register(EventModuleContext ctx) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> {
            dispatcher.register(
                literal("evento").then(
                    literal("poke-gladiator")
                        .requires(src -> src.hasPermission(2))
                        .then(literal("modo")
                            .then(literal("LAST_TRAINER_STANDING").executes(c -> setMode(c.getSource().getPlayerOrException(), "LAST_TRAINER_STANDING", ctx)))
                            .then(literal("HYBRID_FREE_FOR_ALL").executes(c -> setMode(c.getSource().getPlayerOrException(), "HYBRID_FREE_FOR_ALL", ctx)))
                        )
                        .then(literal("vidas")
                            .then(Commands.argument("quantidade", IntegerArgumentType.integer(0, 99))
                                .executes(c -> {
                                    int v = IntegerArgumentType.getInteger(c, "quantidade");
                                    return setLives(c.getSource().getPlayerOrException(), v, ctx);
                                })
                            )
                        )
                        .then(literal("tempo")
                            .then(Commands.argument("segundos", IntegerArgumentType.integer(10, 3600))
                                .executes(c -> {
                                    int s = IntegerArgumentType.getInteger(c, "segundos");
                                    return setRoundTime(c.getSource().getPlayerOrException(), s, ctx);
                                })
                            )
                        )
                        .then(literal("info").executes(c -> {
                            showInfo(c.getSource().getPlayerOrException(), ctx);
                            return 1;
                        }))
                        .then(literal("ranking").executes(c -> {
                            showRanking(c.getSource().getPlayerOrException(), ctx);
                            return 1;
                        }))
                )
            );
        });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getOrCreateSettings(EventDefinition def) {
        Object raw = def.typeSettings().get("poke_gladiator");
        Map<String, Object> map;
        if (raw instanceof Map) {
            map = new java.util.HashMap<>((Map<String, Object>) raw);
        } else {
            map = new java.util.HashMap<>();
        }
        return map;
    }

    private static void saveSettings(EventDefinition def, Map<String, Object> map) {
        def.typeSettings().put("poke_gladiator", map);
    }

    private static int setMode(net.minecraft.server.level.ServerPlayer player, String mode, EventModuleContext ctx) {
        EventSession session = ctx.api().getSessionByPlayer(player.getUUID()).orElse(null);
        if (session == null) {
            player.sendSystemMessage(Component.literal(PokeGladiatorMessages.NOT_IN_SESSION));
            return 0;
        }
        EventDefinition def = ctx.api().findEvent(session.eventId()).orElse(null);
        if (def == null) return 0;
        Map<String, Object> map = getOrCreateSettings(def);
        map.put("mode", mode);
        saveSettings(def, map);
        ctx.api().saveEvent(def);
        player.sendSystemMessage(Component.literal("§aModo definido para: " + mode));
        return 1;
    }

    private static int setLives(net.minecraft.server.level.ServerPlayer player, int lives, EventModuleContext ctx) {
        EventSession session = ctx.api().getSessionByPlayer(player.getUUID()).orElse(null);
        if (session == null) {
            player.sendSystemMessage(Component.literal(PokeGladiatorMessages.NOT_IN_SESSION));
            return 0;
        }
        EventDefinition def = ctx.api().findEvent(session.eventId()).orElse(null);
        if (def == null) return 0;
        Map<String, Object> map = getOrCreateSettings(def);
        map.put("initial_lives", lives);
        map.put("max_lives", lives);
        saveSettings(def, map);
        ctx.api().saveEvent(def);
        player.sendSystemMessage(Component.literal("§aVidas definidas para: " + lives));
        return 1;
    }

    private static int setRoundTime(net.minecraft.server.level.ServerPlayer player, int seconds, EventModuleContext ctx) {
        EventSession session = ctx.api().getSessionByPlayer(player.getUUID()).orElse(null);
        if (session == null) {
            player.sendSystemMessage(Component.literal(PokeGladiatorMessages.NOT_IN_SESSION));
            return 0;
        }
        EventDefinition def = ctx.api().findEvent(session.eventId()).orElse(null);
        if (def == null) return 0;
        Map<String, Object> map = getOrCreateSettings(def);
        map.put("round_time", seconds);
        saveSettings(def, map);
        ctx.api().saveEvent(def);
        player.sendSystemMessage(Component.literal("§aTempo de rodada: " + seconds + "s"));
        return 1;
    }

    private static void showInfo(net.minecraft.server.level.ServerPlayer player, EventModuleContext ctx) {
        EventSession session = ctx.api().getSessionByPlayer(player.getUUID()).orElse(null);
        if (session == null) {
            player.sendSystemMessage(Component.literal(PokeGladiatorMessages.NOT_IN_SESSION));
            return;
        }
        EventDefinition def = ctx.api().findEvent(session.eventId()).orElse(null);
        if (def == null) return;

        player.sendSystemMessage(Component.literal("§6§l[PokeGladiator]"));
        player.sendSystemMessage(Component.literal("§7Modo: §f" + PokeGladiatorConfiguration.mode(def)));
        player.sendSystemMessage(Component.literal("§7Vidas: §f" + PokeGladiatorConfiguration.initialLives(def)));
        player.sendSystemMessage(Component.literal("§7Tempo: §f" + PokeGladiatorConfiguration.roundTime(def) + "s"));
        player.sendSystemMessage(Component.literal("§7Politica de perda: §f" + PokeGladiatorConfiguration.lossPolicy(def)));
        player.sendSystemMessage(Component.literal("§7Recuperacao: §f" + PokeGladiatorConfiguration.recoveryPolicy(def)));
    }

    private static void showRanking(net.minecraft.server.level.ServerPlayer player, EventModuleContext ctx) {
        EventSession session = ctx.api().getSessionByPlayer(player.getUUID()).orElse(null);
        if (session == null) {
            player.sendSystemMessage(Component.literal(PokeGladiatorMessages.NOT_IN_SESSION));
            return;
        }
        EventDefinition def = ctx.api().findEvent(session.eventId()).orElse(null);
        if (def == null) return;

        PokeGladiatorRankingService ranking = new PokeGladiatorRankingService();
        var entries = ranking.rank(session, PokeGladiatorConfiguration.rankingStrategy(def));

        player.sendSystemMessage(Component.literal("§6§l[PokeGladiator - Ranking]"));
        for (var entry : entries) {
            String status = entry.eliminated() ? "§cEliminado" : "§aAtivo";
            player.sendSystemMessage(Component.literal(
                entry.position() + "§f " + entry.name() + " §7- §f" + entry.kills() + " abates §7- " + entry.lives() + " vidas - " + status));
        }
    }
}
