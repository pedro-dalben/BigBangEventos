package br.com.bigbangcraft.eventos.pokegladiator;

import com.pedrodalben.bigbangeventos.definition.EventDefinition;

import java.util.Map;

@SuppressWarnings("unchecked")
public final class PokeGladiatorConfiguration {
    private PokeGladiatorConfiguration() {}

    private static final String KEY = "poke_gladiator";

    private static Map<String, Object> settings(EventDefinition def) {
        Object raw = def.typeSettings().get(KEY);
        return raw instanceof Map ? (Map<String, Object>) raw : Map.of();
    }

    public static String mode(EventDefinition def) { return string(settings(def), "mode", "LAST_TRAINER_STANDING"); }
    public static String sendOutMode(EventDefinition def) { return string(settings(def), "send_out", "MANUAL_SEND_OUT"); }
    public static String lossPolicy(EventDefinition def) { return string(settings(def), "loss_policy", "TRAINER_DEATH"); }
    public static String recoveryPolicy(EventDefinition def) { return string(settings(def), "recovery", "RESTORE_AFTER_ROUND"); }
    public static String rankingStrategy(EventDefinition def) { return string(settings(def), "ranking", "SURVIVAL_THEN_COMBINED_KILLS"); }

    public static int registeredPerTrainer(EventDefinition def) { return intSetting(settings(def), "registered", 1); }
    public static int maxActive(EventDefinition def) { return intSetting(settings(def), "max_active", 1); }
    public static int initialLives(EventDefinition def) { return intSetting(settings(def), "initial_lives", 3); }
    public static int maxLives(EventDefinition def) { return intSetting(settings(def), "max_lives", 3); }
    public static int roundsTotal(EventDefinition def) { return intSetting(settings(def), "rounds", 1); }
    public static int roundTime(EventDefinition def) { return intSetting(settings(def), "round_time", 600); }
    public static int respawnDelay(EventDefinition def) { return intSetting(settings(def), "respawn_delay", 5); }
    public static int invulnerability(EventDefinition def) { return intSetting(settings(def), "invulnerability", 3); }

    public static boolean trainerVsTrainer(EventDefinition def) { return bool(settings(def), "trainer_vs_trainer", true); }
    public static boolean trainerVsPokemon(EventDefinition def) { return bool(settings(def), "trainer_vs_pokemon", true); }
    public static boolean pokemonVsTrainer(EventDefinition def) { return bool(settings(def), "pokemon_vs_trainer", true); }
    public static boolean pokemonVsPokemon(EventDefinition def) { return bool(settings(def), "pokemon_vs_pokemon", true); }
    public static boolean friendlyFire(EventDefinition def) { return bool(settings(def), "friendly_fire", false); }
    public static boolean wildPokemonAllowed(EventDefinition def) { return bool(settings(def), "wild_pokemon", false); }
    public static boolean becomeSpectator(EventDefinition def) { return bool(settings(def), "become_spectator", true); }
    public static boolean pokemonXp(EventDefinition def) { return bool(settings(def), "pokemon_xp", false); }
    public static boolean evGain(EventDefinition def) { return bool(settings(def), "ev_gain", false); }
    public static boolean evolutionProgress(EventDefinition def) { return bool(settings(def), "evolution", false); }
    public static boolean mobDrops(EventDefinition def) { return bool(settings(def), "mob_drops", false); }
    public static boolean minecraftXp(EventDefinition def) { return bool(settings(def), "minecraft_xp", false); }

    private static String string(Map<String, Object> map, String key, String def) {
        Object v = map.get(key);
        return v != null ? v.toString() : def;
    }

    private static int intSetting(Map<String, Object> map, String key, int def) {
        Object v = map.get(key);
        return v instanceof Number ? ((Number) v).intValue() : def;
    }

    private static boolean bool(Map<String, Object> map, String key, boolean def) {
        Object v = map.get(key);
        if (v instanceof Boolean b) return b;
        if (v instanceof String s) return Boolean.parseBoolean(s);
        return def;
    }
}
