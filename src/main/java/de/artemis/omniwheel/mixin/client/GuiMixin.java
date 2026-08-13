package de.artemis.omniwheel.mixin.client;

import de.artemis.omniwheel.client.runtime.OmniWheelClientRuntime;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class GuiMixin {
    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void omniwheel$hideCrosshair(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo callbackInfo) {
        if (OmniWheelClientRuntime.getInstance().isOverlayUiOpen()) {
            callbackInfo.cancel();
        }
    }
}
