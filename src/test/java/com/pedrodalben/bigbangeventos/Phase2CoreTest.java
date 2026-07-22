package com.pedrodalben.bigbangeventos;

import com.pedrodalben.bigbangeventos.data.*;
import com.pedrodalben.bigbangeventos.domain.*;
import com.pedrodalben.bigbangeventos.definition.*;
import com.pedrodalben.bigbangeventos.objective.*;
import com.pedrodalben.bigbangeventos.platform.PlatformScheduler;
import com.pedrodalben.bigbangeventos.participant.EventParticipant;
import com.pedrodalben.bigbangeventos.session.*;
import com.pedrodalben.bigbangeventos.stage.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.slf4j.LoggerFactory;

class Phase2CoreTest {
    @Test void objectiveProgressIsCappedAndCompletionIsIdempotent() {
        var clock=Clock.fixed(Instant.EPOCH,ZoneOffset.UTC); var bus=new DomainEventBus(new InlineScheduler(),LoggerFactory.getLogger("test")); var types=new ObjectiveTypeRegistry(); var objectives=new ObjectiveService(clock,types,bus);
        var def=new EventDefinition("hunt","generic","server"); def.putStage(new EventStageDefinition("s1","S1","",1,true,true,0,List.of("o1"),null,true,Map.of())); def.putObjective(new ObjectiveDefinition("o1","O1","","counter","s1",true,1,3,ObjectiveScope.PARTICIPANT,true,Map.of()));
        var session=new EventSession(UUID.randomUUID(),"hunt",1,Instant.EPOCH,null); session.state(SessionState.RUNNING,Instant.EPOCH); var player=UUID.randomUUID(); session.addParticipant(new EventParticipant(player,"p",Instant.EPOCH));
        var completed=new int[1]; bus.subscribe("test",ObjectiveEvents.ObjectiveCompleted.class,e->completed[0]++);
        assertTrue(objectives.addProgress(def,session,"o1",player,5,"test").success()); assertEquals(3,objectives.getProgress(session,"o1",player).orElseThrow().current()); assertEquals(ObjectiveStatus.COMPLETED,objectives.getProgress(session,"o1",player).orElseThrow().status()); assertEquals(1,completed[0]);
        assertTrue(objectives.complete(def,session,"o1",player,"again").success()); assertEquals(1,completed[0]);
    }
    @Test void typedDataRoundTripsAndPreservesUnknownValues() {
        var data=new InMemoryDataContainer(); var score=new DataKey<>("test","score",DataCodecs.LONG,0L); data.set(score,3L); assertEquals(3L,data.getOrDefault(score)); data.loadRaw("future:value","future_type",Map.of("x",1)); assertTrue(data.keys().contains("future:value")); data.remove(score); assertFalse(data.contains(score));
    }
    @Test void eventBusKeepsDispatchingAfterListenerFailureAndSupportsUnsubscribe() {
        var bus=new DomainEventBus(new InlineScheduler(),LoggerFactory.getLogger("test"));var seen=new int[1];var sub=bus.subscribe("one",ObjectiveEvents.ParticipantJoined.class,e->{throw new IllegalStateException("boom");});bus.subscribe("two",ObjectiveEvents.ParticipantJoined.class,e->seen[0]++);bus.publish(new ObjectiveEvents.ParticipantJoined("e",UUID.randomUUID(),UUID.randomUUID()));assertEquals(1,seen[0]);sub.close();bus.publish(new ObjectiveEvents.ParticipantJoined("e",UUID.randomUUID(),UUID.randomUUID()));assertEquals(2,seen[0]);
    }
    @Test void sessionScopeIsSharedBetweenParticipants() {
        var clock=Clock.fixed(Instant.EPOCH,ZoneOffset.UTC);var bus=new DomainEventBus(new InlineScheduler(),LoggerFactory.getLogger("test"));var objectives=new ObjectiveService(clock,new ObjectiveTypeRegistry(),bus);var d=new EventDefinition("shared","generic","server");d.putStage(new EventStageDefinition("s","S","",1,true,true,0,List.of("o"),null,false,Map.of()));d.putObjective(new ObjectiveDefinition("o","O","","counter","s",true,1,2,ObjectiveScope.SESSION,true,Map.of()));var s=new EventSession(UUID.randomUUID(),"shared",1,Instant.EPOCH,null);s.state(SessionState.RUNNING,Instant.EPOCH);assertTrue(objectives.addProgress(d,s,"o",UUID.randomUUID(),1,"test").success());assertEquals(1,objectives.getProgress(s,"o",UUID.randomUUID()).orElseThrow().current());
    }
    @Test void stageAutoCompletesAfterAllParticipantsFinishRequiredObjectives() {
        var clock=Clock.fixed(Instant.EPOCH,ZoneOffset.UTC); var bus=new DomainEventBus(new InlineScheduler(),LoggerFactory.getLogger("test")); var objectives=new ObjectiveService(clock,new ObjectiveTypeRegistry(),bus); var stages=new StageService(clock,objectives,bus); objectives.stages(stages);
        var def=new EventDefinition("hunt2","generic","server"); def.putStage(new EventStageDefinition("s1","S1","",1,true,true,0,List.of("o1"),null,true,Map.of())); def.putObjective(new ObjectiveDefinition("o1","O1","","manual","s1",true,1,1,ObjectiveScope.PARTICIPANT,true,Map.of())); var s=new EventSession(UUID.randomUUID(),"hunt2",1,Instant.EPOCH,null);s.state(SessionState.RUNNING,Instant.EPOCH);UUID p=UUID.randomUUID();s.addParticipant(new EventParticipant(p,"p",Instant.EPOCH));assertTrue(stages.activateFirst(def,s).success());assertTrue(objectives.complete(def,s,"o1",p,"test").success());assertEquals(StageStatus.COMPLETED,s.stageProgress().get("s1").status());
    }
    private static final class InlineScheduler implements PlatformScheduler {public boolean isServerThread(){return true;}public void executeOnServerThread(Runnable r){r.run();}public ScheduledHandle schedule(Duration d,Runnable r){return handle();}public ScheduledHandle scheduleRepeating(Duration d,Runnable r){return handle();}private ScheduledHandle handle(){return new ScheduledHandle(){public void cancel(){}public boolean isCancelled(){return false;}};}}
}
