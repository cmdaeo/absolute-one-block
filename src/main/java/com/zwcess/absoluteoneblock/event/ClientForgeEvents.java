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
            // We are holding the tool and sneaking, so change the size instead of scrolling the hotbar
            event.setCanceled(true);

            int width = PlatformBuilderToolItem.getPlatformWidth(stack);
            int height = PlatformBuilderToolItem.getPlatformHeight(stack);
            
            // Scrolling up increases size, scrolling down decreases
            int change = event.getScrollDelta() > 0 ? 2 : -2; // Change by 2 to go from 1x1 to 3x3, etc.
            int newWidth = width + change;
            int newHeight = height + change;

            // Clamp the values to be at least 1 and odd
            newWidth = Math.max(1, newWidth);
            newHeight = Math.max(1, newHeight);

            // Enforce the maximum block count (576)
            if (newWidth * newHeight > PlatformBuilderToolItem.MAX_BLOCKS) {
                // Don't apply the change if it exceeds the max
                return;
            }

            // Send the update to the server
            PacketHandler.sendToServer(new C2SUpdatePlatformSizePacket(newWidth, newHeight));
            // Also update the client-side stack immediately for responsiveness
            PlatformBuilderToolItem.setPlatformSize(stack, newWidth, newHeight);

            // Show feedback to the player on the action bar
            player.displayClientMessage(Component.literal("Platform Size: " + newWidth + "x" + newHeight), true);
        }
    }
}
