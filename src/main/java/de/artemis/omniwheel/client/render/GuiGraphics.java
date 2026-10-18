package de.artemis.omniwheel.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.item.ItemStack;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The wheel's drawing operations backed by Minecraft 1.16.5's matrix-based GUI.
 */
public final class GuiGraphics {

    private final Minecraft minecraft;

    private final MatrixStack pose;

    private final Deque<Clip> clips = new ArrayDeque<>();

    private final static class Clip {

        private final int left;

        private final int top;

        private final int right;

        private final int bottom;

        private Clip(int left, int top, int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        public int left() {
            return left;
        }

        public int top() {
            return top;
        }

        public int right() {
            return right;
        }

        public int bottom() {
            return bottom;
        }

        @Override
        public boolean equals(Object value) {
            if (this == value)
                return true;
            if (value == null || value.getClass() != getClass())
                return false;
            Clip other = (Clip) value;
            return true && java.util.Objects.equals(left, other.left) && java.util.Objects.equals(top, other.top) && java.util.Objects.equals(right, other.right) && java.util.Objects.equals(bottom, other.bottom);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(left, top, right, bottom);
        }

        @Override
        public String toString() {
            return "Clip[" + "left=" + left + ", top=" + top + ", right=" + right + ", bottom=" + bottom + "]";
        }
    }

    public GuiGraphics(Minecraft minecraft, MatrixStack pose) {
        this.minecraft = minecraft;
        this.pose = pose;
    }

    public MatrixStack pose() {
        return pose;
    }

    public void flush() {
        minecraft.renderBuffers().bufferSource().endBatch();
    }

    public void fill(int left, int top, int right, int bottom, int color) {
        AbstractGui.fill(pose, left, top, right, bottom, color);
    }

    public void fillGradient(int left, int top, int right, int bottom, int topColor, int bottomColor) {
        Gradient.draw(pose, left, top, right, bottom, topColor, bottomColor);
    }

    private static final class Gradient extends AbstractGui {

        private static final Gradient INSTANCE = new Gradient();

        private static void draw(MatrixStack pose, int left, int top, int right, int bottom, int a, int b) {
            INSTANCE.fillGradient(pose, left, top, right, bottom, a, b);
        }
    }

    public int drawString(FontRenderer font, String text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            return font.drawShadow(pose, text, x, y, color);
        } else {
            return font.draw(pose, text, x, y, color);
        }
    }

    public int drawString(FontRenderer font, ITextComponent text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            return font.drawShadow(pose, text, x, y, color);
        } else {
            return font.draw(pose, text, x, y, color);
        }
    }

    public int drawString(FontRenderer font, IReorderingProcessor text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            return font.drawShadow(pose, text, x, y, color);
        } else {
            return font.draw(pose, text, x, y, color);
        }
    }

    public int drawString(FontRenderer font, String text, int x, int y, int color) {
        return drawString(font, text, x, y, color, true);
    }

    public int drawString(FontRenderer font, ITextComponent text, int x, int y, int color) {
        return drawString(font, text, x, y, color, true);
    }

    public int drawString(FontRenderer font, IReorderingProcessor text, int x, int y, int color) {
        return drawString(font, text, x, y, color, true);
    }

    public void drawCenteredString(FontRenderer font, String text, int x, int y, int color) {
        drawString(font, text, x - font.width(text) / 2, y, color);
    }

    public void drawCenteredString(FontRenderer font, ITextComponent text, int x, int y, int color) {
        drawString(font, text, x - font.width(text) / 2, y, color);
    }

    public void drawCenteredString(FontRenderer font, IReorderingProcessor text, int x, int y, int color) {
        drawString(font, text, x - font.width(text) / 2, y, color);
    }

    public void blit(ResourceLocation texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        minecraft.getTextureManager().bind(texture);
        AbstractGui.blit(pose, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    public void blit(int x, int y, int z, int width, int height, TextureAtlasSprite sprite) {
        minecraft.getTextureManager().bind(sprite.atlas().location());
        AbstractGui.blit(pose, x, y, z, width, height, sprite);
    }

    public void renderItem(ItemStack stack, int x, int y) {
        flush();
        RenderSystem.pushMatrix();
        RenderSystem.multMatrix(pose.last().pose());
        try {
            minecraft.getItemRenderer().renderAndDecorateItem(stack, x, y);
        } finally {
            RenderSystem.popMatrix();
        }
    }

    public void enableScissor(int left, int top, int right, int bottom) {
        if (!clips.isEmpty()) {
            Clip parent = clips.peek();
            left = Math.max(left, parent.left());
            top = Math.max(top, parent.top());
            right = Math.min(right, parent.right());
            bottom = Math.min(bottom, parent.bottom());
        }
        clips.push(new Clip(left, top, Math.max(left, right), Math.max(top, bottom)));
        applyClip();
    }

    public void disableScissor() {
        clips.pop();
        applyClip();
    }

    private void applyClip() {
        flush();
        if (clips.isEmpty()) {
            RenderSystem.disableScissor();
            return;
        }
        Clip clip = clips.peek();
        double scale = minecraft.getWindow().getGuiScale();
        RenderSystem.enableScissor((int) (clip.left() * scale), (int) (minecraft.getWindow().getHeight() - clip.bottom() * scale), (int) ((clip.right() - clip.left()) * scale), (int) ((clip.bottom() - clip.top()) * scale));
    }

    public void renderTooltip(FontRenderer font, ItemStack stack, int x, int y) {
        Screen screen = minecraft.screen;
        if (screen != null)
            screen.renderComponentTooltip(pose, screen.getTooltipFromItem(stack), x, y);
    }

    public void renderTooltip(FontRenderer font, ITextComponent text, int x, int y) {
        if (minecraft.screen != null)
            minecraft.screen.renderTooltip(pose, text, x, y);
    }
}
