package de.artemis.omniwheel.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;

public final class EntryIconRenderer {
    private static final String EFFECT_PREFIX = "effect:";
    private static final String GUI_PREFIX = "gui:";
    private static final String PARTICLE_PREFIX = "particle:";
    private static final String SYMBOL_PREFIX = "symbol:";
    private static final String TEXTURE_PREFIX = "texture:";
    private static final ResourceLocation GUI_ICONS = minecraftTexture("gui/icons.png");
    private static final ResourceLocation ACCESSIBILITY = minecraftTexture("gui/accessibility.png");
    private static final ResourceLocation REPORT_BUTTON = minecraftTexture("gui/report_button.png");
    private static final ResourceLocation RESOURCE_PACKS = minecraftTexture("gui/resource_packs.png");
    private static final ResourceLocation SERVER_SELECTION = minecraftTexture("gui/server_selection.png");
    private static final Map<ResourceLocation, List<ResourceLocation>> PARTICLE_FRAMES = new LinkedHashMap<>();
    private static final Map<String, String> SYMBOLS = new LinkedHashMap<>();
    private static final Map<String, TextureIcon> TEXTURE_ICONS = new LinkedHashMap<>();

    static {
        registerTextureIcon("gui:hud/crosshair", GUI_ICONS, 256, 256, 0, 0, 15, 15);
        registerTextureIcon("gui:hud/armor_empty", GUI_ICONS, 256, 256, 16, 9, 9, 9);
        registerTextureIcon("gui:hud/armor_half", GUI_ICONS, 256, 256, 25, 9, 9, 9);
        registerTextureIcon("gui:hud/armor_full", GUI_ICONS, 256, 256, 34, 9, 9, 9);
        registerTextureIcon("gui:hud/food_empty", GUI_ICONS, 256, 256, 16, 27, 9, 9);
        registerTextureIcon("gui:hud/food_half", GUI_ICONS, 256, 256, 25, 27, 9, 9);
        registerTextureIcon("gui:hud/food_full", GUI_ICONS, 256, 256, 34, 27, 9, 9);
        registerTextureIcon("gui:hud/food_empty_hunger", GUI_ICONS, 256, 256, 88, 27, 9, 9);
        registerTextureIcon("gui:hud/food_half_hunger", GUI_ICONS, 256, 256, 97, 27, 9, 9);
        registerTextureIcon("gui:hud/food_full_hunger", GUI_ICONS, 256, 256, 106, 27, 9, 9);
        registerTextureIcon("gui:hud/air", GUI_ICONS, 256, 256, 16, 18, 9, 9);
        registerTextureIcon("gui:hud/air_bursting", GUI_ICONS, 256, 256, 25, 18, 9, 9);
        registerTextureIcon("gui:hud/heart/container", GUI_ICONS, 256, 256, 16, 0, 9, 9);
        registerTextureIcon("gui:hud/heart/full", GUI_ICONS, 256, 256, 52, 0, 9, 9);
        registerTextureIcon("gui:hud/heart/half", GUI_ICONS, 256, 256, 61, 0, 9, 9);
        registerTextureIcon("gui:hud/heart/hardcore_full", GUI_ICONS, 256, 256, 52, 45, 9, 9);
        registerTextureIcon("gui:hud/heart/hardcore_half", GUI_ICONS, 256, 256, 61, 45, 9, 9);
        registerTextureIcon("gui:hud/heart/poisoned_full", GUI_ICONS, 256, 256, 88, 0, 9, 9);
        registerTextureIcon("gui:hud/heart/poisoned_half", GUI_ICONS, 256, 256, 97, 0, 9, 9);
        registerTextureIcon("gui:hud/heart/withered_full", GUI_ICONS, 256, 256, 124, 0, 9, 9);
        registerTextureIcon("gui:hud/heart/withered_half", GUI_ICONS, 256, 256, 133, 0, 9, 9);
        registerTextureIcon("gui:hud/heart/absorbing_full", GUI_ICONS, 256, 256, 160, 0, 9, 9);
        registerTextureIcon("gui:hud/heart/absorbing_half", GUI_ICONS, 256, 256, 169, 0, 9, 9);
        registerTextureIcon("gui:hud/heart/frozen_full", GUI_ICONS, 256, 256, 52, 36, 9, 9);
        registerTextureIcon("gui:hud/heart/frozen_half", GUI_ICONS, 256, 256, 61, 36, 9, 9);
        registerTextureIcon("gui:hud/heart/vehicle_full", GUI_ICONS, 256, 256, 52, 9, 9, 9);
        registerTextureIcon("gui:hud/heart/vehicle_half", GUI_ICONS, 256, 256, 61, 9, 9, 9);
        registerTextureIcon("gui:icon/checkmark", RESOURCE_PACKS, 256, 256, 32, 0, 16, 16);
        registerTextureIcon("gui:icon/search", SERVER_SELECTION, 256, 256, 0, 0, 10, 10);
        registerTextureIcon("gui:icon/language", ACCESSIBILITY, 256, 256, 0, 0, 20, 20);
        registerTextureIcon("gui:icon/accessibility", ACCESSIBILITY, 256, 256, 0, 0, 20, 20);
        registerTextureIcon("gui:icon/draft_report", REPORT_BUTTON, 64, 64, 0, 0, 20, 20);
        registerTextureIcon("gui:icon/ping_unknown", GUI_ICONS, 256, 256, 0, 176, 10, 8);
        registerTextureIcon("gui:icon/ping_1", GUI_ICONS, 256, 256, 0, 168, 10, 8);
        registerTextureIcon("gui:icon/ping_2", GUI_ICONS, 256, 256, 0, 160, 10, 8);
        registerTextureIcon("gui:icon/ping_3", GUI_ICONS, 256, 256, 0, 152, 10, 8);
        registerTextureIcon("gui:icon/ping_4", GUI_ICONS, 256, 256, 0, 144, 10, 8);
        registerTextureIcon("gui:icon/ping_5", GUI_ICONS, 256, 256, 0, 136, 10, 8);
        SYMBOLS.put("plus", "+");
        SYMBOLS.put("minus", "-");
        SYMBOLS.put("check", "\u2713");
        SYMBOLS.put("warning", "!");
    }

    private EntryIconRenderer() {
    }

    public static boolean drawIcon(GuiGraphics graphics, Minecraft minecraft, String icon, float centerX, float centerY, float scale) {
        ItemStack stack = resolveItemStack(icon);
        if (!stack.isEmpty()) {
            float iconSize = 16.0F * scale;
            int x = Math.round(centerX - (iconSize * 0.5F));
            int y = Math.round(centerY - (iconSize * 0.5F));
            graphics.renderItem(stack, x, y, scale);
            return true;
        }

        if (drawEffectIcon(graphics, minecraft, icon, centerX, centerY, scale)) {
            return true;
        }

        if (drawTextureIcon(graphics, icon, centerX, centerY, scale)) {
            return true;
        }

        if (drawParticleIcon(graphics, minecraft, icon, centerX, centerY, scale)) {
            return true;
        }

        return drawSymbolIcon(graphics, minecraft, icon, centerX, centerY, scale);
    }

    public static boolean isItemIcon(String icon) {
        return !resolveItemStack(icon).isEmpty();
    }

    public static boolean isVisualIcon(String icon) {
        return isItemIcon(icon) || isEffectIcon(icon) || isGuiSpriteIcon(icon) || isTextureIcon(icon) || isParticleIcon(icon) || isSymbolIcon(icon);
    }

    public static boolean isEffectIcon(String icon) {
        return resolveEffect(icon) != null;
    }

    public static boolean isSymbolIcon(String icon) {
        return resolveSymbol(icon) != null;
    }

    public static boolean isGuiSpriteIcon(String icon) {
        return resolveTextureIcon(icon) != null;
    }

    public static boolean isTextureIcon(String icon) {
        return resolveTextureIcon(icon) != null;
    }

    public static boolean isParticleIcon(String icon) {
        return resolveParticleId(icon) != null;
    }

    private static ItemStack resolveItemStack(String icon) {
        String trimmed = icon == null ? "" : icon.trim();
        if (trimmed.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (!trimmed.contains(":")) {
            trimmed = "minecraft:" + trimmed;
        }

        ResourceLocation id = ResourceLocation.tryParse(trimmed);
        if (id == null) {
            return ItemStack.EMPTY;
        }

        Item item = Registry.ITEM.getOptional(id).orElse(null);
        if (item != null && item != Items.AIR) {
            return new ItemStack(item);
        }

        Block block = Registry.BLOCK.getOptional(id).orElse(null);
        if (block == null) {
            return ItemStack.EMPTY;
        }

        Item blockItem = block.asItem();
        return blockItem == Items.AIR ? ItemStack.EMPTY : new ItemStack(blockItem);
    }

    private static boolean drawEffectIcon(GuiGraphics graphics, Minecraft minecraft, String icon, float centerX, float centerY, float scale) {
        ResourceLocation effectId = resolveEffectId(icon);
        if (effectId == null) {
            return false;
        }

        MobEffect effect = Registry.MOB_EFFECT.getOptional(effectId).orElse(null);
        if (effect == null) {
            return false;
        }

        ResourceLocation texture = ResourceLocation.tryParse(effectId.getNamespace() + ":textures/mob_effect/" + effectId.getPath() + ".png");
        if (texture == null || !textureResourceExists(minecraft, texture)) {
            return false;
        }

        return drawTextureIcon(graphics, new TextureIcon(texture, 18, 18, 0, 0, 18, 18), centerX, centerY, scale);
    }

    private static boolean drawTextureIcon(GuiGraphics graphics, String icon, float centerX, float centerY, float scale) {
        TextureIcon textureIcon = resolveTextureIcon(icon);
        if (textureIcon == null) {
            return false;
        }

        float baseSize = Math.max(textureIcon.srcWidth(), textureIcon.srcHeight());
        float renderScale = (18.0F * scale) / Math.max(1.0F, baseSize);
        graphics.pose().pushPose();
        graphics.pose().translate(centerX - ((textureIcon.srcWidth() * renderScale) * 0.5F), centerY - ((textureIcon.srcHeight() * renderScale) * 0.5F), 200.0F);
        graphics.pose().scale(renderScale, renderScale, 1.0F);
        graphics.blit(textureIcon.texture(), 0, 0, 0, (float) textureIcon.u(), (float) textureIcon.v(), textureIcon.srcWidth(), textureIcon.srcHeight(), textureIcon.textureWidth(), textureIcon.textureHeight());
        graphics.pose().popPose();
        return true;
    }

    private static boolean drawParticleIcon(GuiGraphics graphics, Minecraft minecraft, String icon, float centerX, float centerY, float scale) {
        ResourceLocation particleId = resolveParticleId(icon);
        if (particleId == null) {
            return false;
        }

        List<ResourceLocation> frames = resolveParticleFrames(minecraft, particleId);
        if (frames.isEmpty()) {
            return false;
        }

        int frameIndex = Math.floorMod((int) (System.currentTimeMillis() / 90L), frames.size());
        ResourceLocation frameId = frames.get(frameIndex);
        TextureIcon particleTexture = resolveParticleTextureIcon(frameId);
        if (particleTexture == null || !textureResourceExists(minecraft, particleTexture.texture())) {
            return false;
        }

        return drawTextureIcon(graphics, particleTexture, centerX, centerY, scale);
    }

    private static boolean drawSymbolIcon(GuiGraphics graphics, Minecraft minecraft, String icon, float centerX, float centerY, float scale) {
        String symbol = resolveSymbol(icon);
        if (symbol == null) {
            return false;
        }

        float symbolScale = Math.max(0.9F, scale * 1.2F);
        int textWidth = minecraft.font.width(symbol);
        int x = Math.round((centerX / symbolScale) - (textWidth / 2.0F));
        int y = Math.round((centerY / symbolScale) - 4.0F);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 200.0F);
        graphics.pose().scale(symbolScale, symbolScale, 1.0F);
        graphics.drawString(minecraft.font, symbol, x, y, 0xFFF2F5F8, false);
        graphics.pose().popPose();
        return true;
    }

    private static MobEffect resolveEffect(String icon) {
        ResourceLocation effectId = resolveEffectId(icon);
        if (effectId == null) {
            return null;
        }
        return Registry.MOB_EFFECT.getOptional(effectId).orElse(null);
    }

    private static ResourceLocation resolveEffectId(String icon) {
        String trimmed = icon == null ? "" : icon.trim();
        if (!trimmed.regionMatches(true, 0, EFFECT_PREFIX, 0, EFFECT_PREFIX.length())) {
            return null;
        }

        String effectPart = trimmed.substring(EFFECT_PREFIX.length()).trim().toLowerCase(Locale.ROOT);
        if (effectPart.isEmpty()) {
            return null;
        }
        if (!effectPart.contains(":")) {
            effectPart = "minecraft:" + effectPart;
        }
        ResourceLocation effectId = ResourceLocation.tryParse(effectPart);
        if (effectId == null) {
            return null;
        }
        return Registry.MOB_EFFECT.containsKey(effectId) ? effectId : null;
    }

    private static TextureIcon resolveTextureIcon(String icon) {
        String trimmed = icon == null ? "" : icon.trim();
        if (trimmed.regionMatches(true, 0, GUI_PREFIX, 0, GUI_PREFIX.length())) {
            return TEXTURE_ICONS.get(trimmed.toLowerCase(Locale.ROOT));
        }
        if (!trimmed.regionMatches(true, 0, TEXTURE_PREFIX, 0, TEXTURE_PREFIX.length())) {
            return null;
        }
        String key = trimmed.substring(TEXTURE_PREFIX.length()).trim().toLowerCase(Locale.ROOT);
        return TEXTURE_ICONS.get(key);
    }

    private static ResourceLocation resolveParticleId(String icon) {
        String trimmed = icon == null ? "" : icon.trim();
        if (!trimmed.regionMatches(true, 0, PARTICLE_PREFIX, 0, PARTICLE_PREFIX.length())) {
            return null;
        }

        String particlePart = trimmed.substring(PARTICLE_PREFIX.length()).trim().toLowerCase(Locale.ROOT);
        if (particlePart.isEmpty()) {
            return null;
        }
        if (!particlePart.contains(":")) {
            particlePart = "minecraft:" + particlePart;
        }
        ResourceLocation particleId = ResourceLocation.tryParse(particlePart);
        if (particleId == null) {
            return null;
        }
        return Registry.PARTICLE_TYPE.containsKey(particleId) ? particleId : null;
    }

    private static List<ResourceLocation> resolveParticleFrames(Minecraft minecraft, ResourceLocation particleId) {
        List<ResourceLocation> cached = PARTICLE_FRAMES.get(particleId);
        if (cached != null) {
            return cached;
        }

        List<ResourceLocation> frames = loadParticleFrames(minecraft, particleId);
        PARTICLE_FRAMES.put(particleId, frames);
        return frames;
    }

    private static List<ResourceLocation> loadParticleFrames(Minecraft minecraft, ResourceLocation particleId) {
        ResourceLocation definitionId = ResourceLocation.tryParse(particleId.getNamespace() + ":particles/" + particleId.getPath() + ".json");
        if (definitionId == null) {
            return List.of();
        }

        Resource resource;
        try {
            resource = minecraft.getResourceManager().getResource(definitionId);
        } catch (IOException ignored) {
            return List.of();
        }

        try (Reader reader = new InputStreamReader(resource.getInputStream())) {
            JsonObject data = GsonHelper.parse(reader);
            JsonArray textures = GsonHelper.getAsJsonArray(data, "textures", null);
            if (textures == null || textures.isEmpty()) {
                return List.of();
            }

            List<ResourceLocation> frames = new ArrayList<>();
            for (int index = 0; index < textures.size(); index++) {
                String textureId = GsonHelper.convertToString(textures.get(index), "texture");
                ResourceLocation frameId = ResourceLocation.tryParse(textureId);
                if (frameId != null) {
                    frames.add(frameId);
                }
            }
            return frames.isEmpty() ? List.of() : List.copyOf(frames);
        } catch (IOException ignored) {
            return List.of();
        }
    }

    private static String resolveSymbol(String icon) {
        String trimmed = icon == null ? "" : icon.trim();
        if (!trimmed.regionMatches(true, 0, SYMBOL_PREFIX, 0, SYMBOL_PREFIX.length())) {
            return null;
        }
        String key = trimmed.substring(SYMBOL_PREFIX.length()).trim().toLowerCase(Locale.ROOT);
        return SYMBOLS.get(key);
    }

    private static void registerTextureIcon(String key, String textureId, int textureWidth, int textureHeight, int u, int v, int srcWidth, int srcHeight) {
        ResourceLocation id = ResourceLocation.tryParse(textureId);
        if (id != null) {
            TEXTURE_ICONS.put(key.toLowerCase(Locale.ROOT), new TextureIcon(id, textureWidth, textureHeight, u, v, srcWidth, srcHeight));
        }
    }

    private static void registerTextureIcon(String key, ResourceLocation texture, int textureWidth, int textureHeight, int u, int v, int srcWidth, int srcHeight) {
        TEXTURE_ICONS.put(key.toLowerCase(Locale.ROOT), new TextureIcon(texture, textureWidth, textureHeight, u, v, srcWidth, srcHeight));
    }

    private static boolean drawTextureIcon(GuiGraphics graphics, TextureIcon textureIcon, float centerX, float centerY, float scale) {
        float baseSize = Math.max(textureIcon.srcWidth(), textureIcon.srcHeight());
        float renderScale = (18.0F * scale) / Math.max(1.0F, baseSize);
        graphics.pose().pushPose();
        graphics.pose().translate(centerX - ((textureIcon.srcWidth() * renderScale) * 0.5F), centerY - ((textureIcon.srcHeight() * renderScale) * 0.5F), 200.0F);
        graphics.pose().scale(renderScale, renderScale, 1.0F);
        graphics.blit(textureIcon.texture(), 0, 0, 0, (float) textureIcon.u(), (float) textureIcon.v(), textureIcon.srcWidth(), textureIcon.srcHeight(), textureIcon.textureWidth(), textureIcon.textureHeight());
        graphics.pose().popPose();
        return true;
    }

    private static TextureIcon resolveParticleTextureIcon(ResourceLocation frameId) {
        String path = frameId.getPath();
        if (!path.startsWith("particle/")) {
            path = "particle/" + path;
        }
        ResourceLocation texture = ResourceLocation.tryParse(frameId.getNamespace() + ":textures/" + path + ".png");
        return texture == null ? null : new TextureIcon(texture, 16, 16, 0, 0, 16, 16);
    }

    private static boolean textureResourceExists(Minecraft minecraft, ResourceLocation texture) {
        try {
            minecraft.getResourceManager().getResource(texture);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static ResourceLocation minecraftTexture(String path) {
        ResourceLocation texture = ResourceLocation.tryParse("minecraft:textures/" + path);
        if (texture == null) {
            throw new IllegalArgumentException("Invalid Minecraft texture path: " + path);
        }
        return texture;
    }

    private record TextureIcon(ResourceLocation texture, int textureWidth, int textureHeight, int u, int v, int srcWidth, int srcHeight) {
    }
}
