package br.com.bigbangcraft.eventos.treasurehunt;

import com.pedrodalben.bigbangeventos.definition.*;
import com.pedrodalben.bigbangeventos.objective.*;
import com.pedrodalben.bigbangeventos.stage.*;
import com.pedrodalben.bigbangeventos.platform.StoredLocation;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TreasureHuntTest {
    @Test void validConfigurationPasses() {
        var d=definition(); assertTrue(new TreasureHuntValidator(new ObjectiveTypeRegistry()).validate(d).valid());
    }
    @Test void cycleFailsValidation() {
        var d=definition(); d.putStage(new EventStageDefinition("s2","S2","",2,true,true,0,List.of(),"s1",true,Map.of()));
        d.removeObjective("o1"); d.replaceStage(new EventStageDefinition("s1","S1","",1,true,true,0,List.of(),"s2",true,Map.of()));
        assertFalse(new TreasureHuntValidator(new ObjectiveTypeRegistry()).validate(d).valid());
    }
    @Test void scoreAndFinishDefaultsAreStable() { var d=definition(); assertEquals("SCORE_THEN_TIME",TreasureHuntConfiguration.rankingMode(d).name()); assertEquals("FIRST_FINISHER",TreasureHuntConfiguration.finishMode(d).name()); }
    private static EventDefinition definition(){var d=new EventDefinition("hunt","treasure_hunt","server");var l=new EventLocation("server","minecraft:overworld",0,64,0,0,0);d.location(LocationName.LOBBY,l);d.location(LocationName.ENTRANCE,l);d.location(LocationName.EXIT,l);d.putStage(new EventStageDefinition("s1","S1","",1,true,true,0,List.of("o1"),null,true,Map.of("clue","Find the tree")));d.putObjective(new ObjectiveDefinition("o1","O1","","trigger","s1",true,1,1,ObjectiveScope.PARTICIPANT,true,Map.of("points","20")));return d;}
}
