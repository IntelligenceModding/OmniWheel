package de.artemis.omniwheel.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.CoreShaders;
import org.joml.Matrix4f;

public final class GeometryRenderer {
    private GeometryRenderer() {
    }

    public static void fillCircle(GuiGraphics graphics, float centerX, float centerY, float radius, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(CoreShaders.POSITION_COLOR);

        Matrix4f pose = graphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        addVertex(buffer, pose, centerX, centerY, color);

        int steps = Math.max(24, (int) (radius * 0.65F));
        for (int i = 0; i <= steps; i++) {
            double angle = (Math.PI * 2.0D * i) / steps;
            float x = centerX + (float) Math.cos(angle) * radius;
            float y = centerY + (float) Math.sin(angle) * radius;
            addVertex(buffer, pose, x, y, color);
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
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
                    Math.PI * 2.0D,
                    0.0D,
                    Math.PI * 2.0D,
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
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(CoreShaders.POSITION_COLOR);

        Matrix4f pose = graphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        double span = Math.max(Math.abs(innerEndAngle - innerStartAngle), Math.abs(outerEndAngle - outerStartAngle));
        int steps = Math.max(8, (int) Math.ceil(span * 18.0D));

        for (int i = 0; i <= steps; i++) {
            double progress = (double) i / (double) steps;
            double outerAngle = outerStartAngle + ((outerEndAngle - outerStartAngle) * progress);
            double innerAngle = innerStartAngle + ((innerEndAngle - innerStartAngle) * progress);
            float outerCos = (float) Math.cos(outerAngle);
            float outerSin = (float) Math.sin(outerAngle);
            float innerCos = (float) Math.cos(innerAngle);
            float innerSin = (float) Math.sin(innerAngle);
            addVertex(buffer, pose, centerX + (outerCos * outerRadius), centerY + (outerSin * outerRadius), color);
            addVertex(buffer, pose, centerX + (innerCos * innerRadius), centerY + (innerSin * innerRadius), color);
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }

    private static void addVertex(BufferBuilder buffer, Matrix4f pose, float x, float y, int color) {
        float alpha = ((color >> 24) & 0xFF) / 255.0F;
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        buffer.addVertex(pose, x, y, 0.0F).setColor(red, green, blue, alpha);
    }
}
