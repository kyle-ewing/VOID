package information.enemy.enemytechbuildings.protoss;

import java.util.HashSet;

import bwapi.UnitType;
import information.enemy.EnemyUnits;
import information.enemy.enemytechbuildings.EnemyTechBuilding;

public class RoboSupportBay extends EnemyTechBuilding {
    public RoboSupportBay() {
        super("Robotics Support Bay", UnitType.Protoss_Robotics_Support_Bay);
    }

    public boolean isEnemyBuilding(HashSet<EnemyUnits> enemyUnits) {
        return enemyUnits.stream().anyMatch(unit -> unit.getEnemyType() == buildingType);
    }

    public void friendlyBuildingResponse() {
        friendlyBuildingResponse.add(UnitType.Terran_Starport);
    }

}
