package config;

import io.github.cdimascio.dotenv.Dotenv;

public final class Config {
    // Debug drawing flags
    // HUD and general info
    public boolean debugHud = false;

    // Map
    public boolean debugBases = false;
    public boolean debugChokes = false;
    public boolean debugBaseTiles = false;
    public boolean debugMineralExclusionZone = false;
    public boolean debugGeyserExclusionZone = false;
    public boolean debugCCExclusionZone = false;

    // Bases and buildings
    public boolean debugBuildTiles = false;
    public boolean debugBunkerTiles = false;
    public boolean debugTurretTiles = false;

    // Combat Units and workers
    public boolean debugCombatUnits = false;
    public boolean debugScout = false;
    public boolean debugWorkers = false;

    // Production and planning
    public boolean debugProductionQueue = false;

    public Config() {

        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        // Load debug drawing flags
        // HUD and general info
        this.debugHud = Boolean.parseBoolean(dotenv.get("DEBUG_HUD"));

        // Map and pathfinding
        this.debugBaseTiles = Boolean.parseBoolean(dotenv.get("DEBUG_BASE_TILES"));
        this.debugChokes = Boolean.parseBoolean(dotenv.get("DEBUG_CHOKES"));
        this.debugMineralExclusionZone = Boolean.parseBoolean(dotenv.get("DEBUG_MINERAL_EXCLUSION_ZONE"));
        this.debugGeyserExclusionZone = Boolean.parseBoolean(dotenv.get("DEBUG_GEYSER_EXCLUSION_ZONE"));
        this.debugCCExclusionZone = Boolean.parseBoolean(dotenv.get("DEBUG_CC_EXCLUSION_ZONE"));

        // Bases and buildings
        this.debugBases = Boolean.parseBoolean(dotenv.get("DEBUG_BASES"));
        this.debugChokes = Boolean.parseBoolean(dotenv.get("DEBUG_CHOKES"));
        this.debugBuildTiles = Boolean.parseBoolean(dotenv.get("DEBUG_BUILD_TILES"));
        this.debugBunkerTiles = Boolean.parseBoolean(dotenv.get("DEBUG_BUNKER_TILES"));
        this.debugTurretTiles = Boolean.parseBoolean(dotenv.get("DEBUG_TURRET_TILES"));

        // Combat and units
        this.debugCombatUnits = Boolean.parseBoolean(dotenv.get("DEBUG_COMBAT_UNITS"));
        this.debugWorkers = Boolean.parseBoolean(dotenv.get("DEBUG_WORKERS"));
        this.debugScout = Boolean.parseBoolean(dotenv.get("DEBUG_SCOUT"));

        // Production and planning
        this.debugProductionQueue = Boolean.parseBoolean(dotenv.get("DEBUG_PRODUCTION_QUEUE"));
    }
}