package macro.buildorders;

import bwapi.UnitType;
import planner.PlannedItem;
import planner.PlannedItemStatus;
import planner.PlannedItemType;

import java.util.ArrayList;
import java.util.HashMap;

public class EightRax extends BuildOrder {

    public BuildOrderName getBuildOrderName() {
        return BuildOrderName.EIGHTRAX;
    }
    public ArrayList<PlannedItem> getBuildOrder() {
        ArrayList<PlannedItem> buildOrder = new ArrayList<>();
        buildOrder.add(new PlannedItem(UnitType.Terran_Barracks, 8, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 9, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 14, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 22, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        return  buildOrder;
    }

    public void setLiftableBuildings() {
    }

    public BunkerLocation getBunkerLocation() {
        return null;
    }

    public HashMap<UnitType, Integer> getMoveOutCondition() {
        HashMap<UnitType, Integer> moveOutCondition = new HashMap<>();
        return moveOutCondition;
    }
}
