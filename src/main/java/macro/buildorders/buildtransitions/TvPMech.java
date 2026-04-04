package macro.buildorders.buildtransitions;

import java.util.ArrayList;

import bwapi.UnitType;
import bwapi.UpgradeType;
import macro.buildorders.BuildOrder;
import macro.buildorders.BuildType;
import planner.PlannedItem;
import planner.PlannedItemType;

public class TvPMech extends BuildTransition {
    public BuildTransitionName getBuildTransitionName() {
        return BuildTransitionName.TVPMECH;
    }

    public ArrayList<PlannedItem> getTransitionBuild() {
        ArrayList<PlannedItem> transitionItems = new ArrayList<>();
        transitionItems.add(new PlannedItem(UnitType.Terran_Armory, 72, PlannedItemType.BUILDING, 6));
        transitionItems.add(new PlannedItem(UpgradeType.Terran_Vehicle_Weapons, PlannedItemType.UPGRADE, UnitType.Terran_Armory, 2, 4));
        transitionItems.add(new PlannedItem(UpgradeType.Terran_Vehicle_Plating, PlannedItemType.UPGRADE, UnitType.Terran_Armory, 2, 5));
        transitionItems.add(new PlannedItem(UpgradeType.Terran_Vehicle_Weapons, PlannedItemType.UPGRADE, UnitType.Terran_Armory, 3, 4));
        transitionItems.add(new PlannedItem(UpgradeType.Terran_Vehicle_Plating, PlannedItemType.UPGRADE, UnitType.Terran_Armory, 3, 5));
        transitionItems.add(new PlannedItem(UnitType.Terran_Command_Center, PlannedItemType.BUILDING, 4));
        transitionItems.add(new PlannedItem(UnitType.Terran_Factory, 70, PlannedItemType.BUILDING, 3, true));
        transitionItems.add(new PlannedItem(UnitType.Terran_Factory, 70, PlannedItemType.BUILDING, 3, true));
        transitionItems.add(new PlannedItem(UnitType.Terran_Factory, 85, PlannedItemType.BUILDING, 3, false));
        transitionItems.add(new PlannedItem(UnitType.Terran_Factory, 85, PlannedItemType.BUILDING, 3, false));
        transitionItems.add(new PlannedItem(UnitType.Terran_Factory, 85, PlannedItemType.BUILDING, 7, false));
        transitionItems.add(new PlannedItem(UnitType.Terran_Machine_Shop, PlannedItemType.ADDON, 6));
        transitionItems.add(new PlannedItem(UnitType.Terran_Machine_Shop, PlannedItemType.ADDON, 6));
        return transitionItems;
    }

    public ArrayList<PlannedItem> getOptionalBuildings() {
        ArrayList<PlannedItem> optionalItems = new ArrayList<>();
        optionalItems.add(new PlannedItem(UnitType.Terran_Starport, PlannedItemType.BUILDING, 5, true));
        optionalItems.add(new PlannedItem(UnitType.Terran_Science_Facility, PlannedItemType.BUILDING, 6, true));
        optionalItems.add(new PlannedItem(UnitType.Terran_Control_Tower, PlannedItemType.ADDON, 5));
        return optionalItems;
    }

    @Override
    public boolean transitionsFrom(BuildOrder buildOrder) {
        return buildOrder.buildType() == BuildType.MECH;
    }
}
