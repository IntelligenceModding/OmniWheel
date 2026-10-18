package de.artemis.omniwheel.client.overlay;

import org.junit.Test;
import static org.junit.Assert.*;

public class CustomColorPickerTest {

    @Test
    public void hexEntryAppliesExactColorOnlyOnConfirmation() {
        de.artemis.omniwheel.client.overlay.CustomColorPicker picker = new CustomColorPicker();
        int[] saved = { 0xFF123456 };
        picker.open("Color", saved[0], value -> saved[0] = value);
        picker.keyPressed(258, 0);
        picker.keyPressed(258, 0);
        for (char value : "A12BC3".toCharArray()) picker.charTyped(value);
        assertEquals(0xFF123456, saved[0]);
        picker.keyPressed(257, 0);
        assertEquals(0xFFA12BC3, saved[0]);
        assertFalse(picker.isOpen());
    }

    @Test
    public void cancelAndIncompleteHexDoNotChangeTheEntry() {
        de.artemis.omniwheel.client.overlay.CustomColorPicker picker = new CustomColorPicker();
        int[] saved = { 0xFF123456 };
        picker.open("Color", saved[0], value -> saved[0] = value);
        picker.keyPressed(258, 0);
        picker.keyPressed(258, 0);
        picker.charTyped('F');
        picker.keyPressed(257, 0);
        assertTrue(picker.isOpen());
        picker.keyPressed(256, 0);
        assertFalse(picker.isOpen());
        assertEquals(0xFF123456, saved[0]);
    }

    @Test
    public void keyboardCanChooseAColorWithoutHexFormatting() {
        de.artemis.omniwheel.client.overlay.CustomColorPicker picker = new CustomColorPicker();
        int[] saved = { 0xFF123456 };
        picker.open("Color", saved[0], value -> saved[0] = value);
        for (int step = 0; step < 20; step++) picker.keyPressed(262, 0);
        picker.keyPressed(257, 0);
        assertFalse(picker.isOpen());
        assertNotEquals(0xFF123456, saved[0]);
        assertEquals(255, saved[0] >>> 24);
    }
}
