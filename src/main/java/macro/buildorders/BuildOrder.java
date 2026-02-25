package macro.buildorders;

import bwapi.TilePosition;
import bwapi.UnitType;
import information.enemy.EnemyUnits;
import planner.PlannedItem;
import util.Time;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public abstract class BuildOrder {
    protected HashSet<UnitType> liftableBuildings = new HashSet<>();
    protected TilePosition bunkerPostion = null;


    public abstract BuildOrderName getBuildOrderName();
    public abstract ArrayList<PlannedItem> getBuildOrder();
    public abstract HashSet<UnitType> getLiftableBuildings();
    public abstract BunkerLocation getBunkerLocation();
    public abstract BuildType buildType();
    public abstract HashMap<UnitType, Integer> getMoveOutCondition(Time time, HashSet<EnemyUnits> enemyUnits);

    public int getScoutSupply() {
        return 10;
    }

    public TilePosition getBunkerPostion() {
        return bunkerPostion;
    }

    public void setBunkerPostion(TilePosition bunkerPostion) {
        this.bunkerPostion = bunkerPostion;
    }
}
