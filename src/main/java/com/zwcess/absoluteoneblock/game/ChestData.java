package com.zwcess.absoluteoneblock.game;

public class ChestData {
    public double weight;
    public String loot_table;
    public boolean once = false;

    public ChestData() {
    }

    public ChestData(double weight, String loot_table, boolean once) {
        this.weight = weight;
        this.loot_table = loot_table;
        this.once = once;
    }
}
