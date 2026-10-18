package de.artemis.omniwheel.client.runtime;

import net.minecraft.client.MainWindow;
import de.artemis.omniwheel.client.input.ShortcutInputState;
import org.lwjgl.glfw.GLFW;
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
            if ((token.startsWith("mouse") || token.startsWith("scan") || token.startsWith("key")) && part.contains("-"))
                return null;
            if (token.isEmpty()) {
                continue;
            }
            switch(token) {
                case "ctrl":
                case "control":
                case "strg":
                    {
                        ctrl = true;
                        break;
                    }
                case "shift":
                case "umschalt":
                    {
                        shift = true;
                        break;
                    }
                case "alt":
                case "option":
                    {
                        alt = true;
                        break;
                    }
                case "super":
                case "win":
                case "windows":
                case "cmd":
                case "command":
                    {
                        superKey = true;
                        break;
                    }
                default:
                    {
                        {
                            if (token.startsWith("mouse") || token.startsWith("scan")) {
                                boolean mouse = token.startsWith("mouse");
                                Integer code = parseCode(token.substring(mouse ? 5 : 4));
                                if (code == null || mouse && code == 0)
                                    return null;
                                (mouse ? mouseButtons : scanCodes).add(mouse ? code - 1 : code);
                                continue;
                            }
                            Integer key = resolveKey(token);
                            if (key == null) {
                                return null;
                            }
                            keys.add(key);
                        }
                        break;
                    }
            }
        }
        if (keys.isEmpty() && mouseButtons.isEmpty() && scanCodes.isEmpty()) {
            return null;
        } else {
            return new ShortcutSpec(ctrl, shift, alt, superKey, de.artemis.omniwheel.common.Java8.copySet(keys), de.artemis.omniwheel.common.Java8.copySet(mouseButtons), de.artemis.omniwheel.common.Java8.copySet(scanCodes));
        }
    }

    private static Integer parseCode(String token) {
        try {
            int code = Integer.parseInt(token);
            if (code >= 0 && code < Integer.MAX_VALUE) {
                return code;
            } else {
                return null;
            }
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static String captureShortcutText(MainWindow window, int keyCode, int modifiers) {
        return captureShortcutText(window, keyCode, -1, modifiers);
    }

    public static String captureShortcutText(MainWindow window, int keyCode, int scanCode, int modifiers) {
        if (isModifierOnlyKey(keyCode) || keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            return null;
        }
        List<String> parts = modifierLabels(modifiers);
        int modifierCount = parts.size();
        Set<Integer> pressedKeys = collectPressedKeys(window, keyCode);
        for (int pressedKey : pressedKeys) parts.add(labelForKey(pressedKey));
        Set<Integer> scans = new TreeSet<>(ShortcutInputState.unknownScans());
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN && scanCode >= 0)
            scans.add(scanCode);
        for (int scan : scans) parts.add("Scan" + scan);
        for (int button : new TreeSet<>(ShortcutInputState.mouseButtons())) parts.add("Mouse" + (button + 1));
        if (parts.size() == modifierCount) {
            return null;
        } else {
            return String.join("+", parts);
        }
    }

    public static String captureMouseShortcutText(MainWindow window, int button) {
        if (button < 0 || button == Integer.MAX_VALUE)
            return null;
        int modifiers = 0;
        if (ShortcutSpec.isCtrlDown(window))
            modifiers |= GLFW.GLFW_MOD_CONTROL;
        if (ShortcutSpec.isShiftDown(window))
            modifiers |= GLFW.GLFW_MOD_SHIFT;
        if (ShortcutSpec.isAltDown(window))
            modifiers |= GLFW.GLFW_MOD_ALT;
        if (ShortcutSpec.isSuperDown(window))
            modifiers |= GLFW.GLFW_MOD_SUPER;
        List<String> parts = modifierLabels(modifiers);
        for (int key : collectPressedKeys(window, -1)) parts.add(labelForKey(key));
        for (int scan : new TreeSet<>(ShortcutInputState.unknownScans())) parts.add("Scan" + scan);
        Set<Integer> buttons = new TreeSet<>(ShortcutInputState.mouseButtons());
        buttons.add(button);
        for (int pressed : buttons) parts.add("Mouse" + (pressed + 1));
        return String.join("+", parts);
    }

    private static List<String> modifierLabels(int modifiers) {
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
        if ((modifiers & GLFW.GLFW_MOD_SUPER) != 0)
            parts.add("Super");
        return parts;
    }

    private static String normalizeToken(String token) {
        if (token == null) {
            return "";
        } else {
            return token.trim().toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "").replace("-", "");
        }
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
                int key = Integer.parseInt(token.substring(3));
                if (key >= GLFW.GLFW_KEY_SPACE && key <= GLFW.GLFW_KEY_LAST) {
                    return key;
                } else {
                    return null;
                }
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        if (java.util.Objects.equals(token, "space")) {
            return GLFW.GLFW_KEY_SPACE;
        } else {
            if (java.util.Objects.equals(token, "tab")) {
                return GLFW.GLFW_KEY_TAB;
            } else {
                if (java.util.Objects.equals(token, "enter") || java.util.Objects.equals(token, "return")) {
                    return GLFW.GLFW_KEY_ENTER;
                } else {
                    if (java.util.Objects.equals(token, "esc") || java.util.Objects.equals(token, "escape")) {
                        return GLFW.GLFW_KEY_ESCAPE;
                    } else {
                        if (java.util.Objects.equals(token, "del") || java.util.Objects.equals(token, "delete")) {
                            return GLFW.GLFW_KEY_DELETE;
                        } else {
                            if (java.util.Objects.equals(token, "backspace")) {
                                return GLFW.GLFW_KEY_BACKSPACE;
                            } else {
                                if (java.util.Objects.equals(token, "insert") || java.util.Objects.equals(token, "ins")) {
                                    return GLFW.GLFW_KEY_INSERT;
                                } else {
                                    if (java.util.Objects.equals(token, "home")) {
                                        return GLFW.GLFW_KEY_HOME;
                                    } else {
                                        if (java.util.Objects.equals(token, "end")) {
                                            return GLFW.GLFW_KEY_END;
                                        } else {
                                            if (java.util.Objects.equals(token, "pageup") || java.util.Objects.equals(token, "pgup")) {
                                                return GLFW.GLFW_KEY_PAGE_UP;
                                            } else {
                                                if (java.util.Objects.equals(token, "pagedown") || java.util.Objects.equals(token, "pgdn")) {
                                                    return GLFW.GLFW_KEY_PAGE_DOWN;
                                                } else {
                                                    if (java.util.Objects.equals(token, "left")) {
                                                        return GLFW.GLFW_KEY_LEFT;
                                                    } else {
                                                        if (java.util.Objects.equals(token, "right")) {
                                                            return GLFW.GLFW_KEY_RIGHT;
                                                        } else {
                                                            if (java.util.Objects.equals(token, "up")) {
                                                                return GLFW.GLFW_KEY_UP;
                                                            } else {
                                                                if (java.util.Objects.equals(token, "down")) {
                                                                    return GLFW.GLFW_KEY_DOWN;
                                                                } else {
                                                                    if (java.util.Objects.equals(token, "capslock")) {
                                                                        return GLFW.GLFW_KEY_CAPS_LOCK;
                                                                    } else {
                                                                        if (java.util.Objects.equals(token, "scrolllock")) {
                                                                            return GLFW.GLFW_KEY_SCROLL_LOCK;
                                                                        } else {
                                                                            if (java.util.Objects.equals(token, "numlock")) {
                                                                                return GLFW.GLFW_KEY_NUM_LOCK;
                                                                            } else {
                                                                                if (java.util.Objects.equals(token, "printscreen")) {
                                                                                    return GLFW.GLFW_KEY_PRINT_SCREEN;
                                                                                } else {
                                                                                    if (java.util.Objects.equals(token, "pause")) {
                                                                                        return GLFW.GLFW_KEY_PAUSE;
                                                                                    } else {
                                                                                        if (java.util.Objects.equals(token, "menu") || java.util.Objects.equals(token, "application")) {
                                                                                            return GLFW.GLFW_KEY_MENU;
                                                                                        } else {
                                                                                            if (java.util.Objects.equals(token, "world1")) {
                                                                                                return GLFW.GLFW_KEY_WORLD_1;
                                                                                            } else {
                                                                                                if (java.util.Objects.equals(token, "world2")) {
                                                                                                    return GLFW.GLFW_KEY_WORLD_2;
                                                                                                } else {
                                                                                                    if (java.util.Objects.equals(token, "comma")) {
                                                                                                        return GLFW.GLFW_KEY_COMMA;
                                                                                                    } else {
                                                                                                        if (java.util.Objects.equals(token, "period") || java.util.Objects.equals(token, "dot")) {
                                                                                                            return GLFW.GLFW_KEY_PERIOD;
                                                                                                        } else {
                                                                                                            if (java.util.Objects.equals(token, "slash")) {
                                                                                                                return GLFW.GLFW_KEY_SLASH;
                                                                                                            } else {
                                                                                                                if (java.util.Objects.equals(token, "semicolon")) {
                                                                                                                    return GLFW.GLFW_KEY_SEMICOLON;
                                                                                                                } else {
                                                                                                                    if (java.util.Objects.equals(token, "apostrophe") || java.util.Objects.equals(token, "quote")) {
                                                                                                                        return GLFW.GLFW_KEY_APOSTROPHE;
                                                                                                                    } else {
                                                                                                                        if (java.util.Objects.equals(token, "equal") || java.util.Objects.equals(token, "equals")) {
                                                                                                                            return GLFW.GLFW_KEY_EQUAL;
                                                                                                                        } else {
                                                                                                                            if (java.util.Objects.equals(token, "minus")) {
                                                                                                                                return GLFW.GLFW_KEY_MINUS;
                                                                                                                            } else {
                                                                                                                                if (java.util.Objects.equals(token, "leftbracket") || java.util.Objects.equals(token, "lbracket")) {
                                                                                                                                    return GLFW.GLFW_KEY_LEFT_BRACKET;
                                                                                                                                } else {
                                                                                                                                    if (java.util.Objects.equals(token, "rightbracket") || java.util.Objects.equals(token, "rbracket")) {
                                                                                                                                        return GLFW.GLFW_KEY_RIGHT_BRACKET;
                                                                                                                                    } else {
                                                                                                                                        if (java.util.Objects.equals(token, "backslash")) {
                                                                                                                                            return GLFW.GLFW_KEY_BACKSLASH;
                                                                                                                                        } else {
                                                                                                                                            if (java.util.Objects.equals(token, "grave") || java.util.Objects.equals(token, "backtick")) {
                                                                                                                                                return GLFW.GLFW_KEY_GRAVE_ACCENT;
                                                                                                                                            } else {
                                                                                                                                                if (java.util.Objects.equals(token, "numpad0") || java.util.Objects.equals(token, "num0") || java.util.Objects.equals(token, "kp0")) {
                                                                                                                                                    return GLFW.GLFW_KEY_KP_0;
                                                                                                                                                } else {
                                                                                                                                                    if (java.util.Objects.equals(token, "numpad1") || java.util.Objects.equals(token, "num1") || java.util.Objects.equals(token, "kp1")) {
                                                                                                                                                        return GLFW.GLFW_KEY_KP_1;
                                                                                                                                                    } else {
                                                                                                                                                        if (java.util.Objects.equals(token, "numpad2") || java.util.Objects.equals(token, "num2") || java.util.Objects.equals(token, "kp2")) {
                                                                                                                                                            return GLFW.GLFW_KEY_KP_2;
                                                                                                                                                        } else {
                                                                                                                                                            if (java.util.Objects.equals(token, "numpad3") || java.util.Objects.equals(token, "num3") || java.util.Objects.equals(token, "kp3")) {
                                                                                                                                                                return GLFW.GLFW_KEY_KP_3;
                                                                                                                                                            } else {
                                                                                                                                                                if (java.util.Objects.equals(token, "numpad4") || java.util.Objects.equals(token, "num4") || java.util.Objects.equals(token, "kp4")) {
                                                                                                                                                                    return GLFW.GLFW_KEY_KP_4;
                                                                                                                                                                } else {
                                                                                                                                                                    if (java.util.Objects.equals(token, "numpad5") || java.util.Objects.equals(token, "num5") || java.util.Objects.equals(token, "kp5")) {
                                                                                                                                                                        return GLFW.GLFW_KEY_KP_5;
                                                                                                                                                                    } else {
                                                                                                                                                                        if (java.util.Objects.equals(token, "numpad6") || java.util.Objects.equals(token, "num6") || java.util.Objects.equals(token, "kp6")) {
                                                                                                                                                                            return GLFW.GLFW_KEY_KP_6;
                                                                                                                                                                        } else {
                                                                                                                                                                            if (java.util.Objects.equals(token, "numpad7") || java.util.Objects.equals(token, "num7") || java.util.Objects.equals(token, "kp7")) {
                                                                                                                                                                                return GLFW.GLFW_KEY_KP_7;
                                                                                                                                                                            } else {
                                                                                                                                                                                if (java.util.Objects.equals(token, "numpad8") || java.util.Objects.equals(token, "num8") || java.util.Objects.equals(token, "kp8")) {
                                                                                                                                                                                    return GLFW.GLFW_KEY_KP_8;
                                                                                                                                                                                } else {
                                                                                                                                                                                    if (java.util.Objects.equals(token, "numpad9") || java.util.Objects.equals(token, "num9") || java.util.Objects.equals(token, "kp9")) {
                                                                                                                                                                                        return GLFW.GLFW_KEY_KP_9;
                                                                                                                                                                                    } else {
                                                                                                                                                                                        if (java.util.Objects.equals(token, "numpaddecimal") || java.util.Objects.equals(token, "numdecimal") || java.util.Objects.equals(token, "kpdecimal")) {
                                                                                                                                                                                            return GLFW.GLFW_KEY_KP_DECIMAL;
                                                                                                                                                                                        } else {
                                                                                                                                                                                            if (java.util.Objects.equals(token, "numpaddivide") || java.util.Objects.equals(token, "numdivide") || java.util.Objects.equals(token, "kpdivide")) {
                                                                                                                                                                                                return GLFW.GLFW_KEY_KP_DIVIDE;
                                                                                                                                                                                            } else {
                                                                                                                                                                                                if (java.util.Objects.equals(token, "numpadmultiply") || java.util.Objects.equals(token, "nummultiply") || java.util.Objects.equals(token, "kpmultiply")) {
                                                                                                                                                                                                    return GLFW.GLFW_KEY_KP_MULTIPLY;
                                                                                                                                                                                                } else {
                                                                                                                                                                                                    if (java.util.Objects.equals(token, "numpadsubtract") || java.util.Objects.equals(token, "numsubtract") || java.util.Objects.equals(token, "kpsubtract")) {
                                                                                                                                                                                                        return GLFW.GLFW_KEY_KP_SUBTRACT;
                                                                                                                                                                                                    } else {
                                                                                                                                                                                                        if (java.util.Objects.equals(token, "numpadadd") || java.util.Objects.equals(token, "numadd") || java.util.Objects.equals(token, "kpadd") || java.util.Objects.equals(token, "plus")) {
                                                                                                                                                                                                            return GLFW.GLFW_KEY_KP_ADD;
                                                                                                                                                                                                        } else {
                                                                                                                                                                                                            if (java.util.Objects.equals(token, "numpadenter") || java.util.Objects.equals(token, "numenter") || java.util.Objects.equals(token, "kpenter")) {
                                                                                                                                                                                                                return GLFW.GLFW_KEY_KP_ENTER;
                                                                                                                                                                                                            } else {
                                                                                                                                                                                                                if (java.util.Objects.equals(token, "numpadequal") || java.util.Objects.equals(token, "numequal") || java.util.Objects.equals(token, "kpequal")) {
                                                                                                                                                                                                                    return GLFW.GLFW_KEY_KP_EQUAL;
                                                                                                                                                                                                                } else {
                                                                                                                                                                                                                    return null;
                                                                                                                                                                                                                }
                                                                                                                                                                                                            }
                                                                                                                                                                                                        }
                                                                                                                                                                                                    }
                                                                                                                                                                                                }
                                                                                                                                                                                            }
                                                                                                                                                                                        }
                                                                                                                                                                                    }
                                                                                                                                                                                }
                                                                                                                                                                            }
                                                                                                                                                                        }
                                                                                                                                                                    }
                                                                                                                                                                }
                                                                                                                                                            }
                                                                                                                                                        }
                                                                                                                                                    }
                                                                                                                                                }
                                                                                                                                            }
                                                                                                                                        }
                                                                                                                                    }
                                                                                                                                }
                                                                                                                            }
                                                                                                                        }
                                                                                                                    }
                                                                                                                }
                                                                                                            }
                                                                                                        }
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean isModifierOnlyKey(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT || keyCode == GLFW.GLFW_KEY_LEFT_CONTROL || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL || keyCode == GLFW.GLFW_KEY_LEFT_ALT || keyCode == GLFW.GLFW_KEY_RIGHT_ALT || keyCode == GLFW.GLFW_KEY_LEFT_SUPER || keyCode == GLFW.GLFW_KEY_RIGHT_SUPER;
    }

    private static Set<Integer> collectPressedKeys(MainWindow window, int keyCode) {
        Set<Integer> keys = new LinkedHashSet<>();
        for (int candidate = GLFW.GLFW_KEY_SPACE; candidate <= GLFW.GLFW_KEY_LAST; candidate++) {
            if (isModifierOnlyKey(candidate)) {
                continue;
            }
            if (ShortcutInputState.keyDown(window, candidate)) {
                keys.add(candidate);
            }
        }
        if (keyCode >= GLFW.GLFW_KEY_SPACE && keyCode <= GLFW.GLFW_KEY_LAST)
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
        if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_SPACE)) {
            return "Space";
        } else {
            if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_TAB)) {
                return "Tab";
            } else {
                if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_HOME)) {
                    return "Home";
                } else {
                    if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_END)) {
                        return "End";
                    } else {
                        if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_PAGE_UP)) {
                            return "PageUp";
                        } else {
                            if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_PAGE_DOWN)) {
                                return "PageDown";
                            } else {
                                if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_UP)) {
                                    return "Up";
                                } else {
                                    if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_DOWN)) {
                                        return "Down";
                                    } else {
                                        if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_LEFT)) {
                                            return "Left";
                                        } else {
                                            if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_RIGHT)) {
                                                return "Right";
                                            } else {
                                                if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_INSERT)) {
                                                    return "Insert";
                                                } else {
                                                    if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_DELETE)) {
                                                        return "Delete";
                                                    } else {
                                                        if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_BACKSPACE)) {
                                                            return "Backspace";
                                                        } else {
                                                            if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_CAPS_LOCK)) {
                                                                return "CapsLock";
                                                            } else {
                                                                if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_SCROLL_LOCK)) {
                                                                    return "ScrollLock";
                                                                } else {
                                                                    if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_NUM_LOCK)) {
                                                                        return "NumLock";
                                                                    } else {
                                                                        if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_PRINT_SCREEN)) {
                                                                            return "PrintScreen";
                                                                        } else {
                                                                            if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_PAUSE)) {
                                                                                return "Pause";
                                                                            } else {
                                                                                if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_MENU)) {
                                                                                    return "Menu";
                                                                                } else {
                                                                                    if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_WORLD_1)) {
                                                                                        return "World1";
                                                                                    } else {
                                                                                        if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_WORLD_2)) {
                                                                                            return "World2";
                                                                                        } else {
                                                                                            if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_COMMA)) {
                                                                                                return "Comma";
                                                                                            } else {
                                                                                                if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_PERIOD)) {
                                                                                                    return "Period";
                                                                                                } else {
                                                                                                    if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_SLASH)) {
                                                                                                        return "Slash";
                                                                                                    } else {
                                                                                                        if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_SEMICOLON)) {
                                                                                                            return "Semicolon";
                                                                                                        } else {
                                                                                                            if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_APOSTROPHE)) {
                                                                                                                return "Apostrophe";
                                                                                                            } else {
                                                                                                                if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_EQUAL)) {
                                                                                                                    return "Equal";
                                                                                                                } else {
                                                                                                                    if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_MINUS)) {
                                                                                                                        return "Minus";
                                                                                                                    } else {
                                                                                                                        if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_LEFT_BRACKET)) {
                                                                                                                            return "LeftBracket";
                                                                                                                        } else {
                                                                                                                            if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_RIGHT_BRACKET)) {
                                                                                                                                return "RightBracket";
                                                                                                                            } else {
                                                                                                                                if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_BACKSLASH)) {
                                                                                                                                    return "Backslash";
                                                                                                                                } else {
                                                                                                                                    if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_GRAVE_ACCENT)) {
                                                                                                                                        return "Grave";
                                                                                                                                    } else {
                                                                                                                                        if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_KP_0)) {
                                                                                                                                            return "Numpad0";
                                                                                                                                        } else {
                                                                                                                                            if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_KP_1)) {
                                                                                                                                                return "Numpad1";
                                                                                                                                            } else {
                                                                                                                                                if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_KP_2)) {
                                                                                                                                                    return "Numpad2";
                                                                                                                                                } else {
                                                                                                                                                    if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_KP_3)) {
                                                                                                                                                        return "Numpad3";
                                                                                                                                                    } else {
                                                                                                                                                        if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_KP_4)) {
                                                                                                                                                            return "Numpad4";
                                                                                                                                                        } else {
                                                                                                                                                            if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_KP_5)) {
                                                                                                                                                                return "Numpad5";
                                                                                                                                                            } else {
                                                                                                                                                                if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_KP_6)) {
                                                                                                                                                                    return "Numpad6";
                                                                                                                                                                } else {
                                                                                                                                                                    if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_KP_7)) {
                                                                                                                                                                        return "Numpad7";
                                                                                                                                                                    } else {
                                                                                                                                                                        if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_KP_8)) {
                                                                                                                                                                            return "Numpad8";
                                                                                                                                                                        } else {
                                                                                                                                                                            if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_KP_9)) {
                                                                                                                                                                                return "Numpad9";
                                                                                                                                                                            } else {
                                                                                                                                                                                if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_KP_DECIMAL)) {
                                                                                                                                                                                    return "NumpadDecimal";
                                                                                                                                                                                } else {
                                                                                                                                                                                    if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_KP_DIVIDE)) {
                                                                                                                                                                                        return "NumpadDivide";
                                                                                                                                                                                    } else {
                                                                                                                                                                                        if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_KP_MULTIPLY)) {
                                                                                                                                                                                            return "NumpadMultiply";
                                                                                                                                                                                        } else {
                                                                                                                                                                                            if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_KP_SUBTRACT)) {
                                                                                                                                                                                                return "NumpadSubtract";
                                                                                                                                                                                            } else {
                                                                                                                                                                                                if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_KP_ADD)) {
                                                                                                                                                                                                    return "NumpadAdd";
                                                                                                                                                                                                } else {
                                                                                                                                                                                                    if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_KP_ENTER)) {
                                                                                                                                                                                                        return "NumpadEnter";
                                                                                                                                                                                                    } else {
                                                                                                                                                                                                        if (java.util.Objects.equals(keyCode, GLFW.GLFW_KEY_KP_EQUAL)) {
                                                                                                                                                                                                            return "NumpadEqual";
                                                                                                                                                                                                        } else {
                                                                                                                                                                                                            return "Key" + keyCode;
                                                                                                                                                                                                        }
                                                                                                                                                                                                    }
                                                                                                                                                                                                }
                                                                                                                                                                                            }
                                                                                                                                                                                        }
                                                                                                                                                                                    }
                                                                                                                                                                                }
                                                                                                                                                                            }
                                                                                                                                                                        }
                                                                                                                                                                    }
                                                                                                                                                                }
                                                                                                                                                            }
                                                                                                                                                        }
                                                                                                                                                    }
                                                                                                                                                }
                                                                                                                                            }
                                                                                                                                        }
                                                                                                                                    }
                                                                                                                                }
                                                                                                                            }
                                                                                                                        }
                                                                                                                    }
                                                                                                                }
                                                                                                            }
                                                                                                        }
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public final static class ShortcutSpec {

        private final boolean ctrl;

        private final boolean shift;

        private final boolean alt;

        private final boolean superKey;

        private final Set<Integer> keys;

        private final Set<Integer> mouseButtons;

        private final Set<Integer> scanCodes;

        public boolean matches(MainWindow window) {
            if (ctrl != isCtrlDown(window) || shift != isShiftDown(window) || alt != isAltDown(window) || superKey != isSuperDown(window)) {
                return false;
            }
            for (int key : keys) {
                if (!ShortcutInputState.keyDown(window, key)) {
                    return false;
                }
            }
            for (int button : mouseButtons) if (!ShortcutInputState.mouseDown(button))
                return false;
            for (int scan : scanCodes) if (!ShortcutInputState.scanDown(scan))
                return false;
            return true;
        }

        public int weight() {
            return keys.size() + mouseButtons.size() + scanCodes.size() + (ctrl ? 1 : 0) + (shift ? 1 : 0) + (alt ? 1 : 0) + (superKey ? 1 : 0);
        }

        private static boolean isCtrlDown(MainWindow window) {
            return ShortcutInputState.keyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL) || ShortcutInputState.keyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
        }

        private static boolean isShiftDown(MainWindow window) {
            return ShortcutInputState.keyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) || ShortcutInputState.keyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
        }

        private static boolean isAltDown(MainWindow window) {
            return ShortcutInputState.keyDown(window, GLFW.GLFW_KEY_LEFT_ALT) || ShortcutInputState.keyDown(window, GLFW.GLFW_KEY_RIGHT_ALT);
        }

        private static boolean isSuperDown(MainWindow window) {
            return ShortcutInputState.keyDown(window, GLFW.GLFW_KEY_LEFT_SUPER) || ShortcutInputState.keyDown(window, GLFW.GLFW_KEY_RIGHT_SUPER);
        }

        public ShortcutSpec(boolean ctrl, boolean shift, boolean alt, boolean superKey, Set<Integer> keys, Set<Integer> mouseButtons, Set<Integer> scanCodes) {
            this.ctrl = ctrl;
            this.shift = shift;
            this.alt = alt;
            this.superKey = superKey;
            this.keys = keys;
            this.mouseButtons = mouseButtons;
            this.scanCodes = scanCodes;
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

        public boolean superKey() {
            return superKey;
        }

        public Set<Integer> keys() {
            return keys;
        }

        public Set<Integer> mouseButtons() {
            return mouseButtons;
        }

        public Set<Integer> scanCodes() {
            return scanCodes;
        }

        @Override
        public boolean equals(Object value) {
            if (this == value)
                return true;
            if (value == null || value.getClass() != getClass())
                return false;
            ShortcutSpec other = (ShortcutSpec) value;
            return true && java.util.Objects.equals(ctrl, other.ctrl) && java.util.Objects.equals(shift, other.shift) && java.util.Objects.equals(alt, other.alt) && java.util.Objects.equals(superKey, other.superKey) && java.util.Objects.equals(keys, other.keys) && java.util.Objects.equals(mouseButtons, other.mouseButtons) && java.util.Objects.equals(scanCodes, other.scanCodes);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(ctrl, shift, alt, superKey, keys, mouseButtons, scanCodes);
        }

        @Override
        public String toString() {
            return "ShortcutSpec[" + "ctrl=" + ctrl + ", shift=" + shift + ", alt=" + alt + ", superKey=" + superKey + ", keys=" + keys + ", mouseButtons=" + mouseButtons + ", scanCodes=" + scanCodes + "]";
        }
    }
}
