package information.enemy.enemyopeners;

import bwapi.UnitType;
import information.enemy.EnemyUnits;
import util.Time;

import java.util.ArrayList;
import java.util.HashSet;

public abstract class EnemyStrategy {
    protected String strategyName;
    protected ArrayList<UnitType> buildingResponse = new ArrayList<>();
    protected EnemyUnits priorityEnemyUnit = null;
    protected boolean defendedStrategy = false;

    public EnemyStrategy(String strategyName) {
        this.strategyName = strategyName;
    }

    public abstract boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time);
    public abstract void buildingResponse();

    public String getStrategyName() {
        return strategyName;
    }

    public ArrayList<UnitType> getBuildingResponse() {
        return buildingResponse;
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
}
