package de.artemis.omniwheel.client.render;

import net.minecraft.client.Minecraft;
import de.artemis.omniwheel.client.render.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
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
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EntryIconRenderer {
    private static final String EFFECT_PREFIX = "effect:";
    private static final String GUI_PREFIX = "gui:";
    private static final String PARTICLE_PREFIX = "particle:";
    private static final String SYMBOL_PREFIX = "symbol:";
    private static final String TEXTURE_PREFIX = "texture:";
    private static final Map<String, Boolean> VALID_ICONS = new LinkedHashMap<>();
    private static long resourceGeneration;
    private static final Map<String, ResourceLocation> GUI_SPRITES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, List<ResourceLocation>> PARTICLE_FRAMES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, List<TextureAtlasSprite>> PARTICLE_SPRITES = new LinkedHashMap<>();
    private static final Map<String, String> SYMBOLS = new LinkedHashMap<>();
    private static final Map<String, TextureIcon> TEXTURE_ICONS = new LinkedHashMap<>();

    static {
        registerGuiSprite("hud/crosshair");
        registerGuiSprite("hud/armor_full");
        registerGuiSprite("hud/armor_half");
        registerGuiSprite("hud/armor_empty");
        registerGuiSprite("hud/food_full");
        registerGuiSprite("hud/food_half");
        registerGuiSprite("hud/food_empty");
        registerGuiSprite("hud/food_full_hunger");
        registerGuiSprite("hud/food_half_hunger");
        registerGuiSprite("hud/food_empty_hunger");
        registerGuiSprite("hud/air");
        registerGuiSprite("hud/air_bursting");
        registerGuiSprite("hud/heart/full");
        registerGuiSprite("hud/heart/half");
        registerGuiSprite("hud/heart/container");
        registerGuiSprite("hud/heart/hardcore_full");
        registerGuiSprite("hud/heart/hardcore_half");
        registerGuiSprite("hud/heart/poisoned_full");
        registerGuiSprite("hud/heart/poisoned_half");
        registerGuiSprite("hud/heart/withered_full");
        registerGuiSprite("hud/heart/withered_half");
        registerGuiSprite("hud/heart/absorbing_full");
        registerGuiSprite("hud/heart/absorbing_half");
        registerGuiSprite("hud/heart/frozen_full");
        registerGuiSprite("hud/heart/frozen_half");
        registerGuiSprite("hud/heart/vehicle_full");
        registerGuiSprite("hud/heart/vehicle_half");
        registerGuiSprite("icon/checkmark");
        registerGuiSprite("icon/search");
        registerGuiSprite("icon/language");
        registerGuiSprite("icon/accessibility");
        registerGuiSprite("icon/draft_report");
        registerGuiSprite("icon/ping_unknown");
        registerGuiSprite("icon/ping_1");
        registerGuiSprite("icon/ping_2");
        registerGuiSprite("icon/ping_3");
        registerGuiSprite("icon/ping_4");
        registerGuiSprite("icon/ping_5");
        SYMBOLS.put("plus", "+");
        SYMBOLS.put("minus", "-");
        SYMBOLS.put("check", "\u2713");
        SYMBOLS.put("warning", "!");
    }

    private EntryIconRenderer() {
    }

    public static long resourceGeneration() {
        return resourceGeneration;
    }

    public static void clearResourceCaches() {
        VALID_ICONS.clear();
        CustomItemIcons.clearCache();
        CustomImageIcons.clearCache();
        PARTICLE_FRAMES.clear();
        PARTICLE_SPRITES.clear();
        IconAssetValidator.clear();
        resourceGeneration++;
    }

    public static boolean isUsableIcon(Minecraft minecraft, String icon) {
        return VALID_ICONS.computeIfAbsent(icon, key -> {
            try {
                if (CustomImageData.isImage(key)) return CustomImageIcons.isUsable(key);
                ItemStack stack = resolveItemStack(key);
                if (!stack.isEmpty()) {
                    return IconAssetValidator.item(minecraft, stack);
                }
                ResourceLocation effectId = resolveEffectId(key);
                if (effectId != null) {
                    var holder = Registry.MOB_EFFECT.getOptional(effectId).orElse(null);
                    return holder != null && IconAssetValidator.sprite(minecraft.getMobEffectTextures().get(holder));
                }
                ResourceLocation guiSprite = resolveGuiSprite(key);
                if (guiSprite != null) {
                    return IconAssetValidator.guiSprite(minecraft, guiSprite);
                }
                TextureIcon texture = resolveTextureIcon(key);
                if (texture != null) {
                    return IconAssetValidator.texture(minecraft, texture.texture(), texture.textureWidth(), texture.textureHeight(),
                            texture.u(), texture.v(), texture.srcWidth(), texture.srcHeight());
                }
                ResourceLocation particle = resolveParticleId(key);
                if (particle != null) {
                    return !resolveParticleSprites(minecraft, particle).isEmpty();
                }
                return resolveSymbol(key) != null;
            } catch (RuntimeException | LinkageError ignored) {
                // A broken mod asset must not break the entire browser.
                return false;
            }
        });
    }

    public static boolean drawIcon(GuiGraphics graphics, Minecraft minecraft, String icon, float centerX, float centerY, float scale) {
        if (!isUsableIcon(minecraft, icon)) {
            return false;
        }
        if (CustomImageData.isImage(icon)) return CustomImageIcons.draw(graphics, icon, centerX, centerY, scale);
        ItemStack stack = resolveItemStack(icon);
        if (!stack.isEmpty()) {
            float iconSize = 16.0F * scale;
            graphics.flush();
            graphics.pose().pushPose();
            graphics.pose().translate(centerX - (iconSize * 0.5F), centerY - (iconSize * 0.5F), 0.0F);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.renderItem(stack, 0, 0);
            graphics.pose().popPose();
            return true;
        }

        if (drawEffectIcon(graphics, minecraft, icon, centerX, centerY, scale)) {
            return true;
        }

        if (drawGuiSpriteIcon(graphics, icon, centerX, centerY, scale)) {
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
        return CustomImageData.isImage(icon) || isItemIcon(icon) || isEffectIcon(icon) || isGuiSpriteIcon(icon) || isTextureIcon(icon) || isParticleIcon(icon) || isSymbolIcon(icon);
    }

    public static boolean isEffectIcon(String icon) {
        return resolveEffect(icon) != null;
    }

    public static boolean isSymbolIcon(String icon) {
        return resolveSymbol(icon) != null;
    }

    public static boolean isGuiSpriteIcon(String icon) {
        return resolveGuiSprite(icon) != null;
    }

    public static boolean isTextureIcon(String icon) {
        return resolveTextureIcon(icon) != null;
    }

    public static boolean isParticleIcon(String icon) {
        return resolveParticleId(icon) != null;
    }

    public static String particleVisualSignature(Minecraft minecraft, String icon) {
        ResourceLocation particleId = resolveParticleId(icon);
        if (particleId == null) {
            return "";
        }
        return particleVisualSignature(minecraft, particleId);
    }

    private static ItemStack resolveItemStack(String icon) {
        // Embedded PNGs can contain megabytes of data. They are not registry IDs;
        // the legacy parser otherwise builds an equally large exception message.
        if (CustomImageData.isImage(icon)) return ItemStack.EMPTY;
        if (CustomItemIcons.isCustom(icon)) return CustomItemIcons.decode(icon);
        String trimmed = icon == null ? "" : icon.trim();
        if (trimmed.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (!trimmed.contains(":")) {
            trimmed = "minecraft:" + trimmed;
        }

        if (!ResourceLocation.isValidResourceLocation(trimmed)) return ItemStack.EMPTY;
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

        MobEffect effectHolder = Registry.MOB_EFFECT.getOptional(effectId).orElse(null);
        if (effectHolder == null) {
            return false;
        }

        int size = Math.max(12, Math.round(18.0F * scale));
        int x = Math.round(centerX - (size * 0.5F));
        int y = Math.round(centerY - (size * 0.5F));
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        graphics.blit(x, y, 0, size, size, minecraft.getMobEffectTextures().get(effectHolder));
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        return true;
    }

    private static boolean drawGuiSpriteIcon(GuiGraphics graphics, String icon, float centerX, float centerY, float scale) {
        ResourceLocation spriteId = resolveGuiSprite(icon);
        if (spriteId == null) {
            return false;
        }

        int size = Math.max(12, Math.round(18.0F * scale));
        int x = Math.round(centerX - (size * 0.5F));
        int y = Math.round(centerY - (size * 0.5F));
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        LegacyGuiIcons.draw(graphics, spriteId, x, y, size, size);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        return true;
    }

    private static boolean drawTextureIcon(GuiGraphics graphics, String icon, float centerX, float centerY, float scale) {
        TextureIcon textureIcon = resolveTextureIcon(icon);
        if (textureIcon == null) {
            return false;
        }

        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(centerX - (8.0F * scale), centerY - (8.0F * scale), 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        graphics.blit(
                textureIcon.texture(),
                0,
                0,
                (float) textureIcon.u(),
                (float) textureIcon.v(),
                textureIcon.srcWidth(),
                textureIcon.srcHeight(),
                textureIcon.textureWidth(),
                textureIcon.textureHeight()
        );
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        graphics.pose().popPose();
        return true;
    }

    private static boolean drawParticleIcon(GuiGraphics graphics, Minecraft minecraft, String icon, float centerX, float centerY, float scale) {
        ResourceLocation particleId = resolveParticleId(icon);
        if (particleId == null) {
            return false;
        }

        List<TextureAtlasSprite> sprites = resolveParticleSprites(minecraft, particleId);
        if (sprites.isEmpty()) {
            return false;
        }

        int frameIndex = Math.floorMod((int) (System.currentTimeMillis() / 90L), sprites.size());
        TextureAtlasSprite sprite = sprites.get(frameIndex);

        int size = Math.max(12, Math.round(18.0F * scale));
        int x = Math.round(centerX - (size * 0.5F));
        int y = Math.round(centerY - (size * 0.5F));
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        graphics.blit(x, y, 0, size, size, sprite);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        return true;
    }

    private static List<TextureAtlasSprite> resolveParticleSprites(Minecraft minecraft, ResourceLocation particleId) {
        List<TextureAtlasSprite> cached = PARTICLE_SPRITES.get(particleId);
        if (cached != null) {
            return cached;
        }

        List<TextureAtlasSprite> sprites = loadParticleAtlasSprites(minecraft, particleId);
        sprites = sprites.stream().filter(IconAssetValidator::sprite).distinct().toList();
        PARTICLE_SPRITES.put(particleId, sprites);
        return sprites;
    }

    private static List<TextureAtlasSprite> loadParticleAtlasSprites(Minecraft minecraft, ResourceLocation particleId) {
        List<ResourceLocation> frames = resolveParticleFrames(minecraft, particleId);
        if (frames.isEmpty()) {
            return List.of();
        }

        if (minecraft.particleEngine == null) return List.of();
        TextureAtlas atlas = ((de.artemis.omniwheel.mixin.client.ParticleEngineAccessor) minecraft.particleEngine).omniwheel$getTextureAtlas();

        TextureAtlasSprite missingSprite = atlas.getSprite(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation());
        List<TextureAtlasSprite> sprites = new ArrayList<>(frames.size());
        for (ResourceLocation frame : frames) {
            // Before 1.19.3 the particle atlas keeps the particle/ path prefix.
            TextureAtlasSprite sprite = atlas.getSprite(java.util.Objects.requireNonNull(
                    ResourceLocation.tryParse(frame.getNamespace() + ":particle/" + frame.getPath())));
            if (sprite != null && sprite != missingSprite && IconAssetValidator.sprite(sprite)) {
                sprites.add(sprite);
            }
        }
        return sprites.isEmpty() ? List.of() : List.copyOf(sprites);
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
        graphics.flush();
        graphics.pose().pushPose();
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

    private static ResourceLocation resolveGuiSprite(String icon) {
        String trimmed = icon == null ? "" : icon.trim();
        if (!trimmed.regionMatches(true, 0, GUI_PREFIX, 0, GUI_PREFIX.length())) {
            return null;
        }
        String key = trimmed.substring(GUI_PREFIX.length()).trim().toLowerCase(Locale.ROOT);
        return GUI_SPRITES.get(key);
    }

    private static TextureIcon resolveTextureIcon(String icon) {
        String trimmed = icon == null ? "" : icon.trim();
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

    private static String particleVisualSignature(Minecraft minecraft, ResourceLocation particleId) {
        List<TextureAtlasSprite> sprites = resolveParticleSprites(minecraft, particleId);
        if (sprites.isEmpty()) {
            return "";
        }
        StringBuilder signature = new StringBuilder();
        for (TextureAtlasSprite sprite : sprites) {
            if (!signature.isEmpty()) {
                signature.append('|');
            }
            signature.append(sprite.atlas().location()).append('#').append(sprite.getName());
        }
        return signature.toString();
    }

    private static List<ResourceLocation> loadParticleFrames(Minecraft minecraft, ResourceLocation particleId) {
        ResourceLocation definitionId = ResourceLocation.tryParse(particleId.getNamespace() + ":particles/" + particleId.getPath() + ".json");
        if (definitionId == null) {
            return List.of();
        }

        try (Resource resource = minecraft.getResourceManager().getResource(definitionId);
             Reader reader = new java.io.InputStreamReader(resource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8)) {
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
        } catch (IOException | RuntimeException ignored) {
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

    private static void registerGuiSprite(String path) {
        ResourceLocation id = ResourceLocation.tryParse("minecraft:" + path);
        if (id != null) {
            GUI_SPRITES.put(path.toLowerCase(Locale.ROOT), id);
        }
    }

    private static void registerTextureIcon(String key, String textureId, int textureWidth, int textureHeight, int u, int v, int srcWidth, int srcHeight) {
        ResourceLocation id = ResourceLocation.tryParse(textureId);
        if (id != null) {
            TEXTURE_ICONS.put(key.toLowerCase(Locale.ROOT), new TextureIcon(id, textureWidth, textureHeight, u, v, srcWidth, srcHeight));
        }
    }

    private record TextureIcon(ResourceLocation texture, int textureWidth, int textureHeight, int u, int v, int srcWidth, int srcHeight) {
    }
}
