package de.artemis.omniwheel.client.render;

public final class RadialLayout {
    private RadialLayout() {
    }

    public static int pickSegment(
            float mouseX,
            float mouseY,
            float centerX,
            float centerY,
            float deadzoneRadius,
            float outerRadius,
            int segmentCount
    ) {
        return pickSegment(mouseX, mouseY, centerX, centerY, deadzoneRadius, outerRadius, segmentCount, 0.0D);
    }

    public static int pickSegment(
            float mouseX,
            float mouseY,
            float centerX,
            float centerY,
            float deadzoneRadius,
            float outerRadius,
            int segmentCount,
            double gapWidth
    ) {
        float dx = mouseX - centerX;
        float dy = mouseY - centerY;
        double distance = Math.hypot(dx, dy);
        if (distance < deadzoneRadius || distance > outerRadius) {
            return -1;
        }

        double angle = normalizeAngle(Math.atan2(dy, dx) + (Math.PI / 2.0D));
        double segmentSize = (Math.PI * 2.0D) / segmentCount;
        int index = Math.min(segmentCount - 1, (int) (angle / segmentSize));

        if (gapWidth > 0.0D) {
            double halfGapAngle = Math.min(segmentSize * 0.49D, gapWidth / Math.max(distance * 2.0D, 1.0D));
            double angleInSegment = angle - (segmentSize * index);
            if (angleInSegment < halfGapAngle || angleInSegment > (segmentSize - halfGapAngle)) {
                return -1;
            }
        }

        return index;
    }

    public static int pickDirectionalSegment(
            float mouseX,
            float mouseY,
            float centerX,
            float centerY,
            float minimumRadius,
            int segmentCount
    ) {
        if (segmentCount <= 0) {
            return -1;
        }

        float dx = mouseX - centerX;
        float dy = mouseY - centerY;
        double distance = Math.hypot(dx, dy);
        if (distance < minimumRadius) {
            return -1;
        }

        double angle = normalizeAngle(Math.atan2(dy, dx) + (Math.PI / 2.0D));
        double segmentSize = (Math.PI * 2.0D) / segmentCount;
        return Math.min(segmentCount - 1, (int) (angle / segmentSize));
    }

    public static double segmentStartAngle(int index, int segmentCount) {
        return ((Math.PI * 2.0D) / segmentCount) * index - (Math.PI / 2.0D);
    }

    public static double segmentEndAngle(int index, int segmentCount) {
        return segmentStartAngle(index + 1, segmentCount);
    }

    public static double segmentMidAngle(int index, int segmentCount) {
        return segmentStartAngle(index, segmentCount) + (((Math.PI * 2.0D) / segmentCount) * 0.5D);
    }

    private static double normalizeAngle(double angle) {
        double tau = Math.PI * 2.0D;
        double normalized = angle % tau;
        return normalized < 0.0D ? normalized + tau : normalized;
    }
}
