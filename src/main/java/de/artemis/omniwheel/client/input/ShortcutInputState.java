package de.artemis.omniwheel.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;

import java.util.HashSet;
import java.util.Set;

/** Tracks SDL physical keys and mouse buttons received by the input callbacks. */
public final class ShortcutInputState {
    private static final Set<Integer> KEYS = new HashSet<>();
    private static final Set<Integer> BUTTONS = new HashSet<>();

    private ShortcutInputState() {}

    public static void keyboard(int key, int action) {
        update(KEYS, key, action);
    }

    public static void mouse(int button, int action) { update(BUTTONS, button, action); }

    private static void update(Set<Integer> values, int code, int action) {
        if (code < 0) return;
        if (action == InputConstants.RELEASE) values.remove(code);
        else if (action == InputConstants.PRESS) values.add(code);
    }

    public static boolean keyDown(Window window, int key) {
        return KEYS.contains(key) || InputConstants.isKeyDown(key);
    }

    public static boolean scanDown(int scan) { return KEYS.contains(scan); }
    public static boolean mouseDown(int button) { return BUTTONS.contains(button); }
    public static Set<Integer> mouseButtons() { return Set.copyOf(BUTTONS); }

    public static void clear() {
        KEYS.clear();
        BUTTONS.clear();
    }
}
