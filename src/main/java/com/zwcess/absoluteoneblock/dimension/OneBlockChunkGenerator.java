// main.java.com.zwcess.absoluteoneblock.dimension.OneBlockChunkGenerator.java
package com.zwcess.absoluteoneblock.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.zwcess.absoluteoneblock.core.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class OneBlockChunkGenerator extends NoiseBasedChunkGenerator {

    // --- THIS IS THE FINAL FIX ---
    // We hold our own reference to the settings to provide a stable getter for the CODEC.
    private final Holder<NoiseGeneratorSettings> settingsHolder;

    public static final Codec<OneBlockChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
            NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(g -> g.settingsHolder) // Use our own field
        ).apply(instance, OneBlockChunkGenerator::new));
    // --- END OF FIX ---

    public OneBlockChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings) {
        super(biomeSource, settings);
        this.settingsHolder = settings; // Store the settings locally
    }

    @Override
    @Nonnull
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    // By extending NoiseBasedChunkGenerator, we get all the complex biome noise for free.
    // We only need to override the methods that actually place blocks.

    @Override
    @Nonnull
    public CompletableFuture<ChunkAccess> fillFromNoise(@Nonnull Executor executor, @Nonnull Blender blender, @Nonnull RandomState randomState, @Nonnull StructureManager structureManager, @Nonnull ChunkAccess chunkAccess) {
        // We override this method to do nothing, preventing terrain from generating and creating a void.
        return CompletableFuture.completedFuture(chunkAccess);
    }

    @Override
    public void buildSurface(@Nonnull WorldGenRegion level, @Nonnull StructureManager structureManager, @Nonnull RandomState randomState, @Nonnull ChunkAccess chunk) {
        // Instead of building a surface, we use this hook to place our single starting block.
        if (chunk.getPos().x == 0 && chunk.getPos().z == 0) {
            BlockPos pos = new BlockPos(0, 100, 0);
            if (level.getBlockState(pos).isAir()) {
                level.setBlock(pos, Registration.ONE_BLOCK.get().defaultBlockState(), 2);
            }
            BlockPos pos_dirt = new BlockPos(0, 99, 0);
            if (level.getBlockState(pos_dirt).isAir()) {
                level.setBlock(pos_dirt, Blocks.DIRT.defaultBlockState(), 2);
            }
        }
    }
    
    // We must also override getBaseHeight to prevent issues with structure placement checks in a void world.
    @Override
    public int getBaseHeight(int x, int z, @Nonnull Heightmap.Types types, @Nonnull LevelHeightAccessor level, @Nonnull RandomState randomState) {
        return 0; // Return 0 for void world stability.
    }
}
