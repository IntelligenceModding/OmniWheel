package de.artemis.omniwheel.client.tutorial;

import de.artemis.omniwheel.client.navigation.ManagementNavigation.Bounds;
import de.artemis.omniwheel.client.render.GeometryRenderer;
import net.minecraft.client.gui.GuiGraphics;
import java.util.List;

/** Dims the existing rendered interface rather than hiding or reconstructing controls. */
public final class TutorialSpotlight {
    private TutorialSpotlight() {}
    public static void drawCircle(GuiGraphics graphics, int width, int height,
                                  float centerX, float centerY, float radius) {
        // A single annulus covers the viewport without overlapping translucent strips.
        float coveringRadius = (float) Math.hypot(Math.max(Math.abs(centerX), Math.abs(width - centerX)),
                Math.max(Math.abs(centerY), Math.abs(height - centerY))) + 2;
        graphics.flush();
        GeometryRenderer.fillRingSegment(graphics, centerX, centerY, radius,
                Math.max(radius + 1, coveringRadius), 0, Math.PI * 2, 0x94000000);
        graphics.flush();
        GeometryRenderer.fillRingSegment(graphics, centerX, centerY, Math.max(0, radius - 0.5F),
                radius + 0.5F, 0, Math.PI * 2, 0xFF8BDAF8);
        graphics.flush();
    }
    public static Bounds clipped(int width, int height, Bounds focus) {
        int left = Math.clamp(focus.x(), 0, width), top = Math.clamp(focus.y(), 0, height);
        int right = Math.clamp(focus.right(), left, width), bottom = Math.clamp(focus.bottom(), top, height);
        return new Bounds(left, top, right - left, bottom - top);
    }
    public static List<Bounds> dimmedRegions(int width, int height, Bounds focus) {
        Bounds area = clipped(width, height, focus);
        return List.of(new Bounds(0, 0, width, area.y()),
                new Bounds(0, area.bottom(), width, height - area.bottom()),
                new Bounds(0, area.y(), area.x(), area.height()),
                new Bounds(area.right(), area.y(), width - area.right(), area.height()));
    }
    public static void draw(GuiGraphics graphics, int width, int height, Bounds focus) {
        graphics.flush();
        for (Bounds region : dimmedRegions(width, height, focus)) {
            if (region.width() > 0 && region.height() > 0) graphics.fill(region.x(), region.y(), region.right(), region.bottom(), 0x94000000);
        }
        Bounds area = clipped(width, height, focus);
        if (area.width() <= 0 || area.height() <= 0) return;
        int edge = 0xFF8BDAF8;
        graphics.fill(area.x(), area.y(), area.right(), area.y() + 1, edge);
        graphics.fill(area.x(), area.bottom() - 1, area.right(), area.bottom(), edge);
        graphics.fill(area.x(), area.y(), area.x() + 1, area.bottom(), edge);
        graphics.fill(area.right() - 1, area.y(), area.right(), area.bottom(), edge);
        graphics.flush();
    }
}
