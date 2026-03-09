This file is intented to be used by Claude Code when working with this repository

## Project Overview

VOID is a StarCraft: BroodWar Terran bot built with JBWAPI.

## Build

**Do NOT run `mvn` commands.** Claude Code cannot pull Maven dependencies.

## Dependencies

- **JBWAPI** 
- **BWEM**
- **dotenv-java**

---

## Architecture

### Entry Point
- **Bot.java** - Extends `DefaultBWListener`. `onStart()` initializes all managers. `onFrame()` is called every game tick (24 fps) and drives all logic by calling each manager's `onFrame()` in sequence. Other handlers: `onUnitCreate`, `onUnitComplete`, `onUnitDestroy`, `onUnitDiscover`, `onUnitShow`, `onUnitMorph`, `onUnitRenegade`.

### Central State
- **GameState** (`information/GameState.java`) - Single source of truth for all game state. Holds references to all managers, the production queue (`PriorityQueue<PlannedItem>`), all unit/building collections, boolean flags (e.g. `moveOutConditionsMet`, `hasTransitioned`, `beingSieged`), and the active `BuildOrder`/`BuildTransition`.

### Map Information
- **BaseInfo** (`information/BaseInfo.java`) - Analyzes map structure using BWEM. Tracks base locations (starting, natural, expansions), choke points, siege positions, tile sets per base, and geyser locations. Initialized once in `onStart()` via `init()`. Disseminates map data to all managers.

### Production
- **ProductionManager** (`macro/ProductionManager.java`) - The only source for queuing and executing unit/building/upgrade production. Reads from `GameState.productionQueue` (a `PriorityQueue<PlannedItem>` sorted by `BuildComparator`). Handles building placement using `BuildTiles`.
- **UnitProduction** (`macro/UnitProduction.java`) - Helper called by `ProductionManager` to issue actual unit production commands.
- **ResourceTracking** (`macro/ResourceTracking.java`) - Tracks mineral/gas availability.

### Build Orders
- **BuildOrder** (`macro/buildorders/BuildOrder.java`) - Implementations define the opening build queue (`getBuildOrder()`), move-out conditions (`getMoveOutCondition()`), bunker location, and build type (BIO/MECH).
- **BuildOrderManager** (`macro/buildorders/BuildOrderManager.java`) - Selects the opener based on opponent race.
- **BuildTransition** (`macro/buildorders/buildtransitions/BuildTransition.java`) - Runs after the initial `BuildOrder` queue is exhausted.

### Unit Control
- **UnitManager** (`unitgroups/UnitManager.java`) - Coordinates all combat units. Calls each unit's `onFrame()`, manages rally/attack/defend states, assigns scouts, detects all-in situations.
- **CombatUnits** (`unitgroups/units/CombatUnits.java`) - Abstract base for all owned combat unit wrappers (Marine, SiegeTank, Vulture, Goliath, etc.). Tracks `UnitStatus`, target references, threat flags, and implements shared attack logic. Subclasses override `rally()`, `defend()`, `onFrame()`, etc.
- **WorkerManager** (`unitgroups/WorkerManager.java`) - Manages SCVs. Workers use the `Workers` wrapper and `WorkerStatus`.

### Enemy Tracking
- **EnemyInformation** (`information/enemy/EnemyInformation.java`) - Tracks all known enemy units, identifies enemy strategy opener, manages valid threat sets and tech unit detection.
- **EnemyUnits** (`information/enemy/EnemyUnits.java`) - Wrapper for BWAPI enemy units. Tracks last known position, unit type, etc.
- **EnemyStrategyManager** (`information/enemy/EnemyStrategyManager.java`) - Detects opponent openers. 
- **EnemyTechUnits** - Detected tech units live in `information/enemy/enemytechunits/`. Responses stored in `GameState.techUnitResponse`.

### Planning / Queue
- **PlannedItem** (`planner/PlannedItem.java`) - Represents a single production item (unit, building, addon, or upgrade) with a type (`PlannedItemType`), status (`PlannedItemStatus`), and priority.
- **PlannedItemStatus** is broken down through these states in order: NOT_STARTED -> SCV_ASSIGNED (non addon buildings only) -> IN_PROGRESS -> COMPLETED

### Utilities
- **Time** (`util/Time.java`) - Frame/minute/second conversions.
- **ClosestUnit** (`util/ClosestUnit.java`) - Finds closest target to the given unit. Can be used for closest friendly or enemy units.
- **RallyPoint** (`util/RallyPoint.java`) - Computes rally positions.
- **PositionInterpolator** (`util/PositionInterpolator.java`) - Generates valid positions between two points from 0.0 to 1 (1 being the endpoint).
- **PathFinding** (`map/PathFinding.java`) - Path calculation using A*.

### Debug / Config
- **Config** (`config/Config.java`) - All debug drawing toggles, loaded from `.env` via dotenv-java.
- **Painters** (`debug/Painters.java`) - All in-game debug visualization.

---

## Code Style

- **No comments inside function bodies.**
- **No ternary operators.**
- **Else-if and else on their own line**, not sharing the line with a closing brace:
  ```java
  if (condition) {
      ...
  }
  else if (other) {
      ...
  }
  else {
      ...
  }
  ```
- Naming: PascalCase classes, camelCase methods/fields, UPPER_CASE constants/enums.
- Prefer early returns over deep nesting.