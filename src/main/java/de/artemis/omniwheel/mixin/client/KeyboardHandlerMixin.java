package de.artemis.omniwheel.mixin.client;

import de.artemis.omniwheel.client.input.ShortcutInputState;
import de.artemis.omniwheel.client.input.SyntheticKeybindInput;
import de.artemis.omniwheel.client.runtime.OmniWheelClientRuntime;
import net.minecraft.client.KeyboardListener;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardListener.class)
public abstract class KeyboardHandlerMixin {

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void omniwheel$trackShortcut(long window, int keyCode, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (window == Minecraft.getInstance().getWindow().getWindow() && SyntheticKeybindInput.current() == null) {
            ShortcutInputState.keyboard(keyCode, scanCode, action);
            if (OmniWheelClientRuntime.getInstance().handleTutorialKey(keyCode, modifiers, action))
                ci.cancel();
        }
    }

    @Inject(method = "keyPress", at = @At("RETURN"))
    private void omniwheel$dispatchShortcut(long window, int keyCode, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (window == Minecraft.getInstance().getWindow().getWindow() && SyntheticKeybindInput.current() == null) {
            OmniWheelClientRuntime.getInstance().shortcutInputChanged(action);
        }
    }
}
