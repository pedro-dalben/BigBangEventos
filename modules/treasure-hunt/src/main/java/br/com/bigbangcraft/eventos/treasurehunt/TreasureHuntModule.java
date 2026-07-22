package br.com.bigbangcraft.eventos.treasurehunt;

import com.pedrodalben.bigbangeventos.api.module.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import java.io.IOException;
import java.nio.file.Files;

public final class TreasureHuntModule implements BigBangEventModule {
    private TreasureHuntSessionService sessions; private EventModuleContext context;
    public String id(){return "treasure_hunt";} public String name(){return "BigBangEventos Treasure Hunt";} public String version(){return "0.3.0-SNAPSHOT";} public int requiredApiVersion(){return 2;}
    public void onLoad(EventModuleContext ctx){context=ctx;try{Files.createDirectories(ctx.configDirectory());}catch(IOException e){ctx.logger().warn("Não foi possível criar configuração: {}",e.getMessage());}}
    public void onEnable(EventModuleContext ctx){sessions=new TreasureHuntSessionService(ctx.api(),ctx.players(),ctx.logger(),ctx.events());ctx.eventTypes().register(new TreasureHuntEventType(new TreasureHuntValidator(ctx.objectives()),sessions));registerCommands();ctx.logger().info("Treasure Hunt module loaded");}
    public void onDisable(EventModuleContext ctx){if(sessions!=null)sessions.shutdown();ctx.events().disable(id());}
    private void registerCommands(){Object game=FabricLoader.getInstance().getGameInstance();if(game instanceof MinecraftServer server&&server.getCommands()!=null)TreasureHuntCommandRegistrar.register(server.getCommands().getDispatcher(),sessions);}
}
