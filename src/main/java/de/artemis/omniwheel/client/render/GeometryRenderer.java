package de.artemis.omniwheel.client.render;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.OptionalInt;

import static de.artemis.omniwheel.OmniWheel.MOD_ID;

public final class GeometryRenderer {
    private static final RenderPipeline TRIANGLE_FAN_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(ResourceLocation.fromNamespaceAndPath(MOD_ID, "geometry_triangle_fan"))
            .withCull(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_FAN)
            .build();
    private static final RenderPipeline TRIANGLE_STRIP_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(ResourceLocation.fromNamespaceAndPath(MOD_ID, "geometry_triangle_strip"))
            .withCull(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_STRIP)
            .build();

    private GeometryRenderer() {
    }

    public static void fillCircle(GuiGraphics graphics, float centerX, float centerY, float radius, int color) {
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

        draw(buffer.buildOrThrow(), TRIANGLE_FAN_PIPELINE);
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

        draw(buffer.buildOrThrow(), TRIANGLE_STRIP_PIPELINE);
    }

    private static void addVertex(BufferBuilder buffer, Matrix4f pose, float x, float y, int color) {
        float alpha = ((color >> 24) & 0xFF) / 255.0F;
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        buffer.addVertex(pose, x, y, 0.0F).setColor(red, green, blue, alpha);
    }

    private static void draw(MeshData meshData, RenderPipeline pipeline) {
        var device = RenderSystem.getDevice();
        var renderTarget = Minecraft.getInstance().getMainRenderTarget();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        try (meshData;
             var vertexBuffer = device.createBuffer(() -> "OmniWheel geometry", BufferType.VERTICES, BufferUsage.DYNAMIC_WRITE, meshData.vertexBuffer());
             var renderPass = device.createCommandEncoder()
                     .createRenderPass(renderTarget.getColorTexture(), OptionalInt.empty())) {
            renderPass.setPipeline(pipeline);
            renderPass.setVertexBuffer(0, vertexBuffer);
            renderPass.draw(0, meshData.drawState().vertexCount());
        }
    }
}
