package information.enemy.enemyopeners;

import java.util.HashMap;
import java.util.HashSet;

import bwapi.TechType;
import bwapi.UnitType;
import information.MapInfo;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildType;
import util.Time;

public class DragoonRangeRush extends EnemyStrategy {
    private MapInfo mapInfo;

    public DragoonRangeRush(MapInfo mapInfo) {
        super(EnemyStrategyName.DRAGOONRANGERUSH);
        this.mapInfo = mapInfo;

        buildingResponse();
        techUpgradeResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        int dragoonCount = 0;

        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyType() == UnitType.Protoss_Cybernetics_Core && enemyUnit.getEnemyUnit().isVisible() && enemyUnit.getEnemyUnit().isUpgrading()) {
                if (time.lessThanOrEqual(new Time(4,30))) {
                    return true;
                }
            }

            if (enemyUnit.getEnemyType() == UnitType.Protoss_Dragoon) {
                dragoonCount++;

                if (dragoonCount >= 2 && time.lessThanOrEqual(new Time(4,45))) {
                    return true;
                }
            }
        }

        return false;
    }

    public void buildingResponse() {
        getBuildingResponse().add(UnitType.Terran_Refinery);
        getBuildingResponse().add(UnitType.Terran_Factory);
        getBuildingResponse().add(UnitType.Terran_Machine_Shop);
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
