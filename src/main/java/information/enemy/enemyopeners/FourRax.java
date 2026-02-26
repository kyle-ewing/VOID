package information.enemy.enemyopeners;

import bwapi.UnitType;
import bwem.Base;
import information.BaseInfo;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildType;
import util.Time;

import java.util.HashMap;
import java.util.HashSet;

//Why Dave?
public class FourRax extends  EnemyStrategy {
    private BaseInfo baseInfo;
    private boolean isStartingBase = false;

    public FourRax(BaseInfo baseInfo) {
        super("Four Rax");
        this.baseInfo = baseInfo;

        buildingResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        for(EnemyUnits enemyUnit : enemyUnits) {
            if(enemyUnit.getEnemyType() == null || enemyUnit.getEnemyPosition() == null ) {
                continue;
            }

            if(enemyUnit.getEnemyType() == UnitType.Terran_Barracks && enemyUnit.getEnemyUnit().isCompleted() && time.lessThanOrEqual(new Time(2, 0))) {
                return true;
            }

            if(enemyUnit.getEnemyType() == UnitType.Terran_Marine
                    && enemyUnits.stream().filter(type -> type.getEnemyType() == UnitType.Terran_SCV).count() <= 10
                    && (time.lessThanOrEqual(new Time(2, 20))
                    || (enemyUnit.getEnemyUnit().getDistance(baseInfo.getStartingBase().getCenter()) < 1500) && time.lessThanOrEqual(new Time(2, 50))
                    || enemyUnits.stream().filter(type -> type.getEnemyType() == UnitType.Terran_Marine).count() >= 2 && time.lessThanOrEqual(new Time(2, 45))
                    || enemyUnits.stream().filter(type -> type.getEnemyType() == UnitType.Terran_Marine).count() >= 3 && time.lessThanOrEqual(new Time(3, 5)))) {
                    return true;
            }
        }
        return false;
    }

    public void buildingResponse() {
        getBuildingResponse().add(UnitType.Terran_Bunker);
        getBuildingResponse().add(UnitType.Terran_Vulture);
        getBuildingResponse().add(UnitType.Terran_Vulture);

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
