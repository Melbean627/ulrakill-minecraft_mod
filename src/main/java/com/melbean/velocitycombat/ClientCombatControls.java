package com.melbean.velocitycombat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
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
            while (PARRY.consumeClick()) {
                CombatNetwork.send(CombatNetwork.Ability.PARRY);
            }
            while (SWITCH_WEAPON.consumeClick()) {
                CombatNetwork.send(CombatNetwork.Ability.SWITCH_WEAPON);
            }
        }
    }
}
