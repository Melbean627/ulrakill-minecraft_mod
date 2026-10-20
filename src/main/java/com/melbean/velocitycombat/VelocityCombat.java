package com.melbean.velocitycombat;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(VelocityCombat.MOD_ID)
public final class VelocityCombat {
    public static final String MOD_ID = "velocitycombat";

    public VelocityCombat() {
        CombatNetwork.register();
        MinecraftForge.EVENT_BUS.register(new CombatEvents());
    }
}
