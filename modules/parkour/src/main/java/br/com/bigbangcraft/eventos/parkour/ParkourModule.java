package br.com.bigbangcraft.eventos.parkour;

import com.pedrodalben.bigbangeventos.api.module.BigBangEventModule;
import com.pedrodalben.bigbangeventos.api.module.EventModuleContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Files;

public final class ParkourModule implements BigBangEventModule {

    private ParkourSessionService sessionService;
    private EventModuleContext context;

    @Override
    public String id() {
        return "parkour";
    }

    @Override
    public String name() {
        return "Parkour";
    }

    @Override
    public String version() {
        return "0.3.0-SNAPSHOT";
    }

    @Override
    public int requiredApiVersion() {
        return 1;
    }

    @Override
    public void onLoad(EventModuleContext ctx) {
        this.context = ctx;
        try {
            Files.createDirectories(ctx.configDirectory());
        } catch (IOException e) {
            ctx.logger().warn("Nao foi possivel criar diretorio de configuracao: {}", e.getMessage());
        }
        ctx.logger().info("ParkourModule carregado.");
    }

    @Override
    public void onEnable(EventModuleContext ctx) {
        var api = ctx.api();
        var scheduler = ctx.scheduler();
        var players = ctx.players();
        var teleport = ctx.teleport();
        var logger = ctx.logger();

        var participantService = new ParkourParticipantService();
        var timerService = new ParkourTimerService();
        var checkpointService = new ParkourCheckpointService(api, participantService);
        var rankingService = new ParkourRankingService(participantService, timerService);
        var fallService = new ParkourFallService(api, players, teleport, participantService, checkpointService);
        var completionService = new ParkourCompletionService(api, participantService, timerService,
                rankingService, checkpointService, teleport);
        var validator = new ParkourValidator();

        sessionService = new ParkourSessionService(api, scheduler, players, teleport, logger,
                participantService, checkpointService, fallService, timerService,
                rankingService, completionService, validator);

        var eventType = new ParkourEventType(sessionService, validator);
        ctx.eventTypes().register(eventType);

        ParkourCommandRegistrar.init(api, sessionService);

        registerCommands();

        logger.info("ParkourModule habilitado.");
    }

    @Override
    public void onDisable(EventModuleContext ctx) {
        if (sessionService != null) {
            sessionService.shutdown();
        }
        ctx.logger().info("ParkourModule desabilitado.");
    }

    private void registerCommands() {
        if (context == null) return;
        Object gameInstance = FabricLoader.getInstance().getGameInstance();
        if (gameInstance instanceof MinecraftServer server && server.getCommands() != null) {
            var dispatcher = server.getCommands().getDispatcher();
            ParkourCommandRegistrar.register(dispatcher);
        }
    }
}
