package com.melbean.velocitycombat;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class CombatEvents {
    private static final String DASH_COOLDOWN = "velocitycombat_dash_cooldown";
    private static final String PARRY_COOLDOWN = "velocitycombat_parry_cooldown";
    private static final String PARRY_TICKS = "velocitycombat_parry_ticks";
    private static final String COMBO = "velocitycombat_combo";
    private static final String SCORE = "velocitycombat_score";
    private static final String COMBO_TICKS = "velocitycombat_combo_ticks";

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }

        var data = event.player.getPersistentData();
        decrement(data, DASH_COOLDOWN);
        decrement(data, PARRY_COOLDOWN);
        decrement(data, PARRY_TICKS);
        int comboTicks = data.getInt(COMBO_TICKS);
        if (comboTicks > 0) {
            data.putInt(COMBO_TICKS, comboTicks - 1);
            if (comboTicks == 1) {
                data.putInt(COMBO, 0);
            }
        }
    }

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player
                && event.getAmount() > 0.0F) {
            awardStyle(player, 100, "HIT");
        }
    }

    @SubscribeEvent
    public void onLivingAttacked(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.getPersistentData().getInt(PARRY_TICKS) <= 0
                || !(event.getSource().getEntity() instanceof LivingEntity attacker)
                || attacker == player) {
            return;
        }

        event.setCanceled(true);
        attacker.hurt(player.damageSources().playerAttack(player), 4.0F);
        attacker.knockback(0.8D, player.getX() - attacker.getX(), player.getZ() - attacker.getZ());
        player.getPersistentData().putInt(PARRY_TICKS, 0);
        CombatNetwork.sendParryFeedback(player);
        awardStyle(player, 500, "PARRY");
    }

    static void handleAbility(ServerPlayer player, CombatNetwork.Ability ability) {
        var data = player.getPersistentData();
        switch (ability) {
            case DASH -> {
                if (data.getInt(DASH_COOLDOWN) > 0) {
                    return;
                }
                Vec3 direction = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
                if (direction.lengthSqr() < 0.001D) {
                    direction = new Vec3(0.0D, 0.0D, 1.0D).yRot(
                            -player.getYRot() * ((float) Math.PI / 180F));
                }
                Vec3 dashVelocity = direction.normalize().scale(1.45D);
                player.setDeltaMovement(dashVelocity.x,
                        Math.max(player.getDeltaMovement().y, 0.12D), dashVelocity.z);
                player.hasImpulse = true;
                player.setSprinting(true);
                data.putInt(DASH_COOLDOWN, 20);
            }
            case PARRY -> {
                if (data.getInt(PARRY_COOLDOWN) > 0) {
                    return;
                }
                data.putInt(PARRY_TICKS, 8);
                data.putInt(PARRY_COOLDOWN, 40);
                player.displayClientMessage(Component.literal("PARRY READY"), true);
            }
            case SWITCH_WEAPON -> switchWeapon(player);
        }
    }

    private static void switchWeapon(ServerPlayer player) {
        int selected = player.getInventory().selected;
        for (int offset = 1; offset < 9; offset++) {
            int slot = (selected + offset) % 9;
            if (!player.getInventory().getItem(slot).isEmpty()) {
                player.getInventory().selected = slot;
                player.connection.send(new ClientboundSetCarriedItemPacket(slot));
                return;
            }
        }
    }

    private static void awardStyle(ServerPlayer player, int points, String action) {
        var data = player.getPersistentData();
        int combo = data.getInt(COMBO) + 1;
        int score = data.getInt(SCORE) + points * Math.min(combo, 5);
        data.putInt(COMBO, combo);
        data.putInt(SCORE, score);
        data.putInt(COMBO_TICKS, 100);
        player.displayClientMessage(
                Component.literal(action + "  |  STYLE " + score + "  x" + combo), true);
    }

    private static void decrement(net.minecraft.nbt.CompoundTag data, String key) {
        int remaining = data.getInt(key);
        if (remaining > 0) {
            data.putInt(key, remaining - 1);
        }
    }
}
