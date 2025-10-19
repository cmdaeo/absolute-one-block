// main.java.com.zwcess.absoluteoneblock.game.Phase.java
package com.zwcess.absoluteoneblock.game;

import java.util.Map;

public class Phase {
    public String name;
    public String description;
    // This is now a map of String to either Double or ChestData
    public Map<String, Object> blocks;
    // Mobs can also have special objects, so we use Object here too
    public Map<String, Object> mobs;
    public double mob_spawn_chance;
    public int blocks_needed;
    public boolean repeatable;
    // Add the new rewards object
    public EndOfPhaseRewards end_of_phase_rewards;
}
