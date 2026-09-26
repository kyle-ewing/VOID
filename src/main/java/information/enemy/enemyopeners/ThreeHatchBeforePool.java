package information.enemy.enemyopeners;

import java.util.HashMap;
import java.util.HashSet;

import bwapi.UnitType;
import information.MapInfo;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildType;
import util.Time;

public class ThreeHatchBeforePool extends EnemyStrategy {
    private MapInfo mapInfo;
    private int mainHatchSeenFrames = 0;

    public ThreeHatchBeforePool(MapInfo mapInfo) {
        super(EnemyStrategyName.THREEHATCHBEFOREPOOL);
        this.mapInfo = mapInfo;
        this.bypassNatural = true;
        this.bypassTime = new Time(4, 15);

        buildingResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        boolean hasNaturalHatch = false;
        if (mapInfo.getEnemyNatural() != null) {
            hasNaturalHatch = enemyUnits.stream()
                    .filter(eu -> eu.getEnemyType().isResourceDepot())
                    .anyMatch(eu -> eu.getEnemyPosition().getDistance(mapInfo.getEnemyNatural().getLocation().toPosition()) < 200);
        }

        EnemyUnits mainHatch = enemyUnits.stream().filter(eu -> eu.getEnemyType() == UnitType.Zerg_Hatchery)
                .filter(eu -> mapInfo.getEnemyMain() != null && eu.getEnemyPosition().getDistance(mapInfo.getEnemyMain().getCenter()) < 50)
                .findFirst().orElse(null);

        if (mainHatch == null) {
            return false;
        }

        mainHatchSeenFrames++;

        if (!hasNaturalHatch) {
            return false;
        }

        boolean noPoolSeen = enemyUnits.stream().map(EnemyUnits::getEnemyType).noneMatch(et -> et == UnitType.Zerg_Spawning_Pool);

        if (noPoolSeen && mainHatch.getEnemyUnit().isVisible()
            && mainHatchSeenFrames >= 96
            && time.greaterThan(new Time(2, 30))
            && time.lessThanOrEqual(new Time(2, 45))) {
            return true;
        }

        return false;
    }

public void buildingResponse() {
    getBuildingResponse().add(UnitType.Terran_Marine);
    getBuildingResponse().add(UnitType.Terran_Marine);
    getBuildingResponse().add(UnitType.Terran_Marine);
    getBuildingResponse().add(UnitType.Terran_Marine);
}

public void upgradeResponse() {
}

public HashMap<UnitType, Integer> getMoveOutCondition(BuildType buildType, Time time, HashSet<EnemyUnits> enemyUnits) {
    HashMap<UnitType, Integer> moveOutCondition = new HashMap<>();

    if (time.lessThanOrEqual(new Time(4, 15))) {
        moveOutCondition.put(UnitType.Terran_Marine, 3);
    }

    return moveOutCondition;
}

public HashSet<UnitType> removeBuildings() {
    return new HashSet<>();
}
}
