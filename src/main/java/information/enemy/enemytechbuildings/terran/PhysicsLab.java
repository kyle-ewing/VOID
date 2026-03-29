package information.enemy.enemytechbuildings.terran;

import java.util.HashSet;

import bwapi.UnitType;
import information.enemy.EnemyUnits;
import information.enemy.enemytechbuildings.EnemyTechBuilding;

public class PhysicsLab extends EnemyTechBuilding {
    public PhysicsLab() {
        super("Physics Lab", UnitType.Terran_Physics_Lab);
    }

    public boolean isEnemyBuilding(HashSet<EnemyUnits> enemyUnits) {
        return enemyUnits.stream().anyMatch(unit -> unit.getEnemyType() == buildingType);
    }

    public void friendlyBuildingResponse() {

    }
}
