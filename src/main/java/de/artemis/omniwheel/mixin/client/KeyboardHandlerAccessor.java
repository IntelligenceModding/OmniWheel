package de.artemis.omniwheel.mixin.client;

import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(KeyboardHandler.class)
public interface KeyboardHandlerAccessor {
    @Invoker("keyPress")
    void omniwheel$keyPress(long window, int keyCode, int scanCode, int action, int modifiers);

    @Invoker("handleDebugKeys")
    boolean omniwheel$handleDebugKeys(int keyCode);

    @Accessor("handledDebugKey")
    boolean omniwheel$getHandledDebugKey();

    @Accessor("handledDebugKey")
    void omniwheel$setHandledDebugKey(boolean handled);
}
