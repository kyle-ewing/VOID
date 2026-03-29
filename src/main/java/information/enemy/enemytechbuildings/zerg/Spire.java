package information.enemy.enemytechbuildings.zerg;

import java.util.HashSet;

import bwapi.UnitType;
import information.enemy.EnemyUnits;
import information.enemy.enemytechbuildings.EnemyTechBuilding;

public class Spire extends EnemyTechBuilding {
    public Spire() {
        super("Spire", UnitType.Zerg_Spire);
    }

    public boolean isEnemyBuilding(HashSet<EnemyUnits> enemyUnits) {
        return enemyUnits.stream().anyMatch(unit -> unit.getEnemyType() == buildingType);
    }

    public void friendlyBuildingResponse() {
        friendlyBuildingResponse.add(UnitType.Terran_Engineering_Bay);
    }
}
