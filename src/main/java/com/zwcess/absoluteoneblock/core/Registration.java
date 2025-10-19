// main.java.com.zwcess.absoluteoneblock.core.Registration.java
package com.zwcess.absoluteoneblock.core;

import com.zwcess.absoluteoneblock.AbsoluteOneBlock;
import com.zwcess.absoluteoneblock.block.OneBlock;
import com.zwcess.absoluteoneblock.dimension.OneBlockChunkGenerator;
import com.zwcess.absoluteoneblock.item.HeartOfTheVoidItem;
import com.zwcess.absoluteoneblock.item.PlatformBuilderToolItem;
import com.zwcess.absoluteoneblock.loot.AddItemModifier;
import com.zwcess.absoluteoneblock.menu.PlatformBuilderToolMenu;
import com.mojang.serialization.Codec;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class Registration {

        public static final DeferredRegister<Block> BLOCKS =
                DeferredRegister.create(ForgeRegistries.BLOCKS, AbsoluteOneBlock.MOD_ID);

        public static final DeferredRegister<Item> ITEMS =
                DeferredRegister.create(ForgeRegistries.ITEMS, AbsoluteOneBlock.MOD_ID);
        
        public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, AbsoluteOneBlock.MOD_ID);

        public static final RegistryObject<Item> HEART_OF_THE_VOID = ITEMS.register("heart_of_the_void",
        () -> new HeartOfTheVoidItem(new Item.Properties()));

        public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
                DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AbsoluteOneBlock.MOD_ID);

        public static final RegistryObject<Block> ONE_BLOCK = BLOCKS.register("one_block", OneBlock::new);

        public static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATORS =
                DeferredRegister.create(Registries.CHUNK_GENERATOR, AbsoluteOneBlock.MOD_ID);

        public static final RegistryObject<Codec<OneBlockChunkGenerator>> ONE_BLOCK_CHUNK_GENERATOR =
                CHUNK_GENERATORS.register("one_block", () -> OneBlockChunkGenerator.CODEC);
        
        public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS =
        DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, AbsoluteOneBlock.MOD_ID);

        public static final RegistryObject<Codec<? extends IGlobalLootModifier>> ADD_ITEM =
        LOOT_MODIFIER_SERIALIZERS.register("add_item", AddItemModifier.CODEC);

        public static final RegistryObject<Item> ONE_BLOCK_ITEM = ITEMS.register("one_block",
                () -> new BlockItem(ONE_BLOCK.get(), new Item.Properties()));
        
        public static final RegistryObject<Item> PLATFORM_BUILDER_TOOL = ITEMS.register(
        "platform_builder_tool", 
                () -> new PlatformBuilderToolItem(new Item.Properties())
        );

        public static final RegistryObject<MenuType<PlatformBuilderToolMenu>> PLATFORM_BUILDER_TOOL_MENU =
                MENUS.register("platform_builder_tool_menu",
                        () -> IForgeMenuType.create((windowId, inv, buf) -> new PlatformBuilderToolMenu(windowId, inv, buf))
                );

        public static void register(IEventBus eventBus) {
                CHUNK_GENERATORS.register(eventBus);
                BLOCKS.register(eventBus);
                ITEMS.register(eventBus);
                BLOCK_ENTITIES.register(eventBus);
                MENUS.register(eventBus);
                LOOT_MODIFIER_SERIALIZERS.register(eventBus);
        }
}
