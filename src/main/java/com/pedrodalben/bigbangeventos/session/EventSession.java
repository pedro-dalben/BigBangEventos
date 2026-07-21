package com.pedrodalben.bigbangeventos.session;

import com.pedrodalben.bigbangeventos.participant.EventParticipant;
import java.time.Instant;
import java.util.*;

public final class EventSession {
    private final UUID id; private final String eventId; private final int configurationVersion; private final Instant createdAt;
    private SessionState state = SessionState.CREATED; private Instant openedAt, startedAt, endedAt; private UUID administrator; private String cancelReason;
    private final Map<UUID, EventParticipant> participants = new LinkedHashMap<>(); private final Set<UUID> spectators = new HashSet<>();
    public EventSession(UUID id, String eventId, int configurationVersion, Instant createdAt, UUID administrator) { this.id=id;this.eventId=eventId;this.configurationVersion=configurationVersion;this.createdAt=createdAt;this.administrator=administrator; }
    public UUID id(){return id;} public String eventId(){return eventId;} public int configurationVersion(){return configurationVersion;} public Instant createdAt(){return createdAt;} public SessionState state(){return state;} public Optional<Instant> openedAt(){return Optional.ofNullable(openedAt);} public Optional<Instant> startedAt(){return Optional.ofNullable(startedAt);} public Optional<Instant> endedAt(){return Optional.ofNullable(endedAt);} public Optional<String> cancelReason(){return Optional.ofNullable(cancelReason);}
    public Collection<EventParticipant> participants(){return List.copyOf(participants.values());} public Optional<EventParticipant> participant(UUID id){return Optional.ofNullable(participants.get(id));} public boolean hasParticipant(UUID id){return participants.containsKey(id);} public int participantCount(){return participants.size();}
    public void addParticipant(EventParticipant participant){participants.put(participant.playerId(),participant);} public void removeParticipant(UUID id){participants.remove(id);} public void state(SessionState value, Instant at){state=value; if(value==SessionState.REGISTRATION_OPEN) openedAt=at; if(value==SessionState.RUNNING) startedAt=at; if(value==SessionState.FINISHED||value==SessionState.CANCELLED||value==SessionState.FAILED) endedAt=at;} public void cancelReason(String value){cancelReason=value;}
}
