package de.artemis.omniwheel.client.input;

import net.minecraft.client.util.InputMappings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class OmniWheelKeyMappings {
    public static final String CATEGORY = "key.categories.omniwheel";
    public static final KeyBinding OPEN_WHEEL = new KeyBinding(
            "key.omniwheel.open_wheel",
            InputMappings.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
    );
    public static final KeyBinding OPEN_MANAGER = new KeyBinding(
            "key.omniwheel.open_manager",
            InputMappings.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            CATEGORY
    );
    public static final KeyBinding RADIAL_TOP_LEFT = new KeyBinding(
            "key.omniwheel.radial_top_left",
            InputMappings.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_7,
            CATEGORY
    );
    public static final KeyBinding RADIAL_TOP = new KeyBinding(
            "key.omniwheel.radial_top",
            InputMappings.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_8,
            CATEGORY
    );
    public static final KeyBinding RADIAL_TOP_RIGHT = new KeyBinding(
            "key.omniwheel.radial_top_right",
            InputMappings.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_9,
            CATEGORY
    );
    public static final KeyBinding RADIAL_LEFT = new KeyBinding(
            "key.omniwheel.radial_left",
            InputMappings.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_4,
            CATEGORY
    );
    public static final KeyBinding RADIAL_CENTER = new KeyBinding(
            "key.omniwheel.radial_center",
            InputMappings.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_5,
            CATEGORY
    );
    public static final KeyBinding RADIAL_RIGHT = new KeyBinding(
            "key.omniwheel.radial_right",
            InputMappings.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_6,
            CATEGORY
    );
    public static final KeyBinding RADIAL_BOTTOM_LEFT = new KeyBinding(
            "key.omniwheel.radial_bottom_left",
            InputMappings.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_1,
            CATEGORY
    );
    public static final KeyBinding RADIAL_BOTTOM = new KeyBinding(
            "key.omniwheel.radial_bottom",
            InputMappings.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_2,
            CATEGORY
    );
    public static final KeyBinding RADIAL_BOTTOM_RIGHT = new KeyBinding(
            "key.omniwheel.radial_bottom_right",
            InputMappings.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_3,
            CATEGORY
    );
    public static final List<KeyBinding> RADIAL_POSITION_KEYS = com.google.common.collect.ImmutableList.of(
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
        ClientRegistry.registerKeyBinding(OPEN_WHEEL);
        ClientRegistry.registerKeyBinding(OPEN_MANAGER);
        for (KeyBinding KeyBinding : RADIAL_POSITION_KEYS) {
            ClientRegistry.registerKeyBinding(KeyBinding);
        }
    }
}
