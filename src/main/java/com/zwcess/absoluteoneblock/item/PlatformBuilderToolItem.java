package com.zwcess.absoluteoneblock.item;

import com.zwcess.absoluteoneblock.menu.PlatformBuilderToolMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

public class PlatformBuilderToolItem extends Item implements MenuProvider {
    public static final int MAX_BLOCKS = 64 * 9; 
    private static final String NBT_WIDTH = "PlatformWidth";
    private static final String NBT_HEIGHT = "PlatformHeight";

    public PlatformBuilderToolItem(Properties properties) {
        super(properties.stacksTo(1).durability(256));
    }

    public static int getPlatformWidth(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(NBT_WIDTH) ? Math.max(1, tag.getInt(NBT_WIDTH)) : 3;
    }

    public static int getPlatformHeight(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(NBT_HEIGHT) ? Math.max(1, tag.getInt(NBT_HEIGHT)) : 3;
    }

    public static void setPlatformSize(ItemStack stack, int width, int height) {
        width = Math.max(1, width);
        height = Math.max(1, height);

        long area = (long) width * (long) height;
        if (area > MAX_BLOCKS) {
            double scale = Math.sqrt((double) MAX_BLOCKS / (double) area);
            int newWidth = Math.max(1, (int) Math.floor(width * scale));
            int newHeight = Math.max(1, (int) Math.floor(height * scale));

            while ((long) newWidth * (long) newHeight > MAX_BLOCKS) {
                if (newWidth >= newHeight && newWidth > 1) newWidth--;
                else if (newHeight > 1) newHeight--;
                else break;
            }
            width = newWidth;
            height = newHeight;
        }

        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(NBT_WIDTH, width);
        tag.putInt(NBT_HEIGHT, height);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, this, buf -> buf.writeItem(stack));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            boolean success = buildPlatform(serverPlayer, stack);
            if (success) {
                stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
                return InteractionResultHolder.success(stack);
            }
        }
        return InteractionResultHolder.fail(stack);
    }

    @SuppressWarnings("null")
    private boolean buildPlatform(ServerPlayer player, ItemStack tool) {
        IItemHandler handler = tool.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        if (handler == null) return false;

        final int width = getPlatformWidth(tool);
        final int height = getPlatformHeight(tool);
        final long area = (long) width * (long) height;
        if (area > MAX_BLOCKS) return false; 

        final int halfW = width / 2;
        final int halfH = height / 2;

        Level level = player.level();
        BlockPos base = player.blockPosition().below();
        boolean placedAny = false;

        for (int dx = -halfW; dx <= halfW; dx++) {
            for (int dz = -halfH; dz <= halfH; dz++) {
                BlockPos target = base.offset(dx, 0, dz);
                if (!level.isEmptyBlock(target)) continue;

                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    ItemStack blockStack = handler.getStackInSlot(slot);
                    if (blockStack.isEmpty() || !(blockStack.getItem() instanceof BlockItem blockItem)) continue;

                    BlockState state = blockItem.getBlock().defaultBlockState();
                    if (!state.canSurvive(level, target)) continue;

                    level.setBlock(target, state, 3);
                    handler.extractItem(slot, 1, false);
                    placedAny = true;
                    break; 
                }
            }
        }

        return placedAny;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("item.absoluteoneblock.platform_builder");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new PlatformBuilderToolMenu(windowId, playerInventory, player.getMainHandItem());
    }
}
