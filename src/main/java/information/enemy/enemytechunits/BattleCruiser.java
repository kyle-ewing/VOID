package information.enemy.enemytechunits;

import bwapi.UnitType;
import bwapi.UpgradeType;
import information.enemy.EnemyUnits;
import planner.PlannedItem;
import planner.PlannedItemStatus;
import planner.PlannedItemType;

import java.util.HashSet;

public class BattleCruiser extends EnemyTechUnits {
    public BattleCruiser() {
        super("BattleCruiser", UnitType.Terran_Goliath);
    }

    public boolean isEnemyTechUnit(HashSet<EnemyUnits> enemyUnits) {
        for(EnemyUnits enemyUnit : enemyUnits) {
            if(enemyUnit.getEnemyType() == null) {
                continue;
            }

            if(enemyUnit.getEnemyType() == UnitType.Terran_Battlecruiser || enemyUnit.getEnemyType() == UnitType.Terran_Physics_Lab) {
                return true;
            }
        }
        return false;
    }

    public void techBuildingResponse() {
        getFriendlyBuildingResponse().add(UnitType.Terran_Armory);
    }

    public void techUpgradeResponse() {
        getFriendlyUpgradeResponse().add(new PlannedItem(UpgradeType.Charon_Boosters, 1, PlannedItemStatus.NOT_STARTED, PlannedItemType.UPGRADE, UnitType.Terran_Factory, 1, 1));
    }
}
