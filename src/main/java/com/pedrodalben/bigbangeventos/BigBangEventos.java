package com.pedrodalben.bigbangeventos;

import com.pedrodalben.bigbangeventos.api.BigBangEventosApi;
import com.pedrodalben.bigbangeventos.core.EventEngine;
import com.pedrodalben.bigbangeventos.fabric.*;
import com.pedrodalben.bigbangeventos.persistence.LocalEventStorage;
import net.minecraft.server.MinecraftServer;
import org.slf4j.*;
import java.io.InputStream;
import java.nio.file.*;
import java.time.Clock;

public final class BigBangEventos {
    private static final Logger LOGGER = LoggerFactory.getLogger(BigBangEventos.class);
    private static EventEngine engine;
    private static FabricScheduler scheduler;
    private BigBangEventos() {}

    public static synchronized void init(Path configDirectory, MinecraftServer server) {
        if (engine != null) return;
        copyDefault(configDirectory, "config.yml");
        copyDefault(configDirectory, "messages.yml");

        Clock clock = Clock.systemUTC();
        FabricPlayerService playerService = new FabricPlayerService(server);
        FabricTeleportService teleportService = new FabricTeleportService(server);
        FabricScheduler schedulerInstance = new FabricScheduler(server);
        FabricSnapshotGateway snapshotGateway = new FabricSnapshotGateway(server);
        LocalEventStorage storage = new LocalEventStorage(configDirectory);

        scheduler = schedulerInstance;
        engine = new EventEngine(storage, clock, snapshotGateway, teleportService,
                playerService, schedulerInstance);

        LOGGER.info("BigBangEventos iniciado — runtime Fabric ativo");
    }

    private static void copyDefault(Path directory, String file) {
        Path target = directory.resolve(file);
        if (Files.exists(target)) return;
        try {
            Files.createDirectories(directory);
            try (InputStream resource = BigBangEventos.class.getResourceAsStream("/" + file)) {
                if (resource != null) Files.copy(resource, target);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível criar configuração padrão", e);
        }
    }

    public static EventEngine engine() {
        if (engine == null) throw new IllegalStateException("BigBangEventos ainda não iniciou");
        return engine;
    }

    public static FabricScheduler scheduler() {
        return scheduler;
    }

    public static BigBangEventosApi api() {
        return new BigBangEventosApi(engine());
    }
}
