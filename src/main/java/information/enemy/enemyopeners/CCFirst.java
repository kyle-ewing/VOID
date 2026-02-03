package information.enemy.enemyopeners;

import bwapi.UnitType;
import bwem.Base;
import information.BaseInfo;
import information.enemy.EnemyUnits;
import planner.PlannedItem;
import util.Time;

import java.util.HashSet;

public class CCFirst extends  EnemyStrategy {
    private BaseInfo baseInfo;
    private boolean isStartingBase = false;

    public CCFirst(BaseInfo baseInfo) {
        super("CC First");
        this.baseInfo = baseInfo;
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        for(EnemyUnits enemyUnit : enemyUnits) {
            if(enemyUnit.getEnemyType() == null || enemyUnit.getEnemyPosition() == null ) {
                continue;
            }

            if(enemyUnit.getEnemyType() == UnitType.Terran_Command_Center) {
                for(Base base : baseInfo.getMapBases()) {
                    if(base.isStartingLocation()) {
                        if(base.getLocation().getApproxDistance(enemyUnit.getEnemyPosition().toTilePosition()) < 10) {
                            isStartingBase = true;
                            break;
                        }
                    }
                    else if(base.getLocation().getApproxDistance(enemyUnit.getEnemyPosition().toTilePosition()) < 10) {
                        //Check for bunker to reduce false positives
                        if(time.lessThanOrEqual(new Time(3,30))
                                && enemyUnits.stream().map(EnemyUnits::getEnemyType).noneMatch(ut -> ut == UnitType.Terran_Bunker)) {
                            return true;
                        }
                        else if(time.lessThanOrEqual(new Time(4,0)) && enemyUnit.getEnemyUnit().isCompleted()) {
                            return true;
                        }
                    }
                }
            }
        }

        //Not ideal check, does not cover proxies
        return enemyUnits.stream().map(EnemyUnits::getEnemyType).noneMatch(ut -> ut == UnitType.Terran_Barracks)
                && enemyUnits.stream().map(EnemyUnits::getEnemyType).noneMatch(ut -> ut == UnitType.Terran_Marine)
                && enemyUnits.stream().map(EnemyUnits::getEnemyType).anyMatch(ut -> ut == UnitType.Terran_Command_Center)
                && isStartingBase
                && time.greaterThan(new Time(2, 0))
                && time.lessThanOrEqual(new Time(4, 0));
    }

    public void buildingResponse() {
    }
}
