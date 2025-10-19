package com.zwcess.absoluteoneblock.game;

public class ChestData {
    public double weight;
    public String loot_table;
    public boolean once = false;

    /**
     * Default constructor used by GSON when loading from phases.json.
     */
    public ChestData() {
    }

    /**
     * Convenience constructor used by the PhaseManager for creating
     * dynamic chests in the Infinity phase.
     * @param weight The spawn weight (can be 0 for this use case).
     * @param loot_table The full ResourceLocation of the loot table.
     * @param once Whether this is a one-time spawn (always false for Infinity).
     */
    public ChestData(double weight, String loot_table, boolean once) {
        this.weight = weight;
        this.loot_table = loot_table;
        this.once = once;
    }
}
