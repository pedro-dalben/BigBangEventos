package com.pedrodalben.bigbangeventos.core.spectator;

import com.pedrodalben.bigbangeventos.api.OperationResult;
import com.pedrodalben.bigbangeventos.domain.CombatEvents;
import com.pedrodalben.bigbangeventos.domain.DomainEventBus;
import com.pedrodalben.bigbangeventos.participant.spectator.SpectatorReason;
import com.pedrodalben.bigbangeventos.platform.PlatformTeleportService;
import com.pedrodalben.bigbangeventos.platform.StoredLocation;
import com.pedrodalben.bigbangeventos.session.EventSession;
import com.pedrodalben.bigbangeventos.participant.ParticipantState;
import java.util.Optional;
import java.util.UUID;

public final class SpectatorService {
    private final DomainEventBus events;
    private final PlatformTeleportService teleport;

    public SpectatorService(DomainEventBus events, PlatformTeleportService teleport) {
        this.events = events; this.teleport = teleport;
    }

    public OperationResult makeSpectator(EventSession session, UUID playerId, SpectatorReason reason,
                                          StoredLocation spectatorSpawn) {
        if (session.hasSpectator(playerId)) return OperationResult.ok("Jogador já é espectador");
        session.addSpectator(playerId);
        session.participant(playerId).ifPresent(p -> p.state(ParticipantState.RESTORE_PENDING));
        if (spectatorSpawn != null) {
            teleport.teleport(playerId, spectatorSpawn);
        }
        events.publish(new CombatEvents.ParticipantBecameSpectator(session.eventId(), session.id(), playerId));
        return OperationResult.ok("Jogador agora é espectador");
    }

    public OperationResult removeSpectator(EventSession session, UUID playerId, StoredLocation destination) {
        if (!session.hasSpectator(playerId)) return OperationResult.fail("not_spectator", "Jogador não é espectador");
        session.removeSpectator(playerId);
        if (destination != null) {
            teleport.teleport(playerId, destination);
        }
        events.publish(new CombatEvents.ParticipantLeftSpectator(session.eventId(), session.id(), playerId));
        return OperationResult.ok("Espectador removido");
    }

    public boolean isSpectator(EventSession session, UUID playerId) {
        return session.hasSpectator(playerId);
    }

    public void cleanup(EventSession session) {
        // cleanup handled by session lifecycle
    }
}
