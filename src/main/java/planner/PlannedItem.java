package planner;

import bwapi.*;
import unitgroups.units.Workers;

public class PlannedItem {
    private UnitType unitType;
    private UnitType techBuilding;
    private Integer supply = 0;
    private Integer upgradeLevel;
    private PlannedItemStatus plannedItemStatus;
    private PlannedItemType plannedItemType;
    private TilePosition buildPosition;
    private Workers assignedBuilder;
    private TechType techUpgrade;
    private UpgradeType upgradeType;
    private Unit addOnParent = null;
    private Unit productionBuilding;

    //priority 1-5, 1 being the highest
    private int priority;

    public PlannedItem(UnitType unitType, Integer supply, PlannedItemStatus plannedItemStatus, PlannedItemType plannedItemType, int priority) {
        this.unitType = unitType;
        this.supply = supply;
        this.plannedItemStatus = plannedItemStatus;
        this.plannedItemType = plannedItemType;
        this.priority = priority;
        assignedBuilder = null;
        buildPosition = null;
    }

    public PlannedItem(UnitType unitType, PlannedItemType plannedItemType, int priority) {
        this.unitType = unitType;
        this.plannedItemStatus = PlannedItemStatus.NOT_STARTED;
        this.priority = priority;
        this.plannedItemType = plannedItemType;
    }

    public PlannedItem(TechType techUpgrade, Integer supply, PlannedItemStatus plannedItemStatus, PlannedItemType plannedItemType, int priority) {
        this.supply = supply;
        this.plannedItemStatus = plannedItemStatus;
        this.plannedItemType = plannedItemType;
        this.techUpgrade = techUpgrade;
        this.priority = priority;
        this.upgradeType = null;
    }

    public PlannedItem(UpgradeType upgradeType, Integer supply, PlannedItemStatus plannedItemStatus, PlannedItemType plannedItemType, int priority) {
        this.supply = supply;
        this.plannedItemStatus = plannedItemStatus;
        this.plannedItemType = plannedItemType;
        this.upgradeType = upgradeType;
        this.priority = priority;
        this.techUpgrade = null;
    }

    public PlannedItem(TechType techUpgrade, Integer supply, PlannedItemStatus plannedItemStatus, PlannedItemType plannedItemType, UnitType techBuilding, int priority) {
        this.techUpgrade = techUpgrade;
        this.supply = supply;
        this.plannedItemStatus = plannedItemStatus;
        this.plannedItemType = plannedItemType;
        this.techBuilding = techBuilding;
        this.priority = priority;
    }

    public PlannedItem(UpgradeType upgradeType, Integer supply, PlannedItemStatus plannedItemStatus, PlannedItemType plannedItemType, UnitType techBuilding, Integer upgradeLevel, int priority) {
        this.upgradeType = upgradeType;
        this.supply = supply;
        this.plannedItemStatus = plannedItemStatus;
        this.plannedItemType = plannedItemType;
        this.techBuilding = techBuilding;
        this.upgradeLevel = upgradeLevel;
        this.priority = priority;
    }

    public PlannedItem(UnitType unitType, Integer supply, PlannedItemStatus plannedItemStatus, PlannedItemType plannedItemType, TilePosition buildPosition , int priority) {
        this.unitType = unitType;
        this.supply = supply;
        this.plannedItemStatus = plannedItemStatus;
        this.plannedItemType = plannedItemType;
        this.buildPosition = buildPosition;
        this.priority = priority;
    }

    public PlannedItem(UnitType unitType, PlannedItemStatus plannedItemStatus, PlannedItemType plannedItemType, int priority) {
        this.unitType = unitType;
        this.supply = 0;
        this.plannedItemStatus = plannedItemStatus;
        this.plannedItemType = plannedItemType;
        this.priority = priority;
        assignedBuilder = null;
        buildPosition = null;
    }

    public PlannedItem(TechType techUpgrade, PlannedItemStatus plannedItemStatus, PlannedItemType plannedItemType, UnitType techBuilding, int priority) {
        this.techUpgrade = techUpgrade;
        this.supply = 0;
        this.plannedItemStatus = plannedItemStatus;
        this.plannedItemType = plannedItemType;
        this.techBuilding = techBuilding;
        this.priority = priority;
    }

    public PlannedItem(UpgradeType upgradeType, PlannedItemStatus plannedItemStatus, PlannedItemType plannedItemType, UnitType techBuilding, Integer upgradeLevel, int priority) {
        this.upgradeType = upgradeType;
        this.supply = 0;
        this.plannedItemStatus = plannedItemStatus;
        this.plannedItemType = plannedItemType;
        this.techBuilding = techBuilding;
        this.upgradeLevel = upgradeLevel;
        this.priority = priority;
    }

    public PlannedItemStatus getPlannedItemStatus() {
        return plannedItemStatus;
    }

    public void setPlannedItemStatus(PlannedItemStatus plannedItemStatus) {
        this.plannedItemStatus = plannedItemStatus;
    }

    public PlannedItemType getPlannedItemType() {
        return plannedItemType;
    }

    public void setPlannedItemType(PlannedItemType plannedItemType) {
        this.plannedItemType = plannedItemType;
    }

    public UnitType getUnitType() {
        return unitType;
    }

    public void setUnitType(UnitType unitType) {
        this.unitType = unitType;
    }

    public Integer getSupply() {
        return supply;
    }

    public void setSupply(Integer supply) {
        this.supply = supply;
    }

    public TilePosition getBuildPosition() {
        return buildPosition;
    }

    public void setBuildPosition(TilePosition setBuildPosition) {
        this.buildPosition = setBuildPosition;
    }

    public Workers getAssignedBuilder() {
        return assignedBuilder;
    }

    public void setAssignedBuilder(Workers assignedBuilder) {
        this.assignedBuilder = assignedBuilder;
    }

    public UnitType getTechBuilding() {
        return techBuilding;
    }

    public void setTechBuilding(UnitType techBuilding) {
        this.techBuilding = techBuilding;
    }

    public TechType getTechUpgrade() {
        return techUpgrade;
    }

    public void setTechUpgrade(TechType techUpgrade) {
        this.techUpgrade = techUpgrade;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public UpgradeType getUpgradeType() {
        return upgradeType;
    }

    public void setUpgradeType(UpgradeType upgradeType) {
        this.upgradeType = upgradeType;
    }

    public Integer getUpgradeLevel() {
        return upgradeLevel;
    }

    public void setUpgradeLevel(Integer upgradeLevel) {
        this.upgradeLevel = upgradeLevel;
    }

    public Unit getAddOnParent() {
        return addOnParent;
    }

    public void setAddOnParent(Unit addOnParent) {
        this.addOnParent = addOnParent;
    }

    public Unit getProductionBuilding() {
        return productionBuilding;
    }

    public void setProductionBuilding(Unit productionBuilding) {
        this.productionBuilding = productionBuilding;
    }
}
