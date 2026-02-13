package util;

import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;
import information.enemy.EnemyUnits;
import unitgroups.units.CombatUnits;
import unitgroups.units.WorkerStatus;
import unitgroups.units.Workers;
import unitgroups.units.SiegeTank;
import map.PathFinding;

import java.util.HashSet;
import java.util.List;

public class ClosestUnit {
    public static void findClosestUnit(CombatUnits combatUnit, HashSet<EnemyUnits> enemyUnits, int range) {
        if(combatUnit.getUnitType() == UnitType.Terran_Medic) {
            return;
        }

        EnemyUnits closestEnemy = findClosestEnemyUnit(combatUnit, enemyUnits, range);

        if(closestEnemy == null) {
            combatUnit.setEnemyUnit(null);
            return;
        }

        combatUnit.setEnemyUnit(closestEnemy);
    }

    //UnitType looks for specific unit type to assign as friendly unit
    public static void findClosestFriendlyUnit(CombatUnits combatUnit, HashSet<CombatUnits> friendlyUnits, UnitType unitType) {
        CombatUnits closestUnit = null;
        int closestDistance = combatUnit.getTargetRange();

        if(combatUnit.getFriendlyUnit() != null && combatUnit.getFriendlyUnit().getUnit().exists()) {
            return;
        }

        for(CombatUnits friendlyUnit : friendlyUnits) {
            if(friendlyUnit.getUnitID() == combatUnit.getUnitID()) {
                continue;
            }

            if(friendlyUnit.getUnitType().isMechanical() && combatUnit.getUnitType() == UnitType.Terran_Medic ) {
                continue;
            }

            if(friendlyUnit.isInBunker() || friendlyUnit.getUnit().isLoaded()) {
                continue;
            }

            if(friendlyUnit.getUnitType() != unitType) {
                if(!(friendlyUnit instanceof SiegeTank)) {
                    continue;
                }
            }

            if(friendlyUnit.getUnitType() == UnitType.Terran_Medic) {
                continue;
            }

            if(combatUnit.getUnitType() == UnitType.Terran_Medic) {
                boolean alreadyAssigned = false;
                for(CombatUnits assignedUnit : friendlyUnits) {
                    if(assignedUnit.getUnitType() == UnitType.Terran_Medic && assignedUnit.getUnitID() != combatUnit.getUnitID()) {
                        if(assignedUnit.getFriendlyUnit() != null && assignedUnit.getFriendlyUnit().getUnit().getID() == friendlyUnit.getUnitID()) {
                            alreadyAssigned = true;
                            break;
                        }
                    }
                }

                if(alreadyAssigned) {
                    continue;
                }
            }

            if(!friendlyUnit.getUnitType().isMechanical() || unitType.isMechanical()) {
                int distance = combatUnit.getUnit().getDistance(friendlyUnit.getUnit());
                if(distance < closestDistance) {
                    closestUnit = friendlyUnit;
                    closestDistance = distance;
                }
            }
        }

        if(closestUnit != null) {
            combatUnit.setFriendlyUnit(closestUnit);
        }
        else {
            combatUnit.setFriendlyUnit(null);
        }
    }

    public static void priorityTargets(CombatUnits combatUnit, HashSet<UnitType> priorityUnit, HashSet<EnemyUnits> enemyUnits, int range) {
        HashSet<EnemyUnits> priorityEnemies = new HashSet<>();

        if(!enemyUnits.contains(combatUnit.getPriorityEnemyUnit())) {
            combatUnit.setPriorityEnemyUnit(null);
        }

        if (combatUnit.priorityTargetLock()) {
            return;
        }

        for(EnemyUnits enemyUnit : enemyUnits) {
            if(priorityUnit.contains(enemyUnit.getEnemyType())) {
                if(combatUnit.getPriorityEnemyUnit() == enemyUnit && combatUnit.ignoreCurrentPriorityTarget()) {
                    continue;
                }

                priorityEnemies.add(enemyUnit);
                combatUnit.setPriorityTargetExists(true);
            }
        }

        EnemyUnits closestEnemy = findClosestEnemyUnit(combatUnit, priorityEnemies, range);

        combatUnit.setPriorityEnemyUnit(closestEnemy);
    }

    //Closest worker to build position
    public static Workers findClosestWorker(Position position, HashSet<Workers> workers, PathFinding pathFinding) {
        Workers closestWorker = null;
        int closestDistance = Integer.MAX_VALUE;

        for(Workers worker : workers) {
            if(worker.getWorkerStatus() != WorkerStatus.MINERALS) {
                continue;
            }

            Position workerPos = worker.getUnit().getPosition();
            List<Position> path = pathFinding.findPath(workerPos, position);

            if (path == null || path.isEmpty()) {
                continue;
            }

            int pathLen = path.size();
            if (pathLen < closestDistance) {
                closestDistance = pathLen;
                closestWorker = worker;
            }
        }


        return closestWorker;
    }

    public static EnemyUnits findClosestEnemyUnit(CombatUnits combatUnit, HashSet<EnemyUnits> enemyUnits, int range) {
        int closestDistance = range;
        EnemyUnits closestEnemy = null;

        //Priority tracking for sunks over creep colonies
        EnemyUnits prioritySunk = null;

        for(EnemyUnits enemyUnit : enemyUnits) {
            Position enemyPosition = enemyUnit.getEnemyPosition();
            Position unitPosition = combatUnit.getUnit().getPosition();

            if(enemyUnit.getEnemyUnit().isVisible() && enemyUnit.getEnemyType() == UnitType.Zerg_Sunken_Colony) {
                prioritySunk = enemyUnit;
            }

            if(combatUnit.getUnit().getDistance(enemyPosition) > range) {
                continue;
            }

            //Stop units from getting stuck on outdated position info
            if(combatUnit.getUnit().getDistance(enemyPosition) < 250 && !enemyUnit.getEnemyUnit().exists()) {
                boolean burrowedLurker = (enemyUnit.getEnemyType() == UnitType.Zerg_Lurker && enemyUnit.wasBurrowed());

                if(burrowedLurker) {
                    if(hasDetectionNearPosition(enemyUnit, combatUnit)) {
                        enemyUnit.setEnemyPosition(null);
                        continue;
                    }
                }
                else {
                    enemyUnit.setEnemyPosition(null);
                }

                //Hard force position reset if units literally on top of an enemy that is clearly not there anymore
                if(combatUnit.getUnit().getDistance(enemyPosition) < 10 && !enemyUnit.getEnemyUnit().isVisible()) {
                    enemyUnit.setEnemyPosition(null);
                    continue;
                }

                continue;
            }

            if(!combatUnit.getUnit().hasPath(enemyPosition)) {
                continue;
            }

            if((!combatUnit.getUnit().getType().airWeapon().targetsAir() && !combatUnit.getUnit().isFlying()) && enemyUnit.getEnemyUnit().getType().isFlyer()) {
                continue;
            }

            if(((enemyUnit.getEnemyUnit().isCloaked() || enemyUnit.getEnemyUnit().isBurrowed()) && !enemyUnit.getEnemyUnit().isDetected())
                    || enemyUnit.getEnemyUnit().getType() == UnitType.Zerg_Overlord
                    || enemyUnit.getEnemyUnit().getType() == UnitType.Protoss_Observer
                    || enemyUnit.getEnemyUnit().getType() == UnitType.Zerg_Larva
                    || enemyUnit.getEnemyUnit().getType() == UnitType.Zerg_Egg) {
                continue;
            }

            if(enemyPosition == null) {
                continue;
            }

            int distance = unitPosition.getApproxDistance(enemyPosition);


            //Edge case focus sunk over creep colony
            if(closestEnemy != null
                    && prioritySunk != null
                    && enemyUnit.getEnemyType() == UnitType.Zerg_Creep_Colony
                    && enemyUnit.getEnemyUnit().isVisible()) {

                closestDistance = unitPosition.getApproxDistance(prioritySunk.getEnemyPosition());
                closestEnemy = prioritySunk;
            }
            //Focus units over buildings if visible and not static defense
            else if(closestEnemy != null && closestEnemy.getEnemyType().isBuilding()
                    && enemyUnit.getEnemyUnit().isVisible()
                    && distance < 500
                    && (!enemyUnit.getEnemyType().isBuilding() || isStaticDefense(enemyUnit.getEnemyType()))) {
                closestDistance = distance;
                closestEnemy = enemyUnit;
            }
            else if(distance < closestDistance
                    && (closestEnemy == null
                    || !closestEnemy.getEnemyType().isBuilding()
                    || !isStaticDefense(closestEnemy.getEnemyType())
                    || isStaticDefense(enemyUnit.getEnemyType()))) {

                closestDistance = distance;
                closestEnemy = enemyUnit;
            }


        }

        return closestEnemy;
    }

    private static boolean hasDetectionNearPosition(EnemyUnits enemyUnit, CombatUnits combatUnit) {
        if(!combatUnit.getUnitType().isDetector()) {
            return false;
        }

        int detectionRange = 0;

        if(combatUnit.getUnitType() == UnitType.Terran_Missile_Turret) {
            detectionRange = 224;
        }
        else {
            detectionRange = 320;
        }

        return combatUnit.getUnit().getDistance(enemyUnit.getEnemyPosition()) < detectionRange;
    }

    private static boolean isStaticDefense(UnitType unitType) {
        return unitType == UnitType.Zerg_Sunken_Colony
                || unitType == UnitType.Zerg_Creep_Colony
                || unitType == UnitType.Protoss_Photon_Cannon
                || unitType == UnitType.Terran_Missile_Turret
                || unitType == UnitType.Terran_Bunker;
    }

}
