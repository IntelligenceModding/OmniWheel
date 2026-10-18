package de.artemis.omniwheel.client.runtime;

import net.minecraft.client.util.InputMappings;
import de.artemis.omniwheel.client.input.OmniWheelKeyMappings;
import de.artemis.omniwheel.client.input.SyntheticKeybindInput;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class KeybindActionController {

    private final Set<String> pending = new LinkedHashSet<>();

    private final List<KeyBinding> pressed = new ArrayList<>();

    public static KeyBinding find(Minecraft minecraft, String id) {
        for (KeyBinding mapping : minecraft.options.keyMappings) {
            if (mapping.getName().equals(id)) {
                return mapping;
            }
        }
        return null;
    }

    public void enqueue(String id) {
        pending.add(id);
    }

    public void startTick(Minecraft minecraft) {
        for (KeyBinding mapping : pressed) {
            if (minecraft.player != null && minecraft.level != null) {
                SyntheticKeybindInput.dispatch(minecraft, mapping, GLFW.GLFW_RELEASE);
            }
            mapping.setDown(isPhysicallyDown(minecraft, mapping));
        }
        pressed.clear();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            pending.clear();
            return;
        }
        for (String id : pending) {
            KeyBinding mapping = find(minecraft, id);
            if (mapping == null) {
                continue;
            }
            SyntheticKeybindInput.dispatch(minecraft, mapping, GLFW.GLFW_PRESS);
            pressed.add(mapping);
        }
        pending.clear();
    }

    private static boolean isPhysicallyDown(Minecraft minecraft, KeyBinding mapping) {
        InputMappings.Input key = OmniWheelKeyMappings.currentKey(mapping);
        if (key.getValue() < 0) {
            return false;
        }
        net.minecraft.client.MainWindow window = minecraft.getWindow();
        if (java.util.Objects.equals(key.getType(), net.minecraft.client.util.InputMappings.Type.KEYSYM)) {
            return InputMappings.isKeyDown(window.getWindow(), key.getValue());
        } else {
            if (java.util.Objects.equals(key.getType(), net.minecraft.client.util.InputMappings.Type.SCANCODE)) {
                return GLFW.glfwGetKey(window.getWindow(), key.getValue()) == GLFW.GLFW_PRESS;
            } else {
                if (java.util.Objects.equals(key.getType(), net.minecraft.client.util.InputMappings.Type.MOUSE)) {
                    return GLFW.glfwGetMouseButton(window.getWindow(), key.getValue()) == GLFW.GLFW_PRESS;
                } else {
                    return false;
                }
            }
        }
    }
}
