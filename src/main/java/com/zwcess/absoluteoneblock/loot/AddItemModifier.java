package com.zwcess.absoluteoneblock.loot;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class AddItemModifier extends LootModifier {
    public static final Supplier<Codec<AddItemModifier>> CODEC = Suppliers.memoize(() ->
        RecordCodecBuilder.create(inst -> codecStart(inst)
            .and(ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(m -> m.item))
            .and(Codec.DOUBLE.fieldOf("chance").forGetter(m -> m.chance))
            .and(Codec.BOOL.optionalFieldOf("chests_only", true).forGetter(m -> m.chestsOnly))
            .apply(inst, AddItemModifier::new))
    );

    private final Item item;
    private final double chance;
    private final boolean chestsOnly;

    public AddItemModifier(LootItemCondition[] conditionsIn, Item item, double chance, boolean chestsOnly) {
        super(conditionsIn);
        this.item = item;
        this.chance = chance;
        this.chestsOnly = chestsOnly;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        // Check if we should only affect chests
        if (chestsOnly) {
            ResourceLocation lootTable = context.getQueriedLootTableId();
            if (lootTable == null || !lootTable.getPath().contains("chests/")) {
                return generatedLoot; // Not a chest, skip
            }
        }

        // Check random chance
        if (context.getRandom().nextDouble() > chance) {
            return generatedLoot; // Failed chance roll
        }

        // Check other conditions
        for (LootItemCondition condition : this.conditions) {
            if (!condition.test(context)) {
                return generatedLoot;
            }
        }

        // Add the item
        generatedLoot.add(new ItemStack(this.item));
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
