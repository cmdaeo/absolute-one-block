package com.zwcess.absoluteoneblock.event;

import com.zwcess.absoluteoneblock.AbsoluteOneBlock;
import com.zwcess.absoluteoneblock.config.Config;
import com.zwcess.absoluteoneblock.game.GameModeManager;
import com.zwcess.absoluteoneblock.game.Phase;
import com.zwcess.absoluteoneblock.game.PhaseManager;
import com.zwcess.absoluteoneblock.network.PacketHandler;
import com.zwcess.absoluteoneblock.network.SyncProgressS2CPacket;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AbsoluteOneBlock.MOD_ID)
public class PlayerEventHandler {

    private static final ResourceKey<Level> ONEBLOCK_DIM = ResourceKey.create(
        Registries.DIMENSION,
        ResourceLocation.fromNamespaceAndPath(AbsoluteOneBlock.MOD_ID, "oneblock_dimension")
    );

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            
            // If player is joining and is in the One Block dimension
            if (level.dimension().equals(ONEBLOCK_DIM)) {
                // Get or create their spawn position
                BlockPos spawnPos = GameModeManager.getPlayerBlockPosition(player);
                
                // In competitive mode, create their island if it doesn't exist
                if (Config.isCompetitiveMode()) {
                    if (level.getBlockState(spawnPos).isAir()) {
                        GameModeManager.createPlayerIsland(level, spawnPos);
                    }
                }
                
                // Teleport player to their spawn position (slightly above the block)
                player.teleportTo(level, 
                    spawnPos.getX() + 0.5, 
                    spawnPos.getY() + 1.5,  // Changed from +1 to +1.5 so they don't suffocate
                    spawnPos.getZ() + 0.5, 
                    0f, 0f);
            }
            
            // Sync progress data
            syncInitialData(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            
            // If player just entered the One Block dimension
            if (level.dimension().equals(ONEBLOCK_DIM)) {
                // Get or create their spawn position
                BlockPos spawnPos = GameModeManager.getPlayerBlockPosition(player);
                
                // In competitive mode, create their island if it doesn't exist
                if (Config.isCompetitiveMode()) {
                    if (level.getBlockState(spawnPos).isAir()) {
                        GameModeManager.createPlayerIsland(level, spawnPos);
                    }
                }
                
                // Teleport player to their spawn position
                player.teleportTo(level, 
                    spawnPos.getX() + 0.5, 
                    spawnPos.getY() + 1.5, 
                    spawnPos.getZ() + 0.5, 
                    0f, 0f);
            }
            
            // Sync progress data
            syncInitialData(player);
        }
    }

    private static void syncInitialData(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (player.level().dimension().equals(ONEBLOCK_DIM)) {
                PhaseManager phaseManager = AbsoluteOneBlock.phaseManager;
                Phase currentPhase = phaseManager.getCurrentPhase();
                Phase nextPhase = phaseManager.getNextPhase();

                if (currentPhase != null) {
                    int blocksBroken = phaseManager.getProgressData().blocksBroken;
                    int nextPhaseTarget = phaseManager.getNextPhaseBlocksNeeded(); 
                    String currentName = currentPhase.name;
                    String nextName = (nextPhase != null) ? nextPhase.name : "Infinity";
                    
                    PacketHandler.sendToPlayer(
                        new SyncProgressS2CPacket(blocksBroken, nextPhaseTarget, currentName, nextName),
                        serverPlayer
                    );
                }
            }
        }
    }
}
