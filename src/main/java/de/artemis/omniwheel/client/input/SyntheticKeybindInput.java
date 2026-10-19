package de.artemis.omniwheel.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import de.artemis.omniwheel.mixin.client.KeyboardHandlerAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;

public final class SyntheticKeybindInput {
    private static final ThreadLocal<SyntheticKeybindInput> CURRENT = new ThreadLocal<>();

    private final KeyMapping target;
    private final KeyEvent event = new KeyEvent(-1, -1, 0);
    private final InputConstants.Key key = InputConstants.getKey(event);

    private SyntheticKeybindInput(KeyMapping target) {
        this.target = target;
    }

    public static SyntheticKeybindInput current() {
        return CURRENT.get();
    }

    public Boolean matches(KeyMapping mapping, KeyEvent candidate) {
        if (candidate != event) {
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
            keyboard.omniwheel$keyPress(minecraft.getWindow().handle(), action, input.event);
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
