package macro.buildorders.random;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import bwapi.Unit;
import bwapi.UnitType;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildOrderName;
import macro.buildorders.BuildType;
import macro.buildorders.BunkerLocation;
import macro.buildorders.MechBuildOrder;
import planner.PlannedItem;
import planner.PlannedItemType;
import util.Time;

public class RandomStub extends MechBuildOrder {

    public BuildOrderName getBuildOrderName() {
        return BuildOrderName.RANDOMSTUB;
    }

    public ArrayList<PlannedItem> getBuildOrder() {
        ArrayList<PlannedItem> buildOrder = new ArrayList<>();
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 9, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Barracks, 9, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Refinery, 12, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Marine, 14, PlannedItemType.UNIT, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Bunker, 16, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Marine, 16, PlannedItemType.UNIT, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Factory, 17, PlannedItemType.BUILDING, 1, true));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 17, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Marine, 18, PlannedItemType.UNIT, 3));
        buildOrder.add(new PlannedItem(UnitType.Terran_Marine, 18, PlannedItemType.UNIT, 3));
        buildOrder.add(new PlannedItem(UnitType.Terran_Marine, 21, PlannedItemType.UNIT, 3));
        buildOrder.add(new PlannedItem(UnitType.Terran_Command_Center, 23, PlannedItemType.BUILDING, 1));
        return buildOrder;
    }

    public HashSet<UnitType> getLiftableBuildings() {
        liftableBuildings.add(UnitType.Terran_Engineering_Bay);
        liftableBuildings.add(UnitType.Terran_Barracks);
        return liftableBuildings;
    }

    public BunkerLocation getBunkerLocation() {
        return BunkerLocation.NATURAL;
    }

    public BuildType buildType() {
        return BuildType.MECH;
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

    public HashMap<UnitType, Integer> getMoveOutCondition(Time time, HashSet<EnemyUnits> enemyUnits, float enemySupplyLost) {
        HashMap<UnitType, Integer> moveOutCondition = new HashMap<>();
        moveOutCondition.put(UnitType.Terran_Marine, 12);
        return moveOutCondition;
    }

    @Override
    public int getScoutSupply() {
        return 9;
    }

    @Override
    public boolean prioritizeTankFirst() {
        return false;
    }
}
