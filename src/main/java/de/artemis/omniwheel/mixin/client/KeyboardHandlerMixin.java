package de.artemis.omniwheel.mixin.client;

import de.artemis.omniwheel.client.input.ShortcutInputState;
import de.artemis.omniwheel.client.input.SyntheticKeybindInput;
import de.artemis.omniwheel.client.runtime.OmniWheelClientRuntime;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void omniwheel$trackShortcut(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (window == Minecraft.getInstance().getWindow().handle() && SyntheticKeybindInput.current() == null) {
            ShortcutInputState.keyboard(event.key(), event.scancode(), action);
            if (OmniWheelClientRuntime.getInstance().handleTutorialKey(event.key(), event.modifiers(), action)) ci.cancel();
        }
    }

    @Inject(method = "keyPress", at = @At("RETURN"))
    private void omniwheel$dispatchShortcut(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (window == Minecraft.getInstance().getWindow().handle() && SyntheticKeybindInput.current() == null) {
            OmniWheelClientRuntime.getInstance().shortcutInputChanged(action);
        }
    }
}
