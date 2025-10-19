package com.zwcess.absoluteoneblock.event;

import com.zwcess.absoluteoneblock.AbsoluteOneBlock;
import com.zwcess.absoluteoneblock.core.Registration;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = AbsoluteOneBlock.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ItemCapabilityEventHandler {

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<ItemStack> event) {
        ItemStack stack = event.getObject();
        if (stack.getItem() == Registration.PLATFORM_BUILDER_TOOL.get()) {
            ICapabilitySerializable<CompoundTag> provider = new ICapabilitySerializable<>() {
                private final ItemStackHandler handler = new ItemStackHandler(9);
                private final LazyOptional<IItemHandler> optional = LazyOptional.of(() -> handler);

                @Override
                public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
                    return cap == ForgeCapabilities.ITEM_HANDLER ? optional.cast() : LazyOptional.empty();
                }

                @Override
                public CompoundTag serializeNBT() {
                    return handler.serializeNBT();
                }

                @Override
                public void deserializeNBT(CompoundTag nbt) {
                    handler.deserializeNBT(nbt);
                }
            };

            event.addCapability(new ResourceLocation(AbsoluteOneBlock.MOD_ID, "platform_builder_inventory"), provider);
        }
    }
}
