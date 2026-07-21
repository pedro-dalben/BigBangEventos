package br.com.bigbangcraft.eventos.parkour;

import br.com.bigbangcraft.eventos.parkour.model.*;
import com.pedrodalben.bigbangeventos.definition.EventDefinition;
import com.pedrodalben.bigbangeventos.definition.EventLocation;
import com.pedrodalben.bigbangeventos.definition.LocationName;

import java.util.*;

public final class ParkourConfiguration {

    private ParkourConfiguration() {}

    public static final String KEY_FINISH_RADIUS = "parkour.finishRadius";
    public static final String KEY_FALL_MODE = "parkour.fallMode";
    public static final String KEY_FALL_Y_LEVEL = "parkour.fallYLevel";
    public static final String KEY_RESET_MODE = "parkour.resetMode";
    public static final String KEY_CHECKPOINTS_REQUIRED = "parkour.checkpointsRequired";
    public static final String KEY_MAX_TIME_SECONDS = "parkour.maxTimeSeconds";
    public static final String KEY_MAX_ATTEMPTS = "parkour.maxAttempts";
    public static final String KEY_FINISH_MODE = "parkour.finishMode";
    public static final String KEY_RANKING_STRATEGY = "parkour.rankingStrategy";
    public static final String KEY_COMPLETE_DESTINATION = "parkour.completeDestination";
    public static final String KEY_TOP_N = "parkour.topN";

    // Start / finish stored as EventDefinition locations
    public static Optional<EventLocation> getStartLocation(EventDefinition def) {
        return def.location(LocationName.LOBBY);
    }

    public static void setStartLocation(EventDefinition def, EventLocation loc) {
        def.location(LocationName.LOBBY, loc);
    }

    public static Optional<EventLocation> getFinishLocation(EventDefinition def) {
        return def.location(LocationName.EXIT);
    }

    public static void setFinishLocation(EventDefinition def, EventLocation loc) {
        def.location(LocationName.EXIT, loc);
    }

    // Finish radius
    public static double getFinishRadius(EventDefinition def) {
        return getDouble(def, KEY_FINISH_RADIUS, 2.0);
    }

    public static void setFinishRadius(EventDefinition def, double radius) {
        def.typeSetting(KEY_FINISH_RADIUS, radius);
    }

    // Fall mode
    public static ParkourFallMode getFallMode(EventDefinition def) {
        String val = getString(def, KEY_FALL_MODE, null);
        if (val == null) return ParkourFallMode.Y_LEVEL;
        try { return ParkourFallMode.valueOf(val); } catch (IllegalArgumentException e) { return ParkourFallMode.Y_LEVEL; }
    }

    public static void setFallMode(EventDefinition def, ParkourFallMode mode) {
        def.typeSetting(KEY_FALL_MODE, mode.name());
    }

    // Fall Y level
    public static double getFallYLevel(EventDefinition def) {
        return getDouble(def, KEY_FALL_Y_LEVEL, -64.0);
    }

    public static void setFallYLevel(EventDefinition def, double y) {
        def.typeSetting(KEY_FALL_Y_LEVEL, y);
    }

    // Reset mode
    public static ParkourResetMode getResetMode(EventDefinition def) {
        String val = getString(def, KEY_RESET_MODE, null);
        if (val == null) return ParkourResetMode.START;
        try { return ParkourResetMode.valueOf(val); } catch (IllegalArgumentException e) { return ParkourResetMode.START; }
    }

    public static void setResetMode(EventDefinition def, ParkourResetMode mode) {
        def.typeSetting(KEY_RESET_MODE, mode.name());
    }

    // Checkpoints required
    public static boolean isCheckpointsRequired(EventDefinition def) {
        return getBoolean(def, KEY_CHECKPOINTS_REQUIRED, true);
    }

    public static void setCheckpointsRequired(EventDefinition def, boolean required) {
        def.typeSetting(KEY_CHECKPOINTS_REQUIRED, required);
    }

    // Max time (seconds), 0 = unlimited
    public static long getMaxTimeSeconds(EventDefinition def) {
        return getLong(def, KEY_MAX_TIME_SECONDS, 0L);
    }

    public static void setMaxTimeSeconds(EventDefinition def, long seconds) {
        def.typeSetting(KEY_MAX_TIME_SECONDS, seconds);
    }

    // Max attempts, 0 = unlimited
    public static int getMaxAttempts(EventDefinition def) {
        return getInt(def, KEY_MAX_ATTEMPTS, 0);
    }

    public static void setMaxAttempts(EventDefinition def, int attempts) {
        def.typeSetting(KEY_MAX_ATTEMPTS, attempts);
    }

    // Finish mode
    public static ParkourFinishMode getFinishMode(EventDefinition def) {
        String val = getString(def, KEY_FINISH_MODE, null);
        if (val == null) return ParkourFinishMode.ALL_FINISHERS;
        try { return ParkourFinishMode.valueOf(val); } catch (IllegalArgumentException e) { return ParkourFinishMode.ALL_FINISHERS; }
    }

    public static void setFinishMode(EventDefinition def, ParkourFinishMode mode) {
        def.typeSetting(KEY_FINISH_MODE, mode.name());
    }

    // Ranking strategy
    public static ParkourRankingStrategy getRankingStrategy(EventDefinition def) {
        String val = getString(def, KEY_RANKING_STRATEGY, null);
        if (val == null) return ParkourRankingStrategy.TIME_ASCENDING;
        try { return ParkourRankingStrategy.valueOf(val); } catch (IllegalArgumentException e) { return ParkourRankingStrategy.TIME_ASCENDING; }
    }

    public static void setRankingStrategy(EventDefinition def, ParkourRankingStrategy strategy) {
        def.typeSetting(KEY_RANKING_STRATEGY, strategy.name());
    }

    // Complete destination
    public static ParkourCompleteDestination getCompleteDestination(EventDefinition def) {
        String val = getString(def, KEY_COMPLETE_DESTINATION, null);
        if (val == null) return ParkourCompleteDestination.EXIT;
        try { return ParkourCompleteDestination.valueOf(val); } catch (IllegalArgumentException e) { return ParkourCompleteDestination.EXIT; }
    }

    public static void setCompleteDestination(EventDefinition def, ParkourCompleteDestination dest) {
        def.typeSetting(KEY_COMPLETE_DESTINATION, dest.name());
    }

    // Top N (if finish mode is TOP_N)
    public static int getTopN(EventDefinition def) {
        return getInt(def, KEY_TOP_N, 3);
    }

    public static void setTopN(EventDefinition def, int n) {
        def.typeSetting(KEY_TOP_N, n);
    }

    // Generic helpers
    private static String getString(EventDefinition def, String key, String fallback) {
        Object val = def.typeSettings().get(key);
        return val instanceof String s ? s : fallback;
    }

    private static double getDouble(EventDefinition def, String key, double fallback) {
        Object val = def.typeSettings().get(key);
        if (val instanceof Number n) return n.doubleValue();
        return fallback;
    }

    private static int getInt(EventDefinition def, String key, int fallback) {
        Object val = def.typeSettings().get(key);
        if (val instanceof Number n) return n.intValue();
        return fallback;
    }

    private static long getLong(EventDefinition def, String key, long fallback) {
        Object val = def.typeSettings().get(key);
        if (val instanceof Number n) return n.longValue();
        return fallback;
    }

    private static boolean getBoolean(EventDefinition def, String key, boolean fallback) {
        Object val = def.typeSettings().get(key);
        if (val instanceof Boolean b) return b;
        if (val instanceof String s) return Boolean.parseBoolean(s);
        return fallback;
    }
}
