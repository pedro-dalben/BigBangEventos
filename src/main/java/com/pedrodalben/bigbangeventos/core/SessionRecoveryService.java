package com.pedrodalben.bigbangeventos.core;

import com.pedrodalben.bigbangeventos.api.OperationResult;
import com.pedrodalben.bigbangeventos.participant.EventParticipant;
import com.pedrodalben.bigbangeventos.participant.ParticipantState;
import com.pedrodalben.bigbangeventos.persistence.EventStorage;
import com.pedrodalben.bigbangeventos.platform.AuditLogger;
import com.pedrodalben.bigbangeventos.platform.PlatformPlayerService;
import com.pedrodalben.bigbangeventos.session.EventSession;
import com.pedrodalben.bigbangeventos.session.SessionState;
import com.pedrodalben.bigbangeventos.snapshot.PlayerSnapshot;
import com.pedrodalben.bigbangeventos.snapshot.SnapshotService;
import com.pedrodalben.bigbangeventos.snapshot.SnapshotState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class SessionRecoveryService {
    private static final Logger LOG = LoggerFactory.getLogger(SessionRecoveryService.class);
    private final EventStorage storage;
    private final SnapshotService snapshots;
    private final PlayerRestoreService restore;
    private final PlatformPlayerService players;

    public SessionRecoveryService(EventStorage storage, SnapshotService snapshots,
                                  PlayerRestoreService restore, PlatformPlayerService players) {
        this.storage = storage;
        this.snapshots = snapshots;
        this.restore = restore;
        this.players = players;
    }

    public synchronized void recoverOnStartup() {
        Collection<EventSession> unfinished = storage.findUnfinishedSessions();
        LOG.info("Recuperando {} sessões não finalizadas", unfinished.size());

        for (EventSession session : unfinished) {
            try {
                if (session.state() == SessionState.FINISHED
                        || session.state() == SessionState.CANCELLED
                        || session.state() == SessionState.FAILED) {
                    continue;
                }

                session.state(SessionState.FAILED, java.time.Clock.systemUTC().instant());
                session.cancelReason("SERVER_RESTART");
                storage.saveSession(session);

                mapSnapshotIds(session.id());

                for (EventParticipant p : session.participants()) {
                    if (p.state() == ParticipantState.RESTORED || p.state() == ParticipantState.LEFT)
                        continue;

                    PlayerSnapshot snapshot = snapshots.findPendingForPlayer(p.playerId());
                    if (snapshot != null && snapshot.state() == SnapshotState.RESTORED)
                        continue;

                    if (players.isOnline(p.playerId())) {
                        restore.restorePending(p.playerId());
                    }
                    // ponytail: offline players keep pending snapshot, restored on login
                }

                AuditLogger.sessionRecovered(session.id(), session.eventId());
            } catch (Exception e) {
                LOG.error("Falha ao recuperar sessão {}", session.id(), e);
            }
        }
    }

    private void mapSnapshotIds(UUID sessionId) {
        for (PlayerSnapshot snapshot : snapshots.allPending()) {
            if (snapshot.sessionId().equals(sessionId)) {
                snapshots.loadPersisted(snapshot);
            }
        }
    }
}
