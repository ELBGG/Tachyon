package me.elb.tachyon;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.syncdata.AccessorRegistries;
import com.lowdragmc.photon.Photon;
import com.lowdragmc.photon.PhotonNetworking;
import com.lowdragmc.photon.PhotonRegistries;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tachyon — VFX Rendering API for Fabric client mods.
 * <p>
 * Embeds Photon + LDLib2 as the rendering backend. External mods should use
 * {@link me.elb.tachyon.api.TachyonAPI} as the sole entry point.
 */
public class Tachyon implements ModInitializer {

    public static final String MOD_ID = "tachyon";
    public static final String NAME = "Tachyon";
    public static final String API_VERSION = "1";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    @Override
    public void onInitialize() {
        LOGGER.info("{} v{} initializing on platform: {}", NAME, API_VERSION, Platform.platformName());

        // 1. Bootstrap LDLib2 (creates asset dirs, registers accessors)
        LDLib2.init();
        AccessorRegistries.init();

        // 2. Bootstrap Photon core (creates photon asset folder, inits registries)
        Photon.init();
        PhotonRegistries.init();

        // 3. Register Fabric S2C payloads (BlockVFX, EntityVFX, Remove* commands)
        //    These are repurposed from Photon's networking but routed through Tachyon.
        PhotonNetworking.registerPayloads();

        LOGGER.info("{} common initialization complete.", NAME);
    }

    /** Helper to build a ResourceLocation in the tachyon namespace. */
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
