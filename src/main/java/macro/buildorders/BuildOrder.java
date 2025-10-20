package macro.buildorders;

import bwapi.UnitType;
import planner.PlannedItem;

import java.util.ArrayList;
import java.util.HashSet;

public abstract class BuildOrder {
    private HashSet<UnitType> liftableBuildings = new HashSet<>();


    public abstract BuildOrderName getBuildOrderName();
    public abstract ArrayList<PlannedItem> getBuildOrder();
    public abstract void setLiftableBuildings();

    public HashSet<UnitType> getLiftableBuildings() {
        return liftableBuildings;
    }
}
