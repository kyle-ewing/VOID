package macro.unitgroups;

import bwapi.*;
import information.EnemyUnits;

public class CombatUnits {
    protected Game game;
    protected Unit unit;
    protected Unit friendlyUnit;
    protected UnitType unitType;
    protected UnitStatus unitStatus;
    protected EnemyUnits enemyUnit;
    protected EnemyUnits closestEnemyBuilding;
    protected TilePosition rallyPoint;

    protected int unitID;
    protected int resetClock = 0;
    protected int targetRange = 200;
    protected boolean inBunker;
    protected boolean enemyInBase = false;
    protected boolean inRangeOfThreat = false;

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

    public Unit getFriendlyUnit() {
        return friendlyUnit;
    }

    public void setFriendlyUnit(Unit friendlyUnit) {
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
}
