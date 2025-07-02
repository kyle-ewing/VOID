package macro.unitgroups.units;

import bwapi.*;
import javafx.geometry.Pos;
import macro.unitgroups.CombatUnits;
import macro.unitgroups.UnitStatus;

public class Vulture extends CombatUnits {
    public Vulture(Game game, Unit unit) {
        super(game, unit);
        unitStatus = UnitStatus.ATTACK;
    }

    @Override
    public void attack() {
        if(enemyUnit == null) {
            return;
        }

        if(minimnumThreshold(1.05)) {
            unit.patrol(kiteTo());
        }
        else if(minimnumThreshold(0.5)) {
            unit.move(kiteTo());
        }
        else {
            unit.move(enemyUnit.getEnemyPosition());
        }
    }

    private Position kiteTo() {
        Position enemyPos = enemyUnit.getEnemyPosition();
        Position unitPos = unit.getPosition();
        int dx = unitPos.getX() - enemyPos.getX();
        int dy = unitPos.getY() - enemyPos.getY();

        int patrolDistance = weaponRange();
        double moveX = unitPos.getX() + (dx * patrolDistance / Math.max(1, unitPos.getDistance(enemyPos)));
        double moveY = unitPos.getY() + (dy * patrolDistance / Math.max(1, unitPos.getDistance(enemyPos)));

        moveX = Math.min(Math.max(moveX, 0), game.mapWidth() * 32);
        moveY = Math.min(Math.max(moveY, 0), game.mapHeight() * 32);
        return new Position((int) moveX, (int) moveY);
    }

    private boolean minimnumThreshold(double threshold) {
        double halfRange = weaponRange() * threshold;
        Position enemyPosition = enemyUnit.getEnemyPosition();
        Position unitPosition = unit.getPosition();
        double distanceToEnemy = unitPosition.getDistance(enemyPosition);

        return distanceToEnemy < halfRange;
    }

    private int weaponRange() {
        WeaponType weaponType = unit.getType().groundWeapon();
            return weaponType.maxRange();
    }
}
