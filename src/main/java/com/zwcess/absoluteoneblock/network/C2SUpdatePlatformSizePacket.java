package com.zwcess.absoluteoneblock.network;

import com.zwcess.absoluteoneblock.item.PlatformBuilderToolItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SUpdatePlatformSizePacket {
    private final int width;
    private final int height;

    public C2SUpdatePlatformSizePacket(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public static void encode(C2SUpdatePlatformSizePacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.width);
        buf.writeInt(msg.height);
    }

    public static C2SUpdatePlatformSizePacket decode(FriendlyByteBuf buf) {
        return new C2SUpdatePlatformSizePacket(buf.readInt(), buf.readInt());
    }

    public static void handle(C2SUpdatePlatformSizePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof PlatformBuilderToolItem) {
                PlatformBuilderToolItem.setPlatformSize(stack, msg.width, msg.height);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
