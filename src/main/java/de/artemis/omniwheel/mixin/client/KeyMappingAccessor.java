package de.artemis.omniwheel.mixin.client;

import net.minecraft.client.util.InputMappings;
import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyBinding.class)
public interface KeyMappingAccessor {

    @Accessor("key")
    InputMappings.Input omniwheel$getKey();

    @Accessor("clickCount")
    int omniwheel$getClickCount();

    @Accessor("clickCount")
    void omniwheel$setClickCount(int count);
}
