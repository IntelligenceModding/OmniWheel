package de.artemis.omniwheel;

import de.artemis.omniwheel.common.config.OmniWheelConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(OmniWheel.MOD_ID)
public final class OmniWheel {
    public static final String MOD_ID = "omniwheel";
    public static final Logger LOGGER = LogUtils.getLogger();

    public OmniWheel(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, OmniWheelConfig.CLIENT_SPEC);
    }
}
