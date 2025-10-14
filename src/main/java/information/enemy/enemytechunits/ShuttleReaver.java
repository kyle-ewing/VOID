package information.enemy.enemytechunits;

import bwapi.UnitType;
import information.enemy.EnemyUnits;

import java.util.HashSet;

public class ShuttleReaver extends EnemyTechUnits {
    public ShuttleReaver() {
        super("Shuttle Reaver", UnitType.Terran_Wraith);


    }

    public boolean isEnemyTechUnit(HashSet<EnemyUnits> enemyUnits) {
        for(EnemyUnits enemyUnit : enemyUnits) {
            if(enemyUnit.getEnemyType() == null) {
                continue;
            }

            if(enemyUnit.getEnemyType() == UnitType.Protoss_Shuttle) {
                return true;
            }
        }
        return false;
    }

    public void techBuildingResponse() {
        getFriendlyBuildingResponse().add(UnitType.Terran_Starport);
    }

    public UnitType techUnitResponse() {
        return UnitType.Terran_Wraith;
    }
}
