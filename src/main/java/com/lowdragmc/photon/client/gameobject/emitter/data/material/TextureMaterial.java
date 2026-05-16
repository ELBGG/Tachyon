package com.lowdragmc.photon.client.gameobject.emitter.data.material;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.shader.LDProgramDefineManager;
import com.lowdragmc.lowdraglib2.client.shader.LDShaderInstance;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.photon.Photon;
import com.lowdragmc.photon.client.PhotonShaders;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.vfyjxf.taffy.style.AlignItems;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
@ParametersAreNonnullByDefault
@LDLRegisterClient(name = "texture", registry = "photon:material")
@Setter
@Getter
public class TextureMaterial extends ShaderInstanceMaterial {
    public enum HDRMode {
        ADDITIVE(0),
        MULTIPLICATIVE(1);
        public final int mode;

        HDRMode(int mode) {
            this.mode = mode;
        }
    }

    public static class PixelArt {
        public boolean isEnable() { return false; }
        
        
        public int bits = 8;
    }

    
    protected ResourceLocation texture = Photon.id("textures/particle/circle.png");
    
    
    protected float discardThreshold = 0.1f;
    
    
    protected Vector4f hdr = new Vector4f(0, 0, 0, 1);
    
    protected HDRMode hdrMode = HDRMode.ADDITIVE;
    
    protected final PixelArt pixelArt = new PixelArt();
    // runtime
    private static final Map<String, ShaderInstance> hdrParticleShaders = new HashMap<>();
    private static final Map<String, ShaderInstance> pixelHDRParticleShaders = new HashMap<>();

    public TextureMaterial() {
    }

    public TextureMaterial(ResourceLocation texture) {
        this.texture = texture;
    }

    @Override
    public IMaterial copy() {
        var mat = new TextureMaterial(texture);
        mat.discardThreshold = discardThreshold;
        return mat;
    }

    @Override
    public ShaderInstance getShader(MaterialContext context) {
        if (context.getShaderDefine().isEmpty()) {
            return pixelArt.isEnable() ? PhotonShaders.getPixelHDRParticleShader() : PhotonShaders.getHDRParticleShader();
        } else {
            if (pixelArt.isEnable()) {
                return pixelHDRParticleShaders.computeIfAbsent(context.getShaderDefine(), define -> {
                    // remove cache
                    Program.Type.FRAGMENT.getPrograms().remove(PhotonShaders.getPixelHDRParticleShader().getFragmentProgram().getName());
                    Program.Type.VERTEX.getPrograms().remove(PhotonShaders.getPixelHDRParticleShader().getVertexProgram().getName());
                    LDProgramDefineManager.addProgramDefine(define);
                    LDShaderInstance shader = null;
                    try {
                        shader = LDShaderInstance.create(Photon.id("pixel_hdr_particle"), DefaultVertexFormat.BLOCK);
                    } catch (Throwable e) {
                        Photon.LOGGER.error("Failed to create pixel HDR particle shader", e);
                        throw new RuntimeException(e);
                    }
                    LDProgramDefineManager.removeProgramDefine(define);
                    return shader;
                });
            } else {
                return hdrParticleShaders.computeIfAbsent(context.getShaderDefine(), define -> {
                    // remove cache
                    Program.Type.FRAGMENT.getPrograms().remove(PhotonShaders.getHDRParticleShader().getFragmentProgram().getName());
                    Program.Type.VERTEX.getPrograms().remove(PhotonShaders.getHDRParticleShader().getVertexProgram().getName());
                    LDProgramDefineManager.addProgramDefine(define);
                    LDShaderInstance shader = null;
                    try {
                        shader = LDShaderInstance.create(Photon.id("hdr_particle"), DefaultVertexFormat.BLOCK);
                    } catch (Throwable e) {
                        Photon.LOGGER.error("Failed to create HDR particle shader", e);
                        throw new RuntimeException(e);
                    }
                    LDProgramDefineManager.removeProgramDefine(define);
                    return shader;
                });
            }
        }
    }

    @Override
    public void setupUniform(MaterialContext context) {
        RenderSystem.setShaderTexture(0, texture);
        var shader = getShader(context);
        shader.safeGetUniform("DiscardThreshold").set(discardThreshold);
        if (context.isRenderingPreview()) {
            shader.safeGetUniform("HDR").set(hdr.x, hdr.y, hdr.z, 1);
            shader.safeGetUniform("HDRMode").set(hdrMode.mode);
        } else {
            shader.safeGetUniform("HDR").set(hdr.x, hdr.y, hdr.z, hdr.w);
            shader.safeGetUniform("HDRMode").set(hdrMode.mode);
        }
        if (pixelArt.isEnable()) {
            shader.safeGetUniform("Bits").set(pixelArt.bits * 1f);
        }
    }

    

    public @Nullable ResourceLocation getTextureFromFile(File filePath) {
        String fullPath = filePath.getPath().replace('\\', '/');
        int assetsIndex = fullPath.indexOf("assets/");
        if (assetsIndex == -1) {
            return null;
        } else {
            String relativePath = fullPath.substring(assetsIndex + "assets/".length());
            int slashIndex = relativePath.indexOf(47);
            if (slashIndex == -1) {
                return null;
            } else {
                String modId = relativePath.substring(0, slashIndex);
                String subPath = relativePath.substring(slashIndex + 1);
                String location = modId + ":" + subPath;
                return LDLib2.isValidResourceLocation(location) ? ResourceLocation.parse(location) : null;
            }
        }
    }
}
