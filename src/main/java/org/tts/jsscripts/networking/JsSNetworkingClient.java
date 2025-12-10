package org.tts.jsscripts.networking;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import org.tts.jsscripts.client.JsSEditorScreen;
import org.tts.jsscripts.client.ScriptsScreen;

public final class JsSNetworkingClient {

    private JsSNetworkingClient() {}

    public static void registerReceivers() {

        // OPEN MENU
        ClientPlayNetworking.registerGlobalReceiver(JsSNetworking.OpenMenu.ID,
                (payload, ctx) -> MinecraftClient.getInstance()
                        .execute(() -> MinecraftClient.getInstance()
                                .setScreen(new ScriptsScreen(payload.directory()))));

        // LIST
        ClientPlayNetworking.registerGlobalReceiver(JsSNetworking.ScriptsList.ID, (payload, context) -> {
            MinecraftClient client = MinecraftClient.getInstance();

            client.execute(() -> {
                if (client.currentScreen instanceof ScriptsScreen screen) {
                    screen.updateEntries(payload.directory(), payload.entries());
                }
            });
        });


        // OPEN EDITOR
        ClientPlayNetworking.registerGlobalReceiver(JsSNetworking.OpenEditor.ID,
                (payload, ctx) -> MinecraftClient.getInstance()
                        .execute(() -> MinecraftClient.getInstance()
                                .setScreen(new JsSEditorScreen(payload.path(), payload.code()))));
    }


    // ============= CLIENT → SERVER =============

    public static void sendRequestList(String dir) {
        ClientPlayNetworking.send(new JsSNetworking.RequestList(dir));
    }

    public static void sendSaveScript(String path, String code) {
        ClientPlayNetworking.send(new JsSNetworking.SaveScript(path, code));
    }

    public static void sendDeleteEntry(String path, boolean directory) {
        ClientPlayNetworking.send(new JsSNetworking.DeleteEntry(path, directory));
    }

    public static void sendRunScript(String path) {
        ClientPlayNetworking.send(new JsSNetworking.RunScript(path));
    }

    public static void sendOpenEditor(String path) {
        ClientPlayNetworking.send(new JsSNetworking.OpenEditorRequest(path));
    }
}
