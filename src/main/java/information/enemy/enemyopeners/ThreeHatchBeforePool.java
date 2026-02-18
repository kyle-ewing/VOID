package information.enemy.enemyopeners;

import bwapi.UnitType;
import information.BaseInfo;
import information.enemy.EnemyUnits;
import util.Time;

import java.util.HashMap;
import java.util.HashSet;

public class ThreeHatchBeforePool extends EnemyStrategy {
    private BaseInfo baseInfo;

    public ThreeHatchBeforePool(BaseInfo baseInfo) {
        super("Three Hatch Before Pool");
        this.baseInfo = baseInfo;

        buildingResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        boolean hasNaturalHatch = false;
        if(baseInfo.getEnemyNatural() != null) {
            hasNaturalHatch = enemyUnits.stream()
                    .filter(eu -> eu.getEnemyType() == UnitType.Zerg_Hatchery)
                    .anyMatch(eu -> eu.getEnemyPosition().getDistance(baseInfo.getEnemyNatural().getCenter()) < 50);
        }

        EnemyUnits mainHatch = enemyUnits.stream().filter(eu -> eu.getEnemyType() == UnitType.Zerg_Hatchery)
                .filter(eu -> baseInfo.getEnemyMain() != null && eu.getEnemyPosition().getDistance(baseInfo.getEnemyMain().getCenter()) < 50)
                .findFirst().orElse(null);

        if(mainHatch == null) {
            return false;
        }

        return enemyUnits.stream().map(EnemyUnits::getEnemyType).filter(et -> et == UnitType.Zerg_Hatchery).count() > 1
                && enemyUnits.stream().map(EnemyUnits::getEnemyType).noneMatch(et -> et == UnitType.Zerg_Spawning_Pool)
                && hasNaturalHatch
                && mainHatch.getEnemyUnit().isVisible()
                && time.greaterThan(new Time(2, 40))
                && time.lessThanOrEqual(new Time(3, 0));
    }

public void buildingResponse() {
}

public void upgradeResponse() {
}

public HashMap<UnitType, Integer> getMoveOutCondition(Time time) {
    HashMap<UnitType, Integer> moveOutCondition = new HashMap<>();

    if(time.lessThanOrEqual(new Time(5,0))) {
        moveOutCondition.put(UnitType.Terran_Marine, 4);
    }

    return moveOutCondition;
}

public HashSet<UnitType> removeBuildings() {
    return new HashSet<>();
}
}
