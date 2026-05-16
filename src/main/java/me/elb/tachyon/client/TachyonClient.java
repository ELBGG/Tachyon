package me.elb.tachyon.client;

import com.lowdragmc.lowdraglib2.client.ClientProxy;
import com.lowdragmc.lowdraglib2.client.shader.LDLibShaders;
import com.lowdragmc.photon.PhotonNetworking;
import com.lowdragmc.photon.client.PhotonClientListeners;
import com.lowdragmc.photon.client.PhotonShaders;
import dev.felnull.specialmodelloader.api.event.SpecialModelLoaderEvents;
import me.elb.tachyon.Tachyon;
import me.elb.tachyon.client.TachyonResourceReloadListener;
import me.elb.tachyon.networking.TachyonPluginChannelHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

/**
 * Tachyon client-side initializer.
 * <p>
 * Bootstraps the complete VFX rendering pipeline:
 * <ul>
 *   <li>LDLib2 shaders + model loading</li>
 *   <li>Photon particle system + post-processing</li>
 *   <li>Fabric S2C network handlers (block/entity VFX)</li>
 *   <li>Plugin channel {@code tachyon:vfx} for Paper/Spigot compatibility</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public class TachyonClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Tachyon.LOGGER.info("Tachyon client initializing...");

        // ── LDLib2 client stack ───────────────────────────────────────────────
        ClientProxy.register();

        // ── Photon shaders & rendering pipeline ──────────────────────────────
        // Shaders need GL context, defer until the client is fully started
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            LDLibShaders.init();
            PhotonShaders.init();
        });
        PhotonShaders.registerShaders();
        CoreShaderRegistrationCallback.EVENT.register(LDLibShaders::registerCoreShaders);

        // ── Photon lifecycle listeners (particle tick, render hooks) ─────────
        PhotonClientListeners.init();

        // ── Fabric S2C handlers (block/entity VFX from Fabric servers) ───────
        PhotonNetworking.registerClientHandlers();

        // ── Plugin Channel — tachyon:vfx (Paper/Spigot / vanilla servers) ────
        // Registered as a raw custom payload channel. The server sends raw bytes;
        // TachyonPluginChannelHandler decodes the protocol.
        // Fabric's ClientPlayNetworking supports raw channels via registerReceiver
        // with a CustomChannelPayload; we use the simple byte-buffer approach.
        /* network payload replaced */

        // ── SpecialModelLoader — OBJ model scope for tachyon namespace ────────
        // Allows VFX that reference 3D OBJ models to load from the tachyon/photon/ldlib2 namespaces.
        SpecialModelLoaderEvents.LOAD_SCOPE.register(() -> (resourceManager, location) -> {
            String ns = location.getNamespace();
            return ns.equals("photon") || ns.equals("ldlib2") || ns.equals(Tachyon.MOD_ID);
        });

        // ── FX cache cleanup on resource reload ───────────────────────────────
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                new TachyonResourceReloadListener()
        );

        Tachyon.LOGGER.info("Tachyon client initialization complete.");
    }
}
