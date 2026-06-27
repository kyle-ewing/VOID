package information.enemy.enemyopeners;

import java.util.HashMap;
import java.util.HashSet;

import bwapi.UnitType;
import information.MapInfo;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildType;
import util.Time;

public class NinePool extends EnemyStrategy {
    private MapInfo mapInfo;

    public NinePool(MapInfo mapInfo) {
        super(EnemyStrategyName.NINEPOOL);
        this.mapInfo = mapInfo;
        openerSwitchWindow = 2880;

        buildingResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        boolean hasExtractor = enemyUnits.stream().anyMatch(eu -> eu.getEnemyType() == UnitType.Zerg_Extractor);

        if (hasExtractor) {
            return false;
        }

        long knownLings = enemyUnits.stream().filter(eu -> eu.getEnemyType() == UnitType.Zerg_Zergling).count();

        if (knownLings >= 8 && time.greaterThan(new Time(2,40)) && time.lessThanOrEqual(new Time(3,15))) {
            return true;
        }
        else if (knownLings >= 6 && time.greaterThan(new Time(2,30)) && time.lessThanOrEqual(new Time(3,15))) {
            return true;
        }

        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyPosition() == null) {
                continue;
            }

            //Refine later to prevent false positives
           if (enemyUnit.getEnemyType() == UnitType.Zerg_Spawning_Pool) {
               Time poolCompletion = new Time(time.getFrames() + enemyUnit.getEnemyUnit().getRemainingBuildTime());
               if (poolCompletion.lessThanOrEqual(new Time(2, 10))) {
                    return true;
               }
           }
            if (enemyUnit.getEnemyType() == UnitType.Zerg_Zergling) {
                if (mapInfo.getStartingBase().getCenter().getDistance(enemyUnit.getEnemyPosition()) < 1200
                && time.lessThanOrEqual(new Time(3,0))) {
                    return true;
                }
            }
        }
        return false;
    }

    public void buildingResponse() {
        getBuildingResponse().add(UnitType.Terran_Bunker);
        getBuildingResponse().add(UnitType.Terran_Marine);
        getBuildingResponse().add(UnitType.Terran_Marine);
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
