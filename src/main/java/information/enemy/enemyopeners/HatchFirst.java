package information.enemy.enemyopeners;

import java.util.HashMap;
import java.util.HashSet;

import bwapi.UnitType;
import information.MapInfo;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildType;
import map.bwemwrappers.Area;
import util.Time;

public class HatchFirst extends EnemyStrategy {
    private MapInfo mapInfo;

    public HatchFirst(MapInfo mapInfo) {
        super(EnemyStrategyName.HATCHFIRST);
        this.mapInfo = mapInfo;

        buildingResponse();
        upgradeResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyType() != UnitType.Zerg_Hatchery || enemyUnit.getEnemyPosition() == null) {
                continue;
            }

            Area hatchArea = mapInfo.getGameMap().getArea(enemyUnit.getEnemyTilePosition());

            if (hatchArea == null) {
                continue;
            }

            if (mapInfo.getStartingBases().stream().anyMatch(b -> b.getArea() == hatchArea)) {
                continue;
            }

            if (!enemyUnit.getEnemyUnit().isVisible()) {
                continue;
            }

            Time finishTime = new Time(time.getFrames() + remainingBuildFrames(enemyUnit));

            if (finishTime.lessThanOrEqual(new Time(3, 10))) {
                return true;
            }
        }

        return false;
    }

    public void buildingResponse() {
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
