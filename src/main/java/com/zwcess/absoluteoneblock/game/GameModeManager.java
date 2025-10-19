package com.zwcess.absoluteoneblock.game;

import com.zwcess.absoluteoneblock.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class GameModeManager {
    private static final Random RANDOM = new Random();
    private static final BlockPos COOP_BLOCK_POS = new BlockPos(0, 100, 0);
    
    // Store each player's block position in competitive mode
    private static final Map<UUID, BlockPos> playerBlockPositions = new HashMap<>();
    
    /**
     * Get the One Block position for a player.
     * In coop: everyone gets (0, 100, 0)
     * In competitive: each player gets their own unique position
     */
    public static BlockPos getPlayerBlockPosition(ServerPlayer player) {
        if (!Config.isCompetitiveMode()) {
            return COOP_BLOCK_POS;
        }
        
        // Competitive mode: assign or retrieve player's unique position
        UUID playerId = player.getUUID();
        if (!playerBlockPositions.containsKey(playerId)) {
            BlockPos newPos = generateRandomPosition();
            playerBlockPositions.put(playerId, newPos);
        }
        return playerBlockPositions.get(playerId);
    }
    
    /**
     * Create the starting island for a player in competitive mode
     */
    public static void createPlayerIsland(ServerLevel level, BlockPos center) {
        // Create 3x3 stone platform
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos platformPos = center.below().offset(x, 0, z);
                level.setBlock(platformPos, Blocks.STONE.defaultBlockState(), 3);
            }
        }
        
        // Place the One Block
        level.setBlock(center, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
    }
    
    /**
     * Generate a random position around (0, 100, 0) for competitive mode
     */
    private static BlockPos generateRandomPosition() {
        int spacing = Config.getCompetitiveSpacing();
        double angle = RANDOM.nextDouble() * 2 * Math.PI;
        
        int x = (int) (Math.cos(angle) * spacing);
        int z = (int) (Math.sin(angle) * spacing);
        
        return new BlockPos(x, 100, z);
    }
    
    /**
     * Check if a broken block is the correct One Block for this player
     */
    public static boolean isPlayerOneBlock(ServerPlayer player, BlockPos brokenPos) {
        BlockPos playerBlock = getPlayerBlockPosition(player);
        return brokenPos.equals(playerBlock);
    }
}
