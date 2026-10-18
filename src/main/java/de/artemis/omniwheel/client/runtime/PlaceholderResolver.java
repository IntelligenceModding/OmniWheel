package de.artemis.omniwheel.client.runtime;

import net.minecraft.client.Minecraft;
import de.artemis.omniwheel.common.OmniWheelText;

public final class PlaceholderResolver {
    private PlaceholderResolver() {}
    public static String resolve(String raw, Minecraft minecraft) {
        return LiveDataSnapshot.capture(minecraft).resolve(OmniWheelText.resolve(raw));
    }
}
