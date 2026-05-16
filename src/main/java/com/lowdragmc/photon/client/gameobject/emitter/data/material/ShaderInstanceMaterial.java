package com.lowdragmc.photon.client.gameobject.emitter.data.material;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.HolderLookup;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author KilaBash
 * @date 2023/6/1
 * @implNote ShaderInstanceMaterial
 * @port ELB_GG 
 * @date_port 2026/03/29 
 * @port_to fabric
 */
@Environment(EnvType.CLIENT)
@ParametersAreNonnullByDefault
public abstract class ShaderInstanceMaterial implements IMaterial {

    abstract public ShaderInstance getShader(MaterialContext context);

    public void setupUniform(MaterialContext context) {
    }

    @Override
    public ShaderInstance begin(MaterialContext context) {
        setupUniform(context);
        return getShader(context);
    }

    @Override
    public final CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return IMaterial.super.serializeNBT(provider);
    }

    

    

}
