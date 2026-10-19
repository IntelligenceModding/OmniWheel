package de.artemis.omniwheel.mixin.client;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface ItemLayerRenderStateAccessor {
    @Accessor("quads")
    List<BakedQuad> omniwheel$getQuads();

    @Accessor("specialRenderer")
    SpecialModelRenderer<?> omniwheel$getSpecialRenderer();

    @Accessor("localTransform")
    Matrix4f omniwheel$getLocalTransform();
}
