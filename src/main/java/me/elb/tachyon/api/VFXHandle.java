package me.elb.tachyon.api;

import com.lowdragmc.photon.client.fx.FXEffectExecutor;
import com.lowdragmc.photon.client.fx.FXRuntime;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

/**
 * A handle to a playing VFX effect instance.
 * <p>
 * Obtain via {@link VFXBuilder#play()}. Allows runtime control of the effect
 * after it has been started.
 * <p>
 * Example:
 * <pre>{@code
 * VFXHandle handle = TachyonAPI.vfx("mymod:aura").onEntity(player).play();
 *
 * // Later, when the effect should stop:
 * handle.stop();
 * }</pre>
 */
@Environment(EnvType.CLIENT)
public class VFXHandle {

    @Nullable
    private final FXRuntime runtime;
    @Nullable
    private final FXEffectExecutor executor;
    private final boolean valid;

    /** Internal constructor — use {@link VFXBuilder#play()} to obtain handles. */
    VFXHandle(@Nullable FXRuntime runtime, @Nullable FXEffectExecutor executor) {
        this.runtime = runtime;
        this.executor = executor;
        this.valid = runtime != null;
    }

    /** Creates an invalid (no-op) handle, used when FX loading fails. */
    static VFXHandle invalid() {
        return new VFXHandle(null, null);
    }

    /**
     * Returns {@code true} if the FX was successfully loaded and started.
     * An invalid handle is returned when the .fx file could not be found.
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Returns {@code true} if the effect is still alive (particles in flight
     * or emitter still running).
     */
    public boolean isAlive() {
        return valid && runtime != null && runtime.isAlive();
    }

    /**
     * Gracefully stops the effect. Running particles finish their lifetime
     * naturally before the effect is removed.
     */
    public void stop() {
        if (valid && runtime != null) {
            runtime.destroy(false);
        }
    }

    /**
     * Immediately removes all particles and stops the effect.
     */
    public void forceStop() {
        if (valid && runtime != null) {
            runtime.destroy(true);
        }
    }

    /** Returns the underlying {@link FXRuntime}, or {@code null} if invalid. */
    @Nullable
    public FXRuntime getRuntime() {
        return runtime;
    }
}
