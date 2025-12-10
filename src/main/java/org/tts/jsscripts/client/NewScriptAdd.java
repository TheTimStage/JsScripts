package org.tts.jsscripts.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.tts.jsscripts.networking.JsSNetworkingClient;

public class NewScriptAdd extends Screen {

    private final String directory;
    private TextFieldWidget nameField;

    private int boxX, boxY, boxWidth, boxHeight;

    public NewScriptAdd(String directory) {
        super(Text.literal("Create New Script"));
        this.directory = directory == null ? "" : directory;
    }

    @Override
    protected void init() {

        this.boxWidth = Math.max(350, (int) (this.width * 0.40));
        this.boxHeight = Math.max(160, (int) (this.height * 0.25));

        this.boxX = (this.width - boxWidth) / 2;
        this.boxY = (this.height - boxHeight) / 2;

        nameField = new TextFieldWidget(
                this.textRenderer,
                boxX + 20,
                boxY + 50,
                boxWidth - 40,
                20,
                Text.literal("Script Name")
        );
        nameField.setText("new_script.js");
        addDrawableChild(nameField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Create"), btn -> {

            String file = nameField.getText().trim();
            if (file.isEmpty()) return;

            if (!file.endsWith(".js"))
                file += ".js";

            String fullPath = directory.isEmpty()
                    ? file
                    : directory + "/" + file;

            JsSNetworkingClient.sendSaveScript(fullPath, "");
            MinecraftClient.getInstance().setScreen(new ScriptsScreen(directory));

        }).dimensions(
                boxX + 20,
                boxY + boxHeight - 35,
                boxWidth / 2 - 30,
                20
        ).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), btn -> {
            MinecraftClient.getInstance().setScreen(new ScriptsScreen(directory));
        }).dimensions(
                boxX + boxWidth / 2 + 10,
                boxY + boxHeight - 35,
                boxWidth / 2 - 30,
                20
        ).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {

        ctx.fill(0, 0, width, height, 0xCC000000);
        ctx.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF2B2B2B);

        ctx.drawCenteredTextWithShadow(textRenderer, "Create New Script",
                width / 2, boxY + 20, 0xFFFFFF);

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
