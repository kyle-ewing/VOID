package macro;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

import bwapi.Game;
import bwapi.Player;
import bwapi.TechType;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwapi.UpgradeType;
import bwem.Base;
import information.GameState;
import information.MapInfo;
import information.enemy.enemytechbuildings.EnemyTechBuilding;
import information.enemy.enemytechunits.EnemyTechUnits;
import macro.buildorders.BuildOrder;
import macro.buildorders.BuildType;
import map.BuildTiles;
import map.TilePositionValidator;
import planner.PlannedItem;
import planner.PlannedItemStatus;
import planner.PlannedItemType;
import unitgroups.units.WorkerStatus;
import unitgroups.units.Workers;
import util.ClosestUnit;
import util.Time;

public class ProductionManager {
    private Game game;
    private GameState gameState;
    private Player player;
    private MapInfo mapInfo;
    private TilePositionValidator tilePositionValidator;
    private BuildTiles buildTiles;
    private HashMap<UnitType, Integer> unitTypeCount;
    private HashSet<Unit> productionBuildings;
    private HashSet<Unit> allBuildings;
    private HashSet<TilePosition> reservedTurretPositions = new HashSet<>();
    private PriorityQueue<PlannedItem> productionQueue;
    private BuildOrder startingOpener;
    private UnitProduction unitProduction;
    private TilePosition bunkerPosition = null;
    private boolean openerResponse = false;
    private boolean priorityStop = false;



    public ProductionManager(Game game, Player player, MapInfo mapInfo, GameState gameState) {
        this.game = game;
        this.player = player;
        this.mapInfo = mapInfo;
        this.gameState = gameState;

        productionQueue = gameState.getProductionQueue();
        unitTypeCount = gameState.getUnitTypeCount();
        productionBuildings = gameState.getProductionBuildings();
        allBuildings = gameState.getAllBuildings();
        buildTiles = gameState.getBuildTiles();
        startingOpener = gameState.getStartingOpener();
        bunkerPosition = gameState.getBunkerPosition();

        tilePositionValidator = new TilePositionValidator(game);
        unitProduction = new UnitProduction(gameState, game);

        initialize();
    }

    public void initialize() {
        initUnitCounts();
    }

    private void production() {
        boolean hasHighPriorityBuilding = hasHigherPriorityBuilding();
        boolean blockedByHigherPriority = false;
        Workers worker = null;

        for (PlannedItem pi : new PriorityQueue<>(productionQueue)) {
            if (pi.getPriority() == 1 && pi.getPlannedItemStatus() == PlannedItemStatus.NOT_STARTED && pi.getPlannedItemType() == PlannedItemType.BUILDING && meetsRequirements(pi.getUnitType()) && pi.getSupply() <= player.supplyUsed() / 2) {
                priorityStop = true;
            }

            //Override stop if floating too much
            if (gameState.getResourceTracking().getAvailableMinerals() >= 500) {
                priorityStop = false;
                blockedByHigherPriority = false;
                hasHighPriorityBuilding = false;
            }

            if (blockedByHigherPriority && pi.getPlannedItemStatus() == PlannedItemStatus.NOT_STARTED) {
                continue;
            }

            //Throttle gas units but allow mineral only if enough is banked (Vessels very gas heavy and only unit worth prioritizing)
            if (pi.getPriority() == 1 && pi.getPlannedItemStatus() == PlannedItemStatus.NOT_STARTED
                    && pi.getPlannedItemType() == PlannedItemType.UNIT && meetsRequirements(pi.getUnitType())
                    && pi.getSupply() > player.supplyUsed() / 2) {
                if (gameState.getResourceTracking().getAvailableMinerals() >= pi.getUnitType().mineralPrice() && gameState.getResourceTracking().getAvailableGas() < pi.getUnitType().gasPrice()) {
                    priorityStop = true;
                }
            }

            if (gameState.isEnemyInNatural() && (pi.getBuildPosition() != null && !mapInfo.getBaseTiles().contains(pi.getBuildPosition()))) {
                priorityStop = false;
                hasHighPriorityBuilding = false;
                continue;
            }

            if (priorityStop && pi.getPriority() != 1 && (pi.getPlannedItemType() == PlannedItemType.BUILDING || pi.getPlannedItemType() == PlannedItemType.ADDON)
                    && pi.getPlannedItemStatus() == PlannedItemStatus.NOT_STARTED) {
                continue;
            }

            if (hasHighPriorityBuilding && pi.getPlannedItemType() == PlannedItemType.UNIT
                    && pi.getPriority() != 1
                    && pi.getPlannedItemStatus() == PlannedItemStatus.NOT_STARTED) {
                continue;
            }

            switch (pi.getPlannedItemStatus()) {
                case NOT_STARTED:
                    if (pi.getSupply() > player.supplyUsed() / 2) {
                        continue;
                    }

                    if (pi.getPlannedItemType() == PlannedItemType.UPGRADE) {
                        if (pi.getUpgradeType() != null && game.self().getUpgradeLevel(pi.getUpgradeType()) >= pi.getUpgradeLevel()) {
                            pi.setPlannedItemStatus(PlannedItemStatus.COMPLETE);
                            continue;
                        }

                        if (pi.getTechUpgrade() != null && game.self().hasResearched(pi.getTechUpgrade())) {
                            pi.setPlannedItemStatus(PlannedItemStatus.COMPLETE);
                            continue;
                        }

                        if (!canBeResearched(pi.getTechBuilding()) || !researchBuildingAvailable(pi.getTechBuilding())) {
                            continue;
                        }

                        if (pi.getTechUpgrade() != null) {
                            if (gameState.getResourceTracking().getAvailableMinerals() < pi.getTechUpgrade().mineralPrice() || gameState.getResourceTracking().getAvailableGas() < pi.getTechUpgrade().gasPrice()) {
                                blockedByHigherPriority = true;
                                continue;
                            }

                            if (game.self().hasResearched(pi.getTechUpgrade())) {
                                pi.setPlannedItemStatus(PlannedItemStatus.COMPLETE);
                                continue;
                            }

                            researchTech(pi.getTechUpgrade());
                            pi.setPlannedItemStatus(PlannedItemStatus.IN_PROGRESS);
                        }
                        else if (pi.getUpgradeType() != null) {
                            if (!canUpgrade(pi.getUpgradeType())) {
                                continue;
                            }

                            if (gameState.getResourceTracking().getAvailableMinerals() < pi.getUpgradeType().mineralPrice()
                                    || gameState.getResourceTracking().getAvailableGas() < pi.getUpgradeType().gasPrice()) {
                                blockedByHigherPriority = true;
                                continue;
                            }

                            if (game.self().getUpgradeLevel(pi.getUpgradeType()) == pi.getUpgradeLevel()) {
                                pi.setPlannedItemStatus(PlannedItemStatus.COMPLETE);
                                continue;
                            }

                            if (game.self().getUpgradeLevel(pi.getUpgradeType()) != pi.getUpgradeLevel() - 1) {
                                continue;
                            }

                            if (canUpgrade(pi.getUpgradeType())) {
                                researchUpgrade(pi.getUpgradeType());
                                pi.setPlannedItemStatus(PlannedItemStatus.IN_PROGRESS);
                            }
                        }
                        continue;
                    }

                    if ((gameState.getResourceTracking().getAvailableMinerals() < pi.getUnitType().mineralPrice() || gameState.getResourceTracking().getAvailableGas() < pi.getUnitType().gasPrice())
                        && meetsRequirements(pi.getUnitType()) && pi.getPlannedItemType() != PlannedItemType.ADDON) {
                        blockedByHigherPriority = true;
                        continue;
                    }

                    if (pi.getPlannedItemType() == PlannedItemType.UNIT) {
                        for (Unit productionBuilding : productionBuildings) {
                            if (productionBuilding.canTrain(pi.getUnitType()) && !productionBuilding.isTraining()) {
                                productionBuilding.train(pi.getUnitType());
                                pi.setProductionBuilding(productionBuilding);
                                pi.setPlannedItemStatus(PlannedItemStatus.IN_PROGRESS);
                                break;
                            }
                        }
                    }
                    else if (pi.getPlannedItemType() == PlannedItemType.BUILDING) {
                        if (!meetsRequirements(pi.getUnitType())) {
                            priorityStop = false;
                            blockedByHigherPriority = false;
                        }

                        if (pi.getBuildPosition() == null) {
                            if (pi.getUnitType() == UnitType.Terran_Refinery) {
                                setRefineryPosition(pi);
                            }
                            else if (pi.getUnitType() == UnitType.Terran_Command_Center) {
                                setCommandCenterPosition(pi);
                            }
                            else {
                                setBuildingPosition(pi);
                            }

                            //Skip over if out of tiles
                            if (pi.getBuildPosition() == null) {
                                hasHighPriorityBuilding = false;
                                continue;
                            }
                        }

                        if (pi.getBuildPosition() != null && pi.getAssignedBuilder() == null) {
                            worker = ClosestUnit.findClosestWorker(pi.getBuildPosition().toPosition(), gameState.getWorkers(), mapInfo.getPathFinding());
                            pi.setAssignedBuilder(worker);
                        }

                        if (pi.getAssignedBuilder() != null) {
                            worker = pi.getAssignedBuilder();

                            if (worker.getWorkerStatus() == WorkerStatus.MINERALS && worker.getUnit().canBuild(pi.getUnitType())) {
                                worker.build(pi, gameState.getResourceTracking());
                            }
                            else if (worker.getWorkerStatus() != WorkerStatus.MINERALS) {
                                pi.setAssignedBuilder(null);
                                continue;
                            }
                        }
                    }
                    else if (pi.getPlannedItemType() == PlannedItemType.ADDON) {
                        for (Unit productionBuilding : productionBuildings) {
                            if (productionBuilding.canBuildAddon(pi.getUnitType()) && !productionBuilding.isTraining() && productionBuilding.getAddon() == null) {
                                if (productionQueue.stream().anyMatch(plannedItem -> plannedItem.getAddOnParent() == productionBuilding)) {
                                    continue;
                                }

                                if (buildTiles.isAddonPositionBlocked(productionBuilding.getTilePosition())) {
                                    continue;
                                }

                                productionBuilding.buildAddon(pi.getUnitType());
                                pi.setAddOnParent(productionBuilding);
                                pi.setPlannedItemStatus(PlannedItemStatus.IN_PROGRESS);
                                break;
                            }
                        }
                    }

                    //check requirements again in case tiles run out before building starts
                    if ((pi.getPriority() == 1 && pi.getPlannedItemStatus() != PlannedItemStatus.NOT_STARTED) || (pi.getPlannedItemType() == PlannedItemType.BUILDING && !meetsRequirements(pi.getUnitType()))) {
                        priorityStop = false;
                    }

                    break;

                case SCV_ASSIGNED:
                    worker = pi.getAssignedBuilder();
                    if (worker == pi.getAssignedBuilder() && worker.getWorkerStatus() == WorkerStatus.MOVING_TO_BUILD) {
                        if (worker.getUnit().getDistance(pi.getBuildPosition().toPosition()) < 224) {
                            if (pi.getUnitType() == UnitType.Terran_Command_Center
                                    && worker.getIdleClock() > 24) {
                                gameState.scanPosition(pi.getBuildPosition().toPosition());
                                worker.setWorkerStatus(WorkerStatus.CLEARINGMINE);
                                worker.setIdleClock(0);
                            }
                            else {
                                worker.getUnit().build(pi.getUnitType(), pi.getBuildPosition());
                            }
                        }

                        //TODO: ignore if building CC (or other long distance builds)
                        if (worker.getBuildFrameCount() > 420 && mapInfo.getStartingBase().getCenter().getDistance(pi.getBuildPosition().toPosition()) < 1000
                                && worker.getWorkerStatus() == WorkerStatus.MOVING_TO_BUILD) {
                            worker.buildReset(pi, gameState.getResourceTracking());
                        }

                        if (mapInfo.getNaturalBase().getLocation().getDistance(pi.getBuildPosition()) < 10) {
                            mapInfo.setNaturalOwned(true);
                        }
                    }

                    if (worker.getWorkerStatus() == WorkerStatus.MINERALS) {
                        worker.buildReset(pi, gameState.getResourceTracking());
                    }

                    if (worker.getWorkerStatus() == WorkerStatus.STUCK || worker.getWorkerStatus() == WorkerStatus.REPAIRING || worker.getWorkerStatus() == WorkerStatus.DEFEND) {
                        gameState.getResourceTracking().unreserveResources(pi.getUnitType());
                        pi.setPlannedItemStatus(PlannedItemStatus.NOT_STARTED);
                        pi.setAssignedBuilder(null);

                        if (mapInfo.getNaturalBase().getLocation().getDistance(pi.getBuildPosition()) < 10) {
                            mapInfo.setNaturalOwned(false);
                        }
                    }

                    if (!worker.getUnit().exists()) {
                        gameState.getResourceTracking().unreserveResources(pi.getUnitType());
                        pi.setPlannedItemStatus(PlannedItemStatus.NOT_STARTED);
                        pi.setAssignedBuilder(null);

                        if (mapInfo.getNaturalBase().getLocation().getDistance(pi.getBuildPosition()) < 10) {
                            mapInfo.setNaturalOwned(false);
                        }
                    }

                    if (buildingInProduction(pi.getBuildPosition(), pi.getUnitType())) {
                        gameState.getResourceTracking().unreserveResources(pi.getUnitType());
                        pi.setPlannedItemStatus(PlannedItemStatus.IN_PROGRESS);

                        if (pi.getAssignedBuilder() != null && worker.getUnitID() == pi.getAssignedBuilder().getUnitID()) {
                            worker.setWorkerStatus(WorkerStatus.BUILDING);
                        }
                    }
                    break;

                case IN_PROGRESS:
                    if (pi.getPlannedItemType() == PlannedItemType.BUILDING) {
                        if (pi.getAssignedBuilder() != null) {
                            worker = pi.getAssignedBuilder();
                        }

                        for (Unit building : allBuildings) {
                            if (building.getType() == pi.getUnitType() && building.getTilePosition().equals(pi.getBuildPosition()) && building.isCompleted()) {
                                pi.setPlannedItemStatus(PlannedItemStatus.COMPLETE);

                                if (worker.getWorkerStatus() == WorkerStatus.BUILDING) {
                                    worker.setBuildingPosition(null);
                                    worker.setWorkerStatus(WorkerStatus.IDLE);
                                }

                                break;
                            }
                        }

                        boolean builderHasDied = true;
                        for (Workers workers : gameState.getWorkers()) {
                            if (workers == pi.getAssignedBuilder()) {
                                builderHasDied = false;
                                break;
                            }
                        }

                        if (builderHasDied) {
                            for (Workers newWorker : gameState.getWorkers()) {
                                if (newWorker.getWorkerStatus() == WorkerStatus.MINERALS) {
                                    pi.setAssignedBuilder(newWorker);
                                    newWorker.setWorkerStatus(WorkerStatus.MOVING_TO_BUILD);
                                    break;
                                }
                            }
                        }

                        for (Unit building : player.getUnits()) {
                            if (!building.isCompleted() && building.getType() == pi.getUnitType() && !building.isBeingConstructed()) {
                                pi.getAssignedBuilder().getUnit().rightClick(building);
                                break;
                            }

                            if (building.isBeingConstructed() && pi.getAssignedBuilder().getUnit().isConstructing()) {
                                if (worker.getUnit().getID() == pi.getAssignedBuilder().getUnitID()) {
                                    worker.setWorkerStatus(WorkerStatus.BUILDING);
                                    break;
                                }

                            }
                        }
                    }

                    if (pi.getPlannedItemType() == PlannedItemType.UPGRADE) {
                        if (pi.getUpgradeType() != null) {
                            if (game.self().getUpgradeLevel(pi.getUpgradeType()) == pi.getUpgradeLevel()) {
                                pi.setPlannedItemStatus(PlannedItemStatus.COMPLETE);
                            }

                            if (!isUpgrading(pi.getUpgradeType()) && game.self().getUpgradeLevel(pi.getUpgradeType()) < pi.getUpgradeLevel()) {
                                pi.setPlannedItemStatus(PlannedItemStatus.NOT_STARTED);
                            }
                        }
                        else if (pi.getTechUpgrade() != null) {
                            if (game.self().hasResearched(pi.getTechUpgrade())) {
                                pi.setPlannedItemStatus(PlannedItemStatus.COMPLETE);
                            }

                            if (!researchBuildingAvailable(pi.getTechUpgrade()) && !game.self().hasResearched(pi.getTechUpgrade())) {
                                pi.setPlannedItemStatus(PlannedItemStatus.NOT_STARTED);
                            }
                        }
                    }

                    if (pi.getPlannedItemType() == PlannedItemType.ADDON) {
                        if (pi.getAddOnParent() == null || pi.getAddOnParent().getAddon() == null) {
                            pi.setResetCounter(pi.getResetCounter() + 1);

                            if (pi.getResetCounter() > 64) {
                                pi.setResetCounter(0);
                                pi.setPlannedItemStatus(PlannedItemStatus.NOT_STARTED);
                            }
                            continue;
                        }

                        if (pi.getAddOnParent() != null && pi.getAddOnParent().getAddon().isCompleted()) {
                            pi.setPlannedItemStatus(PlannedItemStatus.COMPLETE);
                            break;
                        }
                    }

                    for (Unit productionBuilding : productionBuildings) {
                        if (pi.getProductionBuilding() == productionBuilding && !productionBuilding.isTraining()) {
                            pi.setPlannedItemStatus(PlannedItemStatus.COMPLETE);
                            break;
                        }
                    }
                    break;

                case COMPLETE:
                    break;
            }
        }

        priorityStop = false;
        productionQueue.removeIf(pi -> pi.getPlannedItemStatus() == PlannedItemStatus.COMPLETE);
    }

    private void addToQueue(UnitType unitType, PlannedItemType plannedItemType, int priority) {
        productionQueue.add(new PlannedItem(unitType, 0, plannedItemType, priority));
    }

    private void addToQueue(UnitType unitType, int supply, PlannedItemType plannedItemType, int priority) {
        productionQueue.add(new PlannedItem(unitType, supply, plannedItemType, priority));
    }

    private void addToQueue(UnitType unitType, PlannedItemType plannedItemType,  TilePosition buildPosition, int priority) {
        productionQueue.add(new PlannedItem(unitType, 0, plannedItemType, buildPosition, priority));
    }

    private void addToQueue(UnitType unitType, PlannedItemType plannedItemType, int priority, boolean needsAddon) {
        productionQueue.add(new PlannedItem(unitType, plannedItemType, priority , needsAddon));
    }

    private void addAddOn(UnitType unitType, int priority) {
        productionQueue.add(new PlannedItem(unitType, 0, PlannedItemType.ADDON, priority));
    }

    private boolean hasHigherPriorityBuilding() {
        for (PlannedItem pi : productionQueue) {
            if ((pi.getPlannedItemType() == PlannedItemType.BUILDING) && pi.getPlannedItemStatus() == PlannedItemStatus.NOT_STARTED && pi.getSupply() <= player.supplyUsed() / 2 && meetsRequirements(pi.getUnitType()) && pi.getPriority() < 3) {
                if (gameState.isEnemyInNatural() && pi.getUnitType() == UnitType.Terran_Command_Center) {
                    continue;
                }

                return true;
            }
            else if (pi.getPlannedItemType() == PlannedItemType.UPGRADE && pi.getPlannedItemStatus() == PlannedItemStatus.NOT_STARTED && pi.getSupply() <= player.supplyUsed() / 2 && researchBuildingAvailable(pi.getTechBuilding())) {
                if (pi.getUpgradeType() != null && canUpgrade(pi.getUpgradeType())) {
                    return true;
                }
                else if (pi.getTechUpgrade() != null) {
                    return true;
                }
            }
        }
        return false;
    }

    //Unplanned depot additions to the queue
    private void addSupplyDepot() {
        int usedSupply = game.self().supplyUsed() / 2;
        int totalSupply = game.self().supplyTotal() / 2;
        int freeSupply = totalSupply - usedSupply;

        if (!isDepotInQueue()) {

            if (totalSupply >= 200) {
                return;
            }

            if (freeSupply <= 4 && buildTiles.getMediumBuildTiles().size() >= 2) {
                addToQueue(UnitType.Terran_Supply_Depot, PlannedItemType.BUILDING, 1);
                addToQueue(UnitType.Terran_Supply_Depot, PlannedItemType.BUILDING, 1);
            }

            if (freeSupply <= 4 && buildTiles.getMediumBuildTiles().size() == 1) {
                addToQueue(UnitType.Terran_Supply_Depot, PlannedItemType.BUILDING, 1);
            }
        }
        else if (freeSupply < 2 && productionQueue.stream()
                .anyMatch(pi -> pi.getUnitType() != null && pi.getUnitType() == UnitType.Terran_Command_Center && pi.getSupply() >= usedSupply)
                && productionQueue.stream().noneMatch(pi -> pi.getUnitType() != null && pi.getUnitType() == UnitType.Terran_Supply_Depot && pi.getSupply() == 0)
                && productionQueue.stream().noneMatch(pi -> pi.getUnitType() != null && pi.getUnitType() == UnitType.Terran_Supply_Depot
                        && (pi.getPlannedItemStatus() == PlannedItemStatus.SCV_ASSIGNED || pi.getPlannedItemStatus() == PlannedItemStatus.IN_PROGRESS))
                && gameState.isEnemyInNatural()) {
            addToQueue(UnitType.Terran_Supply_Depot, PlannedItemType.BUILDING, 1);
        }
    }

    private void addProductionBuilding(UnitType unitType, int priority) {
        if (buildTiles.getLargeBuildTiles().isEmpty() && buildTiles.getLargeBuildTilesNoGap().isEmpty()) {
            return;
        }

        int currentlyBuilding = (int) productionQueue.stream().filter(pi -> pi.getUnitType() != null && pi.getUnitType().tileHeight() == 3 && pi.getUnitType().tileWidth() == 4 && (pi.getPlannedItemStatus() == PlannedItemStatus.SCV_ASSIGNED || pi.getPlannedItemStatus() == PlannedItemStatus.IN_PROGRESS)).count();

        if (buildTiles.getLargeBuildTiles().size() + buildTiles.getLargeBuildTilesNoGap().size() == currentlyBuilding) {
            return;
        }

        addToQueue(unitType, PlannedItemType.BUILDING, priority);
    }

    private void addExpansion() {
        if (!gameState.getCanExpand()) {
            return;
        }

        boolean ccAlreadyQueued = productionQueue.stream()
                .anyMatch(pi -> pi.getUnitType() == UnitType.Terran_Command_Center
                        && pi.getPlannedItemStatus() != PlannedItemStatus.COMPLETE);

        if (!ccAlreadyQueued) {
            addToQueue(UnitType.Terran_Command_Center, PlannedItemType.BUILDING, 4);
        }

        gameState.setCanExpand(false);
    }

    private void addCCTurret(Unit unit) {
        TilePosition turretPosition = null;
        Base newBase = null;

        if (unit.getTilePosition().equals(buildTiles.getMainBaseCCTile())) {
            return;
        }

        for (Base base : mapInfo.getMapBases()) {
            if (base.getLocation().getDistance(unit.getTilePosition()) < 10) {
                newBase = base;
                break;
            }
        }

        if (newBase != null) {
            if (newBase == mapInfo.getStartingBase()) {
                return;
            }
            else if (newBase == mapInfo.getNaturalBase() && !tileTaken(buildTiles.getNaturalChokeTurret())) {
                turretPosition = buildTiles.getNaturalChokeTurret();
            }
            else {
                turretPosition = buildTiles.getMineralLineTurrets().get(newBase);
            }
        }

        if (turretPosition != null) {
            addToQueue(UnitType.Terran_Missile_Turret, PlannedItemType.BUILDING, turretPosition, 4);
        }
    }

    private void scvProduction() {
        int ownedBases = mapInfo.getOwnedBases().size();
        int workerCap = 24 * ownedBases;

        //temp fix
        if (gameState.getEnemyOpener() != null) {
            if (gameState.getEnemyOpener().getStrategyName().equals("Gas Steal") && !gameState.moveOutConditionsMet()) {
                workerCap = 12;
            }
        }

        if (unitTypeCount.get(UnitType.Terran_SCV) >= workerCap) {
            return;
        }

        long notStartedSCVs = productionQueue.stream()
                .filter(pi -> pi.getUnitType() == UnitType.Terran_SCV
                        && pi.getPlannedItemStatus() == PlannedItemStatus.NOT_STARTED)
                .count();

        long idleCCs = productionBuildings.stream()
                .filter(b -> b.getType() == UnitType.Terran_Command_Center && !b.isTraining())
                .count();

        long scvsToQueue = idleCCs - notStartedSCVs;

        for (long i = 0; i < scvsToQueue; i++) {
            if (ownedBases == 1) {
                if (unitTypeCount.get(UnitType.Terran_SCV) < 16 && new Time(game.getFrameCount()).greaterThan(new Time(5, 0))
                        || unitTypeCount.get(UnitType.Terran_SCV) < 24 && new Time(game.getFrameCount()).greaterThan(new Time(7, 0))) {
                    addToQueue(UnitType.Terran_SCV, PlannedItemType.UNIT, 2);
                }
                else {
                    addToQueue(UnitType.Terran_SCV, PlannedItemType.UNIT, 3);
                }
            }
            else {
                if (unitTypeCount.get(UnitType.Terran_SCV) < 48) {
                    addToQueue(UnitType.Terran_SCV, PlannedItemType.UNIT, 2);
                }
                else {
                    addToQueue(UnitType.Terran_SCV, PlannedItemType.UNIT, 4);
                }
            }
        }
    }

    private boolean isDepotInQueue() {
        if (buildTiles.getMediumBuildTiles().isEmpty()) {
            return true;
        }

        for (PlannedItem pi : productionQueue) {
            if (pi.getUnitType() == UnitType.Terran_Supply_Depot) {
                return true;
            }

//            if (pi.getUnitType() == UnitType.Terran_Command_Center) {
//                return true;
//            }
        }
        return false;
    }

    private void setRefineryPosition(PlannedItem pi) {
        for (Base base : mapInfo.getOwnedBases()) {
            if (!mapInfo.getGeyserTiles().containsKey(base)) {
                continue;
            }

            if (base.getGeysers().isEmpty()) {
                continue;
            }

            if (mapInfo.getUsedGeysers().contains(mapInfo.getGeyserTiles().get(base))) {
                continue;
            }

            mapInfo.getUsedGeysers().add(mapInfo.getGeyserTiles().get(base));
            pi.setBuildPosition(mapInfo.getGeyserTiles().get(base));
        }
    }

    private void setBuildingPosition(PlannedItem pi) {
        TilePosition cloestBuildTile = null;
        int distanceFromSCV = Integer.MAX_VALUE;

        if (pi.getUnitType().tileHeight() == 3 && pi.getUnitType().tileWidth() == 4) {
            if (buildTiles.getLargeBuildTiles().isEmpty() && buildTiles.getLargeBuildTilesNoGap().isEmpty() && pi.getUnitType().canBuildAddon()) {
                return;
            }

            if ((pi.getUnitType().canBuildAddon() && pi.needsAddon()) || buildTiles.getLargeBuildTilesNoGap().isEmpty()) {
                for (TilePosition tilePosition : buildTiles.getLargeBuildTiles()) {
                    if (tileTaken(tilePosition)) {
                        continue;
                    }

                    int distance = tilePosition.getApproxDistance(mapInfo.getStartingBase().getLocation());

                    if (distance < distanceFromSCV) {
                        distanceFromSCV = distance;
                        cloestBuildTile = tilePosition;
                    }
                }

                //Use no gap tiles if no other large tiles are available
                if (cloestBuildTile == null) {
                    for (TilePosition tilePosition : buildTiles.getLargeBuildTilesNoGap()) {
                        if (tileTaken(tilePosition)) {
                            continue;
                        }

                        int distance = tilePosition.getApproxDistance(mapInfo.getStartingBase().getLocation());

                        if (distance < distanceFromSCV) {
                            distanceFromSCV = distance;
                            cloestBuildTile = tilePosition;
                        }
                    }
                }
            }
            else {
                for (TilePosition tilePosition : buildTiles.getLargeBuildTilesNoGap()) {
                    if (tileTaken(tilePosition)) {
                        continue;
                    }

                    int distance = tilePosition.getApproxDistance(mapInfo.getStartingBase().getLocation());

                    if (distance < distanceFromSCV) {
                        distanceFromSCV = distance;
                        cloestBuildTile = tilePosition;
                    }
                }
            }
            pi.setBuildPosition(cloestBuildTile);

        }
        else if (pi.getUnitType().tileHeight() == 2 && pi.getUnitType().tileWidth() == 3) {
            if (buildTiles.getMediumBuildTiles().isEmpty()) {
                return;
            }

            if (pi.getUnitType() == UnitType.Terran_Bunker) {
                pi.setBuildPosition(setBunkerPosition());
                return;
            }

            for (TilePosition tilePosition : buildTiles.getMediumBuildTiles()) {
                if (tileTaken(tilePosition)) {
                    continue;
                }

                int distance = tilePosition.getApproxDistance(mapInfo.getStartingBase().getLocation());

                if (distance < distanceFromSCV) {
                    distanceFromSCV = distance;
                    cloestBuildTile = tilePosition;
                }
            }
            pi.setBuildPosition(cloestBuildTile);
        }
        else {
            if ((mapInfo.isNaturalOwned() || mapInfo.hasBunkerInNatural() || (gameState.getBunkerPosition() != null && mapInfo.getNaturalTiles().contains(gameState.getBunkerPosition()))) && buildTiles.getNaturalChokeTurret() != null && !reservedTurretPositions.contains(buildTiles.getNaturalChokeTurret())) {
                reservedTurretPositions.add(buildTiles.getNaturalChokeTurret());
                pi.setBuildPosition(buildTiles.getNaturalChokeTurret());
            }
            else if (buildTiles.getMainChokeTurret() != null && !reservedTurretPositions.contains(buildTiles.getMainChokeTurret())) {
                reservedTurretPositions.add(buildTiles.getMainChokeTurret());
                pi.setBuildPosition(buildTiles.getMainChokeTurret());
            }
            else {
                pi.setPlannedItemStatus(PlannedItemStatus.COMPLETE);
            }
        }
    }

    private void setCommandCenterPosition(PlannedItem pi) {
        if (!mapInfo.getOrderedExpansions().isEmpty() && mapInfo.getOrderedExpansions().get(0) == mapInfo.getNaturalBase()) {
            Base natural = mapInfo.getNaturalBase();

            if (gameState.isEnemyInNatural() && !mapInfo.hasBunkerInNatural()) {
                pi.setBuildPosition(buildTiles.getMainBaseCCTile());
                mapInfo.getOrderedExpansions().remove(natural);
                return;
            }

            if (gameState.getEnemyOpener() != null && !mapInfo.hasBunkerInNatural()) {
                boolean naturalBunkerInQueue = productionQueue.stream()
                        .anyMatch(queued -> queued.getUnitType() == UnitType.Terran_Bunker
                                && queued.getBuildPosition() != null
                                && buildTiles.getNaturalChokeBunker() != null
                                && buildTiles.getNaturalChokeBunker().equals(queued.getBuildPosition()));

                if (!naturalBunkerInQueue && !gameState.getEnemyOpener().removeBuildings().contains(UnitType.Terran_Bunker)) {
                    pi.setBuildPosition(buildTiles.getMainBaseCCTile());
                    mapInfo.getOrderedExpansions().remove(natural);
                    return;
                }
            }

            if (!tilePositionValidator.isBuildable(natural.getLocation(), UnitType.Terran_Command_Center)) {
                return;
            }
            pi.setBuildPosition(natural.getLocation());
            mapInfo.getOrderedExpansions().remove(natural);
            return;
        }

        Base best = mapInfo.scoredBestExpansion(startingOpener.buildType(), gameState.getKnownEnemyUnits());
        if (best == null) {
            return;
        }
        if (!tilePositionValidator.isBuildable(best.getLocation(), UnitType.Terran_Command_Center)) {
            return;
        }
        pi.setBuildPosition(best.getLocation());
    }

    private TilePosition setBunkerPosition() {
        if (buildTiles.getCloseBunkerTile() == null) {
            return null;
        }

        if (gameState.getEnemyOpener() != null) {
            switch (gameState.getEnemyOpener().getStrategyName()) {
                case "Cannon Rush":
                case "Four Rax":
                case "SCV Rush":
                case "Two Gate":    
                    return buildTiles.getMainChokeBunker();
                case "Four Pool":
                    return buildTiles.getCloseBunkerTile();
            }
        }

        if (bunkerPosition != null) {
           return bunkerPosition;
        }

        return null;
    }

    private void openerResponse() {
        openerResponse = true;

        Map<UnitType, Integer> buildingCounts = new HashMap<>();
        for (UnitType building : gameState.getEnemyOpener().getBuildingResponse()) {
            if (building.isBuilding()) {
                buildingCounts.merge(building, 1, Integer::sum);
            }
        }

        if (startingOpener.buildType() == BuildType.MECH) {
            boolean factoryActiveOrComplete = unitTypeCount.get(UnitType.Terran_Factory) > 0 ||
                productionQueue.stream().anyMatch(pi -> pi.getUnitType() == UnitType.Terran_Factory &&
                    (pi.getPlannedItemStatus() == PlannedItemStatus.SCV_ASSIGNED ||
                     pi.getPlannedItemStatus() == PlannedItemStatus.IN_PROGRESS));

            productionQueue.removeIf(pi -> pi.getPlannedItemStatus() == PlannedItemStatus.NOT_STARTED &&
                (pi.getUnitType() != null && pi.getUnitType().isBuilding()) &&
                buildingCounts.containsKey(pi.getUnitType()) &&
                pi.getUnitType() != UnitType.Terran_Factory);

            if (!factoryActiveOrComplete && buildingCounts.containsKey(UnitType.Terran_Factory)) {
                productionQueue.stream()
                    .filter(pi -> pi.getUnitType() == UnitType.Terran_Factory &&
                        pi.getPlannedItemStatus() == PlannedItemStatus.NOT_STARTED)
                    .min(Comparator.comparingInt(PlannedItem::getSupply))
                    .ifPresent(productionQueue::remove);
            }
        }
        else {
            productionQueue.removeIf(pi -> pi.getUnitType() != null && pi.getUnitType().isBuilding() && buildingCounts.containsKey(pi.getUnitType()) && pi.getPlannedItemStatus() == PlannedItemStatus.NOT_STARTED); }

        for (UnitType buildingType : gameState.getEnemyOpener().removeBuildings()) {
            List<PlannedItem> queueSnapshot = new ArrayList<>(productionQueue);
            Optional<PlannedItem> toRemove = queueSnapshot.stream()
                    .filter(pi -> pi.getUnitType() == buildingType
                            && pi.getPlannedItemStatus() == PlannedItemStatus.NOT_STARTED)
                    .max(Comparator.comparingInt(PlannedItem::getSupply));

            toRemove.ifPresent(productionQueue::remove);
        }

        for (UnitType building : gameState.getEnemyOpener().getBuildingResponse()) {
            boolean alreadyInProgress = productionQueue.stream().anyMatch(pi -> pi.getUnitType() == building && pi.getPlannedItemStatus() != PlannedItemStatus.NOT_STARTED);

            if ((unitTypeCount.get(building) == 0 || building == UnitType.Terran_Command_Center) && !alreadyInProgress && building.isBuilding()) {
                if (building.isAddon()) {
                    addToQueue(building, PlannedItemType.ADDON, 1);
                }
                else {
                    if (building == UnitType.Terran_Missile_Turret) {
                        if ((mapInfo.isNaturalOwned() || mapInfo.hasBunkerInNatural() || (gameState.getBunkerPosition() != null && mapInfo.getNaturalTiles().contains(gameState.getBunkerPosition()))) && buildTiles.getNaturalChokeTurret() != null
                                && !reservedTurretPositions.contains(buildTiles.getNaturalChokeTurret())) {
                            reservedTurretPositions.add(buildTiles.getNaturalChokeTurret());
                            addToQueue(building, PlannedItemType.BUILDING, buildTiles.getNaturalChokeTurret(),1);
                        }
                        else if (buildTiles.getMainChokeTurret() != null && !hasTurretAtBase(buildTiles.getMainChokeTurret()) && !hasPositionInQueue(buildTiles.getMainChokeTurret())) {
                            reservedTurretPositions.add(buildTiles.getMainChokeTurret());
                            addToQueue(building, PlannedItemType.BUILDING, buildTiles.getMainChokeTurret(),1);
                        }
                        else {
                            for (TilePosition turretTile : gameState.getBuildTiles().getMainTurrets()) {
                                if (turretTile != null && !hasTurretAtBase(turretTile) && !hasPositionInQueue(turretTile) && !tileTaken(turretTile)) {
                                    addToQueue(UnitType.Terran_Missile_Turret, PlannedItemType.BUILDING, turretTile, 3);
                                    break;
                                }
                            }
                        }

                    }
                    else {
                        if (building.canBuildAddon()) {
                            addToQueue(building, PlannedItemType.BUILDING, 1, true);
                        }
                        else {
                            addToQueue(building, PlannedItemType.BUILDING, 1);
                        }
                    }
                }
            }
            // TODO: add separate list for units
            else if (!building.isBuilding()) {
                addToQueue(building, PlannedItemType.UNIT, 1);
            }
        }

        if (!gameState.getEnemyOpener().additionalBuildings().isEmpty()) {
            for (UnitType building : gameState.getEnemyOpener().additionalBuildings()) {
                if (building.canProduce()) {
                    addToQueue(building, PlannedItemType.BUILDING, 1);
                }
                else {
                    addToQueue(building, 20, PlannedItemType.BUILDING, 2);
                }

            }
        }

        if (!gameState.getEnemyOpener().getUnitResponse().isEmpty()) {
            productionQueue.removeIf(pi -> gameState.getEnemyOpener().getUnitResponse().contains(pi.getUnitType()));
        }

        for (UpgradeType upgrade : gameState.getEnemyOpener().getUpgradeResponse()) {
            boolean existingUpgrade = false;
            PlannedItem existingItem = null;
            UnitType researchBuilding = null;
            int upgradeLevel = 1;

            for (PlannedItem pi : productionQueue) {
                if (pi.getUpgradeType() != null && upgrade != null) {
                    if (pi.getUpgradeType() == upgrade) {
                        researchBuilding = pi.getTechBuilding();
                        upgradeLevel = pi.getUpgradeLevel();
                        existingItem = pi;
                        existingUpgrade = true;
                        break;
                    }
                }
            }

            if (existingUpgrade) {
                if (existingItem.getPlannedItemStatus() == PlannedItemStatus.NOT_STARTED) {
                    productionQueue.removeIf(pi -> pi.getUpgradeType() != null && pi.getUpgradeType() == upgrade);
                    productionQueue.add(new PlannedItem(upgrade, 0, PlannedItemType.UPGRADE,  researchBuilding, upgradeLevel,1));
                }
            }
            else {
                if (game.self().getUpgradeLevel(upgrade) < upgradeLevel) {
                    productionQueue.add(new PlannedItem(upgrade, 0, PlannedItemType.UPGRADE, researchBuilding, upgradeLevel,1));
                }
            }
        }

        for (TechType tech : gameState.getEnemyOpener().getTechUpgradeResponse()) {
            boolean existingUpgrade = false;
            PlannedItem existingItem = null;

            for (PlannedItem pi : productionQueue) {
                if (pi.getTechUpgrade() != null && tech != null) {
                    if (pi.getTechUpgrade() == tech) {
                        existingItem = pi;
                        existingUpgrade = true;
                        break;
                    }
                }
            }

            if (existingUpgrade) {
                if (existingItem.getPlannedItemStatus() == PlannedItemStatus.NOT_STARTED) {
                    productionQueue.removeIf(pi -> pi.getTechUpgrade() != null && pi.getTechUpgrade() == tech);
                    productionQueue.add(new PlannedItem(tech, 0, PlannedItemType.UPGRADE, UnitType.Terran_Machine_Shop, 1));
                }
            }
            else {
                if (!game.self().hasResearched(tech)) {
                    productionQueue.add(new PlannedItem(tech, 0, PlannedItemType.UPGRADE, UnitType.Terran_Machine_Shop, 1));
                }
            }
        }
    }

    private void enemyTechUnitResponse() {
        if (gameState.getKnownEnemyTechUnits().isEmpty()) {
            return;
        }

        for (EnemyTechUnits techUnit : gameState.getKnownEnemyTechUnits()) {
            if (techUnit.getFriendlyBuildingResponse().isEmpty()) {
                continue;
            }

            //Remove buildings already owned and already highest priority in queue
            techUnit.getFriendlyBuildingResponse().removeIf(
                    buildingResponse -> allBuildings.stream().anyMatch(unit -> unit.getType() == buildingResponse));

            techUnit.getFriendlyBuildingResponse().removeIf(buildingPriority ->
                    productionQueue.stream().anyMatch(pi -> pi.getUnitType() == buildingPriority
                            && (pi.getPriority() == 1 || pi.getPlannedItemStatus() != PlannedItemStatus.NOT_STARTED)));

            for (UnitType buildingResponse : techUnit.getFriendlyBuildingResponse()) {
                productionQueue.removeIf(pi -> pi.getUnitType() == buildingResponse && pi.getPriority() != 1);

                if (buildingResponse.isAddon()) {
                    addToQueue(buildingResponse, PlannedItemType.ADDON, 1);
                }
                else {
                    if (buildingResponse.canBuildAddon()) {
                        addToQueue(buildingResponse, PlannedItemType.BUILDING, 1, true);
                    }
                    else {
                        addToQueue(buildingResponse, PlannedItemType.BUILDING, 1);
                    }
                }


            }

            //Spam turrets if flyers are detected
            if (techUnit.isFlyer()) {
                for (Base base : mapInfo.getOwnedBases()) {
                    TilePosition turretTile = buildTiles.getMineralLineTurrets().get(base);
                    if (turretTile != null && !hasTurretAtBase(turretTile) && !hasPositionInQueue(turretTile) && !tileTaken(turretTile)) {
                        addToQueue(UnitType.Terran_Missile_Turret, PlannedItemType.BUILDING, turretTile,2);
                    }
                }

                if (!techUnit.mineralLineTurretsOnly()) {
                    int mainTurretsBuilt = (int) buildTiles.getMainTurrets().stream().filter(t -> hasTurretAtBase(t)).count();
                    int mainTurretsInQueue = (int) buildTiles.getMainTurrets().stream().filter(t -> hasPositionInQueue(t)).count();
                    int mainTurretsToAdd = buildTiles.getMainTurrets().size() - mainTurretsBuilt - mainTurretsInQueue;
                    int mainTurretsAdded = 0;

                    for (TilePosition turretTile : buildTiles.getMainTurrets()) {
                        if (mainTurretsAdded >= mainTurretsToAdd) {
                            break;
                        }

                        if (turretTile != null && !hasTurretAtBase(turretTile) && !hasPositionInQueue(turretTile)
                                && !tileTaken(turretTile)) {
                            addToQueue(UnitType.Terran_Missile_Turret, PlannedItemType.BUILDING, turretTile, 3);
                            mainTurretsAdded++;
                        }
                    }

                    TilePosition mainChokeTurret = buildTiles.getMainChokeTurret();
                    TilePosition naturalChokeTurret = buildTiles.getNaturalChokeTurret();

                    if (mainChokeTurret != null && !hasTurretAtBase(mainChokeTurret) && !hasPositionInQueue(mainChokeTurret) && !tileTaken(mainChokeTurret)) {
                        addToQueue(UnitType.Terran_Missile_Turret, PlannedItemType.BUILDING, mainChokeTurret, 2);
                    }

                    if (naturalChokeTurret != null && !hasTurretAtBase(naturalChokeTurret)
                            && !hasPositionInQueue(naturalChokeTurret) && !tileTaken(naturalChokeTurret)
                            && mapInfo.isNaturalOwned() && !gameState.isEnemyInNatural()) {
                        addToQueue(UnitType.Terran_Missile_Turret, PlannedItemType.BUILDING, naturalChokeTurret, 2);
                    }
                }
            }

            if (techUnit.getFriendlyUpgradeResponse().isEmpty()) {
                continue;
            }

            for (PlannedItem upgradeResponse : techUnit.getFriendlyUpgradeResponse()) {
                boolean existingUpgrade = false;

                for (PlannedItem pi : productionQueue) {
                    if (pi.getTechUpgrade() != null && upgradeResponse.getTechUpgrade() != null) {
                        if (pi.getTechUpgrade() == upgradeResponse.getTechUpgrade()) {
                            existingUpgrade = true;
                            break;
                        }
                    }

                    if (pi.getUpgradeType() != null && upgradeResponse.getUpgradeType() != null) {
                        if (pi.getUpgradeType() == upgradeResponse.getUpgradeType()) {
                            existingUpgrade = true;
                            break;
                        }
                    }
                }

                if (upgradeResponse.getTechUpgrade() != null) {
                    if (game.self().hasResearched(upgradeResponse.getTechUpgrade())) {
                        existingUpgrade = true;
                    }
                }

                if (upgradeResponse.getUpgradeType() != null) {
                    if (game.self().getUpgradeLevel(upgradeResponse.getUpgradeType()) >= upgradeResponse.getUpgradeLevel()) {
                        existingUpgrade = true;
                    }
                }

                if (!existingUpgrade) {
                    productionQueue.add(upgradeResponse);
                }
            }

        }
    }

    private void enemyTechBuildingResponse() {
        if (gameState.getKnownEnemyTechBuildings().isEmpty()) {
            return;
        }

        for (EnemyTechBuilding techBuilding : gameState.getKnownEnemyTechBuildings()) {
            if (techBuilding.getFriendlyBuildingResponse().isEmpty()) {
                continue;
            }

            techBuilding.getFriendlyBuildingResponse().removeIf(buildingResponse -> 
                    allBuildings.stream().anyMatch(unit -> unit.getType() == buildingResponse));

            techBuilding.getFriendlyBuildingResponse().removeIf(buildingPriority ->
                    productionQueue.stream().anyMatch(pi -> pi.getUnitType() == buildingPriority
                    && (pi.getPriority() == 1 || pi.getPlannedItemStatus() != PlannedItemStatus.NOT_STARTED)));

            for (UnitType buildingResponse : techBuilding.getFriendlyBuildingResponse()) {
                productionQueue.removeIf(pi -> pi.getUnitType() == buildingResponse && pi.getPriority() != 1);

                if (buildingResponse.isAddon()) {
                    addToQueue(buildingResponse, PlannedItemType.ADDON, 1);
                }
                else {
                    if (buildingResponse.canBuildAddon()) {
                        addToQueue(buildingResponse, PlannedItemType.BUILDING, 1, true);
                    }
                    else {
                        addToQueue(buildingResponse, PlannedItemType.BUILDING, 1);
                    }
                }
            }
        }
                
    }

    private boolean hasTurretAtBase(TilePosition location) {
        for (Unit unit : game.self().getUnits()) {
            if (unit.getType() == UnitType.Terran_Missile_Turret &&
                    unit.getTilePosition().equals(location)) {
                return true;
            }
        }
        return false;
    }

    //Track number of buildings to check for building requirements
    private void addUnitTypeCount(Unit unit) {
        unitTypeCount.put(unit.getType(), unitTypeCount.get(unit.getType()) + 1);
    }

    private void initUnitCounts()  {
        for (UnitType unitType : UnitType.values()) {
            if (unitType.getRace().toString().equals("Terran") && !unitType.isCritter() && !unitType.isHero() && !unitType.isBeacon() && !unitType.isSpecialBuilding()) {
                unitTypeCount.put(unitType, 0);
            }
        }
    }

    private boolean isUpgrading(UpgradeType upgradeType) {
        for (Unit researchBuilding : allBuildings) {
            if (researchBuilding.getAddon() != null && researchBuilding.getAddon().getUpgrade() == upgradeType) {
                return true;
            }

            if (researchBuilding.getUpgrade() == upgradeType) {
                return true;
            }
        }
        return false;
    }

    private boolean researchBuildingAvailable(TechType techType) {
        for (Unit researchBuilding : allBuildings) {
            if (researchBuilding.getAddon() != null && researchBuilding.getAddon().getTech() == techType) {
                return true;
            }

            if (researchBuilding.getTech() == techType) {
                return true;
            }
        }
        return false;
    }

    private void researchUpgrade(UpgradeType upgradeType) {
        if (gameState.getResourceTracking().getAvailableMinerals() >= upgradeType.mineralPrice() && gameState.getResourceTracking().getAvailableGas() >= upgradeType.gasPrice()) {
            for (Unit researchBuilding : allBuildings) {
                if (researchBuilding.canUpgrade(upgradeType) && !researchBuilding.isUpgrading()) {
                    researchBuilding.upgrade(upgradeType);
                    break;
                }
            }
        }
    }

    private void researchTech(TechType techType) {
        if (gameState.getResourceTracking().getAvailableMinerals() >= techType.mineralPrice() && gameState.getResourceTracking().getAvailableGas() >= techType.gasPrice()) {
            for (Unit researchBuilding : allBuildings) {
                if (researchBuilding.canResearch(techType) && !researchBuilding.isUpgrading()) {
                    researchBuilding.research(techType);
                    break;
                }
            }
        }

    }

    private boolean canUpgrade(UpgradeType upgradeType) {
        for (Unit researchBuilding : allBuildings) {
            if (researchBuilding.canUpgrade(upgradeType) && !researchBuilding.isUpgrading() && !researchBuilding.isResearching()) {
                return true;
            }
        }
        return false;
    }

    private boolean researchBuildingAvailable(UnitType unitType) {
        int availableBuildings = 0;
        for (Unit researchBuilding : allBuildings) {
            if (researchBuilding.getType() == unitType && researchBuilding.isCompleted() && !(researchBuilding.isResearching() || researchBuilding.isUpgrading())) {
                availableBuildings++;
            }
        }
        return availableBuildings > 0;
    }

    private boolean buildingInProduction(TilePosition tilePosition, UnitType unitType) {
        if (tilePosition == null) {
            return false;
        }

        for (Unit building : allBuildings) {
            if (building.getType() == unitType && building.getTilePosition().getX() == tilePosition.getX() && building.getTilePosition().getY() == tilePosition.getY()) {
                return true;
            }
        }
        return false;
    }

    private void resetUnitInProduction(Unit destroyedBuilding) {
        for (PlannedItem pi : productionQueue) {
            if (pi.getPlannedItemStatus() == PlannedItemStatus.IN_PROGRESS && pi.getPlannedItemType() == PlannedItemType.UNIT && pi.getProductionBuilding() == destroyedBuilding) {
                pi.setPlannedItemStatus(PlannedItemStatus.NOT_STARTED);
                pi.setProductionBuilding(null);
            }
        }
    }

    private void removeUnitTypeCount(Unit unit) {
            unitTypeCount.put(unit.getType(), unitTypeCount.get(unit.getType()) - 1);
    }

    private void removeUnitTypeCount(UnitType unit) {
        unitTypeCount.put(unit, unitTypeCount.get(unit) - 1);
    }

    private void removeBuilding(Unit unit) {
        allBuildings.remove(unit);
        productionBuildings.remove(unit);
    }

    private void resetBuilding(Unit unit) {
        for (PlannedItem pi : productionQueue) {
            if (pi.getPlannedItemStatus() == PlannedItemStatus.IN_PROGRESS && pi.getUnitType() == unit.getType()) {
                for (Workers worker : gameState.getWorkers()) {
                    if (!worker.getUnit().exists()) {
                        continue;
                    }

                    if (worker.getUnit() == null || pi.getAssignedBuilder() == null) {
                        continue;
                    }

                    if (worker.getUnit().getID() == pi.getAssignedBuilder().getUnitID()) {
                        worker.setWorkerStatus(WorkerStatus.IDLE);
                        break;
                    }
                }
                pi.setPlannedItemStatus(PlannedItemStatus.NOT_STARTED);
                pi.setAssignedBuilder(null);
                pi.setBuildPosition(null);
                pi.setPriority(1);
                break;
            }
        }
    }

    private boolean meetsRequirements(UnitType unitType) {
        Map<UnitType, Integer> requiredUnits = unitType.requiredUnits();

        if (requiredUnits.isEmpty()) {
            return true;
        }

        if (requiredUnits.size() == 1 && requiredUnits.containsKey(UnitType.Terran_SCV)) {
            return true;
        }

        if (unitType.isBuilding()) {
            if (unitType.tileHeight() == 3 && unitType.tileWidth() == 4 && buildTiles.getLargeBuildTiles().isEmpty()) {
                return false;
            }
            if (unitType.tileHeight() == 2 && unitType.tileWidth() == 3 && buildTiles.getMediumBuildTiles().isEmpty()) {
                return false;
            }

            //Check if refinery exists so gas buildings don't deadlock build
            if (unitType.gasPrice() > 0 && unitTypeCount.get(UnitType.Terran_Refinery) == 0) {
                return false;
            }
        }

        if (unitType.gasPrice() > 0 && unitTypeCount.get(UnitType.Terran_Refinery) == 0) {
            return false;
        }

        for (Map.Entry<UnitType, Integer> requirement : requiredUnits.entrySet()) {
            UnitType requiredUnit = requirement.getKey();
            int requiredCount = requirement.getValue();

            if (!requiredUnit.isBuilding()) {
                continue;
            }

            if (unitTypeCount.get(requiredUnit) < requiredCount) {
                return false;
            }
        }

        return true;
    }

    private boolean tileTaken(TilePosition tilePosition) {
        for (PlannedItem pi : productionQueue) {
            if (pi.getBuildPosition() == null) {
                continue;
            }

            if (pi.getBuildPosition() == tilePosition) {
                return true;
            }
        }
        return false;
    }

    private boolean canBeResearched(UnitType unitType) {
        return unitTypeCount.get(unitType) > 0;
    }

    private boolean hasPositionInQueue(TilePosition tilePosition) {
        return productionQueue.stream().anyMatch(pi -> pi.getBuildPosition() != null && pi.getBuildPosition().equals(tilePosition) && pi.getPlannedItemStatus() != PlannedItemStatus.COMPLETE);
    }

    public void onFrame() {
        scvProduction();
        unitProduction.onFrame();
        production();
        addSupplyDepot();

        addExpansion();

        if (gameState.getEnemyOpener() != null && !openerResponse) {
            openerResponse();
        }

        enemyTechUnitResponse();
        enemyTechBuildingResponse();
        buildTiles.onFrame();
    }

    public void onUnitCreate(Unit unit) {
        if (unit.getType().isBuilding()) {
            allBuildings.add(unit);
        }

        if (unit.getType() == UnitType.Terran_Command_Center) {
            addCCTurret(unit);
            addToQueue(UnitType.Terran_Comsat_Station, PlannedItemType.ADDON, 2);

            if (mapInfo.getNaturalBase().getLocation().getDistance(unit.getTilePosition()) < 10
                    && !unit.getTilePosition().equals(buildTiles.getMainBaseCCTile())
                    && !mapInfo.hasBunkerInNatural()
                    && (gameState.getStartingOpener().buildType() == BuildType.BIO || gameState.getEnemyOpener() != null)) {
                addToQueue(UnitType.Terran_Bunker, PlannedItemType.BUILDING, buildTiles.getNaturalChokeBunker(), 3);
            }
        }
    }

    public void onUnitComplete(Unit unit) {
        addUnitTypeCount(unit);

        if (unit.getType() == UnitType.Terran_Command_Center
                && productionQueue.stream()
                .noneMatch(pi -> pi.getUnitType() == UnitType.Terran_Refinery
                        && pi.getPlannedItemStatus() == PlannedItemStatus.NOT_STARTED)) {
            addToQueue(UnitType.Terran_Refinery, PlannedItemType.BUILDING, 3);
        }

        if (unit.canTrain() || unit.getType() == UnitType.Terran_Science_Facility) {
            productionBuildings.add(unit);
        }
        buildTiles.onUnitComplete(unit);
    }

    public void onUnitDestroy(Unit unit) {
        removeUnitTypeCount(unit);
        removeBuilding(unit);

        if (unit.getType().isBuilding()) {
            if (unit.getType() == UnitType.Terran_Refinery) {
                return;
            }

            if (unit.getType() == UnitType.Terran_Bunker && gameState.getStartingOpener().buildType() != BuildType.BIO) {
                return;
            }

            if (unit.getType() == UnitType.Terran_Missile_Turret) {
                reservedTurretPositions.remove(unit.getTilePosition());
                addToQueue(UnitType.Terran_Missile_Turret, PlannedItemType.BUILDING, unit.getTilePosition(), 3);

                if (!unit.isCompleted()) {
                    resetBuilding(unit);
                }

                return;
            }

            //Readd everything as P1 except CCs after the natural
            if (!unit.isCompleted()) {
                resetBuilding(unit);
            }
            else if (unit.getType().isAddon()){
                addToQueue(unit.getType(), PlannedItemType.ADDON, 2);
            }
            else if (unit.getType() == UnitType.Terran_Command_Center && mapInfo.isNaturalOwned()) {
                addToQueue(unit.getType(), PlannedItemType.BUILDING, 4);
            }
            else {
                if (mapInfo.getBaseTiles().contains(unit.getTilePosition())) {
                    addToQueue(unit.getType(), PlannedItemType.BUILDING, 1);
                }
                else {
                    addToQueue(unit.getType(), PlannedItemType.BUILDING, 5);
                }
            }

            resetUnitInProduction(unit);

            if (unit.getType().tileHeight() == 3 && unit.getType().tileWidth() == 4) {
                if (unit.getType() == UnitType.Terran_Command_Center) {
                    mapInfo.readdExpansion(unit);
                    return;
                }

                buildTiles.getLargeBuildTiles().add(unit.getTilePosition());
            }
            else if (unit.getType().tileHeight() == 2 && unit.getType().tileWidth() == 3) {
                if (unit.getType() == UnitType.Terran_Bunker) {
                    return;
                }

                buildTiles.getMediumBuildTiles().add(unit.getTilePosition());
            }
        }

    }

    public void onUnitMorph(Unit unit) {
        if (unit.getType().isBuilding()) {
            allBuildings.add(unit);
        }

        if (unit.getType() == UnitType.Terran_Siege_Tank_Siege_Mode) {
            removeUnitTypeCount(UnitType.Terran_Siege_Tank_Tank_Mode);
        }
        else if (unit.getType() == UnitType.Terran_Siege_Tank_Tank_Mode) {
            removeUnitTypeCount(UnitType.Terran_Siege_Tank_Siege_Mode);
        }
    }

}