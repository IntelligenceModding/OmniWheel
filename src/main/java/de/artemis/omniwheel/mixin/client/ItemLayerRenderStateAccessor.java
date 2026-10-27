package de.artemis.omniwheel.mixin.client;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface ItemLayerRenderStateAccessor {
    @Accessor("quads")
    ItemQuads omniwheel$getQuads();

    @Accessor("specialRenderer")
    SpecialModelRenderer<?> omniwheel$getSpecialRenderer();

    @Accessor("localTransform")
    Matrix4f omniwheel$getLocalTransform();
}
