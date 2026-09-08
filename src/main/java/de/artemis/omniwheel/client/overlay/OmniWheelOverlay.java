package de.artemis.omniwheel.client.overlay;

import com.mojang.blaze3d.platform.InputConstants;
import de.artemis.omniwheel.client.input.OmniWheelKeyMappings;
import de.artemis.omniwheel.client.render.EntryIconRenderer;
import de.artemis.omniwheel.client.render.GeometryRenderer;
import de.artemis.omniwheel.client.render.RadialLayout;
import de.artemis.omniwheel.client.runtime.OmniWheelClientRuntime;
import de.artemis.omniwheel.client.tutorial.TutorialType;
import de.artemis.omniwheel.client.runtime.WheelAvailabilityResolver;
import de.artemis.omniwheel.common.action.FunctionAction;
import de.artemis.omniwheel.common.action.GameplayFunction;
import de.artemis.omniwheel.common.action.OpenScreenAction;
import de.artemis.omniwheel.common.action.OpenWheelAction;
import de.artemis.omniwheel.common.action.ScreenTarget;
import de.artemis.omniwheel.common.OmniWheelText;
import de.artemis.omniwheel.common.profile.WheelProfile;
import de.artemis.omniwheel.common.wheel.WheelDefinition;
import de.artemis.omniwheel.common.wheel.WheelEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class OmniWheelOverlay {
    private static final int MAX_RENDERED_SEGMENTS = 8;
    private static final float WHEEL_RADIUS = 118.0F;
    private static final float DEADZONE_RADIUS = 28.0F;
    private static final int SEGMENT_GAP_DEGREES = 2;
    private static final int PANEL_COLOR = 0xEE141A22;
    private static final int PANEL_OUTLINE = 0xFF6AC7FF;
    private static final int SEGMENT_COLOR = 0xD8212831;
    private static final int SEGMENT_ALT_COLOR = 0xD929313D;
    private static final int CENTER_BUTTON_COLOR = 0xF029313D;
    private static final int CENTER_BUTTON_OUTLINE = 0xFF465462;
    private static final int SELECTED_COLOR = 0xF042556C;
    private static final int SELECTED_OUTLINE = 0xFF8EE1FF;
    private static final int SUBWHEEL_HINT = 0xFFE4B96F;
    private static final int TEXT_PRIMARY = 0xFFF2F5F8;
    private static final int TEXT_SECONDARY = 0xFF9CAAB8;
    private static final int TEXT_SHORTCUT_SELECTED = 0xFFC9D8E7;
    private static final int TUTORIAL_PANEL = 0xF019212B;
    private static final int TUTORIAL_BUTTON = 0xE6232C36;
    private static final int TUTORIAL_BUTTON_HIGHLIGHT = 0xF03B5369;
    private static final int TUTORIAL_EDGE = 0xFF406176;
    private static final int RADIAL_TUTORIAL_STEP_COUNT = 5;
    private static final int REFERENCE_UI_WIDTH = 960;
    private static final int REFERENCE_UI_HEIGHT = 540;
    private final OmniWheelClientRuntime runtime;
    private final Deque<String> wheelPath = new ArrayDeque<>();
    private final Set<String> pressedHotkeys = new HashSet<>();
    private final Set<Integer> pressedRadialPositionKeys = new HashSet<>();

    private boolean open;
    private boolean keyDown;
    private boolean leftMouseDown;
    private boolean rightMouseDown;
    private boolean mouseReleased;
    private boolean centerBackHovered;
    private boolean radialPositionInputLocked;
    private boolean radialPositionModeActive;
    private int hoveredIndex = -1;
    private int currentPageIndex;
    private int radialTutorialStepIndex = -1;
    private int lockedHotbarSlot = -1;
    private int tutorialNextButtonX;
    private int tutorialNextButtonY;
    private int tutorialNextButtonWidth;
    private int tutorialNextButtonHeight;
    private int tutorialSkipAllButtonX;
    private int tutorialSkipAllButtonY;
    private int tutorialSkipAllButtonWidth;
    private int tutorialSkipAllButtonHeight;
    private double lastFreeRawMouseX;
    private double lastFreeRawMouseY;
    private double accumulatedScrollX;
    private double accumulatedScrollY;

    public OmniWheelOverlay(OmniWheelClientRuntime runtime) {
        this.runtime = runtime;
    }

    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean triggerDown = OmniWheelKeyMappings.OPEN_WHEEL.isDown();
        boolean canOpen = minecraft.player != null && minecraft.level != null && minecraft.screen == null;

        if (!canOpen) {
            close();
            keyDown = triggerDown;
            leftMouseDown = isLeftMouseDown(minecraft);
            rightMouseDown = isRightMouseDown(minecraft);
            return;
        }

        if (!open) {
            if (triggerDown && !keyDown) {
                openRoot(minecraft);
            }
            keyDown = triggerDown;
            leftMouseDown = isLeftMouseDown(minecraft);
            rightMouseDown = isRightMouseDown(minecraft);
            return;
        }

        if (isRadialTutorialActive() && minecraft.mouseHandler.isMouseGrabbed()) {
            ensureMouseReleased(minecraft);
            keyDown = triggerDown;
            return;
        }
        if (!isRadialTutorialActive() || !mouseReleased) {
            ensureMouseReleased(minecraft);
        }
        boolean leftPressed = isLeftMouseDown(minecraft);
        boolean rightPressed = isRightMouseDown(minecraft);
        if (!minecraft.mouseHandler.isMouseGrabbed()) {
            lastFreeRawMouseX = minecraft.mouseHandler.xpos();
            lastFreeRawMouseY = minecraft.mouseHandler.ypos();
        }
        List<WheelEntry> visibleEntries = currentDisplayEntries();

        if (isRadialTutorialActive()) {
            suppressGameplayInput(minecraft);
            hoveredIndex = -1;
            centerBackHovered = false;
            if (leftPressed && !leftMouseDown) {
                handleTutorialClick(tutorialMouseX(minecraft), tutorialMouseY(minecraft), minecraft);
            }
            keyDown = triggerDown;
            leftMouseDown = leftPressed;
            rightMouseDown = rightPressed;
            return;
        }

        if (!radialPositionModeActive && handleHotkeys(minecraft, visibleEntries)) {
            leftMouseDown = leftPressed;
            rightMouseDown = rightPressed;
            keyDown = triggerDown;
            return;
        }

        if (handleRadialPositionKeys(visibleEntries)) {
            leftMouseDown = leftPressed;
            rightMouseDown = rightPressed;
            keyDown = triggerDown;
            return;
        }

        if (consumePageScroll(minecraft)) {
            visibleEntries = currentDisplayEntries();
            updateSelection(freeMouseX(minecraft), freeMouseY(minecraft), minecraft);
        }

        if (leftPressed && !leftMouseDown) {
            activateHoveredEntry(visibleEntries, false);
            leftMouseDown = true;
            rightMouseDown = rightPressed;
            keyDown = triggerDown;
            return;
        }

        if (rightPressed && !rightMouseDown) {
            if (!popWheel()) {
                close();
            }
            leftMouseDown = leftPressed;
            rightMouseDown = true;
            keyDown = triggerDown;
            return;
        }

        updateSelection(freeMouseX(minecraft), freeMouseY(minecraft), minecraft);
        lastFreeRawMouseX = minecraft.mouseHandler.xpos();
        lastFreeRawMouseY = minecraft.mouseHandler.ypos();
        if (!triggerDown && keyDown) {
            if (!radialPositionModeActive) {
                activateHoveredEntry(visibleEntries, true);
            } else {
                close();
                keyDown = triggerDown;
                leftMouseDown = leftPressed;
                rightMouseDown = rightPressed;
                return;
            }
        }
        keyDown = triggerDown;
        leftMouseDown = leftPressed;
        rightMouseDown = rightPressed;
    }

    public void render(GuiGraphicsExtractor graphics) {
        if (!open) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        double uiScale = uiScale(minecraft);
        int width = uiWidth(minecraft);
        int height = uiHeight(minecraft);
        if (!isRadialTutorialActive()) {
            updateSelection(freeMouseX(minecraft), freeMouseY(minecraft), minecraft);
        } else {
            hoveredIndex = -1;
            centerBackHovered = false;
        }

        graphics.pose().pushMatrix();
        graphics.pose().scale((float) uiScale, (float) uiScale);
        WheelDefinition wheel = currentWheel();
        List<WheelEntry> visibleEntries = currentDisplayEntries();
        int segmentCount = renderedSegmentCount(visibleEntries);
        float centerX = width * 0.5F;
        float centerY = height * 0.5F;
        float outerRadius = WHEEL_RADIUS;
        float innerRadius = DEADZONE_RADIUS;
        float ringInnerRadius = innerRadius + 18.0F;
        float ringOuterRadius = outerRadius;
        double gapWidth = segmentGapWidth(ringOuterRadius, segmentCount);

        for (int index = 0; index < segmentCount; index++) {
            WheelEntry entry = visibleEntries.get(index);
            boolean selected = index == hoveredIndex;
            float segmentOuter = selected ? ringOuterRadius + 6.0F : ringOuterRadius;
            SegmentShape segmentShape = segmentShape(index, segmentCount, ringInnerRadius, segmentOuter, gapWidth);
            SegmentPalette palette = paletteForGuiColor(entry.guiColor());
            int fillColor = selected ? palette.selectedFill() : (index % 2 == 0 ? palette.baseFill() : palette.altFill());
            int outlineColor = palette.selectedOutline();

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

            if (selected) {
                SegmentShape innerOutline = segmentShape(index, segmentCount, ringInnerRadius - 2.0F, ringInnerRadius + 1.0F, gapWidth);
                SegmentShape outerOutline = segmentShape(index, segmentCount, segmentOuter - 1.0F, segmentOuter + 2.0F, gapWidth);
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

            drawEntry(graphics, minecraft, entry, centerX, centerY, ringInnerRadius, segmentOuter, segmentShape.midAngle(), selected);
        }

        drawCenterPanel(graphics, minecraft, width, height, wheel, visibleEntries);
        drawRadialTutorial(graphics, minecraft, width, height, centerX, centerY, ringOuterRadius);
        graphics.pose().popMatrix();
    }

    public void close() {
        if (!open && !mouseReleased) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        open = false;
        wheelPath.clear();
        hoveredIndex = -1;
        centerBackHovered = false;
        currentPageIndex = 0;
        lockedHotbarSlot = -1;
        leftMouseDown = false;
        rightMouseDown = false;
        pressedHotkeys.clear();
        pressedRadialPositionKeys.clear();
        radialPositionInputLocked = false;
        radialPositionModeActive = false;
        radialTutorialStepIndex = -1;
        accumulatedScrollX = 0.0D;
        accumulatedScrollY = 0.0D;

        if (mouseReleased) {
            mouseReleased = false;
            if (minecraft.screen == null && minecraft.player != null && minecraft.level != null) {
                minecraft.mouseHandler.grabMouse();
            }
        }
    }

    public boolean isOpen() {
        return open;
    }

    public void openWheelPath(List<String> wheelIds) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }

        boolean wasOpen = open;
        runtime.profileManager().ensureLoaded();
        WheelProfile profile = runtime.activeProfile();
        List<String> path = wheelIds == null || wheelIds.isEmpty() ? List.of(resolveOpeningWheelId(profile)) : wheelIds;
        wheelPath.clear();
        wheelPath.addAll(path);
        open = true;
        hoveredIndex = -1;
        centerBackHovered = false;
        currentPageIndex = 0;
        leftMouseDown = isLeftMouseDown(minecraft);
        rightMouseDown = isRightMouseDown(minecraft);
        pressedHotkeys.clear();
        lastFreeRawMouseX = minecraft.mouseHandler.xpos();
        lastFreeRawMouseY = minecraft.mouseHandler.ypos();
        lockedHotbarSlot = selectedHotbarSlot(minecraft.player.getInventory());
        accumulatedScrollX = 0.0D;
        accumulatedScrollY = 0.0D;
        if (!wasOpen && !runtime.tutorialProgressStore().isCompleted(TutorialType.RADIAL)) {
            radialTutorialStepIndex = runtime.tutorialProgressStore().currentStep(TutorialType.RADIAL);
        }

        ensureMouseReleased(minecraft);
        updateSelection(freeMouseX(minecraft), freeMouseY(minecraft), minecraft);
    }

    private void openRoot(Minecraft minecraft) {
        runtime.profileManager().ensureLoaded();
        WheelProfile profile = runtime.activeProfile();
        openWheelPath(List.of(resolveOpeningWheelId(profile)));
    }

    public boolean handleMouseScroll(double scrollDeltaX, double scrollDeltaY) {
        if (!open) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (accumulatedScrollX != 0.0D && Math.signum(scrollDeltaX) != Math.signum(accumulatedScrollX)) {
            accumulatedScrollX = 0.0D;
        }
        if (accumulatedScrollY != 0.0D && Math.signum(scrollDeltaY) != Math.signum(accumulatedScrollY)) {
            accumulatedScrollY = 0.0D;
        }

        accumulatedScrollX += scrollDeltaX;
        accumulatedScrollY += scrollDeltaY;
        int wheelX = (int) accumulatedScrollX;
        int wheelY = (int) accumulatedScrollY;
        if (wheelX == 0 && wheelY == 0) {
            return true;
        }

        accumulatedScrollX -= wheelX;
        accumulatedScrollY -= wheelY;
        int wheel = wheelY == 0 ? -wheelX : wheelY;
        if (wheel == 0) {
            return true;
        }

        if (isShiftDown(minecraft)) {
            cycleActiveProfile(-(int) Math.signum(wheel), minecraft);
            return true;
        }

        if (currentPageCount() > 1) {
            int pageDelta = -(int) Math.signum(wheel);
            int lastPage = currentPageCount() - 1;
            currentPageIndex = Math.max(0, Math.min(currentPageIndex + pageDelta, lastPage));
            hoveredIndex = -1;
        }
        return true;
    }

    public boolean handleMouseButton(int button, int action) {
        if (!open || !isRadialTutorialActive() || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (action == GLFW.GLFW_PRESS) {
            handleTutorialClick(tutorialMouseX(minecraft), tutorialMouseY(minecraft), minecraft);
            leftMouseDown = true;
            return true;
        }
        if (action == GLFW.GLFW_RELEASE) {
            leftMouseDown = false;
            return true;
        }
        return false;
    }

    private boolean handleHotkeys(Minecraft minecraft, List<WheelEntry> visibleEntries) {
        pruneReleasedHotkeys(minecraft);
        for (int index = 0; index < visibleEntries.size(); index++) {
            WheelEntry entry = visibleEntries.get(index);
            String hotkey = normalizeHotkeyGlyph(entry.glyph());
            if (hotkey == null || !isHotkeyDown(minecraft, hotkey) || !pressedHotkeys.add(hotkey)) {
                continue;
            }

            activateEntry(index, visibleEntries);
            return true;
        }
        return false;
    }

    private boolean handleRadialPositionKeys(List<WheelEntry> visibleEntries) {
        boolean anyRadialPositionKeyDown = false;
        for (int position = 0; position < OmniWheelKeyMappings.RADIAL_POSITION_KEYS.size(); position++) {
            if (isRadialPositionKeyDown(Minecraft.getInstance(), position)) {
                anyRadialPositionKeyDown = true;
            } else {
                pressedRadialPositionKeys.remove(position);
            }
        }
        if (!anyRadialPositionKeyDown) {
            radialPositionInputLocked = false;
        }
        if (radialPositionInputLocked) {
            return false;
        }

        for (int position = 0; position < OmniWheelKeyMappings.RADIAL_POSITION_KEYS.size(); position++) {
            if (!isRadialPositionKeyDown(Minecraft.getInstance(), position) || !pressedRadialPositionKeys.add(position)) {
                continue;
            }

            if (position == 4) {
                if (canGoBack()) {
                    centerBackHovered = true;
                    hoveredIndex = -1;
                    activateHoveredEntry(visibleEntries);
                } else {
                    close();
                }
                radialPositionInputLocked = true;
                radialPositionModeActive = true;
                return true;
            }

            int targetIndex = radialPositionTargetIndex(position, visibleEntries.size());
            if (targetIndex < 0 || targetIndex >= visibleEntries.size()) {
                radialPositionInputLocked = true;
                radialPositionModeActive = true;
                return true;
            }

            activateEntry(targetIndex, visibleEntries);
            radialPositionInputLocked = true;
            radialPositionModeActive = true;
            return true;
        }
        return false;
    }

    private void cycleActiveProfile(int delta, Minecraft minecraft) {
        runtime.profileManager().ensureLoaded();
        List<WheelProfile> profiles = runtime.profileManager().getProfiles();
        if (profiles.size() <= 1) {
            return;
        }

        String activeProfileId = runtime.activeProfile().id();
        int currentIndex = 0;
        for (int index = 0; index < profiles.size(); index++) {
            if (profiles.get(index).id().equals(activeProfileId)) {
                currentIndex = index;
                break;
            }
        }

        int nextIndex = Math.floorMod(currentIndex + delta, profiles.size());
        WheelProfile nextProfile = profiles.get(nextIndex);
        if (nextProfile.id().equals(activeProfileId)) {
            return;
        }

        runtime.profileManager().setActiveProfile(nextProfile.id());
        openRoot(minecraft);
    }

    private void pruneReleasedHotkeys(Minecraft minecraft) {
        pressedHotkeys.removeIf(hotkey -> !isHotkeyDown(minecraft, hotkey));
    }

    private boolean isHotkeyDown(Minecraft minecraft, String hotkey) {
        for (int keyCode : hotkeyCodes(hotkey)) {
            if (InputConstants.isKeyDown(minecraft.getWindow(), keyCode)) {
                return true;
            }
        }
        return false;
    }

    private boolean isShiftDown(Minecraft minecraft) {
        return InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private void activateEntry(int index, List<WheelEntry> visibleEntries) {
        hoveredIndex = index;
        centerBackHovered = false;
        activateHoveredEntry(visibleEntries, true);
    }

    private void ensureMouseReleased(Minecraft minecraft) {
        if (!mouseReleased || minecraft.mouseHandler.isMouseGrabbed()) {
            releaseMouseAt(minecraft, lastFreeRawMouseX, lastFreeRawMouseY);
            mouseReleased = true;
        }
    }

    private void releaseMouseAt(Minecraft minecraft, double rawX, double rawY) {
        minecraft.mouseHandler.releaseMouse();
        InputConstants.grabOrReleaseMouse(minecraft.getWindow(), 212993, rawX, rawY);
    }

    private void drawEntry(
            GuiGraphicsExtractor graphics,
            Minecraft minecraft,
            WheelEntry entry,
            float centerX,
            float centerY,
            float innerRadius,
            float outerRadius,
            double labelAngle,
            boolean selected
    ) {
        float labelRadius = (innerRadius + outerRadius) * 0.52F;
        int labelX = Math.round(centerX + ((float) Math.cos(labelAngle) * labelRadius));
        int labelY = Math.round(centerY + ((float) Math.sin(labelAngle) * labelRadius));
        int textColor = selected ? TEXT_PRIMARY : entry.color();
        int glyphColor = entry.action() instanceof OpenWheelAction && entry.color() == WheelEntry.DEFAULT_COLOR ? SUBWHEEL_HINT : textColor;
        boolean showIcon = entry.showIcon();
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
            if (!EntryIconRenderer.drawIcon(graphics, minecraft, entry.glyph(), labelX, iconCenterY, iconScale)) {
                int glyphY = Math.round(iconCenterY - (showLabel || showShortcut ? 4.0F : 8.0F));
                graphics.centeredText(minecraft.font, entry.glyph(), labelX, glyphY, glyphColor);
            }
        }
        if (showLabel) {
            String label = fitPlainText(minecraft.font, OmniWheelText.resolve(entry.label()), maxTextWidth);
            graphics.centeredText(minecraft.font, label, labelX, labelTextY, textColor);
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
                    selected ? TEXT_SHORTCUT_SELECTED : TEXT_SECONDARY
            );
        }
    }

    private void drawCenterPanel(GuiGraphicsExtractor graphics, Minecraft minecraft, int width, int height, WheelDefinition wheel, List<WheelEntry> entries) {
        WheelEntry hovered = hoveredIndex >= 0 && hoveredIndex < entries.size() ? entries.get(hoveredIndex) : null;
        int panelRadius = Math.round(DEADZONE_RADIUS + 18.0F);
        int contentWidth = Math.max(72, (panelRadius * 2) - 10);
        int contentHeight = Math.max(46, (panelRadius * 2) - 16);
        int centerX = width / 2;
        int centerY = height / 2;

        List<TextLayout> blocks = new ArrayList<>();
        blocks.add(layoutText(minecraft, hovered != null ? OmniWheelText.resolve(hovered.label()) : OmniWheelText.resolve(activeProfile().displayName()), contentWidth, 30, 2));

        if (canGoBack()) {
            drawCenterBackButton(graphics, minecraft, centerX, centerY, panelRadius);
            return;
        }

        if (currentPageCount() > 1) {
            blocks.add(layoutText(
                    minecraft,
                    OmniWheelText.translate("omniwheel.radial.page", currentPageIndex + 1, currentPageCount()),
                    contentWidth,
                    18,
                    1
            ));
        }

        String hint = centerHint(hovered, entries);
        if (hint != null) {
            blocks.add(layoutText(minecraft, hint, contentWidth, 20, 2));
        }

        int spacing = 4;
        int totalHeight = -spacing;
        for (TextLayout block : blocks) {
            totalHeight += block.height() + spacing;
        }

        int startY = centerY - Math.min(totalHeight, contentHeight) / 2;
        for (int index = 0; index < blocks.size(); index++) {
            TextLayout block = blocks.get(index);
            int color = index == 0 ? TEXT_PRIMARY : index == blocks.size() - 1 && hint != null && hovered != null && hovered.action().requiresConfirmation()
                    ? SUBWHEEL_HINT
                    : TEXT_SECONDARY;
            drawAdaptiveCenteredText(graphics, minecraft, block, centerX, startY, color);
            startY += block.height() + spacing;
        }

    }

    private void suppressGameplayInput(Minecraft minecraft) {
        if (minecraft.player == null) {
            return;
        }
        for (KeyMapping keyMapping : gameplayKeyMappings(minecraft)) {
            keyMapping.setDown(false);
        }
        minecraft.player.xxa = 0.0F;
        minecraft.player.yya = 0.0F;
        minecraft.player.zza = 0.0F;
        minecraft.player.setJumping(false);
        minecraft.player.setSprinting(false);
    }

    private static List<KeyMapping> gameplayKeyMappings(Minecraft minecraft) {
        return List.of(
                minecraft.options.keyUp,
                minecraft.options.keyDown,
                minecraft.options.keyLeft,
                minecraft.options.keyRight,
                minecraft.options.keyJump,
                minecraft.options.keyShift,
                minecraft.options.keySprint,
                minecraft.options.keyAttack,
                minecraft.options.keyUse,
                minecraft.options.keyPickItem
        );
    }

    private void drawAdaptiveCenteredText(GuiGraphicsExtractor graphics, Minecraft minecraft, TextLayout layout, int centerX, int topY, int color) {
        int lineY = topY;
        for (FormattedCharSequence line : layout.lines()) {
            graphics.pose().pushMatrix();
            graphics.pose().scale(layout.scale(), layout.scale());
            graphics.centeredText(
                    minecraft.font,
                    line,
                    Math.round(centerX / layout.scale()),
                    Math.round(lineY / layout.scale()),
                    color
            );
            graphics.pose().popMatrix();
            lineY += layout.lineHeight();
        }
    }

    private TextLayout layoutText(Minecraft minecraft, String text, int maxWidth, int maxHeight, int maxLines) {
        float minScale = 0.55F;
        for (float scale = 1.0F; scale >= minScale; scale -= 0.05F) {
            int scaledWidth = Math.max(1, Math.round(maxWidth / scale));
            List<FormattedCharSequence> lines = minecraft.font.split(Component.literal(text), scaledWidth);
            if (lines.size() > maxLines) {
                continue;
            }

            int lineHeight = Math.max(7, Math.round(9.0F * scale));
            int height = lines.size() * lineHeight;
            if (height <= maxHeight) {
                return new TextLayout(lines, scale, lineHeight, height);
            }
        }

        float fallbackScale = minScale;
        int fallbackWidth = Math.max(1, Math.round(maxWidth / fallbackScale));
        List<FormattedCharSequence> fallbackLines = new ArrayList<>(minecraft.font.split(Component.literal(text), fallbackWidth));
        if (fallbackLines.size() > maxLines) {
            fallbackLines = new ArrayList<>(fallbackLines.subList(0, maxLines));
            if (!fallbackLines.isEmpty()) {
                int maxChars = Math.max(3, Math.round(fallbackWidth / 6.0F));
                String shortened = shortenPlainText(text, maxChars);
                fallbackLines = new ArrayList<>(minecraft.font.split(Component.literal(shortened), fallbackWidth));
                if (fallbackLines.size() > maxLines) {
                    fallbackLines = new ArrayList<>(fallbackLines.subList(0, maxLines));
                }
            }
        }
        int fallbackLineHeight = Math.max(7, Math.round(9.0F * fallbackScale));
        return new TextLayout(fallbackLines, fallbackScale, fallbackLineHeight, fallbackLines.size() * fallbackLineHeight);
    }

    private void drawCenterBackButton(GuiGraphicsExtractor graphics, Minecraft minecraft, int centerX, int centerY, int panelRadius) {
        float buttonRadius = centerBackButtonRadius();
        int fill = centerBackHovered ? SELECTED_COLOR : CENTER_BUTTON_COLOR;
        int outline = centerBackHovered ? SELECTED_OUTLINE : 0;

        GeometryRenderer.fillDisc(graphics, centerX, centerY, buttonRadius, fill);
        if (centerBackHovered) {
            GeometryRenderer.fillRingSegment(
                    graphics,
                    centerX,
                    centerY,
                    buttonRadius - 2.0F,
                    buttonRadius + 1.5F,
                    0.0D,
                    Math.PI * 2.0D,
                    0.0D,
                    Math.PI * 2.0D,
                    outline
            );
        }

        graphics.centeredText(minecraft.font, "\u2190", centerX, centerY - 9, TEXT_PRIMARY);
        graphics.centeredText(minecraft.font, OmniWheelText.translate("omniwheel.common.back"), centerX, centerY + 4, TEXT_PRIMARY);
    }

    private void drawRadialTutorial(GuiGraphicsExtractor graphics, Minecraft minecraft, int width, int height, float centerX, float centerY, float outerRadius) {
        TutorialStep tutorialStep = currentRadialTutorialStep(width, height, centerX, centerY, outerRadius);
        if (tutorialStep == null) {
            return;
        }

        int boxWidth = tutorialStep.width();
        TextLayout title = layoutText(minecraft, tutorialStep.title(), boxWidth - 20, 16, 1);
        List<FormattedCharSequence> bodyLines = minecraft.font.split(Component.literal(tutorialStep.body()), boxWidth - 20);
        int bodyHeight = Math.max(8, bodyLines.size() * 9);
        int buttonHeight = 20;
        int buttonGap = 6;
        boolean lastStep = radialTutorialStepIndex >= RADIAL_TUTORIAL_STEP_COUNT - 1;
        int buttonWidth = (boxWidth - 22 - buttonGap) / 2;
        int boxHeight = 18 + title.height() + 8 + bodyHeight + 12 + buttonHeight + 12;
        int boxX = Math.max(8, Math.min(tutorialStep.x(), Math.max(8, width - boxWidth - 8)));
        int boxY = Math.max(8, Math.min(tutorialStep.y(), Math.max(8, height - boxHeight - 8)));

        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, TUTORIAL_PANEL);
        drawTutorialFrame(graphics, boxX, boxY, boxWidth, boxHeight);
        String stepLabel = (Math.min(radialTutorialStepIndex, RADIAL_TUTORIAL_STEP_COUNT - 1) + 1) + "/" + RADIAL_TUTORIAL_STEP_COUNT;
        graphics.text(minecraft.font, stepLabel, boxX + boxWidth - 10 - minecraft.font.width(stepLabel), boxY + 10, TEXT_SECONDARY, false);
        drawAdaptiveCenteredText(graphics, minecraft, title, boxX + (boxWidth / 2), boxY + 10, TEXT_PRIMARY);
        drawWrappedTutorialBody(graphics, minecraft, bodyLines, boxX + 10, boxY + 18 + title.height(), TEXT_SECONDARY);

        int buttonY = boxY + boxHeight - buttonHeight - 10;
        tutorialNextButtonX = boxX + 8;
        tutorialNextButtonY = buttonY;
        tutorialNextButtonWidth = lastStep ? boxWidth - 16 : buttonWidth;
        tutorialNextButtonHeight = buttonHeight;
        tutorialSkipAllButtonX = tutorialNextButtonX + buttonWidth + buttonGap;
        tutorialSkipAllButtonY = buttonY;
        tutorialSkipAllButtonWidth = lastStep ? 0 : buttonWidth;
        tutorialSkipAllButtonHeight = buttonHeight;

        drawTutorialButton(
                graphics,
                minecraft,
                tutorialNextButtonX,
                tutorialNextButtonY,
                tutorialNextButtonWidth,
                tutorialNextButtonHeight,
                OmniWheelText.translate(lastStep ? "omniwheel.common.finish" : "omniwheel.common.next")
        );
        if (!lastStep) {
            drawTutorialButton(
                    graphics,
                    minecraft,
                    tutorialSkipAllButtonX,
                    tutorialSkipAllButtonY,
                    tutorialSkipAllButtonWidth,
                    tutorialSkipAllButtonHeight,
                    OmniWheelText.translate("omniwheel.common.skip_all")
            );
        }
    }

    private void drawWrappedTutorialBody(GuiGraphicsExtractor graphics, Minecraft minecraft, List<FormattedCharSequence> bodyLines, int x, int y, int color) {
        int lineY = y;
        for (FormattedCharSequence line : bodyLines) {
            graphics.text(minecraft.font, line, x, lineY, color, false);
            lineY += 9;
        }
    }

    private void drawTutorialFrame(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + 1, TUTORIAL_EDGE);
        graphics.fill(x, y + height - 1, x + width, y + height, TUTORIAL_EDGE);
        graphics.fill(x, y, x + 1, y + height, TUTORIAL_EDGE);
        graphics.fill(x + width - 1, y, x + width, y + height, TUTORIAL_EDGE);
    }

    private void drawTutorialButton(GuiGraphicsExtractor graphics, Minecraft minecraft, int x, int y, int width, int height, String label) {
        boolean highlighted = contains(tutorialMouseX(minecraft), tutorialMouseY(minecraft), x, y, width, height);
        int fill = highlighted ? TUTORIAL_BUTTON_HIGHLIGHT : TUTORIAL_BUTTON;
        graphics.fill(x, y, x + width, y + height, fill);
        drawTutorialFrame(graphics, x, y, width, height);
        if (highlighted) {
            graphics.fill(x, y, x + width, y + 1, SELECTED_OUTLINE);
            graphics.fill(x, y + height - 1, x + width, y + height, SELECTED_OUTLINE);
            graphics.fill(x, y, x + 1, y + height, SELECTED_OUTLINE);
            graphics.fill(x + width - 1, y, x + width, y + height, SELECTED_OUTLINE);
        }
        graphics.centeredText(minecraft.font, label, x + (width / 2), y + 6, TEXT_PRIMARY);
    }

    private TutorialStep currentRadialTutorialStep(int width, int height, float centerX, float centerY, float outerRadius) {
        if (radialTutorialStepIndex < 0 || runtime.tutorialProgressStore().isCompleted(TutorialType.RADIAL)) {
            return null;
        }

        Font font = Minecraft.getInstance().font;
        String title;
        String body;
        int cardWidth;
        return switch (Math.min(radialTutorialStepIndex, 4)) {
            case 0 -> {
                title = OmniWheelText.translate("omniwheel.tutorial.radial.menu.title");
                body = OmniWheelText.translate("omniwheel.tutorial.radial.menu.body");
                cardWidth = tutorialCardWidth(font, title, body, 176, 248);
                yield new TutorialStep(title, body, 16, 18, cardWidth);
            }
            case 1 -> {
                title = OmniWheelText.translate("omniwheel.tutorial.radial.direction.title");
                body = OmniWheelText.translate("omniwheel.tutorial.radial.direction.body");
                cardWidth = tutorialCardWidth(font, title, body, 184, 264);
                yield new TutorialStep(title, body, width - cardWidth - 16, 18, cardWidth);
            }
            case 2 -> {
                title = OmniWheelText.translate("omniwheel.tutorial.radial.submenus.title");
                body = OmniWheelText.translate("omniwheel.tutorial.radial.submenus.body");
                cardWidth = tutorialCardWidth(font, title, body, 184, 256);
                yield new TutorialStep(title, body, width - cardWidth - 16, height - 116, cardWidth);
            }
            case 3 -> {
                title = OmniWheelText.translate("omniwheel.tutorial.radial.center.title");
                body = OmniWheelText.translate("omniwheel.tutorial.radial.center.body");
                cardWidth = tutorialCardWidth(font, title, body, 184, 256);
                yield new TutorialStep(title, body, 16, 18, cardWidth);
            }
            default -> {
                title = OmniWheelText.translate("omniwheel.tutorial.radial.numpad.title");
                body = OmniWheelText.translate("omniwheel.tutorial.radial.numpad.body");
                cardWidth = tutorialCardWidth(font, title, body, 184, 252);
                yield new TutorialStep(title, body, 16, height - 116, cardWidth);
            }
        };
    }

    private boolean handleTutorialClick(float mouseX, float mouseY, Minecraft minecraft) {
        int width = uiWidth(minecraft);
        int height = uiHeight(minecraft);
        if (currentRadialTutorialStep(width, height,
                width * 0.5F,
                height * 0.5F,
                WHEEL_RADIUS) == null) {
            return false;
        }
        int button = tutorialButtonAt(mouseX, mouseY);
        if (button == 1) {
            advanceRadialTutorial();
            return true;
        }
        if (button == 2) {
            completeRadialTutorial();
            return true;
        }
        return isRadialTutorialActive();
    }

    private int tutorialButtonAt(float mouseX, float mouseY) {
        if (contains(mouseX, mouseY, tutorialNextButtonX, tutorialNextButtonY, tutorialNextButtonWidth, tutorialNextButtonHeight)) {
            return 1;
        }
        if (contains(mouseX, mouseY, tutorialSkipAllButtonX, tutorialSkipAllButtonY, tutorialSkipAllButtonWidth, tutorialSkipAllButtonHeight)) {
            return 2;
        }
        return 0;
    }

    private void advanceRadialTutorial() {
        if (radialTutorialStepIndex < 0) {
            return;
        }
        radialTutorialStepIndex++;
        if (radialTutorialStepIndex >= 5) {
            completeRadialTutorial();
            return;
        }
        runtime.tutorialProgressStore().setCurrentStep(TutorialType.RADIAL, radialTutorialStepIndex);
    }

    private void completeRadialTutorial() {
        Minecraft minecraft = Minecraft.getInstance();
        List<String> currentPath = List.copyOf(wheelPath);
        runtime.tutorialProgressStore().markCompleted(TutorialType.RADIAL);
        radialTutorialStepIndex = -1;
        if (minecraft.player != null && minecraft.level != null) {
            keyDown = OmniWheelKeyMappings.OPEN_WHEEL.isDown();
            openWheelPath(currentPath);
        }
    }

    private boolean isRadialTutorialActive() {
        return radialTutorialStepIndex >= 0 && !runtime.tutorialProgressStore().isCompleted(TutorialType.RADIAL);
    }

    private String centerHint(WheelEntry hovered, List<WheelEntry> entries) {
        if (hovered == null) {
            if (entries.isEmpty()) {
                return OmniWheelText.translate("omniwheel.radial.no_actions");
            }
            return null;
        }
        if (hovered.action().requiresConfirmation()) {
            return OmniWheelText.translate("omniwheel.radial.release_to_confirm");
        }
        return null;
    }

    private void activateHoveredEntry(List<WheelEntry> visibleEntries) {
        activateHoveredEntry(visibleEntries, true);
    }

    private void activateHoveredEntry(List<WheelEntry> visibleEntries, boolean closeOnEmpty) {
        if (centerBackHovered && canGoBack()) {
            popWheel();
            return;
        }

        if (hoveredIndex < 0 || hoveredIndex >= visibleEntries.size()) {
            if (closeOnEmpty) {
                close();
            }
            return;
        }

        WheelEntry entry = visibleEntries.get(hoveredIndex);
        if (entry.action() instanceof OpenWheelAction openWheelAction) {
            if (isWheelAvailable(openWheelAction.wheelId())) {
                navigateToWheel(openWheelAction.wheelId());
                hoveredIndex = -1;
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                minecraft.player.sendOverlayMessage(OmniWheelText.component("omniwheel.message.missing_wheel", openWheelAction.wheelId()));
            }
            close();
            return;
        }

        if (entry.action() instanceof OpenScreenAction openScreenAction) {
            runtime.openTargetScreen(openScreenAction.target(), null);
            return;
        }

        if (entry.action() instanceof FunctionAction functionAction && handleOmniWheelFunction(functionAction.function())) {
            return;
        }

        runtime.actionExecutor().execute(entry.action());
        close();
    }

    private boolean handleOmniWheelFunction(GameplayFunction function) {
        Minecraft minecraft = Minecraft.getInstance();
        switch (function) {
            case OMNI_BACK -> {
                if (!popWheel()) {
                    close();
                }
                return true;
            }
            case OMNI_OPEN_PROFILES -> {
                navigateToWheel(WheelAvailabilityResolver.PROFILES_WHEEL_ID);
                hoveredIndex = -1;
                return true;
            }
            case OMNI_OPEN_MANAGER -> {
                runtime.openTargetScreen(ScreenTarget.PROFILE_MANAGER, null);
                return true;
            }
            case OMNI_NEXT_PROFILE -> {
                cycleActiveProfile(1, minecraft);
                hoveredIndex = -1;
                return true;
            }
            case OMNI_PREVIOUS_PROFILE -> {
                cycleActiveProfile(-1, minecraft);
                hoveredIndex = -1;
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private boolean popWheel() {
        if (wheelPath.size() <= 1) {
            return false;
        }

        wheelPath.removeLast();
        hoveredIndex = -1;
        centerBackHovered = false;
        currentPageIndex = 0;
        return true;
    }

    private void navigateToWheel(String wheelId) {
        if (wheelPath.isEmpty()) {
            wheelPath.addLast(wheelId);
            currentPageIndex = 0;
            return;
        }

        if (wheelId.equals(wheelPath.getLast())) {
            return;
        }

        while (wheelPath.size() > 1 && wheelPath.contains(wheelId) && !wheelId.equals(wheelPath.getLast())) {
            wheelPath.removeLast();
        }

        if (!wheelId.equals(wheelPath.getLast())) {
            wheelPath.addLast(wheelId);
        }
        centerBackHovered = false;
        currentPageIndex = 0;
    }

    private void updateSelection(float mouseX, float mouseY, Minecraft minecraft) {
        if (!open) {
            hoveredIndex = -1;
            centerBackHovered = false;
            return;
        }

        List<WheelEntry> visibleEntries = currentDisplayEntries();
        int segmentCount = renderedSegmentCount(visibleEntries);
        centerBackHovered = canGoBack() && isMouseInCenterBackButton(mouseX, mouseY, minecraft);

        if (segmentCount == 0 || centerBackHovered) {
            hoveredIndex = -1;
        } else {
            hoveredIndex = RadialLayout.pickDirectionalSegment(
                    mouseX,
                    mouseY,
                    uiWidth(minecraft) * 0.5F,
                    uiHeight(minecraft) * 0.5F,
                    radialSelectionRadius(),
                    segmentCount
            );
            if (hoveredIndex >= visibleEntries.size()) {
                hoveredIndex = -1;
            }
        }
    }

    private boolean canGoBack() {
        return wheelPath.size() > 1;
    }

    private boolean isMouseInCenterBackButton(float mouseX, float mouseY, Minecraft minecraft) {
        float centerX = uiWidth(minecraft) * 0.5F;
        float centerY = uiHeight(minecraft) * 0.5F;
        float dx = mouseX - centerX;
        float dy = mouseY - centerY;
        float radius = centerBackButtonRadius();
        return (dx * dx) + (dy * dy) <= (radius * radius);
    }

    private float centerBackButtonRadius() {
        return Math.max(20.0F, DEADZONE_RADIUS - 6.0F);
    }

    private float radialSelectionRadius() {
        if (canGoBack()) {
            return centerBackButtonRadius() + 3.0F;
        }
        float configuredDeadzone = DEADZONE_RADIUS;
        return Math.max(14.0F, Math.min(24.0F, configuredDeadzone * 0.35F));
    }

    private String resolveOpeningWheelId(WheelProfile profile) {
        String rootWheelId = profile.rootWheelId();
        if (!runtime.availabilityResolver().visibleEntries(profile, rootWheelId, new ArrayDeque<>(List.of(rootWheelId))).isEmpty()) {
            return rootWheelId;
        }

        for (WheelDefinition wheel : profile.wheels().values()) {
            if (!wheel.active() || wheel.id().equals(rootWheelId)) {
                continue;
            }
            if (!runtime.availabilityResolver().visibleEntries(profile, wheel.id(), new ArrayDeque<>(List.of(wheel.id()))).isEmpty()) {
                return wheel.id();
            }
        }

        return rootWheelId;
    }

    private WheelDefinition currentWheel() {
        String wheelId = wheelPath.getLast();
        if (WheelAvailabilityResolver.PROFILES_WHEEL_ID.equals(wheelId)) {
            return new WheelDefinition(
                    wheelId,
                    "omniwheel.profiles_wheel.title",
                    "omniwheel.profiles_wheel.description",
                    8,
                    currentVisibleEntries()
            );
        }
        return activeProfile().wheel(wheelId);
    }

    private WheelProfile activeProfile() {
        return runtime.activeProfile();
    }

    private List<WheelEntry> currentVisibleEntries() {
        return runtime.availabilityResolver().visibleEntries(activeProfile(), wheelPath.getLast(), wheelPath);
    }

    private List<WheelEntry> currentDisplayEntries() {
        List<WheelEntry> visibleEntries = currentVisibleEntries();
        if (visibleEntries.isEmpty()) {
            currentPageIndex = 0;
            return visibleEntries;
        }

        int pageCount = pageCount(visibleEntries);
        currentPageIndex = Math.max(0, Math.min(currentPageIndex, pageCount - 1));
        int startIndex = currentPageIndex * MAX_RENDERED_SEGMENTS;
        int endIndex = Math.min(visibleEntries.size(), startIndex + MAX_RENDERED_SEGMENTS);
        return List.copyOf(visibleEntries.subList(startIndex, endIndex));
    }

    private static int renderedSegmentCount(List<WheelEntry> visibleEntries) {
        return Math.min(MAX_RENDERED_SEGMENTS, visibleEntries.size());
    }

    private int currentPageCount() {
        return pageCount(currentVisibleEntries());
    }

    private static int pageCount(List<WheelEntry> visibleEntries) {
        return Math.max(1, (visibleEntries.size() + MAX_RENDERED_SEGMENTS - 1) / MAX_RENDERED_SEGMENTS);
    }

    private int selectedHotbarSlot(Inventory inventory) {
        for (String methodName : List.of("getSelectedSlot", "getSelected")) {
            try {
                Method method = inventory.getClass().getMethod(methodName);
                Object value = method.invoke(inventory);
                if (value instanceof Number number) {
                    return number.intValue();
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }

        try {
            var field = inventory.getClass().getField("selected");
            return field.getInt(inventory);
        } catch (ReflectiveOperationException ignored) {
        }
        return 0;
    }

    private void setSelectedHotbarSlot(Inventory inventory, int selectedSlot) {
        for (String methodName : List.of("setSelectedSlot", "setSelected")) {
            try {
                Method method = inventory.getClass().getMethod(methodName, int.class);
                method.invoke(inventory, selectedSlot);
                return;
            } catch (ReflectiveOperationException ignored) {
            }
        }

        try {
            var field = inventory.getClass().getField("selected");
            field.setInt(inventory, selectedSlot);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private boolean consumePageScroll(Minecraft minecraft) {
        if (minecraft.player == null || lockedHotbarSlot < 0) {
            return false;
        }

        Inventory inventory = minecraft.player.getInventory();
        int selectedSlot = selectedHotbarSlot(inventory);
        if (selectedSlot == lockedHotbarSlot) {
            return false;
        }

        setSelectedHotbarSlot(inventory, lockedHotbarSlot);
        int normalizedDelta = selectedSlot - lockedHotbarSlot;
        if (normalizedDelta > 4) {
            normalizedDelta -= 9;
        } else if (normalizedDelta < -4) {
            normalizedDelta += 9;
        }

        if (Math.abs(normalizedDelta) != 1 || currentPageCount() <= 1) {
            return false;
        }

        int nextPage = currentPageIndex + normalizedDelta;
        int lastPage = currentPageCount() - 1;
        currentPageIndex = Math.max(0, Math.min(nextPage, lastPage));
        hoveredIndex = -1;
        return true;
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

    private String buildPathLabel() {
        List<String> titles = new ArrayList<>();
        for (String wheelId : wheelPath) {
            if (WheelAvailabilityResolver.PROFILES_WHEEL_ID.equals(wheelId)) {
                titles.add(OmniWheelText.translate("omniwheel.profiles_wheel.title"));
            } else {
                titles.add(OmniWheelText.resolve(activeProfile().wheel(wheelId).title()));
            }
        }
        return String.join(" > ", titles);
    }

    private boolean isWheelAvailable(String wheelId) {
        return WheelAvailabilityResolver.PROFILES_WHEEL_ID.equals(wheelId) || activeProfile().wheels().containsKey(wheelId);
    }

    private static float currentMouseX(Minecraft minecraft) {
        return scaleRawX(minecraft, minecraft.mouseHandler.xpos());
    }

    private static float currentMouseY(Minecraft minecraft) {
        return scaleRawY(minecraft, minecraft.mouseHandler.ypos());
    }

    private float tutorialMouseX(Minecraft minecraft) {
        return freeMouseX(minecraft);
    }

    private float tutorialMouseY(Minecraft minecraft) {
        return freeMouseY(minecraft);
    }

    private float freeMouseX(Minecraft minecraft) {
        return scaleRawX(minecraft, lastFreeRawMouseX);
    }

    private float freeMouseY(Minecraft minecraft) {
        return scaleRawY(minecraft, lastFreeRawMouseY);
    }

    private static float scaleRawX(Minecraft minecraft, double rawX) {
        double guiX = rawX * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth();
        return (float) (guiX / uiScale(minecraft));
    }

    private static float scaleRawY(Minecraft minecraft, double rawY) {
        double guiY = rawY * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getScreenHeight();
        return (float) (guiY / uiScale(minecraft));
    }

    private static int uiWidth(Minecraft minecraft) {
        return Math.max(1, (int) Math.round(minecraft.getWindow().getGuiScaledWidth() / uiScale(minecraft)));
    }

    private static int uiHeight(Minecraft minecraft) {
        return Math.max(1, (int) Math.round(minecraft.getWindow().getGuiScaledHeight() / uiScale(minecraft)));
    }

    private static double uiScale(Minecraft minecraft) {
        double widthScale = minecraft.getWindow().getGuiScaledWidth() / (double) REFERENCE_UI_WIDTH;
        double heightScale = minecraft.getWindow().getGuiScaledHeight() / (double) REFERENCE_UI_HEIGHT;
        return Math.max(0.05D, Math.min(widthScale, heightScale));
    }

    private static boolean isLeftMouseDown(Minecraft minecraft) {
        return GLFW.glfwGetMouseButton(minecraft.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

    private static boolean isRightMouseDown(Minecraft minecraft) {
        return GLFW.glfwGetMouseButton(minecraft.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
    }

    private static String shorten(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private static String shortenPlainText(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(0, maxLength - 3)) + "...";
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
            GuiGraphicsExtractor graphics,
            Font font,
            String text,
            int centerX,
            int topY,
            float scale,
            int color
    ) {
        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);
        graphics.centeredText(
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
                    SEGMENT_COLOR,
                    SEGMENT_ALT_COLOR,
                    SELECTED_COLOR,
                    SELECTED_OUTLINE
            );
        };
    }

    private static String normalizeHotkeyGlyph(String glyph) {
        if (EntryIconRenderer.isItemIcon(glyph)) {
            return null;
        }
        if (glyph.length() != 1) {
            return null;
        }

        char character = Character.toUpperCase(glyph.charAt(0));
        if ((character >= 'A' && character <= 'Z') || (character >= '0' && character <= '9')) {
            return Character.toString(character);
        }
        return null;
    }

    private static int[] hotkeyCodes(String hotkey) {
        char character = hotkey.charAt(0);
        if (character >= 'A' && character <= 'Z') {
            return new int[]{GLFW.GLFW_KEY_A + (character - 'A')};
        }
        if (character >= '1' && character <= '9') {
            int offset = character - '1';
            return new int[]{GLFW.GLFW_KEY_1 + offset, GLFW.GLFW_KEY_KP_1 + offset};
        }
        if (character == '0') {
            return new int[]{GLFW.GLFW_KEY_0, GLFW.GLFW_KEY_KP_0};
        }
        return new int[0];
    }

    private static int tutorialCardWidth(Font font, String title, String body, int minWidth, int maxWidth) {
        int buttonFloorWidth = 166;
        int titleWidth = font.width(title) + 20;
        int bodyWidth = font.width(body) + 20;
        int widestTokenWidth = 0;
        for (String token : body.split("\\s+")) {
            widestTokenWidth = Math.max(widestTokenWidth, font.width(token) + 28);
        }
        int minimum = Math.max(buttonFloorWidth, Math.min(minWidth, Math.max(titleWidth, widestTokenWidth)));
        int preferred = Math.max(titleWidth, Math.max(widestTokenWidth, Math.min(maxWidth, bodyWidth)));
        return Math.max(minimum, Math.min(maxWidth, preferred));
    }

    private static boolean contains(float mouseX, float mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private static boolean isRadialPositionKeyDown(Minecraft minecraft, int position) {
        if (position < 0 || position >= OmniWheelKeyMappings.RADIAL_POSITION_KEYS.size()) {
            return false;
        }

        var window = minecraft.getWindow();
        InputConstants.Key key = OmniWheelKeyMappings.currentKey(OmniWheelKeyMappings.RADIAL_POSITION_KEYS.get(position));
        long windowHandle = window.handle();
        return switch (key.getType()) {
            case KEYSYM -> InputConstants.isKeyDown(window, key.getValue());
            case SCANCODE -> GLFW.glfwGetKey(windowHandle, key.getValue()) == GLFW.GLFW_PRESS;
            case MOUSE -> GLFW.glfwGetMouseButton(windowHandle, key.getValue()) == GLFW.GLFW_PRESS;
            default -> OmniWheelKeyMappings.RADIAL_POSITION_KEYS.get(position).isDown();
        };
    }

    private static int radialPositionTargetIndex(int positionIndex, int segmentCount) {
        if (segmentCount <= 0 || positionIndex < 0 || positionIndex >= 9 || positionIndex == 4) {
            return -1;
        }

        DirectionVector desiredDirection = switch (positionIndex) {
            case 0 -> new DirectionVector(-1.0D, -1.0D);
            case 1 -> new DirectionVector(0.0D, -1.0D);
            case 2 -> new DirectionVector(1.0D, -1.0D);
            case 3 -> new DirectionVector(-1.0D, 0.0D);
            case 5 -> new DirectionVector(1.0D, 0.0D);
            case 6 -> new DirectionVector(-1.0D, 1.0D);
            case 7 -> new DirectionVector(0.0D, 1.0D);
            case 8 -> new DirectionVector(1.0D, 1.0D);
            default -> null;
        };
        if (desiredDirection == null) {
            return -1;
        }

        double bestScore = Double.NEGATIVE_INFINITY;
        int bestIndex = -1;
        for (int index = 0; index < segmentCount; index++) {
            double angle = RadialLayout.segmentMidAngle(index, segmentCount);
            double score = desiredDirection.dot(Math.cos(angle), Math.sin(angle));
            if (score > bestScore) {
                bestScore = score;
                bestIndex = index;
            }
        }
        return bestIndex;
    }

    private record TextLayout(List<FormattedCharSequence> lines, float scale, int lineHeight, int height) {
    }

    private record SegmentShape(double innerStartAngle, double innerEndAngle, double outerStartAngle, double outerEndAngle, double midAngle) {
    }

    private record SegmentPalette(int baseFill, int altFill, int selectedFill, int selectedOutline) {
    }

    private record DirectionVector(double x, double y) {
        private double dot(double otherX, double otherY) {
            double length = Math.hypot(x, y);
            if (length <= 0.0D) {
                return Double.NEGATIVE_INFINITY;
            }
            return ((x / length) * otherX) + ((y / length) * otherY);
        }
    }

    private record TutorialStep(String title, String body, int x, int y, int width) {
    }
}
