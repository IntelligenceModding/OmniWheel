package de.artemis.omniwheel.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class CustomImageIcons {
    private static final Map<String, Optional<ImageTexture>> TEXTURES = new LinkedHashMap<>();
    private record ImageTexture(Identifier id, DynamicTexture texture, int width, int height) {}

    private CustomImageIcons() {}

    private static ImageTexture resolve(String icon) {
        if (!CustomImageData.isImage(icon)) return null;
        return TEXTURES.computeIfAbsent(icon, key -> {
            try {
                CustomImageData image = CustomImageData.decode(key);
                String hash = HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8)));
                Identifier id = Identifier.fromNamespaceAndPath("omniwheel", "custom_images/" + hash);
                NativeImage pixels = NativeImage.read(image.png());
                DynamicTexture texture = new DynamicTexture(() -> "OmniWheel icon: " + image.name(), pixels);
                Minecraft.getInstance().getTextureManager().register(id, texture);
                return Optional.of(new ImageTexture(id, texture, image.width(), image.height()));
            } catch (Exception | LinkageError ignored) {
                return Optional.empty();
            }
        }).orElse(null);
    }

    public static boolean isUsable(String icon) { return resolve(icon) != null; }

    public static boolean draw(GuiGraphicsExtractor graphics, String icon, float centerX, float centerY, float scale) {
        ImageTexture image = resolve(icon);
        if (image == null) return false;
        float pixelScale = 16.0F * scale / Math.max(image.width(), image.height());
        float width = image.width() * pixelScale, height = image.height() * pixelScale;
        graphics.nextStratum();
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX - width * 0.5F, centerY - height * 0.5F);
        graphics.pose().scale(pixelScale, pixelScale);
        graphics.blit(image.texture().getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR),
                0, 0, image.width(), image.height(), 0, 1, 0, 1);
        graphics.pose().popMatrix();
        return true;
    }

    public static void clearCache() {
        for (var image : TEXTURES.values()) image.ifPresent(texture -> Minecraft.getInstance().getTextureManager().release(texture.id()));
        TEXTURES.clear();
    }
}
