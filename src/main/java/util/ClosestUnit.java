package util;

import bwapi.Position;
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

            if(friendlyUnit.isInBunker()) {
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

        for(EnemyUnits enemyUnit : enemyUnits) {
            Position enemyPosition = enemyUnit.getEnemyPosition();
            Position unitPosition = combatUnit.getUnit().getPosition();

            boolean burrowedLurker = (enemyUnit.getEnemyType() == UnitType.Zerg_Lurker && enemyUnit.wasBurrowed());

            //Stop units from getting stuck on outdated position info
            if(combatUnit.getUnit().getDistance(enemyPosition) < 250 && !enemyUnit.getEnemyUnit().exists() && !burrowedLurker) {
                enemyUnit.setEnemyPosition(null);
                continue;
            }

            if(!combatUnit.getUnit().hasPath(enemyPosition)) {
                continue;
            }

            if((!combatUnit.getUnit().getType().airWeapon().targetsAir() && !combatUnit.getUnit().isFlying()) && enemyUnit.getEnemyUnit().getType().isFlyer()) {
                continue;
            }

            if(((enemyUnit.getEnemyUnit().isCloaked() || enemyUnit.getEnemyUnit().isBurrowed()) && !enemyUnit.getEnemyUnit().isDetected()) || enemyUnit.getEnemyUnit().isMorphing()
                    || enemyUnit.getEnemyUnit().getType() == UnitType.Zerg_Overlord || enemyUnit.getEnemyUnit().getType() == UnitType.Protoss_Observer) {
                continue;
            }

            if(enemyPosition == null) {
                continue;
            }

            int distance = unitPosition.getApproxDistance(enemyPosition);

            if(distance < closestDistance) {
                closestDistance = distance;
                closestEnemy = enemyUnit;
            }
        }

        return closestEnemy;
    }

}
