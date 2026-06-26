package information.enemy.enemytechunits;

import java.util.Arrays;
import java.util.HashSet;

import bwapi.UnitType;
import bwapi.UpgradeType;
import information.enemy.EnemyUnits;
import planner.PlannedItem;
import planner.PlannedItemType;

public class Mutalisk extends EnemyTechUnits {
    public Mutalisk() {
        super("Mutalisk", true,
                Arrays.asList(UnitType.Terran_Marine, UnitType.Terran_Valkyrie),
                Arrays.asList(UnitType.Terran_Goliath, UnitType.Terran_Valkyrie));
    }

    public boolean isEnemyTechUnit(HashSet<EnemyUnits> enemyUnits) {
        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyType() == null) {
                continue;
            }

            if (enemyUnit.getEnemyType() == UnitType.Zerg_Mutalisk) {
                return true;
            }
        }
        return false;
    }

    public void techBuildingResponse() {
        getFriendlyBuildingResponse().add(UnitType.Terran_Engineering_Bay);
        getFriendlyBuildingResponse().add(UnitType.Terran_Armory);
    }

    public void techUpgradeResponse() {
        getFriendlyUpgradeResponse().add(new PlannedItem(UpgradeType.Charon_Boosters, 1, PlannedItemType.UPGRADE, UnitType.Terran_Factory, 1, 1));
    }
}
