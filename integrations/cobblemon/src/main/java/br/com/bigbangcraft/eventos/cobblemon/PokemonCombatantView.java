package br.com.bigbangcraft.eventos.cobblemon;

import com.cobblemon.mod.common.pokemon.Pokemon;

import java.util.*;

public final class PokemonCombatantView {
    private final UUID pokemonUuid;
    private final UUID entityUuid;
    private final UUID trainerUuid;
    private final String speciesId;
    private final String formId;
    private final int level;
    private final int partySlot;
    private final int currentHp;
    private final int maximumHp;
    private final boolean fainted;
    private final boolean inTraditionalBattle;
    private final boolean active;

    private PokemonCombatantView(UUID pokemonUuid, UUID entityUuid, UUID trainerUuid,
                                  String speciesId, String formId, int level, int partySlot,
                                  int currentHp, int maximumHp, boolean fainted,
                                  boolean inTraditionalBattle, boolean active) {
        this.pokemonUuid = pokemonUuid;
        this.entityUuid = entityUuid;
        this.trainerUuid = trainerUuid;
        this.speciesId = speciesId;
        this.formId = formId;
        this.level = level;
        this.partySlot = partySlot;
        this.currentHp = currentHp;
        this.maximumHp = maximumHp;
        this.fainted = fainted;
        this.inTraditionalBattle = inTraditionalBattle;
        this.active = active;
    }

    public static PokemonCombatantView from(Pokemon pokemon, UUID trainerUuid, int partySlot,
                                            UUID entityUuid, boolean active) {
        return new PokemonCombatantView(
            pokemon.getUuid(), entityUuid, trainerUuid,
            pokemon.getSpecies().getName(), pokemon.getForm().getName(),
            pokemon.getLevel(), partySlot,
            pokemon.getCurrentHealth(), pokemon.getMaxHealth(),
            pokemon.isFainted(), false, active
        );
    }

    public UUID pokemonUuid() { return pokemonUuid; }
    public UUID entityUuid() { return entityUuid; }
    public UUID trainerUuid() { return trainerUuid; }
    public String speciesId() { return speciesId; }
    public String formId() { return formId; }
    public int level() { return level; }
    public int partySlot() { return partySlot; }
    public int currentHp() { return currentHp; }
    public int maximumHp() { return maximumHp; }
    public boolean fainted() { return fainted; }
    public boolean inTraditionalBattle() { return inTraditionalBattle; }
    public boolean active() { return active; }
}
