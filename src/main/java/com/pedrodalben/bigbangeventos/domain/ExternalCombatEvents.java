package com.pedrodalben.bigbangeventos.domain;

import com.pedrodalben.bigbangeventos.api.combat.CombatActorRef;

import java.util.UUID;

public final class ExternalCombatEvents {
    private ExternalCombatEvents() {}

    public record ExternalCombatActorRegistered(
        String eventId,
        UUID sessionId,
        CombatActorRef actorRef
    ) implements DomainEvent {}

    public record ExternalCombatActorRemoved(
        String eventId,
        UUID sessionId,
        CombatActorRef actorRef
    ) implements DomainEvent {}

    public record OwnedCombatantDamaged(
        String eventId,
        UUID sessionId,
        UUID ownedEntityId,
        UUID trainerId,
        float damage,
        String source
    ) implements DomainEvent {}

    public record OwnedCombatantDefeated(
        String eventId,
        UUID sessionId,
        UUID ownedEntityId,
        UUID trainerId,
        UUID persistentSubjectId
    ) implements DomainEvent {}

    public record OwnedCombatantTargetChanged(
        String eventId,
        UUID sessionId,
        UUID ownedEntityId,
        UUID oldTargetId,
        UUID newTargetId
    ) implements DomainEvent {}

    public record OwnedCombatantRecalled(
        String eventId,
        UUID sessionId,
        UUID ownedEntityId,
        UUID trainerId
    ) implements DomainEvent {}

    public record ExternalProjectileAttributed(
        String eventId,
        UUID sessionId,
        UUID projectileEntityId,
        UUID sourcePokemonUuid,
        UUID trainerId
    ) implements DomainEvent {}

    public record ExternalCombatBlocked(
        String eventId,
        UUID sessionId,
        UUID actorId,
        String reason
    ) implements DomainEvent {}
}
