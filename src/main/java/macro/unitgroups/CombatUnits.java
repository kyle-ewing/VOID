package macro.unitgroups;

import bwapi.TechType;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import debug.Painters;
import information.EnemyUnits;

public class CombatUnits {
    private Unit unit;
    private UnitType unitType;
    private UnitStatus unitStatus;
    private EnemyUnits enemyUnit;
    private EnemyUnits closestEnemyBuilding;
    private TilePosition rallyPoint;

    private int unitID;

    public CombatUnits(Unit unit) {
        this.unit = unit;
        this.unitType = unit.getType();
        this.unitID = unit.getID();
        this.rallyPoint = null;
        this.unitStatus = UnitStatus.RALLY;
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

        unit.attack(rallyPoint.toPosition());
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
}
