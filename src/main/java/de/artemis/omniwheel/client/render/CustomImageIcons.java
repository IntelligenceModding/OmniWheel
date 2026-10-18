package de.artemis.omniwheel.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class CustomImageIcons {
    private static final Map<String, Optional<ImageTexture>> TEXTURES = new LinkedHashMap<>();
    private record ImageTexture(ResourceLocation id, DynamicTexture texture, int width, int height) {}

    private CustomImageIcons() {}

    private static ImageTexture resolve(String icon) {
        if (!CustomImageData.isImage(icon)) return null;
        return TEXTURES.computeIfAbsent(icon, key -> {
            try {
                CustomImageData image = CustomImageData.decode(key);
                String hash = HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8)));
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath("omniwheel", "custom_images/" + hash);
                NativeImage pixels = NativeImage.read(image.png());
                DynamicTexture texture = new DynamicTexture(() -> "OmniWheel icon: " + image.name(), pixels);
                texture.setClamp(true);
                texture.setFilter(true, false);
                Minecraft.getInstance().getTextureManager().register(id, texture);
                return Optional.of(new ImageTexture(id, texture, image.width(), image.height()));
            } catch (Exception | LinkageError ignored) {
                return Optional.empty();
            }
        }).orElse(null);
    }

    public static boolean isUsable(String icon) { return resolve(icon) != null; }

    public static boolean draw(GuiGraphics graphics, String icon, float centerX, float centerY, float scale) {
        ImageTexture image = resolve(icon);
        if (image == null) return false;
        float pixelScale = 16.0F * scale / Math.max(image.width(), image.height());
        float width = image.width() * pixelScale, height = image.height() * pixelScale;
        graphics.nextStratum();
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX - width * 0.5F, centerY - height * 0.5F);
        graphics.pose().scale(pixelScale, pixelScale);
        graphics.blit(image.id(),
                0, 0, image.width(), image.height(), 0, 1, 0, 1);
        graphics.pose().popMatrix();
        return true;
    }

    public static void clearCache() {
        for (var image : TEXTURES.values()) image.ifPresent(texture -> Minecraft.getInstance().getTextureManager().release(texture.id()));
        TEXTURES.clear();
    }
}
