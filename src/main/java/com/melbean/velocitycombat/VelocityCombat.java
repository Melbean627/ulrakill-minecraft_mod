package com.melbean.velocitycombat;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(VelocityCombat.MOD_ID)
public final class VelocityCombat {
    public static final String MOD_ID = "velocitycombat";

    public VelocityCombat() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        WeaponItems.register(modEventBus);
        modEventBus.addListener(WeaponItems::addToCreativeTab);
        CombatNetwork.register();
        MinecraftForge.EVENT_BUS.register(new CombatEvents());
    }
}
