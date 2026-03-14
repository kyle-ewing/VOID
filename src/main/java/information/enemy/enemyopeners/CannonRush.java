package information.enemy.enemyopeners;

import bwapi.UnitType;
import information.MapInfo;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildType;
import util.Time;

import java.util.HashMap;
import java.util.HashSet;

public class CannonRush extends EnemyStrategy {
    private MapInfo mapInfo;

    public CannonRush(MapInfo mapInfo) {
        super("Cannon Rush");
        this.mapInfo = mapInfo;

        buildingResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyPosition() == null ) {
                continue;
            }

            if (mapInfo.getBaseTiles().contains(enemyUnit.getEnemyPosition().toTilePosition()) && enemyUnit.getEnemyType() == UnitType.Protoss_Photon_Cannon) {
                if (new Time(3, 30).greaterThan(time)) {
                    return true;
                }
            }
            else if (mapInfo.getBaseTiles().contains(enemyUnit.getEnemyPosition().toTilePosition()) && enemyUnit.getEnemyType() == UnitType.Protoss_Pylon) {
                if (new Time(3, 30).greaterThan(time)) {
                    return true;
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
    }

    public HashMap<UnitType, Integer> getMoveOutCondition(BuildType buildType, Time time) {
        return new HashMap<>();
    }

    public void upgradeResponse() {
    }

    public HashSet<UnitType> removeBuildings() {
        return new HashSet<>();
    }
}
