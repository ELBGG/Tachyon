package me.elb.tachyon.api;

import com.lowdragmc.photon.client.fx.BlockEffectExecutor;
import com.lowdragmc.photon.client.fx.EntityEffectExecutor;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXRuntime;
import me.elb.tachyon.Tachyon;
import me.elb.tachyon.api.executor.PlayerVFXExecutor;
import me.elb.tachyon.api.executor.PositionVFXExecutor;
import me.elb.tachyon.config.TachyonConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import org.jetbrains.annotations.Nullable;

/**
 * Fluent builder for spawning VFX effects via the Tachyon API.
 * <p>
 * Obtain via {@link TachyonAPI#vfx(String)} or {@link TachyonAPI#vfx(ResourceLocation)}.
 * <p>
 * Complete usage example:
 * <pre>{@code
 * // One-shot impact at a block
 * TachyonAPI.vfx("mymod:explosion")
 *     .atBlock(blockPos)
 *     .withScale(1.5f, 1.5f, 1.5f)
 *     .category(VFXCategory.IMPACT)
 *     .play();
 *
 * // Looping aura on an entity
 * VFXHandle handle = TachyonAPI.vfx("mymod:aura")
 *     .onEntity(player)
 *     .withOffset(0, 0.5f, 0)
 *     .allowMultiple(false)
 *     .category(VFXCategory.LOOP)
 *     .play();
 *
 * // Stop later
 * handle.stop();
 * }</pre>
 */
@Environment(EnvType.CLIENT)
public class VFXBuilder {

    private final ResourceLocation fxLocation;

    // Transform
    private Vector3f offset = new Vector3f(0, 0, 0);
    private Quaternionf rotation = new Quaternionf();
    private Vector3f scale = new Vector3f(1, 1, 1);
    private int delayTicks = 0;

    // Behavior flags
    private boolean allowMultiple = true;
    private boolean forcedDeath = false;

    // Category
    private VFXCategory category = VFXCategory.CUSTOM;

    // Target (mutually exclusive — last setter wins)
    private enum TargetType { NONE, POSITION, BLOCK, ENTITY, PLAYER }
    private TargetType targetType = TargetType.NONE;
    private double posX, posY, posZ;
    private BlockPos blockPos;
    private Entity entity;
    private EntityEffectExecutor.AutoRotate autoRotate = EntityEffectExecutor.AutoRotate.NONE;
    private PlayerVFXExecutor.PerspectiveMode perspectiveMode = PlayerVFXExecutor.PerspectiveMode.THIRD_PERSON;

    /** Package-private — use {@link TachyonAPI} to obtain a builder. */
    VFXBuilder(ResourceLocation fxLocation) {
        this.fxLocation = fxLocation;
    }

    // ── Target setters ────────────────────────────────────────────────────────

    /** Anchors the VFX at the given world coordinates. */
    public VFXBuilder atPosition(double x, double y, double z) {
        this.targetType = TargetType.POSITION;
        this.posX = x; this.posY = y; this.posZ = z;
        return this;
    }

    /** Anchors the VFX at the given world position. */
    public VFXBuilder atPosition(Vec3 pos) {
        return atPosition(pos.x, pos.y, pos.z);
    }

    /** Anchors the VFX at the center of the given block position. */
    public VFXBuilder atBlock(BlockPos pos) {
        this.targetType = TargetType.BLOCK;
        this.blockPos = pos;
        return this;
    }

    /**
     * Attaches the VFX to the given entity.
     * The VFX follows the entity each frame.
     */
    public VFXBuilder onEntity(Entity entity) {
        return onEntity(entity, EntityEffectExecutor.AutoRotate.NONE);
    }

    /**
     * Attaches the VFX to the given entity with automatic rotation.
     *
     * @param autoRotate how to rotate the VFX relative to entity facing
     */
    public VFXBuilder onEntity(Entity entity, EntityEffectExecutor.AutoRotate autoRotate) {
        this.targetType = TargetType.ENTITY;
        this.entity = entity;
        this.autoRotate = autoRotate;
        return this;
    }

    /**
     * Attaches the VFX to the local player.
     * No-op if not in a world.
     */
    public VFXBuilder onLocalPlayer() {
        return onLocalPlayer(PlayerVFXExecutor.PerspectiveMode.THIRD_PERSON);
    }

    /**
     * Attaches the VFX to the local player with a perspective mode.
     *
     * @param perspectiveMode FIRST_PERSON (camera-relative) or THIRD_PERSON (model-relative)
     */
    public VFXBuilder onLocalPlayer(PlayerVFXExecutor.PerspectiveMode perspectiveMode) {
        this.targetType = TargetType.PLAYER;
        this.perspectiveMode = perspectiveMode;
        return this;
    }

    // ── Transform setters ─────────────────────────────────────────────────────

    /** Adds a world-space offset to the effect origin. */
    public VFXBuilder withOffset(float x, float y, float z) {
        this.offset = new Vector3f(x, y, z);
        return this;
    }

    /** Sets the rotation of the effect in degrees (XYZ Euler). */
    public VFXBuilder withRotation(float xDeg, float yDeg, float zDeg) {
        this.rotation = new Quaternionf().rotationXYZ(
                (float) Math.toRadians(xDeg),
                (float) Math.toRadians(yDeg),
                (float) Math.toRadians(zDeg));
        return this;
    }

    /** Sets a uniform scale for all axes. */
    public VFXBuilder withScale(float scale) {
        this.scale = new Vector3f(scale, scale, scale);
        return this;
    }

    /** Sets per-axis scale. */
    public VFXBuilder withScale(float x, float y, float z) {
        this.scale = new Vector3f(x, y, z);
        return this;
    }

    /** Delays the effect start by the given number of ticks. */
    public VFXBuilder withDelay(int ticks) {
        this.delayTicks = ticks;
        return this;
    }

    // ── Behavior setters ──────────────────────────────────────────────────────

    /**
     * Controls whether multiple instances of this exact FX can be active
     * on the same target simultaneously (default: {@code true}).
     */
    public VFXBuilder allowMultiple(boolean allow) {
        this.allowMultiple = allow;
        return this;
    }

    /**
     * If {@code true}, instantly removes all particles when the bound target
     * (block/entity) becomes invalid. Default: wait for particle lifetime end.
     */
    public VFXBuilder forcedDeath(boolean forced) {
        this.forcedDeath = forced;
        return this;
    }

    /** Sets the semantic category of this effect (used for global toggles). */
    public VFXBuilder category(VFXCategory category) {
        this.category = category;
        return this;
    }

    // ── Terminal operations ───────────────────────────────────────────────────

    /**
     * Preloads the FX definition into the cache without spawning any particles.
     * Call this during mod initialization to ensure zero-latency on first play.
     */
    public void preload() {
        TachyonAssetLoader.preload(fxLocation);
    }

    /**
     * Spawns the VFX effect and returns a {@link VFXHandle} for runtime control.
     * <p>
     * Returns {@link VFXHandle#invalid()} if:
     * <ul>
     *   <li>The .fx file could not be found in assets</li>
     *   <li>The effect's {@link VFXCategory} is disabled in {@link TachyonConfig}</li>
     *   <li>No valid target was specified</li>
     *   <li>Not called from the client thread</li>
     * </ul>
     *
     * @return a {@link VFXHandle} — never {@code null}
     */
    public VFXHandle play() {
        // Category gate
        if (!TachyonConfig.isCategoryEnabled(category)) {
            return VFXHandle.invalid();
        }

        // Load FX definition
        FX fx = TachyonAssetLoader.load(fxLocation);
        if (fx == null) {
            Tachyon.LOGGER.warn("VFXBuilder.play(): FX '{}' not found.", fxLocation);
            return VFXHandle.invalid();
        }

        var level = Minecraft.getInstance().level;
        if (level == null) {
            Tachyon.LOGGER.warn("VFXBuilder.play(): no active level.");
            return VFXHandle.invalid();
        }

        return switch (targetType) {
            case POSITION -> playAtPosition(fx, level);
            case BLOCK    -> playAtBlock(fx, level);
            case ENTITY   -> playOnEntity(fx, level);
            case PLAYER   -> playOnPlayer(fx, level);
            case NONE -> {
                Tachyon.LOGGER.warn("VFXBuilder.play(): no target specified for '{}'.", fxLocation);
                yield VFXHandle.invalid();
            }
        };
    }

    // ── Internal spawn helpers ────────────────────────────────────────────────

    private VFXHandle playAtPosition(FX fx, net.minecraft.world.level.Level level) {
        var executor = new PositionVFXExecutor(fx, level, posX, posY, posZ);
        executor.setOffset(offset);
        executor.setRotation(rotation);
        executor.setScale(scale);
        executor.setDelay(delayTicks);
        executor.setForcedDeath(forcedDeath);
        executor.setAllowMulti(allowMultiple);
        executor.start();
        return new VFXHandle(executor.getRuntime(), executor);
    }

    private VFXHandle playAtBlock(FX fx, net.minecraft.world.level.Level level) {
        var executor = new BlockEffectExecutor(fx, level, blockPos);
        executor.setOffset(offset);
        executor.setRotation(rotation);
        executor.setScale(scale);
        executor.setDelay(delayTicks);
        executor.setForcedDeath(forcedDeath);
        executor.setAllowMulti(allowMultiple);
        executor.start();
        return new VFXHandle(executor.getRuntime(), executor);
    }

    private VFXHandle playOnEntity(FX fx, net.minecraft.world.level.Level level) {
        var executor = new EntityEffectExecutor(fx, level, entity, autoRotate);
        executor.setOffset(offset);
        executor.setRotation(rotation);
        executor.setScale(scale);
        executor.setDelay(delayTicks);
        executor.setForcedDeath(forcedDeath);
        executor.setAllowMulti(allowMultiple);
        executor.start();
        return new VFXHandle(executor.getRuntime(), executor);
    }

    private VFXHandle playOnPlayer(FX fx, net.minecraft.world.level.Level level) {
        var executor = PlayerVFXExecutor.forLocalPlayer(fx, autoRotate, perspectiveMode);
        if (executor == null) return VFXHandle.invalid();
        executor.setOffset(offset);
        executor.setRotation(rotation);
        executor.setScale(scale);
        executor.setDelay(delayTicks);
        executor.setForcedDeath(forcedDeath);
        executor.setAllowMulti(allowMultiple);
        executor.start();
        return new VFXHandle(executor.getRuntime(), executor);
    }
}
