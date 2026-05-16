package com.lowdragmc.photon.client.gameobject.emitter.particle;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.photon.client.gameobject.emitter.data.material.IMaterial;
import com.lowdragmc.photon.client.gameobject.emitter.data.material.MaterialContext;
import com.lowdragmc.photon.client.gameobject.emitter.renderpipeline.PhotonFXRenderPass;
import com.lowdragmc.photon.client.gameobject.emitter.data.*;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.*;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.color.Color;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.color.Gradient;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.color.RandomColor;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.color.RandomGradient;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.curve.Curve;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.curve.CurveConfig;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.curve.RandomCurve;
import com.lowdragmc.photon.client.gameobject.emitter.renderpipeline.RenderPassPipeline;
import com.lowdragmc.photon.client.gameobject.particle.IParticle;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Camera;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.List;

import static org.lwjgl.opengl.GL30.glBindVertexArray;

/**
 * @author KilaBash
 * @date 2023/6/11
 * @implNote ParticleConfig
 * @port ELB_GG 
 * @date_port 2026/03/29 
 * @port_to fabric
 */
public class ParticleConfig implements IPersistedSerializable {
    @Setter
    @Getter
    
    
    protected int duration = 100;
    @Setter
    @Getter
    
    protected boolean looping = true;
    @Setter
    @Getter
    
    
    protected int prewarm = 0;
    @Setter
    @Getter
    
    @NumberFunctionConfig(types = {Constant.class, RandomConstant.class, Curve.class, RandomCurve.class}, min = 0, curveConfig = @CurveConfig(bound = {0, 100}, xAxis = "duration", yAxis = "delay"))
    protected NumberFunction startDelay = NumberFunction.constant(0);
    @Setter
    @Getter
    
    @NumberFunctionConfig(types = {Constant.class, RandomConstant.class, Curve.class, RandomCurve.class}, min = 0, defaultValue = 100, curveConfig = @CurveConfig(bound = {0, 200}, xAxis = "duration", yAxis = "life time"))
    protected NumberFunction startLifetime = NumberFunction.constant(100);
    @Setter
    @Getter
    
    @NumberFunctionConfig(types = {Constant.class, RandomConstant.class, Curve.class, RandomCurve.class}, defaultValue = 1f, curveConfig = @CurveConfig(bound = {-2, 2}, xAxis = "duration", yAxis = "speed"))
    protected NumberFunction startSpeed = NumberFunction.constant(1);
    @Setter
    @Getter
    
    @NumberFunction3Config(common = @NumberFunctionConfig(types = {Constant.class, RandomConstant.class, Curve.class, RandomCurve.class}, min = 0, defaultValue = 0.1f, curveConfig = @CurveConfig(bound = {0, 1}, xAxis = "duration", yAxis = "size")))
    protected NumberFunction3 startSize = new NumberFunction3(0.1, 0.1, 0.1);
    @Setter
    @Getter
    
    @NumberFunction3Config(affectX = false, affectY = false, common = @NumberFunctionConfig(types = {Constant.class, RandomConstant.class, Curve.class, RandomCurve.class}, wheelDur = 10, curveConfig = @CurveConfig(bound = {0, 360}, xAxis = "duration", yAxis = "rotation")))
    protected NumberFunction3 startRotation = new NumberFunction3(0, 0, 0);
    @Setter
    @Getter
    
    @NumberFunctionConfig(types = {Color.class, RandomColor.class, Gradient.class, RandomGradient.class}, defaultValue = -1)
    protected NumberFunction startColor = NumberFunction.color(-1);
    @Setter
    @Getter
    
    protected Space simulationSpace = Space.Local;
    @Setter
    @Getter
    
    
    protected int maxParticles = 2000;
    @Setter
    @Getter
    
    protected boolean parallelUpdate = false;
    @Setter
    @Getter
    
    protected boolean parallelRendering = false;
    
    public final EmissionSetting emission = new EmissionSetting();
    
    public final ShapeSetting shape = new ShapeSetting();
    
    public final ParticleRendererSetting renderer = new ParticleRendererSetting(this);
    
    public final PhysicsSetting physics = new PhysicsSetting();
    
    public final LightOverLifetimeSetting lights = new LightOverLifetimeSetting();
    
    public final VelocityOverLifetimeSetting velocityOverLifetime = new VelocityOverLifetimeSetting();
    
    public final InheritVelocitySetting inheritVelocity = new InheritVelocitySetting();
    
    public final LifetimeByEmitterSpeedSetting lifetimeByEmitterSpeed = new LifetimeByEmitterSpeedSetting();
    
    public final ForceOverLifetimeSetting forceOverLifetime = new ForceOverLifetimeSetting();
    
    public final ColorOverLifetimeSetting colorOverLifetime = new ColorOverLifetimeSetting();
    
    public final ColorBySpeedSetting colorBySpeed = new ColorBySpeedSetting();
    
    public final SizeOverLifetimeSetting sizeOverLifetime = new SizeOverLifetimeSetting();
    
    public final SizeBySpeedSetting sizeBySpeed = new SizeBySpeedSetting();
    
    public final RotationOverLifetimeSetting rotationOverLifetime = new RotationOverLifetimeSetting();
    
    public final RotationBySpeedSetting rotationBySpeed = new RotationBySpeedSetting();
    
    public final NoiseSetting noise = new NoiseSetting();
    
    public final UVAnimationSetting uvAnimation = new UVAnimationSetting();
    
    public final TrailsSetting trails = new TrailsSetting();
    
    public final SubEmittersSetting subEmitters = new SubEmittersSetting();
    
    public final ParticleAdditionalGPUDataSetting additionalGPUDataSetting = new ParticleAdditionalGPUDataSetting(this);

    // runtime
    public final RenderPass particleRenderType = new RenderPass();

    public enum Space {
        Local,
        World
    }

    public ParticleConfig() {
        renderer.getMaterials().add(new MaterialSetting());
    }

    @ParametersAreNonnullByDefault
    public class RenderPass extends PhotonFXRenderPass {
        private final ParticleInstanceRenderer instanceRenderer = new ParticleInstanceRenderer(ParticleConfig.this);

        public RenderPass() {
            super(renderer, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
        }

        public void clearInstance() {
            instanceRenderer.dispose();
        }

        public void drawParticlesInternal(List<MaterialSetting> materials, RenderPassPipeline pipeline, Collection<IParticle> particles, Camera camera, float partialTicks) {
            if (renderer.isUseGPUInstance()) {
                var context = renderer.getRenderMode() == ParticleRendererSetting.Mode.Model ?
                        MaterialContext.PARTICLE_MODEL_INSTANCE : MaterialContext.PARTICLE_INSTANCE;

                // upload to vbo
                if (instanceRenderer.upload((Collection) particles, camera, partialTicks)) {
                    for (MaterialSetting materialSetting : materials) {
                        materialSetting.pre();
                        renderInstanceWithMaterial(materialSetting.getMaterial(), context);
                        materialSetting.post();
                    }
                }

                // invalidate cache
                glBindVertexArray(0);
                BufferUploader.invalidate();
            } else {
                super.drawParticlesInternal(materials, pipeline, particles, camera, partialTicks);
            }
        }

        protected void renderInstanceWithMaterial(IMaterial material, MaterialContext context) {
            var shader = material.begin(context);
            RenderSystem.setShader(() -> shader);
            instanceRenderer.drawWithShader(shader);
            material.end(context);
        }

        @Override
        public boolean isParallel() {
            return isParallelRendering();
        }

        @Override
        public boolean equals(@Nonnull Object o) {
            return o instanceof RenderPass && super.equals(o);
        }
    }


    public com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction getStartLifetime() { return startLifetime; }

    public com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction getStartSpeed() { return startSpeed; }

    public com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction3 getStartSize() { return startSize; }

    public com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction3 getStartRotation() { return startRotation; }

    public com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction getStartColor() { return startColor; }
}
