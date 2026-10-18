package de.artemis.omniwheel.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {

    @Invoker("startAttack")
    void omniwheel$startAttack();

    @Invoker("startUseItem")
    void omniwheel$startUseItem();

    @Invoker("pickBlock")
    void omniwheel$pickBlock();
}
