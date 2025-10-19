package com.zwcess.absoluteoneblock.core;

import com.zwcess.absoluteoneblock.AbsoluteOneBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AbsoluteOneBlock.MOD_ID);

    public static final RegistryObject<CreativeModeTab> ONE_BLOCK_TAB = CREATIVE_MODE_TABS.register("one_block_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(Registration.ONE_BLOCK_ITEM.get()))
                    .title(Component.translatable("creativetab.absoluteoneblock.one_block_tab"))
                    .displayItems((displayParameters, output) -> {
                        output.accept(Registration.ONE_BLOCK_ITEM.get());
                        output.accept(Registration.HEART_OF_THE_VOID.get());
                        output.accept(Registration.PLATFORM_BUILDER_TOOL.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
