package br.com.bigbangcraft.eventos.gladiator;

import com.pedrodalben.bigbangeventos.definition.EventDefinition;
import java.util.Map;

public final class GladiatorConfiguration {
    private GladiatorConfiguration() {}

    public static final String PREFIX = "gladiator.";

    public static final String MODE = PREFIX + "mode";
    public static final String ROUNDS_TOTAL = PREFIX + "rounds.total";
    public static final String ROUND_TIME_LIMIT = PREFIX + "rounds.time-limit-seconds";
    public static final String COUNTDOWN_SECONDS = PREFIX + "rounds.countdown-seconds";
    public static final String PVP_ENABLED = PREFIX + "combat.pvp-enabled";
    public static final String FRIENDLY_FIRE = PREFIX + "combat.friendly-fire";
    public static final String FALL_DAMAGE = PREFIX + "combat.fall-damage";
    public static final String ENVIRONMENT_DAMAGE = PREFIX + "combat.environment-damage";
    public static final String VOID_ELIMINATES = PREFIX + "combat.void-eliminates";
    public static final String SCORE_PER_KILL = PREFIX + "combat.score-per-kill";
    public static final String SCORE_LIMIT = PREFIX + "combat.score-limit";
    public static final String INITIAL_LIVES = PREFIX + "lives.initial";
    public static final String MAXIMUM_LIVES = PREFIX + "lives.maximum";
    public static final String RESPAWN_POLICY = PREFIX + "respawn.policy";
    public static final String RESPAWN_DELAY = PREFIX + "respawn.delay-seconds";
    public static final String INVULNERABILITY = PREFIX + "respawn.invulnerability-seconds";
    public static final String BECOME_SPECTATOR = PREFIX + "elimination.become-spectator";
    public static final String OUT_OF_BOUNDS_POLICY = PREFIX + "arena.out-of-bounds-policy";
    public static final String RANKING_STRATEGY = PREFIX + "ranking.strategy";

    public static String mode(EventDefinition def) {
        return string(def, MODE, "FREE_FOR_ALL");
    }
    public static int roundsTotal(EventDefinition def) {
        return intSetting(def, ROUNDS_TOTAL, 1);
    }
    public static int roundTimeLimit(EventDefinition def) {
        return intSetting(def, ROUND_TIME_LIMIT, 600);
    }
    public static int countdownSeconds(EventDefinition def) {
        return intSetting(def, COUNTDOWN_SECONDS, 10);
    }
    public static boolean pvpEnabled(EventDefinition def) {
        return boolSetting(def, PVP_ENABLED, true);
    }
    public static boolean friendlyFire(EventDefinition def) {
        return boolSetting(def, FRIENDLY_FIRE, false);
    }
    public static boolean fallDamage(EventDefinition def) {
        return boolSetting(def, FALL_DAMAGE, true);
    }
    public static boolean environmentDamage(EventDefinition def) {
        return boolSetting(def, ENVIRONMENT_DAMAGE, true);
    }
    public static boolean voidEliminates(EventDefinition def) {
        return boolSetting(def, VOID_ELIMINATES, true);
    }
    public static int scorePerKill(EventDefinition def) {
        return intSetting(def, SCORE_PER_KILL, 1);
    }
    public static int scoreLimit(EventDefinition def) {
        return intSetting(def, SCORE_LIMIT, 0);
    }
    public static int initialLives(EventDefinition def) {
        return intSetting(def, INITIAL_LIVES, 3);
    }
    public static int maximumLives(EventDefinition def) {
        return intSetting(def, MAXIMUM_LIVES, 3);
    }
    public static String respawnPolicy(EventDefinition def) {
        return string(def, RESPAWN_POLICY, "DELAYED");
    }
    public static int respawnDelay(EventDefinition def) {
        return intSetting(def, RESPAWN_DELAY, 5);
    }
    public static int invulnerabilitySeconds(EventDefinition def) {
        return intSetting(def, INVULNERABILITY, 3);
    }
    public static boolean becomeSpectator(EventDefinition def) {
        return boolSetting(def, BECOME_SPECTATOR, true);
    }
    public static String outOfBoundsPolicy(EventDefinition def) {
        return string(def, OUT_OF_BOUNDS_POLICY, "ELIMINATE");
    }
    public static String rankingStrategy(EventDefinition def) {
        return string(def, RANKING_STRATEGY, "SURVIVAL_THEN_KILLS");
    }

    private static String string(EventDefinition def, String key, String fallback) {
        Object v = settings(def).get(key);
        return v instanceof String s ? s : fallback;
    }
    private static int intSetting(EventDefinition def, String key, int fallback) {
        Object v = settings(def).get(key);
        if (v instanceof Number n) return n.intValue();
        return fallback;
    }
    private static boolean boolSetting(EventDefinition def, String key, boolean fallback) {
        Object v = settings(def).get(key);
        if (v instanceof Boolean b) return b;
        if (v instanceof String s) return Boolean.parseBoolean(s);
        return fallback;
    }
    @SuppressWarnings("unchecked")
    private static Map<String, Object> settings(EventDefinition def) {
        return (Map<String, Object>) def.typeSettings().getOrDefault("gladiator", Map.of());
    }
}
