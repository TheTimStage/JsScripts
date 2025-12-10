package org.tts.jsscripts.networking;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.tts.jsscripts.JsScripts;

import java.util.ArrayList;
import java.util.List;

public final class JsSNetworking {

    private JsSNetworking() {}

    public record ScriptEntry(String path, boolean directory) {}

    public static final PacketCodec<RegistryByteBuf, ScriptEntry> SCRIPT_ENTRY_CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, ScriptEntry::path,
                    PacketCodecs.BOOLEAN, ScriptEntry::directory,
                    ScriptEntry::new
            );

    public record OpenMenu(String directory) implements CustomPayload {
        public static final Identifier ID_RAW = Identifier.of(JsScripts.MOD_ID, "open_menu");
        public static final Id<OpenMenu> ID = new Id<>(ID_RAW);
        public static final PacketCodec<RegistryByteBuf, OpenMenu> CODEC =
                PacketCodec.tuple(PacketCodecs.STRING, OpenMenu::directory, OpenMenu::new);

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record OpenEditor(String path, String code) implements CustomPayload {
        public static final Identifier ID_RAW = Identifier.of(JsScripts.MOD_ID, "open_editor");
        public static final Id<OpenEditor> ID = new Id<>(ID_RAW);
        public static final PacketCodec<RegistryByteBuf, OpenEditor> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.STRING, OpenEditor::path,
                        PacketCodecs.STRING, OpenEditor::code,
                        OpenEditor::new
                );

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ScriptsList(String directory, List<ScriptEntry> entries) implements CustomPayload {
        public static final Identifier ID_RAW = Identifier.of(JsScripts.MOD_ID, "scripts_list");
        public static final Id<ScriptsList> ID = new Id<>(ID_RAW);
        public static final PacketCodec<RegistryByteBuf, ScriptsList> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.STRING, ScriptsList::directory,
                        PacketCodecs.collection(ArrayList::new, SCRIPT_ENTRY_CODEC), ScriptsList::entries,
                        ScriptsList::new
                );

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record CreateScript(String name) implements CustomPayload {
        public static final Identifier ID_RAW = Identifier.of(JsScripts.MOD_ID, "create_script");
        public static final Id<CreateScript> ID = new Id<>(ID_RAW);
        public static final PacketCodec<RegistryByteBuf, CreateScript> CODEC =
                PacketCodec.tuple(PacketCodecs.STRING, CreateScript::name, CreateScript::new);

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record SaveScript(String path, String code) implements CustomPayload {
        public static final Identifier ID_RAW = Identifier.of(JsScripts.MOD_ID, "save_script");
        public static final Id<SaveScript> ID = new Id<>(ID_RAW);
        public static final PacketCodec<RegistryByteBuf, SaveScript> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.STRING, SaveScript::path,
                        PacketCodecs.STRING, SaveScript::code,
                        SaveScript::new
                );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record DeleteEntry(String path, boolean directory) implements CustomPayload {
        public static final Identifier ID_RAW = Identifier.of(JsScripts.MOD_ID, "delete_entry");
        public static final Id<DeleteEntry> ID = new Id<>(ID_RAW);
        public static final PacketCodec<RegistryByteBuf, DeleteEntry> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.STRING, DeleteEntry::path,
                        PacketCodecs.BOOLEAN, DeleteEntry::directory,
                        DeleteEntry::new
                );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record RunScript(String path) implements CustomPayload {
        public static final Identifier ID_RAW = Identifier.of(JsScripts.MOD_ID, "run_script");
        public static final Id<RunScript> ID = new Id<>(ID_RAW);
        public static final PacketCodec<RegistryByteBuf, RunScript> CODEC =
                PacketCodec.tuple(PacketCodecs.STRING, RunScript::path, RunScript::new);

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record OpenEditorRequest(String path) implements CustomPayload {
        public static final Identifier ID_RAW = Identifier.of(JsScripts.MOD_ID, "request_open_editor");
        public static final Id<OpenEditorRequest> ID = new Id<>(ID_RAW);
        public static final PacketCodec<RegistryByteBuf, OpenEditorRequest> CODEC =
                PacketCodec.tuple(PacketCodecs.STRING, OpenEditorRequest::path, OpenEditorRequest::new);

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record RequestList(String directory) implements CustomPayload {
        public static final Identifier ID_RAW = Identifier.of(JsScripts.MOD_ID, "request_list");
        public static final Id<RequestList> ID = new Id<>(ID_RAW);
        public static final PacketCodec<RegistryByteBuf, RequestList> CODEC =
                PacketCodec.tuple(PacketCodecs.STRING, RequestList::directory, RequestList::new);

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
    

    private static void sendToServer(CustomPayload payload) {
        ClientPlayNetworking.send(payload);
    }

    public static void sendRun(String path) {
        sendToServer(new RunScript(path));
    }

    public static void sendEdit(String path) {
        sendToServer(new OpenEditorRequest(path));
    }

    public static void sendDelete(String path, boolean directory) {
        sendToServer(new DeleteEntry(path, directory));
    }

    public static void sendSaveScript(String path, String code) {
        sendToServer(new SaveScript(path, code));
    }

    public static void sendRequestList(String directory) {
        sendToServer(new RequestList(directory));
    }
    

    public static void registerPayloadTypes() {

        PayloadTypeRegistry.playS2C().register(OpenMenu.ID, OpenMenu.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenEditor.ID, OpenEditor.CODEC);
        PayloadTypeRegistry.playS2C().register(ScriptsList.ID, ScriptsList.CODEC);

        PayloadTypeRegistry.playC2S().register(SaveScript.ID, SaveScript.CODEC);
        PayloadTypeRegistry.playC2S().register(DeleteEntry.ID, DeleteEntry.CODEC);
        PayloadTypeRegistry.playC2S().register(RunScript.ID, RunScript.CODEC);
        PayloadTypeRegistry.playC2S().register(OpenEditorRequest.ID, OpenEditorRequest.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestList.ID, RequestList.CODEC);

        PayloadTypeRegistry.playC2S().register(CreateScript.ID, CreateScript.CODEC);
    }

}
