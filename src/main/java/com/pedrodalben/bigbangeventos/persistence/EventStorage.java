package com.pedrodalben.bigbangeventos.persistence;

import com.pedrodalben.bigbangeventos.definition.EventDefinition;
import com.pedrodalben.bigbangeventos.session.EventSession;
import com.pedrodalben.bigbangeventos.snapshot.PlayerSnapshot;
import java.util.*;

public interface EventStorage {
    void saveDefinition(EventDefinition definition);
    Optional<EventDefinition> findDefinition(String id);
    Collection<EventDefinition> findDefinitions();
    void deleteDefinition(String id);

    void saveSession(EventSession session);
    Optional<EventSession> findSession(UUID id);
    Collection<EventSession> findUnfinishedSessions();

    void saveSnapshot(PlayerSnapshot snapshot);
    Optional<PlayerSnapshot> findSnapshot(UUID snapshotId);
    Collection<PlayerSnapshot> findSnapshotsByPlayer(UUID playerId);
    void deleteSnapshot(UUID snapshotId);
}
