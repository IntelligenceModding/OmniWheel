package de.artemis.omniwheel.client.input;

import de.artemis.omniwheel.mixin.client.KeyboardHandlerAccessor;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import java.util.List;

/**
 * Exposes hardcoded HUD and debug controls as the keybind actions used by newer profiles.
 */
public final class LegacyKeyMappings {

    private static final String DEBUG = "key.category.minecraft.debug";

    private static final List<KeyBinding> MAPPINGS = de.artemis.omniwheel.common.Java8.list(new KeyBinding("key.toggleGui", GLFW.GLFW_KEY_UNKNOWN, "key.categories.misc"), new KeyBinding("key.debug.overlay", GLFW.GLFW_KEY_UNKNOWN, DEBUG), new KeyBinding("key.debug.showHitboxes", GLFW.GLFW_KEY_UNKNOWN, DEBUG), new KeyBinding("key.debug.showChunkBorders", GLFW.GLFW_KEY_UNKNOWN, DEBUG), new KeyBinding("key.debug.showAdvancedTooltips", GLFW.GLFW_KEY_UNKNOWN, DEBUG));

    private LegacyKeyMappings() {
    }

    public static void register() {
        MAPPINGS.forEach(ClientRegistry::registerKeyBinding);
    }

    public static void tick(Minecraft minecraft) {
        for (KeyBinding mapping : MAPPINGS) {
            while (mapping.consumeClick()) dispatch(minecraft, mapping, GLFW.GLFW_PRESS);
        }
    }

    public static boolean dispatch(Minecraft minecraft, KeyBinding mapping, int action) {
        if (!MAPPINGS.contains(mapping))
            return false;
        if (action != GLFW.GLFW_PRESS || minecraft.player == null || minecraft.level == null || minecraft.screen != null)
            return true;
        switch(mapping.getName()) {
            case "key.toggleGui":
                {
                    minecraft.options.hideGui = !minecraft.options.hideGui;
                    break;
                }
            case "key.debug.overlay":
                {
                    minecraft.options.renderDebug = !minecraft.options.renderDebug;
                    break;
                }
            default:
                {
                    {
                        int key = (java.util.Objects.equals(mapping.getName(), "key.debug.showHitboxes") ? GLFW.GLFW_KEY_B : (java.util.Objects.equals(mapping.getName(), "key.debug.showChunkBorders") ? GLFW.GLFW_KEY_G : (java.util.Objects.equals(mapping.getName(), "key.debug.showAdvancedTooltips") ? GLFW.GLFW_KEY_H : de.artemis.omniwheel.common.Java8.fail(new IllegalArgumentException(mapping.getName())))));
                        ((KeyboardHandlerAccessor) minecraft.keyboardHandler).omniwheel$handleDebugKeys(key);
                    }
                    break;
                }
        }
        return true;
    }
}
