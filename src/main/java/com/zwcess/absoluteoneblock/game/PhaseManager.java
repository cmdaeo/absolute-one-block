package com.zwcess.absoluteoneblock.game;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.zwcess.absoluteoneblock.AbsoluteOneBlock;
import com.zwcess.absoluteoneblock.config.Config;
import com.zwcess.absoluteoneblock.util.AdvancementHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStreamReader;
import java.util.*;

public class PhaseManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ResourceLocation PHASES_LOCATION = new ResourceLocation(AbsoluteOneBlock.MOD_ID, "phases.json");
    private static final Gson GSON = createGson();
    private static final int TRANSITION_WINDOW = 50;

    public static PhaseManager INSTANCE;
    private List<Block> randomOreCache;
    private List<Block> randomLogCache;

    private PhaseConfig config;
    private final Random random = new Random();
    private int lastKnownPhaseIndex = 0;

    private List<Block> infinityBlockCache;
    private List<ResourceLocation> infinityChestCache;
    private List<EntityType<?>> infinityMobCache;

    private ProgressManager progressManager;

    private final Map<UUID, Integer> playerLastKnownPhase = new HashMap<>();

    public PhaseManager() {
        INSTANCE = this;
    }

    private static Gson createGson() {
        return new GsonBuilder()
            .registerTypeAdapter(Phase.class, (JsonDeserializer<Phase>) (json, typeOfT, context) -> {
                JsonObject jsonObject = json.getAsJsonObject();
                Phase phase = new Phase();
                phase.name = jsonObject.has("name") ? jsonObject.get("name").getAsString() : "Unnamed Phase";
                phase.description = jsonObject.has("description") ? jsonObject.get("description").getAsString() : "";
                phase.mob_spawn_chance = jsonObject.has("mob_spawn_chance") ? jsonObject.get("mob_spawn_chance").getAsDouble() : 0.0;
                phase.blocks_needed = jsonObject.has("blocks_needed") ? jsonObject.get("blocks_needed").getAsInt() : 0;
                phase.repeatable = jsonObject.has("repeatable") && jsonObject.get("repeatable").getAsBoolean();
                phase.blocks = new HashMap<>();
                if (jsonObject.has("blocks")) {
                    jsonObject.getAsJsonObject("blocks").entrySet().forEach(entry -> {
                        if (entry.getValue().isJsonPrimitive()) {
                            phase.blocks.put(entry.getKey(), entry.getValue().getAsDouble());
                        } else {
                            phase.blocks.put(entry.getKey(), context.deserialize(entry.getValue(), ChestData.class));
                        }
                    });
                }
                phase.mobs = new HashMap<>();
                if (jsonObject.has("mobs")) {
                    jsonObject.getAsJsonObject("mobs").entrySet().forEach(entry -> {
                        if (entry.getValue().isJsonPrimitive()) {
                            phase.mobs.put(entry.getKey(), entry.getValue().getAsDouble());
                        } else {
                            phase.mobs.put(entry.getKey(), context.deserialize(entry.getValue(), Object.class));
                        }
                    });
                }
                if (jsonObject.has("end_of_phase_rewards")) {
                    phase.end_of_phase_rewards = context.deserialize(jsonObject.get("end_of_phase_rewards"), EndOfPhaseRewards.class);
                }
                return phase;
            }).create();
    }

    public void resetPhase() {
        if (progressManager != null) {
            progressManager.getData().blocksBroken = 0;
            progressManager.getData().spawnedOneTimeChests.clear();
            progressManager.markDirtyAndSave();
            this.lastKnownPhaseIndex = 0;
            LOGGER.info("Absolute One Block progression has been reset.");
        }
    }

    public boolean setPhase(int phaseIndex) {
        if (progressManager != null && config != null && phaseIndex >= 0 && phaseIndex < config.phases.size()) {
            Phase targetPhase = config.phases.get(phaseIndex);
            progressManager.getData().blocksBroken = targetPhase.blocks_needed;
            progressManager.markDirtyAndSave();
            this.lastKnownPhaseIndex = phaseIndex;
            LOGGER.info("Absolute One Block phase set to index: {}", phaseIndex);
            return true;
        }
        return false;
    }

    public void setBlocksNeededForNextPhase(int blocksNeeded) {
        Phase currentPhase = getCurrentPhase();
        if (currentPhase != null) {
            Phase nextPhase = getNextPhase();
            if (nextPhase != null) {
                 nextPhase.blocks_needed = progressManager.getData().blocksBroken + blocksNeeded;
                 LOGGER.info("Blocks needed for next phase transition has been temporarily set to a total of {}", nextPhase.blocks_needed);
            }
        }
    }

    public void load(MinecraftServer server) {
        ResourceManager resourceManager = server.getResourceManager();
        try {
            Resource resource = resourceManager.getResource(PHASES_LOCATION).orElseThrow();
            this.config = GSON.fromJson(new InputStreamReader(resource.open()), PhaseConfig.class);
            LOGGER.info("Successfully loaded {} phases for Absolute One Block.", config.phases.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load One Block phases from JSON. This is a critical error!", e);
        }

        augmentPhasesWithDynamicBlocks();

        ServerLevel overworld = server.overworld();
        this.progressManager = overworld.getDataStorage().computeIfAbsent(ProgressManager::load, ProgressManager::new, ProgressManager.DATA_NAME);

        WorldProgressData worldData = progressManager.getData();
        Config.GameMode currentConfigMode = Config.getGameMode();
        Config.GameMode lastSavedMode = worldData.lastKnownGameMode;

        boolean wasCompetitive = (lastSavedMode == Config.GameMode.COMPETITIVE_SHARED || lastSavedMode == Config.GameMode.COMPETITIVE_SOLO);
        boolean isNowCompetitive = (currentConfigMode == Config.GameMode.COMPETITIVE_SHARED || currentConfigMode == Config.GameMode.COMPETITIVE_SOLO);

        if (wasCompetitive != isNowCompetitive) {
            LOGGER.info("Game mode type has changed from {} to {}. Wiping world progression.", lastSavedMode, currentConfigMode);
            worldData.blocksBroken = 0;
            worldData.spawnedOneTimeChests.clear();
            progressManager.getPlayerIslandPositions().clear();
            progressManager.getPlayerProgressData().clear();
        }

        worldData.lastKnownGameMode = currentConfigMode;
        progressManager.markDirtyAndSave();

        buildBlockCaches();
        buildInfinityCaches(server);
    }

    private void buildBlockCaches() {
        randomOreCache = new ArrayList<>();
        randomLogCache = new ArrayList<>();

        for (Block block : ForgeRegistries.BLOCKS) {
            ResourceLocation registryName = ForgeRegistries.BLOCKS.getKey(block);
            if (registryName == null) continue;

            String path = registryName.getPath();
            
            if ((path.endsWith("_ore") || path.contains("_ore_")) && !isBlockInPhase(block, "Mining")) {
                randomOreCache.add(block);
            }
            
            if ((path.endsWith("_log") || path.contains("_log_")) && !path.contains("stripped") && !isBlockInPhase(block, "Exploration")) {
                randomLogCache.add(block);
            }
        }
        
        LOGGER.info("Built random block caches: {} ores, {} logs.", randomOreCache.size(), randomLogCache.size());
    }

    private boolean isBlockInPhase(Block block, String phaseName) {
        if (config == null) return false;
        return config.phases.stream()
            .filter(p -> p.name.equalsIgnoreCase(phaseName))
            .findFirst()
            .map(p -> p.blocks.containsKey(ForgeRegistries.BLOCKS.getKey(block).toString()))
            .orElse(false);
    }

    private void augmentPhasesWithDynamicBlocks() {
        if (config == null || config.phases == null) return;

        LOGGER.info("Augmenting phases with dynamic blocks (ores and logs)...");
        int oresAdded = 0;
        int logsAdded = 0;

        for (Phase phase : config.phases) {
            if (phase.name.equalsIgnoreCase("Mining")) {
                for (Block block : ForgeRegistries.BLOCKS) {
                    ResourceLocation registryName = ForgeRegistries.BLOCKS.getKey(block);
                    if (registryName != null) {
                        String path = registryName.getPath();
                        if (path.endsWith("_ore") || path.contains("_ore_")) {
                            if (phase.blocks.putIfAbsent(registryName.toString(), 1.0) == null) {
                                oresAdded++;
                            }
                        }
                    }
                }
            }
            else if (phase.name.equalsIgnoreCase("Exploration")) {
                for (Block block : ForgeRegistries.BLOCKS) {
                    ResourceLocation registryName = ForgeRegistries.BLOCKS.getKey(block);
                    if (registryName != null) {
                        String path = registryName.getPath();
                        if ((path.endsWith("_log") || path.contains("_log_")) && !path.contains("stripped")) {
                            if (phase.blocks.putIfAbsent(registryName.toString(), 8.0) == null) {
                                logsAdded++;
                            }
                        }
                    }
                }
            }
        }
        LOGGER.info("Dynamically added {} new ores and {} new logs to relevant phases.", oresAdded, logsAdded);
    }

    private void buildInfinityCaches(MinecraftServer server) {
        infinityBlockCache = new ArrayList<>();
        for (Block block : ForgeRegistries.BLOCKS) {
            if (block != null && block != Blocks.AIR && block.defaultBlockState().getRenderShape() != RenderShape.INVISIBLE) {
                infinityBlockCache.add(block);
            }
        }

        infinityChestCache = new ArrayList<>();
        LootDataManager lootDataManager = server.getLootData();
        for (ResourceLocation id : lootDataManager.getKeys(LootDataType.TABLE)) {
            if (id.getPath().contains("chests/")) {
                infinityChestCache.add(id);
            }
        }

        infinityMobCache = new ArrayList<>();
        for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES) {
            if (type.getCategory() != MobCategory.MISC && type != EntityType.ENDER_DRAGON) {
                infinityMobCache.add(type);
            }
        }
        
        LOGGER.info("Infinity Caches Built: {} blocks, {} chest loot tables, {} mobs.",
            infinityBlockCache.size(), infinityChestCache.size(), infinityMobCache.size());
    }

    public PlayerPhaseData getPlayerData(ServerPlayer player) {
        if (Config.isCoopMode() || Config.hasSharedProgress()) {
            PlayerPhaseData sharedView = new PlayerPhaseData();
            Phase currentPhase = getCurrentPhase(); 
            int currentIndex = (currentPhase != null) ? config.phases.indexOf(currentPhase) : 0;
            sharedView.setCurrentPhase(currentIndex);
            sharedView.setBlocksBroken(progressManager.getData().blocksBroken);
            if (Config.getGameMode() == Config.GameMode.COMPETITIVE_SHARED) {
                sharedView.setSpawnedOneTimeChests(progressManager.getPlayerProgressData().computeIfAbsent(player.getUUID(), k -> new PlayerPhaseData()).getSpawnedOneTimeChests());
            } else { 
                sharedView.setSpawnedOneTimeChests(progressManager.getData().spawnedOneTimeChests);
            }
            return sharedView;
        } else {
            return progressManager.getPlayerProgressData().computeIfAbsent(player.getUUID(), k -> new PlayerPhaseData());
        }
    }

    public Phase getPlayerCurrentPhase(ServerPlayer player) {
        if (Config.hasSharedProgress()) {
            return getCurrentPhase();
        } else {
            PlayerPhaseData data = getPlayerData(player);
            int phaseIndex = Math.min(data.getCurrentPhase(), config.phases.size() - 1);
            return config.phases.get(phaseIndex);
        }
    }

    public Phase getPlayerNextPhase(ServerPlayer player) {
        if (Config.hasSharedProgress()) {
            return getNextPhase();
        } else {
            PlayerPhaseData data = getPlayerData(player);
            int nextIndex = data.getCurrentPhase() + 1;
            if (nextIndex < config.phases.size()) {
                return config.phases.get(nextIndex);
            }
            return null;
        }
    }

    public NextSpawn getNextSpawn(ServerPlayer player) {
        if (config == null || config.phases.isEmpty() || progressManager == null) {
            return NextSpawn.of(Blocks.BEDROCK);
        }

        if (Config.hasSharedProgress()) {
            return getNextSpawnShared(player);
        } else {
            return getNextSpawnSolo(player);
        }
    }

    private NextSpawn getNextSpawnShared(ServerPlayer player) {
        WorldProgressData data = progressManager.getData();
        data.blocksBroken++;
        progressManager.markDirtyAndSave();

        Phase previousPhase = config.phases.get(lastKnownPhaseIndex);
        Phase currentPhase = getCurrentPhase();

        if (currentPhase == null) {
            LOGGER.error("Could not determine current phase!");
            return NextSpawn.of(Blocks.STONE);
        }

        int currentPhaseIndex = config.phases.indexOf(currentPhase);
        if (currentPhaseIndex != lastKnownPhaseIndex) {
            if (previousPhase.end_of_phase_rewards != null) {
                executeRewards(player, previousPhase.end_of_phase_rewards);
            }
            player.sendSystemMessage(Component.literal("You have entered the " + currentPhase.name + " Phase!").withStyle(ChatFormatting.GREEN));

            String phaseName = currentPhase.name.toLowerCase().replace(" ", "_");
            AdvancementHelper.grantAdvancement(player, "phases/enter_" + phaseName);

            lastKnownPhaseIndex = currentPhaseIndex;
        }

        return processPhaseLogic(player, currentPhase, getNextPhase(), data.blocksBroken, data);
    }

    private NextSpawn getNextSpawnSolo(ServerPlayer player) {
        UUID playerId = player.getUUID();
        PlayerPhaseData data = getPlayerData(player);
        
        data.incrementBlocks();

        @SuppressWarnings("unused")
        int lastPhaseIndex = playerLastKnownPhase.getOrDefault(playerId, 0);
        Phase currentPhase = config.phases.get(data.getCurrentPhase());
        Phase nextPhase = (data.getCurrentPhase() < config.phases.size() - 1) 
            ? config.phases.get(data.getCurrentPhase() + 1) 
            : null;

        if (!currentPhase.repeatable && nextPhase != null && data.getBlocksBroken() >= currentPhase.blocks_needed) {
            if (currentPhase.end_of_phase_rewards != null) {
                executeRewards(player, currentPhase.end_of_phase_rewards);
            }
            data.setCurrentPhase(data.getCurrentPhase() + 1);
            data.setBlocksBroken(0);
            
            currentPhase = config.phases.get(data.getCurrentPhase());
            player.sendSystemMessage(Component.literal("You have entered the " + currentPhase.name + " Phase!").withStyle(ChatFormatting.GREEN));
            
            String phaseName = currentPhase.name.toLowerCase().replace(" ", "_");
            AdvancementHelper.grantAdvancement(player, "phases/enter_" + phaseName);

            playerLastKnownPhase.put(playerId, data.getCurrentPhase());
        }

        WorldProgressData playerWorldData = new WorldProgressData();
        playerWorldData.blocksBroken = data.getBlocksBroken();
        
        return processPhaseLogic(player, currentPhase, nextPhase, data.getBlocksBroken(), playerWorldData);
    }

    private NextSpawn processPhaseLogic(Player player, Phase currentPhase, Phase nextPhase, int blocksBroken, WorldProgressData worldData) {
        float playerLuck = player.getLuck();
        double unluckyChance = 0.02 - (playerLuck * 0.01);
        if (random.nextDouble() < unluckyChance) {
            return triggerUnluckyEvent(player);
        }

        if (currentPhase.repeatable) {
            if (infinityBlockCache == null) {
                LOGGER.error("Infinity caches have not been built! This should not happen.");
                return NextSpawn.of(Blocks.STONE);
            }
            if (random.nextDouble() < 0.02 && !infinityMobCache.isEmpty()) {
                EntityType<?> randomMob = infinityMobCache.get(random.nextInt(infinityMobCache.size()));
                randomMob.spawn((ServerLevel) player.level(), player.blockPosition().above(2), MobSpawnType.EVENT);
            }
            if (random.nextDouble() < 0.05 && !infinityChestCache.isEmpty()) {
                ResourceLocation randomLoot = infinityChestCache.get(random.nextInt(infinityChestCache.size()));
                return NextSpawn.of(new ChestData(0, randomLoot.toString(), false));
            }
            return NextSpawn.of(infinityBlockCache.get(random.nextInt(infinityBlockCache.size())));
        }

        Map<String, Object> blockProbabilities = new HashMap<>();
        if (nextPhase != null && !nextPhase.repeatable && blocksBroken >= nextPhase.blocks_needed - TRANSITION_WINDOW) {
            double rawProgress = (double)(blocksBroken - (nextPhase.blocks_needed - TRANSITION_WINDOW)) / TRANSITION_WINDOW;
            final double finalTransitionProgress = Math.min(1.0, Math.max(0.0, rawProgress));

            if (currentPhase.blocks != null) {
                currentPhase.blocks.forEach((key, value) -> {
                    blockProbabilities.put(key, getWeight(value) * (1.0 - finalTransitionProgress));
                });
            }
            if (nextPhase.blocks != null) {
                nextPhase.blocks.forEach((key, value) -> {
                    blockProbabilities.merge(key, getWeight(value) * finalTransitionProgress, (a, b) -> (Double)a + (Double)b);
                });
            }
        } else {
            if (currentPhase.blocks != null) {
                blockProbabilities.putAll(currentPhase.blocks);
            }
        }

        Set<String> uniqueChestSet;
        if (Config.isCoopMode()) {
            uniqueChestSet = worldData.spawnedOneTimeChests;
        } else {
            uniqueChestSet = getPlayerData((ServerPlayer) player).getSpawnedOneTimeChests();
        }

        Map<String, Object> finalProbabilities = new HashMap<>();
        for (Map.Entry<String, Object> entry : blockProbabilities.entrySet()) {
            ChestData originalChestData = getOriginalChestData(entry.getKey(), currentPhase, nextPhase);
            if (originalChestData != null && originalChestData.once && uniqueChestSet.contains(entry.getKey())) {
                continue; 
            }
            finalProbabilities.put(entry.getKey(), entry.getValue());
        }

        double totalWeight = finalProbabilities.values().stream().mapToDouble(this::getWeight).sum();
        if (totalWeight <= 0) return NextSpawn.of(Blocks.STONE);
        double randomValue = random.nextDouble() * totalWeight;

        for (Map.Entry<String, Object> entry : finalProbabilities.entrySet()) {
            randomValue -= getWeight(entry.getValue());
            if (randomValue <= 0.0) {
                String key = entry.getKey();

                if (key.equals("absoluteoneblock:random_ore")) {
                    if (randomOreCache != null && !randomOreCache.isEmpty()) {
                        Block randomOre = randomOreCache.get(random.nextInt(randomOreCache.size()));
                        return NextSpawn.of(randomOre);
                    } else {
                        return NextSpawn.of(Blocks.COAL_ORE);
                    }
                }

                if (key.equals("absoluteoneblock:random_log")) {
                    if (randomLogCache != null && !randomLogCache.isEmpty()) {
                        Block randomLog = randomLogCache.get(random.nextInt(randomLogCache.size()));
                        return NextSpawn.of(randomLog);
                    } else {
                        return NextSpawn.of(Blocks.OAK_LOG);
                    }
                }

                ChestData originalChestData = getOriginalChestData(entry.getKey(), currentPhase, nextPhase);
                if (originalChestData != null) {
                    if (originalChestData.once) {
                        uniqueChestSet.add(entry.getKey());
                        progressManager.markDirtyAndSave();
                    }
                    return NextSpawn.of(originalChestData);
                } else {
                    return NextSpawn.of(ForgeRegistries.BLOCKS.getValue(new ResourceLocation(key)));
                }
            }
        }

        return NextSpawn.of(Blocks.STONE);
    }
    
    private double getWeight(Object value) {
        if (value instanceof Double) return (Double) value;
        if (value instanceof ChestData) return ((ChestData) value).weight;
        return 0.0;
    }

    public ProgressManager getProgressManager() {
        return this.progressManager;
    }

    private ChestData getOriginalChestData(String key, Phase current, Phase next) {
        if (next != null && next.blocks != null && next.blocks.get(key) instanceof ChestData) {
            return (ChestData) next.blocks.get(key);
        }
        if (current != null && current.blocks != null && current.blocks.get(key) instanceof ChestData) {
            return (ChestData) current.blocks.get(key);
        }
        return null;
    }

    private NextSpawn triggerUnluckyEvent(Player player) {
        int outcome = random.nextInt(3);
        ServerLevel level = (ServerLevel) player.level();
        switch (outcome) {
            case 0:
                EntityType.SILVERFISH.spawn(level, player.blockPosition().above(), MobSpawnType.EVENT);
                return NextSpawn.of(Blocks.INFESTED_STONE);
            case 1:
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 200, 0));
                return NextSpawn.of(Blocks.SOUL_SAND);
            default:
                return NextSpawn.of(Blocks.GRAVEL);
        }
    }

    public Phase getCurrentPhase() {
        if (config == null || config.phases.isEmpty()) return null;
        for (int i = config.phases.size() - 1; i >= 0; i--) {
            Phase phase = config.phases.get(i);
            if (progressManager.getData().blocksBroken >= phase.blocks_needed) {
                return phase;
            }
        }
        return config.phases.get(0);
    }

    public Phase getNextPhase() {
        if (config == null || config.phases.isEmpty()) return null;
        Phase current = getCurrentPhase();
        if (current == null) return null;
        int index = config.phases.indexOf(current);
        if (index + 1 < config.phases.size()) {
            return config.phases.get(index + 1);
        }
        return null;
    }

    public int getNextPhaseBlocksNeeded() {
        Phase nextPhase = getNextPhase();
        if (nextPhase != null && !nextPhase.repeatable) {
            return nextPhase.blocks_needed;
        }
        return 0;
    }
    
    private void executeRewards(Player player, EndOfPhaseRewards rewards) {
        ServerLevel world = (ServerLevel) player.level();
        MinecraftServer server = world.getServer();

        if (rewards.message != null && rewards.message.text != null) {
            MutableComponent message = Component.literal(rewards.message.text);
            if (rewards.message.color != null) {
                ChatFormatting color = ChatFormatting.getByName(rewards.message.color.toUpperCase());
                if (color != null) message.withStyle(Style.EMPTY.withColor(color));
            }
            player.sendSystemMessage(message);
        }

        if (rewards.commands != null) {
            for (String command : rewards.commands) {
                String parsedCommand = command.replace("@p", player.getGameProfile().getName());
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), parsedCommand);
            }
        }

        if (rewards.loot_table != null) {
            LootTable lootTable = server.getLootData().getLootTable(new ResourceLocation(AbsoluteOneBlock.MOD_ID, "rewards/" + rewards.loot_table));
            LootParams lootParams = new LootParams.Builder(world)
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .withParameter(LootContextParams.ORIGIN, player.position())
                .create(LootContextParamSets.GIFT);
            lootTable.getRandomItems(lootParams).forEach(player::addItem);
        }
    }

    public WorldProgressData getProgressData() {
        return this.progressManager.getData();
    }

    public List<Phase> getPhases() {
        return config != null ? config.phases : new ArrayList<>();
    }
}
