package com.zwcess.absoluteoneblock.event;

import com.zwcess.absoluteoneblock.AbsoluteOneBlock;
import com.zwcess.absoluteoneblock.config.Config;
import com.zwcess.absoluteoneblock.core.Registration;
import com.zwcess.absoluteoneblock.game.GameModeManager;
import com.zwcess.absoluteoneblock.game.NextSpawn;
import com.zwcess.absoluteoneblock.game.Phase;
import com.zwcess.absoluteoneblock.game.PhaseManager;
import com.zwcess.absoluteoneblock.game.PlayerPhaseData;
import com.zwcess.absoluteoneblock.game.ProgressManager;
import com.zwcess.absoluteoneblock.game.WorldProgressData;
import com.zwcess.absoluteoneblock.network.PacketHandler;
import com.zwcess.absoluteoneblock.network.SyncProgressS2CPacket;
import com.zwcess.absoluteoneblock.util.AdvancementHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;
import java.util.Random;

@Mod.EventBusSubscriber(modid = AbsoluteOneBlock.MOD_ID)
public class BlockBreakEventHandler {
    private static final Random random = new Random();

    @SuppressWarnings("null")
    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        // Early exit if client-side
        if (event.getLevel().isClientSide()) {
            return;
        }

        ServerLevel level = (ServerLevel) event.getLevel();
        ServerPlayer player = (ServerPlayer) event.getPlayer();
        BlockPos brokenPos = event.getPos();
        
        // Check if this is the player's One Block (works for both coop and competitive)
        if (!GameModeManager.isPlayerOneBlock(player, brokenPos)) {
            return; // Not their block, ignore it
        }

        event.setCanceled(true);

        BlockState brokenState = event.getState();
        PhaseManager phaseManager = AbsoluteOneBlock.phaseManager;

        NextSpawn nextSpawn = phaseManager.getNextSpawn(player);

        ProgressManager progressManager = phaseManager.getProgressManager();
        WorldProgressData worldData = progressManager.getData();

        if (!worldData.isGameInProgress && worldData.blocksBroken > 0) {
            worldData.isGameInProgress = true;
            progressManager.markDirtyAndSave();
        }

        // Handle drops for non-custom blocks
        if (!brokenState.is(Registration.ONE_BLOCK.get())) {
            if (player.hasCorrectToolForDrops(brokenState)) {
                List<ItemStack> drops = Block.getDrops(brokenState, level, event.getPos(), null, player, player.getMainHandItem());
                drops.forEach(drop -> {
                    if (!player.getInventory().add(drop)) {
                        Block.popResource(level, event.getPos(), drop);
                    }
                });
            }
            level.playSound(null, event.getPos(), brokenState.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0f, 1.0f);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, brokenState),
                event.getPos().getX() + 0.5, event.getPos().getY() + 0.5, event.getPos().getZ() + 0.5,
                12, 0.2, 0.2, 0.2, 0.15);
        }

        // Place the next block or chest
        BlockPos playerBlockPos = GameModeManager.getPlayerBlockPosition(player);
        if (nextSpawn.isChest()) {
            placeLootChest(level, playerBlockPos, nextSpawn);
        } else {
            level.setBlock(playerBlockPos, nextSpawn.getBlock().defaultBlockState(), 3);
        }

        // Damage the held item
        ItemStack heldItem = player.getMainHandItem();
        if (!player.isCreative() && heldItem.isDamageableItem()) {
            heldItem.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(player.getUsedItemHand()));
        }

        // Try to spawn a mob
        trySpawnMob(level, playerBlockPos.above(), phaseManager, player);
        
        // Sync progress data based on game mode
        syncProgressData(level, player, phaseManager);

        if (Config.hasSharedProgress()) {
            int totalBlocks = phaseManager.getProgressData().blocksBroken;
            checkBlockMilestones(player, totalBlocks);
        } else {
            PlayerPhaseData playerData = phaseManager.getPlayerData(player);
            int totalBlocks = playerData.getBlocksBroken();
            checkBlockMilestones(player, totalBlocks);
        }
    }

    private static void checkBlockMilestones(ServerPlayer player, int totalBlocks) {
        // Grant achievements at specific milestones
        switch (totalBlocks) {
            case 1:
                AdvancementHelper.grantAdvancement(player, "challenges/first_block");
                break;
            case 100:
                AdvancementHelper.grantAdvancement(player, "challenges/hundred_blocks");
                break;
            case 1000:
                AdvancementHelper.grantAdvancement(player, "challenges/thousand_blocks");
                break;
            case 10000:
                AdvancementHelper.grantAdvancement(player, "challenges/ten_thousand_blocks");
                break;
        }
    }
    
    private static void syncProgressData(ServerLevel level, ServerPlayer player, PhaseManager phaseManager) {
        if (Config.hasSharedProgress()) {
            // Shared progress: send global data to everyone in the dimension
            Phase currentPhase = phaseManager.getCurrentPhase();
            Phase nextPhase = phaseManager.getNextPhase();
            
            if (currentPhase != null) {
                int blocksBroken = phaseManager.getProgressData().blocksBroken;
                int nextThreshold = currentPhase.repeatable ? 0 : phaseManager.getNextPhaseBlocksNeeded();
                String currentName = currentPhase.name;
                String nextName = (nextPhase != null) ? nextPhase.name : "Infinity";
                
                SyncProgressS2CPacket packet = new SyncProgressS2CPacket(blocksBroken, nextThreshold, currentName, nextName);
                PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(level::dimension), packet);
            }
        } else {
            // Solo progress: send only to the player who broke the block
            PlayerPhaseData playerData = phaseManager.getPlayerData(player);
            Phase currentPhase = phaseManager.getPlayerCurrentPhase(player);
            Phase nextPhase = phaseManager.getPlayerNextPhase(player);
            
            int blocksBroken = playerData.getBlocksBroken();
            int nextThreshold = currentPhase.repeatable ? 0 : currentPhase.blocks_needed;
            String currentName = currentPhase.name;
            String nextName = (nextPhase != null) ? nextPhase.name : "Infinity";
            
            PacketHandler.sendToPlayer(
                new SyncProgressS2CPacket(blocksBroken, nextThreshold, currentName, nextName),
                player
            );
        }
    }
    
    private static void placeLootChest(ServerLevel level, BlockPos pos, NextSpawn spawn) {
        level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ChestBlockEntity chest) {
            @SuppressWarnings("null")
            ResourceLocation lootTableRL = ResourceLocation.tryParse(spawn.getChestData().loot_table);
            if (lootTableRL != null) {
                chest.setLootTable(lootTableRL, level.getRandom().nextLong());
            }
        }
    }

    private static void trySpawnMob(ServerLevel level, BlockPos spawnPos, PhaseManager phaseManager, ServerPlayer player) {
        Phase currentPhase = phaseManager.getPlayerCurrentPhase(player);
        if (currentPhase == null || currentPhase.mobs == null || currentPhase.mobs.isEmpty()) return;

        if (random.nextDouble() < currentPhase.mob_spawn_chance) {
            String mobKey = getWeightedRandomMob(currentPhase.mobs);
            if (mobKey == null) return;
            
            if (mobKey.startsWith("absoluteoneblock:")) {
                handleSpecialSpawn(mobKey, level, spawnPos);
                return;
            }

            ResourceLocation mobId = ResourceLocation.tryParse(mobKey);
            if (mobId == null) return;
            
            if (!ModList.get().isLoaded(mobId.getNamespace())) return;
            
            EntityType<?> mobType = ForgeRegistries.ENTITY_TYPES.getValue(mobId);
            if (mobType != null) {
                mobType.spawn(level, spawnPos, MobSpawnType.EVENT);
            }
        }
    }

    private static void handleSpecialSpawn(String key, ServerLevel level, BlockPos pos) {
        if (key.equals("absoluteoneblock:warden_spawn")) {
            Warden warden = EntityType.WARDEN.create(level);
            if (warden != null) {
                warden.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                level.addFreshEntity(warden);
            }
        }
    }

    private static String getWeightedRandomMob(Map<String, Object> mobProbabilities) {
        double totalWeight = mobProbabilities.values().stream().mapToDouble(value -> {
            if (value instanceof Double) return (Double) value;
            if (value instanceof Map) {
                Object weight = ((Map<?, ?>) value).get("weight");
                if (weight instanceof Number) return ((Number) weight).doubleValue();
            }
            return 0.0;
        }).sum();

        if (totalWeight <= 0) return null;
        double randomValue = random.nextDouble() * totalWeight;

        for (Map.Entry<String, Object> entry : mobProbabilities.entrySet()) {
            double currentWeight = 0;
            if (entry.getValue() instanceof Double) currentWeight = (Double) entry.getValue();
            else if (entry.getValue() instanceof Map) {
                Object weight = ((Map<?, ?>) entry.getValue()).get("weight");
                if (weight instanceof Number) currentWeight = ((Number) weight).doubleValue();
            }
            
            randomValue -= currentWeight;
            if (randomValue <= 0.0) return entry.getKey();
        }
        return null;
    }
}
