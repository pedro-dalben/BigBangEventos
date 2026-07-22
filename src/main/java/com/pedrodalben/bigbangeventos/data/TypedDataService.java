package com.pedrodalben.bigbangeventos.data;

import com.pedrodalben.bigbangeventos.session.EventSession;
import java.util.UUID;

public final class TypedDataService {
    public DataContainer participant(EventSession session, UUID playerId) {
        return session.participant(playerId).orElseThrow(() -> new IllegalArgumentException("Jogador não participa")).typedData();
    }
    public DataContainer session(EventSession session) { return session.data(); }
}
