package information.enemy.enemytechbuildings.zerg;

import java.util.HashSet;

import bwapi.UnitType;
import information.enemy.EnemyUnits;
import information.enemy.enemytechbuildings.EnemyTechBuilding;

public class DefilerMound extends EnemyTechBuilding {
    public DefilerMound() {
        super("Defiler Mound", UnitType.Zerg_Defiler_Mound);
    }

    public boolean isEnemyBuilding(HashSet<EnemyUnits> enemyUnits) {
        return enemyUnits.stream().anyMatch(unit -> unit.getEnemyType() == buildingType);
    }

    public void friendlyBuildingResponse() {
        friendlyBuildingResponse.add(UnitType.Terran_Starport);
        friendlyBuildingResponse.add(UnitType.Terran_Science_Facility);
    }

}
