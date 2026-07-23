package br.com.bigbangcraft.eventos.fightorflight;

import com.pedrodalben.bigbangeventos.api.combat.*;
import com.pedrodalben.bigbangeventos.api.module.BigBangEventModule;
import com.pedrodalben.bigbangeventos.api.module.EventModuleContext;

public class FightOrFlightIntegrationModule implements BigBangEventModule {
    private FightOrFlightCombatProvider provider;

    @Override public String id() { return "fight_or_flight_integration"; }
    @Override public String name() { return "BigBangEventos Fight or Flight Integration"; }
    @Override public String version() { return "0.4.0-SNAPSHOT"; }
    @Override public int requiredApiVersion() { return 4; }

    @Override
    public void onEnable(EventModuleContext ctx) {
        try {
            Class.forName("me.rufia.fightorflight.CobblemonFightOrFlight");
        } catch (ClassNotFoundException e) {
            ctx.logger().warn("Fight or Flight nao detectado. Integracao desabilitada.");
            return;
        }

        provider = new FightOrFlightCombatProvider(ctx.events());
        ctx.api().combatProviders().register(provider);
        ctx.logger().info("FightOrFlightCombatProvider registrado.");
    }

    @Override
    public void onDisable(EventModuleContext ctx) {
        if (provider != null) provider.cleanupAll();
        ctx.logger().info("FightOrFlightIntegration desabilitado.");
    }
}
