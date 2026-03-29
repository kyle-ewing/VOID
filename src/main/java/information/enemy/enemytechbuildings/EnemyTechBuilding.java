package information.enemy.enemytechbuildings;

import bwapi.UnitType;
import information.enemy.EnemyUnits;

import java.util.ArrayList;
import java.util.HashSet;

public abstract class EnemyTechBuilding {
    protected String buildingName;
    protected UnitType buildingType;
    protected ArrayList<UnitType> friendlyBuildingResponse = new ArrayList<>();
    protected boolean triggeredResponse = false;

    public EnemyTechBuilding(String buildingName, UnitType buildingType) {
        this.buildingName = buildingName;
        this.buildingType = buildingType;
    }

    public abstract boolean isEnemyBuilding(HashSet<EnemyUnits> enemyUnits);
    public abstract void friendlyBuildingResponse();

    public String getBuildingName() {
        return buildingName;
    }

    public UnitType getBuildingType() {
        return buildingType;
    }

    public ArrayList<UnitType> getFriendlyBuildingResponse() {
        return friendlyBuildingResponse;
    }

    public boolean hasTriggeredResponse() {
        return triggeredResponse;
    }

    public void setTriggeredResponse(boolean triggeredResponse) {
        this.triggeredResponse = triggeredResponse;
    }

}
