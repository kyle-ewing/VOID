package information.enemy;

import information.MapInfo;
import information.enemy.enemyopeners.*;
import information.enemy.enemytechunits.*;

import java.util.HashSet;

public class EnemyStrategyManager {
    private MapInfo mapInfo;
    private HashSet<EnemyStrategy> enemyStrategies = new HashSet<>();
    private HashSet<EnemyTechUnits> enemyTechUnits = new HashSet<>();

    public EnemyStrategyManager(MapInfo mapInfo) {
        this.mapInfo = mapInfo;

        init();
    }

    private void init() {
        addEnemyStrategies();
        addEnemyTechUnits();
    }

    private void addEnemyStrategies() {
        enemyStrategies.add(new FourPool(mapInfo));
//        enemyStrategies.add(new NinePool(baseInfo));
        enemyStrategies.add(new CannonRush(mapInfo));
        enemyStrategies.add(new GasSteal(mapInfo));
//        enemyStrategies.add(new CCFirst(baseInfo));
        enemyStrategies.add(new FourRax(mapInfo));
        enemyStrategies.add(new TwoFacTank());
        enemyStrategies.add(new SCVRush(mapInfo));
        enemyStrategies.add(new OneBaseMuta(mapInfo));
        enemyStrategies.add(new OneBaseLurker(mapInfo));
        // enemyStrategies.add(new ThreeHatchBeforePool(baseInfo));
        enemyStrategies.add(new TwoBaseLurker(mapInfo));
        enemyStrategies.add(new DTRush());
    }

    private void addEnemyTechUnits() {
        enemyTechUnits.add(new ShuttleReaver());
        enemyTechUnits.add(new Arbiter());
        enemyTechUnits.add(new Carrier());
        enemyTechUnits.add(new DarkTemplar());
        enemyTechUnits.add(new Lurker());
        enemyTechUnits.add(new Wraith());
        enemyTechUnits.add(new BattleCruiser());
        enemyTechUnits.add(new SiegeTank());
        enemyTechUnits.add(new Mutalisk());
        enemyTechUnits.add(new Guardian());
        enemyTechUnits.add(new Queen());
    }

    public HashSet<EnemyStrategy> getEnemyStrategies() {
        return enemyStrategies;
    }

    public HashSet<EnemyTechUnits> getEnemyTechUnits() {
        return enemyTechUnits;
    }
}
