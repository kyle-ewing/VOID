package information.enemy.enemyopeners;

import java.util.HashMap;
import java.util.HashSet;

import bwapi.UnitType;
import information.MapInfo;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildType;
import util.Time;

//Why Dave?
public class DoubleEightRax extends  EnemyStrategy {
    private MapInfo mapInfo;
    private boolean isStartingBase = false;
    private HashSet<EnemyUnits> earlyBarracks = new HashSet<>();

    public DoubleEightRax(MapInfo mapInfo) {
        super(EnemyStrategyName.DOUBLEEIGHTRAX);
        this.mapInfo = mapInfo;

        buildingResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyType() == null || enemyUnit.getEnemyPosition() == null ) {
                continue;
            }

            if (enemyUnit.getEnemyType() == UnitType.Terran_Barracks && enemyUnit.getEnemyUnit().isVisible()) {
                Time finishTime = new Time(time.getFrames() + remainingBuildFrames(enemyUnit));

                if (finishTime.lessThanOrEqual(new Time(2, 45))) {
                    earlyBarracks.add(enemyUnit);
                }
            }

            if (earlyBarracks.size() >= 2) {
                return true;
            }

            if (enemyUnit.getEnemyType() == UnitType.Terran_Marine
                    && enemyUnits.stream().filter(type -> type.getEnemyType() == UnitType.Terran_Marine).count() >= 2 
                    && time.lessThanOrEqual(new Time(2, 45))) {
                    return true;
            }
        }
        return false;
    }

    public void buildingResponse() {
        getBuildingResponse().add(UnitType.Terran_Bunker);
        getBuildingResponse().add(UnitType.Terran_Vulture);
        getBuildingResponse().add(UnitType.Terran_Vulture);

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
