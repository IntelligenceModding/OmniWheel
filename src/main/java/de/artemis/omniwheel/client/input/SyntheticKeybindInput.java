package de.artemis.omniwheel.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import de.artemis.omniwheel.mixin.client.KeyboardHandlerAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;

public final class SyntheticKeybindInput {
    private static final ThreadLocal<SyntheticKeybindInput> CURRENT = new ThreadLocal<>();

    private final KeyMapping target;
    private final KeyMapping debugModifier;
    private final KeyMapping debugOverlay;
    private final boolean overlaySharesModifier;
    private final boolean needsDebugModifier;
    private final KeyEvent event = new KeyEvent(-1, -1, 0);
    private final InputConstants.Key key = InputConstants.getKey(event);

    private SyntheticKeybindInput(Minecraft minecraft, KeyMapping target) {
        this.target = target;
        debugModifier = minecraft.options.keyDebugModifier;
        debugOverlay = minecraft.options.keyDebugOverlay;
        overlaySharesModifier = OmniWheelKeyMappings.currentKey(debugOverlay)
                .equals(OmniWheelKeyMappings.currentKey(debugModifier));
        needsDebugModifier = target.getCategory().equals(KeyMapping.Category.DEBUG)
                && target != debugModifier && target != debugOverlay;
    }

    public static SyntheticKeybindInput current() {
        return CURRENT.get();
    }

    public Boolean matches(KeyMapping mapping, KeyEvent candidate) {
        if (candidate != event) {
            return null;
        }
        return mapping == target || (target == debugOverlay && overlaySharesModifier && mapping == debugModifier);
    }

    public Boolean isDown(KeyMapping mapping) {
        return mapping == debugModifier && mapping != target ? needsDebugModifier : null;
    }

    public boolean routes(InputConstants.Key candidate) {
        return key.equals(candidate);
    }

    public KeyMapping target() {
        return target;
    }

    public static void dispatch(Minecraft minecraft, KeyMapping mapping, int action) {
        SyntheticKeybindInput previous = CURRENT.get();
        SyntheticKeybindInput input = new SyntheticKeybindInput(minecraft, mapping);
        KeyboardHandlerAccessor keyboard = (KeyboardHandlerAccessor) minecraft.keyboardHandler;
        boolean usedDebugModifier = keyboard.omniwheel$getUsedDebugKeyAsModifier();
        CURRENT.set(input);
        try {
            keyboard.omniwheel$keyPress(minecraft.getWindow().handle(), action, input.event);
        } finally {
            keyboard.omniwheel$setUsedDebugKeyAsModifier(usedDebugModifier);
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}
