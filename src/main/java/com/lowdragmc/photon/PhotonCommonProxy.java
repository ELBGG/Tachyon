package com.lowdragmc.photon;

import com.lowdragmc.photon.core.mixins.accessor.ArgumentTypeInfosAccessor;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public class PhotonCommonProxy {

    public static void init() {
        PhotonNetworking.registerPayloads();
        
                        
        
        PhotonRegistries.init();
        PhotonCommonListeners.init();
    }
}
