package de.artemis.omniwheel.client.screen;

import de.artemis.omniwheel.client.overlay.OmniWheelProfilesOverlay;
import de.artemis.omniwheel.client.runtime.OmniWheelClientRuntime;
import de.artemis.omniwheel.common.OmniWheelText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

public final class OmniWheelProfilesScreen extends Screen {
    private final OmniWheelProfilesOverlay editor;
    private final Screen parent;
    private long lastClickTime;
    private double lastClickX, lastClickY;
    private int lastClickButton = -1;

    public OmniWheelProfilesScreen(OmniWheelClientRuntime runtime, Screen parent) {
        super(OmniWheelText.component("omniwheel.screen.profile_manager"));
        this.editor = new OmniWheelProfilesOverlay(runtime);
        this.editor.captureIconSources(parent);
        this.parent = parent;
    }

    @Override
    public void added() {
        super.added();
        editor.openForScreen();
    }

    @Override
    public void removed() {
        editor.closeForScreen();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public boolean isInGameUi() {
        return this.minecraft != null && this.minecraft.level != null;
    }

    @Override
    public void tick() {
        editor.tick();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            editor.persistPendingEdits();
            this.minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editor.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return editor.charTyped(codePoint, modifiers) || super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        long now = net.minecraft.Util.getMillis();
        boolean doubleClick = button == lastClickButton && now - lastClickTime < 250
                && Math.abs(mouseX - lastClickX) < 4 && Math.abs(mouseY - lastClickY) < 4;
        lastClickTime = now;
        lastClickX = mouseX;
        lastClickY = mouseY;
        lastClickButton = doubleClick ? -1 : button;
        return editor.mouseClicked(mouseX, mouseY, button, doubleClick) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return editor.mouseReleased(mouseX, mouseY, button) || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return editor.mouseDragged(mouseX, mouseY, button) || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return editor.mouseScrolled(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft == null || this.minecraft.level == null) {
            super.renderBackground(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        editor.render(graphics);
    }
}
