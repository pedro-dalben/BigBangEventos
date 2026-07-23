package com.pedrodalben.bigbangeventos.api.combat;

import com.pedrodalben.bigbangeventos.domain.DomainEventBus;
import com.pedrodalben.bigbangeventos.session.EventSession;

import java.time.Instant;
import java.util.*;

public final class CombatAttributionService {
    private final DomainEventBus events;
    private final CombatProviderRegistry providers;
    private final Map<String, ProjectileAttribution> projectileAttributions = new HashMap<>();
    private long attributionExpirySeconds = 30;

    public CombatAttributionService(DomainEventBus events, CombatProviderRegistry providers) {
        this.events = events;
        this.providers = providers;
    }

    public void setAttributionExpirySeconds(long seconds) { this.attributionExpirySeconds = seconds; }

    public void attributeDamage(EventSession session, UUID victimId, UUID attackerEntityId,
                                float damage, String source, String eventId) {
        CombatActor attacker = resolveAttacker(attackerEntityId, session.id());
        UUID attackerOwner = attacker != null ? attacker.ownerPlayerId() : null;

        if (attackerOwner != null && isParticipating(session, attackerOwner)) {
            events.publish(new com.pedrodalben.bigbangeventos.domain.CombatEvents
                .ParticipantDamaged(eventId, session.id(), victimId, attackerOwner, damage, source));
        }
    }

    public void attributeKill(EventSession session, UUID victimId, UUID killerEntityId,
                              String damageSource, String eventId) {
        CombatActor killer = resolveAttacker(killerEntityId, session.id());
        UUID killerOwner = killer != null ? killer.ownerPlayerId() : null;

        if (killerOwner != null && isParticipating(session, killerOwner)) {
            events.publish(new com.pedrodalben.bigbangeventos.domain.CombatEvents
                .ParticipantKill(eventId, session.id(), killerOwner, victimId));
        }
    }

    public void registerProjectile(String attributionId, UUID projectileEntityId,
                                   UUID sourcePokemonUuid, UUID trainerId) {
        projectileAttributions.put(attributionId,
            new ProjectileAttribution(projectileEntityId, sourcePokemonUuid, trainerId, Instant.now()));
    }

    public Optional<ProjectileAttribution> resolveProjectile(String attributionId) {
        return Optional.ofNullable(projectileAttributions.get(attributionId));
    }

    public void clearProjectile(String attributionId) {
        projectileAttributions.remove(attributionId);
    }

    public void expireStaleAttributions() {
        Instant cutoff = Instant.now().minusSeconds(attributionExpirySeconds);
        projectileAttributions.entrySet().removeIf(e -> e.getValue().registeredAt().isBefore(cutoff));
    }

    public void cleanupSession(UUID sessionId) {
        projectileAttributions.clear();
    }

    private CombatActor resolveAttacker(UUID entityId, UUID sessionId) {
        for (CombatProvider provider : providers.all()) {
            try {
                CombatActorRef ref = provider.resolveActor(entityId);
                if (ref == null) continue;
                CombatOwnership ownership = provider.resolveOwnership(entityId);
                UUID ownerId = ownership != null ? ownership.ownerPlayerId() : null;
                UUID subjectId = ownership != null ? ownership.persistentSubjectId() : null;
                return new CombatActor(ref, ownerId, subjectId, ref.actorId(), sessionId, null, null);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private boolean isParticipating(EventSession session, UUID playerId) {
        return session.participants().stream()
            .anyMatch(p -> p.playerId().equals(playerId));
    }

    public record ProjectileAttribution(
        UUID projectileEntityId,
        UUID sourcePokemonUuid,
        UUID trainerId,
        Instant registeredAt
    ) {}
}
