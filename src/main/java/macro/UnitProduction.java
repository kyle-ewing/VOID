package macro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import bwapi.Game;
import bwapi.Unit;
import bwapi.UnitType;
import information.GameState;
import information.enemy.EnemyUnits;
import information.enemy.enemyarmycomposition.EnemyArmyCompManager;
import information.enemy.enemyarmycomposition.EnemyArmyCompResponse;
import information.enemy.enemyopeners.EnemyStrategy;
import macro.buildorders.BuildOrder;
import macro.buildorders.BuildOrderName;
import macro.buildorders.MechBuildOrder;
import map.BuildTiles;
import planner.PlannedItem;
import planner.PlannedItemStatus;
import planner.PlannedItemType;

public class UnitProduction {
    private final GameState gameState;
    private final Game game;
    private final BuildOrder buildOrder;
    private final HashSet<Unit> productionBuildings;
    private final HashSet<UnitType> techUnitResponses;
    private final HashMap<UnitType, Integer> unitTypeCount;
    private final BuildTiles buildTiles;
    private final PriorityQueue<PlannedItem> productionQueue;
    private final EnemyArmyCompManager armyCompositionManager;
    private EnemyStrategy enemyOpener = null;

    private static final Set<UnitType> TANK_TRIGGERS = new HashSet<>(Arrays.asList(
            UnitType.Protoss_Reaver,
            UnitType.Zerg_Hydralisk,
            UnitType.Zerg_Lurker,
            UnitType.Protoss_Dragoon,
            UnitType.Zerg_Ultralisk,
            UnitType.Terran_Siege_Tank_Tank_Mode,
            UnitType.Terran_Siege_Tank_Siege_Mode
    ));


    public UnitProduction(GameState gameState, Game game) {
        this.gameState = gameState;
        this.game = game;
        this.buildOrder = gameState.getStartingOpener();
        this.productionBuildings = gameState.getProductionBuildings();
        this.techUnitResponses = gameState.getTechUnitResponse();
        this.unitTypeCount = gameState.getUnitTypeCount();
        this.buildTiles = gameState.getBuildTiles();
        this.productionQueue = gameState.getProductionQueue();
        this.armyCompositionManager = gameState.getArmyCompositionManager();
    }

    public void onFrame() {
        if (enemyOpener == null && gameState.getEnemyOpener() != null) {
            enemyOpener = gameState.getEnemyOpener();
        }

        List<PlannedItem> items;
        switch (buildOrder.buildType()) {
            case BIO:  items = getBioUnits();  break;
            case MECH: items = getMechUnits(); break;
            default:   return;
        }
        productionQueue.addAll(items);
    }

    // --- BIO ---

    private List<PlannedItem> getBioUnits() {
        List<PlannedItem> items = new ArrayList<>();

        for (Unit building : productionBuildings) {
            for (UnitType unitType : techUnitResponses) {
                if (!canBuild(building, unitType.whatBuilds().getKey())) continue;
                if (!isRecruitable(unitType, 1) || hasInQueue(unitType)) continue;

                if (unitType == UnitType.Terran_Science_Vessel) {
                    if (unitTypeCount.get(unitType) < 1) {
                        items.add(plannedUnit(unitType, 1));
                    } 
                    else if (unitTypeCount.get(unitType) < 5) {
                        items.add(plannedUnit(unitType, 2));
                    }
                } 
                else {
                    items.add(plannedUnit(unitType, 2));
                }
            }

            if (canBuild(building, UnitType.Terran_Barracks)) {
                int marineCount = unitTypeCount.get(UnitType.Terran_Marine);
                int medicCap = marineCount > 20 ? 10 : 4;

                if (building.canTrain(UnitType.Terran_Medic)
                        && isRecruitable(UnitType.Terran_Medic)
                        && unitTypeCount.get(UnitType.Terran_Medic) < medicCap
                        && !hasInQueue(UnitType.Terran_Medic)
                        && marineCount > 8) {
                    items.add(plannedUnit(UnitType.Terran_Medic, 2));
                } 
                else if (isRecruitable(UnitType.Terran_Marine) && !hasInQueue(UnitType.Terran_Marine)) {
                    int priority = (enemyOpener != null
                            && enemyOpener.getStrategyName().equals("Gas Steal")) ? 2 : 3;
                    items.add(plannedUnit(UnitType.Terran_Marine, priority));
                }
            }

            if (canBuild(building, UnitType.Terran_Factory)) {
                items.addAll(getBioTankItems());
            }

            items.addAll(getCompositionResponseUnits(building));
        }

        return items;
    }

    private List<PlannedItem> getBioTankItems() {
        int tankCount = unitTypeCount.get(UnitType.Terran_Siege_Tank_Tank_Mode)
                + unitTypeCount.get(UnitType.Terran_Siege_Tank_Siege_Mode);
        if ((!shouldBuildTanks() && !isLurkerOpener())
                || !isRecruitable(UnitType.Terran_Siege_Tank_Tank_Mode)
                || tankCount >= 6
                || hasInQueue(UnitType.Terran_Siege_Tank_Tank_Mode)) {
            return Collections.emptyList();
        }

        if (gameState.hasTransitioned()) {
            return Collections.singletonList(plannedUnit(UnitType.Terran_Siege_Tank_Tank_Mode, 3));
        }
        if (gameState.getTechUnitResponse().contains(UnitType.Zerg_Lurker)) {
            return Collections.singletonList(plannedUnit(UnitType.Terran_Siege_Tank_Tank_Mode, tankCount < 2 ? 1 : 2));
        }
        if (enemyOpener != null) {
            switch (enemyOpener.getStrategyName()) {
                case "One Base Lurker":
                case "Two Base Lurker":
                    return Collections.singletonList(plannedUnit(UnitType.Terran_Siege_Tank_Tank_Mode, tankCount < 2 ? 1 : 2));
                default:
                    return Collections.singletonList(plannedUnit(UnitType.Terran_Siege_Tank_Tank_Mode, 2));
            }
        }
        return Collections.singletonList(plannedUnit(UnitType.Terran_Siege_Tank_Tank_Mode, 2));
    }

    private boolean shouldBuildTanks() {
        for (EnemyUnits enemy : gameState.getKnownEnemyUnits()) {
            if (TANK_TRIGGERS.contains(enemy.getEnemyType())) return true;
        }

        return false;
    }

    // --- MECH ---

    private List<PlannedItem> getMechUnits() {
        List<PlannedItem> items = new ArrayList<>();
        int tankCount = unitTypeCount.get(UnitType.Terran_Siege_Tank_Tank_Mode) + unitTypeCount.get(UnitType.Terran_Siege_Tank_Siege_Mode);
        int mechCount = unitTypeCount.get(UnitType.Terran_Vulture) + unitTypeCount.get(UnitType.Terran_Goliath);
        int vesselCount = unitTypeCount.get(UnitType.Terran_Science_Vessel);
        boolean ratioOverMaximum = tankCount > 0 && mechCount >= tankCount * 3;
        int factoryCap = buildOrder.getBuildOrderName() == BuildOrderName.TWOFAC ? 4 : 5;
        boolean addonFreeFactoryAvailable = productionBuildings.stream()
                .anyMatch(b -> b.getType() == UnitType.Terran_Factory && b.getAddon() == null && !b.isTraining());

        for (Unit building : productionBuildings) {
            for (UnitType unitType : techUnitResponses) {
                if (canBuild(building, unitType.whatBuilds().getKey())
                        && isRecruitable(unitType)
                        && !hasInQueue(unitType)) {

                    if (unitType == UnitType.Terran_Goliath && ratioOverMaximum) {
                        continue;
                    }

                    if (unitType == UnitType.Terran_Science_Vessel) {
                        if (vesselCount == 0) {
                            items.add(plannedUnit(unitType, 1));
                        }
                        else if (vesselCount < 5 && vesselCount < tankCount / 4) {
                            items.add(plannedUnit(unitType, 2));
                        }
                        continue;
                    }

                    items.add(plannedUnit(unitType, 2));
                }
            }

            if (enemyOpener != null
                    && !enemyOpener.getUnitResponse().isEmpty()
                    && !enemyOpener.isStrategyDefended()) {
                for (UnitType unitType : enemyOpener.getUnitResponse()) {
                    if (canBuild(building, unitType.whatBuilds().getKey())
                            && isRecruitable(unitType)
                            && !hasInQueue(unitType)) {

                        if (unitType == UnitType.Terran_Science_Vessel) {
                            if (vesselCount == 0) {
                                items.add(plannedUnit(unitType, 1));
                            }
                            else if (vesselCount < 5 && vesselCount < tankCount / 3) {
                                items.add(plannedUnit(unitType, 2));
                            }
                            continue;
                        }

                        items.add(plannedUnit(unitType, unitTypeCount.get(unitType) < 8 ? 1 : 3));
                    }
                }
            }

            if (canBuild(building, UnitType.Terran_Factory)) {
                boolean firstTankPriority = tankCount == 0
                        && buildOrder instanceof MechBuildOrder
                        && ((MechBuildOrder) buildOrder).prioritizeTankFirst();

                if (isRecruitable(UnitType.Terran_Siege_Tank_Tank_Mode)
                        && building.getAddon() != null
                        && !hasInQueue(UnitType.Terran_Siege_Tank_Tank_Mode)
                        && tankCount < 12
                        && (firstTankPriority || mechCount >= tankCount * 2 || ratioOverMaximum)) {
                    if (firstTankPriority) {
                        items.add(plannedUnit(UnitType.Terran_Siege_Tank_Tank_Mode, 1));
                    }
                    else {
                        items.add(plannedUnit(UnitType.Terran_Siege_Tank_Tank_Mode, 2));
                    }
                }
                else if (!firstTankPriority && (!ratioOverMaximum && !addonFreeFactoryAvailable || building.getAddon() == null)) {
                    if (!ratioOverMaximum && buildOrder.getBuildOrderName() == BuildOrderName.GOLIATHFE) {
                        if (isRecruitable(UnitType.Terran_Goliath) && !hasInQueue(UnitType.Terran_Goliath)) {
                            items.add(plannedUnit(UnitType.Terran_Goliath, 3));
                        }
                    }
                    else if (!ratioOverMaximum) {
                        if (isRecruitable(UnitType.Terran_Vulture) && !hasInQueue(UnitType.Terran_Vulture)) {
                            items.add(plannedUnit(UnitType.Terran_Vulture, 3));
                        }
                    }
                }
            }

            if (canBuild(building, UnitType.Terran_Starport)
                    && building.getAddon() != null
                    && isRecruitable(UnitType.Terran_Battlecruiser)
                    && !hasInQueue(UnitType.Terran_Battlecruiser)
                    && unitTypeCount.get(UnitType.Terran_Battlecruiser) < 5) {
                items.add(plannedUnit(UnitType.Terran_Battlecruiser, 2));
            }

            items.addAll(getCompositionResponseUnits(building));
        }

        if (gameState.getResourceTracking().getAvailableMinerals() > 500
                && unitTypeCount.get(UnitType.Terran_Factory) < factoryCap
                && !hasInQueue(UnitType.Terran_Factory)) {
            PlannedItem extra = productionBuilding(UnitType.Terran_Factory, 3);
            if (extra != null) items.add(extra);
        }

        return items;
    }

    private PlannedItem productionBuilding(UnitType unitType, int priority) {
        if (buildTiles.getLargeBuildTiles().isEmpty() && buildTiles.getLargeBuildTilesNoGap().isEmpty()) {
            return null;
        }

        long currentlyBuilding = 0;
        for (PlannedItem pi : productionQueue) {
            if (pi.getUnitType() != null
                    && pi.getUnitType().tileHeight() == 3
                    && pi.getUnitType().tileWidth() == 4
                    && (pi.getPlannedItemStatus() == PlannedItemStatus.SCV_ASSIGNED
                        || pi.getPlannedItemStatus() == PlannedItemStatus.IN_PROGRESS)) {
                currentlyBuilding++;
            }
        }

        int availableTiles = buildTiles.getLargeBuildTiles().size() + buildTiles.getLargeBuildTilesNoGap().size();
        if (availableTiles == currentlyBuilding) {
            return null;
        }

        return plannedBuilding(unitType, priority);
    }

    private List<PlannedItem> getCompositionResponseUnits(Unit building) {
        List<PlannedItem> items = new ArrayList<>();
        for (EnemyArmyCompResponse response : armyCompositionManager.getTriggeredResponses(gameState.getKnownEnemyUnits())) {
            UnitType responseUnit = response.getResponseUnitType();
            
            if (!canBuild(building, responseUnit.whatBuilds().getKey())) {
                continue;
            }

            if (!building.canTrain(responseUnit)) {
                continue;
            }

            int enemyAmount = response.countMatchingUnits(gameState.getKnownEnemyUnits());
            int desiredCount = response.getDesiredResponseCount(enemyAmount);
            
            if (unitTypeCount.getOrDefault(responseUnit, 0) >= desiredCount) {
                continue;
            }
            
            if (!isRecruitable(responseUnit) || hasInQueue(responseUnit)) {
                continue;
            }
            items.add(plannedUnit(responseUnit, response.getPriority(enemyAmount)));
        }
        return items;
    }

    private PlannedItem plannedUnit(UnitType unitType, int priority) {
        return new PlannedItem(unitType, 0, PlannedItemType.UNIT, priority);
    }

    private PlannedItem plannedBuilding(UnitType unitType, int priority) {
        return new PlannedItem(unitType, 0, PlannedItemType.BUILDING, priority);
    }

    private boolean canBuild(Unit unit, UnitType unitType) {
        return unit.getType() == unitType && !unit.isTraining();
    }

    private boolean hasInQueue(UnitType unitType) {
        for (PlannedItem pi : productionQueue) {
            if (pi.getUnitType() == unitType && pi.getPlannedItemStatus() == PlannedItemStatus.NOT_STARTED) {
                return true;
            }
        }
        return false;
    }

    private boolean isRecruitable(UnitType unitType) {
        int freeSupply = (game.self().supplyTotal() - game.self().supplyUsed()) / 2;
        return gameState.getResourceTracking().getAvailableMinerals() >= unitType.mineralPrice()
                && gameState.getResourceTracking().getAvailableGas() >= unitType.gasPrice()
                && freeSupply >= unitType.supplyRequired() / 2;
    }

    private boolean isRecruitable(UnitType unitType, int priority) {
        int freeSupply = (game.self().supplyTotal() - game.self().supplyUsed()) / 2;
        if (gameState.getResourceTracking().getAvailableMinerals() >= unitType.mineralPrice()
                && gameState.getResourceTracking().getAvailableGas() >= unitType.gasPrice()
                && freeSupply >= unitType.supplyRequired() / 2) {
            return true;
        }
        return priority == 1
                && freeSupply >= unitType.supplyRequired() / 2
                && gameState.getResourceTracking().getAvailableGas() <= unitType.gasPrice();
    }

    private boolean isLurkerOpener() {
        return enemyOpener != null &&
                (enemyOpener.getStrategyName().equals("One Base Lurker") ||
                        enemyOpener.getStrategyName().equals("Two Base Lurker"));
    }
}
