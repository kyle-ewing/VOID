package information.enemy.enemyopeners;

import bwapi.UnitType;
import bwapi.UpgradeType;
import information.BaseInfo;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildType;
import util.Time;

import java.util.HashMap;
import java.util.HashSet;

public class TwoBaseLurker extends EnemyStrategy {
    private BaseInfo baseInfo;

    public TwoBaseLurker(BaseInfo baseInfo) {
        super("Two Base Lurker");
        this.baseInfo = baseInfo;

        buildingResponse();
        upgradeResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        for(EnemyUnits enemyUnit : enemyUnits) {
            if(enemyUnit.getEnemyType() == UnitType.Zerg_Hydralisk && time.lessThanOrEqual(new Time(6,0))) {
                return true;
            }

            if(enemyUnit.getEnemyType() == UnitType.Zerg_Hydralisk_Den) {
                if(time.lessThanOrEqual(new Time(5,30))
                    && enemyUnits.stream().map(EnemyUnits::getEnemyType).filter(et -> et == UnitType.Zerg_Lair || et == UnitType.Zerg_Hatchery).count() > 1
                    && enemyUnits.stream().map(EnemyUnits::getEnemyType).anyMatch(et -> et == UnitType.Zerg_Lair)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void buildingResponse() {
        getBuildingResponse().add(UnitType.Terran_Bunker);
        getBuildingResponse().add(UnitType.Terran_Engineering_Bay);
        getBuildingResponse().add(UnitType.Terran_Missile_Turret);
        getBuildingResponse().add(UnitType.Terran_Factory);
        getBuildingResponse().add(UnitType.Terran_Machine_Shop);
    }

    public void upgradeResponse() {
        getUpgradeResponse().add(UpgradeType.U_238_Shells);
    }

    public HashMap<UnitType, Integer> getMoveOutCondition(BuildType buildType, Time time) {
        HashMap<UnitType, Integer> moveOutCondition = new HashMap<>();

        if(buildType == BuildType.BIO) {
            moveOutCondition.put(UnitType.Terran_Marine, 12);
            moveOutCondition.put(UnitType.Terran_Medic, 4);
            moveOutCondition.put(UnitType.Terran_Siege_Tank_Tank_Mode, 2);
        }

        return moveOutCondition;
    }

    public HashSet<UnitType> removeBuildings() {
        return new HashSet<>();
    }
}
