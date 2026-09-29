package information.enemy.enemytechunits;

import java.util.HashSet;

import bwapi.UnitType;
import information.enemy.EnemyUnits;

public class ShuttleReaver extends EnemyTechUnits {
    public ShuttleReaver() {
        super("Shuttle Reaver", UnitType.Terran_Wraith, true);

        buildingResponse();
    }

    public boolean isEnemyTechUnit(HashSet<EnemyUnits> enemyUnits) {
        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyType() == null) {
                continue;
            }

            if (enemyUnit.getEnemyType() == UnitType.Protoss_Shuttle) {
                return true;
            }
        }
        return false;
    }

    public void buildingResponse() {
        getFriendlyBuildingResponse().add(UnitType.Terran_Starport);
    }

    public void techBuildingResponse() {
    }

    public void techUpgradeResponse() {
    }

    @Override
    public boolean mineralLineTurretsOnly() {
        return true;
    }
}
