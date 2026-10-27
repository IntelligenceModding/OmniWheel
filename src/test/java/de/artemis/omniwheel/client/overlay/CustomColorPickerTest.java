package de.artemis.omniwheel.client.overlay;

import org.junit.Test;
import com.mojang.blaze3d.platform.InputConstants;

import static org.junit.Assert.*;

public class CustomColorPickerTest {
    @Test public void hexEntryAppliesExactColorOnlyOnConfirmation() {
        var picker = new CustomColorPicker();
        int[] saved = {0xFF123456};
        picker.open("Color", saved[0], value -> saved[0] = value);
        picker.keyPressed(InputConstants.KEY_TAB, 0); picker.keyPressed(InputConstants.KEY_TAB, 0);
        for (char value : "A12BC3".toCharArray()) picker.charTyped(value);
        assertEquals(0xFF123456, saved[0]);
        picker.keyPressed(InputConstants.KEY_RETURN, 0);
        assertEquals(0xFFA12BC3, saved[0]);
        assertFalse(picker.isOpen());
    }

    @Test public void cancelAndIncompleteHexDoNotChangeTheEntry() {
        var picker = new CustomColorPicker();
        int[] saved = {0xFF123456};
        picker.open("Color", saved[0], value -> saved[0] = value);
        picker.keyPressed(InputConstants.KEY_TAB, 0); picker.keyPressed(InputConstants.KEY_TAB, 0);
        picker.charTyped('F'); picker.keyPressed(InputConstants.KEY_RETURN, 0);
        assertTrue(picker.isOpen());
        picker.keyPressed(InputConstants.KEY_ESCAPE, 0);
        assertFalse(picker.isOpen());
        assertEquals(0xFF123456, saved[0]);
    }

    @Test public void keyboardCanChooseAColorWithoutHexFormatting() {
        var picker = new CustomColorPicker();
        int[] saved = {0xFF123456};
        picker.open("Color", saved[0], value -> saved[0] = value);
        for (int step = 0; step < 20; step++) picker.keyPressed(InputConstants.KEY_RIGHT, 0);
        picker.keyPressed(InputConstants.KEY_RETURN, 0);
        assertFalse(picker.isOpen());
        assertNotEquals(0xFF123456, saved[0]);
        assertEquals(255, saved[0] >>> 24);
    }
}
