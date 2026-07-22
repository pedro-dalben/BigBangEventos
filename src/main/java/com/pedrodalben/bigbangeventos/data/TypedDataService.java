package com.pedrodalben.bigbangeventos.data;

import com.pedrodalben.bigbangeventos.session.EventSession;
import com.pedrodalben.bigbangeventos.session.team.SessionTeam;
import java.util.UUID;

public final class TypedDataService {
    public DataContainer participant(EventSession session, UUID playerId) {
        return session.participant(playerId).orElseThrow(() -> new IllegalArgumentException("Jogador não participa")).typedData();
    }
    public DataContainer session(EventSession session) { return session.data(); }
    public DataContainer team(EventSession session, String teamDefId) {
        SessionTeam t = session.team(teamDefId).orElseThrow(() -> new IllegalArgumentException("Time não encontrado"));
        return t.data();
    }
}
