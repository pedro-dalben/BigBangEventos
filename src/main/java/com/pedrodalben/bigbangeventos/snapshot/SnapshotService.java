package com.pedrodalben.bigbangeventos.snapshot;

import com.pedrodalben.bigbangeventos.api.OperationResult;
import com.pedrodalben.bigbangeventos.platform.AuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class SnapshotService {
    private static final Logger LOG = LoggerFactory.getLogger(SnapshotService.class);
    private final SnapshotGateway gateway;
    private final Map<UUID, PlayerSnapshot> snapshots = new HashMap<>();
    private final Map<UUID, UUID> playerToSnapshot = new HashMap<>();

    public SnapshotService(SnapshotGateway gateway) {
        this.gateway = gateway;
    }

    public synchronized OperationResult prepare(UUID playerId, UUID sessionId, InventoryMode mode) {
        if (mode == InventoryMode.EVENT_KIT || mode == InventoryMode.ISOLATED)
            return OperationResult.fail("not_implemented", "Modo de inventário ainda não implementado: " + mode);

        PlayerSnapshot existing = findPendingForPlayer(playerId);
        if (existing != null)
            return OperationResult.fail("snapshot_exists", "Jogador já possui snapshot pendente");

        UUID snapshotId = UUID.randomUUID();
        PlayerSnapshot snapshot;
        try {
            snapshot = gateway.capture(playerId, snapshotId, sessionId);
        } catch (Exception e) {
            LOG.error("Falha ao capturar snapshot para {}", playerId, e);
            return OperationResult.fail("snapshot_capture_failed", "Snapshot não pôde ser criado");
        }

        snapshots.put(snapshotId, snapshot);
        playerToSnapshot.put(playerId, snapshotId);

        if (mode == InventoryMode.CLEAR_AND_RESTORE) {
            try {
                gateway.clearInventory(playerId);
            } catch (Exception e) {
                LOG.error("Falha ao limpar inventário para {}", playerId, e);
                snapshots.remove(snapshotId);
                playerToSnapshot.remove(playerId);
                try {
                    gateway.restoreInventory(playerId, snapshot);
                } catch (Exception ignored) {}
                return OperationResult.fail("clear_inventory_failed", "Snapshot revertido: não foi possível limpar inventário");
            }
        }

        AuditLogger.snapshotCreated(playerId, snapshotId, sessionId);
        return OperationResult.ok("Snapshot criado");
    }

    public synchronized PlayerSnapshot findPendingForPlayer(UUID playerId) {
        UUID snapshotId = playerToSnapshot.get(playerId);
        if (snapshotId == null) return null;
        PlayerSnapshot snapshot = snapshots.get(snapshotId);
        if (snapshot != null && snapshot.state() == SnapshotState.RESTORED) {
            playerToSnapshot.remove(playerId);
            return null;
        }
        return snapshot;
    }

    public synchronized Optional<PlayerSnapshot> findById(UUID snapshotId) {
        return Optional.ofNullable(snapshots.get(snapshotId));
    }

    public synchronized void loadPersisted(PlayerSnapshot snapshot) {
        snapshots.putIfAbsent(snapshot.snapshotId(), snapshot);
        if (snapshot.state() != SnapshotState.RESTORED)
            playerToSnapshot.putIfAbsent(snapshot.playerId(), snapshot.snapshotId());
    }

    public synchronized Collection<PlayerSnapshot> allPending() {
        return snapshots.values().stream()
                .filter(s -> s.state() != SnapshotState.RESTORED)
                .toList();
    }

    public synchronized OperationResult removeIfRestored(UUID playerId) {
        UUID snapshotId = playerToSnapshot.get(playerId);
        if (snapshotId == null) return OperationResult.ok("Nada a remover");
        PlayerSnapshot s = snapshots.get(snapshotId);
        if (s != null && s.state() == SnapshotState.RESTORED) {
            snapshots.remove(snapshotId);
            playerToSnapshot.remove(playerId);
        }
        return OperationResult.ok("Ok");
    }
}
