package information.enemy.enemytechbuildings.protoss;

import java.util.HashSet;

import bwapi.UnitType;
import bwapi.UpgradeType;
import information.enemy.EnemyUnits;
import information.enemy.enemytechbuildings.EnemyTechBuilding;
import planner.PlannedItem;
import planner.PlannedItemType;

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

    @Override
    public void friendlyUpgradeResponse() {
        friendlyUpgradeResponse.add(new PlannedItem(UpgradeType.Charon_Boosters, 1, PlannedItemType.UPGRADE, UnitType.Terran_Factory, 1, 1));
    }

}
