package information.enemyopeners;

import bwapi.UnitType;
import bwem.Base;
import information.BaseInfo;
import information.EnemyUnits;
import util.Time;

import java.util.HashSet;

public class CannonRush extends EnemyStrategy {
    private BaseInfo baseInfo;

    public CannonRush(BaseInfo baseInfo) {
        super("Cannon Rush");
        this.baseInfo = baseInfo;

        buildingResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        for(EnemyUnits enemyUnit : enemyUnits) {
            if(enemyUnit.getEnemyPosition() == null ) {
                continue;
            }

            if(enemyUnit.getEnemyPosition().getApproxDistance(baseInfo.getStartingBase().getCenter()) > 1250) {
                continue;
            }

            if(enemyUnit.getEnemyType() == UnitType.Protoss_Photon_Cannon) {
                if(new Time(3, 0).lessThanOrEqual(time)) {
                    return true;
                }
            }
            else if(enemyUnit.getEnemyType() == UnitType.Protoss_Probe) {
                if(time.lessThanOrEqual(new Time(1, 20))) {
                    return true;
                }
            }
            else if(enemyUnit.getEnemyType() == UnitType.Protoss_Pylon) {
                if(time.lessThanOrEqual(new Time(2, 0))) {
                    return true;
                }
            }
        }
        return false;
    }

    public void buildingResponse() {
    }
}
