package com.pedrodalben.bigbangeventos.participant;

import java.time.Instant;
import java.util.*;
import com.pedrodalben.bigbangeventos.data.DataContainer;
import com.pedrodalben.bigbangeventos.data.InMemoryDataContainer;

public final class EventParticipant {
    private final UUID playerId; private final String knownName; private final Instant joinedAt;
    private ParticipantState state = ParticipantState.REGISTERED; private Instant startedAt, finishedAt, disconnectedAt;
    private int score, position, checkpoint, stage, attempts, deaths, falls; private String leaveReason;
    private final Map<String, String> data = new HashMap<>();
    private final InMemoryDataContainer typedData = new InMemoryDataContainer();
    private UUID snapshotId;

    public EventParticipant(UUID playerId, String knownName, Instant joinedAt) { this.playerId=playerId; this.knownName=knownName; this.joinedAt=joinedAt; }
    public UUID playerId(){return playerId;} public String knownName(){return knownName;} public Instant joinedAt(){return joinedAt;} public ParticipantState state(){return state;} public int score(){return score;} public int position(){return position;} public int checkpoint(){return checkpoint;} public int stage(){return stage;} public Instant startedAt(){return startedAt;} public Optional<Instant> finishedAt(){return Optional.ofNullable(finishedAt);} public Map<String,String> data(){return Map.copyOf(data);}
    public Optional<UUID> snapshotId() { return Optional.ofNullable(snapshotId); }
    public void snapshotId(UUID id) { this.snapshotId = id; }
    public void state(ParticipantState value){state=value;} public void start(Instant now){startedAt=now; state=ParticipantState.ACTIVE;} public boolean finish(Instant now){if(state!=ParticipantState.ACTIVE) return false; finishedAt=now; state=ParticipantState.FINISHED; return true;} public void score(int delta){score+=delta;} public void restoreScore(int value){score=value;} public void position(int value){position=value;} public void checkpoint(int value){checkpoint=value;} public void stage(int value){stage=value;} public void disconnected(Instant at){disconnectedAt=at;state=ParticipantState.DISCONNECTED;} public void leave(String reason){leaveReason=reason;state=ParticipantState.LEFT;} public Optional<String> leaveReason(){return Optional.ofNullable(leaveReason);} public void data(String key,String value){data.put(key,value);}
    public DataContainer typedData(){return typedData;}
    public InMemoryDataContainer rawTypedData(){return typedData;}
    public Optional<String> dataValue(String key){return Optional.ofNullable(data.get(key));}
    public void removeData(String key){data.remove(key);}
}
