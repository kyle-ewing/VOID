package information.enemy.enemyopeners;

import bwapi.UnitType;
import information.enemy.EnemyUnits;
import util.Time;

import java.util.HashSet;

public class FourPool extends EnemyStrategy {
    public FourPool() {
        super("Four Pool");

        buildingResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        for(EnemyUnits enemyUnit : enemyUnits) {
            if(enemyUnit.getEnemyType() == UnitType.Zerg_Spawning_Pool) {
                if(time.lessThanOrEqual(new Time(1,20))) {
                    return true;
                }
            }
            else if(enemyUnit.getEnemyType() == UnitType.Zerg_Zergling) {
                if(time.lessThanOrEqual(new Time(2,30))) {
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
