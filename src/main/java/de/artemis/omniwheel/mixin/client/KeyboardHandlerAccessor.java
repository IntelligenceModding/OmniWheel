package de.artemis.omniwheel.mixin.client;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(KeyboardHandler.class)
public interface KeyboardHandlerAccessor {
    @Invoker("keyPress")
    void omniwheel$keyPress(long window, int action, KeyEvent event);

    @Invoker("handleDebugKeys")
    boolean omniwheel$handleDebugKeys(KeyEvent event);

    @Accessor("handledDebugKey")
    boolean omniwheel$getHandledDebugKey();

    @Accessor("handledDebugKey")
    void omniwheel$setHandledDebugKey(boolean handled);
}
