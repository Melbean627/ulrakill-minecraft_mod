package com.melbean.velocitycombat;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class CombatEvents {
    private static final String DASH_COOLDOWN = "velocitycombat_dash_cooldown";
    private static final String SLIDE_TICKS = "velocitycombat_slide_ticks";
    private static final String SLIDE_COOLDOWN = "velocitycombat_slide_cooldown";
    private static final String WALL_KICK_COOLDOWN = "velocitycombat_wall_kick_cooldown";
    private static final String GROUND_SLAM_COOLDOWN = "velocitycombat_ground_slam_cooldown";
    private static final String GROUND_SLAM_ACTIVE = "velocitycombat_ground_slam_active";
    private static final String GROUND_SLAM_TICKS = "velocitycombat_ground_slam_ticks";
    private static final String VITALITY_COOLDOWN = "velocitycombat_vitality_cooldown";
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
        decrement(data, SLIDE_TICKS);
        decrement(data, SLIDE_COOLDOWN);
        decrement(data, WALL_KICK_COOLDOWN);
        decrement(data, GROUND_SLAM_COOLDOWN);
        decrement(data, VITALITY_COOLDOWN);
        decrement(data, PARRY_COOLDOWN);
        decrement(data, PARRY_TICKS);
        tickGroundSlam(event.player);
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
            if (event.getSource().getDirectEntity() == player
                    && player.distanceToSqr(event.getEntity()) <= 9.0D
                    && !player.getMainHandItem().is(WeaponItems.BREACH_SHOTGUN.get())
                    && !player.getMainHandItem().is(WeaponItems.TOKEN_REVOLVER.get())
                    && player.getPersistentData().getInt(GROUND_SLAM_COOLDOWN) <= 0
                    && player.getHealth() < player.getMaxHealth()
                    && player.getPersistentData().getInt(VITALITY_COOLDOWN) <= 0) {
                player.heal(1.0F);
                player.getPersistentData().putInt(VITALITY_COOLDOWN, 20);
            }
        }
    }

    @SubscribeEvent
    public void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.getPersistentData().getBoolean(GROUND_SLAM_ACTIVE)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.getPersistentData().getInt(SLIDE_TICKS) <= 0) {
            return;
        }

        Vec3 movement = player.getDeltaMovement();
        Vec3 direction = movement.multiply(1.0D, 0.0D, 1.0D);
        if (direction.lengthSqr() < 0.001D) {
            direction = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        }
        if (direction.lengthSqr() < 0.001D) {
            direction = new Vec3(0.0D, 0.0D, 1.0D).yRot(
                    -player.getYRot() * ((float) Math.PI / 180F));
        }

        double forwardSpeed = Math.max(Math.sqrt(movement.x * movement.x + movement.z * movement.z) * 1.5D,
                0.52D);
        Vec3 boostedDirection = direction.normalize().scale(forwardSpeed);
        player.setDeltaMovement(boostedDirection.x, Math.min(movement.y, 0.28D), boostedDirection.z);
        player.getPersistentData().putInt(SLIDE_TICKS, 0);
        player.hasImpulse = true;
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
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
                player.connection.send(new ClientboundSetEntityMotionPacket(player));
                player.setSprinting(true);
                data.putInt(DASH_COOLDOWN, 20);
            }
            case SLIDE -> {
                if (data.getInt(SLIDE_COOLDOWN) > 0 || !player.onGround()) {
                    return;
                }

                Vec3 movement = player.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D);
                if (!player.isSprinting() && movement.lengthSqr() <= 0.01D) {
                    return;
                }

                Vec3 direction = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
                if (direction.lengthSqr() < 0.001D) {
                    direction = movement;
                }
                if (direction.lengthSqr() < 0.001D) {
                    return;
                }

                Vec3 slideVelocity = direction.normalize().scale(0.48D);
                player.setDeltaMovement(slideVelocity.x, movement.y, slideVelocity.z);
                player.getPersistentData().putInt(SLIDE_TICKS, 12);
                player.getPersistentData().putInt(SLIDE_COOLDOWN, 8);
                player.hasImpulse = true;
                player.connection.send(new ClientboundSetEntityMotionPacket(player));
            }
            case WALL_KICK -> wallKick(player);
            case GROUND_SLAM -> startGroundSlam(player);
            case PARRY -> {
                if (data.getInt(PARRY_COOLDOWN) > 0) {
                    return;
                }
                data.putInt(PARRY_TICKS, 8);
                data.putInt(PARRY_COOLDOWN, 40);
                player.displayClientMessage(Component.literal("PARRY READY"), true);
            }
            case SWITCH_WEAPON -> switchWeapon(player);
            case FIRE_TOKEN_REVOLVER -> WeaponItems.fireTokenRevolver(player);
        }
    }

    private static void wallKick(ServerPlayer player) {
        var data = player.getPersistentData();
        if (player.onGround() || data.getInt(WALL_KICK_COOLDOWN) > 0
                || data.getBoolean(GROUND_SLAM_ACTIVE)) {
            return;
        }

        AABB bounds = player.getBoundingBox();
        Vec3[] wallDirections = {
                new Vec3(0.0D, 0.0D, 1.0D),
                new Vec3(0.0D, 0.0D, -1.0D),
                new Vec3(1.0D, 0.0D, 0.0D),
                new Vec3(-1.0D, 0.0D, 0.0D)
        };
        for (Vec3 towardWall : wallDirections) {
            if (player.level().noCollision(player, bounds.move(towardWall.scale(0.28D)))) {
                continue;
            }

            Vec3 kick = towardWall.scale(-0.62D);
            player.setDeltaMovement(kick.x, Math.max(player.getDeltaMovement().y, 0.52D), kick.z);
            player.hasImpulse = true;
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
            data.putInt(WALL_KICK_COOLDOWN, 8);
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 0.6F, 1.5F);
            return;
        }
    }

    private static void startGroundSlam(ServerPlayer player) {
        var data = player.getPersistentData();
        if (player.onGround() || data.getInt(GROUND_SLAM_COOLDOWN) > 0
                || data.getBoolean(GROUND_SLAM_ACTIVE)) {
            return;
        }

        data.putBoolean(GROUND_SLAM_ACTIVE, true);
        data.putInt(GROUND_SLAM_TICKS, 60);
        data.putInt(GROUND_SLAM_COOLDOWN, 40);
        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x, Math.min(movement.y, -1.8D), movement.z);
        player.hasImpulse = true;
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }

    private static void tickGroundSlam(Player player) {
        var data = player.getPersistentData();
        if (!data.getBoolean(GROUND_SLAM_ACTIVE)) {
            return;
        }

        int ticksRemaining = data.getInt(GROUND_SLAM_TICKS) - 1;
        data.putInt(GROUND_SLAM_TICKS, ticksRemaining);
        if (player.onGround()) {
            data.putBoolean(GROUND_SLAM_ACTIVE, false);
            data.putInt(GROUND_SLAM_TICKS, 0);
            if (player instanceof ServerPlayer serverPlayer) {
                groundSlamImpact(serverPlayer);
            }
        } else if (ticksRemaining <= 0) {
            data.putBoolean(GROUND_SLAM_ACTIVE, false);
            data.putInt(GROUND_SLAM_TICKS, 0);
        }
    }

    private static void groundSlamImpact(ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(4.0D, 1.5D, 4.0D);
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, area,
                candidate -> candidate != player && candidate.isAlive() && !candidate.isAlliedTo(player))) {
            target.hurt(player.damageSources().playerAttack(player), 6.0F);
            double dx = target.getX() - player.getX();
            double dz = target.getZ() - player.getZ();
            target.knockback(0.8D, -dx, -dz);
            target.setDeltaMovement(target.getDeltaMovement().add(0.0D, 0.25D, 0.0D));
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 0.8F, 1.2F);
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.1D,
                    player.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1D,
                    player.getZ(), 24, 1.5D, 0.1D, 1.5D, 0.08D);
        }
        awardStyle(player, 250, "GROUND SLAM");
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
