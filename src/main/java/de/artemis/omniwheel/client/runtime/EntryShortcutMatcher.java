package de.artemis.omniwheel.client.runtime;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.sdl.SDLScancode;
import com.mojang.blaze3d.platform.Window;
import de.artemis.omniwheel.client.input.ShortcutInputState;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

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
        boolean superKey = false;
        Set<Integer> keys = new LinkedHashSet<>();
        Set<Integer> mouseButtons = new LinkedHashSet<>();
        Set<Integer> scanCodes = new LinkedHashSet<>();

        for (String part : raw.split("\\+")) {
            String token = normalizeToken(part);
            if ((token.startsWith("mouse") || token.startsWith("scan") || token.startsWith("key")) && part.contains("-")) return null;
            if (token.isEmpty()) {
                continue;
            }
            switch (token) {
                case "ctrl", "control", "strg" -> ctrl = true;
                case "shift", "umschalt" -> shift = true;
                case "alt", "option" -> alt = true;
                case "super", "win", "windows", "cmd", "command" -> superKey = true;
                default -> {
                    if (token.startsWith("mouse") || token.startsWith("scan")) {
                        boolean mouse = token.startsWith("mouse");
                        Integer code = parseCode(token.substring(mouse ? 5 : 4));
                        if (code == null || mouse && code == 0) return null;
                        (mouse ? mouseButtons : scanCodes).add(mouse ? mouseButton(code) : code);
                        continue;
                    }
                    Integer key = resolveKey(token);
                    if (key == null) {
                        return null;
                    }
                    keys.add(key);
                }
            }
        }

        return keys.isEmpty() && mouseButtons.isEmpty() && scanCodes.isEmpty() ? null
                : new ShortcutSpec(ctrl, shift, alt, superKey, Set.copyOf(keys), Set.copyOf(mouseButtons), Set.copyOf(scanCodes));
    }

    private static Integer parseCode(String token) {
        try {
            int code = Integer.parseInt(token);
            return code >= 0 && code < Integer.MAX_VALUE ? code : null;
        } catch (NumberFormatException ignored) { return null; }
    }

    public static String captureShortcutText(Window window, int keyCode, int modifiers) {
        return captureShortcutText(window, keyCode, -1, modifiers);
    }

    public static String captureShortcutText(Window window, int keyCode, int scanCode, int modifiers) {
        if (isModifierOnlyKey(keyCode)
                || keyCode == InputConstants.KEY_ESCAPE
                || keyCode == InputConstants.KEY_RETURN
                || keyCode == InputConstants.KEY_NUMPADENTER) {
            return null;
        }

        List<String> parts = modifierLabels(modifiers);
        int modifierCount = parts.size();
        Set<Integer> pressedKeys = collectPressedKeys(window, keyCode);
        for (int pressedKey : pressedKeys) parts.add(labelForKey(pressedKey));
        for (int button : new TreeSet<>(ShortcutInputState.mouseButtons())) parts.add("Mouse" + mouseLabel(button));
        return parts.size() == modifierCount ? null : String.join("+", parts);
    }

    public static String captureMouseShortcutText(Window window, int button) {
        if (button <= 0 || button == Integer.MAX_VALUE) return null;
        int modifiers = 0;
        if (ShortcutSpec.isCtrlDown(window)) modifiers |= InputConstants.MOD_CONTROL;
        if (ShortcutSpec.isShiftDown(window)) modifiers |= InputConstants.MOD_SHIFT;
        if (ShortcutSpec.isAltDown(window)) modifiers |= InputConstants.MOD_ALT;
        if (ShortcutSpec.isSuperDown(window)) modifiers |= InputConstants.MOD_SUPER;
        List<String> parts = modifierLabels(modifiers);
        for (int key : collectPressedKeys(window, -1)) parts.add(labelForKey(key));
        Set<Integer> buttons = new TreeSet<>(ShortcutInputState.mouseButtons());
        buttons.add(button);
        for (int pressed : buttons) parts.add("Mouse" + mouseLabel(pressed));
        return String.join("+", parts);
    }

    // Keep saved Mouse2 (right) and Mouse3 (middle) shortcuts compatible with older profiles.
    private static int mouseButton(int label) {
        return switch (label) {
            case 2 -> InputConstants.MOUSE_BUTTON_RIGHT;
            case 3 -> InputConstants.MOUSE_BUTTON_MIDDLE;
            default -> label;
        };
    }

    private static int mouseLabel(int button) {
        return button == InputConstants.MOUSE_BUTTON_RIGHT ? 2
                : button == InputConstants.MOUSE_BUTTON_MIDDLE ? 3 : button;
    }

    private static List<String> modifierLabels(int modifiers) {
        List<String> parts = new ArrayList<>();
        if ((modifiers & InputConstants.MOD_CONTROL) != 0) {
            parts.add("Strg");
        }
        if ((modifiers & InputConstants.MOD_SHIFT) != 0) {
            parts.add("Shift");
        }
        if ((modifiers & InputConstants.MOD_ALT) != 0) {
            parts.add("Alt");
        }

        if ((modifiers & InputConstants.MOD_SUPER) != 0) parts.add("Super");
        return parts;
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
                return InputConstants.KEY_A + (character - 'a');
            }
            if (character >= '0' && character <= '9') {
                return character == '0' ? InputConstants.KEY_0 : InputConstants.KEY_1 + (character - '1');
            }
        }

        if (token.startsWith("f")) {
            try {
                int functionIndex = Integer.parseInt(token.substring(1));
                if (functionIndex >= 1 && functionIndex <= 24) {
                    return functionIndex <= 12 ? InputConstants.KEY_F1 + functionIndex - 1 : InputConstants.KEY_F13 + functionIndex - 13;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        if (token.startsWith("key")) {
            try {
                int key = Integer.parseInt(token.substring(3));
                return key > SDLScancode.SDL_SCANCODE_UNKNOWN && key <= (SDLScancode.SDL_SCANCODE_COUNT - 1) ? key : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return switch (token) {
            case "space" -> InputConstants.KEY_SPACE;
            case "tab" -> InputConstants.KEY_TAB;
            case "enter", "return" -> InputConstants.KEY_RETURN;
            case "esc", "escape" -> InputConstants.KEY_ESCAPE;
            case "del", "delete" -> InputConstants.KEY_DELETE;
            case "backspace" -> InputConstants.KEY_BACKSPACE;
            case "insert", "ins" -> InputConstants.KEY_INSERT;
            case "home" -> InputConstants.KEY_HOME;
            case "end" -> InputConstants.KEY_END;
            case "pageup", "pgup" -> InputConstants.KEY_PAGEUP;
            case "pagedown", "pgdn" -> InputConstants.KEY_PAGEDOWN;
            case "left" -> InputConstants.KEY_LEFT;
            case "right" -> InputConstants.KEY_RIGHT;
            case "up" -> InputConstants.KEY_UP;
            case "down" -> InputConstants.KEY_DOWN;
            case "capslock" -> InputConstants.KEY_CAPSLOCK;
            case "scrolllock" -> InputConstants.KEY_SCROLLLOCK;
            case "numlock" -> InputConstants.KEY_NUMLOCK;
            case "printscreen" -> InputConstants.KEY_PRINTSCREEN;
            case "pause" -> InputConstants.KEY_PAUSE;
            case "menu", "application" -> SDLScancode.SDL_SCANCODE_APPLICATION;
            case "world1" -> SDLScancode.SDL_SCANCODE_INTERNATIONAL1;
            case "world2" -> SDLScancode.SDL_SCANCODE_INTERNATIONAL2;
            case "comma" -> InputConstants.KEY_COMMA;
            case "period", "dot" -> InputConstants.KEY_PERIOD;
            case "slash" -> InputConstants.KEY_SLASH;
            case "semicolon" -> InputConstants.KEY_SEMICOLON;
            case "apostrophe", "quote" -> InputConstants.KEY_APOSTROPHE;
            case "equal", "equals" -> InputConstants.KEY_EQUALS;
            case "minus" -> InputConstants.KEY_MINUS;
            case "leftbracket", "lbracket" -> InputConstants.KEY_LBRACKET;
            case "rightbracket", "rbracket" -> InputConstants.KEY_RBRACKET;
            case "backslash" -> InputConstants.KEY_BACKSLASH;
            case "grave", "backtick" -> InputConstants.KEY_GRAVE;
            case "numpad0", "num0", "kp0" -> InputConstants.KEY_NUMPAD0;
            case "numpad1", "num1", "kp1" -> InputConstants.KEY_NUMPAD1;
            case "numpad2", "num2", "kp2" -> InputConstants.KEY_NUMPAD2;
            case "numpad3", "num3", "kp3" -> InputConstants.KEY_NUMPAD3;
            case "numpad4", "num4", "kp4" -> InputConstants.KEY_NUMPAD4;
            case "numpad5", "num5", "kp5" -> InputConstants.KEY_NUMPAD5;
            case "numpad6", "num6", "kp6" -> InputConstants.KEY_NUMPAD6;
            case "numpad7", "num7", "kp7" -> InputConstants.KEY_NUMPAD7;
            case "numpad8", "num8", "kp8" -> InputConstants.KEY_NUMPAD8;
            case "numpad9", "num9", "kp9" -> InputConstants.KEY_NUMPAD9;
            case "numpaddecimal", "numdecimal", "kpdecimal" -> SDLScancode.SDL_SCANCODE_KP_PERIOD;
            case "numpaddivide", "numdivide", "kpdivide" -> SDLScancode.SDL_SCANCODE_KP_DIVIDE;
            case "numpadmultiply", "nummultiply", "kpmultiply" -> InputConstants.KEY_MULTIPLY;
            case "numpadsubtract", "numsubtract", "kpsubtract" -> SDLScancode.SDL_SCANCODE_KP_MINUS;
            case "numpadadd", "numadd", "kpadd", "plus" -> InputConstants.KEY_ADD;
            case "numpadenter", "numenter", "kpenter" -> InputConstants.KEY_NUMPADENTER;
            case "numpadequal", "numequal", "kpequal" -> InputConstants.KEY_NUMPADEQUALS;
            default -> null;
        };
    }

    private static boolean isModifierOnlyKey(int keyCode) {
        return keyCode == InputConstants.KEY_LSHIFT
                || keyCode == InputConstants.KEY_RSHIFT
                || keyCode == InputConstants.KEY_LCONTROL
                || keyCode == InputConstants.KEY_RCONTROL
                || keyCode == InputConstants.KEY_LALT
                || keyCode == InputConstants.KEY_RALT
                || keyCode == InputConstants.KEY_LGUI
                || keyCode == InputConstants.KEY_RGUI;
    }

    private static Set<Integer> collectPressedKeys(Window window, int keyCode) {
        Set<Integer> keys = new LinkedHashSet<>();
        for (int candidate = 1; candidate <= (SDLScancode.SDL_SCANCODE_COUNT - 1); candidate++) {
            if (isModifierOnlyKey(candidate)) {
                continue;
            }
            if (ShortcutInputState.keyDown(window, candidate)) {
                keys.add(candidate);
            }
        }
        if (keyCode > SDLScancode.SDL_SCANCODE_UNKNOWN && keyCode <= (SDLScancode.SDL_SCANCODE_COUNT - 1)) keys.add(keyCode);
        return keys;
    }

    private static String labelForKey(int keyCode) {
        if (keyCode >= InputConstants.KEY_A && keyCode <= InputConstants.KEY_Z) {
            return Character.toString((char) ('A' + (keyCode - InputConstants.KEY_A)));
        }
        if (keyCode == InputConstants.KEY_0 || keyCode >= InputConstants.KEY_1 && keyCode <= InputConstants.KEY_9) {
            return keyCode == InputConstants.KEY_0 ? "0" : Character.toString((char) ('1' + keyCode - InputConstants.KEY_1));
        }
        if (keyCode >= InputConstants.KEY_F1 && keyCode <= InputConstants.KEY_F12) {
            return "F" + (keyCode - InputConstants.KEY_F1 + 1);
        }

        if (keyCode >= InputConstants.KEY_F13 && keyCode <= InputConstants.KEY_F24) return "F" + (keyCode - InputConstants.KEY_F13 + 13);

        return switch (keyCode) {
            case InputConstants.KEY_SPACE -> "Space";
            case InputConstants.KEY_TAB -> "Tab";
            case InputConstants.KEY_HOME -> "Home";
            case InputConstants.KEY_END -> "End";
            case InputConstants.KEY_PAGEUP -> "PageUp";
            case InputConstants.KEY_PAGEDOWN -> "PageDown";
            case InputConstants.KEY_UP -> "Up";
            case InputConstants.KEY_DOWN -> "Down";
            case InputConstants.KEY_LEFT -> "Left";
            case InputConstants.KEY_RIGHT -> "Right";
            case InputConstants.KEY_INSERT -> "Insert";
            case InputConstants.KEY_DELETE -> "Delete";
            case InputConstants.KEY_BACKSPACE -> "Backspace";
            case InputConstants.KEY_CAPSLOCK -> "CapsLock";
            case InputConstants.KEY_SCROLLLOCK -> "ScrollLock";
            case InputConstants.KEY_NUMLOCK -> "NumLock";
            case InputConstants.KEY_PRINTSCREEN -> "PrintScreen";
            case InputConstants.KEY_PAUSE -> "Pause";
            case SDLScancode.SDL_SCANCODE_APPLICATION -> "Menu";
            case SDLScancode.SDL_SCANCODE_INTERNATIONAL1 -> "World1";
            case SDLScancode.SDL_SCANCODE_INTERNATIONAL2 -> "World2";
            case InputConstants.KEY_COMMA -> "Comma";
            case InputConstants.KEY_PERIOD -> "Period";
            case InputConstants.KEY_SLASH -> "Slash";
            case InputConstants.KEY_SEMICOLON -> "Semicolon";
            case InputConstants.KEY_APOSTROPHE -> "Apostrophe";
            case InputConstants.KEY_EQUALS -> "Equal";
            case InputConstants.KEY_MINUS -> "Minus";
            case InputConstants.KEY_LBRACKET -> "LeftBracket";
            case InputConstants.KEY_RBRACKET -> "RightBracket";
            case InputConstants.KEY_BACKSLASH -> "Backslash";
            case InputConstants.KEY_GRAVE -> "Grave";
            case InputConstants.KEY_NUMPAD0 -> "Numpad0";
            case InputConstants.KEY_NUMPAD1 -> "Numpad1";
            case InputConstants.KEY_NUMPAD2 -> "Numpad2";
            case InputConstants.KEY_NUMPAD3 -> "Numpad3";
            case InputConstants.KEY_NUMPAD4 -> "Numpad4";
            case InputConstants.KEY_NUMPAD5 -> "Numpad5";
            case InputConstants.KEY_NUMPAD6 -> "Numpad6";
            case InputConstants.KEY_NUMPAD7 -> "Numpad7";
            case InputConstants.KEY_NUMPAD8 -> "Numpad8";
            case InputConstants.KEY_NUMPAD9 -> "Numpad9";
            case SDLScancode.SDL_SCANCODE_KP_PERIOD -> "NumpadDecimal";
            case SDLScancode.SDL_SCANCODE_KP_DIVIDE -> "NumpadDivide";
            case InputConstants.KEY_MULTIPLY -> "NumpadMultiply";
            case SDLScancode.SDL_SCANCODE_KP_MINUS -> "NumpadSubtract";
            case InputConstants.KEY_ADD -> "NumpadAdd";
            case InputConstants.KEY_NUMPADENTER -> "NumpadEnter";
            case InputConstants.KEY_NUMPADEQUALS -> "NumpadEqual";
            default -> "Key" + keyCode;
        };
    }

    public record ShortcutSpec(boolean ctrl, boolean shift, boolean alt, boolean superKey,
                               Set<Integer> keys, Set<Integer> mouseButtons, Set<Integer> scanCodes) {
        public boolean matches(Window window) {
            if (ctrl != isCtrlDown(window) || shift != isShiftDown(window) || alt != isAltDown(window)
                    || superKey != isSuperDown(window)) {
                return false;
            }
            for (int key : keys) {
                if (!ShortcutInputState.keyDown(window, key)) {
                    return false;
                }
            }
            for (int button : mouseButtons) if (!ShortcutInputState.mouseDown(button)) return false;
            for (int scan : scanCodes) if (!ShortcutInputState.scanDown(scan)) return false;
            return true;
        }

        public int weight() {
            return keys.size() + mouseButtons.size() + scanCodes.size()
                    + (ctrl ? 1 : 0) + (shift ? 1 : 0) + (alt ? 1 : 0) + (superKey ? 1 : 0);
        }

        private static boolean isCtrlDown(Window window) {
            return ShortcutInputState.keyDown(window, InputConstants.KEY_LCONTROL)
                    || ShortcutInputState.keyDown(window, InputConstants.KEY_RCONTROL);
        }

        private static boolean isShiftDown(Window window) {
            return ShortcutInputState.keyDown(window, InputConstants.KEY_LSHIFT)
                    || ShortcutInputState.keyDown(window, InputConstants.KEY_RSHIFT);
        }

        private static boolean isAltDown(Window window) {
            return ShortcutInputState.keyDown(window, InputConstants.KEY_LALT)
                    || ShortcutInputState.keyDown(window, InputConstants.KEY_RALT);
        }

        private static boolean isSuperDown(Window window) {
            return ShortcutInputState.keyDown(window, InputConstants.KEY_LGUI)
                    || ShortcutInputState.keyDown(window, InputConstants.KEY_RGUI);
        }
    }
}
