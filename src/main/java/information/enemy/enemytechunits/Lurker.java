package information.enemy.enemytechunits;

import bwapi.TechType;
import bwapi.UnitType;
import bwapi.UpgradeType;
import information.enemy.EnemyUnits;
import planner.PlannedItem;
import planner.PlannedItemStatus;
import planner.PlannedItemType;

import java.util.HashSet;

public class Lurker extends EnemyTechUnits {
    public Lurker() {
        super("Lurker", UnitType.Terran_Science_Vessel, false);
    }

    public boolean isEnemyTechUnit(HashSet<EnemyUnits> enemyUnits) {
        for(EnemyUnits enemyUnit : enemyUnits) {
            if(enemyUnit.getEnemyType() == null) {
                continue;
            }

            if(enemyUnit.getEnemyType() == UnitType.Zerg_Lurker) {
                return true;
            }
        }
        return false;
    }

    public void techBuildingResponse() {
        getFriendlyBuildingResponse().add(UnitType.Terran_Missile_Turret);
        getFriendlyBuildingResponse().add(UnitType.Terran_Factory);
        getFriendlyBuildingResponse().add(UnitType.Terran_Starport);
        getFriendlyBuildingResponse().add(UnitType.Terran_Control_Tower);
        getFriendlyBuildingResponse().add(UnitType.Terran_Science_Facility);
    }

    public void techUpgradeResponse() {
        getFriendlyUpgradeResponse().add(new PlannedItem(TechType.Irradiate, 0, PlannedItemStatus.NOT_STARTED, PlannedItemType.UPGRADE, UnitType.Terran_Science_Facility, 1));
    }
}
