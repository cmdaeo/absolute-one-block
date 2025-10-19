package com.zwcess.absoluteoneblock.game;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class PlayerPhaseData {
    private int currentPhase;
    private int blocksBroken;
    private Set<String> spawnedOneTimeChests;

    public PlayerPhaseData() {
        this.currentPhase = 0;
        this.blocksBroken = 0;
        this.spawnedOneTimeChests = new HashSet<>();
    }

    public int getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(int phase) { this.currentPhase = phase; }
    
    public int getBlocksBroken() { return blocksBroken; }
    public void setBlocksBroken(int blocks) { this.blocksBroken = blocks; }
    public void incrementBlocks() { this.blocksBroken++; }

    public Set<String> getSpawnedOneTimeChests() {
        return spawnedOneTimeChests;
    }

    public void setSpawnedOneTimeChests(Set<String> chests) {
        this.spawnedOneTimeChests = chests;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Phase", currentPhase);
        tag.putInt("Blocks", blocksBroken);

        // Save spawned one-time chests as a string list
        ListTag listTag = new ListTag();
        for (String key : spawnedOneTimeChests) {
            listTag.add(StringTag.valueOf(key));
        }
        tag.put("SpawnedOneTimeChests", listTag);
        return tag;
    }

    public static PlayerPhaseData load(CompoundTag tag) {
        PlayerPhaseData data = new PlayerPhaseData();
        data.currentPhase = tag.getInt("Phase");
        data.blocksBroken = tag.getInt("Blocks");

        Set<String> chests = new HashSet<>();
        if (tag.contains("SpawnedOneTimeChests")) {
            ListTag listTag = tag.getList("SpawnedOneTimeChests", 8); // 8 = String
            chests = listTag.stream()
                    .map(nbt -> nbt.getAsString())
                    .collect(Collectors.toSet());
        }
        data.setSpawnedOneTimeChests(chests);
        return data;
    }
}
