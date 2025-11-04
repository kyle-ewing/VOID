package information.enemy.enemyopeners;

import bwapi.UnitType;
import information.BaseInfo;
import information.enemy.EnemyUnits;
import util.Time;

import java.util.HashSet;

public class FourPool extends EnemyStrategy {
    private BaseInfo baseInfo;

    public FourPool(BaseInfo baseInfo) {
        super("Four Pool");
        this.baseInfo = baseInfo;

        buildingResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        for(EnemyUnits enemyUnit : enemyUnits) {
            if(enemyUnit.getEnemyPosition() == null) {
                continue;
            }

            if(enemyUnit.getEnemyType() == UnitType.Zerg_Spawning_Pool) {
                if(time.lessThanOrEqual(new Time(1,20))) {
                    return true;
                }
            }
            else if(enemyUnit.getEnemyType() == UnitType.Zerg_Zergling) {
                if(time.lessThanOrEqual(new Time(2,30))) {
                    return true;
                }

                if(baseInfo.getStartingBase().getCenter().getDistance(enemyUnit.getEnemyPosition()) < 1000
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
}
