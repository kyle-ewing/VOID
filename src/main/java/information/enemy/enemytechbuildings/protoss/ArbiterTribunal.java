package information.enemy.enemytechbuildings.protoss;

import java.util.HashSet;

import bwapi.UnitType;
import information.enemy.EnemyUnits;
import information.enemy.enemytechbuildings.EnemyTechBuilding;

public class ArbiterTribunal extends EnemyTechBuilding {
    public ArbiterTribunal() {
        super("Arbiter Tribunal", UnitType.Protoss_Arbiter_Tribunal);
    }

    @Override
    public boolean isEnemyBuilding(HashSet<EnemyUnits> enemyUnits) {
        return enemyUnits.stream().anyMatch(unit -> unit.getEnemyType() == buildingType);
    }

    @Override
    public void friendlyBuildingResponse() {
        friendlyBuildingResponse.add(UnitType.Terran_Factory);
        friendlyBuildingResponse.add(UnitType.Terran_Starport);
        friendlyBuildingResponse.add(UnitType.Terran_Science_Facility);
    }

}
