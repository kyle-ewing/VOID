package information.enemy.enemytechunits;

import bwapi.UnitType;
import information.enemy.EnemyUnits;
import planner.PlannedItem;

import java.util.ArrayList;
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
        getFriendlyBuildingResponse().add(UnitType.Terran_Science_Facility);
    }

    public void techUpgradeResponse() {
    }

    public UnitType techUnitResponse() {
        return UnitType.Terran_Science_Vessel;
    }
}
