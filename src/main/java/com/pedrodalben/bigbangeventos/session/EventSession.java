package com.pedrodalben.bigbangeventos.session;

import com.pedrodalben.bigbangeventos.participant.EventParticipant;
import com.pedrodalben.bigbangeventos.data.DataContainer;
import com.pedrodalben.bigbangeventos.data.InMemoryDataContainer;
import com.pedrodalben.bigbangeventos.objective.ObjectiveProgress;
import com.pedrodalben.bigbangeventos.stage.SessionStageProgress;
import java.time.Instant;
import java.util.*;
import com.pedrodalben.bigbangeventos.session.team.SessionTeam;

public final class EventSession {
    private final UUID id; private final String eventId; private final int configurationVersion; private final Instant createdAt;
    private SessionState state = SessionState.CREATED; private Instant openedAt, startedAt, endedAt; private UUID administrator; private String cancelReason;
    private final Map<UUID, EventParticipant> participants = new LinkedHashMap<>(); private final Set<UUID> spectators = new HashSet<>();
    private final Map<String, ObjectiveProgress> objectiveProgress = new LinkedHashMap<>();
    private final Map<String, SessionStageProgress> stageProgress = new LinkedHashMap<>();
    private final InMemoryDataContainer data = new InMemoryDataContainer();
    private final Map<String, SessionTeam> teamsByDef = new LinkedHashMap<>();
    private final Map<UUID, SessionTeam> teamsById = new LinkedHashMap<>();
    public EventSession(UUID id, String eventId, int configurationVersion, Instant createdAt, UUID administrator) { this.id=id;this.eventId=eventId;this.configurationVersion=configurationVersion;this.createdAt=createdAt;this.administrator=administrator; }
    public UUID id(){return id;} public String eventId(){return eventId;} public int configurationVersion(){return configurationVersion;} public Instant createdAt(){return createdAt;} public SessionState state(){return state;} public Optional<Instant> openedAt(){return Optional.ofNullable(openedAt);} public Optional<Instant> startedAt(){return Optional.ofNullable(startedAt);} public Optional<Instant> endedAt(){return Optional.ofNullable(endedAt);} public Optional<String> cancelReason(){return Optional.ofNullable(cancelReason);}
    public Collection<EventParticipant> participants(){return List.copyOf(participants.values());} public Optional<EventParticipant> participant(UUID id){return Optional.ofNullable(participants.get(id));} public boolean hasParticipant(UUID id){return participants.containsKey(id);} public int participantCount(){return participants.size();}
    public void addParticipant(EventParticipant participant){participants.put(participant.playerId(),participant);} public void removeParticipant(UUID id){participants.remove(id);} public void state(SessionState value, Instant at){state=value; if(value==SessionState.REGISTRATION_OPEN) openedAt=at; if(value==SessionState.RUNNING) startedAt=at; if(value==SessionState.FINISHED||value==SessionState.CANCELLED||value==SessionState.FAILED) endedAt=at;} public void cancelReason(String value){cancelReason=value;} public void restoreState(SessionState value, Instant at){state=value;if(value==SessionState.FINISHED||value==SessionState.CANCELLED||value==SessionState.FAILED)endedAt=at;}
    public Map<String, ObjectiveProgress> objectiveProgress(){return objectiveProgress;}
    public Map<String, SessionStageProgress> stageProgress(){return stageProgress;}
    public DataContainer data(){return data;}
    public InMemoryDataContainer rawData(){return data;}
    public Optional<String> activeStageId(){return stageProgress.values().stream().filter(p -> p.status()==com.pedrodalben.bigbangeventos.stage.StageStatus.ACTIVE).map(SessionStageProgress::stageId).findFirst();}
    public Map<String, SessionTeam> teams() { return Collections.unmodifiableMap(teamsByDef); }
    public Optional<SessionTeam> team(String teamDefId) { return Optional.ofNullable(teamsByDef.get(teamDefId)); }
    public Optional<SessionTeam> teamById(UUID teamId) { return Optional.ofNullable(teamsById.get(teamId)); }
    public void addTeam(SessionTeam team) { teamsByDef.put(team.teamDefinitionId(), team); teamsById.put(team.teamId(), team); }
    public void removeTeam(UUID teamId) { SessionTeam t = teamsById.remove(teamId); if (t != null) teamsByDef.remove(t.teamDefinitionId()); }
    public boolean hasSpectator(UUID player) { return spectators.contains(player); }
    public void addSpectator(UUID player) { spectators.add(player); }
    public void removeSpectator(UUID player) { spectators.remove(player); }
    public Set<UUID> spectators() { return Collections.unmodifiableSet(spectators); }
}
