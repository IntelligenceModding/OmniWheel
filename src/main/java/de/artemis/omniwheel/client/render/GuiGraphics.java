package de.artemis.omniwheel.client.render;

import net.minecraft.client.MainWindow;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.item.ItemStack;

public final class GuiGraphics {
    private final MatrixStack pose;
    private final Minecraft minecraft;

    public GuiGraphics(MatrixStack pose) {
        this.pose = pose;
        this.minecraft = Minecraft.getInstance();
    }

    public MatrixStack pose() {
        return pose;
    }

    public void fill(int minX, int minY, int maxX, int maxY, int color) {
        AbstractGui.fill(pose, minX, minY, maxX, maxY, color);
    }

    public void drawString(FontRenderer font, String text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            font.drawShadow(pose, text, x, y, color);
        } else {
            font.draw(pose, text, x, y, color);
        }
    }

    public void drawString(FontRenderer font, ITextComponent text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            font.drawShadow(pose, text, x, y, color);
        } else {
            font.draw(pose, text, x, y, color);
        }
    }

    public void drawString(FontRenderer font, IReorderingProcessor text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            font.drawShadow(pose, text, x, y, color);
        } else {
            font.draw(pose, text, x, y, color);
        }
    }

    public void drawCenteredString(FontRenderer font, String text, int x, int y, int color) {
        font.drawShadow(pose, text, x - font.width(text) / 2.0F, y, color);
    }

    public void drawCenteredString(FontRenderer font, IReorderingProcessor text, int x, int y, int color) {
        font.drawShadow(pose, text, x - font.width(text) / 2.0F, y, color);
    }

    public void blit(ResourceLocation texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        minecraft.getTextureManager().bind(texture);
        AbstractGui.blit(pose, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    public void blit(ResourceLocation texture, int x, int y, int z, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        minecraft.getTextureManager().bind(texture);
        AbstractGui.blit(pose, x, y, z, u, v, width, height, textureWidth, textureHeight);
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

        RenderSystem.pushMatrix();
        RenderSystem.translated(transform.x(), transform.y(), 0.0D);
        RenderSystem.scaled(transform.scale(), transform.scale(), 1.0D);
        itemRenderer.renderAndDecorateItem(stack, 0, 0);
        RenderSystem.popMatrix();
    }

    public void enableScissor(int x1, int y1, int x2, int y2) {
        MainWindow window = minecraft.getWindow();
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

    private static final class ItemRenderTransform {
        private final int x;
        private final int y;
        private final float scale;

        private ItemRenderTransform(int x, int y, float scale) {

            this.x = x;
            this.y = y;
            this.scale = scale;
        }

        public int x() {
            return x;
        }

        public int y() {
            return y;
        }

        public float scale() {
            return scale;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof ItemRenderTransform)) return false;
            ItemRenderTransform that = (ItemRenderTransform) other;
            return java.util.Objects.equals(x, that.x) && java.util.Objects.equals(y, that.y) && java.util.Objects.equals(scale, that.scale);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(x, y, scale);
        }

        @Override
        public String toString() {
            return "ItemRenderTransform[" + "x=" + x + ", " + "y=" + y + ", " + "scale=" + scale + "]";
        }
    }
}
