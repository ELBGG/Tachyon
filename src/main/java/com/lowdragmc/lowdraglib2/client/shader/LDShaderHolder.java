package com.lowdragmc.lowdraglib2.client.shader;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.vfyjxf.taffy.style.AlignItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;

import org.appliedenergistics.yoga.YogaEdge;
import org.jetbrains.annotations.UnknownNullability;
import org.joml.*;

import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX_COLOR;

public class LDShaderHolder implements AutoCloseable {
    public final static String SHADER_UID_DEFINE = "LD_SHADER_%d";
    private final static AtomicInteger SHADER_ID = new AtomicInteger();

    public final String shaderUid;
    public final LDShaderInstance baseInstance;
    // runtime
    private boolean isClosed = false;
    private final Map<Set<String>, LDShaderInstance> shadersWithDefines = new HashMap<>();
    protected final Map<String, Object> samplerCache = new HashMap<>();
    protected final Map<String, Supplier<Object>> dynamicSampler = new HashMap<>();
    protected final Map<String, Consumer<Uniform>> dynamicUniform = new HashMap<>();

    private LDShaderHolder(String shaderUid, LDShaderInstance baseInstance) {
        this.shaderUid = shaderUid;
        this.baseInstance = baseInstance;
        this.baseInstance.setHolder(this);
    }

    @Nullable
    public static LDShaderHolder createSafe(ResourceLocation location, VertexFormat format) {
        try {
            return create(location, format);
        } catch (Throwable e) {
            return null;
        }
    }

    public static LDShaderHolder create(ResourceLocation location, VertexFormat format) throws Throwable {
        var currentId = SHADER_ID.get();
        var id = SHADER_UID_DEFINE.formatted(currentId);
        var shaderInstance = LDShaderInstance.create(location, format, Set.of(id));
        if (shaderInstance == null) return null;
        // if successful, increment shader id
        SHADER_ID.getAndIncrement();
        return new LDShaderHolder(id, shaderInstance);
    }

    public LDShaderInstance getShaderInstance() {
        return getShaderInstance(Collections.emptySet());
    }

    public LDShaderInstance getShaderInstance(Collection<String> defines) {
        if (defines.isEmpty()) return baseInstance;
        return shadersWithDefines.computeIfAbsent(defines.stream().collect(Collectors.toUnmodifiableSet()),
                definesKey -> {
                    var defineWithUid = new LinkedHashSet<>(definesKey);
                    defineWithUid.addFirst(shaderUid);
                    try {
                        var shader = LDShaderInstance.create(baseInstance.shaderLocation, baseInstance.getVertexFormat(), defineWithUid);
                        if (shader == null) return baseInstance;
                        shader.setHolder(this);
                        // copy uniforms from the base instance
                        for (var entry : shader.getShaderInstanceAccessor().getUniformMap().entrySet()) {
                            var name = entry.getKey();
                            var uniform = entry.getValue();
                            if (isBuiltinUniform(uniform, shader)) continue;
                            var baseUniform = baseInstance.getShaderInstanceAccessor().getUniformMap().get(name);
                            if (baseUniform != null && baseUniform.getType() == uniform.getType()) {
                                if (baseUniform.getType() <= 3) {
                                    writeInts(readInts(baseUniform), uniform);
                                } else {
                                    writeFloats(readFloats(baseUniform), uniform);
                                }
                            }
                        }
                        return shader;
                    } catch (Throwable e) {
                        return baseInstance;
                    }
                });
    }

    @Override
    public void close() {
        if (isClosed) return;
        baseInstance.close();
        shadersWithDefines.values().forEach(LDShaderInstance::close);
        isClosed = true;
    }

    public void removeDynamicSampler(String name) {
        dynamicSampler.remove(name);
    }

    public void removeDynamicUniform(String name) {
        dynamicUniform.remove(name);
    }

    public void addDynamicSampler(String name, Supplier<Object> supplier) {
        dynamicSampler.put(name, supplier);
    }

    public void addDynamicUniform(String name, Consumer<Uniform> consumer) {
        dynamicUniform.put(name, consumer);
    }

    private void setSamplerCache(String samplerName, Object sampler) {
        samplerCache.put(samplerName, sampler);
        markAllShaderSamplerDirty();
    }

    private Stream<LDShaderInstance> allShaders() {
        return Stream.concat(Stream.of(baseInstance), shadersWithDefines.values().stream());
    }

    private Stream<AbstractUniform> allUniforms(String name) {
        return allShaders().map(shader -> shader.safeGetUniform(name));
    }

    private void markAllShaderSamplerDirty() {
        allShaders().forEach(LDShaderInstance::markSamplerCacheDirty);
    }

    @Nullable
    public CompoundTag serializeSampler(Object sampler) {
        CompoundTag tag = new CompoundTag();
        if (sampler instanceof ResourceLocation textureLocation) {
            tag.putString("type", "texture");
            tag.putString("resource", textureLocation.toString());
            return tag;
        }
        return null;
    }

    @Nullable
    public Object deserializeSampler(CompoundTag tag) {
        var type  = tag.getString("type");
        if (type.equals("texture")) {
            return ResourceLocation.parse(tag.getString("resource"));
        }
        return null;
    }

    public @UnknownNullability CompoundTag serializeNBT(@Nonnull HolderLookup.Provider provider) {
        var tag = new CompoundTag();
        // uniform
        var uniforms = new CompoundTag();
        for (var entry : baseInstance.getShaderInstanceAccessor().getUniformMap().entrySet()) {
            var name = entry.getKey();
            var uniform = entry.getValue();
            if (isBuiltinUniform(uniform, baseInstance)) continue;
            if (uniform.getType() <= 3) {
                uniforms.put(name, new IntArrayTag(readInts(uniform)));
            } else {
                var list = new ListTag();
                for (var v : readFloats(uniform)) {
                    list.add(FloatTag.valueOf(v));
                }
                uniforms.put(name, list);
            }
        }
        tag.put("uniforms", uniforms);

        var samplers = new CompoundTag();
        for (var entry : samplerCache.entrySet()) {
            var name = entry.getKey();
            var samplerData = serializeSampler(entry.getValue());
            if (samplerData != null) {
                samplers.put(name, samplerData);
            }
        }
        tag.put("samplers", samplers);

        return tag;
    }

    public void deserializeNBT(@Nonnull HolderLookup.Provider provider, @Nonnull CompoundTag tag) {
        samplerCache.clear();
        dynamicSampler.clear();
        dynamicUniform.clear();
        markAllShaderSamplerDirty();

        var uniforms = tag.getCompound("uniforms");
        for (var name : uniforms.getAllKeys()) {
            var uniform = baseInstance.getUniform(name);
            if (uniform == null) continue;
            var data = uniforms.get(name);
            if (data instanceof IntArrayTag intArrayTag) {
                var intArray = intArrayTag.getAsIntArray();
            } else if (data instanceof ListTag floatArrayTag) {
                var floatArray = new float[floatArrayTag.size()];
                for (int i = 0; i < floatArrayTag.size(); i++) {
                    floatArray[i] = floatArrayTag.getFloat(i);
                }
            }
        }

        var samplers = tag.getCompound("samplers");
        for (var name : samplers.getAllKeys()) {
            var sampler = deserializeSampler(samplers.getCompound(name));
            if (sampler == null) continue;
            samplerCache.put(name, sampler);
        }
    }

    private int[] readInts(Uniform uniform) {
        if (uniform.getType() > 3) return new int[0];
        var buffer = uniform.getIntBuffer().duplicate();
        var count = uniform.getCount();
        var result = new int[count];
        buffer.position(0);
        buffer.get(result);
        return result;
    }

    private float[] readFloats(Uniform uniform) {
        if (uniform.getType() <= 3) return new float[0];
        var buffer = uniform.getFloatBuffer().duplicate();
        var count = uniform.getCount();
        var result = new float[count];
        buffer.position(0);
        buffer.get(result);
        return result;
    }

    private void writeInts(int[] intArray, AbstractUniform uniform) {
        if (uniform instanceof Uniform u && intArray.length > u.getCount()) {
            LDLib2.LOGGER.warn("Uniform.set called with a too-large value array (expected {}, got {}). Ignoring.", u.getCount(), intArray.length);
        } else if (intArray.length == 1) {
            uniform.set(intArray[0]);
        } else if (intArray.length == 2) {
            uniform.set(intArray[0], intArray[1]);
        } else if (intArray.length == 3) {
            uniform.set(intArray[0], intArray[1], intArray[2]);
        } else if (intArray.length == 4) {
            uniform.set(intArray[0], intArray[1], intArray[2], intArray[3]);
        }
    }

    private void writeFloats(float[] floatArray, AbstractUniform uniform) {
        uniform.set(floatArray);
    }

    public boolean isBuiltinSampler(String name) {
        return name.startsWith("Sampler");
    }

    public boolean isBuiltinUniform(Uniform uniform, ShaderInstance shaderInstance) {
        return uniform.getName().startsWith("U_") ||
                uniform == shaderInstance.MODEL_VIEW_MATRIX ||
                uniform == shaderInstance.PROJECTION_MATRIX ||
                uniform == shaderInstance.TEXTURE_MATRIX ||
                uniform == shaderInstance.SCREEN_SIZE ||
                uniform == shaderInstance.COLOR_MODULATOR ||
                uniform == shaderInstance.LIGHT0_DIRECTION ||
                uniform == shaderInstance.LIGHT1_DIRECTION ||
                uniform == shaderInstance.GLINT_ALPHA ||
                uniform == shaderInstance.FOG_START ||
                uniform == shaderInstance.FOG_END ||
                uniform == shaderInstance.FOG_COLOR ||
                uniform == shaderInstance.FOG_SHAPE ||
                uniform == shaderInstance.LINE_WIDTH ||
                uniform == shaderInstance.GAME_TIME ||
                uniform == shaderInstance.CHUNK_OFFSET;
    }


    private int getSamplerID(String name) {
        var sampler = samplerCache.get(name);
        if (sampler instanceof ResourceLocation location) {
            return Minecraft.getInstance().getTextureManager().getTexture(location).getId();
        } if (sampler instanceof RenderTarget renderTarget) {
            return renderTarget.getColorTextureId();
        }
        return -1;
    }

}
