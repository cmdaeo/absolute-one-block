package com.zwcess.absoluteoneblock.util;

import com.zwcess.absoluteoneblock.AbsoluteOneBlock;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class AdvancementHelper {
    
    public static void grantAdvancement(ServerPlayer player, String advancementPath) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
            AbsoluteOneBlock.MOD_ID, 
            advancementPath
        );
        
        Advancement advancement = player.server.getAdvancements().getAdvancement(id);
        if (advancement != null) {
            var progress = player.getAdvancements().getOrStartProgress(advancement);
            if (!progress.isDone()) {
                for (String criterion : progress.getRemainingCriteria()) {
                    player.getAdvancements().award(advancement, criterion);
                }
            }
        }
    }
}
