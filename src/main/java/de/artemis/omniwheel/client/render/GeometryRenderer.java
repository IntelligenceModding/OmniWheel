package de.artemis.omniwheel.client.render;

import net.minecraft.client.gui.GuiGraphics;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

import java.util.LinkedHashMap;
import java.util.Map;

public final class GeometryRenderer {
    private static final int CACHE_LIMIT = 256;
    private static final double FULL_CIRCLE = Math.PI * 2.0D;
    private static final double FULL_CIRCLE_EPSILON = 1.0E-4D;
    private static final Map<ShapeKey, CachedMesh> CACHE = new LinkedHashMap<>(64, 0.75F, true);
    private static final CachedMesh CHEVRON_RIGHT = new CachedMesh(new float[]{
            -5, -12, 7, 0, 3, 0, -7, -10,
            7, 0, -5, 12, -7, 10, 3, 0
    });
    private static final CachedMesh CHEVRON_LEFT = new CachedMesh(new float[]{
            5, -12, -7, 0, -3, 0, 7, -10,
            -7, 0, 5, 12, 7, 10, -3, 0
    });

    private GeometryRenderer() {
    }

    public static void drawChevron(GuiGraphics graphics, float x, float y, int direction, int color) {
        drawMesh(graphics, direction < 0 ? CHEVRON_LEFT : CHEVRON_RIGHT, x, y, color);
    }

    public static void drawDisclosureChevron(GuiGraphics graphics, float x, float y, boolean expanded, int color) {
        drawDisclosureChevron(graphics, x, y, expanded ? 1F : 0F, color);
    }

    public static void drawDisclosureChevron(GuiGraphics graphics, float x, float y, float expansion, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotation((float) (Math.PI * 0.5) * expansion));
        graphics.pose().scale(0.32F, 0.32F, 1.0F);
        drawChevron(graphics, 0, 0, 1, color);
        graphics.pose().popPose();
    }

    public static void fillCircle(GuiGraphics graphics, float centerX, float centerY, float radius, int color) {
        fillRingSegment(graphics, centerX, centerY, 0.0F, radius, 0.0D, FULL_CIRCLE, 0.0D, FULL_CIRCLE, color);
    }

    public static void fillDisc(GuiGraphics graphics, float centerX, float centerY, float radius, int color) {
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
            GuiGraphics graphics,
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
            GuiGraphics graphics,
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

        drawMesh(graphics, mesh, centerX, centerY, color);
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

    private static void drawMesh(GuiGraphics graphics, CachedMesh mesh, float originX, float originY, int color) {
        Matrix4f pose = new Matrix4f(graphics.pose().last().pose());
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        try {
            var consumer = Tesselator.getInstance().getBuilder();
            consumer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            float[] vertices = mesh.vertices();
            for (int index = 0; index < vertices.length; index += 8) {
                for (int vertex = 0; vertex < 8; vertex += 2) {
                    consumer.vertex(pose, originX + vertices[index + vertex], originY + vertices[index + vertex + 1], 0)
                            .color((color >> 16) & 255, (color >> 8) & 255, color & 255, (color >>> 24)).endVertex();
                }
            }
            BufferUploader.drawWithShader(consumer.end());
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }
}
