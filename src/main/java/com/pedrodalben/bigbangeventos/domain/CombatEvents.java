package com.pedrodalben.bigbangeventos.domain;

import com.pedrodalben.bigbangeventos.participant.combat.EliminationReason;
import java.util.UUID;

public final class CombatEvents {
    private CombatEvents() {}
    public record ParticipantDamaged(String eventId, UUID sessionId, UUID victimId, UUID attackerId, float damage, String source) implements DomainEvent {}
    public record ParticipantDeath(String eventId, UUID sessionId, UUID victimId, UUID killerId, String damageSource) implements DomainEvent {}
    public record ParticipantKill(String eventId, UUID sessionId, UUID killerId, UUID victimId) implements DomainEvent {}
    public record ParticipantLifeChanged(String eventId, UUID sessionId, UUID playerId, int livesRemaining, int delta) implements DomainEvent {}
    public record ParticipantRespawnScheduled(String eventId, UUID sessionId, UUID playerId, long delaySeconds) implements DomainEvent {}
    public record ParticipantRespawned(String eventId, UUID sessionId, UUID playerId) implements DomainEvent {}
    public record ParticipantEliminated(String eventId, UUID sessionId, UUID playerId, EliminationReason reason) implements DomainEvent {}
    public record ParticipantBecameSpectator(String eventId, UUID sessionId, UUID playerId) implements DomainEvent {}
    public record ParticipantLeftSpectator(String eventId, UUID sessionId, UUID playerId) implements DomainEvent {}
}
