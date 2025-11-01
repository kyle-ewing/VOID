package information.enemy;

import information.BaseInfo;
import information.enemy.enemyopeners.*;
import information.enemy.enemytechunits.*;

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
        enemyStrategies.add(new GasSteal(baseInfo));
        enemyStrategies.add(new DTRush());
    }

    private void addEnemyTechUnits() {
        enemyTechUnits.add(new ShuttleReaver());
        enemyTechUnits.add(new Arbiter());
        enemyTechUnits.add(new Carrier());
        enemyTechUnits.add(new DarkTemplar());
    }

    public HashSet<EnemyStrategy> getEnemyStrategies() {
        return enemyStrategies;
    }

    public HashSet<EnemyTechUnits> getEnemyTechUnits() {
        return enemyTechUnits;
    }
}
