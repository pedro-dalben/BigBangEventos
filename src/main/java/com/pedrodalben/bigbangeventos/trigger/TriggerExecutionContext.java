package com.pedrodalben.bigbangeventos.trigger;
import com.pedrodalben.bigbangeventos.session.EventSession; import java.util.UUID;
public record TriggerExecutionContext(EventSession session, UUID playerId, String playerName, PermissionChecker permissions, TriggerEffects effects) { }
