package de.artemis.omniwheel.client.render;

/**
 * Reversible reveal motion shared by category bodies and their disclosure arrows.
 */
public final class ExpansionAnimation {

    private boolean expanded;

    private float from;

    private long changed;

    public ExpansionAnimation(boolean expanded) {
        this.expanded = expanded;
        from = expanded ? 1 : 0;
    }

    public void setExpanded(boolean value, long now) {
        if (value == expanded)
            return;
        from = amount(now);
        changed = now;
        expanded = value;
    }

    public float amount(long now) {
        double progress = de.artemis.omniwheel.common.Numbers.clamp((now - changed) / 180_000_000.0, 0, 1);
        double ease = progress * progress * (3 - 2 * progress);
        return (float) (from + ((expanded ? 1 : 0) - from) * ease);
    }
}
