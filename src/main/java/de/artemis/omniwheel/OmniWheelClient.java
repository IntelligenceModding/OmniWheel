package de.artemis.omniwheel;

import de.artemis.omniwheel.client.ClientModEvents;
import net.fabricmc.api.ClientModInitializer;

public final class OmniWheelClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientModEvents.register();
    }
}
