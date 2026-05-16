package me.elb.tachyon.api.executor;

import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.EntityEffectExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * An executor that attaches a VFX to the local player entity with additional
 * perspective modes.
 * <p>
 * Extends {@link EntityEffectExecutor} with:
 * <ul>
 *   <li>{@link PerspectiveMode#THIRD_PERSON} — follows the player model (default)</li>
 *   <li>{@link PerspectiveMode#FIRST_PERSON} — offsets relative to the camera</li>
 * </ul>
 * <p>
 * Create via {@link me.elb.tachyon.api.VFXBuilder#onLocalPlayer()}.
 */
@Environment(EnvType.CLIENT)
public class PlayerVFXExecutor extends EntityEffectExecutor {

    /** How the VFX position is calculated relative to the player. */
    public enum PerspectiveMode {
        /** Follows the player model in both first and third person. */
        THIRD_PERSON,
        /**
         * Offsets the VFX relative to the camera eye position.
         * Useful for screen-space or FPS-style effects.
         */
        FIRST_PERSON
    }

    private final PerspectiveMode perspectiveMode;

    public PlayerVFXExecutor(FX fx, Level level, Entity player, AutoRotate autoRotate,
                             PerspectiveMode perspectiveMode) {
        super(fx, level, player, autoRotate);
        this.perspectiveMode = perspectiveMode;
    }

    /**
     * Creates a {@link PlayerVFXExecutor} targeting the current local player.
     *
     * @return {@code null} if no local player is available (e.g. not in a world)
     */
    public static PlayerVFXExecutor forLocalPlayer(FX fx, AutoRotate autoRotate,
                                                    PerspectiveMode perspectiveMode) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return null;
        return new PlayerVFXExecutor(fx, mc.level, mc.player, autoRotate, perspectiveMode);
    }
}
