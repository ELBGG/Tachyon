package com.lowdragmc.photon.client.gameobject.emitter.data;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderTypes;
import com.lowdragmc.lowdraglib2.math.noise.PerlinNoise;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.*;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.curve.Curve;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.curve.CurveConfig;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.curve.RandomCurve;
import com.lowdragmc.photon.client.gameobject.particle.IParticle;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Mth;


/**
 * @author KilaBash
 * @date 2023/5/31
 * @implNote NoiseSetting
 * @port ELB_GG 
 * @date_port 2026/03/29 
 * @port_to fabric
 */
@Environment(EnvType.CLIENT)
@Setter
@Getter
public class NoiseSetting {
    public boolean enable = true;
    public enum Quality {
        Noise1D,
        Noise2D,
        Noise3D
    }

    private final ThreadLocal<PerlinNoise> noise = ThreadLocal.withInitial(PerlinNoise::new);

    
    
    protected float frequency = 1;

    
    protected Quality quality = Quality.Noise2D;

    
    protected final Remap remap = new Remap();

    
    @NumberFunction3Config(common = @NumberFunctionConfig(types = {Constant.class, RandomConstant.class, Curve.class, RandomCurve.class}, curveConfig = @CurveConfig(bound = {0, 1}, xAxis = "lifetime", yAxis = "strength")))
    protected NumberFunction3 position = new NumberFunction3(0.1, 0.1, 0.1);

    
    @NumberFunctionConfig(types = {Constant.class, RandomConstant.class, Curve.class, RandomCurve.class}, wheelDur = 10, curveConfig = @CurveConfig(bound = {0, 180}, xAxis = "rotation amount", yAxis = "lifetime"))
    protected NumberFunction rotation = NumberFunction.constant(0);

    
    @NumberFunctionConfig(types = {Constant.class, RandomConstant.class, Curve.class, RandomCurve.class}, curveConfig = @CurveConfig(bound = {-1, 1}, xAxis = "size amount", yAxis = "lifetime"))
    protected NumberFunction size = NumberFunction.constant(0);


    public float getNoise(float t) {
        var input = t * frequency;
        float value = (float)switch (quality) {
            case Noise1D -> noise.get().noise(input);
            case Noise2D -> noise.get().noise(input, input);
            case Noise3D -> noise.get().noise(input, input, input);
        };
        if (remap.isEnable()) {
            value = remap.remapCurve.get((value + 1) / 2, () -> 0f).floatValue();
        }
        return value;
    }

    public void setupSeed(IParticle particle) {
        noise.get().setSeed(particle.getMemRandom("noise-seed", randomSource -> (float) randomSource.nextGaussian()) * 255);
    }

    public Vector3f getRotation(IParticle particle, float partialTicks) {
        setupSeed(particle);
        var t = particle.getT(partialTicks);
        var degree = rotation.get(t, () -> particle.getMemRandom("noise-rotation")).floatValue();
        if (degree != 0) {
            return new Vector3f(degree, 0, 0).mul(getNoise((t + 10 * particle.getMemRandom("noise-rotation-degree")) * 100) * Mth.TWO_PI / 360);
        }
        return new Vector3f(0 ,0, 0);
    }

    public Vector3f getSize(IParticle particle, float partialTicks) {
        setupSeed(particle);
        var t = particle.getT(partialTicks);
        var scale = size.get(t, () -> particle.getMemRandom("noise-size")).floatValue();
        if (scale != 0) {
            return new Vector3f(scale, scale, scale).mul(getNoise((t + 10 * particle.getMemRandom("noise-size-scale")) * 100));
        }
        return new Vector3f(0 ,0, 0);
    }

    public Vector3f getPosition(IParticle particle, float partialTicks) {
        setupSeed(particle);
        var t = particle.getT(partialTicks);
        var offset = position.get(t, () -> particle.getMemRandom("noise-position"));
        if (!(offset.x == 0 && offset.y == 0 && offset.z == 0)) {
            offset.mul(
                    getNoise((t + 10 * particle.getMemRandom("noise-position-x")) * 100),
                    getNoise((t + 10 * particle.getMemRandom("noise-position-y")) * 100),
                    getNoise((t + 10 * particle.getMemRandom("noise-position-z")) * 100));
            return offset;
        }
        return new Vector3f(0 ,0, 0);
    }

    


    public static class Remap {
        public boolean isEnable() { return true; }
        @Setter
        @Getter
        
        @NumberFunctionConfig(types = {Curve.class}, defaultValue = 1f, curveConfig = @CurveConfig(bound = {-1, 1}, xAxis = "base noise", yAxis = "remap result"))
        protected NumberFunction remapCurve = new Curve(Integer.MIN_VALUE, Integer.MAX_VALUE, -1, 1, 1f, "base noise", "remap result");
    }

    
}
