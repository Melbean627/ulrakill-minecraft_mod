package com.melbean.velocitycombat;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(value = VelocityCombat.MOD_ID, dist = Dist.CLIENT)
public final class VelocityCombat {
    public static final String MOD_ID = "velocitycombat";

    public VelocityCombat() {
        MinecraftForge.EVENT_BUS.register(new ClientVisualFX());
        MinecraftForge.EVENT_BUS.register(ClientCombatControls.ClientEvents.class);
    }
}
