package information.enemy.enemyopeners;

import bwapi.UnitType;
import bwem.Base;
import information.BaseInfo;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildType;
import util.Time;

import java.util.HashMap;
import java.util.HashSet;

//Stone Cold Steve Austin's favorite strategy
public class SCVRush extends EnemyStrategy {
    private BaseInfo baseInfo;

    public SCVRush(BaseInfo baseInfo) {
        super("SCV Rush");
        this.baseInfo = baseInfo;
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        int scvAtBase = 0;

        for(EnemyUnits enemyUnit : enemyUnits) {
            if(enemyUnit.getEnemyType() == null || enemyUnit.getEnemyPosition() == null ) {
                continue;
            }

            if(enemyUnit.getEnemyType() == UnitType.Terran_SCV) {
                if(baseInfo.getStartingBase().getCenter().getDistance(enemyUnit.getEnemyPosition()) < 500) {
                    scvAtBase++;
                }
            }
        }

        if(scvAtBase >= 3 && time.lessThanOrEqual(new Time(3, 0))) {
            return true;
        }
        else {
            return enemyUnits.stream().filter(eu -> eu.getEnemyType() == UnitType.Terran_SCV).count() >= 2
                    && time.lessThanOrEqual(new Time(1, 0));
        }

    }

    public void buildingResponse() {
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
