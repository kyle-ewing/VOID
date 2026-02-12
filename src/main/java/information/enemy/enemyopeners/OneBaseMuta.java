package information.enemy.enemyopeners;

import bwapi.UnitType;
import information.BaseInfo;
import information.enemy.EnemyUnits;
import util.Time;

import java.util.HashMap;
import java.util.HashSet;

public class OneBaseMuta extends EnemyStrategy {
    private BaseInfo baseInfo;

    public OneBaseMuta(BaseInfo baseInfo) {
        super("One Base Muta");
        this.baseInfo = baseInfo;

        buildingResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        for(EnemyUnits enemyUnit : enemyUnits) {
            if(enemyUnit.getEnemyPosition() == null) {
                continue;
            }

            if(enemyUnit.getEnemyType() == UnitType.Zerg_Spire) {
                if(time.lessThanOrEqual(new Time(5,0))
                    && enemyUnits.stream().map(EnemyUnits::getEnemyType).filter(et -> et == UnitType.Zerg_Hatchery || et == UnitType.Zerg_Lair ).count() == 1) {
                    return true;
                }
            }
        }
        return false;
    }

    public void buildingResponse() {
        getBuildingResponse().add(UnitType.Terran_Engineering_Bay);
        getBuildingResponse().add(UnitType.Terran_Missile_Turret);
        getBuildingResponse().add(UnitType.Terran_Missile_Turret);
        getBuildingResponse().add(UnitType.Terran_Missile_Turret);
        getBuildingResponse().add(UnitType.Terran_Missile_Turret);
    }

    public HashMap<UnitType, Integer> getMoveOutCondition() {
        return new HashMap<>();
    }

    public HashSet<UnitType> removeBuildings() {
        return new HashSet<>();
    }
}
