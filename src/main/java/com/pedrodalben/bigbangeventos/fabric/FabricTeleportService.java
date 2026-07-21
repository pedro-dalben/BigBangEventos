package com.pedrodalben.bigbangeventos.fabric;

import com.pedrodalben.bigbangeventos.api.OperationResult;
import com.pedrodalben.bigbangeventos.platform.PlatformTeleportService;
import com.pedrodalben.bigbangeventos.platform.StoredLocation;
import net.minecraft.ResourceLocationException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public final class FabricTeleportService implements PlatformTeleportService {
    private static final Logger LOG = LoggerFactory.getLogger(FabricTeleportService.class);
    private final MinecraftServer server;

    public FabricTeleportService(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public OperationResult teleport(UUID playerId, StoredLocation destination) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null)
            return OperationResult.fail("player_offline", "Jogador não está online");

        if (!Double.isFinite(destination.x()) || !Double.isFinite(destination.y())
                || !Double.isFinite(destination.z()))
            return OperationResult.fail("invalid_location", "Coordenadas inválidas");

        ServerLevel world;
        try {
            ResourceLocation dimId = ResourceLocation.parse(destination.dimension());
            world = server.getLevel(server.registryAccess().registryOrThrow(Registries.DIMENSION).getHolderOrThrow(
                    net.minecraft.resources.ResourceKey.create(Registries.DIMENSION, dimId)).key());
        } catch (Exception e) {
            LOG.warn("Dimensão não encontrada: {}", destination.dimension());
            return OperationResult.fail("world_not_found", "Mundo " + destination.dimension() + " não encontrado");
        }

        if (world == null)
            return OperationResult.fail("world_not_found", "Mundo " + destination.dimension() + " indisponível");

        try {
            player.teleportTo(world, destination.x(), destination.y(), destination.z(),
                    destination.yaw(), destination.pitch());
            return OperationResult.ok("Teleporte realizado");
        } catch (Exception e) {
            LOG.error("Teleporte falhou para {}: {}", playerId, e.getMessage());
            return OperationResult.fail("teleport_failed", "Não foi possível teleportar");
        }
    }
}
