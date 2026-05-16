package com.lowdragmc.lowdraglib2;

import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.registry.AutoRegistry;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class LDLib2Registries {

    @Environment(EnvType.CLIENT)
    public static AutoRegistry.LDLibRegisterClient<IRenderer, Supplier<IRenderer>> RENDERERS;

    static {
        if (LDLib2.isClient()) {
            RENDERERS = AutoRegistry.LDLibRegisterClient
                    .create(LDLib2.id("renderer"), IRenderer.class, AutoRegistry::noArgsCreator);
        }
    }

    public static void init() {
        if (LDLib2.isClient()) {
            RENDERERS.register("empty", AutoRegistry.Holder.of(
                    IRenderer.EmptyRenderer.class.getAnnotation(LDLRegisterClient.class),
                    IRenderer.EmptyRenderer.class,
                    () -> IRenderer.EMPTY));
        }
    }
}
