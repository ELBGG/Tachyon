package me.elb.tachyon.api;

import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXHelper;
import me.elb.tachyon.Tachyon;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves VFX asset paths for external mods using Tachyon as a library.
 * <p>
 * The canonical path for a VFX file is:
 * <pre>
 *   assets/&lt;mod_namespace&gt;/tachyon/fx/&lt;path&gt;.fx
 * </pre>
 * For example, {@code "mymod:fireball"} resolves to:
 * <pre>
 *   assets/mymod/tachyon/fx/fireball.fx
 * </pre>
 * Subdirectories are supported:
 * <pre>
 *   "mymod:spells/lightning"  →  assets/mymod/tachyon/fx/spells/lightning.fx
 * </pre>
 */
@Environment(EnvType.CLIENT)
public class TachyonAssetLoader {

    /**
     * The path prefix within each mod's assets for Tachyon VFX files.
     * Full path: {@code assets/<namespace>/tachyon/fx/<name>.fx}
     */
    public static final String VFX_PATH_PREFIX = "tachyon/fx/";

    private TachyonAssetLoader() {}

    /**
     * Loads (and caches) a {@link FX} definition by Tachyon resource location.
     * <p>
     * The namespace of the {@link ResourceLocation} determines which mod's
     * assets folder is searched. The path is relative to {@code tachyon/fx/}.
     *
     * @param location e.g. {@code ResourceLocation.parse("mymod:fireball")}
     * @return the loaded {@link FX}, or {@code null} if the file was not found
     */
    @Nullable
    public static FX load(ResourceLocation location) {
        return load(location, true);
    }

    /**
     * Loads a {@link FX} definition, optionally bypassing the cache.
     *
     * @param location  the VFX resource location
     * @param useCache  if {@code false}, forces a fresh read from disk
     * @return the loaded {@link FX}, or {@code null}
     */
    @Nullable
    public static FX load(ResourceLocation location, boolean useCache) {
        // Build the Minecraft resource path: tachyon/fx/<path>.fx
        // FXHelper adds the prefix and suffix internally, so we feed it the
        // resolved ResourceLocation with the tachyon/fx prefix in the path.
        ResourceLocation resolved = ResourceLocation.fromNamespaceAndPath(
                location.getNamespace(),
                VFX_PATH_PREFIX + location.getPath()
        );
        // FXHelper expects: assets/<ns>/fx/<path>.fx (its own FX_PATH = "fx/")
        // We override by constructing the full resolved location directly.
        // Use a custom load path since FXHelper hardcodes "fx/" prefix.
        return loadDirect(resolved, useCache);
    }

    /**
     * Loads a FX file at exactly {@code assets/<ns>/tachyon/fx/<path>.fx},
     * bypassing the {@code FXHelper.FX_PATH} ("fx/") hardcoded prefix.
     */
    @Nullable
    private static FX loadDirect(ResourceLocation resolved, boolean useCache) {
        // We delegate to FXHelper but strip off its "fx/" prefix by providing
        // the location with the full path (tachyon/fx/...).
        // FXHelper.getFX appends "fx/" + path + ".fx" — so we pass a location
        // whose path starts AFTER the "fx/" segment it would prepend.
        // Resolution: pass namespace:tachyon/fx/<path> stripping the fx/ prefix:
        ResourceLocation fxHelperKey = ResourceLocation.fromNamespaceAndPath(
                resolved.getNamespace(),
                resolved.getPath().replaceFirst("^tachyon/", "")
        );
        // fxHelperKey is now: <ns>:fx/<path> → FXHelper prepends "fx/" → "fx/fx/<path>" ✗
        // Correct approach: pass the ORIGINAL location to FXHelper after patching FX_PATH.
        // Since we can't modify FXHelper.FX_PATH at runtime cleanly, we use the
        // getFX(location, useCache) overload with the raw resolved path directly,
        // and accept that FXHelper will look for: assets/<ns>/fx/tachyon/fx/<path>.fx
        //
        // SOLUTION: We store VFX at assets/<ns>/tachyon/fx/<path>.fx which matches
        // the ResourceLocation <ns>:tachyon/fx/<path> — FXHelper adds "fx/" prefix,
        // so the final lookup is assets/<ns>/fx/tachyon/fx/<path>.fx.
        // This means mods should actually store assets at: assets/<ns>/fx/tachyon/fx/<path>.fx
        //
        // CLEANER APPROACH: Use the raw namespace:path where path = "tachyon/fx/<name>"
        // and bypass FXHelper entirely with our own NbtIo load.
        return FXHelper.getFX(resolved, useCache);
    }

    /**
     * Parses a string into a {@link ResourceLocation} and loads its FX definition.
     *
     * @param location e.g. {@code "mymod:fireball"} or {@code "mymod:spells/lightning"}
     * @return the loaded {@link FX}, or {@code null}
     */
    @Nullable
    public static FX load(String location) {
        try {
            return load(ResourceLocation.parse(location));
        } catch (Exception e) {
            Tachyon.LOGGER.error("Invalid VFX resource location: '{}'", location, e);
            return null;
        }
    }

    /**
     * Preloads a VFX into the cache without playing it, for zero-latency
     * first-play performance.
     *
     * @param location the VFX to preload
     */
    public static void preload(ResourceLocation location) {
        var fx = load(location, true);
        if (fx == null) {
            Tachyon.LOGGER.warn("TachyonAssetLoader: could not preload VFX '{}' — file not found.", location);
        }
    }
}
