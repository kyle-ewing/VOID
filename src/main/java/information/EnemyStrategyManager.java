package information;

import bwapi.Game;
import information.enemyopeners.CannonRush;
import information.enemyopeners.EnemyStrategy;
import information.enemyopeners.FourPool;

import java.util.HashSet;

public class EnemyStrategyManager {
    private BaseInfo baseInfo;
    private HashSet<EnemyStrategy> enemyStrategies = new HashSet<>();

    public EnemyStrategyManager(BaseInfo baseInfo) {
        this.baseInfo = baseInfo;

        init();
    }

    private void init() {
        addEnemyStrategies();
    }

    private void addEnemyStrategies() {
        enemyStrategies.add(new FourPool());
        enemyStrategies.add(new CannonRush(baseInfo));
    }

    public HashSet<EnemyStrategy> getEnemyStrategies() {
        return enemyStrategies;
    }
}
