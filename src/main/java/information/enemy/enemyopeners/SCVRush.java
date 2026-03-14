package information.enemy.enemyopeners;

import bwapi.UnitType;
import information.MapInfo;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildType;
import util.Time;

import java.util.HashMap;
import java.util.HashSet;

//Stone Cold Steve Austin's favorite strategy
public class SCVRush extends EnemyStrategy {
    private MapInfo mapInfo;

    public SCVRush(MapInfo mapInfo) {
        super("SCV Rush");
        this.mapInfo = mapInfo;

        buildingResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        int scvAtBase = 0;

        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyType() == null || enemyUnit.getEnemyPosition() == null ) {
                continue;
            }

            if (enemyUnit.getEnemyType() == UnitType.Terran_SCV) {
                if (mapInfo.getStartingBase().getCenter().getDistance(enemyUnit.getEnemyPosition()) < 500) {
                    scvAtBase++;
                }
            }
        }

        if (scvAtBase >= 3 && time.lessThanOrEqual(new Time(3, 0))) {
            return true;
        }
        else {
            return enemyUnits.stream().filter(eu -> eu.getEnemyType() == UnitType.Terran_SCV).count() >= 2
                    && time.lessThanOrEqual(new Time(1, 0));
        }

    }

    public void buildingResponse() {
        getBuildingResponse().add(UnitType.Terran_Barracks);
        getBuildingResponse().add(UnitType.Terran_Bunker);
        getBuildingResponse().add(UnitType.Terran_Vulture);
        getBuildingResponse().add(UnitType.Terran_Vulture);
    }

    public void upgradeResponse() {
    }

    @Override
    public HashSet<UnitType> getUnitResponse() {
        HashSet<UnitType> response = new HashSet<>();
        response.add(UnitType.Terran_Marine);
        return response;
    }

    public HashMap<UnitType, Integer> getMoveOutCondition(BuildType buildType, Time time) {
        HashMap<UnitType, Integer> moveOutCondition = new HashMap<>();
        moveOutCondition.put(UnitType.Terran_Goliath, 2);
        return moveOutCondition;
    }

    public HashSet<UnitType> removeBuildings() {
        HashSet<UnitType> removeBuildings = new HashSet<>();
        removeBuildings.add(UnitType.Terran_Command_Center);
        removeBuildings.add(UnitType.Terran_Refinery);
        removeBuildings.add(UnitType.Terran_Factory);
        removeBuildings.add(UnitType.Terran_Engineering_Bay);
        return removeBuildings;
    }

    //Avoids supply block since CC is being removed
    @Override
    public HashSet<UnitType> additionalBuildings() {
        HashSet<UnitType> additionalBuildings = new HashSet<>();
        additionalBuildings.add(UnitType.Terran_Supply_Depot);
        return additionalBuildings;
    }

    @Override
    public boolean overrideBuildingLift() {
        return true;
    }
}
