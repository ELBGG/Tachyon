package com.lowdragmc.photon.client.gameobject.emitter.aratrail;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * Trail cross-section shape definition.
 */
public class TrailSection implements IPersistedSerializable {
    public boolean enable = true;
    public List<Vector2f> vertices = new ArrayList<>();

    public TrailSection() {
        circlePreset(8);
    }

    public void addVertex(Vector2f vertex) {
        vertices.add(vertex);
    }

    public int getSegments() {
        return vertices != null ? vertices.size() - 1 : 0;
    }

    public void circlePreset(int segments) {
        vertices.clear();
        for (int j = 0; j <= segments; ++j) {
            float angle = 2 * (float) Math.PI / segments * j;
            Vector2f right = new Vector2f(1, 0);
            Vector2f up = new Vector2f(0, 1);
            Vector2f point = new Vector2f(right).mul((float) Math.cos(angle))
                    .add(new Vector2f(up).mul((float) Math.sin(angle)));
            vertices.add(point);
        }
    }
    public boolean isEnable() { return enable; }
}