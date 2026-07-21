package com.pedrodalben.bigbangeventos.core;

import com.pedrodalben.bigbangeventos.api.OperationResult;
import com.pedrodalben.bigbangeventos.snapshot.SnapshotGateway;
import com.pedrodalben.bigbangeventos.snapshot.InventoryMode;

import java.util.UUID;

public final class EventPlayerStateService {
    private final SnapshotGateway gateway;

    public EventPlayerStateService(SnapshotGateway gateway) {
        this.gateway = gateway;
    }

    public OperationResult applyEventState(UUID playerId, InventoryMode mode) {
        if (mode == InventoryMode.CLEAR_AND_RESTORE) {
            try {
                gateway.clearInventory(playerId);
                return OperationResult.ok("Estado de evento aplicado");
            } catch (Exception e) {
                return OperationResult.fail("apply_failed", "Falha ao aplicar estado de evento");
            }
        }
        return OperationResult.ok("Estado preservado (KEEP)");
    }
}
