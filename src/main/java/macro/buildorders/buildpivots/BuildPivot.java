package macro.buildorders.buildpivots;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import bwapi.UnitType;
import information.enemy.EnemyUnits;
import information.enemy.enemyopeners.EnemyStrategyName;
import macro.buildorders.BuildType;
import macro.buildorders.BunkerLocation;
import planner.PlannedItem;
import util.Time;

public abstract class BuildPivot {
    protected boolean rushActive = false;

    public abstract BuildPivotName getBuildPivotName();
    public abstract ArrayList<PlannedItem> getPivotBuild();
    public abstract HashMap<UnitType, Integer> getMoveOutCondition(Time time, HashSet<EnemyUnits> enemyUnits);
    public abstract BuildType buildType();
    public abstract BunkerLocation getBunkerLocation();

    public boolean pivotsFrom(EnemyStrategyName enemyStrategies) {
        return false;
    }

    public ArrayList<UnitType> proxyBuildings() {
        return new ArrayList<>();
    }

    public HashSet<UnitType> getLiftableBuildings() {
        return new HashSet<>();
    }

    public HashSet<UnitType> getCancelableBuildings() {
        return new HashSet<>();
    }

    public boolean isRushActive() {
        return rushActive;
    }

    public void setRushActive(boolean rushActive) {
        this.rushActive = rushActive;
    }

}
