package information.enemy.enemytechbuildings.terran;

import java.util.HashSet;

import bwapi.UnitType;
import information.enemy.EnemyUnits;
import information.enemy.enemytechbuildings.EnemyTechBuilding;

public class NuclearSilo extends EnemyTechBuilding {
    public NuclearSilo() {
        super("Nuclear Silo", UnitType.Terran_Nuclear_Silo);
    }

    public boolean isEnemyBuilding(HashSet<EnemyUnits> enemyUnits) {
        return enemyUnits.stream().anyMatch(unit -> unit.getEnemyType() == buildingType);
    }

    public void friendlyBuildingResponse() {

    }
}
