package br.com.bigbangcraft.eventos.gladiator;

import com.pedrodalben.bigbangeventos.BigBangEventos;
import com.pedrodalben.bigbangeventos.api.module.BigBangEventModule;
import com.pedrodalben.bigbangeventos.api.module.EventModuleContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

public final class GladiatorModule implements BigBangEventModule {
    private GladiatorSessionService sessionService;
    private EventModuleContext context;

    @Override public String id() { return "gladiator"; }
    @Override public String name() { return "BigBangEventos Gladiator"; }
    @Override public String version() { return "0.3.0-SNAPSHOT"; }
    @Override public int requiredApiVersion() { return 3; }

    @Override
    public void onLoad(EventModuleContext ctx) {
        this.context = ctx;
        ctx.logger().info("GladiatorModule loaded.");
    }

    @Override
    public void onEnable(EventModuleContext ctx) {
        var engine = BigBangEventos.engine();
        var ranking = new GladiatorRankingService();
        var completion = new GladiatorCompletionService();
        var validator = new GladiatorValidator();
        var server = getServer();

        sessionService = new GladiatorSessionService(engine, ctx.events(), server,
                ranking, completion, validator);
        sessionService.enable();

        ctx.eventTypes().register(new GladiatorEventType(sessionService, validator));

        if (server != null && server.getCommands() != null) {
            GladiatorCommandRegistrar.register(server.getCommands().getDispatcher());
        }

        ctx.logger().info("GladiatorModule enabled.");
    }

    @Override
    public void onDisable(EventModuleContext ctx) {
        if (sessionService != null) {
            sessionService.disable();
        }
        ctx.logger().info("GladiatorModule disabled.");
    }

    private MinecraftServer getServer() {
        Object instance = FabricLoader.getInstance().getGameInstance();
        return instance instanceof MinecraftServer s ? s : null;
    }
}
