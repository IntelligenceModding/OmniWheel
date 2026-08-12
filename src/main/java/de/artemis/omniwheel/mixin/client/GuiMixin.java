package de.artemis.omniwheel.mixin.client;

import de.artemis.omniwheel.client.runtime.OmniWheelClientRuntime;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void omniwheel$hideCrosshair(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo callbackInfo) {
        if (OmniWheelClientRuntime.getInstance().isOverlayUiOpen()) {
            callbackInfo.cancel();
        }
    }
}
