package de.artemis.omniwheel.client.overlay;

import de.artemis.omniwheel.client.render.LiftedRowRenderer;
import de.artemis.omniwheel.common.OmniWheelText;
import net.minecraft.client.Minecraft;
import de.artemis.omniwheel.client.render.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.Locale;
import java.util.function.IntConsumer;

/** Modal color selection. Only Apply writes the entry; Escape and Cancel leave it unchanged. */
final class CustomColorPicker {
    private IntConsumer apply;
    private String title, hex;
    private float hue, saturation, brightness;
    private int original, x, y, focus, dragging = -1;
    private int panelY, panelHeight = HEIGHT;
    private boolean replaceHex;
    private static final int WIDTH = 248, HEIGHT = 232, BOARD_WIDTH = 190, BOARD_HEIGHT = 100;

    boolean isOpen() { return apply != null; }
    de.artemis.omniwheel.client.navigation.ManagementNavigation.Bounds bounds() {
        return new de.artemis.omniwheel.client.navigation.ManagementNavigation.Bounds(x - 2, panelY - 2, WIDTH + 4, panelHeight + 4);
    }
    void close() { apply = null; dragging = -1; }
    void open(String title, int color, IntConsumer apply) {
        this.title = title; this.apply = apply; original = color;
        setColor(color); focus = 0; dragging = -1; replaceHex = true;
    }
    private void setColor(int color) {
        float[] hsv = Color.RGBtoHSB((color >>> 16) & 255, (color >>> 8) & 255, color & 255, null);
        hue = hsv[0]; saturation = hsv[1]; brightness = hsv[2];
        hex = String.format(Locale.ROOT, "%06X", color & 0xFFFFFF);
    }
    private int color() { return Color.HSBtoRGB(hue, saturation, brightness); }
    private void syncHex() { hex = String.format(Locale.ROOT, "%06X", color() & 0xFFFFFF); replaceHex = true; }
    private void apply() {
        if (hex.length() != 6) return;
        int color = 0xFF000000 | Integer.parseInt(hex, 16);
        IntConsumer callback = apply; close(); callback.accept(color);
    }
    private static String text(String key) { return OmniWheelText.translate(key); }
    void render(GuiGraphics graphics, Minecraft minecraft, int width, int height, float mouseX, float mouseY) {
        render(graphics, minecraft, width, height, mouseX, mouseY, true);
    }
    void render(GuiGraphics graphics, Minecraft minecraft, int width, int height, float mouseX, float mouseY, boolean shadeBackground) {
        int titleSpace = title.isBlank() ? -16 : 0;
        panelHeight = HEIGHT + titleSpace;
        x = (width - WIDTH) / 2; panelY = (height - panelHeight) / 2;
        y = panelY + titleSpace;
        graphics.flush();
        if (shadeBackground) graphics.fill(0, 0, width, height, 0x94000000);
        LiftedRowRenderer.shadow(graphics, x, panelY, WIDTH, panelHeight);
        graphics.flush();
        graphics.fill(x, panelY, x + WIDTH, panelY + panelHeight, 0xFF18212B);
        border(graphics, x, panelY, WIDTH, panelHeight, 0xFF526575);
        if (!title.isBlank()) graphics.drawString(minecraft.font, title, x + 12, y + 12, 0xFFF2F5F8, false);
        int left = x + 12, top = y + 32;
        // Two GPU-interpolated quads replace 4,750 individual colored squares.
        // Rotate the vertical gradient to interpolate white -> hue horizontally.
        graphics.pose().pushPose();
        graphics.pose().translate(left, top, 0.0F);
        graphics.pose().mulPose(com.mojang.math.Vector3f.ZP.rotation((float) (-Math.PI / 2)));
        graphics.fillGradient(-BOARD_HEIGHT, 0, 0, BOARD_WIDTH, 0xFFFFFFFF, Color.HSBtoRGB(hue, 1, 1));
        graphics.pose().popPose();
        graphics.fillGradient(left, top, left + BOARD_WIDTH, top + BOARD_HEIGHT, 0x00000000, 0xFF000000);
        // Hue is linear within each of its six RGB intervals.
        for (int band = 0; band < 6; band++) {
            graphics.fillGradient(x + 214, top + BOARD_HEIGHT * band / 6,
                    x + 236, top + BOARD_HEIGHT * (band + 1) / 6,
                    Color.HSBtoRGB(band / 6F, 1, 1), Color.HSBtoRGB((band + 1) / 6F, 1, 1));
        }
        border(graphics, left - 1, top - 1, BOARD_WIDTH + 2, BOARD_HEIGHT + 2, focus == 0 ? 0xFF8BDAF8 : 0xFF526575);
        border(graphics, x + 213, top - 1, 24, BOARD_HEIGHT + 2, focus == 1 ? 0xFF8BDAF8 : 0xFF526575);
        int pointX = left + Math.round(saturation * (BOARD_WIDTH - 1)), pointY = top + Math.round((1 - brightness) * (BOARD_HEIGHT - 1));
        border(graphics, pointX - 3, pointY - 3, 7, 7, 0xFF000000);
        border(graphics, pointX - 2, pointY - 2, 5, 5, 0xFFFFFFFF);
        int hueY = top + Math.round(hue * (BOARD_HEIGHT - 1));
        border(graphics, x + 212, hueY - 2, 26, 5, 0xFFFFFFFF);
        graphics.drawString(minecraft.font, text("omniwheel.color.hex"), x + 12, y + 143, 0xFF9CAAB8, false);
        graphics.drawCenteredString(minecraft.font, text("omniwheel.color.before"), x + 181, y + 143, 0xFF9CAAB8);
        graphics.drawCenteredString(minecraft.font, text("omniwheel.color.after"), x + 219, y + 143, 0xFF9CAAB8);
        graphics.fill(x + 12, y + 155, x + 150, y + 177, 0xFF111820);
        border(graphics, x + 12, y + 155, 138, 22, hex.length() != 6 ? 0xFFE4B96F : focus == 2 ? 0xFF8BDAF8 : 0xFF526575);
        if (focus == 2 && replaceHex) graphics.fill(x + 29, y + 161, x + 29 + minecraft.font.width(hex), y + 171, 0x80506B84);
        graphics.drawString(minecraft.font, "#" + hex, x + 20, y + 162, 0xFFF2F5F8, false);
        graphics.fill(x + 164, y + 155, x + 198, y + 177, original | 0xFF000000);
        graphics.fill(x + 202, y + 155, x + 236, y + 177, color());
        border(graphics, x + 164, y + 155, 34, 22, 0xFF526575);
        border(graphics, x + 202, y + 155, 34, 22, 0xFF526575);
        button(graphics, minecraft, x + 12, y + 198, 106, text("omniwheel.color.cancel"), 3, true, mouseX, mouseY);
        button(graphics, minecraft, x + 130, y + 198, 106, text("omniwheel.color.apply"), 4, hex.length() == 6, mouseX, mouseY);
    }
    private void button(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, String label, int index, boolean enabled, float mouseX, float mouseY) {
        boolean highlighted = enabled && (focus == index || inside(mouseX, mouseY, x, y, width, 22));
        graphics.fill(x, y, x + width, y + 22, highlighted ? 0xFF344657 : 0xFF222D39);
        border(graphics, x, y, width, 22, highlighted ? 0xFF8BDAF8 : 0xFF526575);
        graphics.drawCenteredString(minecraft.font, label, x + width / 2, y + 7, enabled ? 0xFFF2F5F8 : 0xFF53606C);
    }
    private static void border(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color); graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color); graphics.fill(x + width - 1, y, x + width, y + height, color);
    }
    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
    boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return true;
        if (inside(mouseX, mouseY, x + 12, y + 32, BOARD_WIDTH, BOARD_HEIGHT)) focus = dragging = 0;
        else if (inside(mouseX, mouseY, x + 214, y + 32, 22, BOARD_HEIGHT)) focus = dragging = 1;
        else if (inside(mouseX, mouseY, x + 12, y + 155, 138, 22)) { focus = 2; replaceHex = true; }
        else if (inside(mouseX, mouseY, x + 12, y + 198, 106, 22)) close();
        else if (inside(mouseX, mouseY, x + 130, y + 198, 106, 22)) apply();
        if (isOpen()) mouseDragged(mouseX, mouseY);
        return true;
    }
    boolean mouseDragged(double mouseX, double mouseY) {
        if (dragging == 0) {
            saturation = (float) de.artemis.omniwheel.common.Numbers.clamp((mouseX - x - 12) / (BOARD_WIDTH - 1), 0, 1);
            brightness = 1 - (float) de.artemis.omniwheel.common.Numbers.clamp((mouseY - y - 32) / (BOARD_HEIGHT - 1), 0, 1);
        } else if (dragging == 1) hue = (float) de.artemis.omniwheel.common.Numbers.clamp((mouseY - y - 32) / (BOARD_HEIGHT - 1), 0, 1);
        if (dragging >= 0) syncHex();
        return true;
    }
    boolean mouseReleased() { dragging = -1; return true; }
    boolean keyPressed(int key, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE) close();
        else if (key == GLFW.GLFW_KEY_TAB) focus = Math.floorMod(focus + ((modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? -1 : 1), 5);
        else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE && focus >= 3) {
            if (focus == 3) close(); else apply();
        } else if (focus == 2) {
            if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && key == GLFW.GLFW_KEY_V) {
                String pasted = Minecraft.getInstance().keyboardHandler.getClipboard().trim().replaceFirst("^#", "");
                if (pasted.matches("(?i)[0-9a-f]{6}")) { hex = pasted.toUpperCase(Locale.ROOT); setColor(Integer.parseInt(hex, 16)); replaceHex = true; }
            } else if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && key == GLFW.GLFW_KEY_A) replaceHex = true;
            else if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) {
                hex = replaceHex ? "" : hex.isEmpty() ? "" : hex.substring(0, hex.length() - 1); replaceHex = false;
            }
        } else if (focus <= 1) {
            float direction = key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_DOWN ? -1 : key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_UP ? 1 : 0;
            if (focus == 1) hue = (hue + direction / 100 + 1) % 1;
            else if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT) saturation = de.artemis.omniwheel.common.Numbers.clamp(saturation + direction / 100, 0, 1);
            else brightness = de.artemis.omniwheel.common.Numbers.clamp(brightness + direction / 100, 0, 1);
            if (direction != 0) syncHex();
        }
        return true;
    }
    boolean charTyped(char value) {
        if (focus == 2 && Character.digit(value, 16) >= 0 && (replaceHex || hex.length() < 6)) {
            if (replaceHex) hex = "";
            replaceHex = false; hex += Character.toUpperCase(value);
            if (hex.length() == 6) { String typed = hex; setColor(Integer.parseInt(hex, 16)); hex = typed; }
        }
        return true;
    }
}
