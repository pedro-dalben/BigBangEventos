package com.pedrodalben.bigbangeventos.ranking;
import com.pedrodalben.bigbangeventos.participant.EventParticipant; import com.pedrodalben.bigbangeventos.session.EventSession; import java.util.List;
public interface RankingStrategy { List<EventParticipant> rank(EventSession session); }
