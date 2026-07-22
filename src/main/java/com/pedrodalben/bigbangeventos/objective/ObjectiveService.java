package com.pedrodalben.bigbangeventos.objective;

import com.pedrodalben.bigbangeventos.api.OperationResult;
import com.pedrodalben.bigbangeventos.domain.DomainEventBus;
import com.pedrodalben.bigbangeventos.domain.ObjectiveEvents;
import com.pedrodalben.bigbangeventos.definition.EventDefinition;
import com.pedrodalben.bigbangeventos.session.*;
import com.pedrodalben.bigbangeventos.stage.StageService;
import java.time.Clock;
import java.util.*;

public final class ObjectiveService {
    private final Clock clock; private final ObjectiveTypeRegistry types; private final DomainEventBus events;
    private StageService stages;
    public ObjectiveService(Clock clock, ObjectiveTypeRegistry types, DomainEventBus events){this.clock=clock;this.types=types;this.events=events;}
    public void stages(StageService value){stages=value;}
    public synchronized ObjectiveResult activate(EventDefinition def, EventSession session, String id, UUID player, String source){
        ObjectiveDefinition od = def.objective(id).orElse(null); if(od==null)return fail("objective_not_found","Objetivo não encontrado",null);
        if(od.scope()==ObjectiveScope.TEAM)return fail("unsupported_scope","Escopo TEAM ainda não suportado",null);
        ObjectiveProgress p = progress(session,od,player); if(p==null){p=create(session,od,player);}
        if(p.status()==ObjectiveStatus.COMPLETED)return ObjectiveResult.ok("Objetivo já concluído",p);
        if(p.status()==ObjectiveStatus.FAILED||p.status()==ObjectiveStatus.SKIPPED)return fail("terminal_state","Objetivo está em estado terminal",p);
        p.start(clock.instant(),source); events.publish(new ObjectiveEvents.ObjectiveActivated(def.id(),session.id(),p)); return ObjectiveResult.ok("Objetivo ativado",p);
    }
    public synchronized ObjectiveResult addProgress(EventDefinition def, EventSession session, String id, UUID player, long amount, String source){
        if(amount<0)return fail("invalid_progress","Progresso negativo",null);
        ObjectiveResult active=activate(def,session,id,player,source); if(!active.success())return active;
        ObjectiveProgress p=active.progress(); if(p.status()==ObjectiveStatus.COMPLETED)return active;
        p.progress(p.current()+amount,clock.instant(),source); events.publish(new ObjectiveEvents.ObjectiveProgressChanged(def.id(),session.id(),p));
        if(p.current()>=p.target()) return complete(def,session,id,player,source);
        return ObjectiveResult.ok("Progresso atualizado",p);
    }
    public synchronized ObjectiveResult setProgress(EventDefinition def, EventSession session, String id, UUID player, long value, String source){
        if(value<0)return fail("invalid_progress","Progresso inválido",null);
        ObjectiveResult active=activate(def,session,id,player,source); if(!active.success())return active;
        ObjectiveProgress p=active.progress(); if(p.status()==ObjectiveStatus.COMPLETED)return active;
        p.progress(value,clock.instant(),source); events.publish(new ObjectiveEvents.ObjectiveProgressChanged(def.id(),session.id(),p));
        return p.current()>=p.target()?complete(def,session,id,player,source):ObjectiveResult.ok("Progresso atualizado",p);
    }
    public synchronized ObjectiveResult complete(EventDefinition def, EventSession session, String id, UUID player, String source){
        ObjectiveDefinition od=def.objective(id).orElse(null); if(od==null)return fail("objective_not_found","Objetivo não encontrado",null);
        if(od.scope()==ObjectiveScope.TEAM)return fail("unsupported_scope","Escopo TEAM ainda não suportado",null);
        ObjectiveProgress p=progress(session,od,player); if(p==null)p=create(session,od,player);
        if(p.status()==ObjectiveStatus.COMPLETED)return ObjectiveResult.ok("Objetivo já concluído",p);
        if(p.status()==ObjectiveStatus.FAILED||p.status()==ObjectiveStatus.SKIPPED)return fail("terminal_state","Objetivo está em estado terminal",p);
        p.complete(clock.instant(),source); events.publish(new ObjectiveEvents.ObjectiveCompleted(def.id(),session.id(),p));
        if(stages!=null)stages.objectiveChanged(def,session,od.stageId()); return ObjectiveResult.ok("Objetivo concluído",p);
    }
    public synchronized ObjectiveResult fail(EventDefinition def, EventSession session, String id, UUID player, String source){return terminal(def,session,id,player,ObjectiveStatus.FAILED,source);}
    public synchronized ObjectiveResult skip(EventDefinition def, EventSession session, String id, UUID player, String source){return terminal(def,session,id,player,ObjectiveStatus.SKIPPED,source);}
    private ObjectiveResult terminal(EventDefinition def,EventSession session,String id,UUID player,ObjectiveStatus status,String source){ObjectiveDefinition od=def.objective(id).orElse(null);if(od==null)return fail("objective_not_found","Objetivo não encontrado",null);ObjectiveProgress p=progress(session,od,player);if(p==null)p=create(session,od,player);if(p.status()==ObjectiveStatus.COMPLETED)return ObjectiveResult.ok("Objetivo já concluído",p);if(p.status()==status)return ObjectiveResult.ok("Estado já aplicado",p);if(status==ObjectiveStatus.FAILED)p.fail(clock.instant(),source);else p.skip(clock.instant(),source);events.publish(status==ObjectiveStatus.FAILED?new ObjectiveEvents.ObjectiveFailed(def.id(),session.id(),p):new ObjectiveEvents.ObjectiveProgressChanged(def.id(),session.id(),p));return ObjectiveResult.ok("Estado do objetivo atualizado",p);}
    public synchronized Optional<ObjectiveProgress> getProgress(EventSession s, String id, UUID player){return s.objectiveProgress().values().stream().filter(p->p.objectiveId().equals(id)&&(p.scope()==ObjectiveScope.SESSION||Objects.equals(p.playerId(),player))).findFirst();}
    public synchronized List<ObjectiveProgress> listProgress(EventDefinition def, EventSession s, UUID player){return def.objectives().stream().map(o->progress(s,o,player)).filter(Objects::nonNull).toList();}
    public synchronized boolean areRequiredObjectivesCompleted(EventDefinition def, EventSession s, String stageId, UUID player){return def.objectives().stream().filter(o->o.stageId().equals(stageId)&&o.required()&&o.enabled()).allMatch(o->{ObjectiveProgress p=progress(s,o,player);return p!=null&&p.status()==ObjectiveStatus.COMPLETED;});}
    private ObjectiveProgress progress(EventSession s,ObjectiveDefinition o,UUID player){return s.objectiveProgress().get(key(o,player));}
    private ObjectiveProgress create(EventSession s,ObjectiveDefinition o,UUID player){if(o.scope()==ObjectiveScope.PARTICIPANT&&player==null)throw new IllegalArgumentException("Jogador obrigatório");var p=new ObjectiveProgress(o.id(),o.scope(),o.scope()==ObjectiveScope.PARTICIPANT?player:null,o.target()==0?1:o.target(),types.find(o.typeId()).map(t->t.initialProgress(o)).orElse(0L),ObjectiveStatus.AVAILABLE,clock.instant());p.metadata(o.metadata());s.objectiveProgress().put(key(o,player),p);return p;}
    private static String key(ObjectiveDefinition o,UUID player){return o.scope()+":"+o.id()+":"+(o.scope()==ObjectiveScope.PARTICIPANT?player:"session");}
    private static ObjectiveResult fail(String c,String m,ObjectiveProgress p){return ObjectiveResult.fail(c,m,p);}
}
