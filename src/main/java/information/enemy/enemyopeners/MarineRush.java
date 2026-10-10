package information.enemy.enemyopeners;

import java.util.HashMap;
import java.util.HashSet;

import bwapi.UnitType;
import information.MapInfo;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildType;
import util.Time;

public class MarineRush extends EnemyStrategy {
    private MapInfo mapInfo;
    private HashSet<EnemyUnits> marinesOutsideMain = new HashSet<>();

    public MarineRush(MapInfo mapInfo) {
        super(EnemyStrategyName.MARINERUSH);
        this.mapInfo = mapInfo;
        hardLockedWhenSeen = true;
        safeExpansion = false;

        buildingResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        if (!time.lessThanOrEqual(new Time(3, 0))) {
            return false;
        }

        if (enemyUnits.stream().anyMatch(eu -> eu.getEnemyType() == UnitType.Terran_Refinery
                || eu.getEnemyType() == UnitType.Terran_Factory
                || eu.getEnemyType() == UnitType.Terran_Academy)) {
            return false;
        }

        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyType() != UnitType.Terran_Marine || enemyUnit.getEnemyPosition() == null) {
                continue;
            }

            if (mapInfo.getEnemyMain() != null && mapInfo.getEnemyMain().getCenter() != null
                    && enemyUnit.getEnemyPosition().getDistance(mapInfo.getEnemyMain().getCenter()) < 900) {
                continue;
            }

            marinesOutsideMain.add(enemyUnit);
        }

        return marinesOutsideMain.size() >= 2;
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

    @Override
    public HashSet<UnitType> deferredBuildings() {
        HashSet<UnitType> deferredBuildings = new HashSet<>();
        deferredBuildings.add(UnitType.Terran_Factory);
        return deferredBuildings;
    }
}
