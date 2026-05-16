# Tachyon VFX — Developer API Guide

Tachyon es una librería de efectos visuales (VFX) headless basada en Photon/LDLib2, embebida directamente en tu mod. Este documento explica cómo cargar, adjuntar y controlar efectos `.fx` en bloques, entidades y posiciones arbitrarias.

---

## Tabla de contenidos

1. [Agregar Tachyon a tu mod](#1-agregar-tachyon-a-tu-mod)
2. [Estructura de assets requerida](#2-estructura-de-assets-requerida)
3. [Tipos de assets y cómo agregarlos a tu mod](#3-tipos-de-assets-y-cómo-agregarlos-a-tu-mod)
4. [Cargar un FX](#4-cargar-un-fx)
5. [Reproducir un VFX en un Bloque](#5-reproducir-un-vfx-en-un-bloque)
6. [Reproducir un VFX en una Entidad](#6-reproducir-un-vfx-en-una-entidad)
7. [Reproducir un VFX en una posición arbitraria](#7-reproducir-un-vfx-en-una-posición-arbitraria)
8. [Controlar offset, rotación y escala](#8-controlar-offset-rotación-y-escala)
9. [Detener un efecto activo](#9-detener-un-efecto-activo)
10. [Tips y buenas prácticas](#10-tips-y-buenas-prácticas)

---

## 1. Agregar Tachyon a tu mod

Tachyon se incluye como una dependencia local o como un JAR en tu `build.gradle`. Una vez integrado, los paquetes disponibles son:

```
com.lowdragmc.photon.client.fx.*          ← Ejecutores y carga de FX
com.lowdragmc.photon.client.fx.FXHelper   ← Carga de archivos .fx por ResourceLocation
```

> **Nota:** Todos los ejecutores y llamadas de renderizado son **client-side only** (`@Environment(EnvType.CLIENT)`). Nunca llames estas APIs desde código server-side sin un guard de entorno.

### Guard de entorno recomendado (Fabric):

```java
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

// En una clase @Environment(EnvType.CLIENT)
// o rodeando la llamada:
if (Platform.getEnv() == EnvType.CLIENT) {
    // llamar la API de VFX aquí
}
```

---

## 2. Estructura de assets requerida

Un VFX en Tachyon puede componerse de varios tipos de archivos. Todos deben estar en el `resources` de tu mod (o de Tachyon mismo). La estructura completa es:

```
src/main/resources/
└── assets/
    └── <modid>/
        ├── fx/                        ← Definiciones de efectos (.fx)
        │   ├── explosion.fx
        │   └── entity/
        │       └── fire_trail.fx
        │
        ├── textures/
        │   └── particle/              ← Texturas PNG usadas por los materiales
        │       ├── mi_particula.png
        │       └── mi_particula.png.mcmeta  ← (opcional) animación
        │
        ├── models/
        │   ├── block/                 ← JSON de modelo referenciado por SpecialModelLoader
        │   │   └── mi_modelo.json
        │   └── obj/                   ← Modelos 3D OBJ + su MTL
        │       ├── mi_modelo.obj
        │       └── mi_modelo.mtl
        │
        └── shaders/
            └── core/                  ← Shaders GLSL personalizados
                ├── mi_shader.vsh
                ├── mi_shader.fsh
                └── mi_shader.json     ← Descriptor de programa de shader
```

> **Nota:** Tachyon ya incluye todos sus propios assets en el namespace `photon`. Los assets de tu mod van bajo **tu propio `<modid>`**.

---

## 3. Tipos de assets y cómo agregarlos a tu mod

### 3.1 Archivos `.fx` (Definición del efecto)

Son archivos NBT comprimidos generados por el editor de Photon. Contienen la configuración de todos los emisores, materiales, curvas de animación, etc.

**Ruta:** `assets/<modid>/fx/<nombre>.fx`

```
assets/mymi/fx/explosion.fx
assets/mymi/fx/block/reactor_aura.fx
assets/mymi/fx/entity/boss_flames.fx
```

El FX referencia internamente a los otros assets (texturas, modelos, shaders) usando sus `ResourceLocation`.

---

### 3.2 Texturas de partícula (`.png`)

Son las texturas PNG que los materiales aplican a las partículas.

**Ruta:** `assets/<modid>/textures/particle/<nombre>.png`

```
assets/mymi/textures/particle/sparkle.png
assets/mymi/textures/particle/glow_ring.png
```

**Texturas ya incluidas por Tachyon (namespace `photon`):**

| ResourceLocation | Descripción |
|---|---|
| `photon:textures/particle/circle.png` | Círculo suave (default) |
| `photon:textures/particle/smoke.png` | Humo |
| `photon:textures/particle/ring.png` | Anillo |
| `photon:textures/particle/laser.png` | Rayo/Láser |
| `photon:textures/particle/kila_tail.png` | Estela |
| `photon:textures/particle/thaumcraft.png` | Estilo Thaumcraft (animada, tiene `.mcmeta`) |

Para usar una textura del propio namespace de Photon en el editor, selecciona `photon:textures/particle/...`.

**Texturas animadas:** puedes agregar un `.png.mcmeta` junto a la textura para animarla:

```json
{
    "animation": {
        "frametime": 2,
        "frames": [0, 1, 2, 3]
    }
}
```

---

### 3.3 Modelos OBJ (3D Mesh Particles)

Algunos emisores de tipo **Mesh** usan modelos OBJ para emitir partículas con forma 3D personalizada. Requieren dos archivos:

- `.obj` — geometría del modelo
- `.mtl` — materiales del OBJ (referencia a texturas)
- `.json` — descriptor del modelo en formato NeoForge/SpecialModelLoader

**Ruta:**
```
assets/<modid>/models/obj/mi_modelo.obj
assets/<modid>/models/obj/mi_modelo.mtl
assets/<modid>/models/block/mi_modelo.json
```

**Modelos ya incluidos por Tachyon:**

| JSON (namespace photon) | OBJ asociado |
|---|---|
| `photon:models/block/sphere.json` | `models/obj/sphere.obj` |
| `photon:models/block/hello_world.json` | `models/obj/hello_world.obj` |
| `photon:models/block/character.json` | `models/obj/character.obj` |

El `models/block/<nombre>.json` debe tener el formato de SpecialModelLoader con loader `neoforge:obj`:

```json
{
    "loader": "neoforge:obj",
    "model": "mymi:models/obj/mi_modelo.obj",
    "flip-v": true,
    "ambient": false
}
```

**Importante:** Para que tu namespace sea reconocido por SpecialModelLoader, debes registrar un `LOAD_SCOPE` en tu `ClientModInitializer`:

```java
import dev.felnull.specialmodelloader.api.event.SpecialModelLoaderEvents;

// En onInitializeClient():
SpecialModelLoaderEvents.LOAD_SCOPE.register(() -> (resourceManager, location) -> {
    return location.getNamespace().equals("mymi"); // tu modid
});
```

> Sin esto, SpecialModelLoader ignorará los modelos de tu namespace.

---

### 3.4 Shaders personalizados (`.fsh`, `.vsh`, `.json`)

El material de tipo `custom_shader` permite usar shaders GLSL propios. Requiere tres archivos:

**Ruta:** `assets/<modid>/shaders/core/`

```
assets/mymi/shaders/core/mi_efecto.vsh     ← vertex shader
assets/mymi/shaders/core/mi_efecto.fsh     ← fragment shader
assets/mymi/shaders/core/mi_efecto.json    ← descriptor del programa
```

El `.json` descriptor tiene este formato:

```json
{
    "blend": {
        "func": "add",
        "srcrgb": "srcalpha",
        "dstrgb": "1-srcalpha"
    },
    "vertex": "mymi:mi_efecto",
    "fragment": "mymi:mi_efecto",
    "attributes": ["Position", "Color", "UV0"],
    "samplers": [
        { "name": "Sampler0" }
    ],
    "uniforms": [
        { "name": "ModelViewMat",  "type": "matrix4x4", "count": 16, "values": [1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1] },
        { "name": "ProjMat",       "type": "matrix4x4", "count": 16, "values": [1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1] },
        { "name": "ColorModulator","type": "float",     "count": 4,  "values": [1,1,1,1] }
    ]
}
```

**Shaders ya incluidos por Tachyon (namespace `photon`):**

| ResourceLocation del shader | Descripción |
|---|---|
| `photon:circle` | Partícula circular suave |
| `photon:hdr_particle` | Partícula con bloom HDR |
| `photon:pixel_hdr_particle` | Partícula HDR pixelada |
| `photon:sprite_hdr_particle` | Partícula sprite HDR |

Puedes referenciar cualquiera de estos en el editor de Photon sin necesidad de crearlos.

---

### 3.5 Resumen: tabla de tipos de asset

| Tipo | Carpeta | Extensión | Usado por |
|---|---|---|---|
| Definición VFX | `fx/` | `.fx` | `FXHelper.getFX()` |
| Textura partícula | `textures/particle/` | `.png` | Material `texture` |
| Textura animada | `textures/particle/` | `.png` + `.png.mcmeta` | Material `texture` (animado) |
| Sprite del atlas | `textures/particle/` | `.png` (en atlas) | Material `sprite` |
| Modelo OBJ | `models/obj/` | `.obj` + `.mtl` | Emisor Mesh |
| Descriptor modelo | `models/block/` | `.json` | SpecialModelLoader |
| Shader GLSL | `shaders/core/` | `.vsh` + `.fsh` + `.json` | Material `custom_shader` |

---

## 4. Cargar un FX

Usa `FXHelper.getFX(ResourceLocation)` para obtener una instancia de `FX` (con caché automático):

```java
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXHelper;
import net.minecraft.resources.ResourceLocation;

// Carga (con caché) el archivo assets/mymi/fx/explosion.fx
FX fx = FXHelper.getFX(ResourceLocation.fromNamespaceAndPath("mymi", "explosion"));

if (fx == null) {
    // El archivo no se encontró o falló la carga
    return;
}
```

> **`getFX` es cacheado**: la primera carga lee el archivo de recursos; las llamadas siguientes devuelven la instancia en caché.
> Para forzar recarga (por ejemplo tras un F3+T), usa `FXHelper.clearCache()`.

---

## 5. Reproducir un VFX en un Bloque

Usa `BlockEffectExecutor` para vincular un efecto a una posición de bloque. El efecto se destruye automáticamente si el bloque cambia o se descarga.

```java
import com.lowdragmc.photon.client.fx.BlockEffectExecutor;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;

@Environment(EnvType.CLIENT)
public class MyBlockVFX {

    public static void playOnBlock(Level level, BlockPos pos) {
        FX fx = FXHelper.getFX(ResourceLocation.fromNamespaceAndPath("mymi", "my_block_effect"));
        if (fx == null) return;

        BlockEffectExecutor executor = new BlockEffectExecutor(fx, level, pos);

        // Opcional: no permitir múltiples instancias del mismo FX en ese bloque
        executor.setAllowMulti(false);

        // Opcional: verificar que el estado del bloque no haya cambiado para detener el efecto
        executor.setCheckState(true);

        // ¡Iniciar el efecto!
        executor.start();
    }
}
```

### Tabla de opciones del `BlockEffectExecutor`:

| Método | Descripción |
|---|---|
| `setAllowMulti(false)` | Solo un efecto del mismo tipo por posición. Si ya existe uno, no se crea otro. |
| `setCheckState(true)` | El efecto se destruye si el `BlockState` del bloque cambia (no solo el Block). |
| `setOffset(Vector3f)` | Desplazamiento respecto al centro del bloque `(0.5, 0.5, 0.5)`. |
| `setRotation(Quaternionf)` | Rotación inicial del efecto. |
| `setScale(Vector3f)` | Escala del efecto. |
| `setDelay(int)` | Retraso en ticks antes de que el efecto comience a emitir. |
| `setForcedDeath(boolean)` | Si `true`, corta el efecto inmediatamente al destruirlo (sin fade-out). |

---

## 6. Reproducir un VFX en una Entidad

Usa `EntityEffectExecutor` para vincular un efecto a una entidad. El efecto se mueve junto con ella y se destruye cuando la entidad muere o sale del mundo.

```java
import com.lowdragmc.photon.client.fx.EntityEffectExecutor;
import com.lowdragmc.photon.client.fx.EntityEffectExecutor.AutoRotate;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;

@Environment(EnvType.CLIENT)
public class MyEntityVFX {

    public static void playOnEntity(Entity entity) {
        Level level = entity.level();
        FX fx = FXHelper.getFX(ResourceLocation.fromNamespaceAndPath("mymi", "fire_trail"));
        if (fx == null) return;

        EntityEffectExecutor executor = new EntityEffectExecutor(
            fx,
            level,
            entity,
            AutoRotate.FORWARD  // ver tabla abajo
        );

        // Solo un efecto de este tipo por entidad
        executor.setAllowMulti(false);

        // Desplazamiento relativo a los ojos de la entidad
        executor.setOffset(new org.joml.Vector3f(0, -1.5f, 0));

        executor.start();
    }
}
```

### Modos de `AutoRotate`:

| Valor | Comportamiento |
|---|---|
| `NONE` | Sin rotación automática, usa la rotación inicial que le pases. |
| `FORWARD` | El efecto siempre apunta en la dirección de movimiento de la entidad. |
| `LOOK` | El efecto sigue la dirección de visión (`lookAngle`) de la entidad. |
| `XROT` | Rota en el eje Y según la orientación visual de la entidad. |

> **Posición base:** el efecto se ancla a `entity.getEyePosition()` más el `offset` configurado. Para anclarlo a los pies, usa `setOffset(new Vector3f(0, -entity.getEyeHeight(), 0))`.

---

## 7. Reproducir un VFX en una posición arbitraria

Si quieres reproducir un efecto en una posición fija sin vincularlo a ningún objeto del mundo, crea el runtime directamente:

```java
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXHelper;
import com.lowdragmc.photon.client.fx.FXRuntime;
import net.minecraft.resources.ResourceLocation;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class MyFreeVFX {

    private FXRuntime activeRuntime;

    public void play(double x, double y, double z) {
        FX fx = FXHelper.getFX(ResourceLocation.fromNamespaceAndPath("mymi", "sparkle"));
        if (fx == null) return;

        // Crea el runtime (esto no lo inicia aún)
        activeRuntime = fx.createRuntime();

        // Posiciona el efecto en el mundo
        activeRuntime.getRoot().updatePos(new Vector3f((float) x, (float) y, (float) z));

        // Emitir sin un executor (sin auto-tracking)
        // El 0 es el delay inicial en ticks
        activeRuntime.emmit(null, 0);
    }

    public void stop() {
        if (activeRuntime != null && activeRuntime.isAlive()) {
            activeRuntime.destroy(false); // false = con fade-out
            activeRuntime = null;
        }
    }
}
```

> ⚠️ **Advertencia:** Los runtimes libres no tienen tracking automático. Debes gestionar su ciclo de vida (iniciarlo, verificar si aún está vivo y destruirlo cuando ya no sea necesario).

---

## 8. Controlar offset, rotación y escala

Todos los ejecutores heredan de `FXEffectExecutor` y comparten estas propiedades, que deben configurarse **antes** de llamar a `.start()`:

```java
import org.joml.Quaternionf;
import org.joml.Vector3f;

// Desplazamiento: mueve el efecto X bloques desde el origen
executor.setOffset(new Vector3f(0f, 2f, 0f)); // 2 bloques hacia arriba

// Rotación: rota usando un Quaternion
// Ejemplo: 45 grados en el eje Y
Quaternionf rot = new Quaternionf().rotateY((float) Math.toRadians(45));
executor.setRotation(rot);

// Escala: agranda o reduce el efecto
executor.setScale(new Vector3f(2f, 2f, 2f)); // el doble de tamaño

// Retraso en ticks antes de emitir (20 ticks = 1 segundo)
executor.setDelay(10); // 0.5 segundos de delay
```

---

## 9. Detener un efecto activo

### Via el caché de bloque:

```java
// Obtener todos los efectos activos en esa posición
List<BlockEffectExecutor> effects = BlockEffectExecutor.CACHE.get(pos);
if (effects != null) {
    for (BlockEffectExecutor e : effects) {
        if (e.getRuntime() != null) {
            // false = con fade-out natural
            // true  = corte inmediato (forcedDeath)
            e.getRuntime().destroy(false);
        }
    }
    BlockEffectExecutor.CACHE.remove(pos);
}
```

### Via el caché de entidad:

```java
List<EntityEffectExecutor> effects = EntityEffectExecutor.CACHE.get(entity);
if (effects != null) {
    for (EntityEffectExecutor e : effects) {
        if (e.getRuntime() != null) {
            e.getRuntime().destroy(false);
        }
    }
    EntityEffectExecutor.CACHE.remove(entity);
}
```

### Via referencia directa al runtime:

```java
// Si guardaste la referencia al executor al crearlo:
if (myExecutor.getRuntime() != null && myExecutor.getRuntime().isAlive()) {
    myExecutor.getRuntime().destroy(false);
}
```

---

## 10. Tips y buenas prácticas

### ✅ Usar siempre desde el hilo del cliente

```java
// En un ClientTickEvent o dentro de @Environment(EnvType.CLIENT)
Minecraft.getInstance().execute(() -> {
    executor.start();
});
```

### ✅ Registrar tu namespace en SpecialModelLoader

Si tu mod usa modelos OBJ como meshes de partículas, registra tu namespace en el `ClientModInitializer`:

```java
SpecialModelLoaderEvents.LOAD_SCOPE.register(() -> (resourceManager, location) -> {
    return location.getNamespace().equals("mymi");
});
```

### ✅ Llamar `FXHelper.clearCache()` en reload de recursos

```java
// En tu ResourceManagerReloadListener:
@Override
public void onResourceManagerReload(ResourceManager manager) {
    FXHelper.clearCache();
}
```

### ✅ Caché de FX precargado

Para efectos que se reproducen frecuentemente (pasos, impactos), precarga los FX al inicio:

```java
@Environment(EnvType.CLIENT)
public class MyVFXRegistry {
    private static FX EXPLOSION_FX;
    private static FX TRAIL_FX;

    public static void init() {
        EXPLOSION_FX = FXHelper.getFX(ResourceLocation.fromNamespaceAndPath("mymi", "explosion"));
        TRAIL_FX     = FXHelper.getFX(ResourceLocation.fromNamespaceAndPath("mymi", "entity/trail"));
    }

    public static FX explosion() { return EXPLOSION_FX; }
    public static FX trail()     { return TRAIL_FX; }
}
```

### ✅ Solo un efecto por bloque/entidad (setAllowMulti)

Si tu efecto es continuo (loop), usa `setAllowMulti(false)` para evitar que se apilen llamadas duplicadas cuando el evento de disparo se llame varias veces seguidas.

### ✅ Estructura de carpetas completa sugerida para un mod

```
src/main/resources/assets/mymi/
├── fx/
│   ├── block/
│   │   ├── furnace_flames.fx
│   │   └── reactor_glow.fx
│   └── entity/
│       ├── player_dash.fx
│       ├── zombie_death.fx
│       └── boss_aura.fx
│
├── textures/
│   └── particle/
│       ├── my_sparkle.png
│       ├── my_smoke.png
│       └── my_anim.png
│       └── my_anim.png.mcmeta    ← animación
│
├── models/
│   ├── block/
│   │   └── my_mesh.json          ← descriptor SpecialModelLoader
│   └── obj/
│       ├── my_mesh.obj
│       └── my_mesh.mtl
│
└── shaders/
    └── core/
        ├── my_glow.vsh
        ├── my_glow.fsh
        └── my_glow.json
```

---

## Ejemplo completo: efecto en bloque con evento de colocación

```java
// En tu ClientEventListener (Fabric):
@Environment(EnvType.CLIENT)
public class MyClientEvents {

    @SuppressWarnings("unused")
    public static void onBlockPlaced(Level level, BlockPos pos, BlockState state) {
        if (state.is(MyBlocks.MAGIC_BLOCK)) {
            FX fx = FXHelper.getFX(ResourceLocation.fromNamespaceAndPath("mymi", "block/magic_aura"));
            if (fx == null) return;

            BlockEffectExecutor executor = new BlockEffectExecutor(fx, level, pos);
            executor.setAllowMulti(false);  // solo uno por bloque
            executor.setCheckState(false);  // no destruir si el estado cambia
            executor.setOffset(new org.joml.Vector3f(0, 0.5f, 0)); // encima del bloque
            executor.start();
        }
    }
}
```

## Ejemplo completo: efecto en entidad con red (S2C packet)

```java
// En tu handler de paquete C2S/S2C (lado cliente):
@Environment(EnvType.CLIENT)
public static void handleVFXPacket(ResourceLocation fxId, int entityId, AutoRotate mode) {
    Minecraft mc = Minecraft.getInstance();
    Entity entity = mc.level.getEntity(entityId);
    if (entity == null) return;

    FX fx = FXHelper.getFX(fxId);
    if (fx == null) return;

    EntityEffectExecutor executor = new EntityEffectExecutor(fx, mc.level, entity, mode);
    executor.setAllowMulti(false);
    executor.start();
}
```

---

*Tachyon VFX — API Headless | Documentación interna del proyecto ELB_GG*
