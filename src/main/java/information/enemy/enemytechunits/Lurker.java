package information.enemy.enemytechunits;

import java.util.HashSet;

import bwapi.TechType;
import bwapi.UnitType;
import information.enemy.EnemyUnits;
import planner.PlannedItem;
import planner.PlannedItemType;

public class Lurker extends EnemyTechUnits {
    public Lurker() {
        super("Lurker", UnitType.Terran_Science_Vessel, false);
    }

    public boolean isEnemyTechUnit(HashSet<EnemyUnits> enemyUnits) {
        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyType() == null) {
                continue;
            }

            if (enemyUnit.getEnemyType() == UnitType.Zerg_Lurker || enemyUnit.getEnemyType() == UnitType.Zerg_Lurker_Egg) {
                return true;
            }
        }
        return false;
    }

    public boolean isResearchTriggered(HashSet<EnemyUnits> enemyUnits) {
        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyType() != UnitType.Zerg_Hydralisk_Den) {
                continue;
            }

            if (enemyUnit.getEnemyUnit().isResearching()) {
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
        getFriendlyUpgradeResponse().add(new PlannedItem(TechType.Tank_Siege_Mode, 0, PlannedItemType.UPGRADE, UnitType.Terran_Machine_Shop, 1));
        getFriendlyUpgradeResponse().add(new PlannedItem(TechType.Irradiate, 0, PlannedItemType.UPGRADE, UnitType.Terran_Science_Facility, 4));
    }
}
