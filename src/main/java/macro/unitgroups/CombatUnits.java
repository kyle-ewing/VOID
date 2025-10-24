package macro.unitgroups;

import bwapi.*;
import information.enemy.EnemyUnits;

import java.util.HashSet;

public class CombatUnits {
    protected Game game;
    protected Unit unit;
    protected UnitType unitType;
    protected UnitStatus unitStatus;
    protected CombatUnits friendlyUnit;
    protected EnemyUnits enemyUnit;
    protected EnemyUnits closestEnemyBuilding;
    protected TilePosition rallyPoint;

    protected HashSet<UnitType> priorityTargets = new HashSet<>();

    protected int unitID;
    protected int resetClock = 0;
    protected int targetRange = 200;
    protected boolean inBunker;
    protected boolean enemyInBase = false;
    protected boolean inRangeOfThreat = false;
    protected boolean naturalRallySet = false;
    protected boolean hasTankSupport = false;
    protected boolean priorityTargetExists = false;
    protected boolean inBase = true;

    public CombatUnits(Game game, Unit unit) {
        this.game = game;
        this.unit = unit;
        this.unitType = unit.getType();
        this.unitID = unit.getID();
        this.rallyPoint = null;
        this.unitStatus = UnitStatus.RALLY;
        this.inBunker = false;
    }

    public CombatUnits(Unit unit, UnitStatus unitStatus) {
        this.unit = unit;
        this.unitType = unit.getType();
        this.unitID = unit.getID();
        this.rallyPoint = null;
        this.unitStatus = unitStatus;
        this.inBunker = false;
    }

    public CombatUnits(Game game, Unit unit, UnitStatus unitStatus) {
        this.game = game;
        this.unit = unit;
        this.unitType = unit.getType();
        this.unitID = unit.getID();
        this.rallyPoint = null;
        this.unitStatus = unitStatus;
        this.inBunker = false;
    }



    public void attack() {
        if(enemyUnit == null) {
            return;
        }

        if(!unit.isStimmed() && unit.isAttacking()) {
            unit.useTech(TechType.Stim_Packs);
        }

        if(!unit.isStartingAttack() && unit.getGroundWeaponCooldown() == 0 && !unit.isAttackFrame()) {
            unit.attack(enemyUnit.getEnemyPosition());
        }
    }

    public void rally() {
        if(rallyPoint == null) {
            return;
        }

        if(enemyUnit != null) {
            setUnitStatus(UnitStatus.DEFEND);
        }

        unit.attack(rallyPoint.toPosition());

    }

    public void defend() {
        if(enemyUnit == null) {
            setUnitStatus(UnitStatus.RALLY);
            return;
        }

        if(!unit.isStimmed() && unit.isAttacking()) {
            unit.useTech(TechType.Stim_Packs);
        }

        if(!unit.isStartingAttack() && unit.getGroundWeaponCooldown() == 0 && !unit.isAttackFrame()) {
            unit.attack(enemyUnit.getEnemyPosition());
        }

    }

    public void retreat() {
        if(enemyUnit == null) {
            return;
        }

        if(!inRangeOfThreat) {
            setUnitStatus(UnitStatus.ATTACK);
        }
    }

    public void hunting() {

    }

    public void onFrame() {

    }

    public int getResetClock() {
        return resetClock;
    }

    public void setResetClock(int resetClock) {
        this.resetClock = resetClock;
    }

    public Unit getUnit() {
        return unit;
    }

    public UnitStatus getUnitStatus() {
        return unitStatus;
    }

    public void setUnitStatus(UnitStatus unitStatus) {
        this.unitStatus = unitStatus;
    }

    public int getUnitID() {
        return unitID;
    }

    public UnitType getUnitType() {
        return unitType;
    }

    //Siege tanks morph type
    public void setUnitType(UnitType unitType) {
        this.unitType = unitType;
    }

    public void setEnemyUnit(EnemyUnits enemyUnit) {
        this.enemyUnit = enemyUnit;
    }

    public EnemyUnits getEnemyUnit() {
        return enemyUnit;
    }

    public TilePosition getRallyPoint() {
        return rallyPoint;
    }

    public void setRallyPoint(TilePosition rallyPoint) {
        this.rallyPoint = rallyPoint;
    }

    public boolean isInBunker() {
        return inBunker;
    }

    public void setInBunker(boolean inBunker) {
        this.inBunker = inBunker;
    }

    public CombatUnits getFriendlyUnit() {
        return friendlyUnit;
    }

    public void setFriendlyUnit(CombatUnits friendlyUnit) {
        this.friendlyUnit = friendlyUnit;
    }

    public int getTargetRange() {
        return targetRange;
    }

    public void setTargetRange(int targetRange) {
        this.targetRange = targetRange;
    }

    public boolean isEnemyInBase() {
        return enemyInBase;
    }

    public void setEnemyInBase(boolean enemyInBase) {
        this.enemyInBase = enemyInBase;
    }

    public boolean isInRangeOfThreat() {
        return inRangeOfThreat;
    }

    public void setInRangeOfThreat(boolean inRangeOfThreat) {
        this.inRangeOfThreat = inRangeOfThreat;
    }

    public boolean isNaturalRallySet() {
        return naturalRallySet;
    }

    public void setNaturalRallySet(boolean naturalRallySet) {
        this.naturalRallySet = naturalRallySet;
    }

    public boolean hasTankSupport() {
        return hasTankSupport;
    }

    public void setHasTankSupport(boolean hasTankSupport) {
        this.hasTankSupport = hasTankSupport;
    }

    public HashSet<UnitType> getPriorityTargets() {
        return priorityTargets;
    }

    public boolean isPriorityTargetExists() {
        return priorityTargetExists;
    }

    public void setPriorityTargetExists(boolean priorityTargetExists) {
        this.priorityTargetExists = priorityTargetExists;
    }

    public boolean isInBase() {
        return inBase;
    }

    public void setInBase(boolean inBase) {
        this.inBase = inBase;
    }
}
