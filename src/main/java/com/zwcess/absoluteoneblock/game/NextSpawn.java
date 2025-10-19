package com.zwcess.absoluteoneblock.game;

import net.minecraft.world.level.block.Block;
import javax.annotation.Nullable;

public class NextSpawn {
    private final Block block;
    private final ChestData chestData;

    private NextSpawn(@Nullable Block block, @Nullable ChestData chestData) {
        this.block = block;
        this.chestData = chestData;
    }

    public static NextSpawn of(Block block) {
        return new NextSpawn(block, null);
    }

    public static NextSpawn of(ChestData chestData) {
        return new NextSpawn(null, chestData);
    }

    public boolean isChest() {
        return this.chestData != null;
    }

    @Nullable
    public Block getBlock() {
        return block;
    }

    @Nullable
    public ChestData getChestData() {
        return chestData;
    }
}
