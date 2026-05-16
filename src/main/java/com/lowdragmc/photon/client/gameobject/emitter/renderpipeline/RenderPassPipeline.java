package com.lowdragmc.photon.client.gameobject.emitter.renderpipeline;

import com.google.common.collect.Maps;
import com.lowdragmc.lowdraglib2.client.shader.HDRTarget;
import com.lowdragmc.lowdraglib2.client.shader.LDLibShaders;
import com.lowdragmc.lowdraglib2.client.utils.ShaderUtils;
import com.lowdragmc.lowdraglib2.math.PositionedRect;
import com.lowdragmc.photon.Photon;
import com.lowdragmc.photon.PhotonConfig;
import com.lowdragmc.photon.client.PhotonParticleManager;
import com.lowdragmc.photon.client.gameobject.particle.IParticle;
import com.lowdragmc.photon.client.postprocessing.PhotonPostProcessing;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import lombok.Getter;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RenderPassPipeline extends BufferBuilder {
    public static class BufferBuilderPool {
        private final ConcurrentLinkedQueue<Tesselator> pool = new ConcurrentLinkedQueue<>();
        public Tesselator acquire() {
            var tesselator = pool.poll();
            return tesselator != null ? tesselator : new Tesselator(1536);
        }

        public void release(Tesselator tesselator) {
            pool.offer(tesselator);
        }
    }

    private static final int MINIMUM_TASK_SIZE = 64;
    private static final BufferBuilderPool BUILDER_POOL = new BufferBuilderPool();

    @Getter
    private final ByteBufferBuilder sortingBuffer;

    // runtime
    
    @Nullable
    @Getter
    private static RenderPassPipeline current = null;
    private final Map<PhotonFXRenderPass, Queue<IParticle>> particles = Maps.newTreeMap(makeRenderPassComparator());
    @Getter
    private Camera camera;
    @Getter
    private float partialTicks;
    @Getter
    private static HDRTarget DRAW_TARGET;
    private static boolean IS_DRAW_TARGET_DIRTY = true;
    @Nullable
    private static HDRTarget SCENE_SAMPLER;
    private static boolean IS_SCENE_SAMPLER_DIRTY = true;

    // Deferred render list: LevelRenderer calls ParticleEngine.render() before renderClouds().
    // We defer the actual VFX render pass to after renderClouds() via LevelRendererMixin so that
    // VFX appears on top of clouds. At that point prepareTarget() copies mainTarget-with-clouds,
    // and the blit works correctly without any special blending tricks.
    private static final List<RenderPassPipeline> DEFERRED_PIPELINES = new ArrayList<>();

    public static Comparator<PhotonFXRenderPass> makeRenderPassComparator() {
        return (passOne, passTwo) -> {
            var comparedResult = passOne.layerOrder() - passTwo.layerOrder();
            if (comparedResult == 0) {
                if (passOne.equals(passTwo)) {
                    return 0;
                }
                return Integer.compare(passOne.hashCode(), passTwo.hashCode());
            }
            return comparedResult;
        };
    }

    public RenderPassPipeline(ByteBufferBuilder sortingBuffer) {
        super(sortingBuffer, VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        this.sortingBuffer = sortingBuffer;
    }

    @Override
    public @Nullable MeshData build() {
        if (particles.isEmpty()) return null;
        // In main game rendering, defer the entire render pass to after renderClouds().
        // At flush time prepareTarget() will copy mainTarget-with-clouds as the backdrop,
        // so VFX naturally composites on top of clouds with no special blending required.
        // SceneView (getDrawMode() != null) and Iris paths still render immediately.
        DEFERRED_PIPELINES.add(this);
        return null;
    }

    private void doRenderNow() {
        beforeRendering();
        RenderSystem.setShader(GameRenderer::getParticleShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        for (var entry : particles.entrySet()) {
            var renderPass = entry.getKey();
            var particleQueue = entry.getValue();
            if (!particleQueue.isEmpty()) {
                renderPass.prepareStatus(this);
                renderParticles(renderPass, particleQueue);
                renderPass.releaseStatus(this);
            }
            markSceneSamplerDirty();
        }
        clearRenderingState();
        afterRendering();
    }

    private void beforeRendering() {
        current = this;
        var mainTarget = Minecraft.getInstance().getMainRenderTarget();
        prepareTarget(mainTarget.width, mainTarget.height);
        PhotonPostProcessing.prepareTarget(mainTarget.width, mainTarget.height);
        
    }

    public static HDRTarget resize(@Nullable HDRTarget target, int width, int height, boolean useDepth) {
        return resize(target, width, height, useDepth, false);
    }

    public static HDRTarget resize(@Nullable HDRTarget target, int width, int height, boolean useDepth, boolean forceResize) {
        if (target == null) {
            target = new HDRTarget(width, height, GL11.GL_LINEAR, useDepth);
            target.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        } else if (forceResize || target.width != width || target.height != height) {
            target.resize(width, height, Minecraft.ON_OSX);
        }
        return target;
    }

    public static void markDrawTargetDirty() {
        IS_DRAW_TARGET_DIRTY = true;
    }

    private void prepareTarget(int width, int height) {
        DRAW_TARGET = resize(DRAW_TARGET, width, height, true, IS_DRAW_TARGET_DIRTY);
        IS_DRAW_TARGET_DIRTY = false;
        // we will copy the color texture and share the depth texture of the main target.
            var mainTarget = Minecraft.getInstance().getMainRenderTarget();
            DRAW_TARGET.copyColorFrom(mainTarget);
            if (!DRAW_TARGET.hasOtherAttachedDepthTexture() || DRAW_TARGET.getAttachedDepthTexture() != mainTarget.getDepthTextureId()) {
                DRAW_TARGET.attachDepthBuffer(mainTarget);
            }
        DRAW_TARGET.bindWrite(false);
    }

    private void afterRendering() {
        
        var mainTarget = Minecraft.getInstance().getMainRenderTarget();
        var lastViewport = PositionedRect.of(GlStateManager.Viewport.x(), GlStateManager.Viewport.y(), GlStateManager.Viewport.width(), GlStateManager.Viewport.height());
        var background = Minecraft.getInstance().getMainRenderTarget();
        var hasDifferentViewPort = lastViewport.position.x != 0 ||
                lastViewport.position.y != 0 ||
                lastViewport.size.width != background.width ||
                lastViewport.size.height != background.height;
        // setup view port
        if (hasDifferentViewPort) {
            RenderSystem.viewport(0, 0, background.width, background.height);
        }

        var doBloom = PhotonConfig.INSTANCE.enableBloom && (!Photon.isUsingShaderPack() || PhotonConfig.INSTANCE.enableBloomWithIrisShader);
        RenderTarget outputTarget;
        if (doBloom) {
            outputTarget = PhotonPostProcessing.postTarget(DRAW_TARGET);
        } else {
            outputTarget = DRAW_TARGET;
        }

        // we need it because extended shaders only work while the main target bound.
        mainTarget.bindWrite(false);
            if (LDLibShaders.getBlitShader() != null) {
                ShaderUtils.fastBlit(outputTarget, mainTarget);
            } else {
                Photon.LOGGER.error("LDLibShaders.blitShader is null, cannot blit particle render target. Is LDLib2 client initialized?");
            }

        // restore view port
        if (hasDifferentViewPort){
            RenderSystem.viewport(lastViewport.position.x, lastViewport.position.y, lastViewport.size.width, lastViewport.size.height);
        }

        RenderSystem.setShader(GameRenderer::getParticleShader);
        current = null;
    }

    private void renderParticles(PhotonFXRenderPass renderPass, Queue<IParticle> particleQueue) {
//        if (renderPass.isParallel()) {
//            renderParticlesParallel(renderPass, particleQueue);
//        } else {
            renderParticlesSequential(renderPass, particleQueue);
//        }
    }

//    private void renderParticlesParallel(PhotonFXRenderPass renderPass, Queue<IParticle> particleQueue) {
//        try (var forkJoinPool = ForkJoinPool.commonPool()) {
//            var maxThreads = ForkJoinPool.getCommonPoolParallelism() + 1;
//            var task = new ParallelRenderingTask(Math.max(particleQueue.size() / maxThreads, MINIMUM_TASK_SIZE), renderPass, particleQueue.spliterator());
//            var sorting = renderPass.getSorting();
//            for (var pair : forkJoinPool.submit(task).get()) {
//                uploadMeshData(pair.getB(), sorting);
//                BUILDER_POOL.release(pair.getA());
//            }
//        } catch (Throwable throwable) {
//            Photon.LOGGER.error("Error rendering particles in parallel", throwable);
//        }
//    }

    private void renderParticlesSequential(PhotonFXRenderPass renderPass, Queue<IParticle> particleQueue) {
        renderPass.drawParticles(this, particleQueue, camera, partialTicks);
    }

    private void clearRenderingState() {
        particles.clear();
        camera = null;
    }

    public void setupRenderingState(Camera camera, float partialTicks) {
        this.camera = camera;
        this.partialTicks = partialTicks;
    }

    public void pipeQueue(@Nonnull PhotonFXRenderPass renderPass, @Nonnull Collection<IParticle> queue) {
        particles.computeIfAbsent(renderPass, t -> new ArrayDeque<>()).addAll(queue);
    }

    /// Push data parallel
//    class ParallelRenderingTask extends RecursiveTask<List<Pair<Tesselator, BufferBuilder>>> {
//        private final int threshold;
//        private final PhotonFXRenderPass renderPass;
//        private final Spliterator<IParticle> particleSpliterator;
//
//        public ParallelRenderingTask(int threshold, PhotonFXRenderPass renderPass, Spliterator<IParticle> particleSpliterator) {
//            this.renderPass = renderPass;
//            this.particleSpliterator = particleSpliterator;
//            this.threshold = threshold;
//        }
//
//        @Override
//        protected List<Pair<Tesselator, BufferBuilder>> compute() {
//            if (particleSpliterator.estimateSize() > threshold) {
//                var split = particleSpliterator.trySplit();
//                var firstTask = new ParallelRenderingTask(threshold, renderPass, particleSpliterator).fork();
//
//                List<Pair<Tesselator, BufferBuilder>> result = new ArrayList<>();
//                if (split != null) {
//                    result.addAll(new ParallelRenderingTask(threshold, renderPass, split).compute());
//                }
//                result.addAll(firstTask.join());
//
//                return result;
//            } else {
//                var tesselator = BUILDER_POOL.acquire();
//                var buffer = renderPass.begin(tesselator);
//
//                particleSpliterator.forEachRemaining(p -> p.render(buffer, camera, partialTicks));
//                return List.of(new Pair<>(tesselator, buffer));
//            }
//        }
//    }

    /**
     * Renders and blits all deferred Photon VFX pipelines.
     * Called by LevelRendererMixin after renderClouds() returns. At this point mainTarget
     * already contains clouds, so prepareTarget() uses them as the backdrop and VFX
     * composites on top via the normal blit — no special blending required.
     */
    public static void flushRender() {
        if (DEFERRED_PIPELINES.isEmpty()) return;
        for (var pipeline : DEFERRED_PIPELINES) {
            pipeline.doRenderNow();
        }
        DEFERRED_PIPELINES.clear();
    }

    /** Discards deferred pipelines. Called on level change to avoid rendering stale state. */
    public static void clearDeferredRender() {
        for (var pipeline : DEFERRED_PIPELINES) {
            pipeline.clearRenderingState();
        }
        DEFERRED_PIPELINES.clear();
    }

    ///  Scene Sampler
    public @Nonnull HDRTarget getSceneSampler() {
        if (SCENE_SAMPLER != null && !IS_SCENE_SAMPLER_DIRTY) return SCENE_SAMPLER;
        updateSceneSampler();
        DRAW_TARGET.bindWrite(false);
        return SCENE_SAMPLER;
    }

    public void markSceneSamplerDirty() {
        IS_SCENE_SAMPLER_DIRTY = true;
    }

    private void updateSceneSampler() {
        SCENE_SAMPLER = resize(SCENE_SAMPLER, DRAW_TARGET.width, DRAW_TARGET.height, true);
        SCENE_SAMPLER.copyDepthAndColorFrom(DRAW_TARGET);
        IS_SCENE_SAMPLER_DIRTY = false;
    }

        public static RenderPassPipeline getCurrent() { return current; }
}