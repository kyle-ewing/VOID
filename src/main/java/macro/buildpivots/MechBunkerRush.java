package macro.buildpivots;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import bwapi.Race;
import bwapi.TechType;
import bwapi.Unit;
import bwapi.UnitType;
import bwapi.UpgradeType;
import information.enemy.EnemyUnits;
import information.enemy.enemyopeners.EnemyStrategyName;
import macro.buildorders.BuildType;
import macro.buildorders.BunkerLocation;
import planner.PlannedItem;
import planner.PlannedItemType;
import util.Time;

public class MechBunkerRush extends BuildPivot {
    private boolean gasThrottleLifted = false;

    public MechBunkerRush() {
        rushActive = true;
    }

    public BuildPivotName getBuildPivotName() {
        return BuildPivotName.MECHBUNKERRUSH;
    }

    public ArrayList<PlannedItem> getPivotBuild() {
        ArrayList<PlannedItem> buildOrder = new ArrayList<>();
        buildOrder.add(new PlannedItem(UnitType.Terran_Bunker, 0, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 9, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Barracks, 11, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Refinery, 12, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 16, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Factory, 19, PlannedItemType.BUILDING, 1, true));
        buildOrder.add(new PlannedItem(UnitType.Terran_Command_Center, 23, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 24, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Engineering_Bay, 25, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Machine_Shop, 19, PlannedItemType.ADDON, 3));
        buildOrder.add(new PlannedItem(UnitType.Terran_Armory, 27, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Factory, 29, PlannedItemType.BUILDING, 2, true));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 36, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Machine_Shop, 32, PlannedItemType.ADDON, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Academy, 35, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 44, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Refinery, 52, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(TechType.Tank_Siege_Mode, 29, PlannedItemType.UPGRADE, UnitType.Terran_Machine_Shop, 3));
        buildOrder.add(new PlannedItem(UpgradeType.Charon_Boosters, 60, PlannedItemType.UPGRADE, UnitType.Terran_Factory, 1, 3));
        buildOrder.add(new PlannedItem(UpgradeType.Terran_Vehicle_Weapons, 46, PlannedItemType.UPGRADE, UnitType.Terran_Armory, 1, 3));
        buildOrder.add(new PlannedItem(UpgradeType.Terran_Vehicle_Plating, 46, PlannedItemType.UPGRADE, UnitType.Terran_Armory, 1, 6));
        return buildOrder;
    }

    public HashMap<UnitType, Integer> getMoveOutCondition(Time time, HashSet<EnemyUnits> enemyUnits, float enemySupplyLost) {
        HashMap<UnitType, Integer> moveOutCondition = new HashMap<>();

        if (time.lessThanOrEqual(new Time(5, 0)) && rushActive) {
            moveOutCondition.put(UnitType.Terran_Marine, 1);
        }
        else if (naturalDepotKilled && !naturalDepotPushOver) {
            moveOutCondition.put(UnitType.Terran_Marine, 1);
        }
        else if (enemyUnits.stream().filter(type -> type.getEnemyType() == UnitType.Terran_Starport).count() > 1 && time.lessThanOrEqual(new Time(6, 0))) {
            moveOutCondition.put(UnitType.Terran_Goliath, 4);
            moveOutCondition.put(UnitType.Terran_Siege_Tank_Tank_Mode, 2);
        }
        else if (time.lessThanOrEqual(new Time(8, 0))) {
            moveOutCondition.put(UnitType.Terran_Goliath, 8);
            moveOutCondition.put(UnitType.Terran_Siege_Tank_Tank_Mode, 4);
        }
        else {
            moveOutCondition.put(UnitType.Terran_Goliath, 12);
            moveOutCondition.put(UnitType.Terran_Siege_Tank_Tank_Mode, 6);
        }

        return moveOutCondition;
    }

    public BunkerLocation getBunkerLocation() {
        return BunkerLocation.ENEMY_NATURAL;
    }

    public BuildType buildType() {
        return BuildType.MECH;
    }

    @Override
    public boolean pivotsFrom(EnemyStrategyName enemyStrategy, Race enemyRace) {
        return enemyStrategy == EnemyStrategyName.CCFIRST && enemyRace == Race.Terran;
    }

    @Override
    public ArrayList<UnitType> proxyBuildings() {
        ArrayList<UnitType> proxyBuildings = new ArrayList<>();
        proxyBuildings.add(UnitType.Terran_Bunker);
        return proxyBuildings;
    }

    @Override
    public HashSet<UnitType> getLiftableBuildings() {
        HashSet<UnitType> liftableBuildings = new HashSet<>();
        liftableBuildings.add(UnitType.Terran_Engineering_Bay);
        liftableBuildings.add(UnitType.Terran_Barracks);
        return liftableBuildings;
    }

    @Override
    public HashMap<UnitType, Integer> getRequiredBuildings() {
        HashMap<UnitType, Integer> requiredBuildings = new HashMap<>();
        requiredBuildings.put(UnitType.Terran_Barracks, 1);
        requiredBuildings.put(UnitType.Terran_Supply_Depot, 2);
        requiredBuildings.put(UnitType.Terran_Refinery, 1);
        requiredBuildings.put(UnitType.Terran_Factory, 1);
        return requiredBuildings;
    }

    @Override
    public int getGasWorkerTarget(int totalGasGathered, HashSet<Unit> allBuildings) {
        if (gasThrottleLifted) {
            return 3;
        }

        boolean factoryComplete = allBuildings.stream()
            .anyMatch(b -> b.getType() == UnitType.Terran_Factory && b.isCompleted());

        if (factoryComplete) {
            gasThrottleLifted = true;
            return 3;
        }

        if (totalGasGathered >= 100) {
            return 1;
        }

        return 3;
    }

    @Override
    public HashSet<UnitType> getRushUnitTypes() {
        HashSet<UnitType> rushUnitTypes = new HashSet<>();
        rushUnitTypes.add(UnitType.Terran_Marine);
        return rushUnitTypes;
    }
}
