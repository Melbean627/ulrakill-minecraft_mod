package com.melbean.velocitycombat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = VelocityCombat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientCombatControls {
    private static final KeyMapping DASH = new KeyMapping(
            "key.velocitycombat.dash", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R,
            "key.categories.velocitycombat");
    private static final KeyMapping SLIDE = new KeyMapping(
            "key.velocitycombat.slide", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_SHIFT,
            "key.categories.velocitycombat");
    private static final KeyMapping GROUND_SLAM = new KeyMapping(
            "key.velocitycombat.ground_slam", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X,
            "key.categories.velocitycombat");
    private static final KeyMapping PARRY = new KeyMapping(
            "key.velocitycombat.parry", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G,
            "key.categories.velocitycombat");
    private static final KeyMapping SWITCH_WEAPON = new KeyMapping(
            "key.velocitycombat.switch_weapon", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V,
            "key.categories.velocitycombat");

    private ClientCombatControls() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(DASH);
        event.register(SLIDE);
        event.register(GROUND_SLAM);
        event.register(PARRY);
        event.register(SWITCH_WEAPON);
    }

    @Mod.EventBusSubscriber(modid = VelocityCombat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ClientEvents {
        private ClientEvents() {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            while (DASH.consumeClick()) {
                CombatNetwork.send(CombatNetwork.Ability.DASH);
            }
            while (SLIDE.consumeClick()) {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.player == null || !minecraft.player.onGround()) {
                    continue;
                }
                var movement = minecraft.player.getDeltaMovement();
                if (minecraft.player.isSprinting()
                        || movement.x * movement.x + movement.z * movement.z > 0.01D) {
                    CombatNetwork.send(CombatNetwork.Ability.SLIDE);
                }
            }
            while (GROUND_SLAM.consumeClick()) {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.player != null && !minecraft.player.onGround()) {
                    CombatNetwork.send(CombatNetwork.Ability.GROUND_SLAM);
                }
            }
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null && !minecraft.player.onGround()
                    && minecraft.options.keyJump.consumeClick()) {
                CombatNetwork.send(CombatNetwork.Ability.WALL_KICK);
            }
            while (PARRY.consumeClick()) {
                CombatNetwork.send(CombatNetwork.Ability.PARRY);
            }
            while (SWITCH_WEAPON.consumeClick()) {
                CombatNetwork.send(CombatNetwork.Ability.SWITCH_WEAPON);
            }
        }

        @SubscribeEvent
        public static void onInteractionInput(InputEvent.InteractionKeyMappingTriggered event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (event.isAttack() && minecraft.player != null
                    && minecraft.player.getMainHandItem().is(WeaponItems.TOKEN_REVOLVER.get())) {
                event.setCanceled(true);
                CombatNetwork.send(CombatNetwork.Ability.FIRE_TOKEN_REVOLVER);
            }
        }
    }
}
