package de.artemis.omniwheel.client.runtime;

import org.junit.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.Assert.*;

public class EntryShortcutPortTest {
    @Test
    public void parsesModifiersAndMultipleKeysOnJava8() {
        EntryShortcutMatcher.ShortcutSpec shortcut = EntryShortcutMatcher.parse("Ctrl+Shift+G+Numpad1");
        assertNotNull(shortcut);
        assertTrue(shortcut.ctrl());
        assertTrue(shortcut.shift());
        assertFalse(shortcut.alt());
        assertTrue(shortcut.keys().contains(GLFW.GLFW_KEY_G));
        assertTrue(shortcut.keys().contains(GLFW.GLFW_KEY_KP_1));
        assertEquals(shortcut, EntryShortcutMatcher.parse("Shift+Control+Num1+G"));
        assertThrows(UnsupportedOperationException.class, () -> shortcut.keys().clear());
    }

    @Test
    public void rejectsShortcutsWithoutARecognizedKey() {
        assertNull(EntryShortcutMatcher.parse("Ctrl+Shift"));
        assertNull(EntryShortcutMatcher.parse("NotAKey"));
        assertNull(EntryShortcutMatcher.parse(""));
    }
}
