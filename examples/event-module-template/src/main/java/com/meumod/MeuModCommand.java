package com.meumod;

import com.mojang.brigadier.CommandDispatcher;
import com.pedrodalben.bigbangeventos.BigBangEventos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class MeuModCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("meuevento")
            .executes(ctx -> {
                var engine = BigBangEventos.engine();
                ctx.getSource().sendSuccess(
                    () -> Component.literal("MeuEvento ativo: "
                        + engine.activeSessions().size()), false);
                return 1;
            }));
    }
}
