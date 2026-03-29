package information.enemy.enemytechbuildings.protoss;

import java.util.HashSet;

import bwapi.UnitType;
import information.enemy.EnemyUnits;
import information.enemy.enemytechbuildings.EnemyTechBuilding;

public class FleetBeacon extends EnemyTechBuilding {
    public FleetBeacon() {
        super("Fleet Beacon", UnitType.Protoss_Fleet_Beacon);
    }

    @Override
    public boolean isEnemyBuilding(HashSet<EnemyUnits> enemyUnits) {
        return enemyUnits.stream().anyMatch(unit -> unit.getEnemyType() == buildingType);
    }

    @Override
    public void friendlyBuildingResponse() {
        friendlyBuildingResponse.add(UnitType.Terran_Factory);
        friendlyBuildingResponse.add(UnitType.Terran_Armory);
    }

}
