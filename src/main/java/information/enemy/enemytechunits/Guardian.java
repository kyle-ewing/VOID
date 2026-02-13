package information.enemy.enemytechunits;

import bwapi.UnitType;
import information.enemy.EnemyUnits;

import java.util.HashSet;

public class Guardian extends EnemyTechUnits {
    public Guardian() {
        super("Guardian", UnitType.Terran_Wraith, true);
    }

    public boolean isEnemyTechUnit(HashSet<EnemyUnits> enemyUnits) {
        for(EnemyUnits enemyUnit : enemyUnits) {
            if(enemyUnit.getEnemyType() == null) {
                continue;
            }

            if(enemyUnit.getEnemyType() == UnitType.Zerg_Guardian) {
                return true;
            }
        }
        return false;
    }

    public void techBuildingResponse() {
        getFriendlyBuildingResponse().add(UnitType.Terran_Factory);
        getFriendlyBuildingResponse().add(UnitType.Terran_Starport);
    }

    public void techUpgradeResponse() {
    }
}
