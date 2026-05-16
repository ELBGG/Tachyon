package com.lowdragmc.photon.client.gameobject.emitter.aratrail;

import lombok.Getter;
import lombok.Setter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector3f;

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
public class AraPhysicsSetting {
    public boolean enable = true;
    
    public float warmup = 0;               /**< simulation warmup seconds.*/
    
    public Vector3f gravity = new Vector3f();  /**< gravity applied to the trail, in world space. */
    
    
    public float inertia = 0;               /**< amount of GameObject velocity transferred to the trail.*/
    
    
    public float velocitySmoothing = 0.75f;     /**< velocity smoothing amount.*/
    
    
    public float damping = 0.75f;               /**< velocity damping amount.*/
    public boolean isEnable() { return enable; }
}
