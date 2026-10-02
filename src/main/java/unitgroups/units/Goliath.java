package unitgroups.units;

import java.util.HashSet;

import bwapi.Game;
import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitCommand;
import bwapi.UnitCommandType;
import bwapi.WeaponType;
import information.MapInfo;
import information.enemy.EnemyInformation;
import information.enemy.EnemyUnits;
import macro.buildorders.BuildOrderName;
import map.bwemwrappers.Base;
import util.ClosestUnit;

public class Goliath extends CombatUnits {
    private EnemyInformation enemyInformation;
    private MapInfo mapInfo;
    private HashSet<EnemyUnits> enemyUnits;
    private HashSet<Base> visitedExpansions = new HashSet<>();
    private boolean runbyStagingComplete = false;

    public Goliath(Game game, EnemyInformation enemyInformation, Unit unit) {
        super(game, unit);
        this.enemyInformation = enemyInformation;
        this.enemyUnits = enemyInformation.getEnemyUnits();
        this.mapInfo = enemyInformation.getBaseInfo();
    }

    @Override
    public void attack() {
        if (enemyUnit == null) {
            return;
        }

        attackUnit();
    }

    @Override
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

        attackUnit();
    }

    @Override
    public void sallyOut() {
        if (enemyUnit == null) {
            if (rallyPoint != null) {
                unit.move(rallyPoint.toPosition());
            }
            return;
        }

        if (enemyInBase) {
            setUnitStatus(UnitStatus.DEFEND);
            return;
        }

        attackUnit();
    }

    @Override
    public void runby() {
        Base targetBase = null;
        EnemyUnits depot = null;

        for (Base expansion : mapInfo.runbyTargets(unit.getPosition(), enemyUnits)) {
            if (visitedExpansions.contains(expansion)) {
                continue;
            }

            EnemyUnits expansionDepot = mapInfo.findEnemyDepotNearBase(expansion, enemyUnits);
            if (expansionDepot == null && unit.getDistance(expansion.getCenter()) < 200) {
                visitedExpansions.add(expansion);
                continue;
            }

            targetBase = expansion;
            depot = expansionDepot;
            break;
        }

        if (targetBase == null) {
            setInRunbySquad(false);
            setUnitStatus(UnitStatus.RALLY);
            if (rallyPoint != null) {
                unit.move(rallyPoint.toPosition());
            }
            return;
        }

        Position destination = targetBase.getCenter();
        if (depot != null) {
            destination = mapInfo.runbyAttackPos(targetBase, depot);
        }

        if (depot != null && unit.getDistance(destination) < 400) {
            HashSet<EnemyUnits> workers = new HashSet<>();
            for (EnemyUnits enemy : enemyUnits) {
                if (enemy.getEnemyType().isWorker()) {
                    workers.add(enemy);
                }
            }

            EnemyUnits closestWorker = ClosestUnit.findClosestEnemyUnit(this, workers, 400);
            if (closestWorker != null) {
                runbyStagingComplete = true;
                setEnemyUnit(closestWorker);
                attackUnit();
                return;
            }
        }

        if (depot != null && unit.getDistance(destination) < 150) {
            runbyStagingComplete = true;

            if (enemyUnit != null && enemyUnit.getEnemyPosition() != null) {
                attackUnit();
                return;
            }

            if (depot.getEnemyUnit() != null && depot.getEnemyUnit().isVisible()) {
                issueAttack(depot.getEnemyUnit());
                return;
            }

            unit.attack(destination);
            return;
        }

        HashSet<EnemyUnits> inRunbyRange = new HashSet<>();
        for (EnemyUnits enemy : enemyUnits) {
            if (enemy.getEnemyPosition() == null) {
                continue;
            }

            WeaponType weapon = unit.getType().groundWeapon();
            if (enemy.getEnemyType().isFlyer()) {
                weapon = unit.getType().airWeapon();
            }

            if (unit.getDistance(enemy.getEnemyPosition()) > game.self().weaponMaxRange(weapon) - 64) {
                continue;
            }

            inRunbyRange.add(enemy);
        }

        EnemyUnits closeTarget = ClosestUnit.findClosestEnemyUnit(this, inRunbyRange, Integer.MAX_VALUE);
        if (closeTarget != null) {
            setEnemyUnit(closeTarget);
            attackUnit();
            return;
        }

        BuildOrderName buildOrderName = null;
        if (enemyInformation.getStartingOpener() != null) {
            buildOrderName = enemyInformation.getStartingOpener().getBuildOrderName();
        }

        Base stagingBase = mapInfo.runbyStagingBase(buildOrderName, enemyUnits);
        if (stagingBase != null && unit.getDistance(stagingBase.getCenter()) < 160) {
            runbyStagingComplete = true;
        }
        else if (!runbyStagingComplete && stagingBase != null
                && unit.getDistance(stagingBase.getCenter()) < 1400
                && unit.getDistance(stagingBase.getCenter()) < unit.getDistance(targetBase.getCenter())) {
            issueMove(stagingBase.getCenter());
            return;
        }

        runbyStagingComplete = true;
        issueMove(destination);
    }

    @Override
    public void resetRunby() {
        visitedExpansions.clear();
        runbyStagingComplete = false;
    }

    @Override
    public boolean enemyInWeaponRange(int buffer) {
        if (enemyUnit == null || enemyUnit.getEnemyPosition() == null) {
            return false;
        }

        if (enemyUnit.getEnemyUnit().isFlying()) {
            return unit.getDistance(enemyUnit.getEnemyPosition()) <= game.self().weaponMaxRange(unit.getType().airWeapon()) + buffer;
        }

        return unit.getDistance(enemyUnit.getEnemyPosition()) <= game.self().weaponMaxRange(unit.getType().groundWeapon()) + buffer;
    }

    private void attackUnit() {
        if (unit.isStartingAttack() || unit.isAttackFrame()) {
            return;
        }

        if (unit.getLastCommandFrame() >= game.getFrameCount()) {
            return;
        }

        if (enemyUnit.getEnemyUnit() != null && enemyUnit.getEnemyUnit().isVisible()) {
            issueAttack(enemyUnit.getEnemyUnit());
            return;
        }

        if (enemyUnit.getEnemyPosition() == null) {
            return;
        }

        unit.attack(enemyUnit.getEnemyPosition());
    }

    private void issueAttack(Unit target) {
        UnitCommand lastCommand = unit.getLastCommand();
        if (lastCommand != null
                && lastCommand.getType() == UnitCommandType.Attack_Unit
                && lastCommand.getTarget() == target
                && !unit.isIdle()) {
            return;
        }

        unit.attack(target);
    }

    private void issueMove(Position destination) {
        UnitCommand lastCommand = unit.getLastCommand();
        if (lastCommand != null
                && lastCommand.getType() == UnitCommandType.Move
                && destination.equals(lastCommand.getTargetPosition())
                && !unit.isIdle()) {
            return;
        }

        unit.move(destination);
    }
}
