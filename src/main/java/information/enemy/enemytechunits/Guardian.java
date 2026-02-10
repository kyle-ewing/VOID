package information.enemy.enemytechunits;

import bwapi.UnitType;
import information.enemy.EnemyUnits;

import java.util.HashSet;

public class Guardian extends EnemyTechUnits {
    public Guardian() {
        super("Guardian", UnitType.Terran_Wraith, true);
    }

    public boolean isEnemyTechUnit(HashSet<EnemyUnits> enemyUnits) {
        return enemyUnits.stream().anyMatch(eu -> eu.getEnemyType() == UnitType.Zerg_Guardian);
    }

    public void techBuildingResponse() {
        getFriendlyBuildingResponse().add(UnitType.Terran_Factory);
        getFriendlyBuildingResponse().add(UnitType.Terran_Starport);
    }

    public void techUpgradeResponse() {
    }
}
