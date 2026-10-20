package com.melbean.velocitycombat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;

public final class TokenProjectile extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Boolean> RICOCHETING =
            SynchedEntityData.defineId(TokenProjectile.class, EntityDataSerializers.BOOLEAN);
    private static final double TARGET_RANGE = 32.0D;
    private static final double RICOCHET_SPEED = 1.7D;

    public TokenProjectile(EntityType<? extends TokenProjectile> entityType, Level level) {
        super(entityType, level);
        setNoGravity(false);
    }

    public TokenProjectile(EntityType<? extends TokenProjectile> entityType, LivingEntity owner, Level level) {
        super(entityType, owner, level);
        setPos(owner.getX(), owner.getEyeY() - 0.1D, owner.getZ());
        setNoGravity(false);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(RICOCHETING, false);
    }

    @Override
    protected net.minecraft.world.item.Item getDefaultItem() {
        return WeaponItems.TOKEN_ROUND.get();
    }

    public boolean isRicocheting() {
        return entityData.get(RICOCHETING);
    }

    public boolean ricochetTowardNearestEnemy(Player player) {
        LivingEntity target = WeaponItems.findNearestEnemy(level(), position(), player, TARGET_RANGE);
        if (target == null) {
            return false;
        }
        setOwner(player);
        entityData.set(RICOCHETING, true);
        setNoGravity(true);
        Vec3 direction = target.getBoundingBox().getCenter().subtract(position()).normalize();
        setDeltaMovement(direction.scale(RICOCHET_SPEED));
        hasImpulse = true;
        return true;
    }

    @Override
    public void tick() {
        if (tickCount > 100) {
            discard();
            return;
        }
        if (isRicocheting()) {
            setNoGravity(true);
            if (!level().isClientSide && getOwner() instanceof Player owner) {
                LivingEntity target = WeaponItems.findNearestEnemy(
                        level(), position(), owner, TARGET_RANGE);
                if (target != null) {
                    Vec3 direction = target.getBoundingBox().getCenter().subtract(position()).normalize();
                    setDeltaMovement(direction.scale(RICOCHET_SPEED));
                }
            }
        }
        super.tick();
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        Entity entity = hit.getEntity();
        Entity owner = getOwner();
        if (!level().isClientSide && entity instanceof LivingEntity target && entity != owner
                && (owner == null || !target.isAlliedTo(owner))) {
            target.hurt(level().damageSources().thrown(this, owner),
                    isRicocheting() ? 8.0F : 4.0F);
        }
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        if (!level().isClientSide) {
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Ricocheting", isRicocheting());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(RICOCHETING, tag.getBoolean("Ricocheting"));
        setNoGravity(isRicocheting());
    }
}
