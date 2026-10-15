package information.enemy.enemyopeners;

import bwapi.UnitType;
import map.bwemwrappers.Base;
import information.MapInfo;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildType;
import util.Time;

import java.util.HashMap;
import java.util.HashSet;

public class CCFirst extends  EnemyStrategy {
    private MapInfo mapInfo;
    private boolean barracksUnitSeenEarly = false;
    private int earliestBarracksStartFrame = Integer.MAX_VALUE;
    private int naturalCCStartFrame = Integer.MAX_VALUE;
    private int naturalCCSeenFrame = -1;
    private boolean enemyMainCCSeen = false;

    public CCFirst(MapInfo mapInfo) {
        super(EnemyStrategyName.CCFIRST);
        this.mapInfo = mapInfo;
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        if (time.lessThanOrEqual(new Time(3, 0))
                && enemyUnits.stream().map(EnemyUnits::getEnemyType).anyMatch(ut -> ut == UnitType.Terran_Bunker || ut == UnitType.Terran_Marine)) {
            barracksUnitSeenEarly = true;
        }

        if (barracksUnitSeenEarly) {
            return false;
        }

        if (enemyUnits.stream().anyMatch(eu -> eu.getEnemyType() == UnitType.Terran_Bunker)) {
            return false;
        }

        Base enemyNatural = mapInfo.getEnemyNatural();
        Base enemyMain = mapInfo.getEnemyMain();

        for (EnemyUnits enemyUnit : enemyUnits) {
            UnitType enemyType = enemyUnit.getEnemyType();
            if (enemyType != UnitType.Terran_Barracks && enemyType != UnitType.Terran_Command_Center) {
                continue;
            }

            if (enemyUnit.getEnemyUnit() == null || !enemyUnit.getEnemyUnit().isVisible()) {
                continue;
            }

            int startFrame = time.getFrames() + remainingBuildFrames(enemyUnit) - enemyType.buildTime();

            if (enemyType == UnitType.Terran_Barracks) {
                earliestBarracksStartFrame = Math.min(earliestBarracksStartFrame, startFrame);
                continue;
            }

            if (enemyUnit.getEnemyPosition() == null) {
                continue;
            }

            if (enemyMain != null && enemyMain.getLocation().getApproxDistance(enemyUnit.getEnemyPosition().toTilePosition()) < 10) {
                enemyMainCCSeen = true;
                continue;
            }

            if (enemyNatural == null) {
                continue;
            }

            if (enemyNatural.getLocation().getApproxDistance(enemyUnit.getEnemyPosition().toTilePosition()) >= 10) {
                continue;
            }

            naturalCCStartFrame = Math.min(naturalCCStartFrame, startFrame);
        }

        if (naturalCCStartFrame == Integer.MAX_VALUE) {
            return false;
        }

        if (naturalCCSeenFrame < 0) {
            naturalCCSeenFrame = time.getFrames();
        }

        if (earliestBarracksStartFrame < naturalCCStartFrame) {
            return false;
        }

        if (!enemyMainCCSeen && time.getFrames() - naturalCCSeenFrame < new Time(0, 10).getFrames()) {
            return false;
        }

        if (time.lessThanOrEqual(new Time(3, 49))) {
            return true;
        }

        return time.lessThanOrEqual(new Time(4, 10))
                && naturalCCStartFrame + UnitType.Terran_Command_Center.buildTime() <= time.getFrames();
    }

    public void buildingResponse() {
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
