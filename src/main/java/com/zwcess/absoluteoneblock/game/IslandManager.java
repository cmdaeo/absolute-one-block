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

    /**
     * Gets the player's saved island position or calculates a new one if it doesn't exist.
     * This is the main entry point for finding where a player's island should be.
     *
     * @param player The player whose island position is needed.
     * @return The BlockPos of the player's island.
     */
    public BlockPos getOrCreateIslandPosition(ServerPlayer player) {
        Map<String, List<Integer>> existingIslands = progressManager.getPlayerIslandPositions();
        String playerUUID = player.getUUID().toString();

        // If the player already has a saved island, return its position immediately.
        if (existingIslands.containsKey(playerUUID)) {
            List<Integer> coords = existingIslands.get(playerUUID);
            return new BlockPos(coords.get(0), coords.get(1), coords.get(2));
        }

        // --- If no island exists, calculate a new position ---
        
        // Read the distribution setting from persistent world data.
        boolean useEqualDistribution = Config.isEquallyDistributed();

        BlockPos newPos;
        if (useEqualDistribution) {
            newPos = getEquallyDistributedPosition(existingIslands);
        } else {
            // Fallback to the original sequential placement logic.
            newPos = getSequentialPosition(existingIslands);
        }

        // Save the newly calculated position for the player.
        existingIslands.put(playerUUID, List.of(newPos.getX(), newPos.getY(), newPos.getZ()));
        progressManager.markDirtyAndSave(); // Ensure the new position is saved to the world file.

        return newPos;
    }

    /**
     * Calculates the next position to be as far as possible from all other islands using a circular placement algorithm.
     */
    private BlockPos getEquallyDistributedPosition(Map<String, List<Integer>> existingIslands) {
        List<Double> existingAngles = new ArrayList<>();
        for (List<Integer> coords : existingIslands.values()) {
            // atan2(z, x) gives the angle in radians, which is what we need.
            double angle = Math.atan2(coords.get(2), coords.get(0));
            existingAngles.add(angle);
        }
        Collections.sort(existingAngles);

        double newAngle;
        if (existingAngles.isEmpty()) {
            newAngle = 0.0; // The first player is placed along the positive X-axis (East).
        } else {
            double largestGap = 0;
            double startOfLargestGap = 0;

            // Find the largest gap between consecutive players.
            for (int i = 0; i < existingAngles.size() - 1; i++) {
                double gap = existingAngles.get(i + 1) - existingAngles.get(i);
                if (gap > largestGap) {
                    largestGap = gap;
                    startOfLargestGap = existingAngles.get(i);
                }
            }

            // Check the gap between the last and first player to complete the circle.
            double wrapAroundGap = (2 * Math.PI + existingAngles.get(0)) - existingAngles.get(existingAngles.size() - 1);
            if (wrapAroundGap > largestGap) {
                largestGap = wrapAroundGap;
                startOfLargestGap = existingAngles.get(existingAngles.size() - 1);
            }
            
            // The new position is exactly in the middle of the largest empty space.
            newAngle = startOfLargestGap + (largestGap / 2.0);
        }

        int radius = Config.getCompetitiveSpacing(); // Read from persistent data
        int y = 100;
        int x = (int) Math.round(radius * Math.cos(newAngle));
        int z = (int) Math.round(radius * Math.sin(newAngle));
        return new BlockPos(x, y, z);
    }

    /**
     * Calculates the next island position sequentially along the X-axis.
     */
    private BlockPos getSequentialPosition(Map<String, List<Integer>> existingIslands) {
        int playerCount = existingIslands.size();
        int x = playerCount * Config.getCompetitiveSpacing();
        int y = 100;
        int z = 0; // All islands are placed in a straight line.
        return new BlockPos(x, y, z);
    }
}
