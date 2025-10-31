package information.enemy.enemyopeners;

import information.BaseInfo;
import information.enemy.EnemyUnits;
import util.Time;

import java.util.HashSet;

public class GasSteal extends EnemyStrategy{
    private BaseInfo baseInfo;

    public GasSteal(BaseInfo baseInfo) {
        super("Gas Steal");

        this.baseInfo = baseInfo;
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        for(EnemyUnits enemyUnit : enemyUnits) {
            if(enemyUnit.getEnemyType().isRefinery()) {
                if(baseInfo.getBaseTiles().contains(enemyUnit.getEnemyPosition().toTilePosition())) {
                    priorityEnemyUnit = enemyUnit;
                    return true;
                }
            }
        }

        return false;
    }

    public void buildingResponse() {
    }
}
