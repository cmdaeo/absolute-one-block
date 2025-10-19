package com.zwcess.absoluteoneblock.event;

import com.zwcess.absoluteoneblock.AbsoluteOneBlock;
import com.zwcess.absoluteoneblock.item.PlatformBuilderToolItem;
import com.zwcess.absoluteoneblock.network.C2SUpdatePlatformSizePacket;
import com.zwcess.absoluteoneblock.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AbsoluteOneBlock.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientForgeEvents {

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !player.isShiftKeyDown()) return;

        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof PlatformBuilderToolItem) {
            event.setCanceled(true);

            int width = PlatformBuilderToolItem.getPlatformWidth(stack);
            int height = PlatformBuilderToolItem.getPlatformHeight(stack);
            
            int change = event.getScrollDelta() > 0 ? 2 : -2; 
            int newWidth = width + change;
            int newHeight = height + change;

            newWidth = Math.max(1, newWidth);
            newHeight = Math.max(1, newHeight);

            if (newWidth * newHeight > PlatformBuilderToolItem.MAX_BLOCKS) {
                return;
            }

            PacketHandler.sendToServer(new C2SUpdatePlatformSizePacket(newWidth, newHeight));
            PlatformBuilderToolItem.setPlatformSize(stack, newWidth, newHeight);

            player.displayClientMessage(Component.literal("Platform Size: " + newWidth + "x" + newHeight), true);
        }
    }
}
