package de.artemis.omniwheel.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import de.artemis.omniwheel.mixin.client.ItemLayerRenderStateAccessor;
import de.artemis.omniwheel.mixin.client.ItemStackRenderStateAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;

/** Checks the assets actually used for GUI rendering, including resource-pack overrides. */
public final class IconAssetValidator {
    private static final Map<TextureAtlasSprite, Boolean> SPRITES = new IdentityHashMap<>();
    private static final float UV_EPSILON = 0.00001F;

    private IconAssetValidator() {
    }

    public static void clear() {
        SPRITES.clear();
    }

    public static boolean guiSprite(Minecraft minecraft, Identifier id) {
        return sprite(minecraft.getAtlasManager().getAtlasOrThrow(AtlasIds.GUI).getSprite(id));
    }

    public static boolean sprite(TextureAtlasSprite sprite) {
        if (sprite == null) {
            return false;
        }
        return SPRITES.computeIfAbsent(sprite, IconAssetValidator::checkSprite);
    }

    private static boolean checkSprite(TextureAtlasSprite sprite) {
        SpriteContents contents = sprite.contents();
        if (MissingTextureAtlasSprite.getLocation().equals(contents.name())
                || contents.width() <= 0 || contents.height() <= 0
                || !validRange(sprite.getU0(), sprite.getU1())
                || !validRange(sprite.getV0(), sprite.getV1())) {
            return false;
        }
        return visiblePixels(contents, 0, 0, contents.width(), contents.height());
    }

    private static boolean validRange(float start, float end) {
        return Float.isFinite(start) && Float.isFinite(end) && start >= 0 && end <= 1 && start < end;
    }

    private static boolean visiblePixels(SpriteContents contents, int minX, int minY, int maxX, int maxY) {
        for (int frame : contents.getUniqueFrames().toArray()) {
            for (int y = minY; y < maxY; y++) {
                for (int x = minX; x < maxX; x++) {
                    if (!contents.isTransparent(frame, x, y)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean item(Minecraft minecraft, ItemStack stack) {
        ItemStackRenderState state = new ItemStackRenderState();
        minecraft.getItemModelResolver().updateForTopItem(state, stack, ItemDisplayContext.GUI,
                minecraft.level, minecraft.player, 0);
        if (state.isEmpty()) {
            return false;
        }
        ItemStackRenderStateAccessor access = (ItemStackRenderStateAccessor) state;
        boolean visible = false;
        for (int index = 0; index < access.omniwheel$getActiveLayerCount(); index++) {
            ItemLayerRenderStateAccessor layer = (ItemLayerRenderStateAccessor) access.omniwheel$getLayers()[index];
            var transform = layer.omniwheel$getTransform();
            if (!transform.rotation().isFinite() || !transform.translation().isFinite() || !transform.scale().isFinite()) {
                return false;
            }
            if (layer.omniwheel$getSpecialRenderer() != null) {
                // Special renderers (chests, shields and mod renderers) do not use baked quads.
                visible = true;
            }
            for (BakedQuad quad : layer.omniwheel$getQuads()) {
                int result = checkQuad(quad);
                if (result < 0) {
                    return false;
                }
                visible |= result > 0;
            }
        }
        return visible && hasVisibleExtents(state);
    }

    // -1: broken data; 0: valid but invisible geometry; 1: visible geometry.
    private static int checkQuad(BakedQuad quad) {
        TextureAtlasSprite sprite = quad.sprite();
        if (sprite == null || MissingTextureAtlasSprite.getLocation().equals(sprite.contents().name())) {
            return -1;
        }
        float minU = Float.POSITIVE_INFINITY, minV = Float.POSITIVE_INFINITY;
        float maxU = Float.NEGATIVE_INFINITY, maxV = Float.NEGATIVE_INFINITY;
        for (int vertex = 0; vertex < 4; vertex++) {
            var position = quad.position(vertex);
            float u = UVPair.unpackU(quad.packedUV(vertex));
            float v = UVPair.unpackV(quad.packedUV(vertex));
            if (!Float.isFinite(position.x()) || !Float.isFinite(position.y()) || !Float.isFinite(position.z())
                    || !Float.isFinite(u) || !Float.isFinite(v)
                    || u < sprite.getU0() - UV_EPSILON || u > sprite.getU1() + UV_EPSILON
                    || v < sprite.getV0() - UV_EPSILON || v > sprite.getV1() + UV_EPSILON) {
                return -1;
            }
            minU = Math.min(minU, u);
            minV = Math.min(minV, v);
            maxU = Math.max(maxU, u);
            maxV = Math.max(maxV, v);
        }
        float area0 = new Vector3f(quad.position1()).sub(quad.position0())
                .cross(new Vector3f(quad.position2()).sub(quad.position0())).lengthSquared();
        float area1 = new Vector3f(quad.position2()).sub(quad.position0())
                .cross(new Vector3f(quad.position3()).sub(quad.position0())).lengthSquared();
        if ((area0 <= 0.000000000001F && area1 <= 0.000000000001F) || !sprite(sprite)) {
            return 0;
        }
        SpriteContents contents = sprite.contents();
        int x0 = Math.clamp((int) Math.floor((minU - sprite.getU0()) / (sprite.getU1() - sprite.getU0()) * contents.width()), 0, contents.width() - 1);
        int y0 = Math.clamp((int) Math.floor((minV - sprite.getV0()) / (sprite.getV1() - sprite.getV0()) * contents.height()), 0, contents.height() - 1);
        int x1 = Math.clamp((int) Math.ceil((maxU - sprite.getU0()) / (sprite.getU1() - sprite.getU0()) * contents.width()), x0 + 1, contents.width());
        int y1 = Math.clamp((int) Math.ceil((maxV - sprite.getV0()) / (sprite.getV1() - sprite.getV0()) * contents.height()), y0 + 1, contents.height());
        return visiblePixels(contents, x0, y0, x1, y1) ? 1 : 0;
    }

    private static boolean hasVisibleExtents(ItemStackRenderState state) {
        float[] bounds = {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};
        boolean[] finite = {true};
        state.visitExtents(point -> {
            finite[0] &= Float.isFinite(point.x()) && Float.isFinite(point.y()) && Float.isFinite(point.z());
            bounds[0] = Math.min(bounds[0], point.x());
            bounds[1] = Math.min(bounds[1], point.y());
            bounds[2] = Math.min(bounds[2], point.z());
            bounds[3] = Math.max(bounds[3], point.x());
            bounds[4] = Math.max(bounds[4], point.y());
            bounds[5] = Math.max(bounds[5], point.z());
        });
        int dimensions = 0;
        for (int axis = 0; axis < 3; axis++) {
            if (bounds[axis + 3] - bounds[axis] > 0.000001F) {
                dimensions++;
            }
        }
        return finite[0] && dimensions >= 2;
    }

    public static boolean texture(Minecraft minecraft, Identifier id, int textureWidth, int textureHeight,
                                  int u, int v, int width, int height) {
        if (textureWidth <= 0 || textureHeight <= 0 || u < 0 || v < 0 || width <= 0 || height <= 0
                || (long) u + width > textureWidth || (long) v + height > textureHeight) {
            return false;
        }
        var resource = minecraft.getResourceManager().getResource(id);
        if (resource.isEmpty()) {
            return false;
        }
        try (var stream = resource.get().open(); NativeImage image = NativeImage.read(stream)) {
            int x0 = (int) ((long) u * image.getWidth() / textureWidth);
            int y0 = (int) ((long) v * image.getHeight() / textureHeight);
            int x1 = (int) ((long) (u + width) * image.getWidth() / textureWidth);
            int y1 = (int) ((long) (v + height) * image.getHeight() / textureHeight);
            for (int y = y0; y < y1; y++) {
                for (int x = x0; x < x1; x++) {
                    if ((image.getPixel(x, y) >>> 24) != 0) {
                        return true;
                    }
                }
            }
            return false;
        } catch (IOException | RuntimeException ignored) {
            return false;
        }
    }
}
