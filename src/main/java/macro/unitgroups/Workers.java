package macro.unitgroups;

import bwapi.Unit;
import information.EnemyUnits;

public class Workers {
    private Unit unit;
    private Unit repairTarget;
    private WorkerStatus workerStatus;
    private int buildFrameCount;
    private int attackClock;
    private int idleClock = 0;
    private int unitID;
    private EnemyUnits enemyUnit;
    private boolean preemptiveRepair = false;
    private boolean assignedToBase = false;

    public Workers(Unit unit, WorkerStatus workerStatus) {
        this.unit = unit;
        this.workerStatus = workerStatus;
        this.unitID = unit.getID();
        this.buildFrameCount = 0;
        this.attackClock = 0;
    }

    public void selfDefense() {
        if (enemyUnit == null) {
            return;
        }

        if (!unit.isAttackFrame()) {
            attackClock = 0;
            unit.attack(enemyUnit.getEnemyPosition());
        }
    }

    public void repair(Unit target) {
        if (target != null && target.getHitPoints() < target.getType().maxHitPoints() && !unit.isRepairing()) {
            unit.repair(target);
            return;
        }

        if(target != null && preemptiveRepair && target.getHitPoints() == target.getType().maxHitPoints()) {
            unit.move(target.getPosition());
            return;
        }

        if(target == null || target.getHitPoints() >= target.getType().maxHitPoints()  && !preemptiveRepair) {
            workerStatus = WorkerStatus.IDLE;
            repairTarget = null;
        }
    }


    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public WorkerStatus getWorkerStatus() {
        return workerStatus;
    }

    public void setWorkerStatus(WorkerStatus workerStatus) {
        this.workerStatus = workerStatus;
    }

    public int getBuildFrameCount() {
        return buildFrameCount;
    }

    public void setBuildFrameCount(int buildFrameCount) {
        this.buildFrameCount = buildFrameCount;
    }

    public int getAttackClock() {
        return attackClock;
    }

    public void setAttackClock(int attackClock) {
        this.attackClock = attackClock;
    }

    public EnemyUnits getEnemyUnit() {
        return enemyUnit;
    }

    public void setEnemyUnit(EnemyUnits enemyUnit) {
        this.enemyUnit = enemyUnit;
    }

    public Unit getRepairTarget() {
        return repairTarget;
    }

    public void setRepairTarget(Unit repairTarget) {
        this.repairTarget = repairTarget;
    }

    public int getUnitID() {
        return unitID;
    }

    public void setUnitID(int unitID) {
        this.unitID = unitID;
    }

    public int getIdleClock() {
        return idleClock;
    }

    public void setIdleClock(int idleClock) {
        this.idleClock = idleClock;
    }

    public boolean isPreemptiveRepair() {
        return preemptiveRepair;
    }

    public void setPreemptiveRepair(boolean preemptiveRepair) {
        this.preemptiveRepair = preemptiveRepair;
    }

    public boolean isAssignedToBase() {
        return assignedToBase;
    }

    public void setAssignedToBase(boolean assignedToBase) {
        this.assignedToBase = assignedToBase;
    }
}
