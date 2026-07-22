package com.pedrodalben.bigbangeventos.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.pedrodalben.bigbangeventos.BigBangEventos;
import com.pedrodalben.bigbangeventos.api.OperationResult;
import com.pedrodalben.bigbangeventos.definition.*;
import com.pedrodalben.bigbangeventos.snapshot.PlayerSnapshot;
import com.pedrodalben.bigbangeventos.session.SessionState;
import com.pedrodalben.bigbangeventos.trigger.*;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;

import java.util.*;

public final class EventoCommand {
    private static final Map<UUID, String> editing = new HashMap<>();
    private static final Map<UUID, TriggerSelection> binding = new HashMap<>();
    private static final Map<UUID, BlockPos> areaPos1 = new HashMap<>();
    private static final Map<UUID, BlockPos> areaPos2 = new HashMap<>();

    private EventoCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(root("evento"));
        dispatcher.register(Commands.literal("event")
                .redirect(dispatcher.getRoot().getChild("evento")));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> root(String name) {
        var root = Commands.literal(name)
            .then(Commands.literal("create").requires(EventoCommand::admin)
                    .then(Commands.argument("id", StringArgumentType.word())
                            .then(Commands.argument("type", StringArgumentType.word())
                                    .executes(c -> result(c, BigBangEventos.engine().create(
                                            StringArgumentType.getString(c, "id"),
                                            StringArgumentType.getString(c, "type"),
                                            "cobbleverse"))))))
            .then(Commands.literal("edit").requires(EventoCommand::admin)
                    .then(eventId("id").executes(c -> {
                        editing.put(player(c).getUUID(), StringArgumentType.getString(c, "id"));
                        return message(c, "Evento selecionado para edição.");
                    })))
            .then(Commands.literal("list").executes(c -> message(c,
                    BigBangEventos.engine().definitions().stream()
                            .map(EventDefinition::id).sorted()
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("Nenhum evento criado."))))
            .then(Commands.literal("info").then(eventId("id").executes(c -> message(c,
                    BigBangEventos.engine().definition(StringArgumentType.getString(c, "id"))
                            .map(e -> e.id() + " | " + e.type() + " | "
                                    + (e.enabled() ? "habilitado" : "desabilitado"))
                            .orElse("Evento não encontrado.")))))
            .then(control("open", c -> BigBangEventos.engine().open(id(c), player(c).getUUID())))
            .then(control("close", c -> BigBangEventos.engine().close(id(c))))
            .then(control("start", c -> BigBangEventos.engine().start(id(c))))
            .then(control("finish", c -> BigBangEventos.engine().finish(id(c))))
            .then(control("pause", c -> BigBangEventos.engine().pause(id(c))))
            .then(control("resume", c -> BigBangEventos.engine().resume(id(c))))
            .then(control("cancel", c -> BigBangEventos.engine().cancel(id(c), "cancelado por administrador")))
            .then(Commands.literal("validate").then(eventId("id").executes(c -> message(c,
                    BigBangEventos.engine().definition(id(c))
                            .map(e -> BigBangEventos.engine().validator().validate(e).issues().toString())
                            .orElse("Evento não encontrado.")))))
            .then(Commands.literal("status").executes(c -> message(c,
                    BigBangEventos.engine().sessionByPlayer(player(c).getUUID())
                            .map(s -> s.eventId() + ": " + s.state())
                            .orElse("Você não está em evento."))))
            .then(Commands.literal("objectives")
                    .then(Commands.literal("list").executes(c -> message(c, selected(player(c)).objectives().toString())))
                    .then(Commands.literal("info").then(eventId("objective").executes(c -> message(c, selected(player(c)).objective(StringArgumentType.getString(c,"objective")).map(Object::toString).orElse("Objetivo não encontrado.")))))
                    .then(Commands.literal("progress").then(Commands.argument("player",StringArgumentType.word()).executes(c -> progressInfo(c)))))
            .then(Commands.literal("stages")
                    .then(Commands.literal("list").executes(c -> message(c, selected(player(c)).stages().toString())))
                    .then(Commands.literal("info").then(eventId("stage").executes(c -> message(c, selected(player(c)).stage(StringArgumentType.getString(c,"stage")).map(Object::toString).orElse("Etapa não encontrada.")))))
                    .then(Commands.literal("current").executes(c -> BigBangEventos.engine().sessionByPlayer(player(c).getUUID()).map(s -> message(c,s.activeStageId().orElse("Nenhuma etapa ativa."))).orElse(message(c,"Você não está em evento.")))))
            .then(Commands.literal("entrar").then(eventId("id").executes(c -> result(c,
                    BigBangEventos.engine().join(id(c), player(c).getUUID(),
                            player(c).getGameProfile().getName(), false, true)))))
            .then(Commands.literal("sair").executes(c -> result(c,
                    BigBangEventos.engine().leave(player(c).getUUID(), "saída voluntária"))))
            .then(Commands.literal("set").requires(EventoCommand::admin)
                    .then(Commands.argument("location", StringArgumentType.word())
                            .executes(EventoCommand::setLocation)))
            .then(Commands.literal("trigger").requires(EventoCommand::admin)
                    .then(Commands.literal("create")
                            .then(Commands.argument("name", StringArgumentType.word())
                                    .then(Commands.argument("type", StringArgumentType.word())
                                            .executes(c -> {
                                                try {
                                                    EventDefinition e = selected(player(c));
                                                    e.putTrigger(new EventTrigger(
                                                            StringArgumentType.getString(c, "name"),
                                                            TriggerType.valueOf(
                                                                    StringArgumentType.getString(c, "type")
                                                                            .toUpperCase(Locale.ROOT))));
                                                    BigBangEventos.engine().save(e);
                                                    return message(c, "Gatilho criado.");
                                                } catch (IllegalArgumentException ex) {
                                                    return message(c, ex.getMessage());
                                                }
                                            }))))
                    .then(Commands.literal("list").executes(c -> message(c,
                            selected(player(c)).triggers().toString())))
                    .then(Commands.literal("bind")
                            .then(Commands.argument("name", StringArgumentType.word()).executes(c -> {
                                String event = editing.get(player(c).getUUID());
                                String triggerName = StringArgumentType.getString(c, "name");
                                if (event == null || BigBangEventos.engine().definition(event)
                                        .flatMap(e -> e.trigger(triggerName)).isEmpty())
                                    return message(c, "Evento ou gatilho não encontrado.");
                                binding.put(player(c).getUUID(), new TriggerSelection(event, triggerName));
                                return message(c, "Clique em uma placa para vincular o gatilho.");
                            })))
                    .then(Commands.literal("bind-cancel").executes(c -> {
                        UUID pid = player(c).getUUID();
                        if (binding.remove(pid) != null) {
                            return message(c, "Bind cancelado.");
                        }
                        return message(c, "Nenhum bind pendente.");
                    }))
                    .then(Commands.literal("area")
                            .then(Commands.literal("pos1").then(eventId("trigger").executes(c -> setAreaPos(c, true))))
                            .then(Commands.literal("pos2").then(eventId("trigger").executes(c -> setAreaPos(c, false))))
                            .then(Commands.literal("radius").then(eventId("trigger")
                                    .then(Commands.argument("radius", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(0.01))
                                            .executes(c -> setRadius(c, 0))))
                                    .then(Commands.argument("trigger", StringArgumentType.word())
                                            .then(Commands.argument("radius", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(0.01))
                                                    .then(Commands.argument("verticalRadius", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(0.01))
                                                            .executes(c -> setRadius(c, com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(c,"verticalRadius")))))))
                            .then(Commands.literal("info").then(eventId("trigger").executes(c -> areaInfo(c))))
                            .then(Commands.literal("clear").then(eventId("trigger").executes(c -> clearArea(c))))));

        var recovery = Commands.literal("recovery").requires(EventoCommand::admin)
                .then(Commands.literal("status").executes(c -> {
                    var pending = BigBangEventos.engine().snapshots().allPending();
                    if (pending.isEmpty()) return message(c, "Nenhum snapshot pendente.");
                    StringBuilder sb = new StringBuilder("Snapshots pendentes: ");
                    for (PlayerSnapshot s : pending) {
                        sb.append("\n  ").append(s.snapshotId()).append(" player=")
                                .append(s.playerId()).append(" state=").append(s.state());
                    }
                    return message(c, sb.toString());
                }))
                .then(Commands.literal("list").executes(c -> {
                    var pending = BigBangEventos.engine().snapshots().allPending();
                    if (pending.isEmpty()) return message(c, "Nenhum snapshot pendente.");
                    StringBuilder sb = new StringBuilder("Snapshots: ");
                    for (PlayerSnapshot s : pending) {
                        sb.append("\n  ").append(s.snapshotId()).append(" -> ")
                                .append(s.playerId()).append(" (").append(s.state()).append(")");
                    }
                    return message(c, sb.toString());
                }))
                .then(Commands.literal("player").then(Commands.argument("player", StringArgumentType.word())
                        .executes(c -> {
                            String playerName = StringArgumentType.getString(c, "player");
                            var uuidOpt = BigBangEventos.engine().players().findOnlineUuidByName(playerName);
                            if (uuidOpt.isEmpty()) return message(c, "Jogador não encontrado online.");
                            UUID pid = uuidOpt.get();
                            PlayerSnapshot snap = BigBangEventos.engine().snapshots().findPendingForPlayer(pid);
                            if (snap == null) return message(c, "Nenhum snapshot pendente para " + playerName + ".");
                            return message(c, "Snapshot: " + snap.snapshotId()
                                    + " estado: " + snap.state()
                                    + " sessão: " + snap.sessionId()
                                    + " restaurados: " + snap.restoredComponents());
                        })))
                .then(Commands.literal("retry").then(Commands.argument("player", StringArgumentType.word())
                        .executes(c -> {
                            String playerName = StringArgumentType.getString(c, "player");
                            var uuidOpt = BigBangEventos.engine().players().findOnlineUuidByName(playerName);
                            if (uuidOpt.isEmpty()) return message(c, "Jogador não encontrado online.");
                            UUID pid = uuidOpt.get();
                            OperationResult r = BigBangEventos.engine().restore().restorePending(pid);
                            return result(c, r);
                        })));
        root = root.then(recovery);

        var debug = Commands.literal("debug").requires(EventoCommand::admin)
                .then(Commands.literal("data").then(Commands.argument("player",StringArgumentType.word()).executes(c -> progressInfo(c))))
                .then(Commands.literal("player").then(Commands.argument("player", StringArgumentType.word())
                        .executes(c -> {
                            String playerName = StringArgumentType.getString(c, "player");
                            var uuidOpt = BigBangEventos.engine().players().findOnlineUuidByName(playerName);
                            if (uuidOpt.isEmpty()) return message(c, "Jogador não encontrado online.");
                            UUID pid = uuidOpt.get();
                            boolean online = BigBangEventos.engine().players().isOnline(pid);
                            var session = BigBangEventos.engine().sessionByPlayer(pid);
                            PlayerSnapshot snap = BigBangEventos.engine().snapshots().findPendingForPlayer(pid);
                            StringBuilder sb = new StringBuilder(playerName + ":").append("\n  UUID: ").append(pid)
                                    .append("\n  Online: ").append(online);
                            session.ifPresent(s -> {
                                sb.append("\n  Evento: ").append(s.eventId())
                                        .append("\n  Sessão: ").append(s.state());
                                s.participant(pid).ifPresent(p ->
                                        sb.append("\n  Estado: ").append(p.state()));
                            });
                            if (snap != null) {
                                sb.append("\n  Snapshot: ").append(snap.snapshotId())
                                        .append(" (").append(snap.state()).append(")")
                                        .append("\n  Localização: ").append(snap.originalLocation().dimension())
                                        .append(" ").append((int) snap.originalLocation().x())
                                        .append(" ").append((int) snap.originalLocation().y())
                                        .append(" ").append((int) snap.originalLocation().z());
                            }
                            return message(c, sb.toString());
                        })));
        root = root.then(debug);

        return root;
    }

    private static String id(CommandContext<CommandSourceStack> c) {
        return StringArgumentType.getString(c, "id");
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String>
    eventId(String name) {
        return Commands.argument(name, StringArgumentType.word());
    }

    private interface Op { OperationResult run(CommandContext<CommandSourceStack> c); }

    private static LiteralArgumentBuilder<CommandSourceStack> control(String command, Op op) {
        return Commands.literal(command).requires(EventoCommand::admin)
                .then(eventId("id").executes(c -> result(c, op.run(c))));
    }

    private static int setLocation(CommandContext<CommandSourceStack> c) {
        try {
            ServerPlayer p = player(c);
            EventDefinition e = selected(p);
            LocationName n = LocationName.valueOf(
                    StringArgumentType.getString(c, "location").toUpperCase(Locale.ROOT));
            var pos = p.position();
            e.location(n, new EventLocation(e.serverId(),
                    p.level().dimension().location().toString(),
                    pos.x, pos.y, pos.z, p.getYRot(), p.getXRot()));
            BigBangEventos.engine().save(e);
            return message(c, "Localização " + n + " definida.");
        } catch (IllegalArgumentException ex) {
            return message(c, ex.getMessage());
        }
    }
    private static int progressInfo(CommandContext<CommandSourceStack> c){String name=StringArgumentType.getString(c,"player");var id=BigBangEventos.engine().players().findOnlineUuidByName(name);if(id.isEmpty())return message(c,"Jogador não encontrado online.");var s=BigBangEventos.engine().sessionByPlayer(id.get());if(s.isEmpty())return message(c,"Jogador não está em evento.");var p=s.get().participant(id.get()).orElse(null);if(p==null)return message(c,"Participante não encontrado.");return message(c,"Objetivos="+BigBangEventos.engine().objectives().listProgress(BigBangEventos.engine().definition(s.get().eventId()).orElseThrow(),s.get(),id.get())+" data="+p.typedData().keys());}

    private static int setAreaPos(CommandContext<CommandSourceStack> c, boolean first) {
        try { ServerPlayer p=player(c); String trigger=StringArgumentType.getString(c,"trigger"); selected(p).trigger(trigger).orElseThrow(()->new IllegalArgumentException("Gatilho não encontrado.")); (first?areaPos1:areaPos2).put(p.getUUID(),p.blockPosition()); return message(c,(first?"Posição 1":"Posição 2")+" definida para "+trigger+"."); }
        catch(IllegalArgumentException e){return message(c,e.getMessage());}
    }
    private static int setRadius(CommandContext<CommandSourceStack> c, double vertical) {
        try { ServerPlayer p=player(c); EventDefinition e=selected(p); String id=StringArgumentType.getString(c,"trigger"); var t=e.trigger(id).orElseThrow(()->new IllegalArgumentException("Gatilho não encontrado.")); if(t.type()!=TriggerType.REGION_ENTER&&t.type()!=TriggerType.REGION_EXIT)throw new IllegalArgumentException("Gatilho incompatível com área."); double r=com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(c,"radius"); double vr=vertical==0?r:vertical; t.area(new EventArea.Radius(e.serverId(),p.level().dimension().location().toString(),p.getX(),p.getY(),p.getZ(),r,vr)); BigBangEventos.engine().save(e); return message(c,"Área radius definida."); }
        catch(IllegalArgumentException ex){return message(c,ex.getMessage());}
    }
    private static int areaInfo(CommandContext<CommandSourceStack> c){try{var t=selected(player(c)).trigger(StringArgumentType.getString(c,"trigger")).orElseThrow(()->new IllegalArgumentException("Gatilho não encontrado."));return message(c,t.area().map(Object::toString).orElse("Sem área."));}catch(IllegalArgumentException e){return message(c,e.getMessage());}}
    private static int clearArea(CommandContext<CommandSourceStack> c){try{EventDefinition e=selected(player(c));e.trigger(StringArgumentType.getString(c,"trigger")).ifPresent(t->t.area(null));BigBangEventos.engine().save(e);return message(c,"Área removida.");}catch(IllegalArgumentException ex){return message(c,ex.getMessage());}}

    public static String getEditing(UUID playerId) {
        return editing.get(playerId);
    }

    public static boolean bindSelected(UUID player, String value) {
        TriggerSelection selection = binding.remove(player);
        if (selection == null) return false;
        EventDefinition e = BigBangEventos.engine().definition(selection.eventId()).orElse(null);
        if (e == null) return false;
        e.trigger(selection.triggerId()).ifPresent(t -> t.binding(value));
        BigBangEventos.engine().save(e);
        return true;
    }

    private static EventDefinition selected(ServerPlayer p) {
        String id = editing.get(p.getUUID());
        if (id == null) throw new IllegalArgumentException("Use /evento edit <id> antes.");
        return BigBangEventos.engine().definition(id)
                .orElseThrow(() -> new IllegalArgumentException("Evento não encontrado."));
    }

    private record TriggerSelection(String eventId, String triggerId) {}

    private static boolean admin(CommandSourceStack source) {
        return source.hasPermission(2) || source.getServer().isSingleplayer();
    }

    private static ServerPlayer player(CommandContext<CommandSourceStack> c) {
        try { return c.getSource().getPlayerOrException(); }
        catch (Exception e) { throw new IllegalArgumentException("Este comando exige jogador"); }
    }

    private static int result(CommandContext<CommandSourceStack> c, OperationResult r) {
        return message(c, r.message());
    }

    private static int message(CommandContext<CommandSourceStack> c, String text) {
        c.getSource().sendSuccess(() -> Component.literal("[Eventos] " + text), false);
        return 1;
    }
}
