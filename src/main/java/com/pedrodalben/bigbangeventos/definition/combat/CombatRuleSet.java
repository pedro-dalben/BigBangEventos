package com.pedrodalben.bigbangeventos.definition.combat;

import java.util.List;

public record CombatRuleSet(
    boolean pvpEnabled,
    boolean friendlyFire,
    boolean fallDamage,
    boolean voidEliminates,
    boolean environmentDamage,
    boolean allowItemDrop,
    boolean allowItemPickup,
    boolean allowBlockBreak,
    boolean allowBlockPlace,
    boolean allowInteraction,
    List<String> blockedCommands,
    OutOfBoundsPolicy outOfBoundsPolicy
) {
    public static CombatRuleSet defaults() {
        return new CombatRuleSet(true, false, true, true, true, false, false, false, false, false, List.of(), OutOfBoundsPolicy.IGNORE);
    }

    public enum OutOfBoundsPolicy { IGNORE, TELEPORT_BACK, DAMAGE, ELIMINATE }
}
