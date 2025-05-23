package information.enemyopeners;

import bwapi.UnitType;
import information.EnemyUnits;

import java.util.HashSet;

public class FourPool extends EnemyStrategy {
    public FourPool() {
        super("Four Pool");

        buildingResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, int frameCount) {
        for(EnemyUnits enemyUnit : enemyUnits) {
            if(enemyUnit.getEnemyType() == UnitType.Zerg_Spawning_Pool) {
                if(frameCount < 2400) {
                    return true;
                }
            }
            else if(enemyUnit.getEnemyType() == UnitType.Zerg_Zergling) {
                if(frameCount < 4200) {
                    return true;
                }
            }
        }
        return false;
    }

    public void buildingResponse() {
        getBuildingResponse().add(UnitType.Terran_Bunker);
    }
}
