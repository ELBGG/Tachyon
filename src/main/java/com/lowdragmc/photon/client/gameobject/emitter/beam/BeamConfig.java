package com.lowdragmc.photon.client.gameobject.emitter.beam;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.photon.client.gameobject.emitter.data.material.TextureMaterial;
import com.lowdragmc.photon.client.gameobject.emitter.renderpipeline.PhotonFXRenderPass;
import com.lowdragmc.photon.client.gameobject.emitter.data.LightOverLifetimeSetting;
import com.lowdragmc.photon.client.gameobject.emitter.data.MaterialSetting;
import com.lowdragmc.photon.client.gameobject.emitter.data.RendererSetting;
import com.lowdragmc.photon.client.gameobject.emitter.data.UVAnimationSetting;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.Constant;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunctionConfig;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.RandomConstant;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.color.Color;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.color.Gradient;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.color.RandomColor;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.color.RandomGradient;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.curve.Curve;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.curve.CurveConfig;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.curve.RandomCurve;
import com.mojang.blaze3d.vertex.*;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.level.ClipContext;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Optional;

/**
 * @author KilaBash
 * @date 2023/6/21
 * @implNote BeamConfig
 * @port ELB_GG 
 * @date_port 2026/03/29 
 * @port_to fabric
 */
public class BeamConfig implements IPersistedSerializable {
    @Setter
    @Getter
    
    
    protected int duration = 100;
    @Setter
    @Getter
    
    protected boolean looping = true;
    @Setter
    @Getter
    
    
    protected int startDelay = 0;
    @Getter
    
    
    protected Vector3f end = new Vector3f(0, 0, -3);
    @Setter
    @Getter
    
    @NumberFunctionConfig(types = {Constant.class, RandomConstant.class, Curve.class, RandomCurve.class}, min = 0, curveConfig = @CurveConfig(bound = {0, 1}, xAxis = "duration", yAxis = "width"))
    protected NumberFunction width = NumberFunction.constant(0.2);
    @Setter
    @Getter
    
    @NumberFunctionConfig(types = {Constant.class, RandomConstant.class, Curve.class, RandomCurve.class}, curveConfig = @CurveConfig(bound = {0, 1}, xAxis = "duration", yAxis = "width"))
    protected NumberFunction emitRate = NumberFunction.constant(0);
    @Setter
    @Getter
    
    
    protected RaycastMode raycast = RaycastMode.NONE;
    @Setter
    @Getter
    @Persisted
    protected ClipContext.Block raycastBlockMode = ClipContext.Block.VISUAL;
    @Setter
    @Getter
    @Persisted
    protected ClipContext.Fluid raycastFluidMode = ClipContext.Fluid.NONE;
    @Setter
    @Getter
    
    @NumberFunctionConfig(types = {Color.class, RandomColor.class, Gradient.class, RandomGradient.class}, defaultValue = -1)
    protected NumberFunction color = new Color();
    @Getter
    
    public final RendererSetting renderer = new RendererSetting();
    
    public final UVAnimationSetting uvAnimation = new UVAnimationSetting();
    @Getter
    
    public final LightOverLifetimeSetting lights = new LightOverLifetimeSetting();

    public enum RaycastMode {
        NONE,
        BLOCKS,
        ENTITIES,
        BLOCKS_AND_ENTITIES;
    }

    // runtime
    public final PhotonFXRenderPass particleRenderType = new RenderPass();

    public BeamConfig() {
        renderer.getMaterials().add(new MaterialSetting(new TextureMaterial()));
    }

    

    private class RenderPass extends PhotonFXRenderPass {

        public RenderPass() {
            super(renderer, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
        }


        @Override
        public boolean equals(@Nonnull Object o) {
            return o instanceof RenderPass && super.equals(o);
        }

    }

    

    

    
}
