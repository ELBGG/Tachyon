package com.lowdragmc.photon.client.gameobject.emitter.data.shape;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.client.model.ModelFactory;
import com.lowdragmc.lowdraglib2.client.renderer.impl.IModelRenderer;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.utils.data.BlockInfo;
import com.lowdragmc.lowdraglib2.utils.virtuallevel.TrackedDummyWorld;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.vfyjxf.taffy.style.AlignItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import org.joml.Vector3f;
import lombok.Getter;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class MeshData implements IPersistedSerializable {
    @Getter
    
    private ResourceLocation modelLocation = ResourceLocation.withDefaultNamespace("block/stone");
    // runtime
    private boolean isLoaded = false;
    private final List<Vector3f> vertices = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private final List<Triangle> triangles = new ArrayList<>();
    @Getter
    private double edgeSumLength;
    @Getter
    private double triangleSumArea;

    public MeshData() {
    }

    public MeshData(CompoundTag nbt) {
        deserializeNBT(Platform.getFrozenRegistry(), nbt);
    }

    public MeshData(ResourceLocation modelLocation) {
        loadFromModel(modelLocation);
    }

    
    public void setModelLocation(ResourceLocation modelLocation) {
        this.modelLocation = modelLocation;
        clear();
    }

    public void clear() {
        isLoaded = false;
        vertices.clear();
        edges.clear();
        triangles.clear();
        edgeSumLength = 0;
        triangleSumArea = 0;
    }

    private void loadFromModel(ResourceLocation modelLocation) {
        var random = RandomSource.create();
        var bakedModel = ModelFactory.getUnBakedModel(modelLocation).bake(
                ModelFactory.getModelBaker(),
                Material::sprite,
                BlockModelRotation.X0_Y0);
        if (bakedModel == null) {
            bakedModel = ModelFactory.getUnBakedModel(ResourceLocation.withDefaultNamespace("block/stone")).bake(
                    ModelFactory.getModelBaker(),
                    Material::sprite,
                    BlockModelRotation.X0_Y0);
        }
        var quads = new ArrayList<>(bakedModel.getQuads(null, null, random));
        for (var side : Direction.values()) {
            quads.addAll(bakedModel.getQuads(null, side, random));
        }
        loadFromQuads(quads);
    }

    private void loadFromQuads(List<BakedQuad> quads) {
        // do not access the model during reloading
        if (Minecraft.getInstance().getOverlay() instanceof LoadingOverlay) {
            return;
        }
        clear();
        double sumLength = 0;
        double sumArea = 0;
        for (var quad : quads) {
            var vertices = quad.getVertices();
            Vector3f[] points = new Vector3f[4];
            for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
                int offset = vertexIndex * 8 + 0;
                points[vertexIndex] = new Vector3f(Float.intBitsToFloat(vertices[offset]) - 0.5f,
                        Float.intBitsToFloat(vertices[offset + 1]) - 0.5f,
                        Float.intBitsToFloat(vertices[offset + 2]) - 0.5f);
                // add vertexes
                this.vertices.add(points[vertexIndex]);
            }
            // add edges
            sumLength += addEdge(points[0], points[1]);
            sumLength += addEdge(points[1], points[2]);
            sumLength += addEdge(points[2], points[3]);
            sumLength += addEdge(points[3], points[0]);
            sumLength += addEdge(points[1], points[3]);
            // add triangles
            sumArea += addTriangle(points[0], points[1], points[2]);
            sumArea += addTriangle(points[2], points[3], points[0]);
        }
        this.edgeSumLength = sumLength;
        this.triangleSumArea = sumArea;
        isLoaded = true;
    }

    private void ensureLoaded() {
        if (!isLoaded) {
            loadFromModel(modelLocation);
        }
    }

    public List<Vector3f> getVertices() {
        ensureLoaded();
        return vertices;
    }

    public List<Edge> getEdges() {
        ensureLoaded();
        return edges;
    }

    public List<Triangle> getTriangles() {
        ensureLoaded();
        return triangles;
    }

    @Nullable
    public Vector3f getRandomVertex(float t) {
        ensureLoaded();
        if (vertices.isEmpty()) return null;
        return vertices.get((int) (vertices.size() * t));
    }

    @Nullable
    public Edge getRandomEdge(float t) {
        ensureLoaded();
        if (edges.isEmpty()) return null;
        var l = t * edgeSumLength;
        var cl = 0d;
        for (Edge edge : edges) {
            if (l <= edge.length + cl) {
                return edge;
            }
            cl += edge.length;
        }
        return edges.getLast();
    }

    @Nullable
    public Triangle getRandomTriangle(float t) {
        ensureLoaded();
        if (triangles.isEmpty()) return null;
        var a = t * triangleSumArea;
        var ca = 0d;
        for (var triangle : triangles) {
            if (a <= triangle.area + ca) {
                return triangle;
            }
            ca += triangle.area;
        }
        return triangles.getLast();
    }

    private double addEdge(Vector3f a, Vector3f b) {
        var ab = new Edge(a, b);
        if (ab.length > 0) {
            edges.add(ab);
        }
        return ab.length;
    }

    private double addTriangle(Vector3f a, Vector3f b, Vector3f c) {
        var abc = new Triangle(a, b, c);
        if (abc.area > 0) {
            triangles.add(abc);
        }
        return abc.area;
    }

    
    

    
    public void drawLineFrames(PoseStack poseStack) {
        var edges = getEdges();
        if (edges.isEmpty()) return;
        var tessellator = Tesselator.getInstance();
        var pose = poseStack.last();
        var mat = pose.pose();

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
        var buffer = tessellator.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        RenderSystem.lineWidth(10);

        for (var edge : edges) {
            var a = edge.a;
            var b = edge.b;
            float f = b.x - a.x;
            float f1 = b.y - a.y;
            float f2 = b.z - a.z;
            float f3 = Mth.sqrt(f * f + f1 * f1 + f2 * f2);
            f /= f3;
            f1 /= f3;
            f2 /= f3;

            buffer.addVertex(mat, a.x + 0.5f, a.y + 0.5f, a.z + 0.5f).setColor(-1).setNormal(poseStack.last(), f, f1, f2);
            buffer.addVertex(mat, b.x + 0.5f, b.y + 0.5f, b.z + 0.5f).setColor(-1).setNormal(poseStack.last(), f, f1, f2);
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }

    

    public static class Edge {

        public final Vector3f a, b;

        public final double length;

        public Edge(Vector3f a, Vector3f b) {
            this.a = a;
            this.b = b;
            length = new Vector3f(a).sub(b).length();
        }
    }

    public static class Triangle {

        public final Vector3f a, b, c;

        public final double area;

        public Triangle(Vector3f a, Vector3f b, Vector3f c) {
            this.a = a;
            this.b = b;
            this.c = c;
            var nx = (b.y - a.y) * (c.z - a.z) - (b.z - a.z) * (c.y - a.y);
            var ny = (b.z - a.z) * (c.x - a.x) - (b.x - a.x) * (c.z - a.z);
            var nz = (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
            area = 0.5 * Math.sqrt(nx * nx + ny * ny + nz * nz);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MeshData meshData = (MeshData) o;
        return Objects.equals(modelLocation, meshData.modelLocation);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(modelLocation);
    }

    @Override
    public CompoundTag serializeToCompound() {
        var tag = new CompoundTag();
        tag.putString("modelLocation", modelLocation.toString());
        return tag;
    }

    @Override
    public void deserializeFromCompound(CompoundTag tag) {
        if (tag.contains("modelLocation")) {
            setModelLocation(ResourceLocation.parse(tag.getString("modelLocation")));
        }
    }
}
