package org.tts.jsscripts.client;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.gl.RenderPipelines;

public class SctiptsButton {
    private static final int SIZE = 20;
    private static final Identifier ICON =
            Identifier.of("jsscripts", "textures/gui/icon.png");

    private static int btnX, btnY;

    public static void init() {

        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof ChatScreen)) return;

            btnX = width - SIZE - 8;
            btnY = 8;
            ScreenEvents.afterRender(screen).register((scr, ctx, mouseX, mouseY, delta) -> {
                renderButton(ctx, mouseX, mouseY);
            });

            ScreenMouseEvents.afterMouseClick(screen).register((scr, mouseX, mouseY, button) -> {
                if (button == 0 && inside(mouseX, mouseY)) {
                    if (client.player != null)
                        client.player.networkHandler.sendChatCommand("jsscripts gui");
                }
            });
        });
    }

    private static boolean inside(double mx, double my) {
        return mx >= btnX && mx <= btnX + SIZE &&
                my >= btnY && my <= btnY + SIZE;
    }

    private static void renderButton(DrawContext ctx, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY);
        int bg = hover ? 0xFFCCCCCC : 0xFF888888;

        ctx.fill(btnX, btnY, btnX + SIZE, btnY + SIZE, bg);
        ctx.drawBorder(btnX, btnY, SIZE, SIZE, 0xFF000000);
        ctx.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                ICON,
                btnX + 2,
                btnY + 2,
                0f, 0f,
                SIZE - 4,
                SIZE - 4,
                SIZE - 4,
                SIZE - 4
        );


    }
}
