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

public class EightRax extends BuildOrder {

    public BuildOrderName getBuildOrderName() {
        return BuildOrderName.EIGHTRAX;
    }
    public ArrayList<PlannedItem> getBuildOrder() {
        ArrayList<PlannedItem> buildOrder = new ArrayList<>();
        buildOrder.add(new PlannedItem(UnitType.Terran_Barracks, 8, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 9, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 14, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 22, PlannedItemType.BUILDING, 2));
        return  buildOrder;
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
