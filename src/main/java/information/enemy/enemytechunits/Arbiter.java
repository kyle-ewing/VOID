package information.enemy.enemytechunits;

import bwapi.UnitType;
import information.enemy.EnemyUnits;

import java.util.HashSet;

public class Arbiter extends EnemyTechUnits {
    public Arbiter() {
        super("Arbiter", UnitType.Protoss_Arbiter);
    }

    public boolean isEnemyTechUnit(HashSet<EnemyUnits> enemyUnits) {
        for(EnemyUnits enemyUnit : enemyUnits) {
            if(enemyUnit.getEnemyType() == null) {
                continue;
            }

            if(enemyUnit.getEnemyType() == bwapi.UnitType.Protoss_Arbiter) {
                return true;
            }
        }
        return false;
    }

    public void techBuildingResponse() {
        getFriendlyBuildingResponse().add(UnitType.Terran_Starport);
        getFriendlyBuildingResponse().add(bwapi.UnitType.Terran_Science_Facility);
    }

    public UnitType techUnitResponse() {
        return UnitType.Terran_Science_Vessel;
    }
}
