package information.enemy.enemytechbuildings.terran;

import java.util.HashSet;

import bwapi.UnitType;
import information.enemy.EnemyUnits;
import information.enemy.enemytechbuildings.EnemyTechBuilding;

public class CovertOps extends EnemyTechBuilding {
    public CovertOps() {
        super("Covert Ops", UnitType.Terran_Covert_Ops);
    }

    public boolean isEnemyBuilding(HashSet<EnemyUnits> enemyUnits) {
        return enemyUnits.stream().anyMatch(unit -> unit.getEnemyType() == buildingType);
    }

    public void friendlyBuildingResponse() {
        friendlyBuildingResponse.add(UnitType.Terran_Academy);
        friendlyBuildingResponse.add(UnitType.Terran_Engineering_Bay);
    }
}
