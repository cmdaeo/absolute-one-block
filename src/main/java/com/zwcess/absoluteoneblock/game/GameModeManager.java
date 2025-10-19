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
    
    private static final Map<UUID, BlockPos> playerBlockPositions = new HashMap<>();

    public static BlockPos getPlayerBlockPosition(ServerPlayer player) {
        if (!Config.isCompetitiveMode()) {
            return COOP_BLOCK_POS;
        }

        UUID playerId = player.getUUID();
        if (!playerBlockPositions.containsKey(playerId)) {
            BlockPos newPos = generateRandomPosition();
            playerBlockPositions.put(playerId, newPos);
        }
        return playerBlockPositions.get(playerId);
    }

    public static void createPlayerIsland(ServerLevel level, BlockPos center) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos platformPos = center.below().offset(x, 0, z);
                level.setBlock(platformPos, Blocks.STONE.defaultBlockState(), 3);
            }
        }
        
        level.setBlock(center, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
    }

    private static BlockPos generateRandomPosition() {
        int spacing = Config.getCompetitiveSpacing();
        double angle = RANDOM.nextDouble() * 2 * Math.PI;
        
        int x = (int) (Math.cos(angle) * spacing);
        int z = (int) (Math.sin(angle) * spacing);
        
        return new BlockPos(x, 100, z);
    }

    public static boolean isPlayerOneBlock(ServerPlayer player, BlockPos brokenPos) {
        BlockPos playerBlock = getPlayerBlockPosition(player);
        return brokenPos.equals(playerBlock);
    }
}
