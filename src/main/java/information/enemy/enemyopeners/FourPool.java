package information.enemy.enemyopeners;

import java.util.HashMap;
import java.util.HashSet;

import bwapi.UnitType;
import information.MapInfo;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildType;
import map.bwemwrappers.Area;
import util.Time;

public class FourPool extends EnemyStrategy {
    private MapInfo mapInfo;

    public FourPool(MapInfo mapInfo) {
        super(EnemyStrategyName.FOURPOOL);
        this.mapInfo = mapInfo;
        hardLockedWhenSeen = true;

        buildingResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        if (enemyUnits.stream().map(EnemyUnits::getEnemyType).filter(et -> et == UnitType.Zerg_Drone).count() > 5) {
            return false;
        }

        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyPosition() == null) {
                continue;
            }

            if (enemyUnit.getEnemyType() == UnitType.Zerg_Spawning_Pool) {
                if (enemyUnit.getEnemyUnit().isCompleted() && time.lessThanOrEqual(new Time(1,45))) {
                    return true;
                }
            }
            else if (enemyUnit.getEnemyType() == UnitType.Zerg_Zergling) {
                if (time.lessThanOrEqual(new Time(2,18))) {
                    return true;
                }

                if (time.lessThanOrEqual(new Time(2,30))) {
                    Area lingArea = mapInfo.getGameMap().getArea(enemyUnit.getEnemyPosition().toTilePosition());

                    if (lingArea != null) {
                        if (lingArea == mapInfo.getStartingBase().getArea() || lingArea == mapInfo.getNaturalBase().getArea()) {
                            return true;
                        }

                        if (!lingArea.isStartingArea() && !lingArea.isNaturalArea()) {
                            return true;
                        }
                    }
                }

                if (mapInfo.getNaturalBase().getCenter().getDistance(enemyUnit.getEnemyPosition()) < 1600
                && time.lessThanOrEqual(new Time(2,40))) {
                    return true;
                }

                if (mapInfo.getEnemyMain() == null) {
                    continue;
                }

                int travelFrames = (int) (mapInfo.getEnemyMain().getCenter().getDistance(enemyUnit.getEnemyPosition()) / 5.49);

                if (new Time(time.getFrames() - travelFrames).lessThanOrEqual(new Time(2,5))) {
                    return true;
                }
            }
        }
        return false;
    }

    public void buildingResponse() {
        getBuildingResponse().add(UnitType.Terran_Bunker);
        getBuildingResponse().add(UnitType.Terran_Marine); 
        getBuildingResponse().add(UnitType.Terran_Marine);
        getBuildingResponse().add(UnitType.Terran_Marine);
        getBuildingResponse().add(UnitType.Terran_Marine);
    }

    public void upgradeResponse() {
    }

    public HashMap<UnitType, Integer> getMoveOutCondition(BuildType buildType, Time time, HashSet<EnemyUnits> enemyUnits) {
        return new HashMap<>();
    }

    public HashSet<UnitType> removeBuildings() {
        return new HashSet<>();
    }
}
