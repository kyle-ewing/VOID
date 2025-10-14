package information.enemy.enemytechunits;

import bwapi.UnitType;
import information.enemy.EnemyUnits;

import java.util.ArrayList;
import java.util.HashSet;

public abstract class EnemyTechUnits {
    private String techName;
    private UnitType responseUnitType;
    private ArrayList<UnitType> friendlyBuildingResponse = new ArrayList<>();
    private boolean triggeredResponse = false;

    public EnemyTechUnits(String techName, UnitType unitType) {
        this.techName = techName;
        this.responseUnitType = unitType;
    }

    public abstract boolean isEnemyTechUnit(HashSet<EnemyUnits> enemyUnits);
    public abstract void techBuildingResponse();

    public String getTechName() {
        return techName;
    }

    public UnitType getResponseUnitType() {
        return responseUnitType;
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
