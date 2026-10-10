package de.artemis.omniwheel.client.render;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.opengl.GL11;

public final class GuiGraphics {
    private final PoseStack pose;
    private final Minecraft minecraft;

    public GuiGraphics(PoseStack pose) {
        this.pose = pose;
        this.minecraft = Minecraft.getInstance();
    }

    public PoseStack pose() {
        return pose;
    }

    public void fill(int minX, int minY, int maxX, int maxY, int color) {
        GuiComponent.fill(pose, minX, minY, maxX, maxY, color);
    }

    public void drawString(Font font, String text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            font.drawShadow(pose, text, x, y, color);
        } else {
            font.draw(pose, text, x, y, color);
        }
    }

    public void drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            font.drawShadow(pose, text, x, y, color);
        } else {
            font.draw(pose, text, x, y, color);
        }
    }

    public void drawString(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            font.drawShadow(pose, text, x, y, color);
        } else {
            font.draw(pose, text, x, y, color);
        }
    }

    public void drawCenteredString(Font font, String text, int x, int y, int color) {
        font.drawShadow(pose, text, x - font.width(text) / 2.0F, y, color);
    }

    public void drawCenteredString(Font font, FormattedCharSequence text, int x, int y, int color) {
        font.drawShadow(pose, text, x - font.width(text) / 2.0F, y, color);
    }

    public void blit(ResourceLocation texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        RenderSystem.setShaderTexture(0, texture);
        GuiComponent.blit(pose, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    public void blit(ResourceLocation texture, int x, int y, int z, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        RenderSystem.setShaderTexture(0, texture);
        GuiComponent.blit(pose, x, y, z, u, v, width, height, textureWidth, textureHeight);
    }

    public void renderItem(ItemStack stack, int x, int y) {
        ItemRenderer itemRenderer = minecraft.getItemRenderer();
        itemRenderer.renderAndDecorateItem(stack, x, y);
    }

    public void renderItem(ItemStack stack, int x, int y, float scale) {
        ItemRenderer itemRenderer = minecraft.getItemRenderer();
        ItemRenderTransform transform = itemRenderTransform(x, y, scale);
        if (Math.abs(transform.scale() - 1.0F) < 0.001F) {
            itemRenderer.renderAndDecorateItem(stack, x, y);
            return;
        }

        com.mojang.blaze3d.vertex.PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        modelViewStack.translate(transform.x(), transform.y(), 0.0D);
        modelViewStack.scale(transform.scale(), transform.scale(), 1.0F);
        RenderSystem.applyModelViewMatrix();
        itemRenderer.renderAndDecorateItem(stack, 0, 0);
        modelViewStack.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    public void enableScissor(int x1, int y1, int x2, int y2) {
        Window window = minecraft.getWindow();
        double scale = window.getGuiScale();
        int minX = Math.min(x1, x2);
        int minY = Math.min(y1, y2);
        int maxX = Math.max(x1, x2);
        int maxY = Math.max(y1, y2);
        int scissorX = (int) Math.floor(minX * scale);
        int scissorY = (int) Math.floor(window.getHeight() - (maxY * scale));
        int scissorWidth = Math.max(0, (int) Math.ceil((maxX - minX) * scale));
        int scissorHeight = Math.max(0, (int) Math.ceil((maxY - minY) * scale));
        RenderSystem.enableScissor(scissorX, scissorY, scissorWidth, scissorHeight);
    }

    public void disableScissor() {
        RenderSystem.disableScissor();
    }

    private ItemRenderTransform itemRenderTransform(int x, int y, float iconScale) {
        Vector4f topLeft = new Vector4f(x, y, 0.0F, 1.0F);
        Vector4f bottomRight = new Vector4f(x + (16.0F * iconScale), y + (16.0F * iconScale), 0.0F, 1.0F);
        topLeft.transform(pose.last().pose());
        bottomRight.transform(pose.last().pose());
        float transformedScale = Math.max(
                Math.abs(bottomRight.x() - topLeft.x()) / 16.0F,
                Math.abs(bottomRight.y() - topLeft.y()) / 16.0F
        );
        return new ItemRenderTransform(Math.round(topLeft.x()), Math.round(topLeft.y()), transformedScale);
    }

    private record ItemRenderTransform(int x, int y, float scale) {
    }
}
