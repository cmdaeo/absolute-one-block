package com.zwcess.absoluteoneblock.game;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.zwcess.absoluteoneblock.config.Config;

public class IslandManager {
    private final ProgressManager progressManager;

    public IslandManager(ProgressManager progressManager) {
        this.progressManager = progressManager;
    }

    public BlockPos getOrCreateIslandPosition(ServerPlayer player) {
        Map<String, List<Integer>> existingIslands = progressManager.getPlayerIslandPositions();
        String playerUUID = player.getUUID().toString();

        if (existingIslands.containsKey(playerUUID)) {
            List<Integer> coords = existingIslands.get(playerUUID);
            return new BlockPos(coords.get(0), coords.get(1), coords.get(2));
        }

        boolean useEqualDistribution = Config.isEquallyDistributed();

        BlockPos newPos;
        if (useEqualDistribution) {
            newPos = getEquallyDistributedPosition(existingIslands);
        } else {
            newPos = getSequentialPosition(existingIslands);
        }

        existingIslands.put(playerUUID, List.of(newPos.getX(), newPos.getY(), newPos.getZ()));
        progressManager.markDirtyAndSave(); 

        return newPos;
    }

    private BlockPos getEquallyDistributedPosition(Map<String, List<Integer>> existingIslands) {
        List<Double> existingAngles = new ArrayList<>();
        for (List<Integer> coords : existingIslands.values()) {
            double angle = Math.atan2(coords.get(2), coords.get(0));
            existingAngles.add(angle);
        }
        Collections.sort(existingAngles);

        double newAngle;
        if (existingAngles.isEmpty()) {
            newAngle = 0.0; 
        } else {
            double largestGap = 0;
            double startOfLargestGap = 0;

            for (int i = 0; i < existingAngles.size() - 1; i++) {
                double gap = existingAngles.get(i + 1) - existingAngles.get(i);
                if (gap > largestGap) {
                    largestGap = gap;
                    startOfLargestGap = existingAngles.get(i);
                }
            }

            double wrapAroundGap = (2 * Math.PI + existingAngles.get(0)) - existingAngles.get(existingAngles.size() - 1);
            if (wrapAroundGap > largestGap) {
                largestGap = wrapAroundGap;
                startOfLargestGap = existingAngles.get(existingAngles.size() - 1);
            }
            
            newAngle = startOfLargestGap + (largestGap / 2.0);
        }

        int radius = Config.getCompetitiveSpacing(); 
        int y = 100;
        int x = (int) Math.round(radius * Math.cos(newAngle));
        int z = (int) Math.round(radius * Math.sin(newAngle));
        return new BlockPos(x, y, z);
    }

    private BlockPos getSequentialPosition(Map<String, List<Integer>> existingIslands) {
        int playerCount = existingIslands.size();
        int x = playerCount * Config.getCompetitiveSpacing();
        int y = 100;
        int z = 0; 
        return new BlockPos(x, y, z);
    }
}
