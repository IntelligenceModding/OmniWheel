package de.artemis.omniwheel.client.runtime;

import de.artemis.omniwheel.client.input.ShortcutInputState;
import org.junit.After;
import org.junit.Test;
import org.lwjgl.glfw.GLFW;
import java.util.Set;
import static org.junit.Assert.*;

public final class EntryShortcutMatcherTest {

    @After
    public void releaseInputs() {
        ShortcutInputState.clear();
    }

    @Test
    public void existingKeyboardShortcutsKeepTheirMeaning() {
        de.artemis.omniwheel.client.runtime.EntryShortcutMatcher.ShortcutSpec shortcut = EntryShortcutMatcher.parse("Strg+Shift+G");
        assertEquals(EntryShortcutMatcher.parse("Ctrl+Shift+g"), shortcut);
        assertEquals(de.artemis.omniwheel.common.Java8.set(GLFW.GLFW_KEY_G), shortcut.keys());
        assertTrue(shortcut.ctrl());
        assertTrue(shortcut.shift());
        assertTrue(shortcut.mouseButtons().isEmpty());
        assertTrue(shortcut.scanCodes().isEmpty());
    }

    @Test
    public void mixedInputsRemainDistinctAndModifiersAffectPriority() {
        de.artemis.omniwheel.client.runtime.EntryShortcutMatcher.ShortcutSpec shortcut = EntryShortcutMatcher.parse("Win+Alt+F17+Mouse4+Scan511");
        assertTrue(shortcut.superKey());
        assertTrue(shortcut.alt());
        assertEquals(de.artemis.omniwheel.common.Java8.set(GLFW.GLFW_KEY_F17), shortcut.keys());
        assertEquals(de.artemis.omniwheel.common.Java8.set(3), shortcut.mouseButtons());
        assertEquals(de.artemis.omniwheel.common.Java8.set(511), shortcut.scanCodes());
        assertEquals(5, shortcut.weight());
        assertNotEquals(EntryShortcutMatcher.parse("Mouse4"), EntryShortcutMatcher.parse("Key68"));
        assertNotEquals(EntryShortcutMatcher.parse("Mouse4"), EntryShortcutMatcher.parse("Scan4"));
        assertEquals(de.artemis.omniwheel.common.Java8.set(GLFW.GLFW_KEY_F25), EntryShortcutMatcher.parse("F25").keys());
    }

    @Test
    public void malformedInputsCannotReachNativePolling() {
        for (String invalid : new String[] { "", "Ctrl", "Mouse0", "Mouse", "Scan", "Scan-1", "Mouse-4", "Mouse99999999999", "Key99999", "F26", "G1" }) {
            assertNull(invalid, EntryShortcutMatcher.parse(invalid));
        }
    }

    @Test
    public void unknownKeysAndExtraButtonsTrackPressReleaseAndFocusReset() {
        ShortcutInputState.keyboard(GLFW.GLFW_KEY_UNKNOWN, 511, GLFW.GLFW_PRESS);
        ShortcutInputState.mouse(4, GLFW.GLFW_PRESS);
        assertTrue(ShortcutInputState.scanDown(511));
        assertTrue(ShortcutInputState.unknownScans().contains(511));
        assertTrue(ShortcutInputState.mouseDown(4));
        ShortcutInputState.keyboard(GLFW.GLFW_KEY_UNKNOWN, 511, GLFW.GLFW_REPEAT);
        ShortcutInputState.keyboard(GLFW.GLFW_KEY_UNKNOWN, 511, GLFW.GLFW_RELEASE);
        ShortcutInputState.mouse(4, GLFW.GLFW_RELEASE);
        assertFalse(ShortcutInputState.scanDown(511));
        assertFalse(ShortcutInputState.mouseDown(4));
        ShortcutInputState.keyboard(GLFW.GLFW_KEY_UNKNOWN, 512, GLFW.GLFW_PRESS);
        ShortcutInputState.mouse(6, GLFW.GLFW_PRESS);
        ShortcutInputState.clear();
        assertTrue(ShortcutInputState.unknownScans().isEmpty());
        assertTrue(ShortcutInputState.mouseButtons().isEmpty());
    }
}
