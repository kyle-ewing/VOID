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

        buildingResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyPosition() == null) {
                continue;
            }

            //Refine later to prevent false positives
//            if (enemyUnit.getEnemyType() == UnitType.Zerg_Spawning_Pool) {
//                if (time.lessThanOrEqual(new Time(2,30))) {
//                    if (enemyUnit.getEnemyUnit().isCompleted()) {
//                        return true;
//                    }
//                }
//            }
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
