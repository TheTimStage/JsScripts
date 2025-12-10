package org.tts.jsscripts;

import net.fabricmc.api.ClientModInitializer;
import org.tts.jsscripts.client.SctiptsButton;
import org.tts.jsscripts.networking.JsSNetworkingClient;

public class JsScriptsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        JsSNetworkingClient.registerReceivers();
        SctiptsButton.init();
    }
}
