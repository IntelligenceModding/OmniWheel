package de.artemis.omniwheel.mixin.client;

import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.texture.AtlasTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ParticleManager.class)
public interface ParticleEngineAccessor {

    @Accessor("textureAtlas")
    AtlasTexture omniwheel$getTextureAtlas();
}
