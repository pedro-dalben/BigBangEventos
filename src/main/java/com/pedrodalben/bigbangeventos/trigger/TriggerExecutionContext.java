package com.pedrodalben.bigbangeventos.trigger;
import com.pedrodalben.bigbangeventos.session.EventSession; import com.pedrodalben.bigbangeventos.definition.EventDefinition; import java.util.UUID;
public record TriggerExecutionContext(EventSession session, UUID playerId, String playerName, PermissionChecker permissions, TriggerEffects effects, EventDefinition definition) {
    public TriggerExecutionContext(EventSession session, UUID playerId, String playerName, PermissionChecker permissions, TriggerEffects effects) { this(session,playerId,playerName,permissions,effects,null); }
}
