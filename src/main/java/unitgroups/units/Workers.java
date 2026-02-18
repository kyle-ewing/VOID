package unitgroups.units;

import bwapi.Game;
import bwapi.Position;
import bwapi.Unit;
import information.enemy.EnemyUnits;
import macro.ResourceTracking;
import planner.PlannedItem;
import planner.PlannedItemStatus;

public class Workers extends CombatUnits {
    private Unit repairTarget;
    private WorkerStatus workerStatus;
    private int buildFrameCount;
    private int attackClock;
    private int idleClock = 0;
    private int lastFrameChecked = 0;
    private int unitID;
    private Integer distanceToBuildTarget = null;
    private EnemyUnits enemyUnit;
    private boolean preemptiveRepair = false;
    private boolean assignedToBase = false;
    private Position buildingPosition;

    public Workers(Game game, Unit unit) {
        super(game, unit);
        super.hasStaticStatus = true;
        this.workerStatus = WorkerStatus.IDLE;
        this.unitStatus = UnitStatus.WORKER;
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

    //Adjust worker position in case it gets stuck ontop of build tile
    public void pulseCheck() {
        if(buildingPosition == null) {
            workerStatus = WorkerStatus.IDLE;
            return;
        }

        if(unit.isIdle()) {
            idleClock++;
        }

        if(idleClock > 48) {
            unit.move(new Position(unit.getPosition().getX() + 2, unit.getPosition().getY() + 2));
            idleClock = 0;
        }
    }

    public void stuckCheck() {
        lastFrameChecked++;

        if(lastFrameChecked < 240) {
            return;
        }

        if(distanceToBuildTarget == null || buildingPosition == null) {
            return;
        }

        int currentDistance = unit.getDistance(buildingPosition);

        if(Math.abs(distanceToBuildTarget - currentDistance) < 32 && distanceToBuildTarget > 96) {
            setWorkerStatus(WorkerStatus.STUCK);
        }

    }

    public void build(PlannedItem pi, ResourceTracking resourceTracking) {
        resourceTracking.reserveResources(pi.getUnitType());
        this.setBuildingPosition(pi.getBuildPosition().toPosition());
        this.distanceToBuildTarget = this.getUnit().getDistance(pi.getBuildPosition().toPosition());
        this.getUnit().move(pi.getBuildPosition().toPosition());
        pi.setPlannedItemStatus(PlannedItemStatus.SCV_ASSIGNED);

        this.setWorkerStatus(WorkerStatus.MOVING_TO_BUILD);
    }

    //Build clock timeout, reset build
    public void buildReset(PlannedItem pi, ResourceTracking resourceTracking) {
        this.setWorkerStatus(WorkerStatus.IDLE);
        this.setBuildingPosition(null);
        this.getUnit().stop();
        this.idleClock = 0;
        resourceTracking.unreserveResources(pi.getUnitType());
        pi.setPlannedItemStatus(PlannedItemStatus.NOT_STARTED);
    }



//    public Unit getUnit() {
//        return unit;
//    }

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

    public Position getBuildingPosition() {
        return buildingPosition;
    }

    public void setBuildingPosition(Position buildingPosition) {
        this.buildingPosition = buildingPosition;
    }

    public int getDistanceToBuildTarget() {
        return distanceToBuildTarget;
    }

    public void setDistanceToBuildTarget(int distanceToBuildTarget) {
        this.distanceToBuildTarget = distanceToBuildTarget;
    }

    public int getLastFrameChecked() {
        return lastFrameChecked;
    }

    public void setLastFrameChecked(int lastFrameChecked) {
        this.lastFrameChecked = lastFrameChecked;
    }
}
