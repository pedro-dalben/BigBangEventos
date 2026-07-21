package com.pedrodalben.bigbangeventos.trigger;
import java.util.UUID; public interface PermissionChecker { boolean has(UUID playerId, String permission); }
