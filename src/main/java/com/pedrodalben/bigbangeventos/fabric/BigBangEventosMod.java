package com.pedrodalben.bigbangeventos.fabric;

import com.pedrodalben.bigbangeventos.BigBangEventos;
import com.pedrodalben.bigbangeventos.command.EventoCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;

public final class BigBangEventosMod implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
        ServerTickEvents.START_SERVER_TICK.register(this::onServerTick);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            BigBangEventos.engine().onPlayerReconnect(handler.player.getUUID());
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            BigBangEventos.engine().onPlayerDisconnect(handler.player.getUUID());
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                EventoCommand.register(dispatcher));
        FabricEvents.register();
    }

    private void onServerStarting(MinecraftServer server) {
        BigBangEventos.init(net.fabricmc.loader.api.FabricLoader.getInstance()
                .getConfigDir().resolve("bigbangeventos"), server);
    }

    private void onServerStarted(MinecraftServer server) {
        BigBangEventos.engine().recoverOnStartup();
    }

    private void onServerStopping(MinecraftServer server) {
        try {
            BigBangEventos.engine().onServerShutdown();
        } catch (Exception e) {
            BigBangEventos.engine().players().sendMessage(null, ""); // no-op
        }
    }

    private void onServerTick(MinecraftServer server) {
        FabricScheduler sched = BigBangEventos.scheduler();
        if (sched != null) sched.onTick();
    }
}
