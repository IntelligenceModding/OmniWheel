package de.artemis.omniwheel.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

/** Callback state also covers keys which GLFW cannot poll by a standard key code. */
public final class ShortcutInputState {
    private static final Set<Integer> KEYS = new HashSet<>();
    private static final Set<Integer> SCANS = new HashSet<>();
    private static final Set<Integer> UNKNOWN_SCANS = new HashSet<>();
    private static final Set<Integer> BUTTONS = new HashSet<>();

    private ShortcutInputState() {}

    public static void keyboard(int key, int scan, int action) {
        update(KEYS, key, action);
        update(SCANS, scan, action);
        if (key == GLFW.GLFW_KEY_UNKNOWN) update(UNKNOWN_SCANS, scan, action);
    }

    public static void mouse(int button, int action) { update(BUTTONS, button, action); }

    private static void update(Set<Integer> values, int code, int action) {
        if (code < 0) return;
        if (action == GLFW.GLFW_RELEASE) values.remove(code);
        else if (action == GLFW.GLFW_PRESS) values.add(code);
    }

    public static boolean keyDown(Window window, int key) {
        return KEYS.contains(key) || InputConstants.isKeyDown(window, key);
    }

    public static boolean scanDown(int scan) { return SCANS.contains(scan); }
    public static boolean mouseDown(int button) { return BUTTONS.contains(button); }
    public static Set<Integer> unknownScans() { return Set.copyOf(UNKNOWN_SCANS); }
    public static Set<Integer> mouseButtons() { return Set.copyOf(BUTTONS); }

    public static void clear() {
        KEYS.clear();
        SCANS.clear();
        UNKNOWN_SCANS.clear();
        BUTTONS.clear();
    }
}
