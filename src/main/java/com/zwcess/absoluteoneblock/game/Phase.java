package com.zwcess.absoluteoneblock.game;

import java.util.Map;

public class Phase {
    public String name;
    public String description;
    
    public Map<String, Object> blocks;
    
    public Map<String, Object> mobs;
    public double mob_spawn_chance;
    public int blocks_needed;
    public boolean repeatable;
    
    public EndOfPhaseRewards end_of_phase_rewards;
}
