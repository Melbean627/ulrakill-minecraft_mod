package com.melbean.velocitycombat;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class CombatNetwork {
    private static final String PROTOCOL_VERSION = "4";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(VelocityCombat.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();
    private static int packetId;

    private CombatNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(packetId++, AbilityPacket.class, AbilityPacket::encode,
                AbilityPacket::decode, AbilityPacket::handle);
        CHANNEL.registerMessage(packetId++, ParryFeedbackPacket.class, ParryFeedbackPacket::encode,
                ParryFeedbackPacket::decode, ParryFeedbackPacket::handle);
    }

    public static void send(Ability ability) {
        CHANNEL.sendToServer(new AbilityPacket(ability));
    }

    static void sendParryFeedback(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ParryFeedbackPacket());
    }

    public enum Ability {
        DASH,
        SLIDE,
        WALL_KICK,
        GROUND_SLAM,
        PARRY,
        SWITCH_WEAPON,
        FIRE_TOKEN_REVOLVER
    }

    private record AbilityPacket(Ability ability) {
        private static void encode(AbilityPacket packet, FriendlyByteBuf buffer) {
            buffer.writeEnum(packet.ability);
        }

        private static AbilityPacket decode(FriendlyByteBuf buffer) {
            return new AbilityPacket(buffer.readEnum(Ability.class));
        }

        private static void handle(AbilityPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    CombatEvents.handleAbility(player, packet.ability);
                }
            });
            context.setPacketHandled(true);
        }
    }

    private record ParryFeedbackPacket() {
        private static void encode(ParryFeedbackPacket packet, FriendlyByteBuf buffer) {
        }

        private static ParryFeedbackPacket decode(FriendlyByteBuf buffer) {
            return new ParryFeedbackPacket();
        }

        private static void handle(ParryFeedbackPacket packet,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT, () -> ClientParryFeedback::trigger));
            context.setPacketHandled(true);
        }
    }
}
