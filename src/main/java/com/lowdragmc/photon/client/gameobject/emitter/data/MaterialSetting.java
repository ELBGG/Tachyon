package com.lowdragmc.photon.client.gameobject.emitter.data;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.photon.client.gameobject.emitter.data.material.BlendMode;
import com.lowdragmc.photon.client.gameobject.emitter.data.material.IMaterial;
import com.lowdragmc.photon.client.gameobject.emitter.data.material.TextureMaterial;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import javax.annotation.Nonnull;
import java.util.Optional;

@Environment(EnvType.CLIENT)
@Getter
@Setter
@Accessors(chain = true)
@EqualsAndHashCode
public class MaterialSetting implements IPersistedSerializable {
    @Nonnull
    
    protected IMaterial material;
    
    protected final BlendMode blendMode = new BlendMode();
    
    protected boolean cull = true;
    
    protected boolean depthTest = true;
    
    protected boolean depthMask = false;

    public MaterialSetting() {
        this(new TextureMaterial());
    }

    public MaterialSetting(@Nonnull IMaterial material) {
        this.material = material;
    }

    public void pre() {
        blendMode.apply();
        if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
        if (depthTest) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
        RenderSystem.depthMask(depthMask);
    }

    public void post() {
        if (blendMode.getBlendFunc() != BlendMode.BlendFuc.ADD) {
            RenderSystem.blendEquation(BlendMode.BlendFuc.ADD.op);
        }
        blendMode.reset();
        if (!cull) RenderSystem.enableCull();
        if (!depthTest) RenderSystem.enableDepthTest();
        if (!depthMask) RenderSystem.depthMask(true);
    }


    public IMaterial getMaterial() { return material; }
    public void setMaterial(IMaterial material) { this.material = material; }
    public boolean getCull() { return cull; }
    public void setCull(boolean cull) { this.cull = cull; }
    public boolean getDepthMask() { return depthMask; }
    public void setDepthMask(boolean depthMask) { this.depthMask = depthMask; }
    public boolean getDepthTest() { return depthTest; }
    public void setDepthTest(boolean depthTest) { this.depthTest = depthTest; }
}
