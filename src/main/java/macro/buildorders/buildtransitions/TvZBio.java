package macro.buildorders.buildtransitions;

import bwapi.UnitType;
import bwapi.UpgradeType;
import macro.buildorders.BuildOrder;
import macro.buildorders.BuildType;
import planner.PlannedItem;
import planner.PlannedItemStatus;
import planner.PlannedItemType;

import java.util.ArrayList;

public class TvZBio extends BuildTransition {
    public BuildTransitionName getBuildTransitionName() {
        return BuildTransitionName.TVZBIO;
    }

    public ArrayList<PlannedItem> getTransitionBuild() {
        ArrayList<PlannedItem> transitionItems = new ArrayList<>();
        transitionItems.add(new PlannedItem(UpgradeType.Terran_Infantry_Weapons, PlannedItemStatus.NOT_STARTED, PlannedItemType.UPGRADE, UnitType.Terran_Engineering_Bay, 2, 3));
        transitionItems.add(new PlannedItem(UpgradeType.Terran_Infantry_Armor, PlannedItemStatus.NOT_STARTED, PlannedItemType.UPGRADE, UnitType.Terran_Engineering_Bay, 2, 4));
        transitionItems.add(new PlannedItem(UpgradeType.Terran_Infantry_Weapons, PlannedItemStatus.NOT_STARTED, PlannedItemType.UPGRADE, UnitType.Terran_Engineering_Bay, 3, 3));
        transitionItems.add(new PlannedItem(UpgradeType.Terran_Infantry_Armor, PlannedItemStatus.NOT_STARTED, PlannedItemType.UPGRADE, UnitType.Terran_Engineering_Bay, 3, 4));
        transitionItems.add(new PlannedItem(UnitType.Terran_Factory, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 4));
        transitionItems.add(new PlannedItem(UnitType.Terran_Machine_Shop, PlannedItemStatus.NOT_STARTED, PlannedItemType.ADDON, 4));
        return transitionItems;
    }

    public ArrayList<PlannedItem> getOptionalBuildings() {
        ArrayList<PlannedItem> optionalItems = new ArrayList<>();
        optionalItems.add(new PlannedItem(UnitType.Terran_Starport, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 3));
        optionalItems.add(new PlannedItem(UnitType.Terran_Science_Facility, PlannedItemStatus.NOT_STARTED, PlannedItemType.BUILDING, 3));
        optionalItems.add(new PlannedItem(UnitType.Terran_Control_Tower, PlannedItemStatus.NOT_STARTED, PlannedItemType.ADDON, 5));
        return optionalItems;
    }

    @Override
    public boolean transitionsFrom(BuildOrder buildOrder) {
        return buildOrder.buildType() == BuildType.BIO;
    }
}
