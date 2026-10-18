package de.artemis.omniwheel.client.render;

import de.artemis.omniwheel.common.OmniWheelText;
import de.artemis.omniwheel.common.action.OpenWheelAction;
import de.artemis.omniwheel.common.wheel.WheelEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import java.util.List;

/** Shared segment rendering for the actual wheel and its interactive editor preview. */
public final class RadialWheelRenderer {
    public static final float OUTER_RADIUS = 118;
    public static final float INNER_RADIUS = 46;
    private RadialWheelRenderer() {}
    private static final int SEGMENT_GAP_DEGREES = 2;
    private static final int SEGMENT_COLOR = 0xD8212831;
    private static final int SEGMENT_ALT_COLOR = 0xD929313D;
    private static final int SELECTED_COLOR = 0xF042556C;
    private static final int SELECTED_OUTLINE = 0xFF8EE1FF;
    private static final int SUBWHEEL_HINT = 0xFFE4B96F;
    private static final int TEXT_PRIMARY = 0xFFF2F5F8;
    private static final int TEXT_SECONDARY = 0xFF9CAAB8;
    private static final int TEXT_SHORTCUT_SELECTED = 0xFFC9D8E7;
    public static void render(GuiGraphics graphics, Minecraft minecraft, List<WheelEntry> visibleEntries,
                              float[] selections, float centerX, float centerY, float ringInnerRadius,
                              float ringOuterRadius, int placeholderIndex) {
        render(graphics, minecraft, visibleEntries, selections, centerX, centerY, ringInnerRadius,
                ringOuterRadius, placeholderIndex, null);
    }

    public static void render(GuiGraphics graphics, Minecraft minecraft, List<WheelEntry> visibleEntries,
                              float[] selections, float centerX, float centerY, float ringInnerRadius,
                              float ringOuterRadius, int placeholderIndex, double[] angleOffsets) {
        render(graphics, minecraft, visibleEntries, selections, centerX, centerY, ringInnerRadius,
                ringOuterRadius, placeholderIndex, angleOffsets, -1, true);
    }

    public static void render(GuiGraphics graphics, Minecraft minecraft, List<WheelEntry> visibleEntries,
                              float[] selections, float centerX, float centerY, float ringInnerRadius,
                              float ringOuterRadius, int placeholderIndex, double[] angleOffsets,
                              int dropIndex, boolean validDrop) {
        render(graphics, minecraft, visibleEntries, selections, centerX, centerY, ringInnerRadius,
                ringOuterRadius, placeholderIndex, angleOffsets, dropIndex, validDrop, null);
    }

    public static void render(GuiGraphics graphics, Minecraft minecraft, List<WheelEntry> visibleEntries,
                              float[] selections, float centerX, float centerY, float ringInnerRadius,
                              float ringOuterRadius, int placeholderIndex, double[] angleOffsets,
                              int dropIndex, boolean validDrop, boolean[] enabledEntries) {
        int segmentCount = visibleEntries.size();
        double gapWidth = segmentGapWidth(ringOuterRadius, segmentCount);
        for (int index = 0; index < segmentCount; index++) {
            if (index == placeholderIndex || !(visibleEntries.get(index).action() instanceof OpenWheelAction)) continue;
            boolean enabled = enabledEntries == null ? visibleEntries.get(index).active() : enabledEntries[index];
            float outer = ringOuterRadius + (enabled ? 6.0F * selections[index] : 0);
            SegmentShape shape = segmentShape(index, segmentCount, ringInnerRadius, outer, gapWidth)
                    .offset(angleOffsets == null ? 0 : angleOffsets[index]);
            drawStackLayer(graphics, shape, centerX, centerY, ringInnerRadius, outer, 5, 2, 0xFF18212B, 0xFF384E62);
            drawStackLayer(graphics, shape, centerX, centerY, ringInnerRadius, outer, 2.5F, 1, 0xFF253340, 0xFF657888);
        }
        for (int index = 0; index < segmentCount; index++) {
            WheelEntry entry = visibleEntries.get(index);
            boolean enabled = enabledEntries == null ? entry.active() : enabledEntries[index];
            float selection = enabled ? selections[index] : 0;
            float segmentOuter = ringOuterRadius + 6.0F * selection;
            double offset = angleOffsets == null ? 0 : angleOffsets[index];
            SegmentShape segmentShape = segmentShape(index, segmentCount, ringInnerRadius, segmentOuter, gapWidth).offset(offset);
            SegmentPalette palette = paletteForGuiColor(entry.guiColor());
            int baseColor = index % 2 == 0 ? palette.baseFill() : palette.altFill();
            int fillColor = RadialAnimation.blendColor(baseColor, palette.selectedFill(), selection);
            if (index == dropIndex) fillColor = validDrop ? 0xE0374E60 : 0xE0634230;
            if (!enabled && index != dropIndex) fillColor = RadialAnimation.blendColor(baseColor | 0xFF000000, 0xFF161E27, .65F);
            if (index == placeholderIndex) fillColor = (fillColor & 0x00FFFFFF) | 0x50000000;
            // The top sheet must cover the outlines of the stacked sheets below it.
            if (index != placeholderIndex && entry.action() instanceof OpenWheelAction) {
                fillColor |= 0xFF000000;
            }
            int outlineColor = RadialAnimation.blendColor(palette.selectedOutline() & 0x00FFFFFF, palette.selectedOutline(), selection);

            GeometryRenderer.fillRingSegment(
                    graphics,
                    centerX,
                    centerY,
                    ringInnerRadius,
                    segmentOuter,
                    segmentShape.innerStartAngle(),
                    segmentShape.innerEndAngle(),
                    segmentShape.outerStartAngle(),
                    segmentShape.outerEndAngle(),
                    fillColor
            );

            if (selection > 0) {
                SegmentShape innerOutline = segmentShape(index, segmentCount, ringInnerRadius - 2.0F, ringInnerRadius + 1.0F, gapWidth).offset(offset);
                SegmentShape outerOutline = segmentShape(index, segmentCount, segmentOuter - 1.0F, segmentOuter + 2.0F, gapWidth).offset(offset);
                GeometryRenderer.fillRingSegment(
                        graphics,
                        centerX,
                        centerY,
                        ringInnerRadius - 2.0F,
                        ringInnerRadius + 1.0F,
                        innerOutline.innerStartAngle(),
                        innerOutline.innerEndAngle(),
                        innerOutline.outerStartAngle(),
                        innerOutline.outerEndAngle(),
                        outlineColor
                );
                GeometryRenderer.fillRingSegment(
                        graphics,
                        centerX,
                        centerY,
                        segmentOuter - 1.0F,
                        segmentOuter + 2.0F,
                        outerOutline.innerStartAngle(),
                        outerOutline.innerEndAngle(),
                        outerOutline.outerStartAngle(),
                        outerOutline.outerEndAngle(),
                    outlineColor
                );
            }

            if (index == dropIndex) drawDropOutline(graphics, segmentShape, centerX, centerY, ringInnerRadius,
                    segmentOuter, validDrop ? 0xFF8EE1FF : 0xFFFFC46A);
            if (index != placeholderIndex) drawEntry(graphics, minecraft, entry, centerX, centerY, ringInnerRadius, segmentOuter, segmentShape.midAngle(), selection, enabled);
        }

    }

    private static void drawDropOutline(GuiGraphics graphics, SegmentShape shape, float x, float y,
                                         float inner, float outer, int color) {
        GeometryRenderer.fillRingSegment(graphics, x, y, inner, inner + 1,
                shape.innerStartAngle(), shape.innerEndAngle(), color);
        GeometryRenderer.fillRingSegment(graphics, x, y, outer - 1, outer,
                shape.outerStartAngle(), shape.outerEndAngle(), color);
        if (shape.outerEndAngle() - shape.outerStartAngle() >= Math.PI * 2 - .001) return;
        GeometryRenderer.fillRingSegment(graphics, x, y, inner, outer,
                shape.innerStartAngle(), shape.innerStartAngle() + .018,
                shape.outerStartAngle(), shape.outerStartAngle() + .009, color);
        GeometryRenderer.fillRingSegment(graphics, x, y, inner, outer,
                shape.innerEndAngle() - .018, shape.innerEndAngle(),
                shape.outerEndAngle() - .009, shape.outerEndAngle(), color);
    }

    private static void drawStackLayer(GuiGraphics graphics, SegmentShape shape, float centerX, float centerY,
                                       float inner, float outer, float distance, float depth, int fill, int edge) {
        float x = centerX + (float) Math.cos(shape.midAngle()) * distance;
        float y = centerY + (float) Math.sin(shape.midAngle()) * distance + depth;
        GeometryRenderer.fillRingSegment(graphics, x, y, inner, outer, shape.innerStartAngle(), shape.innerEndAngle(),
                shape.outerStartAngle(), shape.outerEndAngle(), edge);
        GeometryRenderer.fillRingSegment(graphics, x, y, inner + 1, outer - 1,
                shape.innerStartAngle() + .01, shape.innerEndAngle() - .01,
                shape.outerStartAngle() + .01, shape.outerEndAngle() - .01, fill);
    }

    private static void drawEntry(
            GuiGraphics graphics,
            Minecraft minecraft,
            WheelEntry entry,
            float centerX,
            float centerY,
            float innerRadius,
            float outerRadius,
            double labelAngle,
            float selection,
            boolean enabled
    ) {
        float labelRadius = (innerRadius + outerRadius) * 0.52F;
        int labelX = Math.round(centerX + ((float) Math.cos(labelAngle) * labelRadius));
        int labelY = Math.round(centerY + ((float) Math.sin(labelAngle) * labelRadius));
        int textColor = enabled ? RadialAnimation.blendColor(entry.color(), TEXT_PRIMARY, selection) : 0xFF687A8A;
        boolean showIcon = entry.showIcon() && EntryIconRenderer.isUsableIcon(minecraft, entry.glyph());
        boolean showLabel = entry.showLabel();
        boolean showShortcut = entry.showShortcut() && !entry.shortcut().isBlank();
        int maxTextWidth = Math.max(34, Math.round((outerRadius - innerRadius) * 0.9F));

        float iconScale;
        float iconCenterY;
        int labelTextY;
        int shortcutTextY;

        if (showIcon && showLabel && showShortcut) {
            iconScale = 0.88F;
            iconCenterY = labelY - 9.0F;
            labelTextY = labelY - 1;
            shortcutTextY = labelY + 10;
        } else if (showIcon && showLabel) {
            iconScale = 0.94F;
            iconCenterY = labelY - 6.0F;
            labelTextY = labelY + 2;
            shortcutTextY = labelY + 11;
        } else if (showIcon && showShortcut) {
            iconScale = 1.04F;
            iconCenterY = labelY - 6.0F;
            labelTextY = labelY + 1;
            shortcutTextY = labelY + 7;
        } else if (showIcon) {
            iconScale = 1.28F;
            iconCenterY = labelY + 1.0F;
            labelTextY = labelY + 1;
            shortcutTextY = labelY + 5;
        } else if (showLabel && showShortcut) {
            iconScale = 0.0F;
            iconCenterY = labelY;
            labelTextY = labelY - 4;
            shortcutTextY = labelY + 7;
        } else if (showLabel) {
            iconScale = 0.0F;
            iconCenterY = labelY;
            labelTextY = labelY + 1;
            shortcutTextY = labelY + 7;
        } else {
            iconScale = 0.0F;
            iconCenterY = labelY;
            labelTextY = labelY + 1;
            shortcutTextY = labelY + 2;
        }

        if (showIcon) {
            iconScale *= 1 + 0.035F * selection;
            EntryIconRenderer.drawIcon(graphics, minecraft, entry.glyph(), labelX, iconCenterY, iconScale);
        }
        if (showLabel) {
            String label = fitPlainText(minecraft.font, OmniWheelText.resolve(entry.label()), maxTextWidth);
            graphics.drawCenteredString(minecraft.font, label, labelX, labelTextY, textColor);
        }
        if (showShortcut) {
            float shortcutScale = showLabel ? 0.8F : 0.88F;
            String shortcut = fitPlainText(minecraft.font, entry.shortcut(), Math.round(maxTextWidth / shortcutScale));
            drawScaledCenteredString(
                    graphics,
                    minecraft.font,
                    shortcut,
                    labelX,
                    shortcutTextY,
                    shortcutScale,
                    enabled ? RadialAnimation.blendColor(TEXT_SECONDARY, TEXT_SHORTCUT_SELECTED, selection) : 0xFF687A8A
            );
        }
    }

    private static double segmentGapWidth(float outerRadius, int segmentCount) {
        if (segmentCount <= 1) {
            return 0.0D;
        }
        return Math.toRadians(SEGMENT_GAP_DEGREES) * outerRadius;
    }

    private static SegmentShape segmentShape(int index, int segmentCount, float innerRadius, float outerRadius, double gapWidth) {
        double start = RadialLayout.segmentStartAngle(index, segmentCount);
        double end = RadialLayout.segmentEndAngle(index, segmentCount);
        double span = end - start;
        double innerOffset = gapAngleOffset(gapWidth, innerRadius, span);
        double outerOffset = gapAngleOffset(gapWidth, outerRadius, span);
        double innerStart = start + innerOffset;
        double innerEnd = end - innerOffset;
        double outerStart = start + outerOffset;
        double outerEnd = end - outerOffset;
        double midAngle = (innerStart + innerEnd + outerStart + outerEnd) * 0.25D;
        return new SegmentShape(innerStart, innerEnd, outerStart, outerEnd, midAngle);
    }

    private static double gapAngleOffset(double gapWidth, float radius, double span) {
        if (gapWidth <= 0.0D || radius <= 0.0F) {
            return 0.0D;
        }
        return Math.min(span * 0.49D, gapWidth / Math.max(radius * 2.0D, 1.0D));
    }

    private static String fitPlainText(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }

        String fitted = text;
        while (!fitted.isEmpty() && font.width(fitted + "...") > maxWidth) {
            fitted = fitted.substring(0, fitted.length() - 1);
        }
        return fitted.isEmpty() ? "..." : fitted + "...";
    }

    private static void drawScaledCenteredString(
            GuiGraphics graphics,
            Font font,
            String text,
            int centerX,
            int topY,
            float scale,
            int color
    ) {
        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);
        graphics.drawCenteredString(
                font,
                text,
                Math.round(centerX / scale),
                Math.round(topY / scale),
                color
        );
        graphics.pose().popMatrix();
    }

    private static SegmentPalette paletteForGuiColor(int guiColor) {
        return switch (guiColor) {
            case WheelEntry.DEFAULT_GUI_COLOR -> new SegmentPalette(
                    SEGMENT_COLOR,
                    SEGMENT_ALT_COLOR,
                    SELECTED_COLOR,
                    SELECTED_OUTLINE
            );
            case 0xFFFFD36A -> new SegmentPalette(
                    0xD8343022,
                    0xD83E3827,
                    0xF06D5A2F,
                    0xFFFFE08A
            );
            case 0xFF7FE3FF -> new SegmentPalette(
                    0xD81C2D35,
                    0xD8243740,
                    0xF0365E6E,
                    0xFF9EEBFF
            );
            case 0xFF8EE89A -> new SegmentPalette(
                    0xD81D2E24,
                    0xD825382C,
                    0xF0376446,
                    0xFFA7F2B1
            );
            case 0xFFFF9AB5 -> new SegmentPalette(
                    0xD8322430,
                    0xD83C2C3A,
                    0xF0634261,
                    0xFFFFB5CB
            );
            case 0xFFD1A0FF -> new SegmentPalette(
                    0xD82A2435,
                    0xD8332D40,
                    0xF0524573,
                    0xFFE0BCFF
            );
            case 0xFFFFB36A -> new SegmentPalette(
                    0xD834281F,
                    0xD83F3125,
                    0xF06B4C30,
                    0xFFFFC68A
            );
            case 0xFFA7B6C6 -> new SegmentPalette(
                    0xD8262C33,
                    0xD82F363E,
                    0xF0465565,
                    0xFFC3D1DF
            );
            default -> new SegmentPalette(
                    RadialAnimation.blendColor(0xFF101820, guiColor | 0xFF000000, .22F) & 0x00FFFFFF | 0xD8000000,
                    RadialAnimation.blendColor(0xFF18212B, guiColor | 0xFF000000, .28F) & 0x00FFFFFF | 0xD8000000,
                    RadialAnimation.blendColor(0xFF222D39, guiColor | 0xFF000000, .52F) & 0x00FFFFFF | 0xF0000000,
                    RadialAnimation.blendColor(guiColor | 0xFF000000, 0xFFFFFFFF, .25F)
            );
        };
    }

    private record SegmentShape(double innerStartAngle, double innerEndAngle, double outerStartAngle, double outerEndAngle, double midAngle) {
        SegmentShape offset(double angle) {
            return new SegmentShape(innerStartAngle + angle, innerEndAngle + angle, outerStartAngle + angle,
                    outerEndAngle + angle, midAngle + angle);
        }
    }
    private record SegmentPalette(int baseFill, int altFill, int selectedFill, int selectedOutline) {}
}
