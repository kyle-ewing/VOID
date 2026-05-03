package information.enemy.enemytechunits;

import java.util.HashSet;

import bwapi.UnitType;
import bwapi.UpgradeType;
import information.enemy.EnemyUnits;
import planner.PlannedItem;
import planner.PlannedItemType;

public class Scout extends EnemyTechUnits {
    public Scout() {
        super("Scout", UnitType.Terran_Goliath, true);
    }



    public boolean isEnemyTechUnit(HashSet<EnemyUnits> enemyUnits) {
        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyType() == null) {
                continue;
            }

            if (enemyUnit.getEnemyType() == UnitType.Protoss_Scout) {
                return true;
            }
        }
        return false;
    }

    public void techBuildingResponse() {
        getFriendlyBuildingResponse().add(UnitType.Terran_Armory);
    }

    public void techUpgradeResponse() {
        getFriendlyUpgradeResponse().add(new PlannedItem(UpgradeType.Charon_Boosters, 1, PlannedItemType.UPGRADE, UnitType.Terran_Factory, 1, 1));
    }


}
