package org.tts.jsscripts.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.tts.jsscripts.networking.JsSNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class JsSCommand {

    public static void init() {
        CommandRegistrationCallback.EVENT.register(JsSCommand::register);
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                 CommandRegistryAccess registryAccess,
                                 CommandManager.RegistrationEnvironment env) {

        dispatcher.register(
                CommandManager.literal("jsscripts")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.literal("gui")
                                .executes(ctx -> {

                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) {
                                        ctx.getSource().sendFeedback(
                                                () -> Text.literal("Only players can use this."),
                                                false
                                        );
                                        return 0;
                                    }

                                    // отправляем пакет открытия меню
                                    ServerPlayNetworking.send(player, new JsSNetworking.OpenMenu(""));

                                    return 1;
                                }))
        );
    }
}
