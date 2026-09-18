package information.enemy.enemyopeners;

import java.util.HashMap;
import java.util.HashSet;

import bwapi.Game;
import bwapi.UnitType;
import bwapi.UpgradeType;
import information.MapInfo;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildType;
import util.Time;

public class NinePoolSpeedling extends EnemyStrategy {
    private Game game;
    private MapInfo mapInfo;
    private NinePool ninePool;
    private boolean triggered = false;

    public NinePoolSpeedling(Game game, MapInfo mapInfo, NinePool ninePool) {
        super(EnemyStrategyName.NINEPOOLSPEEDLING);
        this.game = game;
        this.mapInfo = mapInfo;
        this.ninePool = ninePool;
        openerSwitchWindow = 2880;

        buildingResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        if (enemyUnits.stream().anyMatch(eu -> eu.getEnemyType() == UnitType.Zerg_Lair
                || eu.getEnemyType() == UnitType.Zerg_Hydralisk_Den)) {
            return false;
        }

        if (triggered) {
            return true;
        }

        if (time.lessThanOrEqual(new Time(4,15)) && game.enemy().getUpgradeLevel(UpgradeType.Metabolic_Boost) > 0) {
            triggered = true;
            ninePool.setHandedOff(true);
            return true;
        }

        long knownLings = enemyUnits.stream().filter(eu -> eu.getEnemyType() == UnitType.Zerg_Zergling).count();

        if (knownLings >= 8 && time.greaterThan(new Time(3,20)) && time.lessThanOrEqual(new Time(4,0))) {
            triggered = true;
            ninePool.setHandedOff(true);
            return true;
        }

        boolean hasNaturalHatch = false;
        if (mapInfo.getEnemyNatural() != null) {
            hasNaturalHatch = enemyUnits.stream()
                    .filter(eu -> eu.getEnemyType().isResourceDepot())
                    .anyMatch(eu -> eu.getEnemyPosition().getDistance(mapInfo.getEnemyNatural().getLocation().toPosition()) < 200);
        }

        boolean poolUpgrading = enemyUnits.stream()
                .filter(eu -> eu.getEnemyType() == UnitType.Zerg_Spawning_Pool)
                .anyMatch(eu -> eu.getEnemyUnit().isVisible() && eu.getEnemyUnit().isUpgrading());

        if (poolUpgrading && !hasNaturalHatch
                && time.greaterThan(new Time(3,30)) && time.lessThanOrEqual(new Time(5,0))) {
            triggered = true;
            ninePool.setHandedOff(true);
            return true;
        }

        if (!ninePool.hasMatched()) {
            return false;
        }

        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyType() == UnitType.Zerg_Extractor) {
                triggered = true;
                ninePool.setHandedOff(true);
                return true;
            }

            if (enemyUnit.getEnemyType() == UnitType.Zerg_Drone && enemyUnit.getEnemyUnit().isCarryingGas()) {
                triggered = true;
                ninePool.setHandedOff(true);
                return true;
            }

            if (enemyUnit.getEnemyType() != UnitType.Zerg_Spawning_Pool) {
                continue;
            }

            if (enemyUnit.getEnemyUnit().isVisible() && enemyUnit.getEnemyUnit().isUpgrading()
                    && time.greaterThan(new Time(2,0)) && time.lessThanOrEqual(new Time(3,30))) {
                triggered = true;
                ninePool.setHandedOff(true);
                return true;
            }
        }

        return false;
    }

    public void buildingResponse() {
    }

    public void upgradeResponse() {
    }

    public HashMap<UnitType, Integer> getMoveOutCondition(BuildType buildType, Time time, HashSet<EnemyUnits> enemyUnits) {
        HashMap<UnitType, Integer> moveOutCondition = new HashMap<>();
        long knownLings = enemyUnits.stream().filter(eu -> eu.getEnemyType() == UnitType.Zerg_Zergling).count();

        if (buildType == BuildType.BIO) {
            if (time.lessThanOrEqual(new Time(8,0)) && knownLings > 10) {
                moveOutCondition.put(UnitType.Terran_Marine, 20);
                moveOutCondition.put(UnitType.Terran_Medic, 5);
                moveOutCondition.put(UnitType.Terran_Firebat, 3);
            }
            else if (time.lessThanOrEqual(new Time(8,0)) && knownLings < 10) {
                return new HashMap<>();
            }
            else {
                moveOutCondition.put(UnitType.Terran_Marine, 25);
                moveOutCondition.put(UnitType.Terran_Medic, 6);
            }
        }

        return moveOutCondition;
    }

    public HashSet<UnitType> removeBuildings() {
        return new HashSet<>();
    }
}
