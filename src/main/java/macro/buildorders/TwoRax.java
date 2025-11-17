package macro.buildorders;

import bwapi.UnitType;
import planner.PlannedItem;
import planner.PlannedItemStatus;
import planner.PlannedItemType;

import java.util.ArrayList;
import java.util.HashMap;

public class TwoRax extends BuildOrder {
    public BuildOrderName getBuildOrderName() {
        return BuildOrderName.TWORAX;
    }

    public ArrayList<PlannedItem> getBuildOrder() {
        ArrayList<PlannedItem> buildOrder = new ArrayList<>();
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 9, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Barracks, 10, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Barracks, 11, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 14, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 21, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 1));
        return buildOrder;
    }

    public void setLiftableBuildings() {

    }

    public BunkerLocation getBunkerLocation() {
        return BunkerLocation.MAIN;
    }

    public HashMap<UnitType, Integer> getMoveOutCondition() {
        HashMap<UnitType, Integer> moveOutCondition = new HashMap<>();
        return moveOutCondition;
    }
}
