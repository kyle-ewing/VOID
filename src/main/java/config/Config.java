package config;

import io.github.cdimascio.dotenv.Dotenv;

public final class Config {
    Dotenv dotenv  = Dotenv.configure().ignoreIfMissing().load();;

    // Debug drawing flags
    // HUD and general info
    public boolean debugHud = true;
    public boolean gameSpeed = false;

    // Map
    public boolean debugBases = false;
    public boolean debugChokes = false;
    public boolean debugBaseTiles = false;
    public boolean debugMineralExclusionZone = false;
    public boolean debugGeyserExclusionZone = false;
    public boolean debugCCExclusionZone = false;

    // Bases and buildings
    public boolean debugBuildTiles = true;
    public boolean debugBunkerTiles = true;
    public boolean debugTurretTiles = true;

    // Combat Units and workers
    public boolean debugCombatUnits = true;
    public boolean debugScout = true;
    public boolean debugWorkers = true;
    public boolean debugDetailedUnitInfo = false;

    //Enemy Units
    public boolean debugEnemyUnits = true;

    // Production and planning
    public boolean debugProductionQueue = true;

    public Config() {
        // HUD and general info
        this.debugHud = getBooleanOrDefault("DEBUG_HUD", this.debugHud);
        this.gameSpeed = getBooleanOrDefault("GAME_SPEED", this.gameSpeed);

        // Map and pathfinding
        this.debugBaseTiles = getBooleanOrDefault("DEBUG_BASE_TILES", this.debugBaseTiles);
        this.debugChokes = getBooleanOrDefault("DEBUG_CHOKES", this.debugChokes);
        this.debugMineralExclusionZone = getBooleanOrDefault("DEBUG_MINERAL_EXCLUSION_ZONE", this.debugMineralExclusionZone);
        this.debugGeyserExclusionZone = getBooleanOrDefault("DEBUG_GEYSER_EXCLUSION_ZONE", this.debugGeyserExclusionZone);
        this.debugCCExclusionZone = getBooleanOrDefault("DEBUG_CC_EXCLUSION_ZONE", this.debugCCExclusionZone);

        // Bases and buildings
        this.debugBases = getBooleanOrDefault("DEBUG_BASES", this.debugBases);
        this.debugBuildTiles = getBooleanOrDefault("DEBUG_BUILD_TILES", this.debugBuildTiles);
        this.debugBunkerTiles = getBooleanOrDefault("DEBUG_BUNKER_TILES", this.debugBunkerTiles);
        this.debugTurretTiles = getBooleanOrDefault("DEBUG_TURRET_TILES", this.debugTurretTiles);

        // Combat and units
        this.debugCombatUnits = getBooleanOrDefault("DEBUG_COMBAT_UNITS", this.debugCombatUnits);
        this.debugWorkers = getBooleanOrDefault("DEBUG_WORKERS", this.debugWorkers);
        this.debugScout = getBooleanOrDefault("DEBUG_SCOUT", this.debugScout);
        this.debugDetailedUnitInfo = getBooleanOrDefault("DEBUG_DETAILED_UNIT_INFO", this.debugDetailedUnitInfo);

        // Enemy Units
         this.debugEnemyUnits = getBooleanOrDefault("DEBUG_ENEMY_UNITS", this.debugEnemyUnits);

        // Production and planning
        this.debugProductionQueue = getBooleanOrDefault("DEBUG_PRODUCTION_QUEUE", this.debugProductionQueue);
    }

    private boolean getBooleanOrDefault(String key, boolean defaultValue) {
        String value = dotenv.get(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }
}