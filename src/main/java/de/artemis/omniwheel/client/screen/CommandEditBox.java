package de.artemis.omniwheel.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/** Exposes the older edit box's position and focus API to the shared editor. */
public class CommandEditBox extends EditBox {
    public CommandEditBox(Font font, int x, int y, int width, int height, Component label) {
        super(font, x, y, width, height, label);
    }

    @Override public void setFocused(boolean focused) { super.setFocused(focused); }
    public int getX() { return x; }
    public int getY() { return y; }
    public void setY(int value) { y = value; }
}
