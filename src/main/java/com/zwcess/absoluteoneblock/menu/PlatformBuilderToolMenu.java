package com.zwcess.absoluteoneblock.menu;

import com.zwcess.absoluteoneblock.core.Registration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;

public class PlatformBuilderToolMenu extends AbstractContainerMenu {

    private final ItemStack toolStack;

    // Client-side ctor (from network buffer)
    public PlatformBuilderToolMenu(int windowId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(windowId, playerInventory, playerInventory.player.getMainHandItem());
    }

    // Server-side ctor (from MenuProvider)
    public PlatformBuilderToolMenu(int windowId, Inventory playerInventory, ItemStack toolStack) {
        super(Registration.PLATFORM_BUILDER_TOOL_MENU.get(), windowId);
        this.toolStack = toolStack;

        // Tool's internal 3x3 inventory
        toolStack.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    addSlot(new SlotItemHandler(handler, col + row * 3, 62 + col * 18, 17 + row * 18));
                }
            }
        });

        // Player main inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack returnStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            returnStack = current.copy();

            final int toolInvSize = 9;           // 0..8
            final int playerInvStart = toolInvSize;   // 9..35
            final int hotbarStart = playerInvStart + 27; // 36..44
            final int totalSlots = hotbarStart + 9; // 45

            if (index < toolInvSize) {
                // Move from tool -> player inventory
                if (!moveItemStackTo(current, playerInvStart, totalSlots, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move from player -> tool
                if (!moveItemStackTo(current, 0, toolInvSize, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (current.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return returnStack;
    }

    @Override
    public boolean stillValid(Player player) {
        // Close if player no longer holds the tool or is dead/removed
        if (!player.isAlive() || player.isRemoved()) return false;
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return main == this.toolStack || off == this.toolStack;
    }
}
