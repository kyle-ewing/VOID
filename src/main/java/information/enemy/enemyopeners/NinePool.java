package information.enemy.enemyopeners;

import bwapi.UnitType;
import information.MapInfo;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildType;
import util.Time;

import java.util.HashMap;
import java.util.HashSet;

public class NinePool extends EnemyStrategy {
    private MapInfo mapInfo;

    public NinePool(MapInfo mapInfo) {
        super("Nine Pool");
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

    public HashMap<UnitType, Integer> getMoveOutCondition(BuildType buildType, Time time) {
        return new HashMap<>();
    }

    public HashSet<UnitType> removeBuildings() {
        return new HashSet<>();
    }
}
