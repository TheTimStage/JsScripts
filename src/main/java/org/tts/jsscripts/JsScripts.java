package org.tts.jsscripts;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.tts.jsscripts.commands.JsSCommand;
import org.tts.jsscripts.networking.JsSNetworking;
import org.tts.jsscripts.networking.JsSNetworkingServer;
import org.tts.jsscripts.systems.ScriptManager;

public class JsScripts implements ModInitializer {
    public static final String MOD_ID = "jsscripts";

    @Override
    public void onInitialize() {
        JsSNetworking.registerPayloadTypes();
        JsSNetworkingServer.registerReceivers();

        JsSCommand.init();
    }
}
