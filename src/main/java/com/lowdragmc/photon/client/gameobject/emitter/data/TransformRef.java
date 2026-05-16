package com.lowdragmc.photon.client.gameobject.emitter.data;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
public class TransformRef implements IPersistedSerializable {
    @Getter @Setter private UUID transformId;
    public TransformRef() {}
    public TransformRef(UUID transformId) { this.transformId = transformId; }
}
