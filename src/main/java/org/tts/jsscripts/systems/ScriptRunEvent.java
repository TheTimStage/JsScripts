package org.tts.jsscripts.systems;

import net.minecraft.server.network.ServerPlayerEntity;

public class ScriptRunEvent {
    public final String name;
    public final ServerPlayerEntity player;

    public ScriptRunEvent(String name, ServerPlayerEntity player) {
        this.name = name;
        this.player = player;
    }


}
