package information.enemy.enemytechbuildings.protoss;

import java.util.HashSet;

import bwapi.UnitType;
import information.enemy.EnemyUnits;
import information.enemy.enemytechbuildings.EnemyTechBuilding;

public class CitadelOfAdun extends EnemyTechBuilding {
    public CitadelOfAdun() {
        super("Citadel of Adun", UnitType.Protoss_Citadel_of_Adun);
    }

    @Override
    public boolean isEnemyBuilding(HashSet<EnemyUnits> enemyUnits) {
        return enemyUnits.stream().anyMatch(unit -> unit.getEnemyType() == buildingType);
    }

    @Override
    public void friendlyBuildingResponse() {
        friendlyBuildingResponse.add(UnitType.Terran_Engineering_Bay);
        friendlyBuildingResponse.add(UnitType.Terran_Academy);
    }

}
