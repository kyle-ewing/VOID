package information.enemy.enemyopeners;

import java.util.HashMap;
import java.util.HashSet;

import bwapi.UnitType;
import information.MapInfo;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildType;
import util.Time;

public class TwoBaseMuta extends EnemyStrategy {
    private MapInfo mapInfo;

    public TwoBaseMuta(MapInfo mapInfo) {
        super(EnemyStrategyName.TWOBASEMUTA);
        this.mapInfo = mapInfo;

        buildingResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        boolean hasNaturalDepot = false;
        EnemyUnits natural = null;
        if (mapInfo.getEnemyNatural() != null) {
            natural = enemyUnits.stream()
                    .filter(eu -> eu.getEnemyType().isResourceDepot())
                    .filter(eu -> eu.getEnemyPosition().getDistance(mapInfo.getEnemyNatural().getLocation().toPosition()) < 200)
                    .findFirst()
                    .orElse(null);
            hasNaturalDepot = natural != null;
        }

        if (!hasNaturalDepot) {
            return false;
        }

        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyPosition() == null) {
                continue;
            }

            if (enemyUnit.getEnemyType() == UnitType.Zerg_Extractor) {
                if (natural != null && enemyUnit.getEnemyPosition().getDistance(natural.getEnemyPosition()) < 200) {
                    Time extractorCompletion = new Time(time.getFrames() + enemyUnit.getEnemyUnit().getRemainingBuildTime());
                    if (extractorCompletion.lessThanOrEqual(new Time(6, 0))) {
                        return true;
                    }
                }
            }

            if (enemyUnit.getEnemyType() != UnitType.Zerg_Spire) {
                continue;
            }

            Time spireCompletion = new Time(time.getFrames() + enemyUnit.getEnemyUnit().getRemainingBuildTime());
            if (spireCompletion.lessThanOrEqual(new Time(6, 15))) {
                return true;
            }
        }
        return false;
    }

    public void buildingResponse() {
        getBuildingResponse().add(UnitType.Terran_Engineering_Bay);
        getBuildingResponse().add(UnitType.Terran_Armory);
        getBuildingResponse().add(UnitType.Terran_Missile_Turret);
        getBuildingResponse().add(UnitType.Terran_Missile_Turret);
        getBuildingResponse().add(UnitType.Terran_Missile_Turret);
        getBuildingResponse().add(UnitType.Terran_Missile_Turret);
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
