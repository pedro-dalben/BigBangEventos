package com.pedrodalben.bigbangeventos.api;
import com.pedrodalben.bigbangeventos.core.EventEngine; import com.pedrodalben.bigbangeventos.definition.EventDefinition; import com.pedrodalben.bigbangeventos.session.EventSession; import com.pedrodalben.bigbangeventos.domain.DomainEventBus; import com.pedrodalben.bigbangeventos.objective.*; import com.pedrodalben.bigbangeventos.stage.StageService; import com.pedrodalben.bigbangeventos.data.TypedDataService; import com.pedrodalben.bigbangeventos.core.team.TeamService; import com.pedrodalben.bigbangeventos.core.round.RoundService; import com.pedrodalben.bigbangeventos.core.combat.CombatService; import com.pedrodalben.bigbangeventos.core.combat.LifeService; import com.pedrodalben.bigbangeventos.core.combat.EliminationService; import java.util.*;
public final class BigBangEventosApi {
    public static final int API_VERSION = 3;
    private final EventEngine engine;
    public BigBangEventosApi(EventEngine engine){this.engine=engine;}
    public Optional<EventDefinition> findEvent(String id){return engine.definition(id);}
    public Collection<EventDefinition> getEvents(){return engine.definitions();}
    public Optional<EventSession> getActiveSession(String id){return engine.activeSession(id);}
    public Optional<EventSession> getSessionByPlayer(UUID id){return engine.sessionByPlayer(id);}
    public OperationResult createEvent(String id, String type, String server){return engine.create(id, type, server);}
    public OperationResult saveEvent(EventDefinition def){engine.save(def);return OperationResult.ok("Salvo");}
    public OperationResult openRegistration(String id){return engine.open(id,null);}
    public OperationResult closeRegistration(String id){return engine.close(id);}
    public OperationResult startEvent(String id){return engine.start(id);}
    public OperationResult pauseEvent(String id){return engine.pause(id);}
    public OperationResult resumeEvent(String id){return engine.resume(id);}
    public OperationResult finishEvent(String id){return engine.finish(id);}
    public OperationResult cancelEvent(String id,String reason){return engine.cancel(id,reason);}
    public OperationResult joinEvent(UUID player,String event){return engine.join(event,player,player.toString(),false,true);}
    public OperationResult leaveEvent(UUID player){return engine.leave(player,"api");}
    public OperationResult completeParticipant(String event, UUID player, String mode){try{return engine.complete(event,player,com.pedrodalben.bigbangeventos.participant.CompletionMode.valueOf(mode));}catch(IllegalArgumentException e){return OperationResult.fail("invalid_mode","Modo inválido: "+mode);}}
    public ObjectiveService objectiveService(){return engine.objectives();}
    public StageService stageService(){return engine.stages();}
    public ObjectiveTypeRegistry objectiveTypes(){return engine.objectiveTypes();}
    public DomainEventBus events(){return engine.events();}
    public TypedDataService data(){return engine.data();}
    public TeamService teams(){return engine.teams();}
    public RoundService rounds(){return engine.rounds();}
    public CombatService combat(){return engine.combat();}
    public LifeService lifeService(){return engine.lifeService();}
    public EliminationService eliminationService(){return engine.eliminationService();}
}
