package br.com.bigbangcraft.eventos.cobblemon;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;

final class PokemonRecaller {
    private PokemonRecaller() {}

    static void recall(PokemonEntity entity, Pokemon pokemon) {
        if (entity == null || pokemon == null) return;
        if (entity.isRemoved()) return;
        try {
            entity.discard();
        } catch (Exception ignored) {}
    }
}
