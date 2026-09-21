package information.enemy.enemyopeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import bwapi.TechType;
import bwapi.Unit;
import bwapi.UnitType;
import bwapi.UpgradeType;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildType;
import util.Time;

public abstract class EnemyStrategy {
    protected EnemyStrategyName strategyName;
    protected ArrayList<UnitType> buildingResponse = new ArrayList<>();
    protected ArrayList<UpgradeType> upgradeResponse = new ArrayList<>();
    protected ArrayList<TechType> techUpgradeResponse = new ArrayList<>();
    protected EnemyUnits priorityEnemyUnit = null;
    protected boolean defendedStrategy = false;
    protected boolean hardLockedWhenSeen = false;
    protected int openerSwitchWindow = 1440;

    public EnemyStrategy(EnemyStrategyName strategyName) {
        this.strategyName = strategyName;
    }

    public abstract boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time);
    public abstract void buildingResponse();
    public abstract void upgradeResponse();
    public abstract HashSet<UnitType> removeBuildings();
    public abstract HashMap<UnitType, Integer> getMoveOutCondition(BuildType buildType, Time time, HashSet<EnemyUnits> enemyUnits);

    public EnemyStrategyName getStrategyName() {
        return strategyName;
    }

    //getRemainingBuildTime is always zero for enemy units, estimate from health instead
    protected int remainingBuildFrames(EnemyUnits enemyUnit) {
        Unit unit = enemyUnit.getEnemyUnit();

        if (unit.isCompleted()) {
            return 0;
        }

        UnitType enemyType = enemyUnit.getEnemyType();
        int maxTotalHealth = enemyType.maxHitPoints() + enemyType.maxShields();
        int startingHealth = 1 + (maxTotalHealth / 10);
        double progress = (double) (unit.getHitPoints() + unit.getShields() - startingHealth) / (maxTotalHealth - startingHealth);

        if (progress < 0) {
            progress = 0;
        }

        return (int) ((1 - progress) * enemyType.buildTime());
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

    public boolean mineralLineTurretsOnly() {
        return false;
    }

    public boolean isStrategyDefended() {
        return defendedStrategy;
    }

    public boolean isHardLockedWhenSeen() {
        return hardLockedWhenSeen;
    }

    public int getOpenerSwitchWindow() {
        return openerSwitchWindow;
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
