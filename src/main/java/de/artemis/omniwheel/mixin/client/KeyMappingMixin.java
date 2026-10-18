package de.artemis.omniwheel.mixin.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.artemis.omniwheel.client.input.SyntheticKeybindInput;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = KeyMapping.class, priority = 1100)
public abstract class KeyMappingMixin {
    @Inject(method = "matches", at = @At("HEAD"), cancellable = true)
    private void omniwheel$matchesSyntheticInput(int keyCode, int scanCode, CallbackInfoReturnable<Boolean> cir) {
        SyntheticKeybindInput input = SyntheticKeybindInput.current();
        if (input != null) {
            Boolean matches = input.matches((KeyMapping) (Object) this, keyCode, scanCode);
            if (matches != null) {
                cir.setReturnValue(matches);
            }
        }
    }

    // Route the virtual event directly to its target, even when it is unbound.
    // Never broadcast to other mappings sharing a key (including the unbound key).
    @Inject(method = "click", at = @At("HEAD"), cancellable = true)
    private static void omniwheel$routeSyntheticClick(InputConstants.Key key, CallbackInfo ci) {
        SyntheticKeybindInput input = SyntheticKeybindInput.current();
        if (input != null && input.routes(key)) {
            var target = (KeyMappingAccessor) input.target();
            target.omniwheel$setClickCount(target.omniwheel$getClickCount() + 1);
            ci.cancel();
        }
    }

    @Inject(method = "set", at = @At("HEAD"), cancellable = true)
    private static void omniwheel$routeSyntheticState(InputConstants.Key key, boolean down, CallbackInfo ci) {
        SyntheticKeybindInput input = SyntheticKeybindInput.current();
        if (input != null && input.routes(key)) {
            input.target().setDown(down);
            ci.cancel();
        }
    }
}
