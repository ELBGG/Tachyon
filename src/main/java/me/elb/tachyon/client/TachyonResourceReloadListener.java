package me.elb.tachyon.client;

import com.lowdragmc.photon.client.fx.FXHelper;
import me.elb.tachyon.Tachyon;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Clears the FX definition cache whenever the client reloads resources
 * (e.g. F3+T, resource pack change). Forces fresh re-reads of .fx files.
 */
public class TachyonResourceReloadListener implements SimpleSynchronousResourceReloadListener {

    private static final ResourceLocation ID = Tachyon.id("fx_cache_reload");

    @Override
    public ResourceLocation getFabricId() {
        return ID;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        int cleared = FXHelper.clearCache();
        Tachyon.LOGGER.debug("Cleared {} cached FX definitions on resource reload.", cleared);
    }
}
