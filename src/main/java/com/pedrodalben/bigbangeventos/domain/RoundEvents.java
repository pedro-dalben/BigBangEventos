package com.pedrodalben.bigbangeventos.domain;

import com.pedrodalben.bigbangeventos.session.round.SessionRound;
import java.util.UUID;

public final class RoundEvents {
    private RoundEvents() {}
    public record RoundPrepared(String eventId, UUID sessionId, UUID roundId, int number) implements DomainEvent {}
    public record RoundStarted(String eventId, UUID sessionId, UUID roundId, int number) implements DomainEvent {}
    public record RoundFinished(String eventId, UUID sessionId, UUID roundId, int number,
                                UUID winnerParticipantId, String winnerTeamDefId) implements DomainEvent {}
    public record RoundFailed(String eventId, UUID sessionId, UUID roundId, int number) implements DomainEvent {}
    public record RoundCancelled(String eventId, UUID sessionId, UUID roundId, int number) implements DomainEvent {}
}
