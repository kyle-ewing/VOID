package information.enemy;

import information.BaseInfo;
import information.enemy.enemyopeners.CannonRush;
import information.enemy.enemyopeners.EnemyStrategy;
import information.enemy.enemyopeners. FourPool;
import information.enemy.enemytechunits.Arbiter;
import information.enemy.enemytechunits.Carrier;
import information.enemy.enemytechunits.EnemyTechUnits;
import information.enemy.enemytechunits.ShuttleReaver;

import java.util.HashSet;

public class EnemyStrategyManager {
    private BaseInfo baseInfo;
    private HashSet<EnemyStrategy> enemyStrategies = new HashSet<>();
    private HashSet<EnemyTechUnits> enemyTechUnits = new HashSet<>();

    public EnemyStrategyManager(BaseInfo baseInfo) {
        this.baseInfo = baseInfo;

        init();
    }

    private void init() {
        addEnemyStrategies();
        addEnemyTechUnits();
    }

    private void addEnemyStrategies() {
        enemyStrategies.add(new FourPool());
        enemyStrategies.add(new CannonRush(baseInfo));
    }

    private void addEnemyTechUnits() {
        enemyTechUnits.add(new ShuttleReaver());
        enemyTechUnits.add(new Arbiter());
        enemyTechUnits.add(new Carrier());
    }

    public HashSet<EnemyStrategy> getEnemyStrategies() {
        return enemyStrategies;
    }

    public HashSet<EnemyTechUnits> getEnemyTechUnits() {
        return enemyTechUnits;
    }
}
