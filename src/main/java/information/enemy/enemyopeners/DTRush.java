package information.enemy.enemyopeners;

import bwapi.UnitType;
import information.enemy.EnemyUnits;
import util.Time;

import java.util.HashMap;
import java.util.HashSet;

public class DTRush extends EnemyStrategy{
    public DTRush() {
        super("Dark Templar");

        buildingResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        for(EnemyUnits enemyUnit : enemyUnits) {
            if(enemyUnit.getEnemyType() == UnitType.Protoss_Templar_Archives) {
                if(time.lessThanOrEqual(new Time(5,30))) {
                    return true;
                }
            }

            if(enemyUnit.getEnemyType() == UnitType.Protoss_Citadel_of_Adun) {
                if(time.lessThanOrEqual(new Time(5,0))) {
                    return true;
                }
            }

            if(enemyUnit.getEnemyType() == UnitType.Protoss_Dark_Templar) {
                if(time.lessThanOrEqual(new Time(7,30))) {
                    return true;
                }
            }
        }
        return false;
    }

    public void buildingResponse() {
        getBuildingResponse().add(UnitType.Terran_Refinery);
        getBuildingResponse().add(UnitType.Terran_Engineering_Bay);
        getBuildingResponse().add(UnitType.Terran_Academy);
        getBuildingResponse().add(UnitType.Terran_Comsat_Station);
        getBuildingResponse().add(UnitType.Terran_Missile_Turret);
        getBuildingResponse().add(UnitType.Terran_Factory);
        getBuildingResponse().add(UnitType.Terran_Starport);
        getBuildingResponse().add(UnitType.Terran_Control_Tower);
        getBuildingResponse().add(UnitType.Terran_Science_Facility);
    }

    public HashMap<UnitType, Integer> getMoveOutCondition(Time time) {
        return new HashMap<>();
    }

    public HashSet<UnitType> removeBuildings() {
        return new HashSet<>();
    }
}
