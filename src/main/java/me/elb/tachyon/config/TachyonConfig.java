package me.elb.tachyon.config;

import me.elb.tachyon.api.VFXCategory;

import java.util.EnumSet;
import java.util.Set;

/**
 * Global runtime configuration for Tachyon.
 * <p>
 * Settings can be changed at any time from the client thread.
 * Changes take effect on the next {@link me.elb.tachyon.api.VFXBuilder#play()} call;
 * effects already in flight are not affected (except {@link #setMaxActiveVFX(int)}).
 */
public final class TachyonConfig {

    private TachyonConfig() {}

    // ── Particle quality ──────────────────────────────────────────────────────

    /** Maximum number of simultaneously active VFX runtimes. Default: 256. */
    private static int maxActiveVFX = 256;

    /**
     * Global particle density multiplier. Values between 0.0 (no particles) and
     * 2.0 (double density) are accepted. Default: 1.0.
     */
    private static float particleQualityScale = 1.0f;

    // ── Post-processing ───────────────────────────────────────────────────────

    /** Whether Photon's post-processing pipeline (bloom, HDR) is active. Default: true. */
    private static boolean enablePostProcessing = true;

    // ── Shader compat ─────────────────────────────────────────────────────────

    /**
     * When {@code true}, forces compatibility mode that disables features
     * that break under Iris/Oculus shader packs. When {@code false}, features
     * are auto-detected. Default: false (auto).
     */
    private static boolean forceShaderCompatMode = false;

    // ── Category filtering ────────────────────────────────────────────────────

    /** Set of enabled VFX categories. All categories are enabled by default. */
    private static final Set<VFXCategory> enabledCategories =
            EnumSet.allOf(VFXCategory.class);

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public static int getMaxActiveVFX() { return maxActiveVFX; }
    public static void setMaxActiveVFX(int max) { maxActiveVFX = Math.max(1, max); }

    public static float getParticleQualityScale() { return particleQualityScale; }
    public static void setParticleQualityScale(float scale) {
        particleQualityScale = Math.clamp(scale, 0.0f, 2.0f);
    }

    public static boolean isPostProcessingEnabled() { return enablePostProcessing; }
    public static void setPostProcessingEnabled(boolean enabled) {
        enablePostProcessing = enabled;
    }

    public static boolean isForceShaderCompatMode() { return forceShaderCompatMode; }
    public static void setForceShaderCompatMode(boolean forced) {
        forceShaderCompatMode = forced;
    }

    // ── Category API ──────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if effects in the given category should be spawned.
     * {@link VFXCategory#CUSTOM} is always enabled.
     */
    public static boolean isCategoryEnabled(VFXCategory category) {
        return enabledCategories.contains(category);
    }

    /** Enables a specific VFX category. */
    public static void enableCategory(VFXCategory category) {
        enabledCategories.add(category);
    }

    /** Disables a specific VFX category. New effects in this category are skipped. */
    public static void disableCategory(VFXCategory category) {
        enabledCategories.remove(category);
    }

    /** Enables ALL VFX categories. */
    public static void enableAllCategories() {
        enabledCategories.addAll(EnumSet.allOf(VFXCategory.class));
    }

    /** Disables ALL VFX categories (effectively mutes all Tachyon VFX). */
    public static void disableAllCategories() {
        enabledCategories.clear();
    }
}
