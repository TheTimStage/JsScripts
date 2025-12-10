package org.tts.jsscripts.systems;

import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

public final class ScriptAPI {

    private static MinecraftServer server;

    public static void init(MinecraftServer srv) {
        server = srv;

        ScriptEngine.init(server);
        registerEvents();

        System.out.println("[JsScripts] Script API loaded with extended events.");
    }

    private static void registerEvents() {
        ServerLifecycleEvents.SERVER_STARTED.register(s ->
                ScriptEngine.emit("serverStart")
        );

        ServerTickEvents.END_SERVER_TICK.register(s -> {

            ScriptEngine.emit("serverTick");
            for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
                ScriptEngine.emit("playerTick", p);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, srv2) ->
                ScriptEngine.emit("playerJoin", handler.player)
        );

        ServerPlayConnectionEvents.DISCONNECT.register((handler, srv2) ->
                ScriptEngine.emit("playerLeave", handler.player)
        );

        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) ->
                ScriptEngine.emit("chat",
                        message.getSignedContent(),
                        sender)
        );

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClient())
                ScriptEngine.emit("blockBreak", player, pos, state);
        });

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!world.isClient()) {
                ScriptEngine.emit("blockPlace",
                        player,
                        hit.getBlockPos(),
                        world.getBlockState(hit.getBlockPos()));
            }
            return ActionResult.PASS;
        });
    }
}
