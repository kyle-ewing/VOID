package information.enemy.enemyarmycomposition;

import bwapi.UnitType;

public class Zergling extends EnemyArmyCompResponse {
    private static final int TRIGGER_AMOUNT = 12;
    private static final int MAX_FIREBATS = 8;
    private static final int BASE_PRIORITY = 2;
    private static final int ESCALATION_AMOUNT = 30;

    public Zergling() {
        super(UnitType.Terran_Firebat, TRIGGER_AMOUNT, MAX_FIREBATS, BASE_PRIORITY);
    }

    @Override
    public boolean appliesToUnit(UnitType enemyType) {
        return enemyType == UnitType.Zerg_Zergling;
    }

    @Override
    public int getDesiredResponseCount(int enemyAmount) {
        return Math.min(getMaxResponseCount(), enemyAmount / 4);
    }

    @Override
    public int getPriority(int unitCount) {
        if (unitCount >= ESCALATION_AMOUNT) {
            return 1;
        }
        return BASE_PRIORITY;
    }
}
