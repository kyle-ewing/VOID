package unitgroups.units;

import bwapi.Game;
import bwapi.Unit;
import bwapi.UnitCommand;
import bwapi.UnitCommandType;

public class Goliath extends CombatUnits {
    public Goliath(Game game, Unit unit) {
        super(game, unit);
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
            unit.move(rallyPoint.toPosition());
            return;
        }

        if (enemyInBase) {
            setUnitStatus(UnitStatus.DEFEND);
            return;
        }

        attackUnit();
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

        if (enemyUnit.getEnemyUnit().isVisible()) {
            issueAttack(enemyUnit.getEnemyUnit());
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
}
