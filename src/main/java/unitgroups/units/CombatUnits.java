package unitgroups.units;

import java.util.HashSet;

import bwapi.Game;
import bwapi.Position;
import bwapi.TechType;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import information.enemy.EnemyUnits;

public class CombatUnits {
    protected Game game;
    protected Unit unit;
    protected UnitType unitType;
    protected UnitStatus unitStatus;
    protected CombatUnits friendlyUnit;
    protected EnemyUnits enemyUnit;
    protected EnemyUnits priorityEnemyUnit;
    protected EnemyUnits closestEnemyBuilding;
    protected TilePosition rallyPoint;
    protected Position regroupPosition;
    protected Position lastRegroupCheckPosition = null;

    protected HashSet<UnitType> priorityTargets = new HashSet<>();

    protected int unitID;
    protected int resetClock = 0;
    protected int targetRange = 200;
    protected int regroupStuckCheckTimer = 0;
    protected int regroupStuckCounter = 0;
    protected boolean inBunker;
    protected boolean enemyInBase = false;
    protected boolean inRangeOfThreat = false;
    protected boolean naturalRallySet = false;
    protected boolean hasTankSupport = false;
    protected boolean priorityTargetExists = false;
    protected boolean inBase = true;
    protected boolean hasStaticStatus = false;
    protected boolean ignoreCurrentPriorityTarget = false;
    protected boolean priorityTargetLock = false;
    protected boolean notNeeded = false;
    protected boolean dtUndetected = false;

    protected static final int STUCK_CHECK_INTERVAL = 24;
    protected static final int STUCK_THRESHOLD = 2;

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
        this.hasStaticStatus = true;
    }

    public CombatUnits(Game game, Unit unit, UnitStatus unitStatus, boolean hasStaticStatus) {
        this.game = game;
        this.unit = unit;
        this.unitType = unit.getType();
        this.unitID = unit.getID();
        this.rallyPoint = null;
        this.unitStatus = unitStatus;
        this.inBunker = false;
        this.hasStaticStatus = hasStaticStatus;
    }



    public void attack() {
        if (enemyUnit == null) {
            return;
        }

        if (!unit.isStimmed() && unit.isAttacking()) {
            unit.useTech(TechType.Stim_Packs);
        }

        if (!unit.isStartingAttack() && unit.getGroundWeaponCooldown() == 0 && !unit.isAttackFrame()) {
            unit.attack(enemyUnit.getEnemyPosition());
        }
    }

    public void rally() {
        if (rallyPoint == null) {
            return;
        }

        if (priorityEnemyUnit != null) {
            setEnemyUnit(priorityEnemyUnit);
            setUnitStatus(UnitStatus.DEFEND);
        }

        if (enemyUnit != null) {
            setUnitStatus(UnitStatus.DEFEND);
        }

        unit.attack(rallyPoint.toPosition());

    }

    public void defend() {
        if (enemyUnit == null) {
            setUnitStatus(UnitStatus.RALLY);
            return;
        }

        if (!inBase) {
            setUnitStatus(UnitStatus.RETREAT);
            return;
        }

        if (!enemyInBase) {
            setUnitStatus(UnitStatus.RALLY);
            return;
        }

        if (!unit.isStimmed() && unit.isAttacking()) {
            unit.useTech(TechType.Stim_Packs);
        }

        if (!unit.isStartingAttack() && unit.getGroundWeaponCooldown() == 0 && !unit.isAttackFrame()) {
            unit.attack(enemyUnit.getEnemyPosition());
        }

    }

    public void retreat() {
        if (enemyUnit == null) {
            return;
        }
        
        if (dtUndetected && rallyPoint != null) {
            unit.move(rallyPoint.toPosition());
            return;
        }

        if (!inRangeOfThreat) {
            setUnitStatus(UnitStatus.RALLY);
        }
    }

    public void avoid() {
        	if (enemyUnit == null) {
                return;
            }

            if (!inRangeOfThreat) {
                setUnitStatus(UnitStatus.RALLY);
            }
    }

    public void sallyOut() {
        	if (enemyUnit == null) {
                unit.move(rallyPoint.toPosition());
                return;
            }

            if (enemyInBase) {
                setUnitStatus(UnitStatus.DEFEND);
                return;
            }

            if (!unit.isStartingAttack() && unit.getGroundWeaponCooldown() == 0 && !unit.isAttackFrame()) {
                unit.attack(enemyUnit.getEnemyPosition());
            }
    }


    public void hunting() {

    }

    public void regroup() {
        if (regroupPosition == null) {
            return;
        }

        if (!game.isWalkable(regroupPosition.toWalkPosition())) {
            regroupStuckCounter = 0;
            lastRegroupCheckPosition = null;
            setUnitStatus(UnitStatus.ATTACK);
            return;
        }

        if (enemyUnit != null && enemyUnit.getEnemyUnit().getDistance(unit) < 200) {
            regroupStuckCounter = 0;
            lastRegroupCheckPosition = null;
            setUnitStatus(UnitStatus.ATTACK);
            return;
        }

        if (unit.getPosition().getDistance(regroupPosition) < 225) {
            regroupStuckCounter = 0;
            lastRegroupCheckPosition = null;
            setUnitStatus(UnitStatus.ATTACK);
            return;
        }

        regroupStuckCheckTimer++;
        if (regroupStuckCheckTimer >= STUCK_CHECK_INTERVAL) {
            regroupStuckCheckTimer = 0;
            if (lastRegroupCheckPosition != null && unit.getPosition().getApproxDistance(lastRegroupCheckPosition) < 16) {
                regroupStuckCounter++;
            }
            else {
                regroupStuckCounter = 0;
            }
            lastRegroupCheckPosition = unit.getPosition();
        }

        if (regroupStuckCounter >= STUCK_THRESHOLD) {
            regroupStuckCounter = 0;
            int dx = regroupPosition.getX() - unit.getPosition().getX();
            int dy = regroupPosition.getY() - unit.getPosition().getY();
            double length = Math.sqrt(dx * dx + dy * dy);
            if (length > 0) {
                int perpX = (int)(-dy * 100 / length);
                int perpY = (int)(dx * 100 / length);
                int moveX = Math.min(Math.max(unit.getPosition().getX() + perpX, 0), game.mapWidth() * 32);
                int moveY = Math.min(Math.max(unit.getPosition().getY() + perpY, 0), game.mapHeight() * 32);
                unit.move(new Position(moveX, moveY));
            }
            return;
        }

        unit.move(regroupPosition);
    }

    public void poke() {
        
    }

    public void liftedBuildings(Position bunkerPosition, Position naturalBaseCenter) {
        if (!notNeeded || !unit.isLifted()) {
            return;
        }

        if (unit.isUnderAttack()) {
            unit.move(bunkerPosition);
            return;
        }

        double dx = bunkerPosition.x - naturalBaseCenter.x;
        double dy = bunkerPosition.y - naturalBaseCenter.y;
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length == 0) {
            return;
        }

        Position target = new Position(
            (int)(bunkerPosition.x + (dx / length) * 160),
            (int)(bunkerPosition.y + (dy / length) * 160)
        );

        unit.move(target);
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

    public boolean isDtUndetected() {
        return dtUndetected;
    }

    public void setDtUndetected(boolean dtUndetected) {
        this.dtUndetected = dtUndetected;
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

    public EnemyUnits getPriorityEnemyUnit() {
        return priorityEnemyUnit;
    }

    public void setPriorityEnemyUnit(EnemyUnits priorityEnemyUnit) {
        this.priorityEnemyUnit = priorityEnemyUnit;
    }

    public boolean hasStaticStatus() {
        return hasStaticStatus;
    }

    public boolean ignoreCurrentPriorityTarget() {
        return ignoreCurrentPriorityTarget;
    }

    public void setIgnoreCurrentPriorityTarget(boolean ignoreCurrentPriorityTarget) {
        this.ignoreCurrentPriorityTarget = ignoreCurrentPriorityTarget;
    }

    public boolean priorityTargetLock() {
        return priorityTargetLock;
    }

    public void setPriorityTargetLock(boolean priorityTargetLock) {
        this.priorityTargetLock = priorityTargetLock;
    }

    public boolean notNeeded() {
        return notNeeded;
    }

    public void setNotNeeded(boolean notNeeded) {
        this.notNeeded = notNeeded;
    }

    public Position getRegroupPosition() {
        return regroupPosition;
    }

    public void setRegroupPosition(Position regroupPosition) {
        this.regroupPosition = regroupPosition;
    }

    
}
