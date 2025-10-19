package macro;

import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;
import information.enemy.EnemyUnits;
import macro.unitgroups.CombatUnits;
import macro.unitgroups.units.SiegeTank;

import java.util.HashSet;
import java.util.function.Predicate;

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

            if(friendlyUnit.getUnitType() != unitType) {
                if(!(friendlyUnit instanceof SiegeTank)) {
                    continue;
                }
            }

            if(friendlyUnit.getUnitType() == UnitType.Terran_Medic) {
                continue;
            }

            if(friendlyUnit.isInBunker()) {
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

//    public static void priorityTargets(CombatUnits combatUnit, HashSet<UnitType> priorityUnit, HashSet<EnemyUnits> enemyUnits, int range) {
//        int closestDistance = range;
//        EnemyUnits closestEnemy = null;
//
//        for(EnemyUnits enemyUnit : enemyUnits) {
//            if(!priorityUnit.contains(enemyUnit.getEnemyType())) {
//                continue;
//            }
//
//            Position enemyPosition = enemyUnit.getEnemyPosition();
//            Position unitPosition = combatUnit.getUnit().getPosition();
//
//            //Stop units from getting stuck on outdated position info
//            if(combatUnit.getUnit().getDistance(enemyPosition) < 250 && !enemyUnit.getEnemyUnit().exists()) {
//                enemyUnit.setEnemyPosition(null);
//                continue;
//            }
//
//            if(!combatUnit.getUnit().hasPath(enemyPosition)) {
//                continue;
//            }
//
//            if(!combatUnit.getUnit().getType().airWeapon().targetsAir() && enemyUnit.getEnemyUnit().getType().isFlyer()) {
//                continue;
//            }
//
//            if(enemyUnit.getEnemyUnit().isCloaked() || enemyUnit.getEnemyUnit().isBurrowed() || enemyUnit.getEnemyUnit().isMorphing()
//                    || enemyUnit.getEnemyUnit().getType() == UnitType.Zerg_Overlord || enemyUnit.getEnemyUnit().getType() == UnitType.Protoss_Observer) {
//                continue;
//            }
//
//            if(enemyPosition == null) {
//                continue;
//            }
//
//            int distance = unitPosition.getApproxDistance(enemyPosition);
//
//            if (distance < closestDistance) {
//                closestDistance = distance;
//                closestEnemy = enemyUnit;
//            }
//
//
//        }
//    }

    public static void priorityTargets(CombatUnits combatUnit, HashSet<UnitType> priorityUnit, HashSet<EnemyUnits> enemyUnits, int range) {
        HashSet<EnemyUnits> priorityEnemies = new HashSet<>();
        for(EnemyUnits enemyUnit : enemyUnits) {
            if(priorityUnit.contains(enemyUnit.getEnemyType())) {
                priorityEnemies.add(enemyUnit);
                combatUnit.setPriorityTargetExists(true);
            }
        }

        EnemyUnits closestEnemy = findClosestEnemyUnit(combatUnit, priorityEnemies, range);

        if(closestEnemy == null) {
            combatUnit.setPriorityTargetExists(false);
            closestEnemy = findClosestEnemyUnit(combatUnit, enemyUnits, range);
        }

        combatUnit.setEnemyUnit(closestEnemy);
    }

    private static EnemyUnits findClosestEnemyUnit(CombatUnits combatUnit, HashSet<EnemyUnits> enemyUnits, int range) {
        int closestDistance = range;
        EnemyUnits closestEnemy = null;

        for(EnemyUnits enemyUnit : enemyUnits) {
            Position enemyPosition = enemyUnit.getEnemyPosition();
            Position unitPosition = combatUnit.getUnit().getPosition();

            //Stop units from getting stuck on outdated position info
            if(combatUnit.getUnit().getDistance(enemyPosition) < 250 && !enemyUnit.getEnemyUnit().exists()) {
                enemyUnit.setEnemyPosition(null);
                continue;
            }

            if(!combatUnit.getUnit().hasPath(enemyPosition)) {
                continue;
            }

            if(!combatUnit.getUnit().getType().airWeapon().targetsAir() && enemyUnit.getEnemyUnit().getType().isFlyer()) {
                continue;
            }

            if(enemyUnit.getEnemyUnit().isCloaked() || enemyUnit.getEnemyUnit().isBurrowed() || enemyUnit.getEnemyUnit().isMorphing()
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
