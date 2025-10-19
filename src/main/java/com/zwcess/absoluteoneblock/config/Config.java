package com.zwcess.absoluteoneblock.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.EnumValue<GameMode> GAME_MODE;
    public static final ForgeConfigSpec.BooleanValue EQUALLY_DISTRIBUTED;
    public static final ForgeConfigSpec.IntValue COMPETITIVE_SPACING;

    public enum GameMode {
        COOP,
        COMPETITIVE_SHARED,
        COMPETITIVE_SOLO
    }

    static {
        BUILDER.push("Absolute One Block Settings");

        GAME_MODE = BUILDER
                .comment("The game mode for the world. COOP, COMPETITIVE_SHARED, or COMPETITIVE_SOLO.")
                .defineEnum("gameMode", GameMode.COOP);

        EQUALLY_DISTRIBUTED = BUILDER
                .comment("If true, competitive islands will be spaced out to fill a circle. If false, they will be placed in a straight line.")
                .define("equallyDistributed", true);
        
        COMPETITIVE_SPACING = BUILDER
                .comment("The distance between islands in competitive mode.")
                .defineInRange("competitiveSpacing", 500, 50, 5000);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static GameMode getGameMode() {
        return GAME_MODE.get();
    }

    public static boolean isCoopMode() {
        return getGameMode() == GameMode.COOP;
    }
    
    public static boolean isCompetitiveMode() {
        GameMode mode = getGameMode();
        return mode == GameMode.COMPETITIVE_SHARED || mode == GameMode.COMPETITIVE_SOLO;
    }
    
    public static boolean hasSharedProgress() {
        GameMode mode = getGameMode();
        return mode == GameMode.COOP || mode == GameMode.COMPETITIVE_SHARED;
    }
    
    public static boolean hasOwnIslands() {
        GameMode mode = getGameMode();
        return mode == GameMode.COMPETITIVE_SHARED || mode == GameMode.COMPETITIVE_SOLO;
    }

    public static boolean isEquallyDistributed() {
        return EQUALLY_DISTRIBUTED.get();
    }

    public static int getCompetitiveSpacing() {
        return COMPETITIVE_SPACING.get();
    }
}
