package com.lowdragmc.lowdraglib2.client;

import com.lowdragmc.lowdraglib2.client.shader.LDLibShaders;
import com.lowdragmc.lowdraglib2.client.shader.management.ShaderManager;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;

import java.util.ArrayList;
import java.util.List;

/**
 * @author KilaBash
 * @date 2023/2/9
 * @implNote ClientCommands - stripped of test UI commands for Tachyon
 */
public class ClientCommands {

    public static LiteralArgumentBuilder<FabricClientCommandSource> createLiteral(String command) {
        return ClientCommandManager.literal(command);
    }

    public static List<LiteralArgumentBuilder<FabricClientCommandSource>> createClientCommands() {
        var commands = new ArrayList<LiteralArgumentBuilder<FabricClientCommandSource>>();
        commands.add(createLiteral("ldlib2_client").then(createLiteral("reload_shader")
                .executes(context -> {
                    LDLibShaders.reload();
                    ShaderManager.getInstance().reload();
                    return 1;
                })));
        return commands;
    }
}
