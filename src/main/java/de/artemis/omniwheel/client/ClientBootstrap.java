package de.artemis.omniwheel.client;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class ClientBootstrap {

    private ClientBootstrap() {
    }

    public static void initialize() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(ClientModEvents::onClientSetup);
        MinecraftForge.EVENT_BUS.addListener(ClientModEvents::onRenderGui);
        MinecraftForge.EVENT_BUS.addListener(ClientModEvents::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(ClientModEvents::onClientTickStart);
    }
}
