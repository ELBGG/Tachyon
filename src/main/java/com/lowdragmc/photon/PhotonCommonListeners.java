package com.lowdragmc.photon;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import java.util.List;

public class PhotonCommonListeners {
    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            java.util.List<com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack>> commands = new java.util.ArrayList<>();
            commands.forEach(dispatcher::register);
        });
    }
}
