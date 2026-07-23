package br.com.bigbangcraft.eventos.cobblemon;

import com.cobblemon.mod.common.pokemon.Pokemon;

import java.util.*;

final class PokemonSnapshot {
    private final UUID pokemonUuid;
    private final int hp;
    private final boolean fainted;
    private boolean restored;

    PokemonSnapshot(UUID pokemonUuid, int hp, boolean fainted) {
        this.pokemonUuid = pokemonUuid;
        this.hp = hp;
        this.fainted = fainted;
        this.restored = false;
    }

    static PokemonSnapshot from(Pokemon pokemon) {
        return new PokemonSnapshot(
            pokemon.getUuid(),
            pokemon.getCurrentHealth(),
            pokemon.isFainted()
        );
    }

    void restore(Pokemon pokemon) {
        if (restored) return;
        if (!pokemon.getUuid().equals(pokemonUuid)) return;
        pokemon.setCurrentHealth(hp);
        if (fainted != pokemon.isFainted()) {
            // ponytail: setFainted not available as public API; rely on HP restore
        }
        restored = true;
    }

    boolean isRestored() { return restored; }

    Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("pokemonUuid", pokemonUuid);
        map.put("hp", hp);
        map.put("fainted", fainted);
        return map;
    }

    @SuppressWarnings("unchecked")
    static PokemonSnapshot fromMap(Map<String, Object> map) {
        UUID uuid = (UUID) map.get("pokemonUuid");
        int hp = ((Number) map.getOrDefault("hp", 0)).intValue();
        boolean fainted = (boolean) map.getOrDefault("fainted", false);
        return new PokemonSnapshot(uuid, hp, fainted);
    }
}
