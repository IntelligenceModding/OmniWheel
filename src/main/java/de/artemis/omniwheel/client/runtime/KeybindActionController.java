package de.artemis.omniwheel.client.runtime;

import com.mojang.blaze3d.platform.InputConstants;
import de.artemis.omniwheel.client.input.OmniWheelKeyMappings;
import de.artemis.omniwheel.client.input.SyntheticKeybindInput;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class KeybindActionController {
    private final Set<String> pending = new LinkedHashSet<>();
    private final List<KeyMapping> pressed = new ArrayList<>();

    public static KeyMapping find(Minecraft minecraft, String id) {
        for (KeyMapping mapping : minecraft.options.keyMappings) {
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
        for (KeyMapping mapping : pressed) {
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
            KeyMapping mapping = find(minecraft, id);
            if (mapping == null) {
                continue;
            }
            SyntheticKeybindInput.dispatch(minecraft, mapping, GLFW.GLFW_PRESS);
            pressed.add(mapping);
        }
        pending.clear();
    }

    private static boolean isPhysicallyDown(Minecraft minecraft, KeyMapping mapping) {
        InputConstants.Key key = OmniWheelKeyMappings.currentKey(mapping);
        if (key.getValue() < 0) {
            return false;
        }
        var window = minecraft.getWindow();
        return switch (key.getType()) {
            case KEYSYM -> InputConstants.isKeyDown(window.getWindow(), key.getValue());
            case SCANCODE -> GLFW.glfwGetKey(window.getWindow(), key.getValue()) == GLFW.GLFW_PRESS;
            case MOUSE -> GLFW.glfwGetMouseButton(window.getWindow(), key.getValue()) == GLFW.GLFW_PRESS;
            default -> false;
        };
    }
}
