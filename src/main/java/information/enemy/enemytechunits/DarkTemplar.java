package information.enemy.enemytechunits;

import java.util.HashSet;

import bwapi.UnitType;
import information.enemy.EnemyUnits;

public class DarkTemplar extends EnemyTechUnits {
    public DarkTemplar() {
        super("Dark Templar", UnitType.Terran_Science_Vessel, false);
    }

    public boolean isEnemyTechUnit(HashSet<EnemyUnits> enemyUnits) {
        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyType() == null) {
                continue;
            }

            if (enemyUnit.getEnemyType() == UnitType.Protoss_Dark_Templar) {
                return true;
            }
        }
        return false;
    }

    public void techBuildingResponse() {
        getFriendlyBuildingResponse().add(UnitType.Terran_Engineering_Bay);
        getFriendlyBuildingResponse().add(UnitType.Terran_Missile_Turret);
        getFriendlyBuildingResponse().add(UnitType.Terran_Factory);
        getFriendlyBuildingResponse().add(UnitType.Terran_Starport);
        getFriendlyBuildingResponse().add(UnitType.Terran_Control_Tower);
        getFriendlyBuildingResponse().add(UnitType.Terran_Science_Facility);
    }

    public void techUpgradeResponse() {
    }

}
