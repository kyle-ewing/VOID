package information.enemy.enemyopeners;

import bwapi.TechType;
import bwapi.UnitType;
import bwapi.UpgradeType;
import information.BaseInfo;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildType;
import util.Time;

import java.util.HashMap;
import java.util.HashSet;

public class OneBaseLurker extends EnemyStrategy {
    private BaseInfo baseInfo;

    public OneBaseLurker(BaseInfo baseInfo) {
        super("One Base Lurker");
        this.baseInfo = baseInfo;

        buildingResponse();
        upgradeResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        boolean hasNaturalHatch = false;
        if(baseInfo.getEnemyNatural() != null) {
            hasNaturalHatch = enemyUnits.stream()
                    .filter(eu -> eu.getEnemyType() == UnitType.Zerg_Hatchery)
                    .anyMatch(eu -> eu.getEnemyPosition().getDistance(baseInfo.getEnemyNatural().getCenter()) < 50);
        }

        for(EnemyUnits enemyUnit : enemyUnits) {
            if(enemyUnit.getEnemyPosition() == null) {
                continue;
            }

            if(enemyUnit.getEnemyType() == UnitType.Zerg_Lair && time.lessThanOrEqual(new Time(2,30))) {
                return true;
            }

            if(enemyUnit.getEnemyType() == UnitType.Zerg_Hydralisk
                    || enemyUnit.getEnemyType() == UnitType.Zerg_Lurker
                    || enemyUnit.getEnemyType() == UnitType.Zerg_Lurker_Egg
                    && time.lessThanOrEqual(new Time(6,0))) {
                return true;
            }

            if(enemyUnit.getEnemyType() == UnitType.Zerg_Hydralisk_Den) {
                if(time.lessThanOrEqual(new Time(5,30))
                        && enemyUnits.stream().map(EnemyUnits::getEnemyType).filter(et -> et == UnitType.Zerg_Lair ).count() == 1
                        && (enemyUnits.stream().map(EnemyUnits::getEnemyType).filter(et -> et == UnitType.Zerg_Hatchery || et == UnitType.Zerg_Lair).count() == 1
                        || !hasNaturalHatch)) {
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
        getBuildingResponse().add(UnitType.Terran_Starport);
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
