package de.artemis.omniwheel.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.Deque;

/** The wheel's drawing operations backed by Minecraft 1.18.2's pose-based GUI. */
public final class GuiGraphics {
    private final Minecraft minecraft;
    private final PoseStack pose;
    private final Deque<Clip> clips = new ArrayDeque<>();
    private record Clip(int left, int top, int right, int bottom) {}

    public GuiGraphics(Minecraft minecraft, PoseStack pose) {
        this.minecraft = minecraft;
        this.pose = pose;
    }

    public PoseStack pose() { return pose; }
    public void flush() { minecraft.renderBuffers().bufferSource().endBatch(); }

    public void fill(int left, int top, int right, int bottom, int color) {
        GuiComponent.fill(pose, left, top, right, bottom, color);
    }

    public void fillGradient(int left, int top, int right, int bottom, int topColor, int bottomColor) {
        Gradient.draw(pose, left, top, right, bottom, topColor, bottomColor);
    }

    private static final class Gradient extends GuiComponent {
        private static void draw(PoseStack pose, int left, int top, int right, int bottom, int a, int b) {
            fillGradient(pose, left, top, right, bottom, a, b, 0);
        }
    }

    public int drawString(Font font, String text, int x, int y, int color, boolean shadow) {
        return shadow ? font.drawShadow(pose, text, x, y, color) : font.draw(pose, text, x, y, color);
    }

    public int drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
        return shadow ? font.drawShadow(pose, text, x, y, color) : font.draw(pose, text, x, y, color);
    }

    public int drawString(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        return shadow ? font.drawShadow(pose, text, x, y, color) : font.draw(pose, text, x, y, color);
    }

    public int drawString(Font font, String text, int x, int y, int color) { return drawString(font, text, x, y, color, true); }
    public int drawString(Font font, Component text, int x, int y, int color) { return drawString(font, text, x, y, color, true); }
    public int drawString(Font font, FormattedCharSequence text, int x, int y, int color) { return drawString(font, text, x, y, color, true); }
    public void drawCenteredString(Font font, String text, int x, int y, int color) { drawString(font, text, x - font.width(text) / 2, y, color); }
    public void drawCenteredString(Font font, Component text, int x, int y, int color) { drawString(font, text, x - font.width(text) / 2, y, color); }
    public void drawCenteredString(Font font, FormattedCharSequence text, int x, int y, int color) { drawString(font, text, x - font.width(text) / 2, y, color); }

    public void blit(ResourceLocation texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        RenderSystem.setShaderTexture(0, texture);
        GuiComponent.blit(pose, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    public void blit(int x, int y, int z, int width, int height, TextureAtlasSprite sprite) {
        RenderSystem.setShaderTexture(0, sprite.atlas().location());
        GuiComponent.blit(pose, x, y, z, width, height, sprite);
    }

    public void renderItem(ItemStack stack, int x, int y) {
        flush();
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.mulPoseMatrix(pose.last().pose());
        RenderSystem.applyModelViewMatrix();
        try {
            minecraft.getItemRenderer().renderAndDecorateItem(stack, x, y);
        } finally {
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
        }
    }

    public void enableScissor(int left, int top, int right, int bottom) {
        if (!clips.isEmpty()) {
            Clip parent = clips.peek();
            left = Math.max(left, parent.left()); top = Math.max(top, parent.top());
            right = Math.min(right, parent.right()); bottom = Math.min(bottom, parent.bottom());
        }
        clips.push(new Clip(left, top, Math.max(left, right), Math.max(top, bottom)));
        applyClip();
    }

    public void disableScissor() { clips.pop(); applyClip(); }

    private void applyClip() {
        flush();
        if (clips.isEmpty()) { RenderSystem.disableScissor(); return; }
        Clip clip = clips.peek();
        double scale = minecraft.getWindow().getGuiScale();
        RenderSystem.enableScissor((int) (clip.left() * scale), (int) (minecraft.getWindow().getHeight() - clip.bottom() * scale),
                (int) ((clip.right() - clip.left()) * scale), (int) ((clip.bottom() - clip.top()) * scale));
    }

    public void renderTooltip(Font font, ItemStack stack, int x, int y) {
        Screen screen = minecraft.screen;
        if (screen != null) screen.renderTooltip(pose, screen.getTooltipFromItem(stack), stack.getTooltipImage(), x, y);
    }

    public void renderTooltip(Font font, Component text, int x, int y) {
        if (minecraft.screen != null) minecraft.screen.renderTooltip(pose, text, x, y);
    }
}
