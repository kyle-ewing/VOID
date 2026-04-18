package information.enemy.enemyopeners;

import java.util.HashMap;
import java.util.HashSet;

import bwapi.TechType;
import bwapi.TilePosition;
import bwapi.UnitType;
import bwem.Base;
import information.MapInfo;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildType;
import util.Time;

public class FFE extends EnemyStrategy{
    private MapInfo mapInfo;

    public FFE(MapInfo mapInfo) {
        super("FFE");

        this.mapInfo = mapInfo;

        buildingResponse();
        upgradeResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        if (time.greaterThan(new Time(3,30))) {
            return false;
        }

        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyPosition() == null) {
                continue;
            }

            if (enemyUnit.getEnemyType() == UnitType.Protoss_Forge) {
                for (Base startingBase : mapInfo.getStartingBases()) {
                    if (startingBase == mapInfo.getStartingBase()) {
                        continue;
                    }

                    HashSet<TilePosition> baseTiles = mapInfo.getBaseTilesAllBases().get(startingBase);                            
                    if (baseTiles != null && baseTiles.contains(enemyUnit.getEnemyPosition().toTilePosition())) {                
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
