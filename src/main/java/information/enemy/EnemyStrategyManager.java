package information.enemy;

import java.util.HashSet;

import information.MapInfo;
import information.enemy.enemyopeners.CannonRush;
import information.enemy.enemyopeners.DTRush;
import information.enemy.enemyopeners.EnemyStrategy;
import information.enemy.enemyopeners.FFE;
import information.enemy.enemyopeners.FourPool;
import information.enemy.enemyopeners.FourRax;
import information.enemy.enemyopeners.GasSteal;
import information.enemy.enemyopeners.OneBaseLurker;
import information.enemy.enemyopeners.OneBaseMuta;
import information.enemy.enemyopeners.SCVRush;
import information.enemy.enemyopeners.TwoBaseLurker;
import information.enemy.enemyopeners.TwoFacTank;
import information.enemy.enemytechbuildings.EnemyTechBuilding;
import information.enemy.enemytechbuildings.protoss.ArbiterTribunal;
import information.enemy.enemytechbuildings.protoss.CitadelOfAdun;
import information.enemy.enemytechbuildings.protoss.FleetBeacon;
import information.enemy.enemytechbuildings.protoss.RoboSupportBay;
import information.enemy.enemytechbuildings.terran.CovertOps;
import information.enemy.enemytechbuildings.terran.NuclearSilo;
import information.enemy.enemytechbuildings.terran.PhysicsLab;
import information.enemy.enemytechbuildings.terran.Starport;
import information.enemy.enemytechbuildings.zerg.DefilerMound;
import information.enemy.enemytechbuildings.zerg.GreaterSpire;
import information.enemy.enemytechbuildings.zerg.QueensNest;
import information.enemy.enemytechbuildings.zerg.Spire;
import information.enemy.enemytechunits.Arbiter;
import information.enemy.enemytechunits.BattleCruiser;
import information.enemy.enemytechunits.Carrier;
import information.enemy.enemytechunits.DarkTemplar;
import information.enemy.enemytechunits.EnemyTechUnits;
import information.enemy.enemytechunits.Guardian;
import information.enemy.enemytechunits.Lurker;
import information.enemy.enemytechunits.Mutalisk;
import information.enemy.enemytechunits.Queen;
import information.enemy.enemytechunits.ShuttleReaver;
import information.enemy.enemytechunits.SiegeTank;
import information.enemy.enemytechunits.Wraith;

public class EnemyStrategyManager {
    private MapInfo mapInfo;
    private HashSet<EnemyStrategy> enemyStrategies = new HashSet<>();
    private HashSet<EnemyTechUnits> enemyTechUnits = new HashSet<>();
    private HashSet<EnemyTechBuilding> enemyTechBuildings = new HashSet<>();

    public EnemyStrategyManager(MapInfo mapInfo) {
        this.mapInfo = mapInfo;

        init();
    }

    private void init() {
        addEnemyStrategies();
        addEnemyTechUnits();
        addEnemyTechBuildings();
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
        enemyStrategies.add(new FFE(mapInfo));
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

    private void addEnemyTechBuildings() {
        enemyTechBuildings.add(new RoboSupportBay());
        enemyTechBuildings.add(new ArbiterTribunal());
        enemyTechBuildings.add(new CitadelOfAdun());
        enemyTechBuildings.add(new FleetBeacon());
        enemyTechBuildings.add(new DefilerMound());
        enemyTechBuildings.add(new QueensNest());
        enemyTechBuildings.add(new Spire());
        enemyTechBuildings.add(new GreaterSpire());
        enemyTechBuildings.add(new CovertOps());
        enemyTechBuildings.add(new NuclearSilo());
        enemyTechBuildings.add(new PhysicsLab());
        enemyTechBuildings.add(new Starport());
    }

    public HashSet<EnemyStrategy> getEnemyStrategies() {
        return enemyStrategies;
    }

    public HashSet<EnemyTechBuilding> getEnemyBuildings() {
        return enemyTechBuildings;
    }

    public HashSet<EnemyTechUnits> getEnemyTechUnits() {
        return enemyTechUnits;
    }
}
