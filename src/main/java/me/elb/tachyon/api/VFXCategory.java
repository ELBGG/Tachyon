package me.elb.tachyon.api;

/**
 * Semantic categories for VFX effects.
 * <p>
 * Categories allow players and servers to toggle groups of effects
 * independently via {@link me.elb.tachyon.config.TachyonConfig}.
 * <p>
 * Usage example:
 * <pre>{@code
 * TachyonAPI.vfx("mymod:explosion")
 *     .atBlock(pos)
 *     .category(VFXCategory.IMPACT)
 *     .play();
 * }</pre>
 */
public enum VFXCategory {
    /** Ambient world effects — fog, fireflies, dust motes. */
    AMBIENT,
    /** Magic / spell cast effects. */
    SPELL,
    /** One-shot impact effects — explosions, hits. */
    IMPACT,
    /** Persistent looping effects — auras, flames, status effects. */
    LOOP,
    /** Hit/damage feedback effects. */
    HIT,
    /** Custom / uncategorized effects. */
    CUSTOM;
}
