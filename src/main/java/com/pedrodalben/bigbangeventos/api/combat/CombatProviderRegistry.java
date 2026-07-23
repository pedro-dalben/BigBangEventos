package com.pedrodalben.bigbangeventos.api.combat;

import java.util.*;

public final class CombatProviderRegistry {
    private final Map<String, CombatProvider> providers = new LinkedHashMap<>();

    public void register(CombatProvider provider) {
        if (providers.containsKey(provider.id()))
            throw new IllegalArgumentException("Provider já registrado: " + provider.id());
        providers.put(provider.id(), provider);
    }

    public Optional<CombatProvider> find(String id) { return Optional.ofNullable(providers.get(id)); }

    public Collection<CombatProvider> all() { return List.copyOf(providers.values()); }

    public CombatProvider getOrThrow(String id) {
        return find(id).orElseThrow(() -> new IllegalArgumentException("Provider não encontrado: " + id));
    }

    public boolean hasCapability(String providerId, ProviderCapability capability) {
        return find(providerId).map(p -> p.capabilities().contains(capability)).orElse(false);
    }
}
