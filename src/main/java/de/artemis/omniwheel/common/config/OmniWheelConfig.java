package de.artemis.omniwheel.common.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class OmniWheelConfig {
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ClientValues CLIENT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        CLIENT = new ClientValues(builder);
        CLIENT_SPEC = builder.build();
    }

    private OmniWheelConfig() {
    }

    public static final class ClientValues {
        public final ModConfigSpec.DoubleValue wheelRadius;
        public final ModConfigSpec.DoubleValue deadzoneRadius;
        public final ModConfigSpec.IntValue segmentGapDegrees;
        public final ModConfigSpec.IntValue backgroundAlpha;

        private ClientValues(ModConfigSpec.Builder builder) {
            builder.push("ui");
            wheelRadius = builder
                    .translation("config.omniwheel.wheel_radius")
                    .comment("Outer radius of the radial wheel in screen pixels.")
                    .defineInRange("wheelRadius", 118.0D, 72.0D, 220.0D);
            deadzoneRadius = builder
                    .translation("config.omniwheel.deadzone_radius")
                    .comment("Inner cancel radius of the radial wheel in screen pixels.")
                    .defineInRange("deadzoneRadius", 28.0D, 8.0D, 96.0D);
            segmentGapDegrees = builder
                    .translation("config.omniwheel.segment_gap_degrees")
                    .comment("Visual spacing between radial segments.")
                    .defineInRange("segmentGapDegrees", 2, 0, 12);
            backgroundAlpha = builder
                    .translation("config.omniwheel.background_alpha")
                    .comment("Dim background alpha while the wheel is open.")
                    .defineInRange("backgroundAlpha", 148, 0, 255);
            builder.pop();
        }
    }
}
