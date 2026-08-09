package de.artemis.omniwheel.client.runtime;

import com.mojang.blaze3d.platform.InputConstants;
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
                case "ctrl", "control", "strg" -> ctrl = true;
                case "shift", "umschalt" -> shift = true;
                case "alt", "option" -> alt = true;
                default -> {
                    Integer key = resolveKey(token);
                    if (key == null) {
                        return null;
                    }
                    keys.add(key);
                }
            }
        }

        return keys.isEmpty() ? null : new ShortcutSpec(ctrl, shift, alt, Set.copyOf(keys));
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

        return switch (token) {
            case "space" -> GLFW.GLFW_KEY_SPACE;
            case "tab" -> GLFW.GLFW_KEY_TAB;
            case "enter", "return" -> GLFW.GLFW_KEY_ENTER;
            case "esc", "escape" -> GLFW.GLFW_KEY_ESCAPE;
            case "del", "delete" -> GLFW.GLFW_KEY_DELETE;
            case "backspace" -> GLFW.GLFW_KEY_BACKSPACE;
            case "insert", "ins" -> GLFW.GLFW_KEY_INSERT;
            case "home" -> GLFW.GLFW_KEY_HOME;
            case "end" -> GLFW.GLFW_KEY_END;
            case "pageup", "pgup" -> GLFW.GLFW_KEY_PAGE_UP;
            case "pagedown", "pgdn" -> GLFW.GLFW_KEY_PAGE_DOWN;
            case "left" -> GLFW.GLFW_KEY_LEFT;
            case "right" -> GLFW.GLFW_KEY_RIGHT;
            case "up" -> GLFW.GLFW_KEY_UP;
            case "down" -> GLFW.GLFW_KEY_DOWN;
            case "capslock" -> GLFW.GLFW_KEY_CAPS_LOCK;
            case "scrolllock" -> GLFW.GLFW_KEY_SCROLL_LOCK;
            case "numlock" -> GLFW.GLFW_KEY_NUM_LOCK;
            case "printscreen" -> GLFW.GLFW_KEY_PRINT_SCREEN;
            case "pause" -> GLFW.GLFW_KEY_PAUSE;
            case "menu", "application" -> GLFW.GLFW_KEY_MENU;
            case "world1" -> GLFW.GLFW_KEY_WORLD_1;
            case "world2" -> GLFW.GLFW_KEY_WORLD_2;
            case "comma" -> GLFW.GLFW_KEY_COMMA;
            case "period", "dot" -> GLFW.GLFW_KEY_PERIOD;
            case "slash" -> GLFW.GLFW_KEY_SLASH;
            case "semicolon" -> GLFW.GLFW_KEY_SEMICOLON;
            case "apostrophe", "quote" -> GLFW.GLFW_KEY_APOSTROPHE;
            case "equal", "equals" -> GLFW.GLFW_KEY_EQUAL;
            case "minus" -> GLFW.GLFW_KEY_MINUS;
            case "leftbracket", "lbracket" -> GLFW.GLFW_KEY_LEFT_BRACKET;
            case "rightbracket", "rbracket" -> GLFW.GLFW_KEY_RIGHT_BRACKET;
            case "backslash" -> GLFW.GLFW_KEY_BACKSLASH;
            case "grave", "backtick" -> GLFW.GLFW_KEY_GRAVE_ACCENT;
            case "numpad0", "num0", "kp0" -> GLFW.GLFW_KEY_KP_0;
            case "numpad1", "num1", "kp1" -> GLFW.GLFW_KEY_KP_1;
            case "numpad2", "num2", "kp2" -> GLFW.GLFW_KEY_KP_2;
            case "numpad3", "num3", "kp3" -> GLFW.GLFW_KEY_KP_3;
            case "numpad4", "num4", "kp4" -> GLFW.GLFW_KEY_KP_4;
            case "numpad5", "num5", "kp5" -> GLFW.GLFW_KEY_KP_5;
            case "numpad6", "num6", "kp6" -> GLFW.GLFW_KEY_KP_6;
            case "numpad7", "num7", "kp7" -> GLFW.GLFW_KEY_KP_7;
            case "numpad8", "num8", "kp8" -> GLFW.GLFW_KEY_KP_8;
            case "numpad9", "num9", "kp9" -> GLFW.GLFW_KEY_KP_9;
            case "numpaddecimal", "numdecimal", "kpdecimal" -> GLFW.GLFW_KEY_KP_DECIMAL;
            case "numpaddivide", "numdivide", "kpdivide" -> GLFW.GLFW_KEY_KP_DIVIDE;
            case "numpadmultiply", "nummultiply", "kpmultiply" -> GLFW.GLFW_KEY_KP_MULTIPLY;
            case "numpadsubtract", "numsubtract", "kpsubtract" -> GLFW.GLFW_KEY_KP_SUBTRACT;
            case "numpadadd", "numadd", "kpadd", "plus" -> GLFW.GLFW_KEY_KP_ADD;
            case "numpadenter", "numenter", "kpenter" -> GLFW.GLFW_KEY_KP_ENTER;
            case "numpadequal", "numequal", "kpequal" -> GLFW.GLFW_KEY_KP_EQUAL;
            default -> null;
        };
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
            if (InputConstants.isKeyDown(windowHandle, candidate)) {
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

        return switch (keyCode) {
            case GLFW.GLFW_KEY_SPACE -> "Space";
            case GLFW.GLFW_KEY_TAB -> "Tab";
            case GLFW.GLFW_KEY_HOME -> "Home";
            case GLFW.GLFW_KEY_END -> "End";
            case GLFW.GLFW_KEY_PAGE_UP -> "PageUp";
            case GLFW.GLFW_KEY_PAGE_DOWN -> "PageDown";
            case GLFW.GLFW_KEY_UP -> "Up";
            case GLFW.GLFW_KEY_DOWN -> "Down";
            case GLFW.GLFW_KEY_LEFT -> "Left";
            case GLFW.GLFW_KEY_RIGHT -> "Right";
            case GLFW.GLFW_KEY_INSERT -> "Insert";
            case GLFW.GLFW_KEY_DELETE -> "Delete";
            case GLFW.GLFW_KEY_BACKSPACE -> "Backspace";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "CapsLock";
            case GLFW.GLFW_KEY_SCROLL_LOCK -> "ScrollLock";
            case GLFW.GLFW_KEY_NUM_LOCK -> "NumLock";
            case GLFW.GLFW_KEY_PRINT_SCREEN -> "PrintScreen";
            case GLFW.GLFW_KEY_PAUSE -> "Pause";
            case GLFW.GLFW_KEY_MENU -> "Menu";
            case GLFW.GLFW_KEY_WORLD_1 -> "World1";
            case GLFW.GLFW_KEY_WORLD_2 -> "World2";
            case GLFW.GLFW_KEY_COMMA -> "Comma";
            case GLFW.GLFW_KEY_PERIOD -> "Period";
            case GLFW.GLFW_KEY_SLASH -> "Slash";
            case GLFW.GLFW_KEY_SEMICOLON -> "Semicolon";
            case GLFW.GLFW_KEY_APOSTROPHE -> "Apostrophe";
            case GLFW.GLFW_KEY_EQUAL -> "Equal";
            case GLFW.GLFW_KEY_MINUS -> "Minus";
            case GLFW.GLFW_KEY_LEFT_BRACKET -> "LeftBracket";
            case GLFW.GLFW_KEY_RIGHT_BRACKET -> "RightBracket";
            case GLFW.GLFW_KEY_BACKSLASH -> "Backslash";
            case GLFW.GLFW_KEY_GRAVE_ACCENT -> "Grave";
            case GLFW.GLFW_KEY_KP_0 -> "Numpad0";
            case GLFW.GLFW_KEY_KP_1 -> "Numpad1";
            case GLFW.GLFW_KEY_KP_2 -> "Numpad2";
            case GLFW.GLFW_KEY_KP_3 -> "Numpad3";
            case GLFW.GLFW_KEY_KP_4 -> "Numpad4";
            case GLFW.GLFW_KEY_KP_5 -> "Numpad5";
            case GLFW.GLFW_KEY_KP_6 -> "Numpad6";
            case GLFW.GLFW_KEY_KP_7 -> "Numpad7";
            case GLFW.GLFW_KEY_KP_8 -> "Numpad8";
            case GLFW.GLFW_KEY_KP_9 -> "Numpad9";
            case GLFW.GLFW_KEY_KP_DECIMAL -> "NumpadDecimal";
            case GLFW.GLFW_KEY_KP_DIVIDE -> "NumpadDivide";
            case GLFW.GLFW_KEY_KP_MULTIPLY -> "NumpadMultiply";
            case GLFW.GLFW_KEY_KP_SUBTRACT -> "NumpadSubtract";
            case GLFW.GLFW_KEY_KP_ADD -> "NumpadAdd";
            case GLFW.GLFW_KEY_KP_ENTER -> "NumpadEnter";
            case GLFW.GLFW_KEY_KP_EQUAL -> "NumpadEqual";
            default -> "Key" + keyCode;
        };
    }

    public record ShortcutSpec(boolean ctrl, boolean shift, boolean alt, Set<Integer> keys) {
        public boolean matches(long windowHandle) {
            if (ctrl != isCtrlDown(windowHandle) || shift != isShiftDown(windowHandle) || alt != isAltDown(windowHandle)) {
                return false;
            }
            for (int key : keys) {
                if (!InputConstants.isKeyDown(windowHandle, key)) {
                    return false;
                }
            }
            return true;
        }

        public int weight() {
            return keys.size() + (ctrl ? 1 : 0) + (shift ? 1 : 0) + (alt ? 1 : 0);
        }

        private static boolean isCtrlDown(long windowHandle) {
            return InputConstants.isKeyDown(windowHandle, GLFW.GLFW_KEY_LEFT_CONTROL)
                    || InputConstants.isKeyDown(windowHandle, GLFW.GLFW_KEY_RIGHT_CONTROL);
        }

        private static boolean isShiftDown(long windowHandle) {
            return InputConstants.isKeyDown(windowHandle, GLFW.GLFW_KEY_LEFT_SHIFT)
                    || InputConstants.isKeyDown(windowHandle, GLFW.GLFW_KEY_RIGHT_SHIFT);
        }

        private static boolean isAltDown(long windowHandle) {
            return InputConstants.isKeyDown(windowHandle, GLFW.GLFW_KEY_LEFT_ALT)
                    || InputConstants.isKeyDown(windowHandle, GLFW.GLFW_KEY_RIGHT_ALT);
        }
    }
}
