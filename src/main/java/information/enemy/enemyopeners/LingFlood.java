package information.enemy.enemyopeners;

import java.util.HashMap;
import java.util.HashSet;

import bwapi.UnitType;
import information.MapInfo;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildType;
import util.Time;

public class LingFlood extends EnemyStrategy {
    private MapInfo mapInfo;

    public LingFlood(MapInfo mapInfo) {
        super(EnemyStrategyName.LINGFLOOD);

        this.mapInfo = mapInfo;

        buildingResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        if (enemyUnits.stream().filter(eu -> eu.getEnemyType() == UnitType.Zerg_Lair).count() > 0 || time.greaterThan(new Time(5, 30))) {
            return false;
        }

        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyPosition() == null) {
                continue;
            }

            if (enemyUnit.getEnemyType() == UnitType.Zerg_Spawning_Pool) {
                if (time.greaterThan(new Time(3, 0)) && time.lessThanOrEqual(new Time(5, 30))) {
                    if (enemyUnit.getEnemyUnit().isResearching()) {
                        return true;
                    }
                }
            }

            long knownLings = enemyUnits.stream().filter(eu -> eu.getEnemyType() == UnitType.Zerg_Zergling).count();
            if (knownLings >= 8 && time.greaterThan(new Time(3, 20)) && time.lessThanOrEqual(new Time(4, 0))) {
                return true;
            }

        }


        return false;
    }

    public HashMap<UnitType, Integer> getMoveOutCondition(BuildType buildType, Time time, HashSet<EnemyUnits> enemyUnits) {
        return new HashMap<>();
    }

    public void buildingResponse() {
        getBuildingResponse().add(UnitType.Terran_Marine);
        getBuildingResponse().add(UnitType.Terran_Marine);
        getBuildingResponse().add(UnitType.Terran_Vulture);
        getBuildingResponse().add(UnitType.Terran_Vulture);
    }

    public void upgradeResponse() {
    }

    public HashSet<UnitType> removeBuildings() {
        return new HashSet<>();
    }

}
