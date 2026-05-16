package com.lowdragmc.photon.client.gameobject.emitter.data.number;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import dev.vfyjxf.taffy.style.FlexWrap;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import dev.vfyjxf.taffy.style.FlexDirection;

import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2023/5/26
 * @implNote RandomConstant
 * @port ELB_GG 
 * @date_port 2026/03/29 
 * @port_to fabric
 */
@LDLRegisterClient(name = "random_constant", registry = "photon:number_function")
@EqualsAndHashCode(callSuper = false)
public class RandomConstant implements NumberFunction {
    @Setter
    @Getter
    @Persisted
    private Number a, b;

    public RandomConstant() {
        a = 0;
        b = 0;
    }

    public RandomConstant(Number a, Number b) {
        this.a = a;
        this.b = b;
    }

    public void loadConfig(NumberFunctionConfig config) {
        a = (float) config.defaultValue();
        b = (float) config.defaultValue();
    }

    @Override
    public Number get(float t, Supplier<Float> lerp) {
        var min = Math.min(a.doubleValue(), b.doubleValue());
        var max = Math.max(a.doubleValue(), b.doubleValue());
        if (min == max) return max;
        return (min + lerp.get() * (max - min));
    }

    @Override
    public NumberFunction copy() {
        return new RandomConstant(a, b);
    }

    

}
