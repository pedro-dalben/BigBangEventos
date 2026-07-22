package br.com.bigbangcraft.eventos.gladiator;

import com.pedrodalben.bigbangeventos.api.module.BigBangEventModule;
import com.pedrodalben.bigbangeventos.api.module.EventModuleContext;

public final class GladiatorModule implements BigBangEventModule {
    @Override public String id() { return "gladiator"; }
    @Override public String name() { return "BigBangEventos Gladiator"; }
    @Override public String version() { return "0.3.0-SNAPSHOT"; }
    @Override public int requiredApiVersion() { return 3; }

    @Override
    public void onLoad(EventModuleContext ctx) {
        ctx.logger().info("GladiatorModule loaded.");
    }

    @Override
    public void onEnable(EventModuleContext ctx) {
        ctx.eventTypes().register(new GladiatorEventType());
        ctx.logger().info("GladiatorModule enabled.");
    }

    @Override
    public void onDisable(EventModuleContext ctx) {
        ctx.logger().info("GladiatorModule disabled.");
    }
}
