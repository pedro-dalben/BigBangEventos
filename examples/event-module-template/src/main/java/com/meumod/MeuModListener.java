package com.meumod;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;

public class MeuModListener {
    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (level.getBlockState(hitResult.getBlockPos()).is(Blocks.GOLD_BLOCK)) {
                player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("Você tocou num bloco de ouro!"));
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
    }
}
