package me.elb.tachyon.api;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;

/**
 * <b>Tachyon VFX API</b> — main entry point for external mods.
 * <p>
 * Tachyon is a client-only VFX rendering library. This class provides the
 * single entry point that external mods should use to spawn particle effects.
 * All effects are loaded from the calling mod's own asset folder:
 * <pre>
 *   assets/&lt;mod_namespace&gt;/tachyon/fx/&lt;effect_name&gt;.fx
 * </pre>
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // 1. Place your .fx file at:
 * //    assets/mymod/tachyon/fx/explosion.fx
 *
 * // 2. Spawn the effect (call from client thread):
 * TachyonAPI.vfx("mymod:explosion")
 *     .atBlock(blockPos)
 *     .withScale(1.5f, 1.5f, 1.5f)
 *     .category(VFXCategory.IMPACT)
 *     .play();
 *
 * // 3. For looping effects, hold a handle:
 * VFXHandle aura = TachyonAPI.vfx("mymod:aura")
 *     .onEntity(player)
 *     .allowMultiple(false)
 *     .play();
 *
 * // 4. Stop when done:
 * aura.stop();
 * }</pre>
 *
 * <h2>Dependency Declaration</h2>
 * In your mod's {@code fabric.mod.json}:
 * <pre>{@code
 * "depends": {
 *     "tachyon": ">=1"
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * All methods on this class <b>must</b> be called from the <b>client render thread</b>.
 * Use {@code Minecraft.getInstance().execute(() -> ...)} if dispatching from another thread.
 */
@Environment(EnvType.CLIENT)
public final class TachyonAPI {

    private TachyonAPI() {}

    /**
     * Creates a {@link VFXBuilder} for the given VFX resource location string.
     * <p>
     * The string format is {@code "namespace:path"}, e.g.:
     * <ul>
     *   <li>{@code "mymod:fireball"} → {@code assets/mymod/tachyon/fx/fireball.fx}</li>
     *   <li>{@code "mymod:spells/lightning"} → {@code assets/mymod/tachyon/fx/spells/lightning.fx}</li>
     * </ul>
     *
     * @param location the VFX resource location string
     * @return a configured builder ready to be pointed at a target and played
     * @throws IllegalArgumentException if {@code location} is not a valid resource location
     */
    public static VFXBuilder vfx(String location) {
        return vfx(ResourceLocation.parse(location));
    }

    /**
     * Creates a {@link VFXBuilder} for the given {@link ResourceLocation}.
     *
     * @param location the VFX resource location
     * @return a configured builder ready to be pointed at a target and played
     */
    public static VFXBuilder vfx(ResourceLocation location) {
        return new VFXBuilder(location);
    }

    /**
     * Preloads a VFX definition into the cache without spawning any particles.
     * <p>
     * Call during your mod's client initialization to ensure zero latency on
     * the first play. Equivalent to {@code TachyonAPI.vfx(location).preload()}.
     *
     * @param location the VFX resource location to preload
     */
    public static void preload(String location) {
        TachyonAssetLoader.preload(ResourceLocation.parse(location));
    }

    /**
     * Preloads multiple VFX definitions at once.
     *
     * @param locations resource location strings to preload
     */
    public static void preloadAll(String... locations) {
        for (String loc : locations) {
            TachyonAssetLoader.preload(ResourceLocation.parse(loc));
        }
    }
}
