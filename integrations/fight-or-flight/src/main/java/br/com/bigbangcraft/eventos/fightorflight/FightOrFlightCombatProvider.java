package br.com.bigbangcraft.eventos.fightorflight;

import com.pedrodalben.bigbangeventos.api.combat.*;
import com.pedrodalben.bigbangeventos.domain.DomainEventBus;
import com.pedrodalben.bigbangeventos.domain.ExternalCombatEvents;

import java.util.*;

public class FightOrFlightCombatProvider implements CombatProvider {

    private static final String PROVIDER_ID = "fight_or_flight";

    private final DomainEventBus events;
    private final Map<UUID, TargetRecord> targetRecords = new HashMap<>();
    private final Set<UUID> blockedEntities = new HashSet<>();
    private boolean enabled = true;

    public FightOrFlightCombatProvider(DomainEventBus events) {
        this.events = events;
    }

    @Override public String id() { return PROVIDER_ID; }
    @Override public String displayName() { return "Fight or Flight"; }

    @Override
    public Set<ProviderCapability> capabilities() {
        return Set.of(
            ProviderCapability.RESOLVE_PROJECTILE,
            ProviderCapability.ATTRIBUTE_MELEE,
            ProviderCapability.ATTRIBUTE_RANGED,
            ProviderCapability.CLEAR_TARGET,
            ProviderCapability.BLOCK_TARGET
        );
    }

    @Override
    public void clearTarget(UUID actorId) {
        targetRecords.remove(actorId);
    }

    @Override
    public void clearAllTargets(UUID sessionId) {
        targetRecords.clear();
    }

    @Override
    public void cleanup(UUID sessionId) {
        clearAllTargets(sessionId);
        blockedEntities.clear();
    }

    public void cleanupAll() {
        targetRecords.clear();
        blockedEntities.clear();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isBlocked(UUID entityId) {
        return blockedEntities.contains(entityId);
    }

    public void blockEntity(UUID entityId) {
        blockedEntities.add(entityId);
    }

    public void unblockEntity(UUID entityId) {
        blockedEntities.remove(entityId);
    }

    public void recordTarget(UUID pokemonEntityId, UUID newTargetId) {
        UUID oldTargetId = null;
        TargetRecord existing = targetRecords.get(pokemonEntityId);
        if (existing != null) oldTargetId = existing.targetId();
        targetRecords.put(pokemonEntityId, new TargetRecord(pokemonEntityId, newTargetId, System.currentTimeMillis()));
    }

    public UUID getCurrentTarget(UUID pokemonEntityId) {
        TargetRecord record = targetRecords.get(pokemonEntityId);
        return record != null ? record.targetId() : null;
    }

    public record TargetRecord(UUID pokemonEntityId, UUID targetId, long timestamp) {}
}
