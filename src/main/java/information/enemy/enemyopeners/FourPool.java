package information.enemy.enemyopeners;

import bwapi.UnitType;
import information.BaseInfo;
import information.enemy.EnemyUnits;
import util.Time;

import java.util.HashMap;
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
                if(time.lessThanOrEqual(new Time(2,0))) {
                    if(enemyUnit.getEnemyUnit().isCompleted()) {
                        return true;
                    }
                }
            }
            else if(enemyUnit.getEnemyType() == UnitType.Zerg_Zergling) {
                if(time.lessThanOrEqual(new Time(2,30))) {
                    if(enemyUnit.getEnemyPosition().getDistance(baseInfo.getStartingBase().getCenter()) < 2000) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void buildingResponse() {
        getBuildingResponse().add(UnitType.Terran_Bunker);
    }

    public HashMap<UnitType, Integer> getMoveOutCondition() {
        return new HashMap<>();
    }

    public HashSet<UnitType> removeBuildings() {
        return new HashSet<>();
    }
}
