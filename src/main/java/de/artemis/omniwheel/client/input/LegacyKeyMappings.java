package de.artemis.omniwheel.client.input;

import de.artemis.omniwheel.mixin.client.KeyboardHandlerAccessor;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Exposes hardcoded 1.21.9 controls as the keybind actions used by newer profiles. */
public final class LegacyKeyMappings {
    private static final KeyMapping.Category DEBUG = KeyMapping.Category.register(ResourceLocation.withDefaultNamespace("debug"));
    private static final List<KeyMapping> MAPPINGS = List.of(
            new KeyMapping("key.toggleGui", GLFW.GLFW_KEY_UNKNOWN, KeyMapping.Category.MISC),
            new KeyMapping("key.debug.overlay", GLFW.GLFW_KEY_UNKNOWN, DEBUG),
            new KeyMapping("key.debug.showHitboxes", GLFW.GLFW_KEY_UNKNOWN, DEBUG),
            new KeyMapping("key.debug.showChunkBorders", GLFW.GLFW_KEY_UNKNOWN, DEBUG),
            new KeyMapping("key.debug.showAdvancedTooltips", GLFW.GLFW_KEY_UNKNOWN, DEBUG));

    private LegacyKeyMappings() {}

    public static void register(RegisterKeyMappingsEvent event) {
        MAPPINGS.forEach(event::register);
    }

    public static void tick(Minecraft minecraft) {
        for (KeyMapping mapping : MAPPINGS) {
            while (mapping.consumeClick()) dispatch(minecraft, mapping, GLFW.GLFW_PRESS);
        }
    }

    public static boolean dispatch(Minecraft minecraft, KeyMapping mapping, int action) {
        if (!MAPPINGS.contains(mapping)) return false;
        if (action != GLFW.GLFW_PRESS || minecraft.player == null || minecraft.level == null || minecraft.screen != null) return true;
        switch (mapping.getName()) {
            case "key.toggleGui" -> minecraft.options.hideGui = !minecraft.options.hideGui;
            case "key.debug.overlay" -> minecraft.debugEntries.toggleF3Visible();
            default -> {
                int key = switch (mapping.getName()) {
                    case "key.debug.showHitboxes" -> GLFW.GLFW_KEY_B;
                    case "key.debug.showChunkBorders" -> GLFW.GLFW_KEY_G;
                    case "key.debug.showAdvancedTooltips" -> GLFW.GLFW_KEY_H;
                    default -> throw new IllegalArgumentException(mapping.getName());
                };
                ((KeyboardHandlerAccessor) minecraft.keyboardHandler).omniwheel$handleDebugKeys(new KeyEvent(key, -1, 0));
            }
        }
        return true;
    }
}
