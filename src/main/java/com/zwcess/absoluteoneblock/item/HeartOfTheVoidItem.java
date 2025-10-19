package com.zwcess.absoluteoneblock.item;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class HeartOfTheVoidItem extends Item {

    public HeartOfTheVoidItem(Properties properties) {
        super(properties);
    }

    // This method makes the item always have the enchanted glint effect.
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    // This makes the item's name appear in a special color (purple).
    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.EPIC;
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        // This line adds your custom tooltip from the lang file.
        pTooltipComponents.add(Component.translatable("item.absoluteoneblock.heart_of_the_void.tooltip"));
        
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }
}
