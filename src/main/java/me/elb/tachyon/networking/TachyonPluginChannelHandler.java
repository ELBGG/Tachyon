package me.elb.tachyon.networking;

import com.lowdragmc.photon.client.fx.BlockEffectExecutor;
import com.lowdragmc.photon.client.fx.EntityEffectExecutor;
import com.lowdragmc.photon.client.fx.FX;
import me.elb.tachyon.Tachyon;
import me.elb.tachyon.api.executor.PositionVFXExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.nio.charset.StandardCharsets;

/**
 * Handles the {@code tachyon:vfx} plugin messaging channel.
 * <p>
 * This channel allows <b>any server</b> (vanilla, Paper, Spigot, Forge) to
 * trigger VFX on connected Tachyon clients using Minecraft's standard plugin
 * messaging protocol — <em>no Fabric on the server is required</em>.
 *
 * <h2>Protocol Specification</h2>
 * <p>All multi-byte integers and floats are big-endian.
 * <p>Strings are encoded as: {@code [unsigned short: length][UTF-8 bytes]}.
 *
 * <pre>
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │  COMMON TRANSFORM BLOCK (appended to types 0x01–0x03):                  │
 * │    float offX, offY, offZ   (world-space offset)                        │
 * │    float rotX, rotY, rotZ   (Euler degrees)                             │
 * │    float scaleX, scaleY, scaleZ                                          │
 * │    int   delayTicks                                                      │
 * │    byte  flags  (bit 0 = allowMultiple, bit 1 = forcedDeath)            │
 * ├──────────────────────────────────────────────────────────────────────────┤
 * │  0x01 — VFX at position:                                                │
 * │    string fxLocation   (e.g. "mymod:fireball")                          │
 * │    double x, y, z      (world position)                                 │
 * │    [TRANSFORM BLOCK]                                                     │
 * ├──────────────────────────────────────────────────────────────────────────┤
 * │  0x02 — VFX at block:                                                   │
 * │    string fxLocation                                                     │
 * │    int blockX, blockY, blockZ                                            │
 * │    [TRANSFORM BLOCK]                                                     │
 * ├──────────────────────────────────────────────────────────────────────────┤
 * │  0x03 — VFX on entity:                                                  │
 * │    string fxLocation                                                     │
 * │    int entityId                                                          │
 * │    byte autoRotate  (0=NONE, 1=FORWARD, 2=LOOK, 3=XROT)                │
 * │    [TRANSFORM BLOCK]                                                     │
 * ├──────────────────────────────────────────────────────────────────────────┤
 * │  0x04 — Remove VFX from block:                                          │
 * │    string fxLocation                                                     │
 * │    int blockX, blockY, blockZ                                            │
 * ├──────────────────────────────────────────────────────────────────────────┤
 * │  0x05 — Remove VFX from entity:                                         │
 * │    string fxLocation                                                     │
 * │    int entityId                                                          │
 * └──────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>Paper/Spigot Usage (no Tachyon jar needed on server)</h2>
 * <pre>{@code
 * // Register outgoing channel in onEnable():
 * getServer().getMessenger().registerOutgoingPluginChannel(this, "tachyon:vfx");
 *
 * // Send a VFX at position (type 0x01):
 * ByteArrayOutputStream bytes = new ByteArrayOutputStream();
 * DataOutputStream out = new DataOutputStream(bytes);
 * out.writeByte(0x01);
 * writeString(out, "mymod:fireball");
 * out.writeDouble(x); out.writeDouble(y); out.writeDouble(z);
 * // transform block (offset=0, rotation=0, scale=1, delay=0, flags=1):
 * out.writeFloat(0); out.writeFloat(0); out.writeFloat(0);
 * out.writeFloat(0); out.writeFloat(0); out.writeFloat(0);
 * out.writeFloat(1); out.writeFloat(1); out.writeFloat(1);
 * out.writeInt(0); out.writeByte(1);
 * player.sendPluginMessage(this, "tachyon:vfx", bytes.toByteArray());
 * }</pre>
 */
@Environment(EnvType.CLIENT)
public class TachyonPluginChannelHandler {

    // Message type constants
    public static final byte TYPE_POSITION   = 0x01;
    public static final byte TYPE_BLOCK      = 0x02;
    public static final byte TYPE_ENTITY     = 0x03;
    public static final byte TYPE_REMOVE_BLOCK  = 0x04;
    public static final byte TYPE_REMOVE_ENTITY = 0x05;

    private TachyonPluginChannelHandler() {}

    /**
     * Registered as the global receiver for the {@code tachyon:vfx} plugin channel.
     * Called on the Netty thread; schedule work to the render thread via
     * {@code context.client().execute(...)}.
     */
    public static void handle(ClientPlayNetworking.Context context,
                              net.fabricmc.fabric.api.networking.v1.PacketSender sender,
                              Minecraft client,
                              net.minecraft.network.protocol.Packet<?> packet) {
        // This signature is for the raw channel receiver — see registration in TachyonClient
    }

    /**
     * Actual handler registered via:
     * {@code ClientPlayNetworking.registerGlobalReceiver(id, (payload, ctx) -> ...)}
     */
    @SuppressWarnings("unused")
    public static void handle(Object payload,
                              ClientPlayNetworking.Context context) {
        // Fabric 0.100+ uses typed payloads; plugin channels arrive as raw bytes.
        // This overload is intentionally empty — the raw channel registration is done below.
    }

    /**
     * Raw plugin channel handler. Called with the raw payload bytes from the server.
     *
     * @param buf the raw message buffer
     */
    public static void handleRaw(FriendlyByteBuf buf, Minecraft client) {
        if (!buf.isReadable()) return;

        byte type = buf.readByte();
        try {
            switch (type) {
                case TYPE_POSITION   -> handlePosition(buf, client);
                case TYPE_BLOCK      -> handleBlock(buf, client);
                case TYPE_ENTITY     -> handleEntity(buf, client);
                case TYPE_REMOVE_BLOCK  -> handleRemoveBlock(buf, client);
                case TYPE_REMOVE_ENTITY -> handleRemoveEntity(buf, client);
                default -> Tachyon.LOGGER.warn("tachyon:vfx — unknown message type 0x{}", Integer.toHexString(type));
            }
        } catch (Exception e) {
            Tachyon.LOGGER.error("tachyon:vfx — error handling type 0x{}: {}", Integer.toHexString(type), e.getMessage(), e);
        }
    }

    // ── Type handlers ─────────────────────────────────────────────────────────

    private static void handlePosition(FriendlyByteBuf buf, Minecraft client) {
        String fxStr = readString(buf);
        double x = buf.readDouble(), y = buf.readDouble(), z = buf.readDouble();
        Transform t = readTransform(buf);

        client.execute(() -> {
            if (client.level == null) return;
            FX fx = me.elb.tachyon.api.TachyonAssetLoader.load(fxStr);
            if (fx == null) return;
            var executor = new PositionVFXExecutor(fx, client.level, x, y, z);
            applyTransform(executor, t);
            executor.start();
        });
    }

    private static void handleBlock(FriendlyByteBuf buf, Minecraft client) {
        String fxStr = readString(buf);
        int bx = buf.readInt(), by = buf.readInt(), bz = buf.readInt();
        Transform t = readTransform(buf);

        client.execute(() -> {
            if (client.level == null) return;
            FX fx = me.elb.tachyon.api.TachyonAssetLoader.load(fxStr);
            if (fx == null) return;
            var executor = new BlockEffectExecutor(fx, client.level, new BlockPos(bx, by, bz));
            applyTransform(executor, t);
            executor.start();
        });
    }

    private static void handleEntity(FriendlyByteBuf buf, Minecraft client) {
        String fxStr = readString(buf);
        int entityId = buf.readInt();
        byte autoRotateByte = buf.readByte();
        Transform t = readTransform(buf);

        EntityEffectExecutor.AutoRotate autoRotate = switch (autoRotateByte) {
            case 1 -> EntityEffectExecutor.AutoRotate.FORWARD;
            case 2 -> EntityEffectExecutor.AutoRotate.LOOK;
            case 3 -> EntityEffectExecutor.AutoRotate.XROT;
            default -> EntityEffectExecutor.AutoRotate.NONE;
        };

        client.execute(() -> {
            if (client.level == null) return;
            Entity entity = client.level.getEntity(entityId);
            if (entity == null) return;
            FX fx = me.elb.tachyon.api.TachyonAssetLoader.load(fxStr);
            if (fx == null) return;
            var executor = new EntityEffectExecutor(fx, client.level, entity, autoRotate);
            applyTransform(executor, t);
            executor.start();
        });
    }

    private static void handleRemoveBlock(FriendlyByteBuf buf, Minecraft client) {
        String fxStr = readString(buf);
        int bx = buf.readInt(), by = buf.readInt(), bz = buf.readInt();
        BlockPos pos = new BlockPos(bx, by, bz);

        client.execute(() -> {
            var key = ResourceLocation.tryParse(fxStr);
            if (key == null) return;
            var effects = BlockEffectExecutor.CACHE.get(pos);
            if (effects == null) return;
            effects.removeIf(e -> key.equals(e.getFx().getFxLocation()) && e.getRuntime() != null
                    );
        });
    }

    private static void handleRemoveEntity(FriendlyByteBuf buf, Minecraft client) {
        String fxStr = readString(buf);
        int entityId = buf.readInt();

        client.execute(() -> {
            if (buf == null) return; // safety
            var key = ResourceLocation.tryParse(fxStr);
            if (key == null || buf.refCnt() == 0) return;
            // Find entity in level and remove matching effects
            var mc = Minecraft.getInstance();
            if (mc.level == null) return;
            Entity entity = mc.level.getEntity(entityId);
            if (entity == null) return;
            var effects = EntityEffectExecutor.CACHE.get(entity);
            if (effects == null) return;
            effects.removeIf(e -> key.equals(e.getFx().getFxLocation()) && e.getRuntime() != null
                    );
        });
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private record Transform(Vector3f offset, Quaternionf rotation, Vector3f scale,
                             int delay, boolean allowMultiple, boolean forcedDeath) {}

    private static Transform readTransform(FriendlyByteBuf buf) {
        var offset = new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
        var rotation = new Quaternionf().rotationXYZ(
                (float) Math.toRadians(buf.readFloat()),
                (float) Math.toRadians(buf.readFloat()),
                (float) Math.toRadians(buf.readFloat()));
        var scale = new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
        int delay = buf.readInt();
        byte flags = buf.readByte();
        return new Transform(offset, rotation, scale, delay, (flags & 1) != 0, (flags & 2) != 0);
    }

    private static void applyTransform(com.lowdragmc.photon.client.fx.FXEffectExecutor executor,
                                       Transform t) {
        executor.setOffset(t.offset());
        executor.setRotation(t.rotation());
        executor.setScale(t.scale());
        executor.setDelay(t.delay());
        executor.setAllowMulti(t.allowMultiple());
        executor.setForcedDeath(t.forcedDeath());
    }

    private static String readString(FriendlyByteBuf buf) {
        int len = buf.readUnsignedShort();
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
