package macro.buildorders.buildtransitions;

import bwapi.UnitType;
import bwapi.UpgradeType;
import macro.buildorders.BuildOrder;
import macro.buildorders.BuildType;
import planner.PlannedItem;
import planner.PlannedItemType;

import java.util.ArrayList;

public class TvZBio extends BuildTransition {
    public BuildTransitionName getBuildTransitionName() {
        return BuildTransitionName.TVZBIO;
    }

    public ArrayList<PlannedItem> getTransitionBuild() {
        ArrayList<PlannedItem> transitionItems = new ArrayList<>();
        transitionItems.add(new PlannedItem(UpgradeType.Terran_Infantry_Weapons, PlannedItemType.UPGRADE, UnitType.Terran_Engineering_Bay, 2, 4));
        transitionItems.add(new PlannedItem(UpgradeType.Terran_Infantry_Armor, PlannedItemType.UPGRADE, UnitType.Terran_Engineering_Bay, 2, 5));
        transitionItems.add(new PlannedItem(UpgradeType.Terran_Infantry_Weapons, PlannedItemType.UPGRADE, UnitType.Terran_Engineering_Bay, 3, 4));
        transitionItems.add(new PlannedItem(UpgradeType.Terran_Infantry_Armor, PlannedItemType.UPGRADE, UnitType.Terran_Engineering_Bay, 3, 5));
        transitionItems.add(new PlannedItem(UnitType.Terran_Barracks, 50, PlannedItemType.BUILDING, 5));
        transitionItems.add(new PlannedItem(UnitType.Terran_Barracks, 50, PlannedItemType.BUILDING, 5));
        transitionItems.add(new PlannedItem(UnitType.Terran_Factory, 75, PlannedItemType.BUILDING, 5));
        transitionItems.add(new PlannedItem(UnitType.Terran_Machine_Shop, PlannedItemType.ADDON, 5));
        transitionItems.add(new PlannedItem(UnitType.Terran_Barracks, 80,PlannedItemType.BUILDING, 5));
        transitionItems.add(new PlannedItem(UnitType.Terran_Barracks, 80,PlannedItemType.BUILDING, 5));
        return transitionItems;
    }

    public ArrayList<PlannedItem> getOptionalBuildings() {
        ArrayList<PlannedItem> optionalItems = new ArrayList<>();
        optionalItems.add(new PlannedItem(UnitType.Terran_Starport, PlannedItemType.BUILDING, 4));
        optionalItems.add(new PlannedItem(UnitType.Terran_Science_Facility, PlannedItemType.BUILDING, 4));
        optionalItems.add(new PlannedItem(UnitType.Terran_Control_Tower, PlannedItemType.ADDON, 5));
        return optionalItems;
    }

    @Override
    public boolean transitionsFrom(BuildOrder buildOrder) {
        return buildOrder.buildType() == BuildType.BIO;
    }
}
