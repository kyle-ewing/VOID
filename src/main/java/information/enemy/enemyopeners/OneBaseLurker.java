package information.enemy.enemyopeners;

import java.util.HashMap;
import java.util.HashSet;

import bwapi.TechType;
import bwapi.UnitType;
import bwapi.UpgradeType;
import information.MapInfo;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildType;
import util.Time;

public class OneBaseLurker extends EnemyStrategy {
    private MapInfo mapInfo;

    public OneBaseLurker(MapInfo mapInfo) {
        super(EnemyStrategyName.ONEBASELURKER);
        this.mapInfo = mapInfo;

        buildingResponse();
        upgradeResponse();
        techUpgradeResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        if (time.greaterThan(new Time(6,0))) {
            return false;
        }

        boolean hasNaturalHatch = false;
        if (mapInfo.getEnemyNatural() != null) {
            hasNaturalHatch = enemyUnits.stream()
                    .filter(eu -> eu.getEnemyType().isResourceDepot())
                    .anyMatch(eu -> eu.getEnemyPosition().getDistance(mapInfo.getEnemyNatural().getLocation().toPosition()) < 200);
        }

        if (hasNaturalHatch) {
            return false;
        }

        boolean denResearching = enemyUnits.stream()
                .filter(eu -> eu.getEnemyType() == UnitType.Zerg_Hydralisk_Den)
                .anyMatch(eu -> eu.getEnemyUnit().isResearching());

        if (denResearching) {
            return true;
        }

        boolean hasLurker = enemyUnits.stream()
                .anyMatch(eu -> eu.getEnemyType() == UnitType.Zerg_Lurker || eu.getEnemyType() == UnitType.Zerg_Lurker_Egg);

        if (hasLurker) {
            return true;
        }

        boolean hasLair = enemyUnits.stream().anyMatch(eu -> eu.getEnemyType() == UnitType.Zerg_Lair);

        if (!hasLair) {
            return false;
        }

        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyPosition() == null) {
                continue;
            }

            if (enemyUnit.getEnemyType() == UnitType.Zerg_Lair && time.lessThanOrEqual(new Time(2,30))) {
                return true;
            }

            if (enemyUnit.getEnemyType() == UnitType.Zerg_Hydralisk) {
                return true;
            }

            if (enemyUnit.getEnemyType() == UnitType.Zerg_Hydralisk_Den) {
                if (time.lessThanOrEqual(new Time(5,30))
                        && enemyUnits.stream().map(EnemyUnits::getEnemyType).filter(et -> et == UnitType.Zerg_Lair ).count() == 1
                        && enemyUnits.stream().map(EnemyUnits::getEnemyType).filter(et -> et == UnitType.Zerg_Hatchery || et == UnitType.Zerg_Lair).count() == 1) {
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
        getBuildingResponse().add(UnitType.Terran_Science_Facility);
        getBuildingResponse().add(UnitType.Terran_Marine);
    }

    public void upgradeResponse() {
        getUpgradeResponse().add(UpgradeType.U_238_Shells);
    }

    public void techUpgradeResponse() {
        getTechUpgradeResponse().add(TechType.Tank_Siege_Mode);
    }


    public HashMap<UnitType, Integer> getMoveOutCondition(BuildType buildType, Time time, HashSet<EnemyUnits> enemyUnits) {
        HashMap<UnitType, Integer> moveOutCondition = new HashMap<>();
        long knownLurkers = enemyUnits.stream().filter(eu -> eu.getEnemyType() == UnitType.Zerg_Lurker || eu.getEnemyType() == UnitType.Zerg_Lurker_Egg).count();

        if (buildType == BuildType.BIO) {
            if (time.lessThanOrEqual(new Time(10,0))) {
                moveOutCondition.put(UnitType.Terran_Marine, 15);
                moveOutCondition.put(UnitType.Terran_Medic, 4);
                moveOutCondition.put(UnitType.Terran_Siege_Tank_Tank_Mode, 2);
                moveOutCondition.put(UnitType.Terran_Science_Vessel, 1);
            }
            else if (knownLurkers > 0) {
                moveOutCondition.put(UnitType.Terran_Marine, 20);
                moveOutCondition.put(UnitType.Terran_Medic, 5);
                moveOutCondition.put(UnitType.Terran_Siege_Tank_Tank_Mode, 3);
            }
            else {
                moveOutCondition.put(UnitType.Terran_Marine, 20);
                moveOutCondition.put(UnitType.Terran_Medic, 5);
            }
        }

        return moveOutCondition;
    }

    public HashSet<UnitType> removeBuildings() {
        return new HashSet<>();
    }
}
