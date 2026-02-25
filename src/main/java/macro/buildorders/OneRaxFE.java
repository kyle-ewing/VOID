package macro.buildorders;

import bwapi.UnitType;
import information.enemy.EnemyUnits;
import planner.PlannedItem;
import planner.PlannedItemStatus;
import planner.PlannedItemType;
import util.Time;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

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

    public HashSet<UnitType> getLiftableBuildings() {
        return  liftableBuildings;
    }

    public BunkerLocation getBunkerLocation() {
        return null;
    }

    public BuildType buildType() {
        return BuildType.BIO;
    }

    public HashMap<UnitType, Integer> getMoveOutCondition(Time time, HashSet<EnemyUnits> enemyUnits) {
        HashMap<UnitType, Integer> moveOutCondition = new HashMap<>();
        return moveOutCondition;
    }

}
