package information;

import bwapi.Game;
import information.enemyopeners.EnemyStrategy;
import information.enemyopeners.FourPool;

import java.util.HashSet;

public class EnemyStrategyManager {
    private HashSet<EnemyStrategy> enemyStrategies = new HashSet<>();

    public EnemyStrategyManager() {
        init();
    }

    private void init() {
        addEnemyStrategies();
    }

    private void addEnemyStrategies() {
        enemyStrategies.add(new FourPool());
    }

    public HashSet<EnemyStrategy> getEnemyStrategies() {
        return enemyStrategies;
    }
}
