package com.pedrodalben.bigbangeventos.fabric;

import com.pedrodalben.bigbangeventos.BigBangEventos;
import com.pedrodalben.bigbangeventos.command.EventoCommand;
import com.pedrodalben.bigbangeventos.trigger.TriggerType;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.SignBlock;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FabricEvents {
    // ponytail: dedup by player+pos+tick, short window, no complex LRU
    private static final Map<String, Long> recentClicks = new ConcurrentHashMap<>();
    private static final long DEDUP_WINDOW_MS = 250;

    private FabricEvents() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || level.isClientSide())
                return InteractionResult.PASS;

            BlockPos pos = hit.getBlockPos();
            if (!(level.getBlockState(pos).getBlock() instanceof SignBlock))
                return InteractionResult.PASS;

            if (serverPlayer.getUsedItemHand() != net.minecraft.world.InteractionHand.MAIN_HAND)
                return InteractionResult.PASS;

            String dedupKey = serverPlayer.getUUID() + ";" + pos.getX() + ";" + pos.getY() + ";" + pos.getZ()
                    + ";" + serverPlayer.server.getTickCount();
            long now = System.currentTimeMillis();
            Long existing = recentClicks.putIfAbsent(dedupKey, now);
            if (existing != null && (now - existing) < DEDUP_WINDOW_MS) {
                return InteractionResult.PASS;
            }
            recentClicks.entrySet().removeIf(e -> now - e.getValue() > 5000);

            String binding = level.dimension().location()+";"+pos.getX()+";"+pos.getY()+";"+pos.getZ();

            if (EventoCommand.bindSelected(serverPlayer.getUUID(), binding)) {
                serverPlayer.sendSystemMessage(Component.literal("[Eventos] Placa vinculada ao gatilho."));
                return InteractionResult.SUCCESS;
            }

            BigBangEventos.engine().definitions().forEach(definition ->
                definition.triggers().stream()
                    .filter(t -> t.type() == TriggerType.SIGN_INTERACT && t.binding().orElse("").equals(binding))
                    .findFirst()
                    .ifPresent(t -> BigBangEventos.engine().activateTrigger(definition.id(), t.id(),
                            serverPlayer.getUUID(), serverPlayer.getGameProfile().getName(),
                            (id, permission) -> serverPlayer.hasPermissions(2),
                            (id, message) -> serverPlayer.sendSystemMessage(
                                    Component.literal("[Eventos] " + message))))
            );

            return InteractionResult.SUCCESS;
        });
    }
}
