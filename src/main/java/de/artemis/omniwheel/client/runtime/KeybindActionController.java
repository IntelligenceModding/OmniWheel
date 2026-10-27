package de.artemis.omniwheel.client.runtime;

import com.mojang.blaze3d.platform.InputConstants;
import de.artemis.omniwheel.client.input.OmniWheelKeyMappings;
import de.artemis.omniwheel.client.input.SyntheticKeybindInput;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

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
                SyntheticKeybindInput.dispatch(minecraft, mapping, InputConstants.RELEASE);
            }
            mapping.setDown(isPhysicallyDown(minecraft, mapping));
        }
        pressed.clear();
        if (minecraft.player == null || minecraft.level == null || minecraft.gui.screen() != null) {
            pending.clear();
            return;
        }

        for (String id : pending) {
            KeyMapping mapping = find(minecraft, id);
            if (mapping == null) {
                continue;
            }
            SyntheticKeybindInput.dispatch(minecraft, mapping, InputConstants.PRESS);
            pressed.add(mapping);
        }
        pending.clear();
    }

    private static boolean isPhysicallyDown(Minecraft minecraft, KeyMapping mapping) {
        InputConstants.Key key = OmniWheelKeyMappings.currentKey(mapping);
        if (key.getValue() < 0) {
            return false;
        }
        return switch (key.getType()) {
            case KEYBOARD -> InputConstants.isKeyDown(key.getValue());
            case MOUSE -> (org.lwjgl.sdl.SDLMouse.SDL_GetMouseState(null, null) & (1 << (key.getValue() - 1))) != 0;
            default -> false;
        };
    }
}
