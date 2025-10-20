package macro.buildorders;

import bwapi.UnitType;
import planner.PlannedItem;
import planner.PlannedItemStatus;
import planner.PlannedItemType;

import java.util.ArrayList;

public class OneRaxFE extends BuildOrder {
    public BuildOrderName getBuildOrderName() {
        return BuildOrderName.ONERAXFE;
    }

    public ArrayList<PlannedItem> getBuildOrder() {
        ArrayList<PlannedItem> buildOrder = new ArrayList<>();
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 9, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Barracks, 11, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Command_Center, 15, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));

        return buildOrder;
    }

    public void setLiftableBuildings() {
    }

}
