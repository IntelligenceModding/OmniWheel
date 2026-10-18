package de.artemis.omniwheel.client.screen;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.ITextComponent;

/**
 * Exposes the older edit box's position and focus API to the shared editor.
 */
public class CommandEditBox extends TextFieldWidget {

    public CommandEditBox(FontRenderer font, int x, int y, int width, int height, ITextComponent label) {
        super(font, x, y, width, height, label);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setY(int value) {
        y = value;
    }
}
