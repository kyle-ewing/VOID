package information.enemy.enemyopeners;

import java.util.HashMap;
import java.util.HashSet;

import bwapi.TechType;
import bwapi.TilePosition;
import bwapi.UnitType;
import bwem.Base;
import information.MapInfo;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildType;
import util.Time;

public class TwoGate extends EnemyStrategy {
    private MapInfo mapInfo;

    public TwoGate(MapInfo mapInfo) {
        super("Two Gate");
        this.mapInfo = mapInfo;

        buildingResponse();
        upgradeResponse();
    }

    public boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, Time time) {
        if (enemyUnits.stream().anyMatch(eu -> eu.getEnemyType() == UnitType.Protoss_Cybernetics_Core)) {
            return false;
        }
        
        if (time.lessThanOrEqual(new Time(3, 0))) {
            int completedGateways = 0;

            for (EnemyUnits enemyUnit : enemyUnits) {
                if (enemyUnit.getEnemyType() == UnitType.Protoss_Gateway && enemyUnit.getEnemyUnit().getHitPoints() > 250) {
                    completedGateways++;
                }
            }

            if (completedGateways >= 2) {
                return true;
            }
        }

        if (time.greaterThan(new Time(3, 25))) {
            return false;
        }

        Base enemyMain = mapInfo.getEnemyMain();
        Base enemyNatural = mapInfo.getEnemyNatural();

        HashSet<TilePosition> enemyMainTiles = null;
        HashSet<TilePosition> enemyNaturalTiles = null;

        if (enemyMain != null) {
            enemyMainTiles = mapInfo.getBaseTilesAllBases().get(enemyMain);
        }

        if (enemyNatural != null) {
            enemyNaturalTiles = mapInfo.getBaseTilesAllBases().get(enemyNatural);
        }

        int zealotCount = 0;

        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyType() != UnitType.Protoss_Zealot) {
                continue;
            }

            zealotCount++;

            if (enemyUnit.getEnemyPosition() == null) {
                continue;
            }

            TilePosition zealotTile = enemyUnit.getEnemyPosition().toTilePosition();

            boolean inEnemyMain = enemyMainTiles != null && enemyMainTiles.contains(zealotTile);
            boolean inEnemyNatural = enemyNaturalTiles != null && enemyNaturalTiles.contains(zealotTile);

            if (!inEnemyMain 
                    && !inEnemyNatural 
                    && zealotCount >= 2
                    && mapInfo.getEnemyNatural() != null
                    && enemyUnit.getEnemyPosition().getDistance(mapInfo.getEnemyNatural().getCenter()) > 400) {
                return true;
            }

            if (mapInfo.getNaturalTiles().contains(zealotTile)) {
                return true;
            }
        }

        return false;
    }

    public void buildingResponse() {
        getBuildingResponse().add(UnitType.Terran_Bunker);
        getBuildingResponse().add(UnitType.Terran_Marine);
        getBuildingResponse().add(UnitType.Terran_Vulture);
        getBuildingResponse().add(UnitType.Terran_Vulture);
        getBuildingResponse().add(UnitType.Terran_Vulture);
        getBuildingResponse().add(UnitType.Terran_Vulture);
    }

    public void upgradeResponse() {
        getTechUpgradeResponse().add(TechType.Spider_Mines);
    }

    public HashMap<UnitType, Integer> getMoveOutCondition(BuildType buildType, Time time) {
        return new HashMap<>();
    }

    public HashSet<UnitType> removeBuildings() {
        return new HashSet<>();
    }
}
