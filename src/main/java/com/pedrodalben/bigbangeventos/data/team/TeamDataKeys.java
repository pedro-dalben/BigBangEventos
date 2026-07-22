package com.pedrodalben.bigbangeventos.data.team;

import com.pedrodalben.bigbangeventos.data.DataCodecs;
import com.pedrodalben.bigbangeventos.data.DataKey;

public final class TeamDataKeys {
    private TeamDataKeys() {}
    public static final DataKey<Integer> SCORE = new DataKey<>("core", "team_score", DataCodecs.INTEGER, 0);
    public static final DataKey<Integer> KILLS = new DataKey<>("core", "team_kills", DataCodecs.INTEGER, 0);
    public static final DataKey<Integer> DEATHS = new DataKey<>("core", "team_deaths", DataCodecs.INTEGER, 0);
    public static final DataKey<Integer> ROUND_WINS = new DataKey<>("core", "team_round_wins", DataCodecs.INTEGER, 0);
}
