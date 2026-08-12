package de.artemis.omniwheel.mixin.client;

import de.artemis.omniwheel.client.runtime.OmniWheelClientRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void omniwheel$handleMouseButton(long windowPointer, int button, int action, int modifiers, CallbackInfo callbackInfo) {
        if (windowPointer == minecraft.getWindow().getWindow()
                && OmniWheelClientRuntime.getInstance().handleMouseButton(button, action)) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void omniwheel$handleMouseScroll(long windowPointer, double xOffset, double yOffset, CallbackInfo callbackInfo) {
        if (windowPointer == minecraft.getWindow().getWindow()
                && OmniWheelClientRuntime.getInstance().handleMouseScroll(xOffset, yOffset)) {
            callbackInfo.cancel();
        }
    }
}
