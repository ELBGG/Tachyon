package com.lowdragmc.photon.client.gameobject.emitter.data;

import com.lowdragmc.lowdraglib2.syncdata.annotation.ReadOnlyManaged;
import com.lowdragmc.photon.client.gameobject.emitter.Emitter;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.IntTag;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RendererSetting {

    public enum Layer {
        Opaque,
        Translucent
    }

    public enum SortMode {
        NONE(() -> null),
        DISTANCE(RenderSystem::getVertexSorting);
        public final Supplier<VertexSorting> vertexSorting;

        SortMode(Supplier<VertexSorting> vertexSorting) {
            this.vertexSorting = vertexSorting;
        }

        public VertexSorting getVertexSorting() {
            return vertexSorting.get();
        }

    }

    
    
    @ReadOnlyManaged(serializeMethod = "materialSerialize", deserializeMethod = "materialDeserialize")
    @EqualsAndHashCode.Include
    protected List<MaterialSetting> materials = new ArrayList<>();

    
    @EqualsAndHashCode.Include
    protected Layer layer = Layer.Translucent;

    
    protected final Cull cull = new Cull();

    
    @EqualsAndHashCode.Include
    protected int orderInLayer = 0;

    
    @EqualsAndHashCode.Include
    protected SortMode vertexSortingMode = SortMode.NONE;

    public static class Cull {
        public boolean enable = true;
        public boolean isEnable() { return enable; }
        @Setter
        @Getter
        
        
        protected AABB cullBox = new AABB(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);

        public AABB getCullAABB(Emitter particle, float partialTicks) {
            var transform = particle.transform();
            var scale = transform.scale();
            var pos = transform.position();
            return new AABB(cullBox.minX * scale.x, cullBox.minY * scale.y, cullBox.minZ * scale.z,
                    cullBox.maxX * scale.x, cullBox.maxY * scale.y, cullBox.maxZ * scale.z)
                    .move(pos.x, pos.y, pos.z);
        }
    }

    

    private MaterialSetting addDefaultMaterial() {
        return new MaterialSetting();
    }

    private IntTag materialSerialize(List<MaterialSetting> value) {
        return IntTag.valueOf(value.size());
    }

    private List<MaterialSetting> materialDeserialize(IntTag size) {
        var materials = new ArrayList<MaterialSetting>();
        for (int i = 0; i < size.getAsInt(); i++) {
            materials.add(addDefaultMaterial());
        }
        return materials;
    }


    public List<MaterialSetting> getMaterials() { return materials; }
    public Cull getCull() { return cull; }
    public Layer getLayer() { return layer; }
    public int getOrderInLayer() { return orderInLayer; }
    public SortMode getVertexSortingMode() { return vertexSortingMode; }
}
