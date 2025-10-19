# Absolute One Block

<p align="center">
  <img src="https://img.shields.io/github/repo-size/cmdaeo/absolute-one-block" alt="GitHub repo size">
  <img src="https://img.shields.io/github/languages/top/cmdaeo/absolute-one-block" alt="GitHub top language">
  <img src="https://img.shields.io/github/license/cmdaeo/absolute-one-block" alt="GitHub license">
</p>

**Absolute One Block** is an advanced and highly customizable Minecraft mod that transforms the "One Block" survival challenge. It places players in a custom, minimalist dimension where they must build an empire starting from a single, infinitely regenerating block. Designed with a robust, data-driven architecture, the mod offers deep customization, extensive multiplayer support, and powerful compatibility features that make it a premier choice for any modpack.

## Table of Contents
1.  [Core Gameplay & Features](#core-gameplay--features)
2.  [Multiplayer and World Architecture](#multiplayer-and-world-architecture)
3.  [Custom Content and Items](#custom-content-and-items)
4.  [Player and Admin Tutorial](#player-and-admin-tutorial)
5.  [Modpack Creator & Developer Guide](#modpack-creator--developer-guide)
6.  [Installation and Building](#installation-and-building)

***

## Core Gameplay & Features

The central gameplay loop revolves around the "One Block," which evolves over time based on a powerful phased progression system, ensuring a dynamic and ever-changing survival experience.

### Phased Progression System
The game is structured into a series of distinct phases (e.g., Surface, Mining, Nether, End), each fully defined in an external `phases.json` file. As players break more blocks, they unlock new phases, which grant them access to new sets of blocks, items, mobs, and treasure chests, creating a clear and rewarding sense of advancement.

### Highly Configurable Spawns
Each phase has a unique, weighted list of possible spawns. Modpack creators can easily edit the `phases.json` file to control exactly what appears, how often it appears, and at what stage of the game. This allows for finely tuned difficulty curves, unique thematic progressions, and seamless integration of content from other mods.

### Data-Driven by Design
Nearly every gameplay element is controlled by external JSON files. This makes the mod incredibly adaptable and easy to modify without any Java programming. This includes:
*   The sequence and block requirements of each phase.
*   The contents of all treasure chests (`loot_tables`).
*   The rewards given to players upon completing a phase.
*   The entire custom advancement tree.

***

## Multiplayer and World Architecture

The mod is engineered from the ground up for a seamless multiplayer experience, offering multiple server-wide game modes and intelligent world management.

### Three Distinct Game Modes
1.  **COOP**: All players work together on a single, shared island at world center, sharing all progression. This mode fosters teamwork and collaboration.
2.  **COMPETITIVE_SHARED**: Each player is given their own private island, but all players share the same global phase progression. This creates a server-wide race to see who can best utilize the resources available at each stage.
3.  **COMPETITIVE_SOLO**: Each player gets their own island and has their own independent phase progression, creating a parallel single-player experience on a multiplayer server.

### Dynamic Island Placement
To ensure fairness in competitive modes, the `IslandManager` automatically assigns island locations to new players. Admins can choose between two strategies:
*   **Equal Distribution**: Places new islands in the middle of the largest available empty space, keeping all players as far apart as possible.
*   **Sequential**: Places new islands in a simple, predictable straight line with a configurable spacing.

### Custom Dimension with Dynamic Structure Loading
The mod features a unique `oneblock_dimension` that is, by default, an empty void perfect for skyblock-style play. Crucially, this dimension's world generator is configured to inherit the Overworld's structure generation settings. This means **any structure that can generate in the Overworld—including those from other mods (e.g., villages, dungeons, modded ruins)—can potentially be discovered by players** as they expand their islands. This creates a vast potential for exploration and emergent gameplay, seamlessly integrating the mod with any modpack.

***

## Custom Content and Items

To enhance the unique survival experience, the mod introduces several custom elements.

*   **Heart of the Void**: A powerful utility item designed to save players from falling into the void, a constant danger in the skyblock environment.
*   **Platform Builder Tool**: A specialized tool that allows players to easily and efficiently expand their island base, reducing the tedium of manual building.
*   **Full Advancement Tree**: A complete set of custom advancements guides new players, marks key progression milestones (like reaching a new phase or breaking 1,000 blocks), and offers unique challenges for veterans.

***

## Player and Admin Tutorial

### Player Commands
*   `/oneblock enter`: Teleports you to your island in the One Block dimension. If it's your first time, an island will be generated for you.

### Administrator Commands
All admin commands require OP (operator) permissions.
*   `/oneblock mode <coop | competitive_shared | competitive_solo>`: Changes the server-wide game mode. **Warning**: Changing the mode *type* (e.g., from COOP to COMPETITIVE) will wipe all player progress.
*   `/oneblock setspacing <number>`: Sets the distance (in blocks) between islands in competitive modes.
*   `/oneblock setdistribution <equal | sequential>`: Sets the island placement strategy.
*   `/oneblock resetprogress [player]` (Coming Soon): Resets the progress for a specific player or all players.

***

## Modpack Creator & Developer Guide

### Customizing Phases (`phases.json`)
You are absolutely right. My previous explanation of the `phases.json` structure was based on a simplified assumption of your mod's design, and the example you provided shows it is far more detailed and powerful. My apologies for that error.

Based on the actual structure you've shared, here is the corrected and much more accurate guide for the README.

***

### Customizing Phases (`phases.json`)
The core of your customization will happen in `src/main/resources/data/absoluteoneblock/phases.json`. The file contains a root object with a single key, `"phases"`, which holds an array of phase objects. Each phase object has a detailed structure that gives you precise control over the gameplay.

**Structure of a Phase Object:**
*   `name` (String): The display name of the phase (e.g., "Surface").
*   `description` (String): A brief description of the phase's theme or content.
*   `blocks` (Object): A map where each key is a block's resource location (`minecraft:dirt`). The value can be:
    *   An **integer** representing its spawn weight.
    *   An **object** for more complex spawns like chests, containing a `weight`, a `loot_table` path, and an optional boolean `once` to make it a one-time spawn.
*   `mobs` (Object): A map where each key is a mob's entity ID (`minecraft:cow`) and the value is its spawn weight.
*   `mob_spawn_chance` (Decimal): A value from 0.0 to 1.0 representing the base probability that a mob will spawn instead of a block.
*   `blocks_needed` (Integer): The **cumulative** number of blocks a player (or team) must have broken to unlock this phase. The first phase should have a value of `0`.
*   `end_of_phase_rewards` (Object): Defines what happens when the phase is completed. It can contain:
    *   A `message` object with `text` and `color` to display to the player.
    *   An array of `commands` to execute on the player.
    *   A `loot_table` to grant a set of reward items.

**Example: The "Surface" Phase**
```json
{
  "name": "Surface",
  "description": "Basic survival - wood, stone, food",
  "blocks": {
    "minecraft:dirt": 25,
    "minecraft:chest": { "weight": 3, "loot_table": "minecraft:chests/village/village_plains_house" },
    "absoluteoneblock:starter_chest": { "weight": 5, "loot_table": "absoluteoneblock:chests/starting_treasure", "once": true }
  },
  "mobs": {
    "minecraft:cow": 8,
    "minecraft:pig": 7
  },
  "mob_spawn_chance": 0.04,
  "blocks_needed": 0,
  "end_of_phase_rewards": {
    "message": {
      "text": "You feel the ground beneath you rumble. Time to start mining!",
      "color": "yellow"
    },
    "commands": ["/give @p minecraft:stone_pickaxe 1"]
  }
}
```

**How to Add Custom Content**
To add Tin Ore from the Thermal Series mod to the "Mining" phase, you would find the "Mining" phase object in the array and add a new entry to its `blocks` map:
```json
"blocks": {
  "minecraft:stone": 20,
  "minecraft:deepslate": 12,
  "thermal:tin_ore": 8,  // <-- Added entry
  ...
}
```
The weight of `8` will be calculated relative to the other block entries in that specific phase, determining its spawn frequency.

### Technical Notes for Developers
*   **Data Persistence**: All player and world data is managed by `ProgressManager.java`. It currently serializes data to JSON strings within NBT tags. For better performance and resilience against code changes, refactoring this to use direct NBT serialization is highly recommended.
*   **Event Bus**: The mod uses Forge's event bus for all major logic. The `BlockBreakEventHandler` is the primary "hot path" for gameplay.
*   **Extensibility**: Consider creating and firing custom Forge events (e.g., `PhaseChangeEvent`) to allow other mods to easily integrate with your progression system.

***

## Installation and Building

### Prerequisites
*   Java Development Kit (JDK) 17 or newer.
*   Minecraft Forge for Minecraft 1.20.1.

### Installation
1.  Download the latest release from the [Releases](https://github.com/cmdaeo/absolute-one-block/releases) page.
2.  Place the downloaded `.jar` file into the `mods` folder of your Minecraft client or server.

### Building from Source
1.  Clone the repository: `git clone https://github.com/cmdaeo/absolute-one-block.git`
2.  Navigate to the project directory: `cd absolute-one-block`
3.  Run the Gradle build command:
    *   On Windows: `gradlew build`
    *   On macOS/Linux: `./gradlew build`
4.  The compiled mod `.jar` will be located in the `build/libs/` directory.

***
**Keywords**: Minecraft Mod, One Block, Skyblock, Forge, 1.20.1, Survival Challenge, Multiplayer, Competitive, Cooperative, Modpack, Data-Driven, Custom Dimension, JSON, Sky Factory, Sky Block