package de.artemis.omniwheel.client.overlay;

import de.artemis.omniwheel.client.render.GeometryRenderer;
import de.artemis.omniwheel.client.render.RadialAnimation;
import de.artemis.omniwheel.client.render.RadialLayout;
import de.artemis.omniwheel.client.render.RadialWheelRenderer;
import de.artemis.omniwheel.common.OmniWheelText;
import de.artemis.omniwheel.common.action.OpenWheelAction;
import de.artemis.omniwheel.common.profile.WheelProfile;
import de.artemis.omniwheel.common.wheel.WheelDefinition;
import de.artemis.omniwheel.common.wheel.WheelEntry;
import de.artemis.omniwheel.common.wheel.WheelEntryMover;
import de.artemis.omniwheel.common.wheel.WheelLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.LongSupplier;

/** Editing-only wheel interaction. Navigation never executes an entry action or commits a pending move. */
public final class RadialWheelPreview {
    public interface Host {
        WheelProfile profile();
        void select(String wheelId, String entryId);
        void closePreview();
        void save(WheelProfile profile, String wheelId, String entryId);
        void button(GuiGraphicsExtractor graphics, Minecraft minecraft, int x, int y, int width, int height,
                    String label, boolean enabled, Runnable action, String id);
        void target(int x, int y, int width, int height, String id, Runnable action);
        boolean focused(String id);
        default boolean[] enabledEntries(String wheel, List<WheelEntry> entries) {
            boolean[] result = new boolean[entries.size()];
            for (int index = 0; index < result.length; index++) result[index] = profile().wheel(wheel).active() && entries.get(index).active();
            return result;
        }
        void draggedEntry(GuiGraphicsExtractor graphics, Minecraft minecraft, WheelEntry entry,
                          String owner, int x, int y, int width);
    }

    private static final int PAGE_SIZE = WheelLayout.PAGE_SIZE;
    private static final int PAGE_BUTTON_WIDTH = 48;
    private static final int PAGE_BUTTON_HEIGHT = 64;
    private static final long NAVIGATION_HOVER_NANOS = 650_000_000L;
    private static final long PAGE_HOVER_NANOS = 500_000_000L;
    private final Host host;
    private final LongSupplier clock;
    private final RadialAnimation animation = new RadialAnimation();
    private final Deque<View> history = new ArrayDeque<>();
    private final Map<String, Double> segmentAngles = new HashMap<>();
    private String motionView;
    private long lastMotionFrame;
    private String profileId, wheelId, lastOwner, lastSelection;
    private int page;
    private float centerX, centerY, scale = 1;
    private int areaX, areaY, areaWidth, areaHeight;
    private String draggedId, draggedWheel;
    private int sourceIndex, gapIndex, targetIndex;
    private String targetWheel, hoverNavigation;
    private String hoveredSubmenuId;
    private float submenuHoverX, submenuHoverY;
    private boolean appendDrop, dragging, validDrop;
    private boolean backDropHovered;
    private float startX, startY;
    private long hoverSince;
    private String hoverKey;
    private int highlightedSlot = -1;
    private int previousX, nextX, pagingY;
    private int backX, backY;

    public RadialWheelPreview(Host host) { this(host, System::nanoTime); }
    RadialWheelPreview(Host host, LongSupplier clock) { this.host = host; this.clock = clock; }
    private static String text(String key, Object... values) { return OmniWheelText.translate(key, values); }

    public void sync(String owner, String selection) {
        WheelProfile profile = host.profile();
        if (profile == null) { cancelDrag(); return; }
        if (!Objects.equals(profileId, profile.id())) {
            cancelDrag();
            profileId = profile.id();
            wheelId = owner;
            page = 0;
            history.clear();
            lastOwner = null;
            lastSelection = null;
        }
        if (!Objects.equals(owner, lastOwner)) {
            cancelDrag();
            if (!Objects.equals(wheelId, owner)) history.clear();
            wheelId = owner;
            page = 0;
            lastOwner = owner;
        }
        if (wheelId == null || !profile.wheels().containsKey(wheelId)) wheelId = profile.rootWheelId();
        if (draggedId != null) {
            WheelDefinition source = profile.wheels().get(draggedWheel);
            sourceIndex = source == null ? -1 : indexOf(source.radialEntries(), draggedId);
            if (sourceIndex < 0) cancelDrag();
        }
        if (!Objects.equals(selection, lastSelection) && !dragging) {
            int selected = indexOf(wheel().radialEntries(), selection);
            if (selected >= 0) page = selected / PAGE_SIZE;
            lastSelection = selection;
        }
        page = Math.clamp(page, 0, pageCount() - 1);
    }

    private WheelDefinition wheel() {
        WheelProfile profile = host.profile();
        return profile == null || wheelId == null ? null : profile.wheels().get(wheelId);
    }

    private WheelEntry draggedEntry() {
        WheelProfile profile = host.profile();
        WheelDefinition source = profile == null || draggedWheel == null ? null : profile.wheels().get(draggedWheel);
        if (source == null) return null;
        int index = indexOf(source.radialEntries(), draggedId);
        return index < 0 ? null : source.radialEntries().get(index);
    }

    private static int indexOf(List<WheelEntry> entries, String id) {
        for (int index = 0; index < entries.size(); index++) if (entries.get(index).id().equals(id)) return index;
        return -1;
    }

    private boolean canMoveTo(String destination) {
        return WheelEntryMover.canMove(host.profile(), draggedWheel, draggedId, destination);
    }

    private int displayCount() {
        if (wheel() == null) return 0;
        return wheel().radialEntries().size() + (dragging && !wheelId.equals(draggedWheel) && canMoveTo(wheelId) ? 1 : 0);
    }

    private int pageCount() { return WheelLayout.pageCount(displayCount()); }

    void drawTutorialSpotlight(GuiGraphicsExtractor graphics, int width, int height) {
        de.artemis.omniwheel.client.tutorial.TutorialSpotlight.drawCircle(graphics, width, height,
                centerX, centerY, RadialWheelRenderer.OUTER_RADIUS * scale + 8);
    }

    // Kept separate from rendering so hit tests and hover navigation share one layout.
    void layout(int x, int y, int width, int height) {
        areaX = x; areaY = y; areaWidth = width; areaHeight = height;
        centerX = x + width * .5F;
        centerY = y + height * .5F + 2;
        scale = Math.max(.5F, Math.min(1.35F, Math.min((width - 24) / 256F, (height - 136) / 256F)));
        pagingY = Math.round(centerY) - PAGE_BUTTON_HEIGHT / 2;
        int pageDistance = (int) Math.ceil(RadialWheelRenderer.OUTER_RADIUS * scale + 22);
        previousX = Math.round(centerX) - pageDistance - PAGE_BUTTON_WIDTH;
        nextX = Math.round(centerX) + pageDistance;
        backX = x + width + 7 - 52;
        backY = y - 25;
    }

    public void draw(GuiGraphicsExtractor graphics, Minecraft minecraft, int x, int y, int width, int height,
                     float mouseX, float mouseY) {
        if (wheel() == null) return;
        layout(x, y, width, height);
        update(mouseX, mouseY, true);
        host.button(graphics, minecraft, backX, backY, 52, 22, text("omniwheel.common.back"), true, this::back, "preview-back");
        drawNavigationTarget(graphics, backX, backY, 52);

        List<WheelEntry> ordered = projectedEntries();
        List<WheelEntry> visible = WheelLayout.page(ordered, page);
        int placeholder = dragging && canMoveTo(wheelId) && gapIndex / PAGE_SIZE == page ? gapIndex % PAGE_SIZE : -1;
        int selected = dragging ? highlightedSlot : visibleSlotAt(mouseX, mouseY, ordered);
        boolean centerHovered = Math.hypot(mouseX - centerX, mouseY - centerY) < RadialWheelRenderer.INNER_RADIUS * scale;
        if (!dragging && selected < 0 && !centerHovered) {
            for (int index = 0; index < visible.size(); index++) {
                if (host.focused("preview-entry:" + visible.get(index).id())) selected = index;
            }
        }
        animation.update(clock.getAsLong(), profileId + "/" + wheelId + "/" + page,
                visible.stream().map(WheelEntry::id).toList(), dragging ? -1 : selected,
                centerHovered || !dragging && selected < 0 && host.focused("preview-center-back"));
        float[] selections = new float[visible.size()];
        for (int index = 0; index < selections.length; index++) selections[index] = dragging ? 0 : animation.selection(index);
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY);
        float visualScale = scale * (dragging ? 1 : animation.viewScale(clock.getAsLong()));
        graphics.pose().scale(visualScale, visualScale);
        double[] offsets = animatedAngles(visible, placeholder);
        RadialWheelRenderer.render(graphics, minecraft, visible, selections, 0, 0,
                RadialWheelRenderer.INNER_RADIUS, RadialWheelRenderer.OUTER_RADIUS, placeholder,
                offsets, dragging && appendDrop ? highlightedSlot : -1, validDrop, host.enabledEntries(wheelId, visible));
        if (dragging && highlightedSlot >= 0 && appendDrop) drawSubmenuTarget(graphics, highlightedSlot, visible.size(), offsets[highlightedSlot]);
        if (placeholder >= 0 && placeholder < visible.size()) drawPlaceholderOutline(graphics, placeholder, visible.size(), offsets[placeholder]);
        String centerLabel = selected >= 0 && selected < visible.size() ? OmniWheelText.resolve(visible.get(selected).label()) : OmniWheelText.resolve(wheel().title());
        String parent = parentWheel();
        boolean centerBack = parent != null && (!dragging || !wheel().radialEntries().isEmpty());
        if (centerBack) {
            boolean hovered = centerHovered || !dragging && selected < 0 && host.focused("preview-center-back");
            float centerHover = animation.centerSelection();
            float centerRadius = 41 + 1.5F * centerHover;
            GeometryRenderer.fillCircle(graphics, 0, 0, centerRadius,
                    RadialAnimation.blendColor(0xD8202933, 0xFF344657, centerHover));
            GeometryRenderer.fillRingSegment(graphics, 0, 0, centerRadius - 1, centerRadius, 0, Math.PI * 2,
                    RadialAnimation.blendColor(0xFF526575, 0xFF8EE1FF, centerHover));
            centerLabel = "< " + text("omniwheel.common.back");
            if (dragging && hovered && Objects.equals(parent, hoverKey)) {
                double progress = Math.clamp((clock.getAsLong() - hoverSince) / (double) NAVIGATION_HOVER_NANOS, 0, 1);
                GeometryRenderer.fillRingSegment(graphics, 0, 0, 42, 44, -Math.PI / 2,
                        -Math.PI / 2 + Math.PI * 2 * progress, 0xFF8EE1FF);
            }
        }
        graphics.centeredText(minecraft.font, fit(minecraft, centerLabel, 82), 0, -9, 0xFFF2F5F8);
        String centerHint = centerBack ? OmniWheelText.resolve(host.profile().wheel(parent).title())
                : dragging && appendDrop && highlightedSlot >= 0
                ? text(validDrop ? "omniwheel.manager.preview.drop_inside" : "omniwheel.manager.preview.cannot_move")
                : text("omniwheel.radial.page", page + 1, pageCount());
        graphics.centeredText(minecraft.font, fit(minecraft, centerHint, 82), 0, 5, 0xFF9CAAB8);
        if (visible.isEmpty()) graphics.centeredText(minecraft.font, text("omniwheel.manager.preview.empty"), 0, 22, 0xFF9CAAB8);
        graphics.pose().popMatrix();
        if (centerBack) {
            int size = Math.round(60 * scale);
            host.target(Math.round(centerX) - size / 2, Math.round(centerY) - size / 2,
                    size, size, "preview-center-back", this::back);
        }
        for (int index = 0; index < visible.size(); index++) {
            if (index == placeholder) continue;
            WheelEntry entry = visible.get(index);
            double angle = RadialLayout.segmentMidAngle(index, visible.size());
            int tx = Math.round(centerX + (float) Math.cos(angle) * 85 * scale);
            int ty = Math.round(centerY + (float) Math.sin(angle) * 85 * scale);
            host.target(tx - 24, ty - 15, 48, 30, "preview-entry:" + entry.id(), () -> {
                if (entry.action() instanceof OpenWheelAction submenu) navigate(submenu.wheelId(), true);
                else host.select(wheelId, entry.id());
            });
        }
        drawPageButton(graphics, previousX, -1, page > 0, mouseX, mouseY);
        drawPageButton(graphics, nextX, 1, page + 1 < pageCount(), mouseX, mouseY);
        if (dragging) drawDraggedEntry(graphics, minecraft, mouseX, mouseY);
    }

    private List<WheelEntry> projectedEntries() {
        List<WheelEntry> entries = wheel().radialEntries();
        WheelEntry moved = draggedEntry();
        if (!dragging || moved == null || !canMoveTo(wheelId)) return entries;
        List<WheelEntry> result = new ArrayList<>(entries);
        if (wheelId.equals(draggedWheel)) result.remove(indexOf(result, draggedId));
        result.add(Math.clamp(gapIndex, 0, result.size()), moved);
        return result;
    }

    double[] animatedAngles(List<WheelEntry> entries, int placeholder) {
        String view = profileId + "/" + wheelId + "/" + page;
        long now = clock.getAsLong();
        if (!view.equals(motionView)) {
            segmentAngles.clear();
            motionView = view;
            lastMotionFrame = now;
        }
        double alpha = 1 - Math.exp(-18 * Math.min(.1, Math.max(0, now - lastMotionFrame) / 1_000_000_000.0));
        lastMotionFrame = now;
        double[] offsets = new double[entries.size()];
        Set<String> present = new HashSet<>();
        for (int index = 0; index < entries.size(); index++) {
            String id = index == placeholder ? "gap:" + draggedId : "entry:" + entries.get(index).id();
            present.add(id);
            double target = RadialLayout.segmentMidAngle(index, entries.size());
            double previous = segmentAngles.getOrDefault(id, target);
            double distance = Math.atan2(Math.sin(target - previous), Math.cos(target - previous));
            double angle = previous + distance * alpha;
            segmentAngles.put(id, angle);
            offsets[index] = Math.atan2(Math.sin(angle - target), Math.cos(angle - target));
        }
        segmentAngles.keySet().retainAll(present);
        return offsets;
    }

    private int slotAt(float x, float y) {
        if (wheel() == null) return -1;
        int count = Math.min(PAGE_SIZE, displayCount() - page * PAGE_SIZE);
        float dx = (x - centerX) / scale, dy = (y - centerY) / scale;
        if (Math.hypot(dx, dy) > RadialWheelRenderer.OUTER_RADIUS + 9) return -1;
        return RadialLayout.pickDirectionalSegment(dx, dy, 0, 0, RadialWheelRenderer.INNER_RADIUS, count);
    }

    public boolean press(float x, float y, boolean doubleClick) {
        if (parentWheel() != null && Math.hypot(x - centerX, y - centerY) < RadialWheelRenderer.INNER_RADIUS * scale) {
            back();
            return true;
        }
        if (wheel() == null) return false;
        int slot = visibleSlotAt(x, y, wheel().radialEntries());
        if (slot < 0) return false;
        int index = page * PAGE_SIZE + slot;
        WheelEntry entry = wheel().radialEntries().get(index);
        if (doubleClick && entry.action() instanceof OpenWheelAction submenu) {
            navigate(submenu.wheelId(), true);
            return true;
        }
        host.select(wheelId, entry.id());
        draggedId = entry.id();
        draggedWheel = wheelId;
        sourceIndex = gapIndex = targetIndex = index;
        startX = x; startY = y;
        return true;
    }

    public void update(float x, float y, boolean pressed) {
        if (draggedId == null) return;
        if (!pressed) { release(x, y); return; }
        if (!dragging && Math.hypot(x - startX, y - startY) < 6) return;
        if (draggedEntry() == null || wheel() == null) { cancelDrag(); return; }
        dragging = true;
        updateTarget(x, y);
        String key = hoverNavigation;
        long delay = NAVIGATION_HOVER_NANOS;
        int direction = pageDirection(x, y);
        if (direction != 0 && page + direction >= 0 && page + direction < pageCount()) {
            key = "page:" + direction;
            delay = PAGE_HOVER_NANOS;
        }
        long now = clock.getAsLong();
        if (!Objects.equals(key, hoverKey)) { hoverKey = key; hoverSince = now; }
        if (key != null && now - hoverSince >= delay) {
            if (key.startsWith("page:")) changePage(direction);
            else if (key.equals(parentWheel())) back();
            else navigate(key, true);
            hoverKey = null;
            updateTarget(x, y);
        }
    }

    private int pageDirection(float x, float y) {
        if (y < pagingY - 5 || y > pagingY + PAGE_BUTTON_HEIGHT + 5) return 0;
        if (x >= previousX - 5 && x <= previousX + PAGE_BUTTON_WIDTH + 5) return -1;
        return x >= nextX - 5 && x <= nextX + PAGE_BUTTON_WIDTH + 5 ? 1 : 0;
    }

    private void drawPageButton(GuiGraphicsExtractor graphics, int x, int direction, boolean enabled, float mouseX, float mouseY) {
        String id = direction < 0 ? "preview-previous" : "preview-next";
        boolean dwelling = dragging && ("page:" + direction).equals(hoverKey);
        boolean hovered = enabled && (dragging ? dwelling : host.focused(id)
                || inside(mouseX, mouseY, x, pagingY, PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT));
        int edge = !enabled ? 0x40425361 : hovered ? 0xFF8EE1FF : 0xFF526575;
        int arrow = !enabled ? 0xFF536270 : hovered ? 0xFFF2F5F8 : 0xFFB9CAD8;
        graphics.fill(x, pagingY, x + PAGE_BUTTON_WIDTH, pagingY + PAGE_BUTTON_HEIGHT,
                !enabled ? 0x60202832 : hovered ? 0xFF344657 : 0xD8202933);
        border(graphics, x, pagingY, PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT, edge);
        int cx = x + PAGE_BUTTON_WIDTH / 2;
        int cy = pagingY + PAGE_BUTTON_HEIGHT / 2;
        GeometryRenderer.drawChevron(graphics, cx, cy, direction, arrow);
        if (dwelling && enabled) {
            double progress = Math.clamp((clock.getAsLong() - hoverSince) / (double) PAGE_HOVER_NANOS, 0, 1);
            graphics.fill(x + 1, pagingY + PAGE_BUTTON_HEIGHT - 3,
                    x + 1 + (int) Math.round((PAGE_BUTTON_WIDTH - 2) * progress), pagingY + PAGE_BUTTON_HEIGHT - 1, 0xFF8EE1FF);
        }
        if (enabled) host.target(x, pagingY, PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT, id, () -> changePage(direction));
    }

    private void updateTarget(float x, float y) {
        targetWheel = hoverNavigation = null;
        backDropHovered = false;
        highlightedSlot = -1;
        validDrop = appendDrop = false;
        if (wheel() == null || draggedEntry() == null) return;
        String parent = parentWheel();
        if ((parent != null || !wheelId.equals(draggedWheel)) && inside(x, y, backX - 4, backY - 4, 60, 30)) {
            hoveredSubmenuId = null;
            backDropHovered = true;
            appendTarget(parent == null ? wheelId : parent); return;
        }
        double radius = Math.hypot(x - centerX, y - centerY) / scale;
        if (radius < RadialWheelRenderer.INNER_RADIUS) {
            hoveredSubmenuId = null;
            if (wheel().radialEntries().isEmpty()) {
                targetWheel = wheelId;
                targetIndex = gapIndex = 0;
                validDrop = canMoveTo(wheelId);
            } else if (parent != null) appendTarget(parent);
            else if (!wheelId.equals(draggedWheel)) appendTarget(wheelId);
            return;
        }
        List<WheelEntry> shown = projectedEntries();
        int submenuSlot = submenuAt(x, y, shown);
        if (submenuSlot >= 0) {
            WheelEntry folder = shown.get(page * PAGE_SIZE + submenuSlot);
            if (!folder.id().equals(hoveredSubmenuId)) {
                hoveredSubmenuId = folder.id();
                submenuHoverX = x;
                submenuHoverY = y;
            }
            highlightedSlot = submenuSlot;
            appendTarget(((OpenWheelAction) folder.action()).wheelId());
            return;
        }
        int slot = slotAt(x, y);
        if (slot < 0) return;
        int index = page * PAGE_SIZE + slot;
        if (index >= shown.size()) return;
        gapIndex = targetIndex = index;
        targetWheel = wheelId;
        validDrop = canMoveTo(wheelId);
        highlightedSlot = slot;
    }

    /** Match moving folder wedges, while reserving the outer rim for reordering. */
    private int submenuAt(float x, float y, List<WheelEntry> shown) {
        double radius = Math.hypot(x - centerX, y - centerY) / scale;
        if (radius < RadialWheelRenderer.INNER_RADIUS || radius >= 113) {
            hoveredSubmenuId = null;
            return -1;
        }
        int count = Math.min(PAGE_SIZE, shown.size() - page * PAGE_SIZE);
        // Keep the chosen folder active while it settles, rather than freezing its geometry.
        if (hoveredSubmenuId != null && Math.hypot(x - submenuHoverX, y - submenuHoverY) <= 8 * scale) {
            for (int slot = 0; slot < count; slot++) {
                int index = page * PAGE_SIZE + slot;
                WheelEntry entry = shown.get(index);
                if (index != gapIndex && entry.id().equals(hoveredSubmenuId) && entry.action() instanceof OpenWheelAction) return slot;
            }
        }
        hoveredSubmenuId = null;
        double angle = Math.atan2(y - centerY, x - centerX);
        double nearest = count == 0 ? 0 : Math.PI / count * .96;
        int result = -1;
        for (int slot = 0; slot < count; slot++) {
            int index = page * PAGE_SIZE + slot;
            if (index == gapIndex || !(shown.get(index).action() instanceof OpenWheelAction)) continue;
            double middle = displayedAngle(shown.get(index), slot, count);
            double distance = Math.abs(Math.atan2(Math.sin(angle - middle), Math.cos(angle - middle)));
            if (distance < nearest) { nearest = distance; result = slot; }
        }
        return result;
    }

    private double displayedAngle(WheelEntry entry, int slot, int count) {
        double target = RadialLayout.segmentMidAngle(slot, count);
        return Objects.equals(motionView, profileId + "/" + wheelId + "/" + page)
                ? segmentAngles.getOrDefault("entry:" + entry.id(), target) : target;
    }

    private int visibleSlotAt(float x, float y, List<WheelEntry> entries) {
        double radius = Math.hypot(x - centerX, y - centerY) / scale;
        if (radius < RadialWheelRenderer.INNER_RADIUS || radius > RadialWheelRenderer.OUTER_RADIUS + 9) return -1;
        int count = Math.min(PAGE_SIZE, entries.size() - page * PAGE_SIZE);
        double angle = Math.atan2(y - centerY, x - centerX);
        double nearest = Math.PI;
        int result = -1;
        for (int slot = 0; slot < count; slot++) {
            double middle = displayedAngle(entries.get(page * PAGE_SIZE + slot), slot, count);
            double distance = Math.abs(Math.atan2(Math.sin(angle - middle), Math.cos(angle - middle)));
            if (distance <= nearest) { nearest = distance; result = slot; }
        }
        return result;
    }

    private void appendTarget(String destination) {
        targetWheel = destination;
        appendDrop = true;
        validDrop = canMoveTo(destination);
        WheelDefinition target = host.profile().wheels().get(destination);
        targetIndex = target == null ? 0 : target.radialEntries().size();
        if (validDrop && !destination.equals(wheelId)) hoverNavigation = destination;
    }

    public boolean release(float x, float y) {
        if (draggedId == null) return false;
        if (dragging) updateTarget(x, y);
        if (dragging && validDrop && canMoveTo(targetWheel)) {
            int boundary = targetIndex;
            sourceIndex = indexOf(host.profile().wheel(draggedWheel).radialEntries(), draggedId);
            if (!appendDrop && draggedWheel.equals(targetWheel) && sourceIndex < targetIndex) boundary++;
            WheelEntryMover.Result result = WheelEntryMover.moveRadial(host.profile(), draggedWheel, draggedId, targetWheel, boundary);
            String destination = targetWheel;
            int destinationPage = indexOf(result.profile().wheel(destination).radialEntries(), result.entryId()) / PAGE_SIZE;
            cancelDrag();
            if (!destination.equals(wheelId)) history.clear();
            wheelId = lastOwner = destination;
            lastSelection = result.entryId();
            page = destinationPage;
            host.save(result.profile(), destination, result.entryId());
        } else cancelDrag();
        return true;
    }

    public void cancelDrag() {
        hoveredSubmenuId = null;
        draggedId = draggedWheel = targetWheel = hoverNavigation = hoverKey = null;
        dragging = validDrop = appendDrop = false;
        backDropHovered = false;
        highlightedSlot = -1;
        page = Math.clamp(page, 0, pageCount() - 1);
    }

    public boolean hasDrag() { return draggedId != null; }
    public boolean isDragging() { return dragging; }

    public boolean scroll(float x, float y, double amount) {
        if (!inside(x, y, areaX, areaY, areaWidth, areaHeight)) return false;
        changePage(amount > 0 ? -1 : 1);
        if (dragging) updateTarget(x, y);
        return true;
    }

    private void changePage(int delta) {
        hoveredSubmenuId = null;
        if (wheel() != null) page = Math.clamp(page + delta, 0, pageCount() - 1);
        hoverKey = null;
    }

    private void navigate(String target, boolean remember) {
        if (host.profile() == null || !host.profile().wheels().containsKey(target) || target.equals(wheelId)) return;
        if (dragging && !canMoveTo(target)) return;
        hoveredSubmenuId = null;
        if (!dragging) cancelDrag();
        if (remember) history.addLast(new View(wheelId, page));
        else history.clear();
        wheelId = lastOwner = target;
        page = 0;
        gapIndex = wheel().radialEntries().size() - (wheelId.equals(draggedWheel) ? 1 : 0);
        gapIndex = Math.max(0, gapIndex);
        List<WheelEntry> entries = wheel().radialEntries();
        lastSelection = entries.isEmpty() ? null : entries.getFirst().id();
        host.select(target, lastSelection);
        hoverKey = null;
    }

    private String parentWheel() {
        if (host.profile() == null || wheelId == null || wheelId.equals(host.profile().rootWheelId())) return null;
        // Navigation history also restores the page where a submenu was entered.
        for (var it = history.descendingIterator(); it.hasNext();) {
            View view = it.next();
            if (pointsTo(view.wheel(), wheelId)) return view.wheel();
        }
        return findParent(host.profile().rootWheelId(), wheelId, new HashSet<>());
    }

    private boolean pointsTo(String from, String to) {
        WheelDefinition parent = host.profile().wheels().get(from);
        return parent != null && parent.radialEntries().stream()
                .anyMatch(entry -> entry.action() instanceof OpenWheelAction action && action.wheelId().equals(to));
    }

    private String findParent(String from, String target, Set<String> visited) {
        if (!visited.add(from)) return null;
        WheelDefinition parent = host.profile().wheels().get(from);
        if (parent == null) return null;
        if (pointsTo(from, target)) return from;
        for (WheelEntry entry : parent.radialEntries()) {
            if (entry.action() instanceof OpenWheelAction action) {
                String found = findParent(action.wheelId(), target, visited);
                if (found != null) return found;
            }
        }
        return null;
    }

    void back() {
        String parent = parentWheel();
        if (parent == null) {
            if (!dragging) {
                cancelDrag();
                host.closePreview();
            }
            return;
        }
        View previous = history.peekLast();
        if (previous != null && previous.wheel().equals(parent)) {
            history.removeLast();
            Deque<View> remaining = new ArrayDeque<>(history);
            navigate(parent, false);
            history.addAll(remaining);
            page = Math.clamp(previous.page(), 0, pageCount() - 1);
        } else navigate(parent, false);
    }

    private void drawNavigationTarget(GuiGraphicsExtractor graphics, int x, int y, int width) {
        if (dragging && backDropHovered && appendDrop && targetWheel != null) {
            border(graphics, x - 2, y - 2, width + 4, 26, validDrop ? 0xFF8EE1FF : 0xFFFFC46A);
            if (validDrop && targetWheel.equals(hoverKey)) drawHoverProgress(graphics, x, y - 2, width, NAVIGATION_HOVER_NANOS);
        }
    }

    private void drawSubmenuTarget(GuiGraphicsExtractor graphics, int slot, int count, double offset) {
        double start = RadialLayout.segmentStartAngle(slot, count) + offset + .04;
        double end = RadialLayout.segmentEndAngle(slot, count) + offset - .04;
        if (validDrop && Objects.equals(targetWheel, hoverKey)) {
            double progress = Math.clamp((clock.getAsLong() - hoverSince) / (double) NAVIGATION_HOVER_NANOS, 0, 1);
            GeometryRenderer.fillRingSegment(graphics, 0, 0, 49, 51, start, start + (end - start) * progress, 0xFF8EE1FF);
        }
    }

    private void drawHoverProgress(GuiGraphicsExtractor graphics, int x, int y, int width, long delay) {
        double progress = Math.clamp((clock.getAsLong() - hoverSince) / (double) delay, 0, 1);
        graphics.fill(x, y + 22, x + (int) Math.round(width * progress), y + 24, 0xFF8EE1FF);
    }

    private void drawPlaceholderOutline(GuiGraphicsExtractor graphics, int slot, int count, double offset) {
        double gap = count == 1 ? 0 : .025;
        double start = RadialLayout.segmentStartAngle(slot, count) + offset + gap;
        double end = RadialLayout.segmentEndAngle(slot, count) + offset - gap;
        int edge = 0x50314252;
        GeometryRenderer.fillRingSegment(graphics, 0, 0, 48, 50, start, end, edge);
        GeometryRenderer.fillRingSegment(graphics, 0, 0, 115, 117, start, end, edge);
        if (count == 1) return;
        GeometryRenderer.fillRingSegment(graphics, 0, 0, 48, 117, start, start + .015, edge);
        GeometryRenderer.fillRingSegment(graphics, 0, 0, 48, 117, end - .015, end, edge);
    }

    private void drawDraggedEntry(GuiGraphicsExtractor graphics, Minecraft minecraft, float mouseX, float mouseY) {
        WheelEntry entry = draggedEntry();
        if (entry == null) return;
        int width = Math.min(208, areaWidth - 16);
        int x = Math.clamp(Math.round(mouseX) - width / 2 + 3, areaX + 4, areaX + Math.max(4, areaWidth - width - 12));
        int y = Math.clamp(Math.round(mouseY) - 18, areaY + 4, areaY + Math.max(4, areaHeight - 58));
        host.draggedEntry(graphics, minecraft, entry, draggedWheel, x, y, width);
        String hint;
        if (!validDrop) hint = targetWheel != null ? text("omniwheel.manager.drag.invalid_submenu") : text("omniwheel.manager.drag.cancel");
        else if (appendDrop) hint = text("omniwheel.manager.drag.into", OmniWheelText.resolve(host.profile().wheel(targetWheel).title()));
        else hint = text("omniwheel.manager.preview.position", targetIndex + 1);
        graphics.text(minecraft.font, fit(minecraft, hint, Math.max(width, 240)), x + 3, y + 40,
                validDrop ? 0xFF9CAAB8 : 0xFFFFC46A, true);
    }

    private static void border(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private static boolean inside(float x, float y, int left, int top, int width, int height) {
        return x >= left && x <= left + width && y >= top && y <= top + height;
    }

    private static String fit(Minecraft minecraft, String label, int width) {
        return minecraft.font.width(label) <= width ? label : minecraft.font.plainSubstrByWidth(label, Math.max(0, width - minecraft.font.width("..."))) + "...";
    }

    private record View(String wheel, int page) {}
}
