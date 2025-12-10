package org.tts.jsscripts.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.tts.jsscripts.networking.JsSNetworkingClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JsSEditorScreen extends Screen {
    private static final int GUTTER_WIDTH = 45;
    private static final int BORDER_SIZE  = 2;
    private static final int LINE_HEIGHT  = 14;

    private static final List<String> KEYWORDS = Arrays.asList(
            "let","var","const","if","else","for","while","switch","case","break",
            "continue","function","return","class","extends","new","try","catch",
            "throw","async","await","true","false","null","undefined",
            "Math","JSON","Object","Array","Number","String","Boolean"
    );
    
    private List<String> lines = new ArrayList<>();
    private final String filePath;
    private int editorX, editorY, editorW, editorH;
    private int scroll = 0;
    private int caretLine = 0;
    private int caretCol  = 0;


    private static class Suggestion {
        final String name;
        final String signature;
        final String description;
        Suggestion(String name, String signature, String description) {
            this.name = name;
            this.signature = signature;
            this.description = description;
        }
    }

    private final List<Suggestion> allSuggestions = new ArrayList<>();
    private List<Suggestion> visibleSuggestions = new ArrayList<>();
    private boolean suggestionOpen = false;
    private int suggestionIndex = 0;
    private int suggestionX = 0;
    private int suggestionY = 0;
    private String lastErrorMessage = null;
    private int lastErrorLine = -1;
    private int lastErrorColumn = -1;


    public JsSEditorScreen(String path, String content) {
        super(Text.literal("JS Editor"));
        this.filePath = path;

        if (content != null && !content.isEmpty()) {
            this.lines = new ArrayList<>(Arrays.asList(content.split("\n", -1)));
        } else {
            this.lines.add("");
        }

        initSuggestions();
        recheckSyntax();
    }

    @Override
    protected void init() {
        editorX = 60;
        editorY = 40;
        editorW = width  - 120;
        editorH = height - 120;

        int btnY = editorY + editorH + 10;
        int btnW = 80;
        int btnH = 20;
        
        addDrawableChild(
                ButtonWidget.builder(Text.literal("Save"), b -> {
                    JsSNetworkingClient.sendSaveScript(filePath, collectText());
                    MinecraftClient.getInstance().setScreen(null);
                }).dimensions(editorX, btnY, btnW, btnH).build()
        );
        
        addDrawableChild(
                ButtonWidget.builder(Text.literal("Back"), b ->
                        MinecraftClient.getInstance().setScreen(null)
                ).dimensions(editorX + btnW + 20, btnY, btnW, btnH).build()
        );
        
        addDrawableChild(
                ButtonWidget.builder(Text.literal("Run"), b -> {
                    JsSNetworkingClient.sendSaveScript(filePath, collectText());
                    JsSNetworkingClient.sendRunScript(filePath);
                }).dimensions(editorX + (btnW + 20) * 2, btnY, btnW, btnH).build()
        );
    }

    private String collectText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            sb.append(lines.get(i));
            if (i < lines.size() - 1) sb.append("\n");
        }
        return sb.toString();
    }

    private void initSuggestions() {
        addSuggestion("let", "let name = value", "Объявление переменной с блочной областью видимости.");
        addSuggestion("const", "const NAME = value", "Константа, которую нельзя переопределить.");
        addSuggestion("var", "var name = value", "Переменная с функциональной областью видимости.");
        addSuggestion("function", "function name(args) {}", "Объявление функции.");
        addSuggestion("class", "class Name {}", "Объявление класса.");
        addSuggestion("async", "async function() {}", "Асинхронная функция.");
        addSuggestion("await", "await expr", "Ожидание Promise.");
        addSuggestion("return", "return value", "Вернуть значение из функции.");
        addSuggestion("if", "if (expr) {}", "Условный оператор.");
        addSuggestion("else", "else {}", "Альтернативная ветка.");
        addSuggestion("for", "for (let i=0;i<...;i++) {}", "Цикл for.");
        addSuggestion("while", "while(expr) {}", "Цикл while.");
        addSuggestion("switch", "switch(x) {}", "Switch-конструкция.");
        addSuggestion("try", "try {} catch(err) {}", "Обработка ошибок.");
        addSuggestion("log", "log(value)", "Вывод в консоль сервера.");
        addSuggestion("scriptType", "scriptType(\"once\" | \"loop\")", "Тип скрипта: одно выполнение или работа в тиках.");
        addSuggestion("scriptEnd", "scriptEnd()", "Принудительно завершает выполнение текущего скрипта.");
        addSuggestion("on", "on(event, (args)=>{})", "Подписка на событие ScriptAPI.");
        addSuggestion("onLoop", "onLoop(()=>{})", "Функция, вызываемая каждый тик, только если scriptType('loop').");
        addSuggestion("wait", "wait(ticks, ()=>{})", "Отложенный вызов функции через N тиков.");
        addSuggestion("delay", "delay(ticks, ()=>{})", "Alias wait().");
        addSuggestion("repeat", "repeat(count, i=>{})", "Выполнить функцию N раз с индексом i.");
        addSuggestion("every", "every(ticks, ()=>{})", "Вызывать функцию каждые N тиков.");
        addSuggestion("serverStart", "\"serverStart\"", "Событие: сервер запущен.");
        addSuggestion("serverTick", "\"serverTick\"", "Событие: каждый тик сервера.");
        addSuggestion("playerJoin", "\"playerJoin\"", "Игрок зашёл.");
        addSuggestion("playerLeave", "\"playerLeave\"", "Игрок вышел.");
        addSuggestion("playerTick", "\"playerTick\"", "Каждый тик по игроку.");
        addSuggestion("chatMessage", "\"chatMessage\"", "Сообщение: (msg, player).");
        addSuggestion("blockBreak", "\"blockBreak\"", "Игрок сломал блок.");
        addSuggestion("blockPlace", "\"blockPlace\"", "Игрок кликнул по блоку.");
        addSuggestion("msg", "msg(player, text)", "Сообщение игроку.");
        addSuggestion("sendMessage", "sendMessage(player, msg)", "Синоним msg().");
        addSuggestion("actionbar", "actionbar(player, text)", "Сообщение в actionbar.");
        addSuggestion("title", "title(player, text)", "Большой заголовок на экране.");
        addSuggestion("subtitle", "subtitle(player, text)", "Подзаголовок.");
        addSuggestion("fullTitle", "fullTitle(player, title, subtitle, fadeIn, stay, fadeOut)", "Полный титр с таймингами.");
        addSuggestion("tp", "tp(player, x,y,z)", "Телепортировать игрока.");
        addSuggestion("give", "give(player, itemId)", "Выдать предмет игроку.");
        addSuggestion("playSound", "playSound(player, id, volume, pitch)", "Проиграть звук игроку.");
        addSuggestion("playSoundAt", "playSoundAt(world, x,y,z, id [,volume,pitch])", "Проиграть звук по координатам.");
        addSuggestion("World", "World", "Глобальный объект мира.");
        addSuggestion("World.overworld", "World.overworld()", "Обычный мир.");
        addSuggestion("World.nether", "World.nether()", "Незер.");
        addSuggestion("World.end", "World.end()", "Энд.");
        addSuggestion("World.setBlock", "World.setBlock(world,x,y,z,blockId)", "Поставить блок.");
        addSuggestion("World.particle", "World.particle(world,id,x,y,z,dx,dy,dz,speed,count)", "Показать частицы.");
        addSuggestion("runCommand", "runCommand(cmd)", "Выполнить команду от имени сервера.");
        addSuggestion("runCommandAs", "runCommandAs(player, cmd)", "Команда от лица игрока.");
    }


    private void addSuggestion(String name, String signature, String desc) {
        allSuggestions.add(new Suggestion(name, signature, desc));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horiz, double vert) {
        int maxScroll = Math.max(0, lines.size() * LINE_HEIGHT - editorH);
        scroll -= (int) (vert * 20);
        if (scroll < 0) scroll = 0;
        if (scroll > maxScroll) scroll = maxScroll;
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (chr == '\r' || chr == '\n') {
            return true;
        }

        if (chr == '}') {
            handleClosingBrace();
            recheckSyntax();
            updateSuggestionsAuto();
            return true;
        }

        if (chr >= 32 && chr != 127) {
            if (autoPair(chr)) {
                recheckSyntax();
                updateSuggestionsAuto();
                return true;
            }

            insertText(String.valueOf(chr));
            updateSuggestionsAuto();
        }

        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (suggestionOpen) {
            if (key == 265) {
                if (!visibleSuggestions.isEmpty()) {
                    suggestionIndex = Math.max(0, suggestionIndex - 1);
                }
                return true;
            }

            if (key == 264) {
                if (!visibleSuggestions.isEmpty()) {
                    suggestionIndex = Math.min(visibleSuggestions.size() - 1, suggestionIndex + 1);
                }
                return true;
            }

            if (key == 257 || key == 258) {
                acceptSuggestion();
                return true;
            }

            if (key == 256) {
                suggestionOpen = false;
                return true;
            }
        }

        if (key == 257) { splitLineWithAutoIndent(); return true; }
        if (key == 259) { backspace(); return true; }
        if (key == 261) { delete(); return true; }
        if (key == 263) { moveLeft(); return true; }
        if (key == 262) { moveRight(); return true; }
        if (key == 265) { moveUp(); return true; }
        if (key == 264) { moveDown(); return true; }
        if (key == 258) {
            insertText("    ");
            updateSuggestionsAuto();
            return true;
        }

        return super.keyPressed(key, scan, mods);
    }
    private int countLeadingSpaces(String s) {
        int i = 0;
        while (i < s.length() && s.charAt(i) == ' ') i++;
        return i;
    }

    private String makeIndent(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) sb.append(' ');
        return sb.toString();
    }

    private String rtrim(String s) {
        int i = s.length() - 1;
        while (i >= 0 && Character.isWhitespace(s.charAt(i))) i--;
        return s.substring(0, i + 1);
    }

    private void insertText(String s) {
        String line = lines.get(caretLine);
        line = line.substring(0, caretCol) + s + line.substring(caretCol);
        lines.set(caretLine, line);
        caretCol += s.length();
        recheckSyntax();
    }

    private void splitLineWithAutoIndent() {
        String line = lines.get(caretLine);
        String beforeCaret = line.substring(0, caretCol);
        String right = line.substring(caretCol);

        int baseIndent = countLeadingSpaces(line);
        boolean betweenBraces = beforeCaret.trim().endsWith("{") && right.trim().equals("}");

        if (betweenBraces) {
            int bracePos = beforeCaret.lastIndexOf('{');
            if (bracePos < 0) bracePos = caretCol - 1;

            String leftPart = line.substring(0, bracePos + 1);
            int totalBaseIndent = countLeadingSpaces(leftPart);

            String baseIndentStr = makeIndent(totalBaseIndent);
            String innerIndentStr = makeIndent(totalBaseIndent + 4);


            lines.set(caretLine, rtrim(leftPart));
            lines.add(caretLine + 1, innerIndentStr);
            lines.add(caretLine + 2, baseIndentStr + "}");

            caretLine++;
            caretCol = innerIndentStr.length();

            recheckSyntax();
            updateSuggestionsAuto();
            return;
        }

        String left = line.substring(0, caretCol);
        String rightRest = line.substring(caretCol);

        int extraIndent = 0;
        if (beforeCaret.contains("{")) {
            extraIndent = 4;
        }

        lines.set(caretLine, left);
        lines.add(caretLine + 1, rightRest);

        caretLine++;
        caretCol = 0;

        int totalIndent = baseIndent + extraIndent;
        if (totalIndent > 0) {
            String indent = makeIndent(totalIndent);
            insertText(indent);
        } else {
            recheckSyntax();
        }

        updateSuggestionsAuto();
    }

    private boolean autoPair(char chr) {
        String line = lines.get(caretLine);
        char next = (caretCol < line.length()) ? line.charAt(caretCol) : '\0';

        if (chr == '"') {
            insertText("\"\"");
            
            caretCol--;
            return true;
        }

        if (chr == '\'') {
            insertText("''");
            caretCol--;
            return true;
        }

        if (chr == '`') {
            insertText("``");
            caretCol--;
            return true;
        }

        if (chr == '(') {
            insertText("()");
            caretCol--;
            return true;
        }

        if (chr == '[') {
            insertText("[]");
            caretCol--;
            return true;
        }

        if (chr == '{') {
            insertText("{}");
            caretCol--;
            return true;
        }

        if ((chr == ')' && next == ')') ||
                (chr == ']' && next == ']') ||
                (chr == '}' && next == '}') ||
                (chr == '"' && next == '"') ||
                (chr == '\'' && next == '\'') ||
                (chr == '`' && next == '`')) {

            caretCol++;
            return true;
        }

        return false;
    }


    private void backspace() {
        if (caretCol > 0) {
            String line = lines.get(caretLine);
            line = line.substring(0, caretCol - 1) + line.substring(caretCol);
            lines.set(caretLine, line);
            caretCol--;
            recheckSyntax();
            updateSuggestionsAuto();
            return;
        }

        if (caretLine > 0) {
            int prevLen = lines.get(caretLine - 1).length();
            lines.set(caretLine - 1, lines.get(caretLine - 1) + lines.get(caretLine));
            lines.remove(caretLine);
            caretLine--;
            caretCol = prevLen;
            recheckSyntax();
            updateSuggestionsAuto();
        }
    }

    private void delete() {
        String line = lines.get(caretLine);
        if (caretCol < line.length()) {
            lines.set(caretLine, line.substring(0, caretCol) + line.substring(caretCol + 1));
            recheckSyntax();
            updateSuggestionsAuto();
        } else if (caretLine < lines.size() - 1) {
            lines.set(caretLine, line + lines.get(caretLine + 1));
            lines.remove(caretLine + 1);
            recheckSyntax();
            updateSuggestionsAuto();
        }
    }

    private void moveLeft() {
        if (caretCol > 0) caretCol--;
        else if (caretLine > 0) {
            caretLine--;
            caretCol = lines.get(caretLine).length();
        }
        suggestionOpen = false;
    }

    private void moveRight() {
        if (caretCol < lines.get(caretLine).length()) caretCol++;
        else if (caretLine < lines.size() - 1) {
            caretLine++;
            caretCol = 0;
        }
        suggestionOpen = false;
    }

    private void moveUp() {
        if (caretLine > 0) caretLine--;
        caretCol = Math.min(caretCol, lines.get(caretLine).length());
        suggestionOpen = false;
    }

    private void moveDown() {
        if (caretLine < lines.size() - 1) caretLine++;
        caretCol = Math.min(caretCol, lines.get(caretLine).length());
        suggestionOpen = false;
    }

    private void handleClosingBrace() {
        String line = lines.get(caretLine);
        int leading = countLeadingSpaces(line);

        if (caretCol > leading) {
            insertText("}");
            return;
        }

        int newIndent = Math.max(0, leading - 4);
        String indent = makeIndent(newIndent);
        String rest = line.trim();

        if (rest.equals("}")) {
            lines.set(caretLine, indent + "}");
            caretCol = indent.length() + 1;
        } else {
            lines.set(caretLine, indent + "}");
            caretCol = indent.length() + 1;
        }
    }

    private void updateSuggestionsAuto() {
        String prefix = getCurrentTokenPrefix();
        if (prefix == null || prefix.isEmpty()) {
            suggestionOpen = false;
            return;
        }
        buildVisibleSuggestions(prefix);
        if (!visibleSuggestions.isEmpty()) {
            suggestionOpen = true;
            suggestionIndex = 0;
            updateSuggestionPopupPosition();
        } else {
            suggestionOpen = false;
        }
    }

    private String getCurrentTokenPrefix() {
        String line = lines.get(caretLine);
        int pos = Math.min(caretCol, line.length());

        int start = pos;
        while (start > 0) {
            char c = line.charAt(start - 1);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '.') start--;
            else break;
        }
        if (start >= pos) return "";

        return line.substring(start, pos);
    }

    private int getCurrentTokenStartIndex() {
        String line = lines.get(caretLine);
        int pos = Math.min(caretCol, line.length());
        int start = pos;
        while (start > 0) {
            char c = line.charAt(start - 1);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '.') start--;
            else break;
        }
        return start;
    }

    private void buildVisibleSuggestions(String prefix) {
        visibleSuggestions = new ArrayList<>();
        if (prefix == null) prefix = "";
        String p = prefix;

        for (Suggestion s : allSuggestions) {
            if (s.name.startsWith(p)) {
                visibleSuggestions.add(s);
            }
        }
    }

    private void updateSuggestionPopupPosition() {
        String line = lines.get(caretLine);
        int x = editorX + 6 + textRenderer.getWidth(
                line.substring(0, Math.min(caretCol, line.length()))
        );
        int y = editorY + caretLine * LINE_HEIGHT - scroll + LINE_HEIGHT;
        suggestionX = x;
        suggestionY = y;
    }

    private void acceptSuggestion() {
        if (!suggestionOpen || visibleSuggestions.isEmpty()) return;
        Suggestion s = visibleSuggestions.get(suggestionIndex);

        String line = lines.get(caretLine);
        int tokenStart = getCurrentTokenStartIndex();
        int tokenEnd = Math.min(caretCol, line.length());

        String newLine = line.substring(0, tokenStart) + s.name + line.substring(tokenEnd);
        lines.set(caretLine, newLine);
        caretCol = tokenStart + s.name.length();

        suggestionOpen = false;
        recheckSyntax();
    }

    private void recheckSyntax() {
        String code = collectText();

        Context cx = null;
        try {
            cx = Context.enter();
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.compileString(code, "<editor>", 1, null);

            lastErrorMessage = null;
            lastErrorLine = -1;
            lastErrorColumn = -1;

        } catch (EvaluatorException e) {
            lastErrorMessage = e.getMessage();
            lastErrorLine = e.lineNumber();
            lastErrorColumn = e.columnNumber();
        } catch (Throwable t) {
            lastErrorMessage = t.getMessage();
            lastErrorLine = -1;
            lastErrorColumn = -1;
        } finally {
            if (cx != null) Context.exit();
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0xFF2B2B2B);

        int innerLeft   = editorX;
        int innerTop    = editorY;
        int innerRight  = editorX + editorW;
        int innerBottom = editorY + editorH;


        ctx.fill(innerLeft, innerTop, innerRight, innerBottom, 0xFF3C3F41);
        int gutterLeft = innerLeft - GUTTER_WIDTH;
        ctx.fill(gutterLeft, innerTop, innerLeft, innerBottom, 0xFF313335);

        int b = BORDER_SIZE;
        int col = 0xFF212223;

        ctx.fill(innerLeft - b, innerTop - b, innerRight + b, innerTop, col);           // top
        ctx.fill(innerLeft - b, innerBottom, innerRight + b, innerBottom + b, col);     // bottom
        ctx.fill(innerLeft - b, innerTop - b, innerLeft, innerBottom + b, col);         // left
        ctx.fill(innerRight, innerTop - b, innerRight + b, innerBottom + b, col);       // right

        int firstIndex = Math.max(0, scroll / LINE_HEIGHT - 1);
        int lastIndex  = Math.min(lines.size() - 1,
                (scroll + editorH) / LINE_HEIGHT + 1);

        for (int i = firstIndex; i <= lastIndex; i++) {
            int y = innerTop + i * LINE_HEIGHT - scroll;
            if (y + LINE_HEIGHT < innerTop || y > innerBottom) continue;

            ctx.drawText(textRenderer, String.valueOf(i + 1),
                    gutterLeft + 8, y, 0xFF808080, false);

            if (lastErrorLine == i + 1) {
                ctx.fill(gutterLeft + 2, y + 2, gutterLeft + 6, y + LINE_HEIGHT - 2, 0xFFCC3333);
            }
        }

        for (int i = firstIndex; i <= lastIndex; i++) {
            int y = innerTop + (i - firstIndex) * LINE_HEIGHT - (scroll % LINE_HEIGHT);
            if (y + LINE_HEIGHT < innerTop || y > innerBottom) continue;
            String line = lines.get(i);

            int bgColor = 0xFF3C3F41;
            if (i == caretLine) {
                bgColor = 0xFF44484A;
            }
            if (lastErrorLine == i + 1) {
                bgColor = 0xFF553333;
            }

            ctx.fill(innerLeft, y, innerRight, y + LINE_HEIGHT, bgColor);

            drawSyntaxLine(ctx, line, innerLeft + 6, y);

            if (lastErrorLine == i + 1 && lastErrorColumn > 0) {
                int colIndex = Math.max(0, Math.min(line.length(), lastErrorColumn - 1));
                int errX = innerLeft + 6 + textRenderer.getWidth(line.substring(0, colIndex));
                ctx.fill(errX, y, errX + 1, y + LINE_HEIGHT, 0xFFFF5555);
            }
        }

        drawCaret(ctx, innerLeft, innerTop, innerBottom);

        if (suggestionOpen && !visibleSuggestions.isEmpty()) {
            renderSuggestionsPopup(ctx);
        }

        renderErrorPanel(ctx);
        super.render(ctx, mouseX, mouseY, delta);
        drawMainBorder(ctx);
    }

    private void drawCaret(DrawContext ctx, int innerLeft, int innerTop, int innerBottom) {
        String line = lines.get(caretLine);
        int caretX = innerLeft + 6 + textRenderer.getWidth(
                line.substring(0, Math.min(caretCol, line.length()))
        );
        int caretY = innerTop + caretLine * LINE_HEIGHT - scroll;

        if (caretY + LINE_HEIGHT < innerTop || caretY > innerBottom) return;

        ctx.fill(caretX, caretY, caretX + 2, caretY + LINE_HEIGHT, 0xFFBBBBBB);
    }

    private void drawMainBorder(DrawContext ctx) {
        int l = editorX;
        int t = editorY;
        int r = editorX + editorW;
        int btm = editorY + editorH;

        int bw = BORDER_SIZE;
        int col = 0xFF1F1F20;

        ctx.fill(l - bw, t - bw, r + bw, t, col);
        ctx.fill(l - bw, btm, r + bw, btm + bw, col);
        ctx.fill(l - bw, t - bw, l, btm + bw, col);
        ctx.fill(r, t - bw, r + bw, btm + bw, col);
    }


    private List<String> wrapText(String text, int maxWidthPx) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty()) return result;

        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();

        for (String w : words) {
            if (current.length() == 0) {
                current.append(w);
            } else {
                String candidate = current + " " + w;
                if (textRenderer.getWidth(candidate) <= maxWidthPx) {
                    current.append(" ").append(w);
                } else {
                    result.add(current.toString());
                    current.setLength(0);
                    current.append(w);
                }
            }
        }

        if (current.length() > 0) {
            result.add(current.toString());
        }

        return result;
    }

    private void renderSuggestionsPopup(DrawContext ctx) {
        if (visibleSuggestions.isEmpty()) return;

        int maxItemsToShow = Math.min(8, visibleSuggestions.size());
        int maxLabelWidth = 0;
        for (int i = 0; i < maxItemsToShow; i++) {
            Suggestion s = visibleSuggestions.get(i);
            int w = textRenderer.getWidth(s.name + "  " + s.signature);
            if (w > maxLabelWidth) maxLabelWidth = w;
        }
        
        Suggestion current = visibleSuggestions.get(suggestionIndex);
        String desc = current.description == null ? "" : current.description;

        int maxPopupWidth = Math.min(400, width - 20);
        int minPopupWidth = 120;

        int rawDescWidth = desc.isEmpty() ? 0 : textRenderer.getWidth(desc);

        int popupW = Math.max(maxLabelWidth + 10, rawDescWidth + 10);
        popupW = Math.max(minPopupWidth, popupW);
        popupW = Math.min(maxPopupWidth, popupW);

        int descAreaWidth = popupW - 8;
        List<String> descLines = wrapText(desc, descAreaWidth);
        int descHeight = descLines.isEmpty() ? 0 : (descLines.size() * (LINE_HEIGHT - 2) + 6);

        int listHeight = maxItemsToShow * (LINE_HEIGHT + 1) + 8;
        int popupH = listHeight + (descHeight > 0 ? (descHeight + 6) : 0);

        int popupX = suggestionX;
        int popupY = suggestionY;

        if (popupX + popupW > width - 4) {
            popupX = Math.max(4, width - popupW - 4);
        }
        if (popupY + popupH > height - 4) {
            popupY = Math.max(4, popupY - popupH - LINE_HEIGHT);
        }

        ctx.fill(popupX, popupY, popupX + popupW, popupY + popupH, 0xEE3C3F41);
        ctx.fill(popupX, popupY, popupX + popupW, popupY + 1, 0xFF212223);
        ctx.fill(popupX, popupY + popupH - 1, popupX + popupW, popupY + popupH, 0xFF212223);
        ctx.fill(popupX, popupY, popupX + 1, popupY + popupH, 0xFF212223);
        ctx.fill(popupX + popupW - 1, popupY, popupX + popupW, popupY + popupH, 0xFF212223);

        int y = popupY + 4;
        for (int i = 0; i < maxItemsToShow; i++) {
            Suggestion s = visibleSuggestions.get(i);

            int bgCol = (i == suggestionIndex) ? 0x553A3D41 : 0x00000000;
            if (bgCol != 0) {
                ctx.fill(popupX + 2, y - 1, popupX + popupW - 2, y + LINE_HEIGHT + 1, bgCol);
            }

            int textX = popupX + 4;
            ctx.drawText(textRenderer, s.name, textX, y, 0xFFCCCCCC, false);
            int nameWidth = textRenderer.getWidth(s.name);

            if (s.signature != null && !s.signature.isEmpty()) {
                ctx.drawText(textRenderer, "  " + s.signature,
                        textX + nameWidth, y, 0xFF808080, false);
            }

            y += LINE_HEIGHT + 1;
        }

        if (!descLines.isEmpty()) {
            int descY = popupY + listHeight + 2;
            ctx.fill(popupX + 2, descY - 2, popupX + popupW - 2, descY - 1, 0xFF212223);

            for (String line : descLines) {
                ctx.drawText(textRenderer, line, popupX + 4, descY, 0xFFBBBBBB, false);
                descY += (LINE_HEIGHT - 2);
            }
        }
    }


    private void renderErrorPanel(DrawContext ctx) {
        if (lastErrorMessage == null) return;

        String msg = lastErrorMessage;
        if (msg.length() > 60) msg = msg.substring(0, 57) + "...";

        String text = "Syntax: " + msg;
        if (lastErrorLine > 0) {
            text += " (line " + lastErrorLine + ", col " + lastErrorColumn + ")";
        }

        int tw = textRenderer.getWidth(text);
        int padding = 6;
        int x2 = width - 10;
        int y2 = height - 10;
        int x1 = x2 - tw - padding * 2;
        int y1 = y2 - LINE_HEIGHT - padding * 2;

        ctx.fill(x1, y1, x2, y2, 0xEE3C3F41);
        ctx.fill(x1, y1, x2, y1 + 1, 0xFFB00020);
        ctx.fill(x1, y2 - 1, x2, y2, 0xFFB00020);
        ctx.fill(x1, y1, x1 + 1, y2, 0xFFB00020);
        ctx.fill(x2 - 1, y1, x2, y2, 0xFFB00020);

        ctx.drawText(textRenderer, text, x1 + padding, y1 + padding, 0xFFFFCCCC, false);
    }

    private void drawSyntaxLine(DrawContext ctx, String line, int x, int y) {
        String trimmed = line.trim();
        if (trimmed.startsWith("//")) {
            ctx.drawText(textRenderer, line, x, y, 0xFF808080, false);
            return;
        }

        List<String> tokens = tokenize(line);
        int xx = x;

        for (String t : tokens) {
            int color = 0xFFD0D0D0;

            if (KEYWORDS.contains(t)) {
                color = 0xFFCC7832;
            } else if (t.startsWith("\"") || t.startsWith("'")) {
                color = 0xFF6A8759;
            } else if (t.matches("[0-9]+")) {
                color = 0xFF6897BB;
            } else if (t.startsWith("//")) {
                color = 0xFF808080;
            } else if ("{}[]();,.".contains(t)) {
                color = 0xFFD0D0D0;
            }

            ctx.drawText(textRenderer, t, xx, y, color, false);
            xx += textRenderer.getWidth(t);
        }
    }

    private List<String> tokenize(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if ("{}[]();, .".indexOf(c) >= 0) {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current.setLength(0);
                }
                result.add(String.valueOf(c));
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            result.add(current.toString());
        }

        return result;
    }
}
