package de.artemis.omniwheel.client.runtime;

import com.mojang.blaze3d.platform.InputConstants;
import de.artemis.omniwheel.client.input.ShortcutInputState;
import org.junit.After;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

public final class EntryShortcutMatcherTest {
    @After public void releaseInputs() { ShortcutInputState.clear(); }

    @Test public void existingKeyboardShortcutsKeepTheirMeaning() {
        var shortcut = EntryShortcutMatcher.parse("Strg+Shift+G");
        assertEquals(EntryShortcutMatcher.parse("Ctrl+Shift+g"), shortcut);
        assertEquals(Set.of(InputConstants.KEY_G), shortcut.keys());
        assertTrue(shortcut.ctrl());
        assertTrue(shortcut.shift());
        assertTrue(shortcut.mouseButtons().isEmpty());
        assertTrue(shortcut.scanCodes().isEmpty());
    }

    @Test public void mixedInputsRemainDistinctAndModifiersAffectPriority() {
        var shortcut = EntryShortcutMatcher.parse("Win+Alt+F17+Mouse4+Scan511");
        assertTrue(shortcut.superKey());
        assertTrue(shortcut.alt());
        assertEquals(Set.of(InputConstants.KEY_F17), shortcut.keys());
        assertEquals(Set.of(InputConstants.MOUSE_BUTTON_4), shortcut.mouseButtons());
        assertEquals(Set.of(511), shortcut.scanCodes());
        assertEquals(5, shortcut.weight());
        assertNotEquals(EntryShortcutMatcher.parse("Mouse4"), EntryShortcutMatcher.parse("Key68"));
        assertNotEquals(EntryShortcutMatcher.parse("Mouse4"), EntryShortcutMatcher.parse("Scan4"));
        assertEquals(Set.of(InputConstants.KEY_F24), EntryShortcutMatcher.parse("F24").keys());
    }

    @Test public void malformedInputsCannotReachNativePolling() {
        for (String invalid : new String[]{"", "Ctrl", "Mouse0", "Mouse", "Scan", "Scan-1", "Mouse-4", "Mouse99999999999", "Key99999", "F25", "F26", "G1"}) {
            assertNull(invalid, EntryShortcutMatcher.parse(invalid));
        }
    }

    @Test public void physicalKeysAndExtraButtonsTrackPressReleaseAndFocusReset() {
        ShortcutInputState.keyboard(511, InputConstants.PRESS);
        ShortcutInputState.mouse(4, InputConstants.PRESS);
        assertTrue(ShortcutInputState.scanDown(511));
        assertTrue(ShortcutInputState.mouseDown(4));
        ShortcutInputState.keyboard(511, InputConstants.REPEAT);
        ShortcutInputState.keyboard(511, InputConstants.RELEASE);
        ShortcutInputState.mouse(4, InputConstants.RELEASE);
        assertFalse(ShortcutInputState.scanDown(511));
        assertFalse(ShortcutInputState.mouseDown(4));
        ShortcutInputState.keyboard(512, InputConstants.PRESS);
        ShortcutInputState.mouse(6, InputConstants.PRESS);
        ShortcutInputState.clear();
        assertFalse(ShortcutInputState.scanDown(512));
        assertTrue(ShortcutInputState.mouseButtons().isEmpty());
    }

    @Test public void sdlKeyRangesPreserveDigitsAndFunctionKeys() {
        assertEquals(Set.of(InputConstants.KEY_0), EntryShortcutMatcher.parse("0").keys());
        assertEquals(Set.of(InputConstants.KEY_1), EntryShortcutMatcher.parse("1").keys());
        assertEquals(Set.of(InputConstants.KEY_9), EntryShortcutMatcher.parse("9").keys());
        assertEquals(Set.of(InputConstants.KEY_F12), EntryShortcutMatcher.parse("F12").keys());
        assertEquals(Set.of(InputConstants.KEY_F13), EntryShortcutMatcher.parse("F13").keys());
        assertEquals(Set.of(InputConstants.KEY_F24), EntryShortcutMatcher.parse("F24").keys());
    }

    @Test public void savedMouseShortcutsKeepRightAndMiddleButtons() {
        assertEquals(Set.of(InputConstants.MOUSE_BUTTON_LEFT), EntryShortcutMatcher.parse("Mouse1").mouseButtons());
        assertEquals(Set.of(InputConstants.MOUSE_BUTTON_RIGHT), EntryShortcutMatcher.parse("Mouse2").mouseButtons());
        assertEquals(Set.of(InputConstants.MOUSE_BUTTON_MIDDLE), EntryShortcutMatcher.parse("Mouse3").mouseButtons());
    }
}
