package br.com.bigbangcraft.eventos.treasurehunt;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.pedrodalben.bigbangeventos.BigBangEventos;
import com.pedrodalben.bigbangeventos.api.*;
import com.pedrodalben.bigbangeventos.definition.EventDefinition;
import com.pedrodalben.bigbangeventos.objective.*;
import com.pedrodalben.bigbangeventos.stage.EventStageDefinition;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

public final class TreasureHuntCommandRegistrar {
    private TreasureHuntCommandRegistrar(){}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,TreasureHuntSessionService service){
        var root=dispatcher.getRoot().getChild("evento"); if(root==null)return;
        root.addChild(node(service).build());
    }
    private static LiteralArgumentBuilder<CommandSourceStack> node(TreasureHuntSessionService service){
        return Commands.literal("treasure")
                .then(Commands.literal("clue").executes(c->msg(c,service.clue(player(c).getUUID()).message())))
                .then(Commands.literal("progress").executes(c->{var p=player(c);var s=BigBangEventos.api().getSessionByPlayer(p.getUUID()).orElse(null);return msg(c,s==null?"Você não está em evento.":service.progress(s,p.getUUID()));}))
                .then(Commands.literal("score").executes(c->{var p=player(c);var s=BigBangEventos.api().getSessionByPlayer(p.getUUID()).orElse(null);return msg(c,s==null?"Você não está em evento.":service.progress(s,p.getUUID()));}))
                .then(Commands.literal("validate").requires(TreasureHuntCommandRegistrar::admin).then(Commands.argument("event",StringArgumentType.word()).executes(c->{var d=BigBangEventos.api().findEvent(StringArgumentType.getString(c,"event")).orElse(null);return msg(c,d==null?"Evento não encontrado":BigBangEventos.api().getEvents().stream().noneMatch(x->x.id().equals(d.id()))?"":new TreasureHuntValidator(BigBangEventos.engine().objectiveTypes()).validate(d).issues().toString());})))
                .then(Commands.literal("stage").requires(TreasureHuntCommandRegistrar::admin)
                        .then(Commands.literal("add").then(Commands.argument("event",StringArgumentType.word()).then(Commands.argument("id",StringArgumentType.word()).executes(c->addStage(c)))))
                        .then(Commands.literal("list").then(Commands.argument("event",StringArgumentType.word()).executes(c->{var d=event(c);return msg(c,d==null?"Evento não encontrado":d.stages().toString());}))))
                .then(Commands.literal("objective").requires(TreasureHuntCommandRegistrar::admin)
                        .then(Commands.literal("add").then(Commands.argument("event",StringArgumentType.word()).then(Commands.argument("stage",StringArgumentType.word()).then(Commands.argument("id",StringArgumentType.word()).then(Commands.argument("type",StringArgumentType.word()).executes(c->addObjective(c)))))))
                        .then(Commands.literal("list").then(Commands.argument("event",StringArgumentType.word()).executes(c->{var d=event(c);return msg(c,d==null?"Evento não encontrado":d.objectives().toString());}))));
    }
    private static int addStage(CommandContext<CommandSourceStack> c){try{EventDefinition d=event(c);if(d==null)return msg(c,"Evento não encontrado");String id=StringArgumentType.getString(c,"id");d.putStage(new EventStageDefinition(id,id,"",d.stages().size()+1,true,true,0,List.of(),null,true,Map.of()));BigBangEventos.engine().save(d);return msg(c,"Etapa criada.");}catch(IllegalArgumentException e){return msg(c,e.getMessage());}}
    private static int addObjective(CommandContext<CommandSourceStack> c){try{EventDefinition d=event(c);if(d==null)return msg(c,"Evento não encontrado");String id=StringArgumentType.getString(c,"id");String stage=StringArgumentType.getString(c,"stage");d.putObjective(new ObjectiveDefinition(id,id,"",StringArgumentType.getString(c,"type"),stage,true, d.objectives().size()+1,1,ObjectiveScope.PARTICIPANT,true,Map.of()));BigBangEventos.engine().save(d);return msg(c,"Objetivo criado.");}catch(IllegalArgumentException e){return msg(c,e.getMessage());}}
    private static EventDefinition event(CommandContext<CommandSourceStack> c){return BigBangEventos.api().findEvent(StringArgumentType.getString(c,"event")).orElse(null);}
    private static ServerPlayer player(CommandContext<CommandSourceStack> c){try{return c.getSource().getPlayerOrException();}catch(Exception e){throw new IllegalArgumentException("Este comando exige jogador");}}
    private static boolean admin(CommandSourceStack s){return s.hasPermission(2)||s.getServer().isSingleplayer();}
    private static int msg(CommandContext<CommandSourceStack> c,String text){c.getSource().sendSuccess(()-> Component.literal("[Eventos] "+text),false);return 1;}
}
