package information.enemy.enemyopeners;

import java.util.HashMap;
import java.util.HashSet;

import bwapi.TechType;
import bwapi.TilePosition;
import bwapi.UnitType;
import map.bwemwrappers.Base;
import map.bwemwrappers.ChokePoint;
import information.MapInfo;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildType;
import util.Time;

public class FFE extends EnemyStrategy{
    private MapInfo mapInfo;

    public FFE(MapInfo mapInfo) {
        super(EnemyStrategyName.FFE);

        this.mapInfo = mapInfo;

        buildingResponse();
        upgradeResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        if (time.greaterThan(new Time(3,40))) {
            return false;
        }

        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyPosition() == null) {
                continue;
            }

            if (enemyUnit.getEnemyType() == UnitType.Protoss_Forge || enemyUnit.getEnemyType() == UnitType.Protoss_Photon_Cannon) {
                if (mapInfo.getBaseTiles().contains(enemyUnit.getEnemyPosition().toTilePosition()) || mapInfo.getNaturalTiles().contains(enemyUnit.getEnemyPosition().toTilePosition())) {
                    return false;
                }

                for (Base startingBase : mapInfo.getStartingBases()) {
                    if (startingBase == mapInfo.getStartingBase()) {
                        continue;
                    }

                    HashSet<TilePosition> baseTiles = mapInfo.getBaseTilesAllBases().get(startingBase);
                    if (baseTiles != null && baseTiles.contains(enemyUnit.getEnemyPosition().toTilePosition())) {
                        ChokePoint mainChoke = mapInfo.getStartingBaseMainChoke(startingBase);
                        if (mainChoke != null && enemyUnit.getEnemyPosition().getDistance(mainChoke.getCenter()) < 500) {
                            continue;
                        }

                        return false;
                    }
                }
                return true;
            }
        }

        return false;
    }

    public void buildingResponse() {
        getBuildingResponse().add(UnitType.Terran_Siege_Tank_Tank_Mode);
        getBuildingResponse().add(UnitType.Terran_Siege_Tank_Tank_Mode);
    }

    public HashMap<UnitType, Integer> getMoveOutCondition(BuildType buildType, Time time) {
        HashMap<UnitType, Integer> moveOutCondition = new HashMap<>();

        if (time.lessThanOrEqual(new Time(9,0))) {
            moveOutCondition.put(UnitType.Terran_Siege_Tank_Tank_Mode, 1);
        }

        return moveOutCondition;
    }

    public void upgradeResponse() {
        getTechUpgradeResponse().add(TechType.Tank_Siege_Mode);
    }

    public HashSet<UnitType> removeBuildings() {
        HashSet<UnitType> removeBuildings = new HashSet<>();
        removeBuildings.add(UnitType.Terran_Bunker);
        removeBuildings.add(UnitType.Terran_Missile_Turret);
        return removeBuildings;
    }
}
