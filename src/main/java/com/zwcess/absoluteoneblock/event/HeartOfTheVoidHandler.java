package com.zwcess.absoluteoneblock.event;

import com.zwcess.absoluteoneblock.AbsoluteOneBlock;
import com.zwcess.absoluteoneblock.core.Registration;
import com.zwcess.absoluteoneblock.game.GameModeManager;
import com.zwcess.absoluteoneblock.util.AdvancementHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AbsoluteOneBlock.MOD_ID)
public class HeartOfTheVoidHandler {

    @SubscribeEvent
    public static void onPlayerDamage(LivingDamageEvent event) {
        // Only handle server-side player damage
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Check if the damage is from the void
        if (!event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)) {
            return;
        }

        // Check if damage would be fatal
        if (player.getHealth() - event.getAmount() > 0) {
            return;
        }

        // Try to find Heart of the Void in hands first (like totem), then inventory
        ItemStack heartItem = findHeartOfTheVoid(player);
        
        if (!heartItem.isEmpty()) {
            // Cancel the damage
            event.setCanceled(true);
            
            // Consume the item
            heartItem.shrink(1);
            
            // Teleport player to safety
            teleportToSafety(player);
            
            // Apply effects
            applyVoidSaveEffects(player);
            
            // Play effects
            playVoidSaveEffects(player);

            AdvancementHelper.grantAdvancement(player, "challenges/survived_void");
        }
    }

    private static ItemStack findHeartOfTheVoid(Player player) {
        // Check main hand
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.is(Registration.HEART_OF_THE_VOID.get())) {
            return mainHand;
        }
        
        // Check off hand
        ItemStack offHand = player.getOffhandItem();
        if (offHand.is(Registration.HEART_OF_THE_VOID.get())) {
            return offHand;
        }
        
        // Check inventory
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(Registration.HEART_OF_THE_VOID.get())) {
                return stack;
            }
        }
        
        return ItemStack.EMPTY;
    }

    private static void teleportToSafety(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        
        // Get their One Block position (works for both coop and competitive)
        BlockPos safePos = GameModeManager.getPlayerBlockPosition(player);
        
        // Teleport to their One Block + 1 block up (standing on the block)
        player.teleportTo(level, 
            safePos.getX() + 0.5, 
            safePos.getY() + 1.0, 
            safePos.getZ() + 0.5, 
            player.getYRot(), 
            player.getXRot());
    }

    private static void applyVoidSaveEffects(ServerPlayer player) {
        // Heal to 1 heart (like totem)
        player.setHealth(1.0F);
        
        // Clear negative effects
        player.removeAllEffects();
        
        // Apply protective buffs (same as totem)
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1)); // 45 seconds Regen II
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));   // 5 seconds Absorption II
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0)); // 40 seconds Fire Resistance
        
        // Optional: Add Slow Falling to prevent immediate death from fall damage
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0)); // 10 seconds
    }

    private static void playVoidSaveEffects(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        RandomSource random = player.getRandom();
        
        // Play totem sound
        level.playSound(null, player.blockPosition(), 
            SoundEvents.TOTEM_USE, 
            SoundSource.PLAYERS, 
            1.0F, 1.0F);
        
        // Spawn totem particles on the server
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
            player.getX(), player.getY() + player.getEyeHeight() / 2.0D, player.getZ(), // Position
            35,                     // Count
            0.5D, 0.5D, 0.5D,       // Spread
            0.05D);                 // Speed

        // Randomly choose one of the two messages
        String messageKey = random.nextBoolean() ?
                "item.absoluteoneblock.heart_of_the_void.used1" :
                "item.absoluteoneblock.heart_of_the_void.used2";
        
        Component titleText = Component.translatable(messageKey);

        // Send the chosen message as a title to the player
        player.connection.send(new ClientboundSetTitleTextPacket(titleText));
    }
}
