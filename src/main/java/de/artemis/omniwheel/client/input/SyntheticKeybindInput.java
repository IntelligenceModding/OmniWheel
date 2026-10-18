package de.artemis.omniwheel.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import de.artemis.omniwheel.mixin.client.KeyboardHandlerAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class SyntheticKeybindInput {
    private static final ThreadLocal<SyntheticKeybindInput> CURRENT = new ThreadLocal<>();

    private final KeyMapping target;
    private final InputConstants.Key key = InputConstants.getKey(-1, -1);

    private SyntheticKeybindInput(KeyMapping target) {
        this.target = target;
    }

    public static SyntheticKeybindInput current() {
        return CURRENT.get();
    }

    public Boolean matches(KeyMapping mapping, int keyCode, int scanCode) {
        if (keyCode != -1 || scanCode != -1) {
            return null;
        }
        return mapping == target;
    }

    public boolean routes(InputConstants.Key candidate) {
        return key.equals(candidate);
    }

    public KeyMapping target() {
        return target;
    }

    public static void dispatch(Minecraft minecraft, KeyMapping mapping, int action) {
        if (LegacyKeyMappings.dispatch(minecraft, mapping, action)) return;
        SyntheticKeybindInput previous = CURRENT.get();
        SyntheticKeybindInput input = new SyntheticKeybindInput(mapping);
        KeyboardHandlerAccessor keyboard = (KeyboardHandlerAccessor) minecraft.keyboardHandler;
        boolean handledDebugKey = keyboard.omniwheel$getHandledDebugKey();
        CURRENT.set(input);
        try {
            keyboard.omniwheel$keyPress(minecraft.getWindow().getWindow(), -1, -1, action, 0);
        } finally {
            keyboard.omniwheel$setHandledDebugKey(handledDebugKey);
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}
