package com.lowdragmc.photon.client.fx;

import com.lowdragmc.lowdraglib2.core.scene.IScene;
import com.lowdragmc.lowdraglib2.core.scene.ISceneObject;
import com.lowdragmc.photon.Photon;
import com.lowdragmc.photon.client.gameobject.EmptyFXObject;
import com.lowdragmc.photon.client.gameobject.IFXObject;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Getter
public class FXRuntime implements IScene {
    public final static UUID ROOT_UUID = new UUID(0, 0);
    public final FXData fxData;
    public final Map<UUID, IFXObject> objects = new LinkedHashMap<>();
    public final IFXObject root;

    public FXRuntime(FXData fxData) {
        this.fxData = fxData;
        this.root = new EmptyFXObject();
        this.root.transform()._setInternalID(ROOT_UUID);
        addSceneObjectInternal(root);
        root.setName("root");
        initRuntime();
    }

    private void initRuntime() {
        for (var fxObject : fxData.objects()) {
            if (fxObject.transform().parent() == null) {
                fxObject.transform().parent(root.transform(), false);
            }
            fxObject.transform().rebuildChildOrder();
        }
    }

    @Nullable
    @Override
    public ISceneObject getSceneObject(UUID uuid) {
        return objects.getOrDefault(uuid, root);
    }

    public void addSceneObjectInternal(ISceneObject sceneObject) {
        if (sceneObject instanceof IFXObject fxObject) {
            var previous = objects.put(fxObject.id(), fxObject);
            if (previous != null) {
                if (previous != fxObject) {
                    Photon.LOGGER.warn("Duplicate fx runtime object id " + fxObject.id() + " is replaced");
                }
            }
        }
    }

    public void removeSceneObjectInternal(ISceneObject sceneObject) {
        if (sceneObject instanceof IFXObject fxObject) {
            objects.remove(fxObject.id());
        }
    }

    public Collection<ISceneObject> getAllSceneObjects() {
        return new ArrayList<>(objects.values());
    }

    public void emmit(IEffectExecutor effect) {
        emmit(effect, 0);
    }

    public void emmit(IEffectExecutor effect, int delay) {
        for (var fxObject : objects.values()) {
            fxObject.emmit(effect);
            fxObject.setDelay(delay);
        }
    }

    public boolean isAlive() {
        return objects.values().stream().anyMatch(IFXObject::isAlive);
    }

    public void destroy(boolean force) {
        for (var fxObject : objects.values()) {
            fxObject.remove(force);
        }
    }

    @Nullable
    public IFXObject findObject(String name) {
        for (var fxObject : objects.values()) {
            if (fxObject.getName().equals(name)) {
                return fxObject;
            }
        }
        return null;
    }

    public List<IFXObject> findObjects(String name) {
        var list = new ArrayList<IFXObject>();
        for (var fxObject : objects.values()) {
            if (fxObject.getName().equals(name)) {
                list.add(fxObject);
            }
        }
        return list;
    }

    public com.lowdragmc.photon.client.gameobject.IFXObject getRoot() { return root; }
}
