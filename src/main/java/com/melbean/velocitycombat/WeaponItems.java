package com.melbean.velocitycombat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
    private static final int SHOTGUN_COOLDOWN = 8;
    private static final int REVOLVER_COOLDOWN = 10;
    private static final int TOKEN_LAUNCH_COOLDOWN = 5;
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, VelocityCombat.MOD_ID);

    public static final RegistryObject<Item> BREACH_SHOTGUN =
            ITEMS.register("breach_shotgun", BreachShotgunItem::new);
    public static final RegistryObject<Item> TOKEN_REVOLVER =
            ITEMS.register("token_revolver", TokenRevolverItem::new);
    static final RegistryObject<Item> TOKEN_ROUND = ITEMS.register("token_round",
            () -> new Item(new Item.Properties().stacksTo(1)));

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

    static void fireTokenRevolver(ServerPlayer player) {
        Item weapon = TOKEN_REVOLVER.get();
        if (!player.getMainHandItem().is(weapon) || player.getCooldowns().isOnCooldown(weapon)) {
            return;
        }

        player.getCooldowns().addCooldown(weapon, REVOLVER_COOLDOWN);
        Vec3 origin = player.getEyePosition();
        Vec3 direction = player.getLookAngle();
        TargetHit targetHit = traceTarget(player.level(), player, origin, direction, 32.0D);
        TokenHit tokenHit = traceToken(player.level(), player, origin, direction, 32.0D);

        if (tokenHit != null && (targetHit == null
                || tokenHit.location().distanceToSqr(origin)
                        <= targetHit.location().distanceToSqr(origin))) {
            if (tokenHit.projectile().ricochetTowardNearestEnemy(player)) {
                addCoinParticles(player.level(), tokenHit.location());
            }
        } else if (targetHit != null
                && targetHit.target().hurt(player.damageSources().playerAttack(player), 8.0F)) {
            targetHit.target().knockback(0.25D,
                    player.getX() - targetHit.target().getX(),
                    player.getZ() - targetHit.target().getZ());
        }

        player.level().playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_SHOOT,
                SoundSource.PLAYERS, 0.9F, 1.25F);
    }

    static LivingEntity findNearestEnemy(Level level, Vec3 from, Player owner, double range) {
        AABB searchBox = new AABB(from, from).inflate(range);
        List<LivingEntity> candidates = new ArrayList<>(
                level.getEntitiesOfClass(LivingEntity.class, searchBox,
                        candidate -> candidate != owner && candidate.isAlive()
                                && !candidate.isAlliedTo(owner)));
        candidates.sort(Comparator.comparingDouble(candidate ->
                candidate.getBoundingBox().getCenter().distanceToSqr(from)));

        for (LivingEntity candidate : candidates) {
            Vec3 destination = candidate.getBoundingBox().getCenter();
            BlockHitResult blockHit = level.clip(new ClipContext(from, destination,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
            if (blockHit.getType() == HitResult.Type.MISS) {
                return candidate;
            }
        }
        return null;
    }

    private static void fireShotgun(ServerPlayer player) {
        Item weapon = BREACH_SHOTGUN.get();
        if (player.getCooldowns().isOnCooldown(weapon)) {
            return;
        }
        player.getCooldowns().addCooldown(weapon, SHOTGUN_COOLDOWN);

        Vec3 origin = player.getEyePosition();
        Vec3 aim = player.getLookAngle();
        for (int pellet = 0; pellet < 8; pellet++) {
            Vec3 direction = aim.add(
                    (player.getRandom().nextDouble() - 0.5D) * 0.12D,
                    (player.getRandom().nextDouble() - 0.5D) * 0.12D,
                    (player.getRandom().nextDouble() - 0.5D) * 0.12D).normalize();
            TargetHit hit = traceTarget(player.level(), player, origin, direction, 18.0D);
            if (hit != null && hit.target().hurt(
                    player.damageSources().playerAttack(player), 2.25F)) {
                hit.target().knockback(0.25D,
                        player.getX() - hit.target().getX(),
                        player.getZ() - hit.target().getZ());
            }
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_SHOOT,
                SoundSource.PLAYERS, 0.9F, 0.75F);
    }

    private static TargetHit traceTarget(Level level, Player player, Vec3 origin,
            Vec3 direction, double maxRange) {
        Vec3 end = clipToBlocks(level, player, origin, origin.add(direction.scale(maxRange)));
        AABB searchBox = new AABB(origin, end).inflate(1.0D);
        return level.getEntitiesOfClass(LivingEntity.class, searchBox,
                        candidate -> candidate != player && candidate.isAlive()
                                && !candidate.isAlliedTo(player))
                .stream()
                .map(candidate -> candidate.getBoundingBox().inflate(0.3D).clip(origin, end)
                        .map(location -> new TargetHit(candidate, location)))
                .flatMap(Optional::stream)
                .min(Comparator.comparingDouble(hit ->
                        hit.location().distanceToSqr(origin)))
                .orElse(null);
    }

    private static TokenHit traceToken(Level level, Player player, Vec3 origin,
            Vec3 direction, double maxRange) {
        Vec3 end = clipToBlocks(level, player, origin, origin.add(direction.scale(maxRange)));
        AABB searchBox = new AABB(origin, end).inflate(1.0D);
        return level.getEntitiesOfClass(TokenProjectile.class, searchBox,
                        projectile -> projectile.isAlive() && !projectile.isRicocheting())
                .stream()
                .map(projectile -> projectile.getBoundingBox().inflate(0.3D).clip(origin, end)
                        .map(location -> new TokenHit(projectile, location)))
                .flatMap(Optional::stream)
                .min(Comparator.comparingDouble(hit ->
                        hit.location().distanceToSqr(origin)))
                .orElse(null);
    }

    private static Vec3 clipToBlocks(Level level, Player player, Vec3 start, Vec3 end) {
        BlockHitResult blockHit = level.clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
    }

    private static void addCoinParticles(Level level, Vec3 position) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT, position.x, position.y, position.z,
                    7, 0.12D, 0.12D, 0.12D, 0.08D);
            serverLevel.sendParticles(ParticleTypes.WAX_ON, position.x, position.y, position.z,
                    5, 0.08D, 0.08D, 0.08D, 0.0D);
        }
    }

    private static final class BreachShotgunItem extends Item {
        private BreachShotgunItem() {
            super(new Item.Properties().stacksTo(1));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                fireShotgun(serverPlayer);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

    private static final class TokenRevolverItem extends Item {
        private TokenRevolverItem() {
            super(new Item.Properties().stacksTo(1));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)
                    || player.getCooldowns().isOnCooldown(TOKEN_ROUND.get())) {
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }

            player.getCooldowns().addCooldown(TOKEN_ROUND.get(), TOKEN_LAUNCH_COOLDOWN);
            TokenProjectile projectile = new TokenProjectile(
                    CombatEntities.TOKEN_PROJECTILE.get(), serverPlayer, level);
            projectile.setItem(TOKEN_ROUND.get().getDefaultInstance());
            Vec3 aim = player.getLookAngle();
            projectile.shoot(aim.x, aim.y, aim.z, 1.8F, 0.0F);
            level.addFreshEntity(projectile);
            level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW,
                    SoundSource.PLAYERS, 0.8F, 1.25F);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }
    }

    private record TargetHit(LivingEntity target, Vec3 location) {
    }

    private record TokenHit(TokenProjectile projectile, Vec3 location) {
    }
}
