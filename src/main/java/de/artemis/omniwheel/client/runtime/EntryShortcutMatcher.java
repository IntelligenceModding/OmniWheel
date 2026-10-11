package de.artemis.omniwheel.client.runtime;

import net.minecraft.client.util.InputMappings;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class EntryShortcutMatcher {
    private EntryShortcutMatcher() {
    }

    public static ShortcutSpec parse(String shortcutText) {
        String raw = shortcutText == null ? "" : shortcutText.trim();
        if (raw.isEmpty()) {
            return null;
        }

        boolean ctrl = false;
        boolean shift = false;
        boolean alt = false;
        Set<Integer> keys = new LinkedHashSet<>();

        for (String part : raw.split("\\+")) {
            String token = normalizeToken(part);
            if (token.isEmpty()) {
                continue;
            }
            switch (token) {
                case "ctrl":
                case "control":
                case "strg": {
                    ctrl = true;
                    break;
                }
                case "shift":
                case "umschalt": {
                    shift = true;
                    break;
                }
                case "alt":
                case "option": {
                    alt = true;
                    break;
                }
                default: {

                    Integer key = resolveKey(token);
                    if (key == null) {
                        return null;
                    }
                    keys.add(key);

                    break;
                }
            }
        }

        return keys.isEmpty() ? null : new ShortcutSpec(ctrl, shift, alt, com.google.common.collect.ImmutableSet.copyOf(keys));
    }

    public static String captureShortcutText(long windowHandle, int keyCode, int modifiers) {
        if (isModifierOnlyKey(keyCode)
                || keyCode == GLFW.GLFW_KEY_ESCAPE
                || keyCode == GLFW.GLFW_KEY_ENTER
                || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            return null;
        }

        List<String> parts = new ArrayList<>();
        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            parts.add("Strg");
        }
        if ((modifiers & GLFW.GLFW_MOD_SHIFT) != 0) {
            parts.add("Shift");
        }
        if ((modifiers & GLFW.GLFW_MOD_ALT) != 0) {
            parts.add("Alt");
        }

        Set<Integer> pressedKeys = collectPressedKeys(windowHandle, keyCode);
        if (pressedKeys.isEmpty()) {
            return null;
        }

        for (int pressedKey : pressedKeys) {
            parts.add(labelForKey(pressedKey));
        }
        return String.join("+", parts);
    }

    private static String normalizeToken(String token) {
        return token == null
                ? ""
                : token.trim().toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "").replace("-", "");
    }

    private static Integer resolveKey(String token) {
        if (token.length() == 1) {
            char character = token.charAt(0);
            if (character >= 'a' && character <= 'z') {
                return GLFW.GLFW_KEY_A + (character - 'a');
            }
            if (character >= '0' && character <= '9') {
                return GLFW.GLFW_KEY_0 + (character - '0');
            }
        }

        if (token.startsWith("f")) {
            try {
                int functionIndex = Integer.parseInt(token.substring(1));
                if (functionIndex >= 1 && functionIndex <= 25) {
                    return GLFW.GLFW_KEY_F1 + (functionIndex - 1);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        if (token.startsWith("key")) {
            try {
                return Integer.parseInt(token.substring(3));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        switch (token) {
            case "space": {
                return GLFW.GLFW_KEY_SPACE;
            }
            case "tab": {
                return GLFW.GLFW_KEY_TAB;
            }
            case "enter":
            case "return": {
                return GLFW.GLFW_KEY_ENTER;
            }
            case "esc":
            case "escape": {
                return GLFW.GLFW_KEY_ESCAPE;
            }
            case "del":
            case "delete": {
                return GLFW.GLFW_KEY_DELETE;
            }
            case "backspace": {
                return GLFW.GLFW_KEY_BACKSPACE;
            }
            case "insert":
            case "ins": {
                return GLFW.GLFW_KEY_INSERT;
            }
            case "home": {
                return GLFW.GLFW_KEY_HOME;
            }
            case "end": {
                return GLFW.GLFW_KEY_END;
            }
            case "pageup":
            case "pgup": {
                return GLFW.GLFW_KEY_PAGE_UP;
            }
            case "pagedown":
            case "pgdn": {
                return GLFW.GLFW_KEY_PAGE_DOWN;
            }
            case "left": {
                return GLFW.GLFW_KEY_LEFT;
            }
            case "right": {
                return GLFW.GLFW_KEY_RIGHT;
            }
            case "up": {
                return GLFW.GLFW_KEY_UP;
            }
            case "down": {
                return GLFW.GLFW_KEY_DOWN;
            }
            case "capslock": {
                return GLFW.GLFW_KEY_CAPS_LOCK;
            }
            case "scrolllock": {
                return GLFW.GLFW_KEY_SCROLL_LOCK;
            }
            case "numlock": {
                return GLFW.GLFW_KEY_NUM_LOCK;
            }
            case "printscreen": {
                return GLFW.GLFW_KEY_PRINT_SCREEN;
            }
            case "pause": {
                return GLFW.GLFW_KEY_PAUSE;
            }
            case "menu":
            case "application": {
                return GLFW.GLFW_KEY_MENU;
            }
            case "world1": {
                return GLFW.GLFW_KEY_WORLD_1;
            }
            case "world2": {
                return GLFW.GLFW_KEY_WORLD_2;
            }
            case "comma": {
                return GLFW.GLFW_KEY_COMMA;
            }
            case "period":
            case "dot": {
                return GLFW.GLFW_KEY_PERIOD;
            }
            case "slash": {
                return GLFW.GLFW_KEY_SLASH;
            }
            case "semicolon": {
                return GLFW.GLFW_KEY_SEMICOLON;
            }
            case "apostrophe":
            case "quote": {
                return GLFW.GLFW_KEY_APOSTROPHE;
            }
            case "equal":
            case "equals": {
                return GLFW.GLFW_KEY_EQUAL;
            }
            case "minus": {
                return GLFW.GLFW_KEY_MINUS;
            }
            case "leftbracket":
            case "lbracket": {
                return GLFW.GLFW_KEY_LEFT_BRACKET;
            }
            case "rightbracket":
            case "rbracket": {
                return GLFW.GLFW_KEY_RIGHT_BRACKET;
            }
            case "backslash": {
                return GLFW.GLFW_KEY_BACKSLASH;
            }
            case "grave":
            case "backtick": {
                return GLFW.GLFW_KEY_GRAVE_ACCENT;
            }
            case "numpad0":
            case "num0":
            case "kp0": {
                return GLFW.GLFW_KEY_KP_0;
            }
            case "numpad1":
            case "num1":
            case "kp1": {
                return GLFW.GLFW_KEY_KP_1;
            }
            case "numpad2":
            case "num2":
            case "kp2": {
                return GLFW.GLFW_KEY_KP_2;
            }
            case "numpad3":
            case "num3":
            case "kp3": {
                return GLFW.GLFW_KEY_KP_3;
            }
            case "numpad4":
            case "num4":
            case "kp4": {
                return GLFW.GLFW_KEY_KP_4;
            }
            case "numpad5":
            case "num5":
            case "kp5": {
                return GLFW.GLFW_KEY_KP_5;
            }
            case "numpad6":
            case "num6":
            case "kp6": {
                return GLFW.GLFW_KEY_KP_6;
            }
            case "numpad7":
            case "num7":
            case "kp7": {
                return GLFW.GLFW_KEY_KP_7;
            }
            case "numpad8":
            case "num8":
            case "kp8": {
                return GLFW.GLFW_KEY_KP_8;
            }
            case "numpad9":
            case "num9":
            case "kp9": {
                return GLFW.GLFW_KEY_KP_9;
            }
            case "numpaddecimal":
            case "numdecimal":
            case "kpdecimal": {
                return GLFW.GLFW_KEY_KP_DECIMAL;
            }
            case "numpaddivide":
            case "numdivide":
            case "kpdivide": {
                return GLFW.GLFW_KEY_KP_DIVIDE;
            }
            case "numpadmultiply":
            case "nummultiply":
            case "kpmultiply": {
                return GLFW.GLFW_KEY_KP_MULTIPLY;
            }
            case "numpadsubtract":
            case "numsubtract":
            case "kpsubtract": {
                return GLFW.GLFW_KEY_KP_SUBTRACT;
            }
            case "numpadadd":
            case "numadd":
            case "kpadd":
            case "plus": {
                return GLFW.GLFW_KEY_KP_ADD;
            }
            case "numpadenter":
            case "numenter":
            case "kpenter": {
                return GLFW.GLFW_KEY_KP_ENTER;
            }
            case "numpadequal":
            case "numequal":
            case "kpequal": {
                return GLFW.GLFW_KEY_KP_EQUAL;
            }
            default: {
                return null;
            }
        }
    }

    private static boolean isModifierOnlyKey(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_LEFT_SHIFT
                || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT
                || keyCode == GLFW.GLFW_KEY_LEFT_CONTROL
                || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL
                || keyCode == GLFW.GLFW_KEY_LEFT_ALT
                || keyCode == GLFW.GLFW_KEY_RIGHT_ALT
                || keyCode == GLFW.GLFW_KEY_LEFT_SUPER
                || keyCode == GLFW.GLFW_KEY_RIGHT_SUPER;
    }

    private static Set<Integer> collectPressedKeys(long windowHandle, int keyCode) {
        Set<Integer> keys = new LinkedHashSet<>();
        for (int candidate = GLFW.GLFW_KEY_SPACE; candidate <= GLFW.GLFW_KEY_LAST; candidate++) {
            if (isModifierOnlyKey(candidate)) {
                continue;
            }
            if (InputMappings.isKeyDown(windowHandle, candidate)) {
                keys.add(candidate);
            }
        }
        keys.add(keyCode);
        return keys;
    }

    private static String labelForKey(int keyCode) {
        if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
            return Character.toString((char) ('A' + (keyCode - GLFW.GLFW_KEY_A)));
        }
        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
            return Character.toString((char) ('0' + (keyCode - GLFW.GLFW_KEY_0)));
        }
        if (keyCode >= GLFW.GLFW_KEY_F1 && keyCode <= GLFW.GLFW_KEY_F25) {
            return "F" + (keyCode - GLFW.GLFW_KEY_F1 + 1);
        }

        switch (keyCode) {
            case GLFW.GLFW_KEY_SPACE: {
                return "Space";
            }
            case GLFW.GLFW_KEY_TAB: {
                return "Tab";
            }
            case GLFW.GLFW_KEY_HOME: {
                return "Home";
            }
            case GLFW.GLFW_KEY_END: {
                return "End";
            }
            case GLFW.GLFW_KEY_PAGE_UP: {
                return "PageUp";
            }
            case GLFW.GLFW_KEY_PAGE_DOWN: {
                return "PageDown";
            }
            case GLFW.GLFW_KEY_UP: {
                return "Up";
            }
            case GLFW.GLFW_KEY_DOWN: {
                return "Down";
            }
            case GLFW.GLFW_KEY_LEFT: {
                return "Left";
            }
            case GLFW.GLFW_KEY_RIGHT: {
                return "Right";
            }
            case GLFW.GLFW_KEY_INSERT: {
                return "Insert";
            }
            case GLFW.GLFW_KEY_DELETE: {
                return "Delete";
            }
            case GLFW.GLFW_KEY_BACKSPACE: {
                return "Backspace";
            }
            case GLFW.GLFW_KEY_CAPS_LOCK: {
                return "CapsLock";
            }
            case GLFW.GLFW_KEY_SCROLL_LOCK: {
                return "ScrollLock";
            }
            case GLFW.GLFW_KEY_NUM_LOCK: {
                return "NumLock";
            }
            case GLFW.GLFW_KEY_PRINT_SCREEN: {
                return "PrintScreen";
            }
            case GLFW.GLFW_KEY_PAUSE: {
                return "Pause";
            }
            case GLFW.GLFW_KEY_MENU: {
                return "Menu";
            }
            case GLFW.GLFW_KEY_WORLD_1: {
                return "World1";
            }
            case GLFW.GLFW_KEY_WORLD_2: {
                return "World2";
            }
            case GLFW.GLFW_KEY_COMMA: {
                return "Comma";
            }
            case GLFW.GLFW_KEY_PERIOD: {
                return "Period";
            }
            case GLFW.GLFW_KEY_SLASH: {
                return "Slash";
            }
            case GLFW.GLFW_KEY_SEMICOLON: {
                return "Semicolon";
            }
            case GLFW.GLFW_KEY_APOSTROPHE: {
                return "Apostrophe";
            }
            case GLFW.GLFW_KEY_EQUAL: {
                return "Equal";
            }
            case GLFW.GLFW_KEY_MINUS: {
                return "Minus";
            }
            case GLFW.GLFW_KEY_LEFT_BRACKET: {
                return "LeftBracket";
            }
            case GLFW.GLFW_KEY_RIGHT_BRACKET: {
                return "RightBracket";
            }
            case GLFW.GLFW_KEY_BACKSLASH: {
                return "Backslash";
            }
            case GLFW.GLFW_KEY_GRAVE_ACCENT: {
                return "Grave";
            }
            case GLFW.GLFW_KEY_KP_0: {
                return "Numpad0";
            }
            case GLFW.GLFW_KEY_KP_1: {
                return "Numpad1";
            }
            case GLFW.GLFW_KEY_KP_2: {
                return "Numpad2";
            }
            case GLFW.GLFW_KEY_KP_3: {
                return "Numpad3";
            }
            case GLFW.GLFW_KEY_KP_4: {
                return "Numpad4";
            }
            case GLFW.GLFW_KEY_KP_5: {
                return "Numpad5";
            }
            case GLFW.GLFW_KEY_KP_6: {
                return "Numpad6";
            }
            case GLFW.GLFW_KEY_KP_7: {
                return "Numpad7";
            }
            case GLFW.GLFW_KEY_KP_8: {
                return "Numpad8";
            }
            case GLFW.GLFW_KEY_KP_9: {
                return "Numpad9";
            }
            case GLFW.GLFW_KEY_KP_DECIMAL: {
                return "NumpadDecimal";
            }
            case GLFW.GLFW_KEY_KP_DIVIDE: {
                return "NumpadDivide";
            }
            case GLFW.GLFW_KEY_KP_MULTIPLY: {
                return "NumpadMultiply";
            }
            case GLFW.GLFW_KEY_KP_SUBTRACT: {
                return "NumpadSubtract";
            }
            case GLFW.GLFW_KEY_KP_ADD: {
                return "NumpadAdd";
            }
            case GLFW.GLFW_KEY_KP_ENTER: {
                return "NumpadEnter";
            }
            case GLFW.GLFW_KEY_KP_EQUAL: {
                return "NumpadEqual";
            }
            default: {
                return "Key" + keyCode;
            }
        }
    }

    public static final class ShortcutSpec {
        private final boolean ctrl;
        private final boolean shift;
        private final boolean alt;
        private final Set<Integer> keys;

        public ShortcutSpec(boolean ctrl, boolean shift, boolean alt, Set<Integer> keys) {

            this.ctrl = ctrl;
            this.shift = shift;
            this.alt = alt;
            this.keys = keys;
        }

        public boolean ctrl() {
            return ctrl;
        }

        public boolean shift() {
            return shift;
        }

        public boolean alt() {
            return alt;
        }

        public Set<Integer> keys() {
            return keys;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof ShortcutSpec)) return false;
            ShortcutSpec that = (ShortcutSpec) other;
            return java.util.Objects.equals(ctrl, that.ctrl) && java.util.Objects.equals(shift, that.shift) && java.util.Objects.equals(alt, that.alt) && java.util.Objects.equals(keys, that.keys);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(ctrl, shift, alt, keys);
        }

        @Override
        public String toString() {
            return "ShortcutSpec[" + "ctrl=" + ctrl + ", " + "shift=" + shift + ", " + "alt=" + alt + ", " + "keys=" + keys + "]";
        }

        public boolean matches(long windowHandle) {
            if (ctrl != isCtrlDown(windowHandle) || shift != isShiftDown(windowHandle) || alt != isAltDown(windowHandle)) {
                return false;
            }
            for (int key : keys) {
                if (!InputMappings.isKeyDown(windowHandle, key)) {
                    return false;
                }
            }
            return true;
        }

        public int weight() {
            return keys.size() + (ctrl ? 1 : 0) + (shift ? 1 : 0) + (alt ? 1 : 0);
        }

        private static boolean isCtrlDown(long windowHandle) {
            return InputMappings.isKeyDown(windowHandle, GLFW.GLFW_KEY_LEFT_CONTROL)
                    || InputMappings.isKeyDown(windowHandle, GLFW.GLFW_KEY_RIGHT_CONTROL);
        }

        private static boolean isShiftDown(long windowHandle) {
            return InputMappings.isKeyDown(windowHandle, GLFW.GLFW_KEY_LEFT_SHIFT)
                    || InputMappings.isKeyDown(windowHandle, GLFW.GLFW_KEY_RIGHT_SHIFT);
        }

        private static boolean isAltDown(long windowHandle) {
            return InputMappings.isKeyDown(windowHandle, GLFW.GLFW_KEY_LEFT_ALT)
                    || InputMappings.isKeyDown(windowHandle, GLFW.GLFW_KEY_RIGHT_ALT);
        }
    }
}
