package macro.buildorders;

import bwapi.TilePosition;
import bwapi.UnitType;
import planner.PlannedItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public abstract class BuildOrder {
    protected HashSet<UnitType> liftableBuildings = new HashSet<>();
    protected TilePosition bunkerPostion = null;


    public abstract BuildOrderName getBuildOrderName();
    public abstract ArrayList<PlannedItem> getBuildOrder();
    public abstract void setLiftableBuildings();
    public abstract BunkerLocation getBunkerLocation();
    public abstract BuildType buildType();
    public abstract HashMap<UnitType, Integer> getMoveOutCondition();

    public HashSet<UnitType> getLiftableBuildings() {
        return liftableBuildings;
    }

    public TilePosition getBunkerPostion() {
        return bunkerPostion;
    }

    public void setBunkerPostion(TilePosition bunkerPostion) {
        this.bunkerPostion = bunkerPostion;
    }
}
