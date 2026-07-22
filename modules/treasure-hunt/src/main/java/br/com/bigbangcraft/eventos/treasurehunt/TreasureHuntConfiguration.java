package br.com.bigbangcraft.eventos.treasurehunt;

import br.com.bigbangcraft.eventos.treasurehunt.model.*;
import com.pedrodalben.bigbangeventos.definition.EventDefinition;

public final class TreasureHuntConfiguration {
    private TreasureHuntConfiguration(){}
    public static TreasureHuntFinishMode finishMode(EventDefinition d){return enumValue(d,"treasure_hunt.finish-mode",TreasureHuntFinishMode.FIRST_FINISHER);}
    public static TreasureHuntRankingMode rankingMode(EventDefinition d){return enumValue(d,"treasure_hunt.ranking-mode",TreasureHuntRankingMode.SCORE_THEN_TIME);}
    public static long maxTimeSeconds(EventDefinition d){Object v=d.typeSettings().get("treasure_hunt.max-time-seconds");return v instanceof Number n?n.longValue():0;}
    public static void finishMode(EventDefinition d,TreasureHuntFinishMode v){d.typeSetting("treasure_hunt.finish-mode",v.name());}
    public static void rankingMode(EventDefinition d,TreasureHuntRankingMode v){d.typeSetting("treasure_hunt.ranking-mode",v.name());}
    public static void maxTimeSeconds(EventDefinition d,long v){if(v<0)throw new IllegalArgumentException("tempo máximo inválido");d.typeSetting("treasure_hunt.max-time-seconds",v);}
    private static <T extends Enum<T>> T enumValue(EventDefinition d,String key,T fallback){Object v=d.typeSettings().get(key);if(v==null)return fallback;try{return Enum.valueOf(fallback.getDeclaringClass(),v.toString());}catch(IllegalArgumentException e){return fallback;}}
}
