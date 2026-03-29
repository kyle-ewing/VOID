package information.enemy.enemytechbuildings.zerg;

import java.util.HashSet;

import bwapi.UnitType;
import information.enemy.EnemyUnits;
import information.enemy.enemytechbuildings.EnemyTechBuilding;

public class GreaterSpire extends EnemyTechBuilding {
    public GreaterSpire() {
        super("Greater Spire", UnitType.Zerg_Greater_Spire);
    }

    public boolean isEnemyBuilding(HashSet<EnemyUnits> enemyUnits) {
        return enemyUnits.stream().anyMatch(unit -> unit.getEnemyType() == buildingType);
    }

    public void friendlyBuildingResponse() {

    }
}
