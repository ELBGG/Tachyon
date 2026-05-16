package me.elb.tachyon.api.executor;

import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXEffectExecutor;
import com.lowdragmc.photon.client.gameobject.IFXObject;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/**
 * An {@link FXEffectExecutor} that anchors a VFX to a fixed world position.
 * <p>
 * Unlike {@code BlockEffectExecutor} (which tracks block state changes) or
 * {@code EntityEffectExecutor} (which follows an entity), this executor keeps
 * the VFX at a static coordinate in the world.
 * <p>
 * Create via {@link me.elb.tachyon.api.VFXBuilder#atPosition(double, double, double)}.
 */
@Environment(EnvType.CLIENT)
public class PositionVFXExecutor extends FXEffectExecutor {

    /** World position where the VFX is anchored. */
    private final Vector3f worldPosition;

    public PositionVFXExecutor(FX fx, Level level, double x, double y, double z) {
        super(fx, level);
        this.worldPosition = new Vector3f((float) x, (float) y, (float) z);
    }

    @Override
    public void start() {
        this.runtime = fx.createRuntime();
        var root = this.runtime.getRoot();
        root.updatePos(new Vector3f(worldPosition).add(offset));
        root.updateRotation(rotation);
        root.updateScale(scale);
        this.runtime.emmit(this, delay);
    }

    @Override
    public void updateFXObjectFrame(IFXObject fxObject, float partialTicks) {
        // Static position — no frame update needed.
        // Override if you need dynamic repositioning.
    }
}
