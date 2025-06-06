package information.enemyopeners;

import bwapi.UnitType;
import bwem.Base;
import information.BaseInfo;
import information.EnemyUnits;

import java.util.HashSet;

public class CannonRush extends EnemyStrategy {
    private BaseInfo baseInfo;

    public CannonRush(BaseInfo baseInfo) {
        super("Cannon Rush");
        this.baseInfo = baseInfo;

        buildingResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, int frameCount) {
        for(EnemyUnits enemyUnit : enemyUnits) {
            if(enemyUnit.getEnemyPosition() == null ) {
                continue;
            }

            if(enemyUnit.getEnemyPosition().getApproxDistance(baseInfo.getStartingBase().getCenter()) > 1000) {
                continue;
            }

            if(enemyUnit.getEnemyType() == UnitType.Protoss_Photon_Cannon) {
                if(frameCount < 4320) {
                    return true;
                }
            }
            else if(enemyUnit.getEnemyType() == UnitType.Protoss_Probe) {
                if(frameCount < 2000) {
                    return true;
                }
            }
            else if(enemyUnit.getEnemyType() == UnitType.Protoss_Pylon) {
                if(frameCount < 2500) {
                    return true;
                }
            }
        }
        return false;
    }

    public void buildingResponse() {
    }
}
