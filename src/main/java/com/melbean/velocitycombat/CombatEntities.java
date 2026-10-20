package com.melbean.velocitycombat;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CombatEntities {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, VelocityCombat.MOD_ID);

    public static final RegistryObject<EntityType<TokenProjectile>> TOKEN_PROJECTILE =
            ENTITY_TYPES.register("token_projectile", () -> EntityType.Builder
                    .<TokenProjectile>of(TokenProjectile::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("token_projectile"));

    private CombatEntities() {
    }

    static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
