package com.melbean.velocitycombat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class WeaponItems {
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, VelocityCombat.MOD_ID);

    public static final RegistryObject<Item> BREACH_SHOTGUN = ITEMS.register("breach_shotgun",
            () -> new HitscanWeapon(8, 18, 8, 2.25F, 0.12D, false));
    public static final RegistryObject<Item> TOKEN_REVOLVER = ITEMS.register("token_revolver",
            () -> new HitscanWeapon(1, 32, 12, 8.0F, 0.0D, true));

    private WeaponItems() {
    }

    static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(BREACH_SHOTGUN);
            event.accept(TOKEN_REVOLVER);
        }
    }

    private static final class HitscanWeapon extends Item {
        private final int pellets;
        private final int range;
        private final int cooldown;
        private final float damage;
        private final double spread;
        private final boolean ricochet;

        private HitscanWeapon(int pellets, int range, int cooldown, float damage,
                double spread, boolean ricochet) {
            super(new Item.Properties().stacksTo(1));
            this.pellets = pellets;
            this.range = range;
            this.cooldown = cooldown;
            this.damage = damage;
            this.spread = spread;
            this.ricochet = ricochet;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide) {
                return InteractionResultHolder.sidedSuccess(stack, true);
            }

            player.getCooldowns().addCooldown(this, cooldown);
            if (ricochet) {
                fireRicochet(level, player);
            } else {
                firePellets(level, player);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_SHOOT,
                    SoundSource.PLAYERS, 0.9F, ricochet ? 1.25F : 0.75F);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        private void firePellets(Level level, Player player) {
            Vec3 origin = player.getEyePosition();
            Vec3 aim = player.getLookAngle();
            for (int pellet = 0; pellet < pellets; pellet++) {
                Vec3 direction = aim.add(
                        (player.getRandom().nextDouble() - 0.5D) * spread,
                        (player.getRandom().nextDouble() - 0.5D) * spread,
                        (player.getRandom().nextDouble() - 0.5D) * spread).normalize();
                LivingEntity target = traceTarget(level, player, origin, direction, range);
                if (target != null && target.hurt(player.damageSources().playerAttack(player), damage)) {
                    target.knockback(0.25D, player.getX() - target.getX(), player.getZ() - target.getZ());
                }
            }
        }

        private void fireRicochet(Level level, Player player) {
            Vec3 origin = player.getEyePosition();
            Vec3 direction = player.getLookAngle();
            LivingEntity target = traceTarget(level, player, origin, direction, range);
            Set<LivingEntity> hitTargets = new HashSet<>();
            Vec3 coinPosition = origin.add(direction.scale(3.0D));
            addCoinParticles(level, coinPosition);

            for (int bounce = 0; target != null && bounce < 3; bounce++) {
                if (!target.hurt(player.damageSources().playerAttack(player), damage)) {
                    break;
                }
                hitTargets.add(target);
                Vec3 hitPosition = target.getBoundingBox().getCenter();
                addCoinParticles(level, hitPosition);
                target = findRicochetTarget(level, player, hitPosition, hitTargets);
            }
        }

        private LivingEntity traceTarget(Level level, Player player, Vec3 origin, Vec3 direction, double maxRange) {
            Vec3 end = origin.add(direction.scale(maxRange));
            BlockHitResult blockHit = level.clip(new ClipContext(origin, end,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (blockHit.getType() != HitResult.Type.MISS) {
                end = blockHit.getLocation();
            }

            Vec3 traceEnd = end;
            AABB searchBox = new AABB(origin, traceEnd).inflate(1.0D);
            return level.getEntitiesOfClass(LivingEntity.class, searchBox,
                            candidate -> candidate != player && candidate.isAlive() && !candidate.isAlliedTo(player))
                    .stream()
                    .map(candidate -> new TargetHit(candidate,
                            candidate.getBoundingBox().inflate(0.3D).clip(origin, traceEnd)))
                    .filter(hit -> hit.location().isPresent())
                    .min(Comparator.comparingDouble(hit -> hit.location().orElseThrow().distanceToSqr(origin)))
                    .map(TargetHit::target)
                    .orElse(null);
        }

        private LivingEntity findRicochetTarget(Level level, Player player, Vec3 from,
                Set<LivingEntity> alreadyHit) {
            AABB searchBox = new AABB(from, from).inflate(12.0D);
            List<LivingEntity> candidates = new ArrayList<>(
                    level.getEntitiesOfClass(LivingEntity.class, searchBox,
                            candidate -> candidate != player && candidate.isAlive()
                                    && !candidate.isAlliedTo(player) && !alreadyHit.contains(candidate)));
            candidates.sort(Comparator.comparingDouble(candidate ->
                    candidate.getBoundingBox().getCenter().distanceToSqr(from)));
            for (LivingEntity candidate : candidates) {
                Vec3 destination = candidate.getBoundingBox().getCenter();
                BlockHitResult blockHit = level.clip(new ClipContext(from, destination,
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                if (blockHit.getType() == HitResult.Type.MISS) {
                    return candidate;
                }
            }
            return null;
        }

        private void addCoinParticles(Level level, Vec3 position) {
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT, position.x, position.y, position.z,
                        7, 0.12D, 0.12D, 0.12D, 0.08D);
                serverLevel.sendParticles(ParticleTypes.WAX_ON, position.x, position.y, position.z,
                        5, 0.08D, 0.08D, 0.08D, 0.0D);
            }
        }

        private record TargetHit(LivingEntity target, java.util.Optional<Vec3> location) {
        }
    }
}
