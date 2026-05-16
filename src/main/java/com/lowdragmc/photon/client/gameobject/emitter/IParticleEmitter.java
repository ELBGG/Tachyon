package com.lowdragmc.photon.client.gameobject.emitter;

import com.lowdragmc.photon.client.gameobject.IFXObject;
import net.minecraft.network.chat.Component;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;


/**
 * @author KilaBash
 * @date 2023/6/2
 * @implNote IParticleEmitter
 * @port ELB_GG 
 * @date_port 2026/03/29 
 * @port_to fabric
 */
@Environment(EnvType.CLIENT)
public interface IParticleEmitter extends IFXObject {

    default Emitter self() {
        return (Emitter) this;
    }

    /**
     * get amount of existing particle which emitted from it.
     */
    int getParticleAmount();

    Vector3f getVelocity();

    /**
     * get the box of cull.
     * <br>
     * return null - culling disabled.
     */
    @Nullable
    default AABB getCullBox(float partialTicks) {
        return null;
    }

    int getAge();

    void setAge(int age);

    boolean isLooping();

    void setRGBAColor(Vector4f color);

    Vector4f getRGBAColor();

    float getT();

    float getT(float partialTicks);

    float getMemRandom(Object object);

    float getMemRandom(Object object, Function<RandomSource, Float> randomFunc);

    int getLightColor(BlockPos pos);

    RandomSource getRandomSource();


}
