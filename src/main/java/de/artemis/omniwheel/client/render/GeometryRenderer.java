package de.artemis.omniwheel.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

import static de.artemis.omniwheel.OmniWheel.MOD_ID;

public final class GeometryRenderer {
    private static final int CACHE_LIMIT = 256;
    private static final double FULL_CIRCLE = Math.PI * 2.0D;
    private static final double FULL_CIRCLE_EPSILON = 1.0E-4D;
    private static final float BOUNDS_PADDING = 2.0F;
    private static final Map<ShapeKey, CachedMesh> CACHE = new LinkedHashMap<>(64, 0.75F, true);

    private GeometryRenderer() {
    }

    public static void fillCircle(GuiGraphicsExtractor graphics, float centerX, float centerY, float radius, int color) {
        fillRingSegment(graphics, centerX, centerY, 0.0F, radius, 0.0D, FULL_CIRCLE, 0.0D, FULL_CIRCLE, color);
    }

    public static void fillDisc(GuiGraphicsExtractor graphics, float centerX, float centerY, float radius, int color) {
        if (radius <= 0.0F) {
            return;
        }

        int radialBands = Math.max(6, (int) Math.ceil(radius / 6.0F));
        for (int band = 0; band < radialBands; band++) {
            float innerRadius = (radius * band) / radialBands;
            float outerRadius = (radius * (band + 1)) / radialBands;
            fillRingSegment(
                    graphics,
                    centerX,
                    centerY,
                    innerRadius,
                    outerRadius,
                    0.0D,
                    FULL_CIRCLE,
                    0.0D,
                    FULL_CIRCLE,
                    color
            );
        }
    }

    public static void fillRingSegment(
            GuiGraphicsExtractor graphics,
            float centerX,
            float centerY,
            float innerRadius,
            float outerRadius,
            double startAngle,
            double endAngle,
            int color
    ) {
        fillRingSegment(graphics, centerX, centerY, innerRadius, outerRadius, startAngle, endAngle, startAngle, endAngle, color);
    }

    public static void fillRingSegment(
            GuiGraphicsExtractor graphics,
            float centerX,
            float centerY,
            float innerRadius,
            float outerRadius,
            double innerStartAngle,
            double innerEndAngle,
            double outerStartAngle,
            double outerEndAngle,
            int color
    ) {
        if (outerRadius <= 0.0F || outerRadius <= innerRadius) {
            return;
        }

        CachedMesh mesh = getOrCreateMesh(new ShapeKey(
                innerRadius,
                outerRadius,
                innerStartAngle,
                innerEndAngle,
                outerStartAngle,
                outerEndAngle
        ));
        if (mesh.isEmpty()) {
            return;
        }

        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor = graphics.peekScissorStack();
        ScreenRectangle bounds = boundsFor(centerX, centerY, outerRadius, pose, scissor);
        if (bounds == null) {
            return;
        }

        graphics.submitGuiElementRenderState(new PolygonRenderState(mesh, pose, centerX, centerY, color, scissor, bounds));
    }

    private static CachedMesh getOrCreateMesh(ShapeKey key) {
        CachedMesh cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        CachedMesh created = buildMesh(key);
        CACHE.put(key, created);
        trimCache();
        return created;
    }

    private static void trimCache() {
        while (CACHE.size() > CACHE_LIMIT) {
            ShapeKey eldestKey = CACHE.keySet().iterator().next();
            CACHE.remove(eldestKey);
        }
    }

    private static CachedMesh buildMesh(ShapeKey key) {
        double span = Math.max(Math.abs(key.innerEndAngle() - key.innerStartAngle()), Math.abs(key.outerEndAngle() - key.outerStartAngle()));
        boolean fullCircle = isFullCircle(key);
        int steps = fullCircle
                ? Math.max(48, (int) Math.ceil(key.outerRadius() * 1.8F))
                : Math.max(18, (int) Math.ceil(span * Math.max(18.0D, key.outerRadius() * 0.7D)));
        float[] vertices = new float[steps * 8];
        int vertexIndex = 0;

        for (int i = 0; i < steps; i++) {
            double startProgress = (double) i / (double) steps;
            double endProgress = (double) (i + 1) / (double) steps;

            double outerAngle0 = lerp(key.outerStartAngle(), key.outerEndAngle(), startProgress);
            double outerAngle1 = lerp(key.outerStartAngle(), key.outerEndAngle(), endProgress);
            double innerAngle0 = lerp(key.innerStartAngle(), key.innerEndAngle(), startProgress);
            double innerAngle1 = lerp(key.innerStartAngle(), key.innerEndAngle(), endProgress);

            float outerX0 = (float) Math.cos(outerAngle0) * key.outerRadius();
            float outerY0 = (float) Math.sin(outerAngle0) * key.outerRadius();
            float outerX1 = (float) Math.cos(outerAngle1) * key.outerRadius();
            float outerY1 = (float) Math.sin(outerAngle1) * key.outerRadius();
            float innerX0 = (float) Math.cos(innerAngle0) * key.innerRadius();
            float innerY0 = (float) Math.sin(innerAngle0) * key.innerRadius();
            float innerX1 = (float) Math.cos(innerAngle1) * key.innerRadius();
            float innerY1 = (float) Math.sin(innerAngle1) * key.innerRadius();

            vertices[vertexIndex++] = outerX0;
            vertices[vertexIndex++] = outerY0;
            vertices[vertexIndex++] = outerX1;
            vertices[vertexIndex++] = outerY1;
            vertices[vertexIndex++] = innerX1;
            vertices[vertexIndex++] = innerY1;
            vertices[vertexIndex++] = innerX0;
            vertices[vertexIndex++] = innerY0;
        }

        return new CachedMesh(vertices);
    }

    private static boolean isFullCircle(ShapeKey key) {
        return Math.abs(key.outerEndAngle() - key.outerStartAngle()) >= FULL_CIRCLE - FULL_CIRCLE_EPSILON
                && Math.abs(key.innerEndAngle() - key.innerStartAngle()) >= FULL_CIRCLE - FULL_CIRCLE_EPSILON;
    }

    @Nullable
    private static ScreenRectangle boundsFor(float centerX, float centerY, float outerRadius, Matrix3x2f pose, @Nullable ScreenRectangle scissor) {
        int radius = (int) Math.ceil(outerRadius + BOUNDS_PADDING);
        int left = (int) Math.floor(centerX) - radius;
        int top = (int) Math.floor(centerY) - radius;
        int size = (radius * 2) + 1;
        ScreenRectangle transformed = new ScreenRectangle(left, top, size, size).transformMaxBounds(pose);
        if (scissor != null) {
            return scissor.intersection(transformed);
        }
        return transformed;
    }

    private static double lerp(double start, double end, double progress) {
        return start + ((end - start) * progress);
    }

    private record ShapeKey(
            float innerRadius,
            float outerRadius,
            double innerStartAngle,
            double innerEndAngle,
            double outerStartAngle,
            double outerEndAngle
    ) {
    }

    private record CachedMesh(float[] vertices) {
        private boolean isEmpty() {
            return vertices.length == 0;
        }
    }

    private record PolygonRenderState(
            CachedMesh mesh,
            Matrix3x2f pose,
            float originX,
            float originY,
            int color,
            @Nullable ScreenRectangle scissorArea,
            @Nullable ScreenRectangle bounds
    ) implements GuiElementRenderState {
        private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath(MOD_ID, "gui_polygon"))
                .withCull(false)
                .build();
        private static final TextureSetup TEXTURE_SETUP = TextureSetup.noTexture();

        @Override
        public void buildVertices(VertexConsumer consumer) {
            float[] vertices = mesh.vertices();
            for (int index = 0; index < vertices.length; index += 8) {
                consumer.addVertexWith2DPose(pose, originX + vertices[index], originY + vertices[index + 1]).setColor(color);
                consumer.addVertexWith2DPose(pose, originX + vertices[index + 2], originY + vertices[index + 3]).setColor(color);
                consumer.addVertexWith2DPose(pose, originX + vertices[index + 4], originY + vertices[index + 5]).setColor(color);
                consumer.addVertexWith2DPose(pose, originX + vertices[index + 6], originY + vertices[index + 7]).setColor(color);
            }
        }

        @Override
        public RenderPipeline pipeline() {
            return PIPELINE;
        }

        @Override
        public TextureSetup textureSetup() {
            return TEXTURE_SETUP;
        }
    }
}
