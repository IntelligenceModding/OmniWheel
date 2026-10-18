package de.artemis.omniwheel.client.render;

import net.minecraft.client.Minecraft;
import de.artemis.omniwheel.client.render.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/** Keeps profile GUI icon IDs stable using 1.18.2's resource-pack texture sheets. */
public final class LegacyGuiIcons {
    private record Region(ResourceLocation texture, int textureWidth, int textureHeight, int x, int y, int width, int height) {}
    private static final Map<String, Region> REGIONS = new LinkedHashMap<>();

    static {
        icon("hud/crosshair", 0, 0, 15, 15);
        icon("hud/armor_empty", 16, 9, 9, 9);
        icon("hud/armor_half", 25, 9, 9, 9);
        icon("hud/armor_full", 34, 9, 9, 9);
        icon("hud/food_empty", 16, 27, 9, 9);
        icon("hud/food_half", 61, 27, 9, 9);
        icon("hud/food_full", 52, 27, 9, 9);
        icon("hud/food_empty_hunger", 133, 27, 9, 9);
        icon("hud/food_half_hunger", 97, 27, 9, 9);
        icon("hud/food_full_hunger", 88, 27, 9, 9);
        icon("hud/air", 16, 18, 9, 9);
        icon("hud/air_bursting", 25, 18, 9, 9);
        icon("hud/heart/container", 16, 0, 9, 9);
        hearts("", 52, 0);
        hearts("hardcore_", 52, 45);
        hearts("poisoned_", 88, 0);
        hearts("withered_", 124, 0);
        hearts("absorbing_", 160, 0);
        hearts("frozen_", 178, 0);
        hearts("vehicle_", 52, 9);
        REGIONS.put("icon/checkmark", new Region(new ResourceLocation("omniwheel", "textures/gui/checkmark.png"), 9, 8, 0, 0, 9, 8));
        region("icon/search", "recipe_book", 256, 256, 10, 14, 13, 13);
        region("icon/language", "widgets", 256, 256, 0, 106, 20, 20);
        region("icon/accessibility", "accessibility", 32, 64, 0, 0, 32, 32);
        REGIONS.put("icon/draft_report", new Region(new ResourceLocation("omniwheel", "textures/gui/report_button.png"), 64, 64, 0, 0, 20, 20));
        for (int bars = 1; bars <= 5; bars++) icon("icon/ping_" + bars, 0, 176 + (5 - bars) * 8, 10, 8);
        icon("icon/ping_unknown", 0, 216, 10, 8);
    }

    private LegacyGuiIcons() {}

    private static void hearts(String prefix, int x, int y) {
        icon("hud/heart/" + prefix + "full", x, y, 9, 9);
        icon("hud/heart/" + prefix + "half", x + 9, y, 9, 9);
    }

    private static void icon(String id, int x, int y, int width, int height) {
        region(id, "icons", 256, 256, x, y, width, height);
    }

    private static void region(String id, String texture, int tw, int th, int x, int y, int width, int height) {
        REGIONS.put(id, new Region(new ResourceLocation("minecraft", "textures/gui/" + texture + ".png"), tw, th, x, y, width, height));
    }

    public static boolean isUsable(Minecraft minecraft, ResourceLocation id) {
        Region region = "minecraft".equals(id.getNamespace()) ? REGIONS.get(id.getPath()) : null;
        return region != null && IconAssetValidator.texture(minecraft, region.texture(), region.textureWidth(), region.textureHeight(),
                region.x(), region.y(), region.width(), region.height());
    }

    public static void draw(GuiGraphics graphics, ResourceLocation id, int x, int y, int width, int height) {
        Region region = REGIONS.get(id.getPath());
        if (region == null) return;
        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(width / (float) region.width(), height / (float) region.height(), 1);
        graphics.blit(region.texture(), 0, 0, region.x(), region.y(), region.width(), region.height(), region.textureWidth(), region.textureHeight());
        graphics.pose().popPose();
    }
}
