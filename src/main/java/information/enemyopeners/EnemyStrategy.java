package information.enemyopeners;

import bwapi.UnitType;
import information.EnemyUnits;

import java.util.ArrayList;
import java.util.HashSet;

public abstract class EnemyStrategy {
    private String strategyName;
    private ArrayList<UnitType> buildingResponse = new ArrayList<>();

    public EnemyStrategy(String strategyName) {
        this.strategyName = strategyName;
    }

    public abstract boolean isEnemyStrategy(HashSet<EnemyUnits> enemyUnits, int frameCount);
    public abstract void buildingResponse();

    public String getStrategyName() {
        return strategyName;
    }

    public ArrayList<UnitType> getBuildingResponse() {
        return buildingResponse;
    }
}
