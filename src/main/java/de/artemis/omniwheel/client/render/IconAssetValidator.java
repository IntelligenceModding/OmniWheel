package de.artemis.omniwheel.client.render;

import net.minecraft.client.renderer.texture.NativeImage;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Direction;
import java.util.Random;
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.vector.Vector3f;
import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Checks the assets actually used for GUI rendering, including resource-pack overrides.
 */
public final class IconAssetValidator {

    private static final Map<TextureAtlasSprite, Boolean> SPRITES = new IdentityHashMap<>();

    private static final float UV_EPSILON = 0.00001F;

    private IconAssetValidator() {
    }

    public static void clear() {
        SPRITES.clear();
    }

    public static boolean guiSprite(Minecraft minecraft, ResourceLocation id) {
        return LegacyGuiIcons.isUsable(minecraft, id);
    }

    public static boolean sprite(TextureAtlasSprite sprite) {
        if (sprite == null) {
            return false;
        }
        return SPRITES.computeIfAbsent(sprite, IconAssetValidator::checkSprite);
    }

    private static boolean checkSprite(TextureAtlasSprite sprite) {
        TextureAtlasSprite contents = sprite;
        if (MissingTextureSprite.getLocation().equals(contents.getName()) || contents.getWidth() <= 0 || contents.getHeight() <= 0 || !validRange(sprite.getU0(), sprite.getU1()) || !validRange(sprite.getV0(), sprite.getV1())) {
            return false;
        }
        return visiblePixels(contents, 0, 0, contents.getWidth(), contents.getHeight());
    }

    private static boolean validRange(float start, float end) {
        return Float.isFinite(start) && Float.isFinite(end) && start >= 0 && end <= 1 && start < end;
    }

    private static boolean visiblePixels(TextureAtlasSprite contents, int minX, int minY, int maxX, int maxY) {
        for (int frame = 0; frame < contents.getFrameCount(); frame++) {
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
        if (stack.isEmpty())
            return false;
        net.minecraft.client.renderer.model.IBakedModel model = minecraft.getItemRenderer().getModel(stack, minecraft.level, minecraft.player);
        if (model == null)
            return false;
        net.minecraft.client.renderer.model.ItemTransformVec3f transform = model.getTransforms().getTransform(TransformType.GUI);
        if (!isFinite(transform.rotation) || !isFinite(transform.translation) || !isFinite(transform.scale)) {
            return false;
        }
        MatrixStack pose = new MatrixStack();
        transform.apply(false, pose);
        pose.translate(-0.5F, -0.5F, -0.5F);
        if (model.isCustomRenderer()) {
            // Built-in renderers (chests, shields and mod renderers) do not use baked quads.
            return hasVisibleExtents(pose, de.artemis.omniwheel.common.Java8.list(), true);
        }
        List<BakedQuad> quads = new ArrayList<>();
        Random random = new Random(42);
        // Match ItemRenderer's directional and unculled model queries.
        for (Direction direction : Direction.values()) {
            random.setSeed(42);
            quads.addAll(model.getQuads(null, direction, random));
        }
        random.setSeed(42);
        quads.addAll(model.getQuads(null, null, random));
        boolean visibleQuads = false;
        for (BakedQuad quad : quads) {
            int result = checkQuad(quad);
            if (result < 0) {
                return false;
            }
            visibleQuads |= result > 0;
        }
        return visibleQuads && hasVisibleExtents(pose, quads, false);
    }

    // -1: broken data; 0: valid but invisible geometry; 1: visible geometry.
    private static int checkQuad(BakedQuad quad) {
        TextureAtlasSprite sprite = quad.getSprite();
        if (sprite == null || MissingTextureSprite.getLocation().equals(sprite.getName())) {
            return -1;
        }
        float minU = Float.POSITIVE_INFINITY, minV = Float.POSITIVE_INFINITY;
        float maxU = Float.NEGATIVE_INFINITY, maxV = Float.NEGATIVE_INFINITY;
        int[] vertices = quad.getVertices();
        if (vertices.length % 4 != 0 || vertices.length / 4 < 6)
            return -1;
        int stride = vertices.length / 4;
        Vector3f[] positions = new Vector3f[4];
        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * stride;
            net.minecraft.util.math.vector.Vector3f position = positions[vertex] = new Vector3f(Float.intBitsToFloat(vertices[offset]), Float.intBitsToFloat(vertices[offset + 1]), Float.intBitsToFloat(vertices[offset + 2]));
            float u = Float.intBitsToFloat(vertices[offset + 4]);
            float v = Float.intBitsToFloat(vertices[offset + 5]);
            if (!Float.isFinite(position.x()) || !Float.isFinite(position.y()) || !Float.isFinite(position.z()) || !Float.isFinite(u) || !Float.isFinite(v) || u < sprite.getU0() - UV_EPSILON || u > sprite.getU1() + UV_EPSILON || v < sprite.getV0() - UV_EPSILON || v > sprite.getV1() + UV_EPSILON) {
                return -1;
            }
            minU = Math.min(minU, u);
            minV = Math.min(minV, v);
            maxU = Math.max(maxU, u);
            maxV = Math.max(maxV, v);
        }
        float area0 = triangleAreaSquared(positions[0], positions[1], positions[2]);
        float area1 = triangleAreaSquared(positions[0], positions[2], positions[3]);
        if ((area0 <= 0.000000000001F && area1 <= 0.000000000001F) || !sprite(sprite)) {
            return 0;
        }
        TextureAtlasSprite contents = sprite;
        int x0 = de.artemis.omniwheel.common.Numbers.clamp((int) Math.floor((minU - sprite.getU0()) / (sprite.getU1() - sprite.getU0()) * contents.getWidth()), 0, contents.getWidth() - 1);
        int y0 = de.artemis.omniwheel.common.Numbers.clamp((int) Math.floor((minV - sprite.getV0()) / (sprite.getV1() - sprite.getV0()) * contents.getHeight()), 0, contents.getHeight() - 1);
        int x1 = de.artemis.omniwheel.common.Numbers.clamp((int) Math.ceil((maxU - sprite.getU0()) / (sprite.getU1() - sprite.getU0()) * contents.getWidth()), x0 + 1, contents.getWidth());
        int y1 = de.artemis.omniwheel.common.Numbers.clamp((int) Math.ceil((maxV - sprite.getV0()) / (sprite.getV1() - sprite.getV0()) * contents.getHeight()), y0 + 1, contents.getHeight());
        if (visiblePixels(contents, x0, y0, x1, y1)) {
            return 1;
        } else {
            return 0;
        }
    }

    private static boolean isFinite(Vector3f value) {
        return Float.isFinite(value.x()) && Float.isFinite(value.y()) && Float.isFinite(value.z());
    }

    private static float triangleAreaSquared(Vector3f a, Vector3f b, Vector3f c) {
        Vector3f ab = b.copy();
        ab.sub(a);
        Vector3f ac = c.copy();
        ac.sub(a);
        ab.cross(ac);
        return ab.dot(ab);
    }

    private static boolean hasVisibleExtents(MatrixStack pose, List<BakedQuad> quads, boolean special) {
        float[] bounds = { Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY };
        boolean[] finite = { true };
        java.util.function.Consumer<Vector3f> visit = point -> {
            net.minecraft.util.math.vector.Vector4f transformed = new net.minecraft.util.math.vector.Vector4f(point.x(), point.y(), point.z(), 1);
            transformed.transform(pose.last().pose());
            point.set(transformed.x(), transformed.y(), transformed.z());
            finite[0] &= Float.isFinite(point.x()) && Float.isFinite(point.y()) && Float.isFinite(point.z());
            bounds[0] = Math.min(bounds[0], point.x());
            bounds[1] = Math.min(bounds[1], point.y());
            bounds[2] = Math.min(bounds[2], point.z());
            bounds[3] = Math.max(bounds[3], point.x());
            bounds[4] = Math.max(bounds[4], point.y());
            bounds[5] = Math.max(bounds[5], point.z());
        };
        if (special) {
            for (int corner = 0; corner < 8; corner++) {
                visit.accept(new Vector3f(corner & 1, (corner >> 1) & 1, (corner >> 2) & 1));
            }
        } else {
            for (BakedQuad quad : quads) {
                int[] vertices = quad.getVertices();
                int stride = vertices.length / 4;
                for (int vertex = 0; vertex < 4; vertex++) {
                    int offset = vertex * stride;
                    visit.accept(new Vector3f(Float.intBitsToFloat(vertices[offset]), Float.intBitsToFloat(vertices[offset + 1]), Float.intBitsToFloat(vertices[offset + 2])));
                }
            }
        }
        int dimensions = 0;
        for (int axis = 0; axis < 3; axis++) {
            if (bounds[axis + 3] - bounds[axis] > 0.000001F) {
                dimensions++;
            }
        }
        return finite[0] && dimensions >= 2;
    }

    public static boolean texture(Minecraft minecraft, ResourceLocation id, int textureWidth, int textureHeight, int u, int v, int width, int height) {
        if (textureWidth <= 0 || textureHeight <= 0 || u < 0 || v < 0 || width <= 0 || height <= 0 || (long) u + width > textureWidth || (long) v + height > textureHeight) {
            return false;
        }
        try (net.minecraft.resources.IResource resource = minecraft.getResourceManager().getResource(id);
            java.io.InputStream stream = resource.getInputStream();
            NativeImage image = NativeImage.read(stream)) {
            int x0 = (int) ((long) u * image.getWidth() / textureWidth);
            int y0 = (int) ((long) v * image.getHeight() / textureHeight);
            int x1 = (int) ((long) (u + width) * image.getWidth() / textureWidth);
            int y1 = (int) ((long) (v + height) * image.getHeight() / textureHeight);
            for (int y = y0; y < y1; y++) {
                for (int x = x0; x < x1; x++) {
                    if ((image.getPixelRGBA(x, y) >>> 24) != 0) {
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
