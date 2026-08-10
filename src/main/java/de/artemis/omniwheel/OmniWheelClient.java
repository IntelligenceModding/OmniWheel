package de.artemis.omniwheel;

import de.artemis.omniwheel.client.ClientModEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = OmniWheel.MOD_ID, dist = Dist.CLIENT)
public final class OmniWheelClient {
    public OmniWheelClient(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(ClientModEvents::registerGuiLayers);
        modEventBus.addListener(ClientModEvents::registerKeyMappings);
        NeoForge.EVENT_BUS.addListener(ClientModEvents::onClientTick);
        NeoForge.EVENT_BUS.addListener(ClientModEvents::onMouseScroll);
        NeoForge.EVENT_BUS.addListener(ClientModEvents::onMouseButton);
    }
}
