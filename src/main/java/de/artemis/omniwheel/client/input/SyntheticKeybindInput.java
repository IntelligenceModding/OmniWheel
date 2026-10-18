package de.artemis.omniwheel.client.input;

import net.minecraft.client.util.InputMappings;
import de.artemis.omniwheel.mixin.client.KeyboardHandlerAccessor;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.Minecraft;

public final class SyntheticKeybindInput {

    private static final ThreadLocal<SyntheticKeybindInput> CURRENT = new ThreadLocal<>();

    private final KeyBinding target;

    private final InputMappings.Input key = InputMappings.getKey(-1, -1);

    private SyntheticKeybindInput(KeyBinding target) {
        this.target = target;
    }

    public static SyntheticKeybindInput current() {
        return CURRENT.get();
    }

    public Boolean matches(KeyBinding mapping, int keyCode, int scanCode) {
        if (keyCode != -1 || scanCode != -1) {
            return null;
        }
        return mapping == target;
    }

    public boolean routes(InputMappings.Input candidate) {
        return key.equals(candidate);
    }

    public KeyBinding target() {
        return target;
    }

    public static void dispatch(Minecraft minecraft, KeyBinding mapping, int action) {
        if (LegacyKeyMappings.dispatch(minecraft, mapping, action))
            return;
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
