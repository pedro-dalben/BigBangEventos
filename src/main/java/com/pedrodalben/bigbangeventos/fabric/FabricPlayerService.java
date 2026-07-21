package com.pedrodalben.bigbangeventos.fabric;

import com.pedrodalben.bigbangeventos.api.OperationResult;
import com.pedrodalben.bigbangeventos.platform.PlatformPlayerService;
import com.pedrodalben.bigbangeventos.platform.StoredLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.players.PlayerList;

import java.util.*;

public final class FabricPlayerService implements PlatformPlayerService {
    private final MinecraftServer server;

    public FabricPlayerService(MinecraftServer server) {
        this.server = server;
    }

    private Optional<ServerPlayer> resolve(UUID playerId) {
        PlayerList list = server.getPlayerList();
        ServerPlayer p = list.getPlayer(playerId);
        return Optional.ofNullable(p);
    }

    @Override
    public Optional<UUID> findOnlineUuidByName(String playerName) {
        ServerPlayer p = server.getPlayerList().getPlayerByName(playerName);
        return p != null ? Optional.of(p.getUUID()) : Optional.empty();
    }

    @Override
    public boolean isOnline(UUID playerId) {
        return resolve(playerId).isPresent();
    }

    @Override
    public OperationResult sendMessage(UUID playerId, String message) {
        ServerPlayer p = resolve(playerId).orElse(null);
        if (p == null) return OperationResult.fail("player_offline", "Jogador não está online");
        p.sendSystemMessage(Component.literal(message));
        return OperationResult.ok("Mensagem enviada");
    }

    @Override
    public OperationResult sendTitle(UUID playerId, String title, String subtitle) {
        ServerPlayer p = resolve(playerId).orElse(null);
        if (p == null) return OperationResult.fail("player_offline", "Jogador não está online");
        if (title != null && !title.isEmpty()) {
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title)));
        }
        if (subtitle != null && !subtitle.isEmpty()) {
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle)));
        }
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        return OperationResult.ok("Título enviado");
    }

    @Override
    public OperationResult sendActionBar(UUID playerId, String message) {
        ServerPlayer p = resolve(playerId).orElse(null);
        if (p == null) return OperationResult.fail("player_offline", "Jogador não está online");
        p.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(message)));
        return OperationResult.ok("Action bar enviada");
    }

    @Override
    public Optional<StoredLocation> captureLocation(UUID playerId) {
        return resolve(playerId).map(p -> new StoredLocation(
                "cobbleverse",
                p.level().dimension().location().toString(),
                p.getX(), p.getY(), p.getZ(),
                p.getYRot(), p.getXRot()
        ));
    }

    @Override
    public Optional<String> captureGameMode(UUID playerId) {
        return resolve(playerId).map(p -> p.gameMode.getGameModeForPlayer().name());
    }

    @Override
    public boolean hasFlyEnabled(UUID playerId) {
        return resolve(playerId).map(p -> p.getAbilities().mayfly).orElse(false);
    }
}
