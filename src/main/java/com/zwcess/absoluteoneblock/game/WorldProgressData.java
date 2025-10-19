package com.zwcess.absoluteoneblock.game;

import java.util.HashSet;
import java.util.Set;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zwcess.absoluteoneblock.config.Config;

public class WorldProgressData {
    public transient static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public int blocksBroken = 0;
    public Set<String> spawnedOneTimeChests = new HashSet<>();

    public boolean isGameInProgress = false; 
    public Config.GameMode lastKnownGameMode = Config.GameMode.COOP;
}
