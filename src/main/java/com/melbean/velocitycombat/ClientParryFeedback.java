package com.melbean.velocitycombat;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = VelocityCombat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class ClientParryFeedback {
    private static final long FLASH_DURATION_NANOS = 300_000_000L;
    private static long flashEndsAt;

    private ClientParryFeedback() {
    }

    public static void trigger() {
        flashEndsAt = System.nanoTime() + FLASH_DURATION_NANOS;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (System.nanoTime() >= flashEndsAt) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        event.getGuiGraphics().fill(0, 0, minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight(), 0x80FFFFFF);
    }
}
