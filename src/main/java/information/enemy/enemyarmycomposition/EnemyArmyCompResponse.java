package information.enemy.enemyarmycomposition;

import bwapi.UnitType;
import information.enemy.EnemyUnits;

import java.util.HashSet;

public abstract class EnemyArmyCompResponse {
    private UnitType responseUnitType;
    private int triggerAmount;
    private int maxResponseCount;
    private int basePriority;

    public EnemyArmyCompResponse(UnitType responseUnitType, int triggerAmount, int maxResponseCount, int basePriority) {
        this.responseUnitType = responseUnitType;
        this.triggerAmount = triggerAmount;
        this.maxResponseCount = maxResponseCount;
        this.basePriority = basePriority;
    }

    public abstract boolean appliesToUnit(UnitType enemyType);

    public int countMatchingUnits(HashSet<EnemyUnits> enemyUnits) {
        int count = 0;
        for (EnemyUnits unit : enemyUnits) {
            if (unit.getEnemyType() == null) {
                continue;
            }
            if (appliesToUnit(unit.getEnemyType())) {
                count++;
            }
        }
        return count;
    }

    public boolean isTriggered(HashSet<EnemyUnits> enemyUnits) {
        return countMatchingUnits(enemyUnits) >= triggerAmount;
    }

    public int getDesiredResponseCount(int enemyAmount) {
        return Math.min(maxResponseCount, enemyAmount / triggerAmount * 2);
    }

    public int getPriority(int unitCount) {
        return basePriority;
    }

    public UnitType getResponseUnitType() {
        return responseUnitType;
    }

    public int getMaxResponseCount() {
        return maxResponseCount;
    }

    public int getTriggerAmount() {
        return triggerAmount;
    }

    public int getBasePriority() {
        return basePriority;
    }
}
