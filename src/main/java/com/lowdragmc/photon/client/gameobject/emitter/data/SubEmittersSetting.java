package com.lowdragmc.photon.client.gameobject.emitter.data;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.ReadOnlyManaged;
import com.lowdragmc.photon.client.fx.FXHelper;
import com.lowdragmc.photon.client.gameobject.emitter.IParticleEmitter;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.Constant;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunctionConfig;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.RandomConstant;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.curve.Curve;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.curve.CurveConfig;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.curve.RandomCurve;
import com.lowdragmc.photon.client.gameobject.particle.TileParticle;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.IntTag;
import net.minecraft.resources.ResourceLocation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Quaternionf;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2023/7/17
 * @implNote SubEmittersSetting
 * @port ELB_GG 
 * @date_port 2026/03/29 
 * @port_to fabric
 */
@Environment(EnvType.CLIENT)
@Setter
@Getter
public class SubEmittersSetting {
    public boolean enable = false;

    
    
    @ReadOnlyManaged(serializeMethod = "emittersSerialize", deserializeMethod = "emittersDeserialize")
    protected List<Emitter> emitters = new ArrayList<>();

    public void triggerEvent(TileParticle father, Event event) {
        for (Emitter candidate : emitters) {
            if (candidate.event == event) {
                candidate.spawnEmitter(father);
            }
        }
    }

    

    private Emitter addDefaultEmitter() {
        return new Emitter();
    }

    private IntTag emittersSerialize(List<Emitter> bursts) {
        return IntTag.valueOf(bursts.size());
    }

    private List<Emitter> emittersDeserialize(IntTag tag) {
        var groups = new ArrayList<Emitter>();
        for (int i = 0; i < tag.getAsInt(); i++) {
            groups.add(addDefaultEmitter());
        }
        return groups;
    }

    public enum Event {
        Birth,
        Death,
        Collision,
        FirstCollision,
        Tick
    }

    public static class Emitter implements IPersistedSerializable {
        @Nullable
        @Persisted
        protected ResourceLocation fxLocation = null;
        
        protected Event event = Event.Birth;
        
        @NumberFunctionConfig(types = {Constant.class, RandomConstant.class, Curve.class, RandomCurve.class}, min = 0, max = 1, curveConfig = @CurveConfig(bound = {0, 1}, xAxis = "probability", yAxis = "lifetime"))
        protected NumberFunction emitProbability = NumberFunction.constant(0);
        
        
        protected int tickInterval = 1;
        
        protected boolean inheritColor = false;
        
        protected boolean inheritSize = false;
        
        protected boolean inheritRotation = false;
        
        protected boolean inheritLifetime = false;
        
        protected boolean inheritDuration = false;

        public void spawnEmitter(TileParticle father) {
            if (fxLocation != null && father.getAge() % tickInterval == 0 &&
                    father.getRandomSource().nextFloat() < emitProbability.get(father.getT(0),
                            () -> father.getMemRandom("sub_emitter_probability")).floatValue()) {
                var fx = FXHelper.getFX(fxLocation);
                if (fx == null) return;
                var runtime = fx.createRuntime();
                runtime.root.updatePos(father.getWorldPos());
                for (var value : runtime.objects.values()) {
                    if (value instanceof IParticleEmitter particleEmitter) {
                        if (inheritLifetime) {
                            particleEmitter.setAge(father.getAge());
                        }
                        if (inheritDuration) {
                            particleEmitter.self().setLifetime(father.getLifetime());
                        }
                        if (inheritColor) {
                            particleEmitter.setRGBAColor(father.getRealColor(0));
                        }
                        if (inheritSize) {
                            particleEmitter.transform().scale(father.getRealSize(0));
                        }
                        if (inheritRotation) {
                            var xyz = father.getRealRotation(0);
                            particleEmitter.transform().rotation(new Quaternionf().rotationXYZ(xyz.x, xyz.y, xyz.z));
                        }
                    }
                }
                runtime.emmit(father.getEmitter().getEffectExecutor());
            }
        }

        
    }
    public boolean isEnable() { return enable; }
}
