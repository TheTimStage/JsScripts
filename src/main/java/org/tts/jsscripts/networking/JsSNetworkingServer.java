package org.tts.jsscripts.networking;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.tts.jsscripts.systems.ScriptAPI;
import org.tts.jsscripts.systems.ScriptManager;
import org.tts.jsscripts.util.JsSStorage;

import java.util.List;

public final class JsSNetworkingServer {
    private static MinecraftServer server;

    public static void registerReceivers() {
        JsSStorage.init();
        ServerPlayNetworking.registerGlobalReceiver(
                JsSNetworking.RequestList.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    sendList(ctx.player(), payload.directory());
                })
        );

        ServerPlayNetworking.registerGlobalReceiver(
                JsSNetworking.SaveScript.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    JsSStorage.saveScript(payload.path(), payload.code());
                    sendList(ctx.player(), extractDir(payload.path()));
                })
        );

        ServerPlayNetworking.registerGlobalReceiver(
                JsSNetworking.DeleteEntry.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    JsSStorage.deleteEntry(payload.path(), payload.directory());
                    sendList(ctx.player(), extractDir(payload.path()));
                })
        );

        ServerPlayNetworking.registerGlobalReceiver(
                JsSNetworking.OpenEditorRequest.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    String content = JsSStorage.loadScript(payload.path());
                    ServerPlayNetworking.send(ctx.player(),
                            new JsSNetworking.OpenEditor(payload.path(), content));
                })
        );

        ServerPlayNetworking.registerGlobalReceiver(
                JsSNetworking.RunScript.ID,
                (payload, ctx) -> ctx.server().execute(() -> {
                    String path = payload.path();
                    String code = JsSStorage.loadScript(path);

                    if (code == null) {
                        System.out.println("[JsScripts] Script not found in storage: " + path);
                        return;
                    }

                    String name = path;
                    int slash = name.lastIndexOf('/');
                    if (slash >= 0) name = name.substring(slash + 1);
                    if (name.endsWith(".js")) name = name.substring(0, name.length() - 3);

                    ScriptManager.save(name, code);
                    ScriptManager.run(name, code);
                })
        );

        ServerLifecycleEvents.SERVER_STARTED.register(server -> ScriptAPI.init(server));


    }

    private static String extractDir(String path) {
        if (path == null || !path.contains("/")) return "";
        return path.substring(0, path.lastIndexOf('/'));
    }


    public static void sendList(ServerPlayerEntity p, String dir) {
        List<JsSStorage.Entry> raw = JsSStorage.list(dir);

        List<JsSNetworking.ScriptEntry> out =
                raw.stream()
                        .map(e -> new JsSNetworking.ScriptEntry(e.path(), e.directory()))
                        .toList();

        ServerPlayNetworking.send(p,
                new JsSNetworking.ScriptsList(dir, out)
        );
    }
}
