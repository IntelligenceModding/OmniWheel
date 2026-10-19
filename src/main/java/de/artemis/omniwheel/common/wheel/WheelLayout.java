package de.artemis.omniwheel.common.wheel;

import java.util.List;

/** Shared ordering and page boundaries for the live wheel and its editor. */
public final class WheelLayout {
    public static final int PAGE_SIZE = 8;

    private WheelLayout() {}

    public static int pageCount(int entryCount) {
        return Math.max(1, (entryCount + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    public static <T> List<T> page(List<T> entries, int page) {
        int current = Math.clamp(page, 0, pageCount(entries.size()) - 1);
        int start = current * PAGE_SIZE;
        return List.copyOf(entries.subList(start, Math.min(start + PAGE_SIZE, entries.size())));
    }
}
