package information.enemy.enemytechunits;

import bwapi.UnitType;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildType;
import planner.PlannedItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public abstract class EnemyTechUnits {
    private String techName;
    private final List<UnitType> bioResponse;
    private final List<UnitType> mechResponse;
    private ArrayList<UnitType> friendlyBuildingResponse = new ArrayList<>();
    private ArrayList<PlannedItem> friendlyUpgradeResponse = new ArrayList<>();
    private boolean isFlyer;
    private boolean triggeredResponse = false;

    public EnemyTechUnits(String techName, UnitType unitType, boolean isFlyer) {
        this(techName, isFlyer, Collections.singletonList(unitType), Collections.singletonList(unitType));
    }

    public EnemyTechUnits(String techName, boolean isFlyer, List<UnitType> response) {
        this(techName, isFlyer, response, response);
    }

    public EnemyTechUnits(String techName, boolean isFlyer, List<UnitType> bioResponse, List<UnitType> mechResponse) {
        this.techName = techName;
        this.isFlyer = isFlyer;
        this.bioResponse = bioResponse;
        this.mechResponse = mechResponse;
    }

    //PlannedItem used because of upgrade/tech difference
    public abstract boolean isEnemyTechUnit(HashSet<EnemyUnits> enemyUnits);
    public abstract void techBuildingResponse();
    public abstract void techUpgradeResponse();

    public boolean isResearchTriggered(HashSet<EnemyUnits> enemyUnits) {
        return false;
    }

    public String getTechName() {
        return techName;
    }

    public boolean isFlyer() {
        return isFlyer;
    }

    public List<UnitType> getResponseUnitTypes(BuildType buildType) {
        if (buildType == BuildType.MECH) {
            return mechResponse;
        }
        return bioResponse;
    }

    public ArrayList<UnitType> getFriendlyBuildingResponse() {
        return friendlyBuildingResponse;
    }

    public ArrayList<PlannedItem> getFriendlyUpgradeResponse() {
        return friendlyUpgradeResponse;
    }

    public boolean mineralLineTurretsOnly() {
        return false;
    }

    public boolean hasTriggeredResponse() {
        return triggeredResponse;
    }

    public void setTriggeredResponse(boolean triggeredResponse) {
        this.triggeredResponse = triggeredResponse;
    }
}
