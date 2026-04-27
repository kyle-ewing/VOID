package information.enemy.enemyopeners;

import java.util.HashMap;
import java.util.HashSet;

import bwapi.UnitType;
import information.MapInfo;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildType;
import util.Time;

public class NexusFirst extends  EnemyStrategy {
    private MapInfo mapInfo;

    public NexusFirst(MapInfo mapInfo) {
        super("Nexus First");
        this.mapInfo = mapInfo;
        
        buildingResponse();
        upgradeResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyType() == null || enemyUnit.getEnemyPosition() == null ) {
                continue;
            }

            if (enemyUnit.getEnemyType() == UnitType.Protoss_Nexus) {
                if (mapInfo.getStartingBases().stream().anyMatch(b -> b.getCenter().getDistance(enemyUnit.getEnemyPosition()) < 50)) {
                    continue;
                }

                if (time.lessThanOrEqual(new Time(2, 25))) {
                    return true;
                } 
                else if (time.lessThanOrEqual(new Time(2, 45))
                        && enemyUnit.getEnemyUnit().getHitPoints() > 300) {
                    return true;
                }
                else if (time.lessThanOrEqual(new Time(3, 0))
                        && enemyUnit.getEnemyUnit().getHitPoints() > 375) {
                    return true;
                }
                else if (time.lessThanOrEqual(new Time(3, 35)) && enemyUnit.getEnemyUnit().isCompleted()) {
                    return true;
                }
            }
        }
        return false;
    }

    public void buildingResponse() {
        getBuildingResponse().add(UnitType.Terran_Command_Center);
    }

    public void upgradeResponse() {
    }

    public HashMap<UnitType, Integer> getMoveOutCondition(BuildType buildType, Time time) {
        return new HashMap<>();
    }

    public HashSet<UnitType> removeBuildings() {
        HashSet<UnitType> removeBuildings = new HashSet<>();
        removeBuildings.add(UnitType.Terran_Bunker);
        return removeBuildings;
    }
}
