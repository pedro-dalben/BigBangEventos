package br.com.bigbangcraft.eventos.parkour;

import br.com.bigbangcraft.eventos.parkour.model.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.pedrodalben.bigbangeventos.api.BigBangEventosApi;
import com.pedrodalben.bigbangeventos.api.OperationResult;
import com.pedrodalben.bigbangeventos.command.EventoCommand;
import com.pedrodalben.bigbangeventos.definition.EventDefinition;
import com.pedrodalben.bigbangeventos.definition.EventLocation;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.stream.Collectors;

public final class ParkourCommandRegistrar {

    private static BigBangEventosApi api;
    private static ParkourSessionService sessionService;

    private ParkourCommandRegistrar() {}

    public static void init(BigBangEventosApi apiInstance, ParkourSessionService service) {
        api = apiInstance;
        sessionService = service;
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var eventoNode = dispatcher.getRoot().getChild("evento");
        if (eventoNode == null) return;

        var parkour = Commands.literal("parkour")
                .requires(ParkourCommandRegistrar::admin)
                .then(Commands.literal("set-start")
                        .executes(ParkourCommandRegistrar::setStart))
                .then(Commands.literal("set-finish")
                        .executes(ctx -> setFinish(ctx, 2.0))
                        .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0.1))
                                .executes(ctx -> setFinish(ctx, DoubleArgumentType.getDouble(ctx, "radius")))))
                .then(Commands.literal("set-finish-radius")
                        .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0.1))
                                .executes(ParkourCommandRegistrar::setFinishRadius)))
                .then(Commands.literal("set-fall-y")
                        .then(Commands.argument("height", DoubleArgumentType.doubleArg())
                                .executes(ParkourCommandRegistrar::setFallY)))
                .then(Commands.literal("reset-mode")
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests(ENUM_SUGGESTOR(ParkourResetMode.values()))
                                .executes(ParkourCommandRegistrar::setResetMode)))
                .then(Commands.literal("max-time")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                                .executes(ParkourCommandRegistrar::setMaxTime)))
                .then(Commands.literal("max-attempts")
                        .then(Commands.argument("quantity", IntegerArgumentType.integer(0))
                                .executes(ParkourCommandRegistrar::setMaxAttempts)))
                .then(Commands.literal("finish-mode")
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests(ENUM_SUGGESTOR(ParkourFinishMode.values()))
                                .executes(ParkourCommandRegistrar::setFinishMode)))
                .then(Commands.literal("ranking-strategy")
                        .then(Commands.argument("strategy", StringArgumentType.word())
                                .suggests(ENUM_SUGGESTOR(ParkourRankingStrategy.values()))
                                .executes(ParkourCommandRegistrar::setRankingStrategy)))
                .then(Commands.literal("complete-destination")
                        .then(Commands.argument("destination", StringArgumentType.word())
                                .suggests(ENUM_SUGGESTOR(ParkourCompleteDestination.values()))
                                .executes(ParkourCommandRegistrar::setCompleteDestination)))
                .then(Commands.literal("checkpoints-required")
                        .then(Commands.argument("value", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(new String[]{"true", "false"}, builder))
                                .executes(ParkourCommandRegistrar::setCheckpointsRequired)))
                .then(checkpointCommands())
                .then(Commands.literal("info")
                        .executes(ParkourCommandRegistrar::info))
                .then(Commands.literal("validate")
                        .executes(ParkourCommandRegistrar::validate));

        eventoNode.addChild(parkour.build());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> checkpointCommands() {
        return Commands.literal("checkpoint")
                .then(Commands.literal("add")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> addCheckpoint(ctx, 2.0))
                                .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0.1))
                                        .executes(ctx -> addCheckpoint(ctx, DoubleArgumentType.getDouble(ctx, "radius"))))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(CHECKPOINT_SUGGESTOR)
                                .executes(ParkourCommandRegistrar::removeCheckpoint)))
                .then(Commands.literal("list")
                        .executes(ParkourCommandRegistrar::listCheckpoints))
                .then(Commands.literal("info")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(CHECKPOINT_SUGGESTOR)
                                .executes(ParkourCommandRegistrar::checkpointInfo)))
                .then(Commands.literal("set-radius")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(CHECKPOINT_SUGGESTOR)
                                .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0.1))
                                        .executes(ParkourCommandRegistrar::setCheckpointRadius))))
                .then(Commands.literal("teleport")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(CHECKPOINT_SUGGESTOR)
                                .executes(ParkourCommandRegistrar::teleportCheckpoint)));
    }

    // === Command handlers ===

    private static int setStart(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            EventDefinition def = selected(player);
            var pos = player.position();
            ParkourConfiguration.setStartLocation(def, new EventLocation(
                    def.serverId(),
                    player.level().dimension().location().toString(),
                    pos.x, pos.y, pos.z, player.getYRot(), player.getXRot()));
            api.saveEvent(def);
            return message(ctx, ParkourMessages.START_SET);
        } catch (Exception e) {
            return message(ctx, "§c" + e.getMessage());
        }
    }

    private static int setFinish(CommandContext<CommandSourceStack> ctx, double radius) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            EventDefinition def = selected(player);
            var pos = player.position();
            ParkourConfiguration.setFinishLocation(def, new EventLocation(
                    def.serverId(),
                    player.level().dimension().location().toString(),
                    pos.x, pos.y, pos.z, player.getYRot(), player.getXRot()));
            ParkourConfiguration.setFinishRadius(def, radius);
            api.saveEvent(def);
            return message(ctx, ParkourMessages.finishSet(String.valueOf(radius)));
        } catch (Exception e) {
            return message(ctx, "§c" + e.getMessage());
        }
    }

    private static int setFinishRadius(CommandContext<CommandSourceStack> ctx) {
        try {
            EventDefinition def = selected(ctx);
            double radius = DoubleArgumentType.getDouble(ctx, "radius");
            ParkourConfiguration.setFinishRadius(def, radius);
            api.saveEvent(def);
            return message(ctx, ParkourMessages.finishRadiusSet(String.valueOf(radius)));
        } catch (Exception e) {
            return message(ctx, "§c" + e.getMessage());
        }
    }

    private static int setFallY(CommandContext<CommandSourceStack> ctx) {
        try {
            EventDefinition def = selected(ctx);
            double height = DoubleArgumentType.getDouble(ctx, "height");
            ParkourConfiguration.setFallYLevel(def, height);
            api.saveEvent(def);
            return message(ctx, ParkourMessages.fallYSet(height));
        } catch (Exception e) {
            return message(ctx, "§c" + e.getMessage());
        }
    }

    private static int setResetMode(CommandContext<CommandSourceStack> ctx) {
        try {
            EventDefinition def = selected(ctx);
            ParkourResetMode mode = ParkourResetMode.valueOf(
                    StringArgumentType.getString(ctx, "mode").toUpperCase(Locale.ROOT));
            ParkourConfiguration.setResetMode(def, mode);
            api.saveEvent(def);
            return message(ctx, ParkourMessages.resetModeSet(mode.name()));
        } catch (Exception e) {
            return message(ctx, "§cValor inválido. Use: START, LAST_CHECKPOINT");
        }
    }

    private static int setMaxTime(CommandContext<CommandSourceStack> ctx) {
        try {
            EventDefinition def = selected(ctx);
            int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
            ParkourConfiguration.setMaxTimeSeconds(def, seconds);
            api.saveEvent(def);
            return message(ctx, ParkourMessages.maxTimeSet(seconds));
        } catch (Exception e) {
            return message(ctx, "§c" + e.getMessage());
        }
    }

    private static int setMaxAttempts(CommandContext<CommandSourceStack> ctx) {
        try {
            EventDefinition def = selected(ctx);
            int qty = IntegerArgumentType.getInteger(ctx, "quantity");
            ParkourConfiguration.setMaxAttempts(def, qty);
            api.saveEvent(def);
            return message(ctx, ParkourMessages.maxAttemptsSet(qty));
        } catch (Exception e) {
            return message(ctx, "§c" + e.getMessage());
        }
    }

    private static int setFinishMode(CommandContext<CommandSourceStack> ctx) {
        try {
            EventDefinition def = selected(ctx);
            ParkourFinishMode mode = ParkourFinishMode.valueOf(
                    StringArgumentType.getString(ctx, "mode").toUpperCase(Locale.ROOT));
            ParkourConfiguration.setFinishMode(def, mode);
            api.saveEvent(def);
            return message(ctx, ParkourMessages.finishModeSet(mode.name()));
        } catch (Exception e) {
            return message(ctx, "§cValor inválido. Use: FIRST_FINISHER, ALL_FINISHERS, MANUAL, TOP_N");
        }
    }

    private static int setRankingStrategy(CommandContext<CommandSourceStack> ctx) {
        try {
            EventDefinition def = selected(ctx);
            ParkourRankingStrategy strategy = ParkourRankingStrategy.valueOf(
                    StringArgumentType.getString(ctx, "strategy").toUpperCase(Locale.ROOT));
            ParkourConfiguration.setRankingStrategy(def, strategy);
            api.saveEvent(def);
            return message(ctx, ParkourMessages.rankingStrategySet(strategy.name()));
        } catch (Exception e) {
            return message(ctx, "§cValor inválido. Use: TIME_ASCENDING");
        }
    }

    private static int setCompleteDestination(CommandContext<CommandSourceStack> ctx) {
        try {
            EventDefinition def = selected(ctx);
            ParkourCompleteDestination dest = ParkourCompleteDestination.valueOf(
                    StringArgumentType.getString(ctx, "destination").toUpperCase(Locale.ROOT));
            ParkourConfiguration.setCompleteDestination(def, dest);
            api.saveEvent(def);
            return message(ctx, ParkourMessages.completeDestinationSet(dest.name()));
        } catch (Exception e) {
            return message(ctx, "§cValor inválido. Use: EXIT, SPECTATOR, ORIGINAL, STAY");
        }
    }

    private static int setCheckpointsRequired(CommandContext<CommandSourceStack> ctx) {
        try {
            EventDefinition def = selected(ctx);
            boolean value = Boolean.parseBoolean(StringArgumentType.getString(ctx, "value"));
            ParkourConfiguration.setCheckpointsRequired(def, value);
            api.saveEvent(def);
            return message(ctx, ParkourMessages.checkpointsRequiredSet(value));
        } catch (Exception e) {
            return message(ctx, "§c" + e.getMessage());
        }
    }

    // === Checkpoint handlers ===

    private static int addCheckpoint(CommandContext<CommandSourceStack> ctx, double radius) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            EventDefinition def = selected(player);
            String id = StringArgumentType.getString(ctx, "id");

            if (sessionService.checkpointService().findCheckpoint(def.id(), id).isPresent()) {
                return message(ctx, "§cCheckpoint '" + id + "' já existe.");
            }

            var pos = player.position();
            List<ParkourCheckpoint> checkpoints = sessionService.checkpointService().getCheckpoints(def.id());
            int order = checkpoints.size();

            EventLocation loc = new EventLocation(
                    def.serverId(),
                    player.level().dimension().location().toString(),
                    pos.x, pos.y, pos.z, player.getYRot(), player.getXRot());
            ParkourCheckpoint cp = new ParkourCheckpoint(id, order, loc, radius);
            sessionService.checkpointService().addCheckpoint(def.id(), cp);
            return message(ctx, ParkourMessages.checkpointAdded(id, order, radius));
        } catch (Exception e) {
            return message(ctx, "§c" + e.getMessage());
        }
    }

    private static int removeCheckpoint(CommandContext<CommandSourceStack> ctx) {
        try {
            EventDefinition def = selected(ctx);
            String id = StringArgumentType.getString(ctx, "id");
            if (sessionService.checkpointService().findCheckpoint(def.id(), id).isEmpty()) {
                return message(ctx, ParkourMessages.checkpointNotFound(id));
            }
            sessionService.checkpointService().removeCheckpoint(def.id(), id);
            return message(ctx, ParkourMessages.checkpointRemoved(id));
        } catch (Exception e) {
            return message(ctx, "§c" + e.getMessage());
        }
    }

    private static int listCheckpoints(CommandContext<CommandSourceStack> ctx) {
        try {
            EventDefinition def = selected(ctx);
            List<ParkourCheckpoint> checkpoints = sessionService.checkpointService().getCheckpoints(def.id());
            if (checkpoints.isEmpty()) {
                return message(ctx, ParkourMessages.NO_CHECKPOINTS);
            }
            StringBuilder sb = new StringBuilder(ParkourMessages.CHECKPOINT_LIST_HEADER);
            for (ParkourCheckpoint cp : checkpoints) {
                String status = cp.enabled() ? "§ahabilitado§r" : "§7desabilitado§r";
                sb.append("\n").append(ParkourMessages.checkpointListEntry(
                        cp.id(), cp.order(), cp.radius(), status));
            }
            return message(ctx, sb.toString());
        } catch (Exception e) {
            return message(ctx, "§c" + e.getMessage());
        }
    }

    private static int checkpointInfo(CommandContext<CommandSourceStack> ctx) {
        try {
            EventDefinition def = selected(ctx);
            String id = StringArgumentType.getString(ctx, "id");
            ParkourCheckpoint cp = sessionService.checkpointService()
                    .findCheckpoint(def.id(), id).orElse(null);
            if (cp == null) {
                return message(ctx, ParkourMessages.checkpointNotFound(id));
            }
            return message(ctx, ParkourMessages.checkpointInfo(
                    cp.id(), cp.order(), cp.radius(),
                    cp.location().x(), cp.location().y(), cp.location().z(),
                    cp.location().dimension()));
        } catch (Exception e) {
            return message(ctx, "§c" + e.getMessage());
        }
    }

    private static int setCheckpointRadius(CommandContext<CommandSourceStack> ctx) {
        try {
            EventDefinition def = selected(ctx);
            String id = StringArgumentType.getString(ctx, "id");
            double radius = DoubleArgumentType.getDouble(ctx, "radius");
            ParkourCheckpoint cp = sessionService.checkpointService()
                    .findCheckpoint(def.id(), id).orElse(null);
            if (cp == null) {
                return message(ctx, ParkourMessages.checkpointNotFound(id));
            }
            List<ParkourCheckpoint> list = new ArrayList<>(sessionService.checkpointService().getCheckpoints(def.id()));
            list.replaceAll(c -> c.id().equals(id)
                    ? new ParkourCheckpoint(id, c.order(), c.location(), radius) : c);
            sessionService.checkpointService().setCheckpoints(def.id(), list);
            return message(ctx, ParkourMessages.checkpointRadiusSet(id, radius));
        } catch (Exception e) {
            return message(ctx, "§c" + e.getMessage());
        }
    }

    private static int teleportCheckpoint(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            EventDefinition def = selected(player);
            String id = StringArgumentType.getString(ctx, "id");
            ParkourCheckpoint cp = sessionService.checkpointService()
                    .findCheckpoint(def.id(), id).orElse(null);
            if (cp == null) {
                return message(ctx, ParkourMessages.checkpointNotFound(id));
            }
            EventLocation loc = cp.location();
            var dimKey = net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    net.minecraft.resources.ResourceLocation.parse(loc.dimension()));
            var world = player.server.getLevel(dimKey);
            if (world != null) {
                player.teleportTo(world, loc.x(), loc.y(), loc.z(), loc.yaw(), loc.pitch());
            }
            return message(ctx, ParkourMessages.checkpointTeleported(id));
        } catch (Exception e) {
            return message(ctx, "§c" + e.getMessage());
        }
    }

    // === Info and Validate ===

    private static int info(CommandContext<CommandSourceStack> ctx) {
        try {
            EventDefinition def = selected(ctx);
            StringBuilder sb = new StringBuilder("§6=== Parkour: " + def.id() + " ===\n");
            sb.append("§7Início: §f");
            ParkourConfiguration.getStartLocation(def).ifPresentOrElse(
                    l -> sb.append(String.format("%.0f,%.0f,%.0f (%s)", l.x(), l.y(), l.z(), l.dimension())),
                    () -> sb.append("§cNÃO DEFINIDO"));
            sb.append("\n§7Saída: §f");
            ParkourConfiguration.getFinishLocation(def).ifPresentOrElse(
                    l -> sb.append(String.format("%.0f,%.0f,%.0f (%s) raio=%.1f",
                            l.x(), l.y(), l.z(), l.dimension(), ParkourConfiguration.getFinishRadius(def))),
                    () -> sb.append("§cNÃO DEFINIDO"));
            sb.append("\n§7Queda: §f").append(ParkourConfiguration.getFallMode(def).name())
                    .append(" (Y<").append(String.format("%.0f", ParkourConfiguration.getFallYLevel(def))).append(")");
            sb.append("\n§7Reset: §f").append(ParkourConfiguration.getResetMode(def).name());
            sb.append("\n§7Checkpoints obrigatórios: §f").append(ParkourConfiguration.isCheckpointsRequired(def) ? "sim" : "não");
            long maxTime = ParkourConfiguration.getMaxTimeSeconds(def);
            sb.append("\n§7Tempo máximo: §f").append(maxTime > 0 ? maxTime + "s" : "ilimitado");
            int maxAttempts = ParkourConfiguration.getMaxAttempts(def);
            sb.append("\n§7Tentativas máximas: §f").append(maxAttempts > 0 ? maxAttempts : "ilimitadas");
            sb.append("\n§7Finalização: §f").append(ParkourConfiguration.getFinishMode(def).name());
            sb.append("\n§7Ranking: §f").append(ParkourConfiguration.getRankingStrategy(def).name());
            sb.append("\n§7Destino ao completar: §f").append(ParkourConfiguration.getCompleteDestination(def).name());
            sb.append("\n§7Checkpoints: §f").append(sessionService.checkpointService().checkpointCount(def.id()));
            return message(ctx, sb.toString());
        } catch (Exception e) {
            return message(ctx, "§c" + e.getMessage());
        }
    }

    private static int validate(CommandContext<CommandSourceStack> ctx) {
        try {
            EventDefinition def = selected(ctx);
            var result = sessionService.validator().validate(def);
            if (result.valid()) {
                return message(ctx, ParkourMessages.VALIDATION_PASSED);
            }
            StringBuilder sb = new StringBuilder(ParkourMessages.VALIDATION_FAILED);
            for (var issue : result.issues()) {
                sb.append("\n §c✘ §7[").append(issue.code()).append("] §f").append(issue.message());
            }
            return message(ctx, sb.toString());
        } catch (Exception e) {
            return message(ctx, "§c" + e.getMessage());
        }
    }

    // === Helpers ===

    private static EventDefinition selected(ServerPlayer player) {
        String eventId = EventoCommand.getEditing(player.getUUID());
        if (eventId == null) throw new IllegalArgumentException(ParkourMessages.SELECIONE_EVENTO);
        EventDefinition def = api.findEvent(eventId).orElse(null);
        if (def == null) throw new IllegalArgumentException(ParkourMessages.EVENTO_NAO_ENCONTRADO);
        if (!def.type().equals("parkour")) throw new IllegalArgumentException(ParkourMessages.NAO_E_PARKOUR);
        return def;
    }

    private static EventDefinition selected(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            return selected(player);
        } catch (Exception e) {
            throw new IllegalArgumentException(ParkourMessages.COMANDO_APENAS_JOGADOR);
        }
    }

    private static boolean admin(CommandSourceStack source) {
        return source.hasPermission(2) || source.getServer().isSingleplayer();
    }

    private static int message(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static SuggestionProvider<CommandSourceStack> ENUM_SUGGESTOR(Enum<?>[] values) {
        return (ctx, builder) -> SharedSuggestionProvider.suggest(
                Arrays.stream(values).map(Enum::name).collect(Collectors.toList()), builder);
    }

    private static final SuggestionProvider<CommandSourceStack> CHECKPOINT_SUGGESTOR = (ctx, builder) -> {
        try {
            EventDefinition def = selected(ctx);
            List<ParkourCheckpoint> cps = sessionService.checkpointService().getCheckpoints(def.id());
            return SharedSuggestionProvider.suggest(
                    cps.stream().map(ParkourCheckpoint::id).collect(Collectors.toList()), builder);
        } catch (Exception e) {
            return builder.buildFuture();
        }
    };

}
