package de.artemis.omniwheel;

import de.artemis.omniwheel.client.ClientBootstrap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;

@Mod(OmniWheel.MOD_ID)
public final class OmniWheelClient {

    public OmniWheelClient() {
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (remoteVersion, isServer) -> true));
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientBootstrap::initialize);
    }
}
