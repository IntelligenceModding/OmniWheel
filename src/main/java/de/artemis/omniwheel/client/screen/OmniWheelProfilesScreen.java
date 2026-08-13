package de.artemis.omniwheel.client.screen;

import de.artemis.omniwheel.client.overlay.OmniWheelProfilesOverlay;
import de.artemis.omniwheel.client.runtime.OmniWheelClientRuntime;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class OmniWheelProfilesScreen extends Screen {
    private final OmniWheelProfilesOverlay editor;
    private final Screen parent;

    public OmniWheelProfilesScreen(OmniWheelClientRuntime runtime, Screen parent) {
        super(Component.literal("OmniWheel Manager"));
        this.editor = new OmniWheelProfilesOverlay(runtime);
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
    public boolean keyPressed(KeyEvent event) {
        if (editor.keyPressed(event.key(), event.scancode(), event.modifiers())) {
            return true;
        }
        if (event.key() == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return editor.charTyped((char) event.codepoint(), event.modifiers()) || super.charTyped(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return editor.mouseClicked(event.x(), event.y(), event.button()) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return editor.mouseReleased(event.x(), event.y(), event.button()) || super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return editor.mouseDragged(event.x(), event.y(), event.button()) || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return editor.mouseScrolled(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        editor.render(graphics);
    }
}
