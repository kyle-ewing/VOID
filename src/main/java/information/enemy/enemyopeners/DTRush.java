package information.enemy.enemyopeners;

import java.util.HashMap;
import java.util.HashSet;

import bwapi.UnitType;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildType;
import util.Time;

public class DTRush extends EnemyStrategy{
    public DTRush() {
        super("Dark Templar");

        buildingResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        if (enemyUnits.stream().anyMatch(unit -> unit.getEnemyType() == UnitType.Protoss_Dragoon)
                && time.lessThanOrEqual(new Time(5, 30))
                && time.greaterThan(new Time(3, 30))) {
            return false;
        }
        
        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyType() == UnitType.Protoss_Templar_Archives) {
                if (time.lessThanOrEqual(new Time(4,0))) {
                    return true;
                }
            }

            if (enemyUnit.getEnemyType() == UnitType.Protoss_Citadel_of_Adun) {
                if (time.lessThanOrEqual(new Time(3,20))) {
                    return true;
                }
            }

            if (enemyUnit.getEnemyType() == UnitType.Protoss_Dark_Templar) {
                if (time.lessThanOrEqual(new Time(5,30))) {
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
        getBuildingResponse().add(UnitType.Terran_Marine);
        getBuildingResponse().add(UnitType.Terran_Marine);
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
