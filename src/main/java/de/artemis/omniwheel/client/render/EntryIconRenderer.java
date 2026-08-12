package de.artemis.omniwheel.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class EntryIconRenderer {
    private static final String EFFECT_PREFIX = "effect:";
    private static final String GUI_PREFIX = "gui:";
    private static final String PARTICLE_PREFIX = "particle:";
    private static final String SYMBOL_PREFIX = "symbol:";
    private static final String TEXTURE_PREFIX = "texture:";
    private static final Field PARTICLE_RESOURCE_MANAGER_FIELD = findField(Minecraft.class, "particleEngine", "resourceManager");
    private static final Field PARTICLE_SPRITE_SETS_FIELD = findField(PARTICLE_RESOURCE_MANAGER_FIELD == null ? null : PARTICLE_RESOURCE_MANAGER_FIELD.getType(), "spriteSets");
    private static final Field PARTICLE_SPRITES_FIELD = findField(PARTICLE_RESOURCE_MANAGER_FIELD == null ? null : findMutableSpriteSetType(PARTICLE_RESOURCE_MANAGER_FIELD.getType()), "sprites");
    private static final Map<String, Identifier> GUI_SPRITES = new LinkedHashMap<>();
    private static final Map<Identifier, List<Identifier>> PARTICLE_FRAMES = new LinkedHashMap<>();
    private static final Map<Identifier, List<TextureAtlasSprite>> PARTICLE_SPRITES = new LinkedHashMap<>();
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

    public static boolean drawIcon(GuiGraphicsExtractor graphics, Minecraft minecraft, String icon, float centerX, float centerY, float scale) {
        ItemStack stack = resolveItemStack(icon);
        if (!stack.isEmpty()) {
            float iconSize = 16.0F * scale;
            graphics.nextStratum();
            graphics.pose().pushMatrix();
            graphics.pose().translate(centerX - (iconSize * 0.5F), centerY - (iconSize * 0.5F));
            graphics.pose().scale(scale, scale);
            graphics.item(stack, 0, 0);
            graphics.pose().popMatrix();
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
        return isItemIcon(icon) || isEffectIcon(icon) || isGuiSpriteIcon(icon) || isTextureIcon(icon) || isParticleIcon(icon) || isSymbolIcon(icon);
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
        Identifier particleId = resolveParticleId(icon);
        if (particleId == null) {
            return "";
        }
        return particleVisualSignature(minecraft, particleId);
    }

    private static ItemStack resolveItemStack(String icon) {
        String trimmed = icon == null ? "" : icon.trim();
        if (trimmed.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (!trimmed.contains(":")) {
            trimmed = "minecraft:" + trimmed;
        }

        Identifier id = Identifier.tryParse(trimmed);
        if (id == null) {
            return ItemStack.EMPTY;
        }

        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item != null && item != Items.AIR) {
            return new ItemStack(item);
        }

        Block block = BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
        if (block == null) {
            return ItemStack.EMPTY;
        }

        Item blockItem = block.asItem();
        return blockItem == Items.AIR ? ItemStack.EMPTY : new ItemStack(blockItem);
    }

    private static boolean drawEffectIcon(GuiGraphicsExtractor graphics, Minecraft minecraft, String icon, float centerX, float centerY, float scale) {
        Identifier effectId = resolveEffectId(icon);
        if (effectId == null) {
            return false;
        }

        Holder<MobEffect> effectHolder = BuiltInRegistries.MOB_EFFECT.get(effectId).orElse(null);
        if (effectHolder == null) {
            return false;
        }

        int size = Math.max(12, Math.round(18.0F * scale));
        int x = Math.round(centerX - (size * 0.5F));
        int y = Math.round(centerY - (size * 0.5F));
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Gui.getMobEffectSprite(effectHolder), x, y, size, size);
        return true;
    }

    private static boolean drawGuiSpriteIcon(GuiGraphicsExtractor graphics, String icon, float centerX, float centerY, float scale) {
        Identifier spriteId = resolveGuiSprite(icon);
        if (spriteId == null) {
            return false;
        }

        int size = Math.max(12, Math.round(18.0F * scale));
        int x = Math.round(centerX - (size * 0.5F));
        int y = Math.round(centerY - (size * 0.5F));
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, spriteId, x, y, size, size);
        return true;
    }

    private static boolean drawTextureIcon(GuiGraphicsExtractor graphics, String icon, float centerX, float centerY, float scale) {
        TextureIcon textureIcon = resolveTextureIcon(icon);
        if (textureIcon == null) {
            return false;
        }

        graphics.nextStratum();
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX - (8.0F * scale), centerY - (8.0F * scale));
        graphics.pose().scale(scale, scale);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
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
        graphics.pose().popMatrix();
        return true;
    }

    private static boolean drawParticleIcon(GuiGraphicsExtractor graphics, Minecraft minecraft, String icon, float centerX, float centerY, float scale) {
        Identifier particleId = resolveParticleId(icon);
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
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, size, size);
        return true;
    }

    private static List<TextureAtlasSprite> resolveParticleSprites(Minecraft minecraft, Identifier particleId) {
        List<TextureAtlasSprite> cached = PARTICLE_SPRITES.get(particleId);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        List<TextureAtlasSprite> sprites = loadBoundParticleSprites(minecraft, particleId);
        if (!sprites.isEmpty()) {
            PARTICLE_SPRITES.put(particleId, sprites);
        }
        return sprites;
    }

    @SuppressWarnings("unchecked")
    private static List<TextureAtlasSprite> loadBoundParticleSprites(Minecraft minecraft, Identifier particleId) {
        if (minecraft.particleEngine == null
                || PARTICLE_RESOURCE_MANAGER_FIELD == null
                || PARTICLE_SPRITE_SETS_FIELD == null
                || PARTICLE_SPRITES_FIELD == null) {
            return List.of();
        }

        try {
            Object resources = PARTICLE_RESOURCE_MANAGER_FIELD.get(minecraft.particleEngine);
            if (resources == null) {
                return List.of();
            }

            Object spriteSetsObject = PARTICLE_SPRITE_SETS_FIELD.get(resources);
            if (!(spriteSetsObject instanceof Map<?, ?> spriteSets)) {
                return List.of();
            }

            Object spriteSet = spriteSets.get(particleId);
            if (spriteSet == null) {
                return List.of();
            }

            Object spritesObject = PARTICLE_SPRITES_FIELD.get(spriteSet);
            if (!(spritesObject instanceof List<?> sprites) || sprites.isEmpty()) {
                return List.of();
            }

            List<TextureAtlasSprite> result = new ArrayList<>(sprites.size());
            for (Object sprite : sprites) {
                if (sprite instanceof TextureAtlasSprite textureAtlasSprite) {
                    result.add(textureAtlasSprite);
                }
            }
            return result.isEmpty() ? List.of() : List.copyOf(result);
        } catch (IllegalAccessException ignored) {
            return List.of();
        }
    }

    private static Class<?> findMutableSpriteSetType(Class<?> ownerType) {
        if (ownerType == null) {
            return null;
        }
        for (Class<?> declaredClass : ownerType.getDeclaredClasses()) {
            if (declaredClass.getSimpleName().equals("MutableSpriteSet")) {
                return declaredClass;
            }
        }
        return null;
    }

    private static Field findField(Class<?> ownerType, String... fieldPath) {
        if (ownerType == null || fieldPath.length == 0) {
            return null;
        }

        Class<?> currentType = ownerType;
        Field resolved = null;
        for (String fieldName : fieldPath) {
            try {
                resolved = currentType.getDeclaredField(fieldName);
                resolved.setAccessible(true);
                currentType = resolved.getType();
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }
        return resolved;
    }

    private static boolean drawSymbolIcon(GuiGraphicsExtractor graphics, Minecraft minecraft, String icon, float centerX, float centerY, float scale) {
        String symbol = resolveSymbol(icon);
        if (symbol == null) {
            return false;
        }

        float symbolScale = Math.max(0.9F, scale * 1.2F);
        int textWidth = minecraft.font.width(symbol);
        int x = Math.round((centerX / symbolScale) - (textWidth / 2.0F));
        int y = Math.round((centerY / symbolScale) - 4.0F);
        graphics.nextStratum();
        graphics.pose().pushMatrix();
        graphics.pose().scale(symbolScale, symbolScale);
        graphics.text(minecraft.font, symbol, x, y, 0xFFF2F5F8, false);
        graphics.pose().popMatrix();
        return true;
    }

    private static MobEffect resolveEffect(String icon) {
        Identifier effectId = resolveEffectId(icon);
        if (effectId == null) {
            return null;
        }
        return BuiltInRegistries.MOB_EFFECT.getOptional(effectId).orElse(null);
    }

    private static Identifier resolveEffectId(String icon) {
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
        Identifier effectId = Identifier.tryParse(effectPart);
        if (effectId == null) {
            return null;
        }
        return BuiltInRegistries.MOB_EFFECT.containsKey(effectId) ? effectId : null;
    }

    private static Identifier resolveGuiSprite(String icon) {
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

    private static Identifier resolveParticleId(String icon) {
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
        Identifier particleId = Identifier.tryParse(particlePart);
        if (particleId == null) {
            return null;
        }
        return BuiltInRegistries.PARTICLE_TYPE.containsKey(particleId) ? particleId : null;
    }

    private static List<Identifier> resolveParticleFrames(Minecraft minecraft, Identifier particleId) {
        List<Identifier> cached = PARTICLE_FRAMES.get(particleId);
        if (cached != null) {
            return cached;
        }

        List<Identifier> frames = loadParticleFrames(minecraft, particleId);
        PARTICLE_FRAMES.put(particleId, frames);
        return frames;
    }

    private static String particleVisualSignature(Minecraft minecraft, Identifier particleId) {
        List<TextureAtlasSprite> sprites = resolveParticleSprites(minecraft, particleId);
        if (sprites.isEmpty()) {
            return "";
        }
        StringBuilder signature = new StringBuilder();
        for (TextureAtlasSprite sprite : sprites) {
            if (!signature.isEmpty()) {
                signature.append('|');
            }
            signature.append(sprite.atlasLocation()).append('#').append(sprite.contents().name());
        }
        return signature.toString();
    }

    private static List<Identifier> loadParticleFrames(Minecraft minecraft, Identifier particleId) {
        Identifier definitionId = Identifier.tryParse(particleId.getNamespace() + ":particles/" + particleId.getPath() + ".json");
        if (definitionId == null) {
            return List.of();
        }

        Optional<Resource> resource = minecraft.getResourceManager().getResource(definitionId);
        if (resource.isEmpty()) {
            return List.of();
        }

        try (Reader reader = resource.get().openAsReader()) {
            JsonObject data = GsonHelper.parse(reader);
            JsonArray textures = GsonHelper.getAsJsonArray(data, "textures", null);
            if (textures == null || textures.isEmpty()) {
                return List.of();
            }

            List<Identifier> frames = new ArrayList<>();
            for (int index = 0; index < textures.size(); index++) {
                String textureId = GsonHelper.convertToString(textures.get(index), "texture");
                Identifier frameId = Identifier.tryParse(textureId);
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

    private static void registerGuiSprite(String path) {
        Identifier id = Identifier.tryParse("minecraft:" + path);
        if (id != null) {
            GUI_SPRITES.put(path.toLowerCase(Locale.ROOT), id);
        }
    }

    private static void registerTextureIcon(String key, String textureId, int textureWidth, int textureHeight, int u, int v, int srcWidth, int srcHeight) {
        Identifier id = Identifier.tryParse(textureId);
        if (id != null) {
            TEXTURE_ICONS.put(key.toLowerCase(Locale.ROOT), new TextureIcon(id, textureWidth, textureHeight, u, v, srcWidth, srcHeight));
        }
    }

    private record TextureIcon(Identifier texture, int textureWidth, int textureHeight, int u, int v, int srcWidth, int srcHeight) {
    }
}
