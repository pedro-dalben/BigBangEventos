package br.com.bigbangcraft.eventos.gladiator;

import com.pedrodalben.bigbangeventos.participant.EventParticipant;
import com.pedrodalben.bigbangeventos.participant.combat.ParticipantCombatState;
import com.pedrodalben.bigbangeventos.session.EventSession;
import java.util.*;

public final class GladiatorRankingService {

    public List<RankedEntry> rank(EventSession session, String strategy) {
        List<RankedEntry> entries = new ArrayList<>();
        for (EventParticipant p : session.participants()) {
            ParticipantCombatState cs = session.combatState(p.playerId()).orElse(null);
            int kills = cs != null ? cs.sessionKills() : 0;
            int deaths = cs != null ? cs.sessionDeaths() : 0;
            int lives = cs != null ? cs.livesRemaining() : 0;
            boolean eliminated = cs != null && cs.eliminated();
            entries.add(new RankedEntry(p.playerId(), p.knownName(), p.score(), kills, deaths, lives, eliminated));
        }

        Comparator<RankedEntry> cmp = switch (strategy) {
            case "SURVIVAL_ORDER" -> Comparator.comparing(RankedEntry::eliminated)
                    .thenComparingInt(RankedEntry::score).reversed();
            case "KILLS_DESCENDING" -> Comparator.comparingInt(RankedEntry::kills).reversed()
                    .thenComparingInt(RankedEntry::deaths);
            case "SCORE_DESCENDING" -> Comparator.comparingInt(RankedEntry::score).reversed()
                    .thenComparingInt(RankedEntry::kills).reversed();
            default -> Comparator.comparing(RankedEntry::eliminated)
                    .thenComparingInt(RankedEntry::kills).reversed()
                    .thenComparingInt(RankedEntry::lives).reversed()
                    .thenComparingInt(RankedEntry::deaths);
        };

        entries.sort(cmp);
        for (int i = 0; i < entries.size(); i++) {
            entries.set(i, new RankedEntry(entries.get(i).playerId(), entries.get(i).name(),
                    entries.get(i).score(), entries.get(i).kills(), entries.get(i).deaths(),
                    entries.get(i).lives(), entries.get(i).eliminated(), i + 1));
        }
        return entries;
    }

    public record RankedEntry(UUID playerId, String name, int score, int kills, int deaths, int lives, boolean eliminated, int position) {
        public RankedEntry(UUID playerId, String name, int score, int kills, int deaths, int lives, boolean eliminated) {
            this(playerId, name, score, kills, deaths, lives, eliminated, 0);
        }
    }
}
