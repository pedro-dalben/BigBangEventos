package br.com.bigbangcraft.eventos.treasurehunt.data;

import com.pedrodalben.bigbangeventos.data.*;
import java.util.Set;

public final class TreasureHuntDataKeys {
    private TreasureHuntDataKeys() {}
    public static final DataKey<Integer> FRAGMENTS = new DataKey<>("treasure_hunt","fragments",DataCodecs.INTEGER,0);
    public static final DataKey<Long> SCORE = new DataKey<>("treasure_hunt","score",DataCodecs.LONG,0L);
    public static final DataKey<String> CURRENT_CLUE = new DataKey<>("treasure_hunt","current_clue",DataCodecs.STRING,"");
    public static final DataKey<Set<String>> DISCOVERED_CLUES = new DataKey<>("treasure_hunt","discovered_clues",DataCodecs.STRING_SET,Set.of());
    public static final DataKey<Set<String>> COMPLETED_STAGES = new DataKey<>("treasure_hunt","completed_stages",DataCodecs.STRING_SET,Set.of());
    public static final DataKey<Boolean> COMPLETION_REWARDED = new DataKey<>("treasure_hunt","completion_rewarded",DataCodecs.BOOLEAN,false);
}
