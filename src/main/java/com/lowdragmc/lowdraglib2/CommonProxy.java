package com.lowdragmc.lowdraglib2;

import com.lowdragmc.lowdraglib2.client.renderer.block.RendererBlock;
import com.lowdragmc.lowdraglib2.client.renderer.block.RendererBlockEntity;
import com.lowdragmc.lowdraglib2.plugin.ILDLibPlugin;
import com.lowdragmc.lowdraglib2.plugin.LDLibPlugin;
import com.lowdragmc.lowdraglib2.syncdata.AccessorRegistries;
import com.lowdragmc.lowdraglib2.utils.ReflectionUtils;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class CommonProxy {

    public static BlockEntityType<RendererBlockEntity> RENDERER_BE_TYPE;

    public CommonProxy() {
        // Registration for blocks, items, BEs
        register("renderer_block", RendererBlock.BLOCK);
        RENDERER_BE_TYPE = register("renderer_block", BlockEntityType.Builder.of(RendererBlockEntity::new, RendererBlock.BLOCK).build(null));

        // init common features
        CommonProxy.init();

        // load ldlib2 plugin
        ReflectionUtils.findAnnotationClasses(LDLibPlugin.class, data -> true, clazz -> {
            try {
                if (clazz.getConstructor().newInstance() instanceof ILDLibPlugin plugin) {
                    plugin.onLoad();
                }
            } catch (Throwable throwable) {
                LDLib2.LOGGER.error("Failed to load plugin {}", clazz.getName(), throwable);
            }
        }, () -> {});
    }

    public static void init() {
        LDLib2Registries.init();
        AccessorRegistries.init();
    }

    private static <T extends Block> T register(String name, T block) {
        return Registry.register(BuiltInRegistries.BLOCK, LDLib2.id(name), block);
    }

    private static <T extends Item> T register(String name, T item) {
        return Registry.register(BuiltInRegistries.ITEM, LDLib2.id(name), item);
    }

    private static <T extends BlockEntityType<?>> T register(String name, T type) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, LDLib2.id(name), type);
    }

}
