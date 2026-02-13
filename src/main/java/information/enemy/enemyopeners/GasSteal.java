package information.enemy.enemyopeners;

import bwapi.UnitType;
import information.BaseInfo;
import information.enemy.EnemyUnits;
import util.Time;

import java.util.HashMap;
import java.util.HashSet;

public class GasSteal extends EnemyStrategy{
    private BaseInfo baseInfo;

    public GasSteal(BaseInfo baseInfo) {
        super("Gas Steal");

        this.baseInfo = baseInfo;
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        if(time.greaterThan(new Time(2,30))) {
            return false;
        }

        for(EnemyUnits enemyUnit : enemyUnits) {
            if(enemyUnit.getEnemyPosition() == null) {
                continue;
            }

            if(enemyUnit.getEnemyType().isRefinery()) {
                if(baseInfo.getBaseTiles().contains(enemyUnit.getEnemyPosition().toTilePosition())) {
                    priorityEnemyUnit = enemyUnit;
                    return true;
                }
            }
        }

        return false;
    }

    public void buildingResponse() {
    }

    public HashMap<UnitType, Integer> getMoveOutCondition(Time time) {
        HashMap<UnitType, Integer> moveOutCondition = new HashMap<>();

        if(time.lessThanOrEqual(new Time(3,30))) {
            moveOutCondition.put(UnitType.Terran_Marine, 2);
        }
        else if(time.lessThanOrEqual(new Time(5,0))) {
            moveOutCondition.put(UnitType.Terran_Marine, 4);
        }

        return moveOutCondition;
    }

    public HashSet<UnitType> removeBuildings() {
        HashSet<UnitType> removeBuildings = new HashSet<>();
        removeBuildings.add(UnitType.Terran_Bunker);
        removeBuildings.add(UnitType.Terran_Missile_Turret);
        return removeBuildings;
    }
}
