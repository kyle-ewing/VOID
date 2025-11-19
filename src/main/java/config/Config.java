package config;

import io.github.cdimascio.dotenv.Dotenv;

public final class Config {
    // Debug drawing flags
    // HUD and general info
    public boolean debugHud = true;

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
    public boolean debugCombatUnits = true;
    public boolean debugScout = true;
    public boolean debugWorkers = true;

    // Production and planning
    public boolean debugProductionQueue = false;

    public Config() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        // HUD and general info
        this.debugHud = getBooleanOrDefault(dotenv, "DEBUG_HUD", this.debugHud);

        // Map and pathfinding
        this.debugBaseTiles = getBooleanOrDefault(dotenv, "DEBUG_BASE_TILES", this.debugBaseTiles);
        this.debugChokes = getBooleanOrDefault(dotenv, "DEBUG_CHOKES", this.debugChokes);
        this.debugMineralExclusionZone =
                getBooleanOrDefault(dotenv, "DEBUG_MINERAL_EXCLUSION_ZONE", this.debugMineralExclusionZone);
        this.debugGeyserExclusionZone =
                getBooleanOrDefault(dotenv, "DEBUG_GEYSER_EXCLUSION_ZONE", this.debugGeyserExclusionZone);
        this.debugCCExclusionZone =
                getBooleanOrDefault(dotenv, "DEBUG_CC_EXCLUSION_ZONE", this.debugCCExclusionZone);

        // Bases and buildings
        this.debugBases = getBooleanOrDefault(dotenv, "DEBUG_BASES", this.debugBases);
        this.debugBuildTiles = getBooleanOrDefault(dotenv, "DEBUG_BUILD_TILES", this.debugBuildTiles);
        this.debugBunkerTiles = getBooleanOrDefault(dotenv, "DEBUG_BUNKER_TILES", this.debugBunkerTiles);
        this.debugTurretTiles = getBooleanOrDefault(dotenv, "DEBUG_TURRET_TILES", this.debugTurretTiles);

        // Combat and units
        this.debugCombatUnits = getBooleanOrDefault(dotenv, "DEBUG_COMBAT_UNITS", this.debugCombatUnits);
        this.debugWorkers = getBooleanOrDefault(dotenv, "DEBUG_WORKERS", this.debugWorkers);
        this.debugScout = getBooleanOrDefault(dotenv, "DEBUG_SCOUT", this.debugScout);

        // Production and planning
        this.debugProductionQueue =
                getBooleanOrDefault(dotenv, "DEBUG_PRODUCTION_QUEUE", this.debugProductionQueue);
    }

    private boolean getBooleanOrDefault(Dotenv dotenv, String key, boolean defaultValue) {
        String value = dotenv.get(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }
}