package br.com.bigbangcraft.eventos.pokegladiator;

import com.pedrodalben.bigbangeventos.participant.EventParticipant;
import com.pedrodalben.bigbangeventos.participant.combat.ParticipantCombatState;
import com.pedrodalben.bigbangeventos.session.EventSession;

import java.util.*;

public final class PokeGladiatorRankingService {

    public List<RankedEntry> rank(EventSession session, String strategy) {
        List<RankedEntry> entries = new ArrayList<>();

        for (EventParticipant p : session.participants()) {
            ParticipantCombatState cs = session.combatState(p.playerId()).orElse(null);
            if (cs == null) continue;

            entries.add(new RankedEntry(
                p.playerId(), p.knownName(),
                cs.sessionKills(), cs.sessionDeaths(),
                cs.livesRemaining(), cs.eliminated()
            ));
        }

        switch (strategy != null ? strategy : "SURVIVAL_THEN_COMBINED_KILLS") {
            case "KILLS_DESCENDING":
                entries.sort(Comparator.comparingInt(RankedEntry::kills).reversed()
                    .thenComparingInt(RankedEntry::deaths));
                break;
            case "SURVIVAL_THEN_COMBINED_KILLS":
            default:
                entries.sort(Comparator.comparing(RankedEntry::eliminated)
                    .thenComparing(Comparator.comparingInt(RankedEntry::kills).reversed())
                    .thenComparing(Comparator.comparingInt(RankedEntry::lives).reversed())
                    .thenComparingInt(RankedEntry::deaths));
                break;
        }

        for (int i = 0; i < entries.size(); i++) {
            entries.set(i, entries.get(i).withPosition(i + 1));
        }

        return entries;
    }

    public record RankedEntry(
        UUID playerId, String name, int kills, int deaths,
        int lives, boolean eliminated, int position
    ) {
        public RankedEntry(UUID playerId, String name, int kills, int deaths, int lives, boolean eliminated) {
            this(playerId, name, kills, deaths, lives, eliminated, 0);
        }

        public RankedEntry withPosition(int pos) {
            return new RankedEntry(playerId, name, kills, deaths, lives, eliminated, pos);
        }
    }
}
