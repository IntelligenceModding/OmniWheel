package de.artemis.omniwheel.mixin.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.artemis.omniwheel.client.input.SyntheticKeybindInput;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {
    @Inject(method = "matches(Lnet/minecraft/client/input/KeyEvent;)Z", at = @At("HEAD"), cancellable = true)
    private void omniwheel$matchesSyntheticInput(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        SyntheticKeybindInput input = SyntheticKeybindInput.current();
        if (input != null) {
            Boolean matches = input.matches((KeyMapping) (Object) this, event);
            if (matches != null) {
                cir.setReturnValue(matches);
            }
        }
    }

    // Route the virtual event directly to its target, even when it is unbound.
    // Never broadcast to other mappings sharing a key (including the unbound key).
    @Inject(method = "forAllKeyMappings", at = @At("HEAD"), cancellable = true)
    private static void omniwheel$routeSyntheticInput(InputConstants.Key key, Consumer<KeyMapping> consumer, CallbackInfo ci) {
        SyntheticKeybindInput input = SyntheticKeybindInput.current();
        if (input != null && input.routes(key)) {
            consumer.accept(input.target());
            ci.cancel();
        }
    }
}
