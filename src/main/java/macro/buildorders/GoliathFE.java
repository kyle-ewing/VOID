package macro.buildorders;

import bwapi.TechType;
import bwapi.UnitType;
import bwapi.UpgradeType;
import information.enemy.EnemyUnits;
import planner.PlannedItem;
import planner.PlannedItemStatus;
import planner.PlannedItemType;
import util.Time;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class GoliathFE extends BuildOrder {

    public BuildOrderName getBuildOrderName() {
        return BuildOrderName.GOLIATHFE;
    }
    public ArrayList<PlannedItem> getBuildOrder() {
        ArrayList<PlannedItem> buildOrder = new ArrayList<>();
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 9, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Barracks, 11, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Refinery, 12, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Marine,15, PlannedItemStatus.NOT_STARTED, PlannedItemType.UNIT, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 16, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Factory, 17, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Marine,17, PlannedItemStatus.NOT_STARTED, PlannedItemType.UNIT, 3));
        buildOrder.add(new PlannedItem(UnitType.Terran_Marine,18, PlannedItemStatus.NOT_STARTED, PlannedItemType.UNIT, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Marine,18, PlannedItemStatus.NOT_STARTED, PlannedItemType.UNIT, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Bunker, 17, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Vulture,22, PlannedItemStatus.NOT_STARTED, PlannedItemType.UNIT, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Command_Center, 24, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 24, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Engineering_Bay, 25, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Machine_Shop, 25, PlannedItemStatus.NOT_STARTED, PlannedItemType.ADDON, 3));
        buildOrder.add(new PlannedItem(UnitType.Terran_Armory, 27, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Factory, 29, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 36, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Machine_Shop, 32, PlannedItemStatus.NOT_STARTED, PlannedItemType.ADDON, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Academy, 42, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 44, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Refinery, 52, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Factory, 50, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 3));
        buildOrder.add(new PlannedItem(UnitType.Terran_Factory, 55, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 3));
        buildOrder.add(new PlannedItem(UnitType.Terran_Factory, 55, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 3));
        buildOrder.add(new PlannedItem(TechType.Tank_Siege_Mode, 30, PlannedItemStatus.NOT_STARTED, PlannedItemType.UPGRADE, UnitType.Terran_Machine_Shop, 2));
        buildOrder.add(new PlannedItem(UpgradeType.Charon_Boosters, 42, PlannedItemStatus.NOT_STARTED, PlannedItemType.UPGRADE, UnitType.Terran_Factory, 1, 3));
        buildOrder.add(new PlannedItem(UpgradeType.Terran_Vehicle_Weapons, 44, PlannedItemStatus.NOT_STARTED, PlannedItemType.UPGRADE, UnitType.Terran_Armory, 1, 3));
        buildOrder.add(new PlannedItem(UpgradeType.Terran_Vehicle_Plating, 44, PlannedItemStatus.NOT_STARTED, PlannedItemType.UPGRADE, UnitType.Terran_Armory, 1, 6));
        buildOrder.add(new PlannedItem(TechType.Spider_Mines, 44, PlannedItemStatus.NOT_STARTED, PlannedItemType.UPGRADE, UnitType.Terran_Machine_Shop, 4));
        buildOrder.add(new PlannedItem(UpgradeType.Ion_Thrusters, 44, PlannedItemStatus.NOT_STARTED, PlannedItemType.UPGRADE, UnitType.Terran_Machine_Shop, 1,5));
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

    public HashMap<UnitType, Integer> getMoveOutCondition(Time time, HashSet<EnemyUnits> enemyUnits) {
        HashMap<UnitType, Integer> moveOutCondition = new HashMap<>();

        if(enemyUnits.stream().filter(type -> type.getEnemyType() == UnitType.Terran_Barracks).count() > 1) {
            moveOutCondition.put(UnitType.Terran_Goliath, 7);
            moveOutCondition.put(UnitType.Terran_Siege_Tank_Tank_Mode, 4);
        }
        else if(enemyUnits.stream().filter(type -> type.getEnemyType() == UnitType.Terran_Starport).count() > 1 && time.lessThanOrEqual(new Time(6,0))) {
            moveOutCondition.put(UnitType.Terran_Goliath, 4);
            moveOutCondition.put(UnitType.Terran_Siege_Tank_Tank_Mode, 2);
        }
        else if(time.lessThanOrEqual(new Time(8,0))) {
            moveOutCondition.put(UnitType.Terran_Goliath, 3);
            moveOutCondition.put(UnitType.Terran_Siege_Tank_Tank_Mode, 2);
        }
        else {
            moveOutCondition.put(UnitType.Terran_Goliath, 7);
            moveOutCondition.put(UnitType.Terran_Siege_Tank_Tank_Mode, 4);
        }



        return moveOutCondition;
    }

    @Override
    public int getScoutSupply() {
        return 12;
    }
}
