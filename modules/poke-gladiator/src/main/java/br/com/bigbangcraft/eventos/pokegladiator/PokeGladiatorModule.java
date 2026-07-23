package br.com.bigbangcraft.eventos.pokegladiator;

import com.pedrodalben.bigbangeventos.api.module.BigBangEventModule;
import com.pedrodalben.bigbangeventos.api.module.EventModuleContext;

public class PokeGladiatorModule implements BigBangEventModule {

    private PokeGladiatorSessionService sessionService;

    @Override public String id() { return "poke_gladiator"; }
    @Override public String name() { return "BigBangEventos PokeGladiator"; }
    @Override public String version() { return "0.4.0-SNAPSHOT"; }
    @Override public int requiredApiVersion() { return 4; }

    @Override
    public void onEnable(EventModuleContext ctx) {
        ctx.eventTypes().register(new PokeGladiatorEventType(ctx));
        sessionService = new PokeGladiatorSessionService(ctx);
        sessionService.enable();

        PokeGladiatorCommandRegistrar.register(ctx);
        ctx.logger().info("PokeGladiator carregado. Tipo registrado: poke_gladiator");
    }

    @Override
    public void onDisable(EventModuleContext ctx) {
        if (sessionService != null) sessionService.cleanupAll();
        ctx.logger().info("PokeGladiator desabilitado.");
    }
}
