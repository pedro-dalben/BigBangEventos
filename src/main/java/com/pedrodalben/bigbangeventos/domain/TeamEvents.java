package com.pedrodalben.bigbangeventos.domain;

import com.pedrodalben.bigbangeventos.session.team.SessionTeam;
import java.util.UUID;

public final class TeamEvents {
    private TeamEvents() {}
    public record TeamCreated(String eventId, UUID sessionId, UUID teamId, String teamDefinitionId) implements DomainEvent {}
    public record PlayerAssignedToTeam(String eventId, UUID sessionId, UUID teamId, UUID playerId) implements DomainEvent {}
    public record PlayerRemovedFromTeam(String eventId, UUID sessionId, UUID teamId, UUID playerId) implements DomainEvent {}
    public record TeamScoreChanged(String eventId, UUID sessionId, UUID teamId, long newScore, long delta) implements DomainEvent {}
    public record TeamEliminated(String eventId, UUID sessionId, UUID teamId) implements DomainEvent {}
    public record TeamFinished(String eventId, UUID sessionId, UUID teamId) implements DomainEvent {}
}
