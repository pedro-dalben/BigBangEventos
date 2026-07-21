package com.pedrodalben.bigbangeventos.platform;

import com.pedrodalben.bigbangeventos.api.OperationResult;
import java.util.UUID;

public interface PlatformTeleportService {
    OperationResult teleport(UUID playerId, StoredLocation destination);
}
