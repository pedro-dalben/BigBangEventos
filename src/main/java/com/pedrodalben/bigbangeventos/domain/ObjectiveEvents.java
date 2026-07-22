package com.pedrodalben.bigbangeventos.domain;

import com.pedrodalben.bigbangeventos.objective.ObjectiveProgress;
import com.pedrodalben.bigbangeventos.stage.SessionStageProgress;
import java.util.UUID;

public final class ObjectiveEvents {
    private ObjectiveEvents() {}
    public record ObjectiveActivated(String eventId, UUID sessionId, ObjectiveProgress progress) implements DomainEvent {}
    public record ObjectiveProgressChanged(String eventId, UUID sessionId, ObjectiveProgress progress) implements DomainEvent {}
    public record ObjectiveCompleted(String eventId, UUID sessionId, ObjectiveProgress progress) implements DomainEvent {}
    public record ObjectiveFailed(String eventId, UUID sessionId, ObjectiveProgress progress) implements DomainEvent {}
    public record StageActivated(String eventId, UUID sessionId, SessionStageProgress progress) implements DomainEvent {}
    public record StageCompleted(String eventId, UUID sessionId, SessionStageProgress progress) implements DomainEvent {}
    public record StageFailed(String eventId, UUID sessionId, SessionStageProgress progress) implements DomainEvent {}
    public record ParticipantCompleted(String eventId, UUID sessionId, UUID playerId) implements DomainEvent {}
    public record ParticipantJoined(String eventId, UUID sessionId, UUID playerId) implements DomainEvent {}
    public record ParticipantLeft(String eventId, UUID sessionId, UUID playerId) implements DomainEvent {}
    public record SessionFinished(String eventId, UUID sessionId) implements DomainEvent {}
}
