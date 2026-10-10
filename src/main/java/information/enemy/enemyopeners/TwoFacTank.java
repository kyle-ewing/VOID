package information.enemy.enemyopeners;

import java.util.HashMap;
import java.util.HashSet;

import bwapi.UnitType;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildType;
import util.Time;

public class TwoFacTank extends  EnemyStrategy {
    public TwoFacTank() {
        super(EnemyStrategyName.TWOFACTANK);

        buildingResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        if (enemyUnits.stream().filter(type -> type.getEnemyType() == UnitType.Terran_Factory).count() >= 2
                && time.lessThanOrEqual(new Time(4, 40))) {
            return true;
        }

        return time.lessThanOrEqual(new Time(4, 10))
                && enemyUnits.stream().filter(type -> type.getEnemyType() == UnitType.Terran_Factory).count() == 2
                && enemyUnits.stream().filter(type -> type.getEnemyType() == UnitType.Terran_Machine_Shop).count() == 2;
    }

    public void buildingResponse() {
        getBuildingResponse().add(UnitType.Terran_Barracks);
    }

    public void upgradeResponse() {
    }

    public HashSet<UnitType> getUnitResponse() {
        HashSet<UnitType> response = new HashSet<>();
        response.add(UnitType.Terran_Marine);
        return response;
    }

    public HashSet<UnitType> additionalBuildings() {
        HashSet<UnitType> additionalBuildings = new HashSet<>();
        return additionalBuildings;
    }

    public HashMap<UnitType, Integer> getMoveOutCondition(BuildType buildType, Time time, HashSet<EnemyUnits> enemyUnits) {
        return new HashMap<>();
    }

    public HashSet<UnitType> removeBuildings() {
        HashSet<UnitType> removeBuildings = new HashSet<>();
        return removeBuildings;
    }

    @Override
    public boolean overrideBuildingLift() {
        return true;
    }
}
