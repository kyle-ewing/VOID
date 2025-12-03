package information;

import bwapi.*;
import bwem.BWEM;
import config.Config;
import information.enemy.EnemyUnits;
import information.enemy.enemyopeners.EnemyStrategy;
import information.enemy.enemytechunits.EnemyTechUnits;
import macro.ResourceTracking;
import macro.buildorders.BuildOrder;
import macro.buildorders.BuildOrderManager;
import macro.buildorders.BunkerLocation;
import unitgroups.units.CombatUnits;
import unitgroups.units.Workers;
import map.BuildTiles;
import planner.BuildComparator;
import planner.PlannedItem;

import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;

public class GameState {
    private Game game;
    private BWEM bwem;
    private Config config;
    private BaseInfo baseInfo;
    private Player player;
    private BuildTiles buildTiles;
    private ResourceTracking resourceTracking;
    private BuildOrderManager buildOrderManager;
    private EnemyStrategy enemyOpener = null;

    private BuildOrder startingOpener = null;
    private EnemyUnits startingEnemyBase = null;
    private TilePosition bunkerPosition = null;

    private boolean enemyInBase = false;
    private boolean enemyInNatural = false;
    private boolean enemyBuildingDiscovered = false;
    private boolean enemyFlyerInBase = false;

    private HashSet<CombatUnits> combatUnits = new HashSet<>();
    private HashSet<Unit> productionBuildings = new HashSet<>();
    private HashSet<Unit> allBuildings = new HashSet<>();
    private HashSet<Workers> workers = new HashSet<>();
    private HashSet<EnemyUnits> knownEnemyUnits = new HashSet<>();
    private HashSet<EnemyUnits> knownValidThreats = new HashSet<>();
    private HashSet<EnemyTechUnits> knownEnemyTechUnits = new HashSet<>();
    private HashSet<UnitType> techUnitResponse = new HashSet<>();
    private HashSet<BuildOrder> openingBuildOrders = new HashSet<>();
    private HashMap<UnitType, Integer> unitTypeCount = new HashMap<>();
    private HashMap<UnitType, Integer> openerMoveOutCondition = new HashMap<>();

    private PriorityQueue<PlannedItem> productionQueue = new PriorityQueue<>(new BuildComparator());

    public GameState(Game game, BWEM bwem, BaseInfo baseInfo) {
        this.game = game;
        this.bwem = bwem;
        this.baseInfo = baseInfo;
        this.config = new Config();

        player = game.self();

        buildTiles = new BuildTiles(game, baseInfo);
        buildOrderManager = new BuildOrderManager(game.enemy().getRace());
        resourceTracking = new ResourceTracking(player);
        addOpeningBuildOrders();
    }

    public void onFrame() {
        resourceTracking.onFrame();
    }

    //Change when learning is added
    private void addOpeningBuildOrders() {
        openingBuildOrders = buildOrderManager.getOpenersForRace();

        for(BuildOrder bo : openingBuildOrders) {
            startingOpener = bo;
            productionQueue.addAll(bo.getBuildOrder());
            setBunkerPosition(bo.getBunkerLocation());
            openerMoveOutCondition = bo.getMoveOutCondition();
        }
    }

    private void setBunkerPosition(BunkerLocation bunkerLocation) {
        switch(bunkerLocation) {
            case MAIN:
                bunkerPosition = buildTiles.getMainChokeBunker();
                break;
            case NATURAL:
                bunkerPosition = buildTiles.getNaturalChokeBunker();
                break;
            default:
                bunkerPosition = null;
                break;
        }
    }

    public BaseInfo getBaseInfo() {
        return baseInfo;
    }

    public BuildTiles getBuildTiles() {
        return buildTiles;
    }

    public Config getConfig() {
        return config;
    }

    public HashSet<Workers> getWorkers() {
        return workers;
    }

    public HashSet<CombatUnits> getCombatUnits() {
        return combatUnits;
    }

    public PriorityQueue<PlannedItem> getProductionQueue() {
        return productionQueue;
    }

    public HashMap<UnitType, Integer> getUnitTypeCount() {
        return unitTypeCount;
    }

    public HashSet<Unit> getAllBuildings() {
        return allBuildings;
    }

    public HashSet<Unit> getProductionBuildings() {
        return productionBuildings;
    }

    public HashSet<BuildOrder> getOpeningBuildOrders() {
        return openingBuildOrders;
    }

    public BuildOrder getStartingOpener() {
        return startingOpener;
    }

    public TilePosition getBunkerPosition() {
        return bunkerPosition;
    }

    public HashSet<EnemyUnits> getKnownEnemyUnits() {
        return knownEnemyUnits;
    }

    public HashSet<EnemyUnits> getKnownValidThreats() {
        return knownValidThreats;
    }

    public HashSet<UnitType> getTechUnitResponse() {
        return techUnitResponse;
    }

    public HashSet<EnemyTechUnits> getKnownEnemyTechUnits() {
        return knownEnemyTechUnits;
    }

    public EnemyUnits getStartingEnemyBase() {
        return startingEnemyBase;
    }

    public boolean isEnemyBuildingDiscovered() {
        return enemyBuildingDiscovered;
    }

    public void setEnemyBuildingDiscovered(boolean enemyBuildingDiscovered) {
        this.enemyBuildingDiscovered = enemyBuildingDiscovered;
    }

    public void setStartingEnemyBase(EnemyUnits startingEnemyBase) {
        this.startingEnemyBase = startingEnemyBase;
    }

    public void setEnemyOpener(EnemyStrategy enemyOpener) {
        this.enemyOpener = enemyOpener;
    }

    public EnemyStrategy getEnemyOpener() {
        return enemyOpener;
    }

    public HashMap<UnitType, Integer> getOpenerMoveOutCondition() {
        return openerMoveOutCondition;
    }

    public boolean isEnemyInBase() {
        return enemyInBase;
    }

    public void setEnemyInBase(boolean enemyInBase) {
        this.enemyInBase = enemyInBase;
    }

    public boolean isEnemyInNatural() {
        return enemyInNatural;
    }

    public void setEnemyInNatural(boolean enemyInNatural) {
        this.enemyInNatural = enemyInNatural;
    }

    public ResourceTracking getResourceTracking() {
        return resourceTracking;
    }

    public boolean enemyFlyerInBase() {
        return enemyFlyerInBase;
    }

    public void setEnemyFlyerInBase(boolean enemyFlyerInBase) {
        this.enemyFlyerInBase = enemyFlyerInBase;
    }
}
