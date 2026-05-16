package com.lowdragmc.photon.client.gameobject.emitter.data;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.photon.PhotonRegistries;
import com.lowdragmc.photon.client.gameobject.emitter.IParticleEmitter;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.curve.Curve;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.curve.CurveConfig;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.curve.RandomCurve;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.*;
import com.lowdragmc.photon.client.gameobject.emitter.data.shape.Cone;
import com.lowdragmc.photon.client.gameobject.emitter.data.shape.IShape;
import com.lowdragmc.photon.client.gameobject.particle.TileParticle;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * @author KilaBash
 * @date 2023/5/27
 * @implNote Shape
 * @port ELB_GG 
 * @date_port 2026/03/29 
 * @port_to fabric
 */
@Environment(EnvType.CLIENT)
@Getter
@Setter
public class ShapeSetting implements IPersistedSerializable {

    @Persisted
    private IShape shape = new Cone();

    
    @NumberFunction3Config(allowSeperated = false, isSeperatedDefault = true, common = @NumberFunctionConfig(types = {Constant.class, RandomConstant.class, Curve.class, RandomCurve.class}, min = -1000, max = 1000, curveConfig = @CurveConfig(bound = {-3, 3}, xAxis = "duration", yAxis = "position")))
    private NumberFunction3 position = new NumberFunction3(0 ,0, 0);

    
    @NumberFunction3Config(allowSeperated = false, isSeperatedDefault = true, common = @NumberFunctionConfig(types = {Constant.class, RandomConstant.class, Curve.class, RandomCurve.class}, wheelDur = 10, min = -Float.MAX_VALUE, max = Float.MAX_VALUE, curveConfig = @CurveConfig(bound = {-180, 180}, xAxis = "duration", yAxis = "rotation")))
    private NumberFunction3 rotation = new NumberFunction3(0 ,0, 0);

    
    @NumberFunction3Config(allowSeperated = false, isSeperatedDefault = true, common = @NumberFunctionConfig(types = {Constant.class, RandomConstant.class, Curve.class, RandomCurve.class}, min = 0, max = 1000, curveConfig = @CurveConfig(bound = {0, 3}, xAxis = "duration", yAxis = "scale")))
    private NumberFunction3 scale = new NumberFunction3(1, 1, 1);

    public void setupParticle(TileParticle particle, IParticleEmitter emitter) {
        var t = emitter.getT();
        shape.nextPosVel(particle, emitter,
                position.get(t, () -> emitter.getMemRandom("shape_position")),
                new Vector3f(rotation.get(t, () -> emitter.getMemRandom("shape_rotation")).mul(Mth.TWO_PI / 360)),
                new Vector3f(scale.get(t, () -> emitter.getMemRandom("shape_scale"))));
    }

    

    public void drawGuideLines(MultiBufferSource bufferSource, float partialTicks, IParticleEmitter emitter) {
        var poseStack = new PoseStack();
        poseStack.mulPose(emitter.transform().localToWorldMatrix());
        var t = emitter.getT(partialTicks);
        shape.drawGuideLines(poseStack, bufferSource, partialTicks, emitter,
                position.get(t, () -> emitter.getMemRandom("shape_position")),
                new Vector3f(rotation.get(t, () -> emitter.getMemRandom("shape_rotation")).mul(Mth.TWO_PI / 360)),
                new Vector3f(scale.get(t, () -> emitter.getMemRandom("shape_scale"))));
    }
}
