package information.enemy.enemytechunits;

import bwapi.UnitType;
import information.enemy.EnemyUnits;

import java.util.ArrayList;
import java.util.HashSet;

public abstract class EnemyTechUnits {
    private String techName;
    private UnitType unitType;
    private ArrayList<UnitType> friendlyBuildingResponse = new ArrayList<>();
    private boolean triggeredResponse = false;

    public EnemyTechUnits(String techName, UnitType unitType) {
        this.techName = techName;
        this.unitType = unitType;
    }

    public abstract boolean isEnemyTechUnit(HashSet<EnemyUnits> enemyUnits);
    public abstract void techBuildingResponse();
    public abstract UnitType techUnitResponse();

    public String getTechName() {
        return techName;
    }

    public UnitType getUnitType() {
        return unitType;
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
