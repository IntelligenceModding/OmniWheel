package de.artemis.omniwheel.common;

/** Bounded values used by wheel layout and animation on Java 17. */
public final class Numbers {
    private Numbers() {}

    public static int clamp(int value, int min, int max) {
        return Math.min(max, Math.max(min, value));
    }

    public static float clamp(float value, float min, float max) {
        return Math.min(max, Math.max(min, value));
    }

    public static double clamp(double value, double min, double max) {
        return Math.min(max, Math.max(min, value));
    }
}
