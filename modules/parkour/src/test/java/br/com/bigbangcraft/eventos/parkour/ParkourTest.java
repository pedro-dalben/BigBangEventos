package br.com.bigbangcraft.eventos.parkour;

import br.com.bigbangcraft.eventos.parkour.model.*;
import com.pedrodalben.bigbangeventos.definition.EventDefinition;
import com.pedrodalben.bigbangeventos.definition.EventLocation;
import com.pedrodalben.bigbangeventos.definition.LocationName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ParkourTest {

    // --- Configuration ---

    @Test void validConfiguration() {
        EventDefinition def = createParkourDef();
        var result = new ParkourValidator().validate(def);
        assertTrue(result.valid(), result.issues().toString());
    }

    @Test void missingStartFails() {
        EventDefinition def = createParkourDefNoStart();
        var result = new ParkourValidator().validate(def);
        assertFalse(result.valid());
    }

    @Test void missingFinishFails() {
        EventDefinition def = createParkourDefNoFinish();
        var result = new ParkourValidator().validate(def);
        assertFalse(result.valid());
    }

    @Test void negativeMaxTimeFails() {
        EventDefinition def = createParkourDef();
        ParkourConfiguration.setMaxTimeSeconds(def, -1);
        var result = new ParkourValidator().validate(def);
        assertFalse(result.valid());
    }

    @Test void negativeMaxAttemptsFails() {
        EventDefinition def = createParkourDef();
        ParkourConfiguration.setMaxAttempts(def, -1);
        var result = new ParkourValidator().validate(def);
        assertFalse(result.valid());
    }

    // --- Checkpoints ---

    @Test void firstCheckpointIsExpected() {
        var ps = new ParkourParticipantService();
        var data = ps.getOrCreate("s1", "p1");
        assertNull(data.currentCheckpointId());
    }

    @Test void correctOrderCanBeTracked() {
        var data = new ParkourParticipantData();
        assertTrue(data.addCompletedCheckpoint("cp0"));
        assertTrue(data.addCompletedCheckpoint("cp1"));
        assertTrue(data.addCompletedCheckpoint("cp2"));
        assertEquals(3, data.completedCount());
    }

    @Test void wrongOrderIsHandled() {
        var data = new ParkourParticipantData();
        data.addCompletedCheckpoint("cp0");
        assertFalse(data.hasCompletedCheckpoint("cp2"));
    }

    @Test void repeatedCheckpointFails() {
        var data = new ParkourParticipantData();
        assertTrue(data.addCompletedCheckpoint("cp0"));
        assertFalse(data.addCompletedCheckpoint("cp0"));
    }

    @Test void allCheckpointsCompleted() {
        var data = new ParkourParticipantData();
        data.addCompletedCheckpoint("cp0");
        data.addCompletedCheckpoint("cp1");
        data.addCompletedCheckpoint("cp2");
        assertTrue(data.hasCompletedCheckpoint("cp0"));
        assertTrue(data.hasCompletedCheckpoint("cp1"));
        assertTrue(data.hasCompletedCheckpoint("cp2"));
        assertEquals(3, data.completedCount());
    }

    // --- Checkpoint model ---

    @Test void checkpointContainsLocation() {
        var loc = new EventLocation("test", "minecraft:overworld", 10, 64, 20, 0, 0);
        var cp = new ParkourCheckpoint("cp1", 0, loc, 2.0);
        assertTrue(cp.contains("test", "minecraft:overworld", 10, 64, 20));
        assertTrue(cp.contains("test", "minecraft:overworld", 11, 64, 20));
        assertFalse(cp.contains("test", "minecraft:overworld", 20, 64, 20));
    }

    @Test void checkpointRejectsInvalidRadius() {
        var loc = new EventLocation("test", "minecraft:overworld", 0, 64, 0, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> new ParkourCheckpoint("bad", 0, loc, 0));
        assertThrows(IllegalArgumentException.class, () -> new ParkourCheckpoint("bad2", 0, loc, -1));
    }

    // --- Timer ---

    @Test void timerFormat() {
        assertEquals("00:00.000", ParkourTimerService.format(0));
        assertEquals("00:01.000", ParkourTimerService.format(1000));
        assertEquals("01:00.000", ParkourTimerService.format(60000));
        assertEquals("01:42.531", ParkourTimerService.format(102531));
        assertEquals("00:00.500", ParkourTimerService.format(500));
    }

    @Test void timerStartAndStop() {
        var ts = new ParkourTimerService();
        ts.start("s1", "p1");
        assertTrue(ts.getElapsedMillis("s1", "p1") >= 0);
        ts.stop("s1", "p1");
    }

    @Test void timerFormatEdgeCases() {
        assertEquals("00:00.000", ParkourTimerService.format(0));
        assertEquals("00:00.001", ParkourTimerService.format(1));
        assertEquals("59:59.999", ParkourTimerService.format(3599999));
    }

    // --- Fall ---

    @Test void fallIncrementsCounter() {
        var data = new ParkourParticipantData();
        assertEquals(0, data.falls());
        data.incrementFalls();
        assertEquals(1, data.falls());
        data.incrementFalls();
        assertEquals(2, data.falls());
    }

    @Test void fallResetToStartByDefault() {
        var def = createParkourDef();
        assertEquals(ParkourResetMode.START, ParkourConfiguration.getResetMode(def));
    }

    @Test void attemptsTracked() {
        var data = new ParkourParticipantData();
        assertEquals(0, data.attempts());
        data.incrementAttempts();
        assertEquals(1, data.attempts());
    }

    @Test void resetInProgressFlag() {
        var data = new ParkourParticipantData();
        assertFalse(data.resetInProgress());
        data.resetInProgress(true);
        assertTrue(data.resetInProgress());
        data.resetInProgress(false);
        assertFalse(data.resetInProgress());
    }

    // --- Completion ---

    @Test void idempotentCompletion() {
        var ts = new ParkourTimerService();
        ts.start("s1", "p1");
        var ps = new ParkourParticipantService();
        var data = ps.getOrCreate("s1", "p1");
        data.startedAt(java.time.Instant.now());
        data.addCompletedCheckpoint("cp0");
        data.addCompletedCheckpoint("cp1");
        data.finishedAt(java.time.Instant.now());
        assertNotNull(data.finishedAt());
    }

    // --- Ranking ---

    @Test void timerFormatMilliseconds() {
        assertEquals("00:00.001", ParkourTimerService.format(1));
        assertEquals("00:00.010", ParkourTimerService.format(10));
        assertEquals("00:00.100", ParkourTimerService.format(100));
    }

    // --- Participant Data ---

    @Test void participantDataTracksCheckpoints() {
        var data = new ParkourParticipantData();
        assertFalse(data.hasCompletedCheckpoint("cp0"));
        data.addCompletedCheckpoint("cp0");
        assertTrue(data.hasCompletedCheckpoint("cp0"));
        assertEquals(1, data.completedCount());
    }

    @Test void participantDataClearsCheckpoints() {
        var data = new ParkourParticipantData();
        data.addCompletedCheckpoint("cp0");
        data.clearCompletedCheckpoints();
        assertEquals(0, data.completedCount());
    }

    @Test void participantDataCurrentCheckpoint() {
        var data = new ParkourParticipantData();
        assertNull(data.currentCheckpointId());
        data.currentCheckpointId("cp0");
        assertEquals("cp0", data.currentCheckpointId());
    }

    @Test void participantDataEquality() {
        var a = new ParkourParticipantData();
        var b = new ParkourParticipantData();
        assertEquals(a, b);
        a.addCompletedCheckpoint("cp0");
        assertNotEquals(a, b);
    }

    // --- Participant Service ---

    @Test void participantServiceGetOrCreate() {
        var ps = new ParkourParticipantService();
        var d1 = ps.getOrCreate("s1", "p1");
        var d2 = ps.getOrCreate("s1", "p1");
        assertSame(d1, d2);
    }

    @Test void participantServiceClearSession() {
        var ps = new ParkourParticipantService();
        ps.getOrCreate("s1", "p1");
        ps.getOrCreate("s1", "p2");
        ps.clearSession("s1");
        assertTrue(ps.get("s1", "p1").isEmpty());
        assertTrue(ps.get("s1", "p2").isEmpty());
    }

    // --- Configuration getters/setters ---

    @Test void configFallYLevel() {
        var def = createParkourDef();
        assertEquals(40, ParkourConfiguration.getFallYLevel(def), 0.001);
        ParkourConfiguration.setFallYLevel(def, 20);
        assertEquals(20, ParkourConfiguration.getFallYLevel(def), 0.001);
    }

    @Test void configFinishRadius() {
        var def = createParkourDef();
        assertEquals(2.0, ParkourConfiguration.getFinishRadius(def), 0.001);
        ParkourConfiguration.setFinishRadius(def, 5.0);
        assertEquals(5.0, ParkourConfiguration.getFinishRadius(def), 0.001);
    }

    @Test void configMaxTime() {
        var def = createParkourDef();
        ParkourConfiguration.setMaxTimeSeconds(def, 300);
        assertEquals(300, ParkourConfiguration.getMaxTimeSeconds(def));
    }

    @Test void configMaxAttempts() {
        var def = createParkourDef();
        ParkourConfiguration.setMaxAttempts(def, 5);
        assertEquals(5, ParkourConfiguration.getMaxAttempts(def));
    }

    @Test void configFinishMode() {
        var def = createParkourDef();
        assertEquals(ParkourFinishMode.ALL_FINISHERS, ParkourConfiguration.getFinishMode(def));
        ParkourConfiguration.setFinishMode(def, ParkourFinishMode.FIRST_FINISHER);
        assertEquals(ParkourFinishMode.FIRST_FINISHER, ParkourConfiguration.getFinishMode(def));
    }

    @Test void configResetMode() {
        var def = createParkourDef();
        assertEquals(ParkourResetMode.START, ParkourConfiguration.getResetMode(def));
        ParkourConfiguration.setResetMode(def, ParkourResetMode.LAST_CHECKPOINT);
        assertEquals(ParkourResetMode.LAST_CHECKPOINT, ParkourConfiguration.getResetMode(def));
    }

    @Test void configCheckpointsRequired() {
        var def = createParkourDef();
        assertTrue(ParkourConfiguration.isCheckpointsRequired(def));
        ParkourConfiguration.setCheckpointsRequired(def, false);
        assertFalse(ParkourConfiguration.isCheckpointsRequired(def));
    }

    @Test void configCompleteDestination() {
        var def = createParkourDef();
        assertEquals(ParkourCompleteDestination.EXIT, ParkourConfiguration.getCompleteDestination(def));
        ParkourConfiguration.setCompleteDestination(def, ParkourCompleteDestination.ORIGINAL);
        assertEquals(ParkourCompleteDestination.ORIGINAL, ParkourConfiguration.getCompleteDestination(def));
    }

    // --- Helpers ---

    private static EventDefinition createParkourDef() {
        var def = new EventDefinition("parkour_test", "parkour", "test");
        def.location(LocationName.LOBBY, new EventLocation("test", "minecraft:overworld", 0, 64, 0, 0, 0));
        def.location(LocationName.ENTRANCE, new EventLocation("test", "minecraft:overworld", 0, 65, 0, 0, 0));
        def.location(LocationName.EXIT, new EventLocation("test", "minecraft:overworld", 100, 64, 100, 0, 0));
        ParkourConfiguration.setStartLocation(def, new EventLocation("test", "minecraft:overworld", 0, 64, 0, 0, 0));
        ParkourConfiguration.setFinishLocation(def, new EventLocation("test", "minecraft:overworld", 100, 64, 100, 0, 0));
        ParkourConfiguration.setFinishRadius(def, 2.0);
        ParkourConfiguration.setFallYLevel(def, 40);
        return def;
    }

    private static EventDefinition createParkourDefNoStart() {
        var def = new EventDefinition("parkour_test", "parkour", "test");
        def.location(LocationName.ENTRANCE, new EventLocation("test", "minecraft:overworld", 0, 65, 0, 0, 0));
        ParkourConfiguration.setFinishLocation(def, new EventLocation("test", "minecraft:overworld", 100, 64, 100, 0, 0));
        ParkourConfiguration.setFinishRadius(def, 2.0);
        ParkourConfiguration.setFallYLevel(def, 40);
        return def;
    }

    private static EventDefinition createParkourDefNoFinish() {
        var def = new EventDefinition("parkour_test", "parkour", "test");
        def.location(LocationName.LOBBY, new EventLocation("test", "minecraft:overworld", 0, 64, 0, 0, 0));
        def.location(LocationName.ENTRANCE, new EventLocation("test", "minecraft:overworld", 0, 65, 0, 0, 0));
        ParkourConfiguration.setStartLocation(def, new EventLocation("test", "minecraft:overworld", 0, 64, 0, 0, 0));
        ParkourConfiguration.setFallYLevel(def, 40);
        return def;
    }
}
