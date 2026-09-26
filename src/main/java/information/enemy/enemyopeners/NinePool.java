package information.enemy.enemyopeners;

import java.util.HashMap;
import java.util.HashSet;

import bwapi.UnitType;
import information.MapInfo;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildType;
import util.Time;

public class NinePool extends EnemyStrategy {
    private MapInfo mapInfo;
    private boolean hasMatched = false;
    private boolean handedOff = false;

    public NinePool(MapInfo mapInfo) {
        super(EnemyStrategyName.NINEPOOL);
        this.mapInfo = mapInfo;
        openerSwitchWindow = new Time(2, 45);

        buildingResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        if (enemyUnits.stream().anyMatch(eu -> eu.getEnemyType() == UnitType.Zerg_Lair
                || eu.getEnemyType() == UnitType.Zerg_Hydralisk_Den)) {
            return false;
        }

        if (handedOff) {
            return false;
        }

        long knownLings = enemyUnits.stream().filter(eu -> eu.getEnemyType() == UnitType.Zerg_Zergling).count();

        if (knownLings >= 8 && time.greaterThan(new Time(2,40)) && time.lessThanOrEqual(new Time(3,20))) {
            hasMatched = true;
            return true;
        }
        else if (knownLings >= 6 && time.greaterThan(new Time(2,30)) && time.lessThanOrEqual(new Time(3,10))) {
            hasMatched = true;
            return true;
        }

        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyPosition() == null) {
                continue;
            }

            if (enemyUnit.getEnemyType() == UnitType.Zerg_Spawning_Pool) {
                if (enemyUnit.getEnemyUnit().isCompleted() && time.greaterThan(new Time(1,45)) && time.lessThanOrEqual(new Time(2,5))) {
                    hasMatched = true;
                    return true;
                }
            }

            if (enemyUnit.getEnemyType() == UnitType.Zerg_Zergling) {
                if (mapInfo.getNaturalBase().getCenter().getDistance(enemyUnit.getEnemyPosition()) < 1400
                && time.greaterThan(new Time(2,37)) && time.lessThanOrEqual(new Time(3,0))) {
                    hasMatched = true;
                    return true;
                }

                if (mapInfo.getEnemyMain() == null) {
                    continue;
                }

                int travelFrames = (int) (mapInfo.getEnemyMain().getCenter().getDistance(enemyUnit.getEnemyPosition()) / 5.49);
                Time impliedHatchTime = new Time(time.getFrames() - travelFrames);

                if (impliedHatchTime.greaterThan(new Time(2,5)) && impliedHatchTime.lessThanOrEqual(new Time(2,25))) {
                    hasMatched = true;
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasMatched() {
        return hasMatched;
    }

    public void setHandedOff(boolean handedOff) {
        this.handedOff = handedOff;
    }

    public void buildingResponse() {
        getBuildingResponse().add(UnitType.Terran_Bunker);
        getBuildingResponse().add(UnitType.Terran_Marine);
        getBuildingResponse().add(UnitType.Terran_Marine);
    }

    public void upgradeResponse() {
    }

    public HashMap<UnitType, Integer> getMoveOutCondition(BuildType buildType, Time time, HashSet<EnemyUnits> enemyUnits) {
        HashMap<UnitType, Integer> moveOutCondition = new HashMap<>();
        long knownLings = enemyUnits.stream().filter(eu -> eu.getEnemyType() == UnitType.Zerg_Zergling).count();

        if (buildType == BuildType.BIO) {
            if (time.lessThanOrEqual(new Time(8,0)) && knownLings > 10) {
                moveOutCondition.put(UnitType.Terran_Marine, 20);
                moveOutCondition.put(UnitType.Terran_Medic, 5);
                moveOutCondition.put(UnitType.Terran_Firebat, 3);
            }
            else if (time.lessThanOrEqual(new Time(8,0)) && knownLings < 10) {
                return new HashMap<>();
            }
            else {
                moveOutCondition.put(UnitType.Terran_Marine, 25);
                moveOutCondition.put(UnitType.Terran_Medic, 6);
            }
        }

        return moveOutCondition;
    }

    public HashSet<UnitType> removeBuildings() {
        return new HashSet<>();
    }
}
