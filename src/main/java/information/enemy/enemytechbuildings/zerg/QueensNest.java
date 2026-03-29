package information.enemy.enemytechbuildings.zerg;

import java.util.HashSet;

import bwapi.UnitType;
import information.enemy.EnemyUnits;
import information.enemy.enemytechbuildings.EnemyTechBuilding;

public class QueensNest extends EnemyTechBuilding {
    public QueensNest() {
        super("Queens Nest", UnitType.Zerg_Queens_Nest);
    }

    public boolean isEnemyBuilding(HashSet<EnemyUnits> enemyUnits) {
        return enemyUnits.stream().anyMatch(unit -> unit.getEnemyType() == buildingType);
    }

    public void friendlyBuildingResponse() {
        friendlyBuildingResponse.add(UnitType.Terran_Starport);
    }
}
