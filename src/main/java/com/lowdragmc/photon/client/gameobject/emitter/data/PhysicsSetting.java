package com.lowdragmc.photon.client.gameobject.emitter.data;

import com.lowdragmc.photon.client.gameobject.emitter.data.number.Constant;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunctionConfig;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.RandomConstant;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.curve.Curve;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.curve.CurveConfig;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.curve.RandomCurve;
import com.lowdragmc.photon.client.gameobject.particle.IParticle;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * @author KilaBash
 * @date 2023/5/31
 * @implNote PhysicsSetting
 * @port ELB_GG 
 * @date_port 2026/03/29 
 * @port_to fabric
 */
@Environment(EnvType.CLIENT)
@Setter
@Getter
public class PhysicsSetting {
    public boolean enable = false;

    
    protected boolean hasCollision = true;
    
    protected boolean removedWhenCollided = false;
    
    @NumberFunctionConfig(types = {Constant.class, RandomConstant.class, Curve.class, RandomCurve.class}, min = 0, max = 1, defaultValue = 1, curveConfig = @CurveConfig(xAxis = "duration", yAxis = "friction"))
    protected NumberFunction friction = NumberFunction.constant(1);
    
    @NumberFunctionConfig(types = {Constant.class, RandomConstant.class, Curve.class, RandomCurve.class}, min = 0, max = 1, defaultValue = 1, curveConfig = @CurveConfig(xAxis = "duration", yAxis = "friction"))
    protected NumberFunction collidedFriction = NumberFunction.constant(0.7f);
    
    @NumberFunctionConfig(types = {Constant.class, RandomConstant.class, Curve.class, RandomCurve.class}, curveConfig = @CurveConfig(bound = {0, 1}, xAxis = "duration", yAxis = "gravity"))
    protected NumberFunction gravity = NumberFunction.constant(0);
    
    @NumberFunctionConfig(types = {Constant.class, RandomConstant.class, Curve.class, RandomCurve.class}, min = 0, max = 1, defaultValue = 1, curveConfig = @CurveConfig(xAxis = "duration", yAxis = "bounce chance"))
    protected NumberFunction bounceChance = NumberFunction.constant(1);
    
    @NumberFunctionConfig(types = {Constant.class, RandomConstant.class, Curve.class, RandomCurve.class}, min = 0, defaultValue = 1, curveConfig = @CurveConfig(bound = {0, 1}, xAxis = "duration", yAxis = "bounce rate"))
    protected NumberFunction bounceRate =NumberFunction.constant(1);
    
    @NumberFunctionConfig(types = {Constant.class, RandomConstant.class, Curve.class, RandomCurve.class}, min = 0, curveConfig = @CurveConfig(bound = {0, 1}, xAxis = "duration", yAxis = "spread"))
    protected NumberFunction bounceSpreadRate = NumberFunction.constant(0);

    public float getFriction(IParticle particle) {
        return friction.get(particle.getT(), () -> particle.getMemRandom("friction")).floatValue();
    }

    public float getCollidedFriction(IParticle particle) {
        return collidedFriction.get(particle.getT(), () -> particle.getMemRandom("collidedFriction")).floatValue();
    }

    public float getGravity(IParticle particle) {
        return gravity.get(particle.getT(), () -> particle.getMemRandom("gravity")).floatValue();
    }

    public float getBounceChance(IParticle particle) {
        return bounceChance.get(particle.getT(), () -> particle.getMemRandom("bounceChance")).floatValue();
    }

    public float getBounceRate(IParticle particle) {
        return bounceRate.get(particle.getT(), () -> particle.getMemRandom("bounceRate")).floatValue();
    }

    public float getBounceSpreadRate(IParticle particle) {
        return bounceSpreadRate.get(particle.getT(), () -> particle.getMemRandom("bounceSpreadRate")).floatValue();
    }
    public boolean isEnable() { return enable; }
    public boolean isHasCollision() { return hasCollision; }

    public boolean isRemovedWhenCollided() { return removedWhenCollided; }
}
