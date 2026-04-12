package information.enemy.enemyopeners;

import java.util.HashMap;
import java.util.HashSet;

import bwapi.TechType;
import bwapi.UnitType;
import information.MapInfo;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildType;
import util.Time;

public class CannonRush extends EnemyStrategy {
    private MapInfo mapInfo;

    public CannonRush(MapInfo mapInfo) {
        super("Cannon Rush");
        this.mapInfo = mapInfo;

        buildingResponse();
        techUpgradeResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyPosition() == null ) {
                continue;
            }

            if (mapInfo.getBaseTiles().contains(enemyUnit.getEnemyPosition().toTilePosition()) || mapInfo.getNaturalTiles().contains(enemyUnit.getEnemyPosition().toTilePosition())) {
                if (enemyUnit.getEnemyType() == UnitType.Protoss_Photon_Cannon || enemyUnit.getEnemyType() == UnitType.Protoss_Pylon) {
                    if (new Time(3, 30).greaterThan(time)) {
                        return true;
                    }
                }

            }
        }
        return false;
    }

    public void buildingResponse() {
        getBuildingResponse().add(UnitType.Terran_Bunker);
        getBuildingResponse().add(UnitType.Terran_Refinery);
        getBuildingResponse().add(UnitType.Terran_Factory);
        getBuildingResponse().add(UnitType.Terran_Machine_Shop);
        getBuildingResponse().add(UnitType.Terran_Marine);
        getBuildingResponse().add(UnitType.Terran_Marine);
        getBuildingResponse().add(UnitType.Terran_Marine);
        getBuildingResponse().add(UnitType.Terran_Marine);
    }

    public HashMap<UnitType, Integer> getMoveOutCondition(BuildType buildType, Time time) {
        return new HashMap<>();
    }

    public void upgradeResponse() {
    }
    
    @Override
    public void techUpgradeResponse() {
        getTechUpgradeResponse().add(TechType.Tank_Siege_Mode);
    }


    public HashSet<UnitType> removeBuildings() {
        return new HashSet<>();
    }
}
