package de.artemis.omniwheel.client.render;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Small, time-based visual transitions; selection and activation remain immediate. */
public final class RadialAnimation {
    private static final double RESPONSE = 24.0;
    private static final double OPEN_SECONDS = 0.14;
    private long openedAt;
    private long closedAt;
    private float closingFromScale;
    private long previousFrame;
    private String context;
    private long viewChangedAt;
    private float centerSelection;
    private List<String> entries = List.of();
    private float[] selection = new float[0];

    public void open(long now) {
        reset();
        openedAt = now;
        previousFrame = now;
    }

    public void reset() {
        openedAt = 0;
        closedAt = 0;
        previousFrame = 0;
        context = null;
        viewChangedAt = 0;
        centerSelection = 0;
        entries = List.of();
        Arrays.fill(selection, 0);
    }

    public void update(long now, String view, List<String> entryIds, int selectedIndex) {
        update(now, view, entryIds, selectedIndex, false);
    }

    public void update(long now, String view, List<String> entryIds, int selectedIndex, boolean centerHovered) {
        if (context != null && !Objects.equals(context, view)) viewChangedAt = now;
        if (!Objects.equals(context, view) || !entries.equals(entryIds)) {
            context = view;
            entries = List.copyOf(entryIds);
            selection = new float[entryIds.size()];
        }
        double seconds = previousFrame == 0 ? 0 : Math.max(0, Math.min(0.1, (now - previousFrame) / 1_000_000_000.0));
        previousFrame = now;
        float blend = (float) -Math.expm1(-RESPONSE * seconds);
        centerSelection += ((centerHovered ? 1 : 0) - centerSelection) * blend;
        for (int index = 0; index < selection.length; index++) {
            float target = index == selectedIndex ? 1 : 0;
            selection[index] += (target - selection[index]) * blend;
            if (Math.abs(target - selection[index]) < 0.001F) selection[index] = target;
        }
    }

    public float selection(int index) {
        return selection[index];
    }

    public float centerSelection() { return centerSelection; }

    /** Only menu/page changes settle; the initial opening retains its own animation. */
    public float viewScale(long now) {
        if (viewChangedAt == 0) return 1;
        double remaining = 1 - de.artemis.omniwheel.common.Numbers.clamp((now - viewChangedAt) / 160_000_000.0, 0, 1);
        return (float) (1 - 0.045 * remaining * remaining * remaining);
    }

    public float openingScale(long now) {
        if (openedAt == 0) return 1;
        double progress = Math.max(0, Math.min(1, (now - openedAt) / (OPEN_SECONDS * 1_000_000_000.0)));
        double remaining = 1 - progress;
        return (float) (1 - 0.035 * remaining * remaining * remaining);
    }

    public void close(long now, float scale) {
        reset();
        closedAt = now;
        closingFromScale = scale;
    }

    public boolean isClosing(long now) {
        return closedAt != 0 && now - closedAt < OPEN_SECONDS * 1_000_000_000.0;
    }

    public float closingScale(long now) {
        if (closedAt == 0) return 0;
        double progress = Math.max(0, Math.min(1, (now - closedAt) / (OPEN_SECONDS * 1_000_000_000.0)));
        return (float) (closingFromScale * (1 - progress * progress * progress));
    }

    public static int blendColor(int from, int to, float progress) {
        float amount = Math.max(0, Math.min(1, progress));
        int result = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int start = (from >>> shift) & 255;
            int end = (to >>> shift) & 255;
            result |= Math.round(start + (end - start) * amount) << shift;
        }
        return result;
    }
}
