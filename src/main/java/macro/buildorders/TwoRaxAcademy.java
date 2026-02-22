package macro.buildorders;

import bwapi.TechType;
import bwapi.UnitType;
import bwapi.UpgradeType;
import planner.PlannedItem;
import planner.PlannedItemStatus;
import planner.PlannedItemType;
import util.Time;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class TwoRaxAcademy extends BuildOrder {
    public BuildOrderName getBuildOrderName() {
        return BuildOrderName.TWORAXACADEMY;
    }

    public ArrayList<PlannedItem> getBuildOrder() {
        ArrayList<PlannedItem> buildOrder = new ArrayList<>();
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 9, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Barracks, 9, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Barracks, 14, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 14, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Bunker, 16, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Refinery, 18, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Academy, 19, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 23, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(TechType.Stim_Packs, 26, PlannedItemStatus.NOT_STARTED, PlannedItemType.UPGRADE, UnitType.Terran_Academy, 2));
        buildOrder.add(new PlannedItem(UpgradeType.U_238_Shells, 26, PlannedItemStatus.NOT_STARTED, PlannedItemType.UPGRADE, UnitType.Terran_Academy, 1, 3));
        buildOrder.add(new PlannedItem(UnitType.Terran_Engineering_Bay, 28, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 30, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Barracks, 36, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Missile_Turret, 33, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UpgradeType.Terran_Infantry_Weapons, 32, PlannedItemStatus.NOT_STARTED, PlannedItemType.UPGRADE, UnitType.Terran_Engineering_Bay, 1, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Factory, 34, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 38, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Machine_Shop, 0, PlannedItemStatus.NOT_STARTED, PlannedItemType.ADDON, 1));
        buildOrder.add(new PlannedItem(UpgradeType.Terran_Infantry_Armor, 38, PlannedItemStatus.NOT_STARTED, PlannedItemType.UPGRADE, UnitType.Terran_Engineering_Bay, 1, 3));
        buildOrder.add(new PlannedItem(TechType.Tank_Siege_Mode, 0, PlannedItemStatus.NOT_STARTED, PlannedItemType.UPGRADE, UnitType.Terran_Machine_Shop, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Command_Center, 49, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 1));
        return buildOrder;
    }

    public HashSet<UnitType> getLiftableBuildings() {
        return  liftableBuildings;
    }

    public BunkerLocation getBunkerLocation() {
        return BunkerLocation.MAIN;
    }

    public BuildType buildType() {
        return BuildType.BIO;
    }

    public HashMap<UnitType, Integer> getMoveOutCondition(Time time) {
        HashMap<UnitType, Integer> moveOutCondition = new HashMap<>();

        if(time.lessThanOrEqual(new Time(10,0))) {
            moveOutCondition.put(UnitType.Terran_Marine, 10);
            moveOutCondition.put(UnitType.Terran_Medic, 3);
        }
        else {
            moveOutCondition.put(UnitType.Terran_Marine, 12);
            moveOutCondition.put(UnitType.Terran_Medic, 4);
            moveOutCondition.put(UnitType.Terran_Siege_Tank_Tank_Mode, 1);
        }

        return moveOutCondition;
    }
}
