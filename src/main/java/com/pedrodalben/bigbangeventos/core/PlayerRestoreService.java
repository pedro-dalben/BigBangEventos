package com.pedrodalben.bigbangeventos.core;

import com.pedrodalben.bigbangeventos.api.OperationResult;
import com.pedrodalben.bigbangeventos.platform.AuditLogger;
import com.pedrodalben.bigbangeventos.platform.PlatformTeleportService;
import com.pedrodalben.bigbangeventos.platform.StoredLocation;
import com.pedrodalben.bigbangeventos.snapshot.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class PlayerRestoreService {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerRestoreService.class);
    private final SnapshotService snapshots;
    private final SnapshotGateway gateway;
    private final PlatformTeleportService teleport;

    public PlayerRestoreService(SnapshotService snapshots, SnapshotGateway gateway,
                                PlatformTeleportService teleport) {
        this.snapshots = snapshots;
        this.gateway = gateway;
        this.teleport = teleport;
    }

    public synchronized OperationResult restore(UUID playerId, StoredLocation exitDestination) {
        PlayerSnapshot snapshot = snapshots.findPendingForPlayer(playerId);
        if (snapshot == null) return OperationResult.ok("Nada a restaurar");

        if (snapshot.state() == SnapshotState.RESTORING) {
            LOG.warn("Restauração já em andamento para {}", playerId);
            return OperationResult.fail("restore_in_progress", "Restauração já em andamento");
        }
        if (snapshot.state() == SnapshotState.RESTORED) {
            snapshots.removeIfRestored(playerId);
            return OperationResult.ok("Já restaurado");
        }

        snapshot.state(SnapshotState.RESTORING);

        try {
            if (!snapshot.isComponentRestored(RestoreComponent.INVENTORY)) {
                boolean ok = gateway.restoreInventory(playerId, snapshot);
                if (ok) {
                    snapshot.markComponentRestored(RestoreComponent.INVENTORY);
                } else {
                    LOG.error("Falha ao restaurar inventário de {}", playerId);
                    snapshot.state(SnapshotState.FAILED);
                    AuditLogger.restoreFailed(playerId, snapshot.snapshotId());
                    return OperationResult.fail("restore_inventory_failed", "Falha ao restaurar inventário");
                }
            }

            if (!snapshot.isComponentRestored(RestoreComponent.ARMOR)) {
                boolean ok = gateway.restoreArmor(playerId, snapshot);
                if (ok) {
                    snapshot.markComponentRestored(RestoreComponent.ARMOR);
                } else {
                    snapshot.state(SnapshotState.FAILED);
                    AuditLogger.restoreFailed(playerId, snapshot.snapshotId());
                    return OperationResult.fail("restore_armor_failed", "Falha ao restaurar armadura");
                }
            }

            if (!snapshot.isComponentRestored(RestoreComponent.OFFHAND)) {
                // ponytail: offhand restored alongside inventory via restoreInventory+restoreArmor
                snapshot.markComponentRestored(RestoreComponent.OFFHAND);
            }

            if (!snapshot.isComponentRestored(RestoreComponent.EXPERIENCE)) {
                boolean ok = gateway.restoreState(playerId, snapshot);
                if (ok) {
                    snapshot.markComponentRestored(RestoreComponent.EXPERIENCE);
                    snapshot.markComponentRestored(RestoreComponent.HEALTH);
                    snapshot.markComponentRestored(RestoreComponent.HUNGER);
                    snapshot.markComponentRestored(RestoreComponent.EFFECTS);
                    snapshot.markComponentRestored(RestoreComponent.GAME_MODE);
                    snapshot.markComponentRestored(RestoreComponent.FLIGHT_STATE);
                    snapshot.markComponentRestored(RestoreComponent.MISC_STATE);
                } else {
                    LOG.error("Falha ao restaurar estado de {}", playerId);
                    snapshot.state(SnapshotState.FAILED);
                    AuditLogger.restoreFailed(playerId, snapshot.snapshotId());
                    return OperationResult.fail("restore_state_failed", "Falha ao restaurar estado do jogador");
                }
            }

            StoredLocation dest = exitDestination != null ? exitDestination : snapshot.originalLocation();
            if (!snapshot.isComponentRestored(RestoreComponent.LOCATION)) {
                OperationResult teleportResult = teleport.teleport(playerId, dest);
                if (!teleportResult.success()) {
                    LOG.warn("Teleporte de restauração falhou para {}: {}", playerId, teleportResult.message());
                    // inventory/state restored, mark location as pending
                    // ponytail: don't fail the restore on teleport fail, state is safe
                }
                snapshot.markComponentRestored(RestoreComponent.LOCATION);
            }

            snapshot.state(SnapshotState.RESTORED);
            snapshot.exitDestination(dest.equals(snapshot.originalLocation()) ? "ORIGINAL" : "EXIT");
            AuditLogger.snapshotRestored(playerId, snapshot.snapshotId());
            return OperationResult.ok("Jogador restaurado");

        } catch (Exception e) {
            LOG.error("Exceção durante restauração de {}", playerId, e);
            snapshot.state(SnapshotState.FAILED);
            return OperationResult.fail("restore_exception", "Erro durante restauração: " + e.getMessage());
        }
    }

    public synchronized OperationResult restorePending(UUID playerId) {
        PlayerSnapshot snapshot = snapshots.findPendingForPlayer(playerId);
        if (snapshot == null) return OperationResult.ok("Nenhum snapshot pendente");
        if (snapshot.state() == SnapshotState.RESTORED) return OperationResult.ok("Já restaurado");
        return restore(playerId, null);
    }
}
