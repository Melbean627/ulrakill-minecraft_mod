package com.melbean.velocitycombat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = VelocityCombat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class ClientParryFeedback {
    private static final long FLASH_DURATION_NANOS = 300_000_000L;
    private static long flashEndsAt;
    private static float frozenYaw;
    private static float frozenPitch;

    private ClientParryFeedback() {
    }

    public static void trigger() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            frozenYaw = minecraft.player.getYRot();
            frozenPitch = minecraft.player.getXRot();
        }
        flashEndsAt = System.nanoTime() + FLASH_DURATION_NANOS;
    }

    static boolean isActive() {
        return System.nanoTime() < flashEndsAt;
    }

    @SubscribeEvent
    public static void onLocalPlayerTick(TickEvent.PlayerTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (!isActive() || player == null || event.player != player) {
            return;
        }

        player.input.forwardImpulse = 0.0F;
        player.input.leftImpulse = 0.0F;
        player.input.jumping = false;
        player.input.shiftKeyDown = false;
        player.setDeltaMovement(Vec3.ZERO);
        player.setYRot(frozenYaw);
        player.setXRot(frozenPitch);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!isActive()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        event.getGuiGraphics().fill(0, 0, minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight(), 0x80FFFFFF);
    }
}
