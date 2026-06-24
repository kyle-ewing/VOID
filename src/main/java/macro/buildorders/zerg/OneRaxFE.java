package macro.buildorders.zerg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import bwapi.UnitType;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildOrder;
import macro.buildorders.BuildOrderName;
import macro.buildorders.BuildType;
import macro.buildorders.BunkerLocation;
import planner.PlannedItem;
import planner.PlannedItemType;
import util.Time;

public class OneRaxFE extends BuildOrder {
    public BuildOrderName getBuildOrderName() {
        return BuildOrderName.ONERAXFE;
    }

    public ArrayList<PlannedItem> getBuildOrder() {
        ArrayList<PlannedItem> buildOrder = new ArrayList<>();
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 9, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Barracks, 11, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Command_Center, 15, PlannedItemType.BUILDING, 2));

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
