package de.artemis.omniwheel.client.render;

import java.util.HashMap;
import java.util.Map;

public final class HighlightAnimation {
    private record State(float amount, long frame) {}
    private final Map<Object, State> states = new HashMap<>();

    public float amount(Object key, boolean highlighted, long now) {
        State previous = states.get(key);
        if (previous == null || now - previous.frame() > 250_000_000L) {
            if (states.size() > 512) states.entrySet().removeIf(entry -> now - entry.getValue().frame() > 250_000_000L);
            previous = new State(0, now);
        }
        double seconds = Math.clamp((now - previous.frame()) / 1_000_000_000.0, 0, .1);
        float amount = previous.amount() + ((highlighted ? 1 : 0) - previous.amount()) * (float) -Math.expm1(-24 * seconds);
        states.put(key, new State(amount, now));
        return amount;
    }

    public void clear() { states.clear(); }
}
