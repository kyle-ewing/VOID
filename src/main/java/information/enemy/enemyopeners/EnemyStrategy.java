package information.enemy.enemyopeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import bwapi.TechType;
import bwapi.UnitType;
import bwapi.UpgradeType;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildType;
import util.Time;

public abstract class EnemyStrategy {
    protected String strategyName;
    protected ArrayList<UnitType> buildingResponse = new ArrayList<>();
    protected ArrayList<UpgradeType> upgradeResponse = new ArrayList<>();
    protected ArrayList<TechType> techUpgradeResponse = new ArrayList<>();
    protected EnemyUnits priorityEnemyUnit = null;
    protected boolean defendedStrategy = false;

    public EnemyStrategy(String strategyName) {
        this.strategyName = strategyName;
    }

    public abstract boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time);
    public abstract void buildingResponse();
    public abstract void upgradeResponse();
    public abstract HashSet<UnitType> removeBuildings();
    public abstract HashMap<UnitType, Integer> getMoveOutCondition(BuildType buildType, Time time);

    public String getStrategyName() {
        return strategyName;
    }

    public void techUpgradeResponse() {
    }

    public ArrayList<UnitType> getBuildingResponse() {
        return buildingResponse;
    }

    public ArrayList<UpgradeType> getUpgradeResponse() {
        return upgradeResponse;
    }

    public ArrayList<TechType> getTechUpgradeResponse() {
        return techUpgradeResponse;
    }

    public HashSet<UnitType> getUnitResponse() {
        return new HashSet<>();
    }

    //Buildings that are fine to have more than one of
    public HashSet<UnitType> additionalBuildings() {
        return new HashSet<>();
    }

    public boolean isStrategyDefended() {
        return defendedStrategy;
    }

    public void setDefendedStrategy(boolean defendedStrategy) {
        this.defendedStrategy = defendedStrategy;
    }

    public EnemyUnits getPriorityEnemyUnit() {
        return priorityEnemyUnit;
    }

    public void setPriorityEnemyUnit(EnemyUnits priorityEnemyUnit) {
        this.priorityEnemyUnit = priorityEnemyUnit;
    }

    public boolean overrideBuildingLift() {
        return false;
    }
}
