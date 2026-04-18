package macro.buildorders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import bwapi.TechType;
import bwapi.UnitType;
import bwapi.UpgradeType;
import information.enemy.EnemyUnits;
import planner.PlannedItem;
import planner.PlannedItemType;
import util.Time;

public class TwoFac extends MechBuildOrder {

    public BuildOrderName getBuildOrderName() {
        return BuildOrderName.TWOFAC;
    }
    public ArrayList<PlannedItem> getBuildOrder() {
        ArrayList<PlannedItem> buildOrder = new ArrayList<>();
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 9, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Barracks, 11, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Refinery, 12, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Marine, 15, PlannedItemType.UNIT, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 16, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Marine, 17, PlannedItemType.UNIT, 3));
        buildOrder.add(new PlannedItem(UnitType.Terran_Marine, 18, PlannedItemType.UNIT, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Marine, 18, PlannedItemType.UNIT, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Factory, 18, PlannedItemType.BUILDING, 1, true));
        buildOrder.add(new PlannedItem(UnitType.Terran_Factory, 20, PlannedItemType.BUILDING, 2, true));
        buildOrder.add(new PlannedItem(UnitType.Terran_Machine_Shop, 21, PlannedItemType.ADDON, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Bunker, 15, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 23, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Machine_Shop, 24, PlannedItemType.ADDON, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 30, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(TechType.Spider_Mines, 32, PlannedItemType.UPGRADE, UnitType.Terran_Machine_Shop, 3));
        buildOrder.add(new PlannedItem(UnitType.Terran_Command_Center, 32, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(TechType.Tank_Siege_Mode, 31, PlannedItemType.UPGRADE, UnitType.Terran_Machine_Shop, 2));
        buildOrder.add(new PlannedItem(UpgradeType.Ion_Thrusters, 36, PlannedItemType.UPGRADE, UnitType.Terran_Machine_Shop, 1,3));
        buildOrder.add(new PlannedItem(UnitType.Terran_Engineering_Bay, 36, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 37, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Armory, 45, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Academy, 50, PlannedItemType.BUILDING, 3));
        buildOrder.add(new PlannedItem(UnitType.Terran_Factory, 52, PlannedItemType.BUILDING, 3));
        buildOrder.add(new PlannedItem(UpgradeType.Terran_Vehicle_Weapons, 61, PlannedItemType.UPGRADE, UnitType.Terran_Armory, 1, 3));
        buildOrder.add(new PlannedItem(UnitType.Terran_Missile_Turret, 60, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Comsat_Station, 60, PlannedItemType.ADDON, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Comsat_Station, 60, PlannedItemType.ADDON, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Refinery, 67, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UpgradeType.Terran_Vehicle_Plating, 70,PlannedItemType.UPGRADE, UnitType.Terran_Armory, 1, 4));

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
        moveOutCondition.put(UnitType.Terran_Vulture, 6);
        moveOutCondition.put(UnitType.Terran_Siege_Tank_Tank_Mode, 5);
        return moveOutCondition;
    }
}
