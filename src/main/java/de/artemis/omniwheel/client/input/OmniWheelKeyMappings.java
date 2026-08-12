package de.artemis.omniwheel.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import de.artemis.omniwheel.mixin.client.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class OmniWheelKeyMappings {
    public static final String CATEGORY = "key.categories.omniwheel";
    public static final KeyMapping OPEN_WHEEL = new KeyMapping(
            "key.omniwheel.open_wheel",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
    );
    public static final KeyMapping OPEN_MANAGER = new KeyMapping(
            "key.omniwheel.open_manager",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            CATEGORY
    );
    public static final KeyMapping RADIAL_TOP_LEFT = new KeyMapping(
            "key.omniwheel.radial_top_left",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_7,
            CATEGORY
    );
    public static final KeyMapping RADIAL_TOP = new KeyMapping(
            "key.omniwheel.radial_top",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_8,
            CATEGORY
    );
    public static final KeyMapping RADIAL_TOP_RIGHT = new KeyMapping(
            "key.omniwheel.radial_top_right",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_9,
            CATEGORY
    );
    public static final KeyMapping RADIAL_LEFT = new KeyMapping(
            "key.omniwheel.radial_left",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_4,
            CATEGORY
    );
    public static final KeyMapping RADIAL_CENTER = new KeyMapping(
            "key.omniwheel.radial_center",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_5,
            CATEGORY
    );
    public static final KeyMapping RADIAL_RIGHT = new KeyMapping(
            "key.omniwheel.radial_right",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_6,
            CATEGORY
    );
    public static final KeyMapping RADIAL_BOTTOM_LEFT = new KeyMapping(
            "key.omniwheel.radial_bottom_left",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_1,
            CATEGORY
    );
    public static final KeyMapping RADIAL_BOTTOM = new KeyMapping(
            "key.omniwheel.radial_bottom",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_2,
            CATEGORY
    );
    public static final KeyMapping RADIAL_BOTTOM_RIGHT = new KeyMapping(
            "key.omniwheel.radial_bottom_right",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_3,
            CATEGORY
    );
    public static final List<KeyMapping> RADIAL_POSITION_KEYS = List.of(
            RADIAL_TOP_LEFT,
            RADIAL_TOP,
            RADIAL_TOP_RIGHT,
            RADIAL_LEFT,
            RADIAL_CENTER,
            RADIAL_RIGHT,
            RADIAL_BOTTOM_LEFT,
            RADIAL_BOTTOM,
            RADIAL_BOTTOM_RIGHT
    );

    private OmniWheelKeyMappings() {
    }

    public static void register() {
        KeyBindingHelper.registerKeyBinding(OPEN_WHEEL);
        KeyBindingHelper.registerKeyBinding(OPEN_MANAGER);
        for (KeyMapping keyMapping : RADIAL_POSITION_KEYS) {
            KeyBindingHelper.registerKeyBinding(keyMapping);
        }
    }

    public static InputConstants.Key currentKey(KeyMapping keyMapping) {
        return ((KeyMappingAccessor) keyMapping).omniwheel$getKey();
    }
}
