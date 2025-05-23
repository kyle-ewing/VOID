package information.enemyopeners;

import information.EnemyUnits;

import java.util.HashSet;

public abstract class EnemyStrategy {
    private String strategyName;

    public EnemyStrategy(String strategyName) {
        this.strategyName = strategyName;
    }

    public abstract boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, int frameCount);

    public String getStrategyName() {
        return strategyName;
    }
}
