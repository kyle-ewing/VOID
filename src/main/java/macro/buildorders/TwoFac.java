package macro.buildorders;

import bwapi.TechType;
import bwapi.UnitType;
import bwapi.UpgradeType;
import planner.PlannedItem;
import planner.PlannedItemStatus;
import planner.PlannedItemType;

import java.util.ArrayList;

public class TwoFac implements BuildOrder {

    public BuildOrderName getBuildOrderName() {
        return BuildOrderName.TWOFAC;
    }
    public ArrayList<PlannedItem> getBuildOrder() {
        ArrayList<PlannedItem> buildOrder = new ArrayList<>();
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 9, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Barracks, 10, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Refinery, 12, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 15, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Factory, 18, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 23, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Factory, 20, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Machine_Shop, 22, PlannedItemStatus.NOT_STARTED, PlannedItemType.ADDON, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Machine_Shop, 22, PlannedItemStatus.NOT_STARTED, PlannedItemType.ADDON, 2));
        buildOrder.add(new PlannedItem(UpgradeType.Ion_Thrusters, 24, PlannedItemStatus.NOT_STARTED, PlannedItemType.UPGRADE, UnitType.Terran_Machine_Shop, 2));
        buildOrder.add(new PlannedItem(TechType.Spider_Mines, 24, PlannedItemStatus.NOT_STARTED, PlannedItemType.UPGRADE, UnitType.Terran_Machine_Shop, 2));
        return  buildOrder;
    }
}
