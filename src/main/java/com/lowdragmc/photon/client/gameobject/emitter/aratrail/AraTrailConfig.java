package com.lowdragmc.photon.client.gameobject.emitter.aratrail;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.photon.client.gameobject.emitter.data.MaterialSetting;
import com.lowdragmc.photon.client.gameobject.emitter.data.RendererSetting;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.Constant;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunctionConfig;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.color.Color;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.color.Gradient;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.color.RandomColor;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.color.RandomGradient;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.curve.Curve;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.curve.CurveConfig;
import com.lowdragmc.photon.client.gameobject.emitter.renderpipeline.PhotonFXRenderPass;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import javax.annotation.Nonnull;

/**
 * @author KilaBash
 * @date 2023/6/11
 * @implNote TrailConfig
 * @port ELB_GG 
 * @date_port 2026/03/29 
 * @port_to fabric
 */
public class AraTrailConfig implements IPersistedSerializable {

    public enum TrailAlignment {
        View,
        Velocity,
        Local
    }

    public enum TrailSpace
    {
        World,
        Local,
        Custom
    }

    public enum TrailSorting {
        OlderOnTop,
        NewerOnTop
    }

    public enum Timescale {
        Normal,
        Unscaled
    }

    public enum TextureMode {
        Stretch,
        Tile,
        WorldTile
    }
    @Setter
    @Getter
    
    
    protected int duration = 100;
    @Setter
    @Getter
    
    protected boolean looping = true;
    
    public final TrailSection section = new TrailSection();
    
    
    public TrailSpace space = TrailSpace.World;
    @Persisted
    
    
    public TrailAlignment alignment = TrailAlignment.View;
    
    public TrailSorting sorting = TrailSorting.OlderOnTop;
    
    
    public float thickness = 0.2f;
    
    
    public int smoothness = 1;
    
    
    public float smoothingDistance = 0.05f;
    
    public boolean highQualityCorners = false;
    
    
    public int cornerRoundness = 5;

    
    
    @NumberFunctionConfig(types = {Constant.class, Curve.class}, min = 0, defaultValue = 1f, curveConfig = @CurveConfig(bound = {0, 1f}, xAxis = "trail length", yAxis = "thickness"))
    public NumberFunction thicknessOverLength = NumberFunction.constant(1f);    /**< maps trail length to thickness.*/
    
    @NumberFunctionConfig(types = {Color.class, RandomColor.class, Gradient.class, RandomGradient.class}, defaultValue = -1)
    public NumberFunction colorOverLength = NumberFunction.color(-1);

    
    
    @NumberFunctionConfig(types = {Constant.class, Curve.class}, min = 0, defaultValue = 1f, curveConfig = @CurveConfig(bound = {0, 1f}, xAxis = "trail length", yAxis = "thickness"))
    public NumberFunction thicknessOverTime = NumberFunction.constant(1f);  /**< maps trail lifetime to thickness.*/
    
    @NumberFunctionConfig(types = {Constant.class, Curve.class}, min = 0, defaultValue = 1f, curveConfig = @CurveConfig(bound = {0, 1f}, xAxis = "trail length", yAxis = "thickness"))
    public NumberFunction thicknessOverSegmentTime = NumberFunction.constant(1f);  /**< maps segment lifetime to thickness.*/
    
    @NumberFunctionConfig(types = {Color.class, RandomColor.class, Gradient.class, RandomGradient.class}, defaultValue = -1)
    public NumberFunction colorOverTime = NumberFunction.color(-1);
    
    @NumberFunctionConfig(types = {Color.class, RandomColor.class, Gradient.class, RandomGradient.class}, defaultValue = -1)
    public NumberFunction colorOverSegmentTime = NumberFunction.color(-1);


    
    
    public boolean emit = true;
    
    
    public float initialThickness = 1;
    
    
    public int initialColor = -1;
    
    public Vector3f initialVelocity = new Vector3f(0, 0, 0);
    
    
    public float timeInterval = 0.05f;
    
    
    public float minDistance = 0.025f;
    
    
    public float time = 1f;

    
    
    public final AraPhysicsSetting physicsSetting = new AraPhysicsSetting();

    
    
    public TextureMode textureMode = TextureMode.Stretch;
    
    public float uvFactor = 1;
    
    public float uvWidthFactor = 1;
    
    
    public float tileAnchor = 1;
    @Getter
    
    public final RendererSetting renderer = new RendererSetting();

    // runtime
    public final PhotonFXRenderPass particleRenderType = new RenderPass();

    public AraTrailConfig() {
        renderer.getMaterials().add(new MaterialSetting());
    }

    private class RenderPass extends PhotonFXRenderPass {

        public RenderPass() {
            super(renderer, VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.BLOCK);
        }

        @Override
        public boolean equals(@Nonnull Object o) {
            return o instanceof RenderPass && super.equals(o);
        }
    }

    

    public boolean isLooping() { return looping; }
    public com.lowdragmc.photon.client.gameobject.emitter.particle.ParticleConfig.Space simulationSpace = com.lowdragmc.photon.client.gameobject.emitter.particle.ParticleConfig.Space.Local;
    public com.lowdragmc.photon.client.gameobject.emitter.particle.ParticleConfig.Space getSimulationSpace() { return simulationSpace; }
}
