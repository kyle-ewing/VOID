package macro.buildorders.buildtransitions;

import java.util.ArrayList;

import bwapi.UnitType;
import bwapi.UpgradeType;
import macro.buildorders.BuildOrder;
import macro.buildorders.BuildType;
import planner.PlannedItem;
import planner.PlannedItemStatus;
import planner.PlannedItemType;

public class TvPMech extends BuildTransition {
    public BuildTransitionName getBuildTransitionName() {
        return BuildTransitionName.TVPMECH;
    }

    public ArrayList<PlannedItem> getTransitionBuild() {
        ArrayList<PlannedItem> transitionItems = new ArrayList<>();
        transitionItems.add(new PlannedItem(UnitType.Terran_Armory, 72, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 6));
        transitionItems.add(new PlannedItem(UpgradeType.Terran_Vehicle_Weapons, PlannedItemStatus.NOT_STARTED, PlannedItemType.UPGRADE, UnitType.Terran_Armory, 2, 4));
        transitionItems.add(new PlannedItem(UpgradeType.Terran_Vehicle_Plating, PlannedItemStatus.NOT_STARTED, PlannedItemType.UPGRADE, UnitType.Terran_Armory, 2, 5));
        transitionItems.add(new PlannedItem(UpgradeType.Terran_Vehicle_Weapons, PlannedItemStatus.NOT_STARTED, PlannedItemType.UPGRADE, UnitType.Terran_Armory, 3, 4));
        transitionItems.add(new PlannedItem(UpgradeType.Terran_Vehicle_Plating, PlannedItemStatus.NOT_STARTED, PlannedItemType.UPGRADE, UnitType.Terran_Armory, 3, 5));
        transitionItems.add(new PlannedItem(UnitType.Terran_Command_Center, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 4));
        transitionItems.add(new PlannedItem(UnitType.Terran_Factory, 70, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 3, true));
        transitionItems.add(new PlannedItem(UnitType.Terran_Factory, 70, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 3, true));
        transitionItems.add(new PlannedItem(UnitType.Terran_Factory, 85, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 3, false));
        transitionItems.add(new PlannedItem(UnitType.Terran_Factory, 85,PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 7, false));
        transitionItems.add(new PlannedItem(UnitType.Terran_Factory, 85, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 7, false));
        transitionItems.add(new PlannedItem(UnitType.Terran_Machine_Shop, PlannedItemStatus.NOT_STARTED, PlannedItemType.ADDON, 6));
        transitionItems.add(new PlannedItem(UnitType.Terran_Machine_Shop, PlannedItemStatus.NOT_STARTED, PlannedItemType.ADDON, 6));
        return transitionItems;
    }

    public ArrayList<PlannedItem> getOptionalBuildings() {
        ArrayList<PlannedItem> optionalItems = new ArrayList<>();
        optionalItems.add(new PlannedItem(UnitType.Terran_Starport, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 5, true));
        optionalItems.add(new PlannedItem(UnitType.Terran_Science_Facility, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 6, true));
        optionalItems.add(new PlannedItem(UnitType.Terran_Control_Tower, PlannedItemStatus.NOT_STARTED, PlannedItemType.ADDON, 5));
        return optionalItems;
    }

    @Override
    public boolean transitionsFrom(BuildOrder buildOrder) {
        return buildOrder.buildType() == BuildType.MECH;
    }
}
