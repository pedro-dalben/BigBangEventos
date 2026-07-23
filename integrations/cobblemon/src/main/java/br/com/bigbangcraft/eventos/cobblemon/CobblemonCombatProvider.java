package br.com.bigbangcraft.eventos.cobblemon;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.pedrodalben.bigbangeventos.api.combat.*;
import com.pedrodalben.bigbangeventos.domain.DomainEventBus;
import com.pedrodalben.bigbangeventos.domain.ExternalCombatEvents;

import java.util.*;

public class CobblemonCombatProvider implements CombatProvider {

    private static final String PROVIDER_ID = "cobblemon";

    private final DomainEventBus events;
    private final Map<UUID, ActorEntry> activeActors = new HashMap<>();
    private final Map<UUID, UUID> pokemonToEntity = new HashMap<>();
    private final Map<UUID, PokemonSnapshot> pokemonSnapshots = new HashMap<>();
    private final Map<UUID, Set<UUID>> sessionActors = new HashMap<>();

    public CobblemonCombatProvider(DomainEventBus events) {
        this.events = events;
    }

    @Override public String id() { return PROVIDER_ID; }
    @Override public String displayName() { return "Cobblemon"; }

    @Override
    public Set<ProviderCapability> capabilities() {
        return Set.of(
            ProviderCapability.RESOLVE_ENTITY,
            ProviderCapability.RESOLVE_OWNER,
            ProviderCapability.RECALL_ENTITY,
            ProviderCapability.LIST_ACTIVE_ENTITIES,
            ProviderCapability.CAPTURE_STATE,
            ProviderCapability.RESTORE_STATE,
            ProviderCapability.DETECT_FAINT,
            ProviderCapability.BLOCK_TRADITIONAL_BATTLE
        );
    }

    @Override
    public CombatActorRef resolveActor(UUID entityId) {
        ActorEntry entry = activeActors.get(entityId);
        return entry != null ? entry.ref() : null;
    }

    @Override
    public CombatOwnership resolveOwnership(UUID entityId) {
        ActorEntry entry = activeActors.get(entityId);
        if (entry == null) return null;
        return new CombatOwnership(entry.ownerId(), entry.pokemonUuid(), PROVIDER_ID, "pokemon");
    }

    @Override
    public List<CombatActorRef> listActiveActors(UUID sessionId) {
        Set<UUID> ids = sessionActors.get(sessionId);
        if (ids == null) return List.of();
        return ids.stream().map(e -> activeActors.get(e)).filter(Objects::nonNull).map(ActorEntry::ref).toList();
    }

    @Override
    public void recallEntity(UUID actorId) {
        ActorEntry entry = activeActors.get(actorId);
        if (entry != null && entry.entity() != null && !entry.entity().isRemoved()) {
            entry.entity().discard();
        }
    }

    @Override
    public void recallAllInSession(UUID sessionId) {
        Set<UUID> ids = sessionActors.get(sessionId);
        if (ids == null) return;
        for (UUID entityId : new ArrayList<>(ids)) {
            recallEntity(entityId);
        }
    }

    @Override
    public Map<String, Object> captureState(UUID actorId) {
        ActorEntry entry = activeActors.get(actorId);
        if (entry == null || entry.pokemon() == null) return Map.of();
        PokemonSnapshot snapshot = PokemonSnapshot.from(entry.pokemon());
        pokemonSnapshots.put(entry.pokemonUuid(), snapshot);
        return snapshot.toMap();
    }

    @Override
    public void restoreState(UUID actorId, Map<String, Object> state) {
        if (state == null || state.isEmpty()) return;
        UUID pkmUuid = (UUID) state.get("pokemonUuid");
        if (pkmUuid == null) return;
        PokemonSnapshot snapshot = pokemonSnapshots.get(pkmUuid);
        if (snapshot == null || snapshot.isRestored()) return;
        ActorEntry entry = activeActors.get(actorId);
        if (entry != null && entry.pokemon() != null) {
            snapshot.restore(entry.pokemon());
        }
    }

    @Override
    public void cleanup(UUID sessionId) {
        recallAllInSession(sessionId);
        sessionActors.remove(sessionId);
    }

    public void cleanupAll() {
        for (UUID id : new ArrayList<>(activeActors.keySet())) {
            recallEntity(id);
        }
        activeActors.clear();
        pokemonToEntity.clear();
        sessionActors.clear();
        pokemonSnapshots.clear();
    }

    public void assignToSession(UUID entityId, UUID sessionId) {
        sessionActors.computeIfAbsent(sessionId, k -> new HashSet<>()).add(entityId);
    }

    public void removeFromSession(UUID entityId, UUID sessionId) {
        Set<UUID> ids = sessionActors.get(sessionId);
        if (ids != null) ids.remove(entityId);
    }

    void onPokemonSent(PokemonEntity entity) {
        Pokemon pokemon = entity.getPokemon();
        if (pokemon == null) return;
        UUID owner = entity.getOwnerUUID();

        CombatActorRef ref = new CombatActorRef(
            "cobblemon:" + pokemon.getUuid(),
            entity.getUUID(),
            owner != null ? CombatActorType.OWNED_ENTITY : CombatActorType.WILD_ENTITY,
            PROVIDER_ID, "pokemon"
        );
        UUID entityId = entity.getUUID();
        UUID pokeUuid = pokemon.getUuid();
        activeActors.put(entityId, new ActorEntry(ref, entity, pokemon, owner, pokeUuid));
        pokemonToEntity.put(pokeUuid, entityId);
    }

    void onPokemonRecalled(PokemonEntity entity) {
        ActorEntry entry = activeActors.remove(entity.getUUID());
        if (entry != null) pokemonToEntity.remove(entry.pokemonUuid());
    }

    void onPokemonFainted(UUID pokemonUuid) {
        UUID entityId = pokemonToEntity.remove(pokemonUuid);
        if (entityId == null) return;
        ActorEntry entry = activeActors.remove(entityId);
        if (entry != null) {
            events.publish(new ExternalCombatEvents.OwnedCombatantDefeated(
                null, null, entityId, entry.ownerId(), pokemonUuid
            ));
        }
    }

    UUID getOwnerOf(UUID entityId) {
        ActorEntry entry = activeActors.get(entityId);
        return entry != null ? entry.ownerId() : null;
    }

    Pokemon getPokemonOf(UUID entityId) {
        ActorEntry entry = activeActors.get(entityId);
        return entry != null ? entry.pokemon() : null;
    }

    private record ActorEntry(CombatActorRef ref, PokemonEntity entity, Pokemon pokemon, UUID ownerId, UUID pokemonUuid) {}
}
