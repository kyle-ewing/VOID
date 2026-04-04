package information.enemy.enemytechunits;

import bwapi.TechType;
import bwapi.UnitType;
import bwapi.UpgradeType;
import information.enemy.EnemyUnits;
import planner.PlannedItem;
import planner.PlannedItemType;

import java.util.ArrayList;
import java.util.HashSet;

public class SiegeTank extends EnemyTechUnits {
    public SiegeTank() {
        super("Siege Tank", UnitType.Terran_Wraith, false);


    }

    public boolean isEnemyTechUnit(HashSet<EnemyUnits> enemyUnits) {
        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyType() == null) {
                continue;
            }

            if (enemyUnit.getEnemyType() == UnitType.Terran_Siege_Tank_Tank_Mode || enemyUnit.getEnemyType() == UnitType.Terran_Siege_Tank_Siege_Mode) {
                return true;
            }
        }
        return false;
    }

    public void techBuildingResponse() {
        getFriendlyBuildingResponse().add(UnitType.Terran_Starport);
        getFriendlyBuildingResponse().add(UnitType.Terran_Control_Tower);
    }

    public void techUpgradeResponse() {
        getFriendlyUpgradeResponse().add(new PlannedItem(TechType.Cloaking_Field, 0, PlannedItemType.UPGRADE, UnitType.Terran_Control_Tower, 2));
    }
}
