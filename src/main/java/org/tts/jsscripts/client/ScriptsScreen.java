// !!! Твой класс целиком, изменения только в renderList !!!

package org.tts.jsscripts.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.tts.jsscripts.networking.JsSNetworking;
import org.tts.jsscripts.networking.JsSNetworkingClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ScriptsScreen extends Screen {

    private List<JsSNetworking.ScriptEntry> allEntries = new ArrayList<>();
    private List<JsSNetworking.ScriptEntry> entries = new ArrayList<>();

    private String directory;

    private int boxX, boxY, boxW, boxH;
    private int scroll = 0;
    private final int rowHeight = 36;

    private int hoverIndex = -1;
    private int hoverRowY = 0;

    private final List<ButtonWidget> hoverButtons = new ArrayList<>();

    private TextFieldWidget searchField;

    private static final Identifier ICON_RUN  = Identifier.of("jsscripts", "textures/gui/runn.png");
    private static final Identifier ICON_EDIT = Identifier.of("jsscripts", "textures/gui/editt.png");
    private static final Identifier ICON_DEL  = Identifier.of("jsscripts", "textures/gui/deletee.png");

    private static final int BTN_SIZE = 20;

    public ScriptsScreen(String directory) {
        super(Text.literal("Scripts"));
        this.directory = directory == null ? "" : directory;
    }

    @Override
    protected void init() {
        boxW = 430;
        boxH = 260;

        boxX = (this.width - boxW) / 2;
        boxY = (this.height - boxH) / 2;

        int searchX = boxX + 10;
        int searchY = boxY + 30;
        int searchW = boxW - 20;

        searchField = new TextFieldWidget(
                this.textRenderer,
                searchX, searchY,
                searchW, 18,
                Text.literal("Search")
        );
        searchField.setPlaceholder(Text.literal("Search by name..."));
        searchField.setChangedListener(s -> applyFilter());
        this.addDrawableChild(searchField);

        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("New Script"), b ->
                                MinecraftClient.getInstance().setScreen(new NewScriptAdd(directory))
                        )
                        .dimensions(boxX + (boxW - 160) / 2, boxY + boxH - 28, 160, 20)
                        .build()
        );

        JsSNetworkingClient.sendRequestList(directory);
    }

    public void updateEntries(String dir, List<JsSNetworking.ScriptEntry> newEntries) {
        this.directory = dir;
        this.allEntries = newEntries != null ? newEntries : new ArrayList<>();
        applyFilter();
    }

    private void applyFilter() {
        String query = searchField != null ? searchField.getText().trim().toLowerCase(Locale.ROOT) : "";
        entries = new ArrayList<>();

        if (query.isEmpty()) entries.addAll(allEntries);
        else {
            for (JsSNetworking.ScriptEntry e : allEntries) {
                String name = e.path();
                int idx = name.lastIndexOf('/');
                if (idx >= 0) name = name.substring(idx + 1);
                if (name.toLowerCase(Locale.ROOT).contains(query)) entries.add(e);
            }
        }

        int listH = boxH - 95;
        int maxScroll = Math.max(0, entries.size() * rowHeight - listH);
        scroll = Math.min(scroll, maxScroll);
        scroll = Math.max(scroll, 0);

        setHoverRow(-1, 0, boxX + 10, boxW - 20);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int listX = boxX + 10;
        int listY = boxY + 55;
        int listW = boxW - 20;
        int listH = boxH - 95;

        if (mouseX < listX || mouseX > listX + listW ||
                mouseY < listY || mouseY > listY + listH)
            return false;

        int contentH = entries.size() * rowHeight;
        int maxScroll = Math.max(0, contentH - listH);

        scroll -= (int)(scrollY * rowHeight);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button != 0) return false;

        int listX = boxX + 10;
        int listY = boxY + 55;

        int index = (int)((my - listY + scroll) / rowHeight);
        if (index < 0 || index >= entries.size()) return false;

        JsSNetworking.ScriptEntry e = entries.get(index);

        int rowY = listY + index * rowHeight - scroll;

        if (my < rowY || my > rowY + rowHeight) return false;

        if (e.directory()) {
            MinecraftClient.getInstance().setScreen(new ScriptsScreen(e.path()));
            return true;
        }

        return false;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0x88000000);

        ctx.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xFF2B2B2B);
        ctx.drawBorder(boxX, boxY, boxW, boxH, 0xFF505050);

        ctx.drawCenteredTextWithShadow(textRenderer, "Scripts", width / 2, boxY + 10, 0xFFFFFFFF);

        int listX = boxX + 10;
        int listY = boxY + 55;
        int listW = boxW - 20;
        int listH = boxH - 95;

        ctx.fill(listX, listY, listX + listW, listY + listH, 0xFF1F1F1F);

        renderList(ctx, mouseX, mouseY, listX, listY, listW, listH);

        ctx.drawBorder(listX, listY, listW, listH, 0xFF808080);

        super.render(ctx, mouseX, mouseY, delta);

        drawIcons(ctx);
        drawScrollbar(ctx, listX + listW - 6, listY, listH);
    }

    private void renderList(DrawContext ctx, int mouseX, int mouseY, int listX, int listY, int listW, int listH) {
        int listBottom = listY + listH;
        int start = Math.max(0, scroll / rowHeight);
        int end = Math.min(entries.size(), start + listH / rowHeight + 4);
        int newHoverIndex = -1;
        int newHoverRowY = 0;

        for (int i = start; i < end; i++) {
            JsSNetworking.ScriptEntry e = entries.get(i);
            int rowY = listY + i * rowHeight - scroll;
            if (rowY < listY || rowY + rowHeight > listBottom)
                continue;

            boolean hover = mouseX >= listX && mouseX <= listX + listW &&
                    mouseY >= rowY && mouseY <= rowY + rowHeight;

            if (hover) {
                newHoverIndex = i;
                newHoverRowY = rowY;
            }

            ctx.fill(listX, rowY, listX + listW, rowY + rowHeight,
                    hover ? 0xFF424242 : 0xFF343434);

            String name = e.path().substring(e.path().lastIndexOf("/") + 1);
            int textY = rowY + (rowHeight - textRenderer.fontHeight) / 2 + 1;
            ctx.drawText(textRenderer, name, listX + 8, textY, 0xFFFFFFFF, false);
        }

        setHoverRow(newHoverIndex, newHoverRowY, listX, listW);
    }

    private void setHoverRow(int index, int rowY, int listX, int listW) {
        for (ButtonWidget btn : hoverButtons) this.remove(btn);
        hoverButtons.clear();

        hoverIndex = index;
        hoverRowY = rowY;

        if (index < 0 || index >= entries.size()) return;
        JsSNetworking.ScriptEntry e = entries.get(index);
        if (e.directory()) return;

        int delX  = listX + listW - BTN_SIZE - 5;
        int editX = delX - BTN_SIZE - 5;
        int runX  = editX - BTN_SIZE - 5;

        ButtonWidget run = ButtonWidget.builder(Text.empty(), b -> JsSNetworkingClient.sendRunScript(e.path()))
                .dimensions(runX, rowY + 8, BTN_SIZE, BTN_SIZE).build();

        ButtonWidget edit = ButtonWidget.builder(Text.empty(), b -> JsSNetworkingClient.sendOpenEditor(e.path()))
                .dimensions(editX, rowY + 8, BTN_SIZE, BTN_SIZE).build();

        ButtonWidget del = ButtonWidget.builder(Text.empty(), b -> JsSNetworkingClient.sendDeleteEntry(e.path(), false))
                .dimensions(delX, rowY + 8, BTN_SIZE, BTN_SIZE).build();

        hoverButtons.add(run);
        hoverButtons.add(edit);
        hoverButtons.add(del);

        addDrawableChild(run);
        addDrawableChild(edit);
        addDrawableChild(del);
    }

    private void drawIcons(DrawContext ctx) {
        for (int i = 0; i < hoverButtons.size(); i++) {
            ButtonWidget btn = hoverButtons.get(i);

            Identifier icon = switch (i) {
                case 0 -> ICON_RUN;
                case 1 -> ICON_EDIT;
                case 2 -> ICON_DEL;
                default -> null;
            };
            if (icon == null) continue;

            ctx.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    icon,
                    btn.getX() + 2,
                    btn.getY() + 2,
                    0, 0,
                    BTN_SIZE - 4,
                    BTN_SIZE - 4,
                    BTN_SIZE - 4,
                    BTN_SIZE - 4
            );
        }
    }

    private void drawScrollbar(DrawContext ctx, int x, int y, int h) {
        int content = entries.size() * rowHeight;
        int view = h;
        if (content <= view) return;

        ctx.fill(x, y, x + 5, y + h, 0xFF101010);

        float ratio = (float) view / content;
        int barH = Math.max(20, (int)(ratio * h));

        float pos = (float) scroll / (content - view);
        int barY = (int)(pos * (h - barH));

        ctx.fill(x + 1, y + barY + 1, x + 4, y + barY + barH - 1, 0xFFCCCCCC);
    }
}
