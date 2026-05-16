package com.lowdragmc.photon.client.gameobject.emitter.trail;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.photon.client.gameobject.emitter.renderpipeline.PhotonFXRenderPass;
import com.lowdragmc.photon.client.gameobject.emitter.data.LightOverLifetimeSetting;
import com.lowdragmc.photon.client.gameobject.emitter.data.MaterialSetting;
import com.lowdragmc.photon.client.gameobject.emitter.data.RendererSetting;
import com.lowdragmc.photon.client.gameobject.emitter.data.UVAnimationSetting;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.Constant;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunctionConfig;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.color.Color;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.color.Gradient;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.color.RandomColor;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.color.RandomGradient;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.curve.Curve;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.curve.CurveConfig;
import com.lowdragmc.photon.client.gameobject.particle.TrailParticle;
import com.mojang.blaze3d.vertex.*;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;

/**
 * @author KilaBash
 * @date 2023/6/11
 * @implNote TrailConfig
 * @port ELB_GG 
 * @date_port 2026/03/29 
 * @port_to fabric
 */
public class TrailConfig implements IPersistedSerializable {
    @Setter
    @Getter
    
    
    protected int duration = 100;
    @Setter
    @Getter
    
    protected boolean looping = true;
    @Setter
    @Getter
    
    
    protected int startDelay = 0;
    @Setter
    @Getter
    
    
    protected int time = 20;
    @Setter
    @Getter
    
    
    protected float minVertexDistance = 0.05f;
    @Getter
    
    protected boolean smoothInterpolation = false;
    @Getter
//    
    protected boolean calculateSmoothByShader = false;
    @Setter
    @Getter
    
    protected boolean parallelRendering = false;
    @Setter
    @Getter
    
    protected TrailParticle.UVMode uvMode = TrailParticle.UVMode.Stretch;
    @Setter
    @Getter
    
    @NumberFunctionConfig(types = {Constant.class, Curve.class}, min = 0, defaultValue = 0.1f, curveConfig = @CurveConfig(bound = {0, 0.1f}, xAxis = "trail position", yAxis = "width"))
    protected NumberFunction widthOverTrail = NumberFunction.constant(0.2f);
    @Setter
    @Getter
    
    @NumberFunctionConfig(types = {Color.class, RandomColor.class, Gradient.class, RandomGradient.class}, defaultValue = -1)
    protected NumberFunction colorOverTrail = new Gradient();
    @Getter
    
    public final RendererSetting renderer = new RendererSetting();
    @Getter
    
    public final LightOverLifetimeSetting lights = new LightOverLifetimeSetting();
    @Getter
    
    public final UVAnimationSetting uvAnimation = new UVAnimationSetting();

    // runtime
    public final PhotonFXRenderPass particleRenderType = new RenderPass();

    public TrailConfig() {
        renderer.getMaterials().add(new MaterialSetting());
    }

    private class RenderPass extends PhotonFXRenderPass {

        public RenderPass() {
            super(renderer, VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.BLOCK);
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
}
