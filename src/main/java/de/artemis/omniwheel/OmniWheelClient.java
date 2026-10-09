package de.artemis.omniwheel;

import de.artemis.omniwheel.client.ClientModEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(OmniWheel.MOD_ID)
public final class OmniWheelClient {
    public OmniWheelClient(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(ClientModEvents::registerKeyMappings);
        MinecraftForge.EVENT_BUS.addListener(ClientModEvents::onChatOverlay);
        MinecraftForge.EVENT_BUS.addListener(ClientModEvents::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(ClientModEvents::onMouseScroll);
        MinecraftForge.EVENT_BUS.addListener(ClientModEvents::onMouseButton);
    }
}
