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
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)) {
            return;
        }

        if (player.getHealth() - event.getAmount() > 0) {
            return;
        }

        ItemStack heartItem = findHeartOfTheVoid(player);
        
        if (!heartItem.isEmpty()) {
            event.setCanceled(true);
            
            heartItem.shrink(1);
            
            teleportToSafety(player);
            
            applyVoidSaveEffects(player);
            
            playVoidSaveEffects(player);

            AdvancementHelper.grantAdvancement(player, "challenges/survived_void");
        }
    }

    private static ItemStack findHeartOfTheVoid(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.is(Registration.HEART_OF_THE_VOID.get())) {
            return mainHand;
        }
        
        ItemStack offHand = player.getOffhandItem();
        if (offHand.is(Registration.HEART_OF_THE_VOID.get())) {
            return offHand;
        }
        
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(Registration.HEART_OF_THE_VOID.get())) {
                return stack;
            }
        }
        
        return ItemStack.EMPTY;
    }

    private static void teleportToSafety(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        
        BlockPos safePos = GameModeManager.getPlayerBlockPosition(player);
        
        player.teleportTo(level, 
            safePos.getX() + 0.5, 
            safePos.getY() + 1.0, 
            safePos.getZ() + 0.5, 
            player.getYRot(), 
            player.getXRot());
    }

    private static void applyVoidSaveEffects(ServerPlayer player) {
        player.setHealth(1.0F);
        
        player.removeAllEffects();
        
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1)); 
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));   
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0)); 
        
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0)); 
    }

    private static void playVoidSaveEffects(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        RandomSource random = player.getRandom();
        
        level.playSound(null, player.blockPosition(), 
            SoundEvents.TOTEM_USE, 
            SoundSource.PLAYERS, 
            1.0F, 1.0F);
        
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
            player.getX(), player.getY() + player.getEyeHeight() / 2.0D, player.getZ(), 
            35,                     
            0.5D, 0.5D, 0.5D,       
            0.05D);                 

        String messageKey = random.nextBoolean() ?
                "item.absoluteoneblock.heart_of_the_void.used1" :
                "item.absoluteoneblock.heart_of_the_void.used2";
        
        Component titleText = Component.translatable(messageKey);

        player.connection.send(new ClientboundSetTitleTextPacket(titleText));
    }
}
