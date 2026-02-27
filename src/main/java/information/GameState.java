package information;

import bwapi.*;
import bwem.BWEM;
import config.Config;
import information.enemy.EnemyUnits;
import information.enemy.enemyopeners.EnemyStrategy;
import information.enemy.enemytechunits.EnemyTechUnits;
import macro.ExpansionCriteria;
import macro.ResourceTracking;
import macro.buildorders.BuildOrder;
import macro.buildorders.BuildOrderManager;
import macro.buildorders.BunkerLocation;
import macro.buildorders.buildtransitions.BuildTransition;
import planner.PlannedItemStatus;
import planner.PlannedItemType;
import unitgroups.units.CombatUnits;
import unitgroups.units.Workers;
import map.BuildTiles;
import planner.BuildComparator;
import planner.PlannedItem;
import util.Time;

import java.util.ArrayList;
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
    private ExpansionCriteria expansionCriteria;

    private BuildOrder startingOpener = null;
    private EnemyUnits startingEnemyBase = null;
    private TilePosition bunkerPosition = null;
    private Time time = new Time(0);

    private boolean enemyInBase = false;
    private boolean enemyInNatural = false;
    private boolean enemyBuildingDiscovered = false;
    private boolean enemyFlyerInBase = false;
    private boolean canExpand = false;
    private boolean enemyOpenerIdentified = false;
    private boolean moveOutConditionsMet = false;
    private boolean hasTransitioned = false;
    private boolean beingSieged = false;

    private HashSet<CombatUnits> combatUnits = new HashSet<>();
    private HashSet<Unit> productionBuildings = new HashSet<>();
    private HashSet<Unit> allBuildings = new HashSet<>();
    private HashSet<Workers> workers = new HashSet<>();
    private HashSet<EnemyUnits> knownEnemyUnits = new HashSet<>();
    private HashSet<EnemyUnits> knownValidThreats = new HashSet<>();
    private HashSet<EnemyTechUnits> knownEnemyTechUnits = new HashSet<>();
    private HashSet<UnitType> techUnitResponse = new HashSet<>();
    private HashSet<BuildOrder> openingBuildOrders = new HashSet<>();
    private HashSet<BuildTransition> buildTransition = new HashSet<>();
    private HashSet<UnitType> liftableBuildings = new HashSet<>();
    private HashSet<UnitType> removeBuildings = new HashSet<>();
    private HashMap<UnitType, Integer> unitTypeCount = new HashMap<>();
    private HashMap<UnitType, Integer> openerMoveOutCondition = new HashMap<>();

    private PriorityQueue<PlannedItem> productionQueue = new PriorityQueue<>(new BuildComparator());

    public GameState(Game game, BWEM bwem, BaseInfo baseInfo) {
        this.game = game;
        this.bwem = bwem;
        this.baseInfo = baseInfo;
        this.config = new Config();

        player = game.self();

        buildTiles = new BuildTiles(game, baseInfo, this);
        buildOrderManager = new BuildOrderManager(game.enemy().getRace());
        resourceTracking = new ResourceTracking(player);
        expansionCriteria = new ExpansionCriteria(game, player, this);
        jadeBunkerPosition();
        addOpeningBuildOrders();
    }

    public void onFrame() {
        time = new Time(game.getFrameCount());

        resourceTracking.onFrame();
        expansionCriteria.onFrame();

        amendMoveOutCondition();
        moveOutConditionsMet();

        if(!hasTransitioned && shouldTransition()) {
            addBuildTransition();
            hasTransitioned = true;
        }

    }

    //Change when learning is added
    private void addOpeningBuildOrders() {
        openingBuildOrders = buildOrderManager.getOpenersForRace();

        for(BuildOrder bo : openingBuildOrders) {
            startingOpener = bo;
            productionQueue.addAll(bo.getBuildOrder());
            openerMoveOutCondition = bo.getMoveOutCondition(time, knownEnemyUnits);
            liftableBuildings.addAll(bo.getLiftableBuildings());

            if(bunkerPosition == null) {
                setBunkerPosition(bo.getBunkerLocation());
            }
        }
    }

    private void addBuildTransition() {
        buildTransition = buildOrderManager.getBuildTransitions();

        for(BuildTransition bt : buildTransition) {
            if(!bt.transitionsFrom(startingOpener)) {
                continue;
            }

            for(PlannedItem pi : bt.getOptionalBuildings()) {
                if(unitTypeCount.getOrDefault(pi.getUnitType(), 0) == 0) {
                    productionQueue.add(pi);
                }
            }

            productionQueue.addAll(bt.getTransitionBuild());
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

    public boolean moveOutConditionsMet() {
        for(UnitType unitType : openerMoveOutCondition.keySet()) {
            int requiredCount = openerMoveOutCondition.get(unitType);

            //Handle both tanks modes
            if(unitType == UnitType.Terran_Siege_Tank_Tank_Mode || unitType == UnitType.Terran_Siege_Tank_Siege_Mode) {
                int tankModeCount = unitTypeCount.getOrDefault(UnitType.Terran_Siege_Tank_Tank_Mode, 0);
                int siegeModeCount = unitTypeCount.getOrDefault(UnitType.Terran_Siege_Tank_Siege_Mode, 0);
                int totalTanks = tankModeCount + siegeModeCount;

                if(totalTanks < requiredCount) {
                    return false;
                }
            }
            //Group goliaths and vultures together
            else if(unitType == UnitType.Terran_Vulture) {
                int vultureCount = unitTypeCount.getOrDefault(UnitType.Terran_Vulture, 0);
                int goliathCount = unitTypeCount.getOrDefault(UnitType.Terran_Goliath, 0);
                int total = vultureCount + goliathCount;

                if(total < requiredCount) {
                    return false;
                }
            }
            else {
                if(!unitTypeCount.containsKey(unitType) || unitTypeCount.get(unitType) < requiredCount) {
                    return false;
                }
            }
        }
        return true;
    }

    private void amendMoveOutCondition() {
        if(enemyOpener != null) {
            HashMap<UnitType, Integer> enemyMoveOutCondition = enemyOpener.getMoveOutCondition(startingOpener.buildType(), time);

            if(enemyMoveOutCondition.isEmpty()) {
                openerMoveOutCondition = startingOpener.getMoveOutCondition(time, knownEnemyUnits);
                return;
            }

            openerMoveOutCondition = enemyMoveOutCondition;
        }
        else {
            openerMoveOutCondition = startingOpener.getMoveOutCondition(time, knownEnemyUnits);
        }
    }

    //Temp fix for Jade high ground bunker
    private void jadeBunkerPosition() {
        if(game.mapFileName().contains("Jade")) {
            bunkerPosition = buildTiles.getNaturalChokeBunker();
        }
    }

    private boolean shouldTransition() {
        return productionQueue.stream().noneMatch(pi -> pi.getSupply() > 0 && pi.getPlannedItemType() == PlannedItemType.BUILDING);
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

    public HashSet<UnitType> getLiftableBuildings() {
        return liftableBuildings;
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

    public boolean getCanExpand() {
        return canExpand;
    }

    public void setCanExpand(boolean canExpand) {
        this.canExpand = canExpand;
    }

    public boolean hasTransitioned() {
        return hasTransitioned;
    }

    public boolean isBeingSieged() {
        return beingSieged;
    }

    public void setBeingSieged(boolean beingSieged) {
        this.beingSieged = beingSieged;
    }
}
