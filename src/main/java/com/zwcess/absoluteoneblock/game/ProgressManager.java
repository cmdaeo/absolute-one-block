package com.zwcess.absoluteoneblock.game;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.zwcess.absoluteoneblock.config.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProgressManager extends SavedData {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String DATA_NAME = "absoluteoneblock_progress";
    private WorldProgressData progressData = new WorldProgressData();
    private Map<UUID, PlayerPhaseData> playerProgressData = new HashMap<>();
    private Map<String, List<Integer>> playerIslandPositions = new HashMap<>();

    // Use a single GSON instance
    private static final Gson GSON = new GsonBuilder().create();

    public static ProgressManager load(CompoundTag nbt) {
        ProgressManager manager = new ProgressManager();
        if (nbt.contains("oneblock_data_json")) {
            String json = nbt.getString("oneblock_data_json");
            manager.progressData = GSON.fromJson(json, WorldProgressData.class);
        }

        // Conditionally load player data only if it exists
        if (nbt.contains("player_progress_json")) {
            String playerJson = nbt.getString("player_progress_json");
            Type type = new TypeToken<Map<UUID, PlayerPhaseData>>(){}.getType();
            manager.playerProgressData = GSON.fromJson(playerJson, type);
        }
        
        // Ensure maps are never null after loading
        if (manager.playerProgressData == null) {
            manager.playerProgressData = new HashMap<>();
        }
        if (manager.progressData.spawnedOneTimeChests == null) {
            manager.progressData.spawnedOneTimeChests = new HashSet<>();
        }

        if (nbt.contains("island_positions_json")) {
            Type type = new TypeToken<Map<String, List<Integer>>>(){}.getType();
            manager.playerIslandPositions = GSON.fromJson(nbt.getString("island_positions_json"), type);
        }
        if (manager.playerIslandPositions == null) {
            manager.playerIslandPositions = new HashMap<>();
        }

        return manager;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        // Save global progress
        nbt.putString("oneblock_data_json", GSON.toJson(this.progressData));

        // Always save island positions, as they are needed for competitive mode
        nbt.putString("island_positions_json", GSON.toJson(this.playerIslandPositions));

        // Only save the per-player data map if in a competitive mode
        if (Config.isCompetitiveMode()) {
            String playerJson = GSON.toJson(this.playerProgressData);
            nbt.putString("player_progress_json", playerJson);
            LOGGER.debug("Saved per-player progress for competitive mode.");
        } else {
            // If not in competitive mode, remove old player data to prevent conflicts
            if (nbt.contains("player_progress_json")) {
                nbt.remove("player_progress_json");
                LOGGER.debug("Removed old per-player progress data after switching to coop mode.");
            }
        }

        LOGGER.debug("Saved One Block world progress.");
        return nbt;
    }

    public Map<String, List<Integer>> getPlayerIslandPositions() {
        return this.playerIslandPositions;
    }

    public WorldProgressData getData() {
        return this.progressData;
    }

    // Add a getter for PhaseManager to access the player data map
    public Map<UUID, PlayerPhaseData> getPlayerProgressData() {
        return this.playerProgressData;
    }

    public void markDirtyAndSave() {
        setDirty();
    }
}
