package com.zwcess.absoluteoneblock.network;

import com.zwcess.absoluteoneblock.AbsoluteOneBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AbsoluteOneBlock.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;
        INSTANCE.messageBuilder(SyncProgressS2CPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncProgressS2CPacket::new)
                .encoder(SyncProgressS2CPacket::toBytes)
                .consumerMainThread(SyncProgressS2CPacket::handle)
                .add();
        INSTANCE.registerMessage(id++, C2SUpdatePlatformSizePacket.class, C2SUpdatePlatformSizePacket::encode, C2SUpdatePlatformSizePacket::decode, C2SUpdatePlatformSizePacket::handle);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }
}
