package macro.buildorders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import bwapi.TechType;
import bwapi.Unit;
import bwapi.UnitType;
import bwapi.UpgradeType;
import information.enemy.EnemyUnits;
import planner.PlannedItem;
import planner.PlannedItemType;
import util.Time;

public class OneFacFE extends MechBuildOrder {

    public BuildOrderName getBuildOrderName() {
        return BuildOrderName.ONEFACFE;
    }
    public ArrayList<PlannedItem> getBuildOrder() {
        ArrayList<PlannedItem> buildOrder = new ArrayList<>();
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 9, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Barracks, 10, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Refinery, 12, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Marine, 14, PlannedItemType.UNIT, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 15, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Factory, 16, PlannedItemType.BUILDING, 1, true));
        buildOrder.add(new PlannedItem(UnitType.Terran_Machine_Shop, 16, PlannedItemType.ADDON, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Bunker, 16, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Marine, 18, PlannedItemType.UNIT, 3));
        buildOrder.add(new PlannedItem(UnitType.Terran_Marine, 18, PlannedItemType.UNIT, 3));
        buildOrder.add(new PlannedItem(UnitType.Terran_Marine, 18, PlannedItemType.UNIT, 3));
        buildOrder.add(new PlannedItem(UnitType.Terran_Command_Center, 24, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 24, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Engineering_Bay, 26, PlannedItemType.BUILDING, 3));
        buildOrder.add(new PlannedItem(TechType.Tank_Siege_Mode, 30, PlannedItemType.UPGRADE, UnitType.Terran_Machine_Shop, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Factory, 38, PlannedItemType.BUILDING, 2, true));
        buildOrder.add(new PlannedItem(UnitType.Terran_Machine_Shop, 24, PlannedItemType.ADDON, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 40, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Academy, 42, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Armory, 44, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(TechType.Spider_Mines, 48, PlannedItemType.UPGRADE, UnitType.Terran_Machine_Shop, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Refinery, 51, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UpgradeType.Terran_Vehicle_Weapons, 58, PlannedItemType.UPGRADE, UnitType.Terran_Armory, 1, 3));
        buildOrder.add(new PlannedItem(UnitType.Terran_Comsat_Station, 56, PlannedItemType.ADDON, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Comsat_Station, 56, PlannedItemType.ADDON, 2));
        buildOrder.add(new PlannedItem(UpgradeType.Ion_Thrusters, 56, PlannedItemType.UPGRADE, UnitType.Terran_Machine_Shop, 1, 3));
        buildOrder.add(new PlannedItem(UnitType.Terran_Factory, 62, PlannedItemType.BUILDING, 3));
        buildOrder.add(new PlannedItem(UpgradeType.Terran_Vehicle_Plating, 70, PlannedItemType.UPGRADE, UnitType.Terran_Armory, 1, 4));

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

    public HashMap<UnitType, Integer> getMoveOutCondition(Time time, HashSet<EnemyUnits> enemyUnits) {
        HashMap<UnitType, Integer> moveOutCondition = new HashMap<>();
        moveOutCondition.put(UnitType.Terran_Vulture, 10);
        moveOutCondition.put(UnitType.Terran_Siege_Tank_Tank_Mode, 6);
        return moveOutCondition;
    }
}
