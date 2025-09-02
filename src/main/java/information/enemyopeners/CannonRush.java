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

            if(baseInfo.getBaseTiles().contains(enemyUnit.getEnemyPosition().toTilePosition()) && enemyUnit.getEnemyType() == UnitType.Protoss_Photon_Cannon) {
                if(new Time(3, 30).greaterThan(time)) {
                    return true;
                }
            }
            else if(baseInfo.getBaseTiles().contains(enemyUnit.getEnemyPosition().toTilePosition()) && enemyUnit.getEnemyType() == UnitType.Protoss_Pylon) {
                if(new Time(3, 30).greaterThan(time)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void buildingResponse() {
    }
}
