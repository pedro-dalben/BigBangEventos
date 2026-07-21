package com.meumod;

import com.pedrodalben.bigbangeventos.definition.EventDefinition;
import com.pedrodalben.bigbangeventos.definition.LocationName;
import com.pedrodalben.bigbangeventos.definition.EventLocation;
import com.pedrodalben.bigbangeventos.engine.EventEngine;
import com.pedrodalben.bigbangeventos.session.SessionState;
import com.pedrodalben.bigbangeventos.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MeuModTest {
    private EventEngine engine;

    @BeforeEach
    void setUp() {
        engine = TestStubs.createEngine();
        engine.types().register(new MeuTipoEvento());
    }

    @Test
    void criarEventoValidaConfig() {
        assertTrue(engine.create("test", "meu_tipo", "server").success());
        EventDefinition d = engine.definition("test").orElseThrow();
        d.location(LocationName.LOBBY, loc());
        d.location(LocationName.ENTRANCE, loc());
        d.location(LocationName.EXIT, loc());
        d.typeSetting("pontos", 100);
        engine.save(d);

        ValidationResult vr = engine.validator().validate(d);
        assertTrue(vr.valid(), vr.issues().toString());
    }

    @Test
    void eventoRequerPontos() {
        assertTrue(engine.create("test", "meu_tipo", "server").success());
        EventDefinition d = engine.definition("test").orElseThrow();
        d.location(LocationName.LOBBY, loc());
        d.location(LocationName.ENTRANCE, loc());
        d.location(LocationName.EXIT, loc());
        engine.save(d);

        ValidationResult vr = engine.validator().validate(d);
        assertFalse(vr.valid());
    }

    @Test
    void jogadorEntraEInicia() {
        assertTrue(engine.create("test", "meu_tipo", "server").success());
        EventDefinition d = engine.definition("test").orElseThrow();
        d.location(LocationName.LOBBY, loc());
        d.location(LocationName.ENTRANCE, loc());
        d.location(LocationName.EXIT, loc());
        d.typeSetting("pontos", 100);
        engine.save(d);

        assertTrue(engine.open("test", null).success());
        assertTrue(engine.join("test", java.util.UUID.randomUUID(), "player", false, true).success());
        assertTrue(engine.start("test").success());

        var session = engine.activeSession("test");
        assertTrue(session.isPresent());
        assertEquals(SessionState.RUNNING, session.get().state());
    }

    private static EventLocation loc() {
        return new EventLocation("minecraft:overworld", 0, 64, 0, 0, 0);
    }
}
