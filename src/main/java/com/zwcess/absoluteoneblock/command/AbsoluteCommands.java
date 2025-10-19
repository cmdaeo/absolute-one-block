package com.zwcess.absoluteoneblock.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.zwcess.absoluteoneblock.config.Config;
import com.zwcess.absoluteoneblock.dimension.AbsoluteDimensions;
import com.zwcess.absoluteoneblock.game.IslandManager;
import com.zwcess.absoluteoneblock.game.PhaseManager;
import com.zwcess.absoluteoneblock.game.ProgressManager;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;

public class AbsoluteCommands {

    @SuppressWarnings("null")
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("oneblock").then(Commands.literal("enter")
        .executes((command) -> {
                ServerPlayer player = command.getSource().getPlayerOrException();
                ServerLevel dimension = player.getServer().getLevel(AbsoluteDimensions.ONEBLOCK_DIMENSION_KEY);

                if (dimension == null) {
                    command.getSource().sendFailure(Component.literal("Dimension not found!"));
                    return 0;
                }

                ProgressManager progressManager = PhaseManager.INSTANCE.getProgressManager();
                IslandManager islandManager = new IslandManager(progressManager);

                // --- THIS IS THE KEY LOGIC ---
                // 1. ALWAYS get or create the persistent island position for the player.
                BlockPos islandPos = islandManager.getOrCreateIslandPosition(player);
                
                // 2. Check if a block exists at that position. If not, create it.
                // This prevents creating a second block if the player already has one.
                if (dimension.getBlockState(islandPos).isAir()) {
                    dimension.setBlock(islandPos, Blocks.DIRT.defaultBlockState(), 3);
                    player.sendSystemMessage(Component.literal("Welcome! Your island has been created."));
                }

                // 3. Teleport the player to their one and only island position.
                player.teleportTo(dimension,
                    islandPos.getX() + 0.5,
                    islandPos.getY() + 1.0,
                    islandPos.getZ() + 0.5,
                    player.getYRot(),
                    player.getXRot());
                
                command.getSource().sendSuccess(() -> Component.literal("Teleported to your One Block."), false);
                return 1;
            }))

            // Subcommand: /oneblock leave
            .then(Commands.literal("leave").executes((command) -> {
                ServerPlayer player = command.getSource().getPlayerOrException();
                ServerLevel overworld = player.getServer().getLevel(ServerLevel.OVERWORLD);

                if (overworld != null) {
                    // Use spawn point for a safer exit
                    BlockPos spawnPoint = overworld.getSharedSpawnPos();
                    player.teleportTo(overworld, spawnPoint.getX(), spawnPoint.getY(), spawnPoint.getZ(), player.getYRot(), player.getXRot());
                    command.getSource().sendSuccess(() -> Component.literal("Teleported to Overworld."), false);
                    return 1;
                } else {
                    command.getSource().sendFailure(Component.literal("Overworld not found!"));
                    return 0;
                }
            }))
            
            // Subcommand: /oneblock reset
            .then(Commands.literal("reset")
                .requires(source -> source.hasPermission(2))
                .executes((command) -> {
                    // This command should also reset island positions
                    ProgressManager progressManager = PhaseManager.INSTANCE.getProgressManager();
                    progressManager.getData().blocksBroken = 0;
                    progressManager.getData().spawnedOneTimeChests.clear();
                    progressManager.getPlayerIslandPositions().clear(); // NEW: Clear island map
                    progressManager.markDirtyAndSave();

                    PhaseManager.INSTANCE.resetPhase(); // Keep internal state reset if needed
                    command.getSource().sendSuccess(() -> Component.literal("Full One Block progression has been reset."), true);
                    return 1;
                }))
            
            // Subcommands: setphase, setblocks (no changes needed here, they work on global progress)
            .then(Commands.literal("setphase")
            .requires(source -> source.hasPermission(2)) // Operator-level permission
            .then(Commands.argument("phase", IntegerArgumentType.integer(0)) // Accepts a positive integer
                .executes((command) -> {
                    int phaseIndex = IntegerArgumentType.getInteger(command, "phase");
                    
                    // Check if the game is in a mode that uses shared progress
                    if (!Config.hasSharedProgress()) {
                        command.getSource().sendFailure(Component.literal("This command only works in COOP or COMPETITIVE_SHARED modes."));
                        return 0;
                    }

                    if (PhaseManager.INSTANCE.setPhase(phaseIndex)) {
                        command.getSource().sendSuccess(() -> 
                            Component.literal("Successfully set global One Block phase to index " + phaseIndex), true);
                        return 1;
                    } else {
                        command.getSource().sendFailure(Component.literal("Invalid phase index. Use a number between 0 and " + (PhaseManager.INSTANCE.getPhases().size() - 1)));
                        return 0;
                    }
                })))

        // Subcommand: /oneblock setblocks <number_of_blocks>
            .then(Commands.literal("setblocks")
                .requires(source -> source.hasPermission(2)) // Operator-level permission
                .then(Commands.argument("blocks", IntegerArgumentType.integer(1)) // Accepts an integer greater than 0
                    .executes((command) -> {
                        int blocksNeeded = IntegerArgumentType.getInteger(command, "blocks");

                        // Check if the game is in a mode that uses shared progress
                        if (!Config.hasSharedProgress()) {
                            command.getSource().sendFailure(Component.literal("This command only works in COOP or COMPETITIVE_SHARED modes."));
                            return 0;
                        }

                        PhaseManager.INSTANCE.setBlocksNeededForNextPhase(blocksNeeded);
                        command.getSource().sendSuccess(() -> 
                            Component.literal("Blocks needed for the next phase transition set to " + blocksNeeded + " from now."), true);
                        return 1;
                    })))
            
            // Subcommand: /oneblock fix
            .then(Commands.literal("fix")
                .executes((command) -> {
                    ServerPlayer player = command.getSource().getPlayerOrException();
                    ServerLevel dimension = player.getServer().getLevel(AbsoluteDimensions.ONEBLOCK_DIMENSION_KEY);

                    if (dimension == null) {
                        command.getSource().sendFailure(Component.literal("Dimension not found!"));
                        return 0;
                    }
                    
                    // --- FIX: Use the same persistent logic as /enter to find the block ---
                    ProgressManager progressManager = PhaseManager.INSTANCE.getProgressManager();
                    Config.GameMode currentMode = Config.getGameMode();

                    BlockPos playerBlockPos;
                    if (currentMode == Config.GameMode.COOP) {
                        playerBlockPos = new BlockPos(0, 100, 0);
                    } else {
                        IslandManager islandManager = new IslandManager(progressManager);
                        playerBlockPos = islandManager.getOrCreateIslandPosition(player);
                    }
                    
                    dimension.setBlock(playerBlockPos, Blocks.DIRT.defaultBlockState(), 3);
                    command.getSource().sendSuccess(() -> Component.literal("Your One Block has been fixed to Dirt."), false);
                    return 1;
                })
            )
            
            // --- FIX: /oneblock mode commands now save persistently ---
            .then(Commands.literal("mode")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("coop")
                    .executes(context -> setGameMode(context.getSource(), Config.GameMode.COOP)))
                .then(Commands.literal("competitive_shared") // Renamed for clarity
                    .executes(context -> setGameMode(context.getSource(), Config.GameMode.COMPETITIVE_SHARED)))
                .then(Commands.literal("competitive_solo") // Renamed for clarity
                    .executes(context -> setGameMode(context.getSource(), Config.GameMode.COMPETITIVE_SOLO)))
            )

            // --- NEW: Command to control island distribution ---
            .then(Commands.literal("settings")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("distribution")
                    .then(Commands.argument("equal", BoolArgumentType.bool())
                        .executes(context -> {
                            boolean isEqual = BoolArgumentType.getBool(context, "equal");
                            Config.EQUALLY_DISTRIBUTED.set(isEqual);
                            // Configs are auto-saved, but a message is good practice.
                            context.getSource().sendSuccess(() -> Component.literal("Island distribution set to: " + (isEqual ? "Equal" : "Sequential")), true);
                            return 1;
                        })
                    )
                )
            )
        );
    }

    // Helper method to avoid repeating code for setting game mode
    private static int setGameMode(CommandSourceStack source, Config.GameMode mode) {
        // Get the current mode from the config to see if we're actually changing anything
        Config.GameMode currentMode = Config.getGameMode();

        if (currentMode == mode) {
            source.sendSuccess(() -> Component.literal("Game mode is already set to " + mode.name()), false);
            return 1;
        }

        // Set the new value in the config file
        Config.GAME_MODE.set(mode);

        boolean wasCompetitive = (currentMode == Config.GameMode.COMPETITIVE_SHARED || currentMode == Config.GameMode.COMPETITIVE_SOLO);
        boolean isNowCompetitive = (mode == Config.GameMode.COMPETITIVE_SHARED || mode == Config.GameMode.COMPETITIVE_SOLO);

        if (wasCompetitive != isNowCompetitive) {
            source.sendSuccess(() -> Component.literal("Game mode set to " + mode.name() + ". A server restart is required to apply the change and wipe the world."), true);
        } else {
            source.sendSuccess(() -> Component.literal("Game mode set to " + mode.name() + ". No restart is needed."), true);
        }
        
        return 1;
    }
}
