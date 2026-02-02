package information.enemy.enemytechunits;

import bwapi.UnitType;
import bwapi.UpgradeType;
import information.enemy.EnemyUnits;
import planner.PlannedItem;
import planner.PlannedItemStatus;
import planner.PlannedItemType;

import java.util.HashSet;

public class Wraith extends EnemyTechUnits {
    public Wraith() {
        super("Wraith", UnitType.Terran_Goliath, true);
    }

    public boolean isEnemyTechUnit(HashSet<EnemyUnits> enemyUnits) {
        for(EnemyUnits enemyUnit : enemyUnits) {
            if(enemyUnit.getEnemyType() == null) {
                continue;
            }

            if(enemyUnit.getEnemyType() == UnitType.Terran_Wraith) {
                return true;
            }
        }
        return false;
    }

    public void techBuildingResponse() {
        getFriendlyBuildingResponse().add(UnitType.Terran_Armory);
        getFriendlyBuildingResponse().add(UnitType.Terran_Engineering_Bay);
        getFriendlyBuildingResponse().add(UnitType.Terran_Academy);
    }

    public void techUpgradeResponse() {
        getFriendlyUpgradeResponse().add(new PlannedItem(UpgradeType.Charon_Boosters, 1, PlannedItemStatus.NOT_STARTED, PlannedItemType.UPGRADE, UnitType.Terran_Factory, 1, 1));
    }
}
