package macro.buildpivots;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import bwapi.Race;
import bwapi.Unit;
import bwapi.UnitType;
import information.enemy.EnemyUnits;
import information.enemy.enemyopeners.EnemyStrategyName;
import macro.buildorders.BuildType;
import macro.buildorders.BunkerLocation;
import planner.PlannedItem;
import util.Time;

public abstract class BuildPivot {
    protected boolean rushActive = false;
    protected HashSet<EnemyStrategyName> enemyStrategies = new HashSet<>();

    public abstract BuildPivotName getBuildPivotName();
    public abstract ArrayList<PlannedItem> getPivotBuild();
    public abstract HashMap<UnitType, Integer> getMoveOutCondition(Time time, HashSet<EnemyUnits> enemyUnits);
    public abstract BuildType buildType();
    public abstract BunkerLocation getBunkerLocation();

    public boolean pivotsFrom(EnemyStrategyName enemyStrategies, Race enemyRace) {
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

    public boolean retainAddedBuildings() {
        return false;
    }

    //How many of each building the pivot needs in place at the time the pivot fires
    //Used to retain pivot build entries that sit below the current supply
    public HashMap<UnitType, Integer> getRequiredBuildings() {
        return new HashMap<>();
    }

    public int getGasWorkerTarget(int totalGasGathered, HashSet<Unit> allBuildings) {
        return 3;
    }

    public boolean isRushActive() {
        return rushActive;
    }

    public void setRushActive(boolean rushActive) {
        this.rushActive = rushActive;
    }

}
