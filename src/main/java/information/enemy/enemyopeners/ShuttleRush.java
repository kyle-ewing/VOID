package information.enemy.enemyopeners;

import java.util.HashMap;
import java.util.HashSet;

import bwapi.UnitType;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildType;
import util.Time;

public class ShuttleRush extends EnemyStrategy{
    public ShuttleRush() {
        super(EnemyStrategyName.SHUTTLERUSH);

        buildingResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        if (enemyUnits.stream().anyMatch(unit -> unit.getEnemyType() == UnitType.Protoss_Dragoon)
                && time.lessThanOrEqual(new Time(5, 30))
                && time.greaterThan(new Time(3, 30))) {
            return false;
        }

        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyType() == UnitType.Protoss_Robotics_Facility) {
                int predictedFinishFrame = time.getFrames() + enemyUnit.getEnemyUnit().getRemainingBuildTime();
                if (new Time(predictedFinishFrame).lessThanOrEqual(new Time(4, 15))) {
                    return true;
                }
            }

            if (enemyUnit.getEnemyType() == UnitType.Protoss_Shuttle) {
                if (time.lessThanOrEqual(new Time(5,0))) {
                    return true;
                }
            }
        }
        return false;
    }

    public void buildingResponse() {
        getBuildingResponse().add(UnitType.Terran_Marine);
        getBuildingResponse().add(UnitType.Terran_Marine);
        getBuildingResponse().add(UnitType.Terran_Marine);
        getBuildingResponse().add(UnitType.Terran_Marine);
        getBuildingResponse().add(UnitType.Terran_Marine);
        getBuildingResponse().add(UnitType.Terran_Marine);
        getBuildingResponse().add(UnitType.Terran_Marine);
    }

    public void upgradeResponse() {
    }

    public HashMap<UnitType, Integer> getMoveOutCondition(BuildType buildType, Time time) {
        return new HashMap<>();
    }

    public HashSet<UnitType> removeBuildings() {
        return new HashSet<>();
    }

    @Override
    public boolean overrideBuildingLift() {
        return true;
    }
}
