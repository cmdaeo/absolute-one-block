// main.java.com.zwcess.absoluteoneblock.dimension.AbsoluteDimensions.java
package com.zwcess.absoluteoneblock.dimension;

import com.zwcess.absoluteoneblock.AbsoluteOneBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public class AbsoluteDimensions {
    public static final ResourceKey<Level> ONEBLOCK_DIMENSION_KEY = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(AbsoluteOneBlock.MOD_ID, "oneblock_dimension")); // Corrected

    public static final ResourceKey<DimensionType> ONEBLOCK_DIMENSION_TYPE_KEY = ResourceKey.create(Registries.DIMENSION_TYPE,
            ResourceLocation.fromNamespaceAndPath(AbsoluteOneBlock.MOD_ID, "oneblock_dimension_type")); // Corrected
}
