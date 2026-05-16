package com.lowdragmc.lowdraglib2.core.scene;

import com.lowdragmc.lowdraglib2.math.Transform;

public interface ISceneObject {
    Transform transform();
    IScene getScene();
    void setScene(IScene scene);
    
    default void onTransformChanged() {}
    default void onParentChanged() {}
    default void onChildChanged() {}
}
