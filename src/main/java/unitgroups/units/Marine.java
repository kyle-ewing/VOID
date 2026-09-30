package unitgroups.units;

import bwapi.Game;
import bwapi.Order;
import bwapi.Position;
import bwapi.TechType;
import bwapi.Unit;
import bwapi.UnitType;
import bwapi.UpgradeType;
import bwapi.WeaponType;

public class Marine extends CombatUnits {
    private static final int UPGRADE_RANGE = 32;
    private static final double[] KITE_ANGLE_OFFSETS = {0, 0.5236, -0.5236, 1.0472, -1.0472, 1.5708, -1.5708};
    private Integer badTargetID = null;

    public Marine(Game game, Unit unit) {
        super(game, unit);
    }

    @Override
    public void rally() {
        if (rallyPoint == null) {
            return;
        }

        if (priorityEnemyUnit != null) {
            setEnemyUnit(priorityEnemyUnit);
            setUnitStatus(UnitStatus.DEFEND);
        }

        if (enemyUnit != null && enemyInBase) {
            setUnitStatus(UnitStatus.DEFEND);
        }

        if (inBase || unit.getDistance(rallyPoint.toPosition()) < 96) {
            unit.attack(rallyPoint.toPosition());
        }
        else {
            setUnitStatus(UnitStatus.RETREAT);
        }

        if (unit.getDistance(rallyPoint.toPosition()) < 96) {
            if (enemyUnit != null && enemyUnit.getEnemyPosition() != null) {
                unit.attack(enemyUnit.getEnemyPosition());
            }
            else {
                unit.attack(rallyPoint.toPosition());
            }
        }

    }

    @Override
    public void attack() {
        if (enemyUnit == null) {
            return;
        }

        if (super.isInBunker()) {
            inBunker = false;
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
    public void retreat() {
        if (enemyUnit == null || enemyUnit.getEnemyPosition() == null) {
            return;
        }

        if (dtUndetected && rallyPoint != null) {
            unit.move(rallyPoint.toPosition());
            return;
        }

        unit.move(rallyPoint.toPosition());

        if (inBase || hasTankSupport || unit.getDistance(rallyPoint.toPosition()) < 48) {
            setUnitStatus(UnitStatus.RALLY);
        }
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

        if (enemyUnit.getEnemyType() == UnitType.Terran_Siege_Tank_Siege_Mode
                && unit.getPosition().getDistance(enemyUnit.getEnemyPosition()) > 48) {
            unit.move(enemyUnit.getEnemyPosition());
            return;
        }

        attackUnit();
    }

    @Override
    public void microOnFrame() {
        if (unitStatus != UnitStatus.ATTACK && unitStatus != UnitStatus.DEFEND && unitStatus != UnitStatus.SALLYOUT) {
            return;
        }

        if (enemyUnit == null) {
            return;
        }

        kite();
    }

    private void attackUnit() {
        if (unitStatus != UnitStatus.SALLYOUT && meleeTooClose()) {
            return;
        }

        if (!unit.isStimmed() && unit.isAttacking()) {
            unit.useTech(TechType.Stim_Packs);
        }

        if (unit.getOrderTarget() != null && unit.getTarget() != null && unit.getOrderTarget().getID() != enemyUnit.getEnemyID() && unit.isAttacking()) {
            if (badTargetID == null || badTargetID != unit.getTarget().getID()) {
                unit.stop();
                return;
            }
        }

        if (unit.getOrderTarget() != null && unit.getOrderTarget().getID() == enemyUnit.getEnemyID()) {
            badTargetID = null;
        }

        if (!unit.isStartingAttack() && unit.getGroundWeaponCooldown() == 0 && !unit.isAttackFrame()) {
            if (enemyUnit.getEnemyUnit().isVisible()) {
                unit.attack(enemyUnit.getEnemyUnit());
            }
            else {
                unit.attack(enemyUnit.getEnemyPosition());
            }
        }
    }

    private void kite() {
        if (enemyUnit.getEnemyPosition() == null) {
            return;
        }

        if (enemyUnit.getEnemyType().isBuilding() || enemyUnit.getEnemyType().isFlyer()) {
            return;
        }

        if (unitStatus == UnitStatus.SALLYOUT && enemyUnit.getEnemyType() == UnitType.Terran_Siege_Tank_Siege_Mode) {
            return;
        }

        if (unit.isStartingAttack() || unit.isAttackFrame()) {
            return;
        }

        if (unit.getGroundWeaponCooldown() == 0
                && (unit.getOrder() == Order.AttackUnit || unit.getOrder() == Order.AttackMove)
                && (unitStatus == UnitStatus.SALLYOUT || !meleeTooClose())) {
            return;
        }

        Position enemyPosition = enemyUnit.getEnemyPosition();
        double distanceToEnemy = unit.getPosition().getDistance(enemyPosition);

        if (distanceToEnemy >= weaponRange() * 0.9) {
            return;
        }

        if (unit.getOrder() == Order.Move && game.getFrameCount() % 8 != 0) {
            return;
        }

        Position kitePosition = findKitePosition(enemyPosition, 96);

        if (kitePosition != null) {
            unit.move(kitePosition);
            return;
        }

        if (rallyPoint != null) {
            unit.move(rallyPoint.toPosition());
        }
    }

    private boolean meleeTooClose() {
        for (Unit nearbyUnit : game.getUnitsInRadius(unit.getPosition(), 96)) {
            if (nearbyUnit.getPlayer() != game.enemy()) {
                continue;
            }

            if (nearbyUnit.getType().isBuilding() || nearbyUnit.isFlying()) {
                continue;
            }

            if (nearbyUnit.getType().groundWeapon() == WeaponType.None) {
                continue;
            }

            if (nearbyUnit.getType().groundWeapon().maxRange() <= 32) {
                return true;
            }
        }

        return false;
    }

    private Position findKitePosition(Position enemyPosition, int hop) {
        Position unitPosition = unit.getPosition();
        double awayAngle = Math.atan2(unitPosition.getY() - enemyPosition.getY(), unitPosition.getX() - enemyPosition.getX());

        for (double firstOffset : KITE_ANGLE_OFFSETS) {
            double firstAngle = awayAngle + firstOffset;
            int spotOneX = (int) (unitPosition.getX() + Math.cos(firstAngle) * hop);
            int spotOneY = (int) (unitPosition.getY() + Math.sin(firstAngle) * hop);
            Position spotOne = new Position(spotOneX, spotOneY);

            if (!walkableRay(unitPosition, spotOne)) {
                continue;
            }

            double spotOneAwayAngle = Math.atan2(spotOneY - enemyPosition.getY(), spotOneX - enemyPosition.getX());

            for (double secondOffset : KITE_ANGLE_OFFSETS) {
                double secondAngle = spotOneAwayAngle + secondOffset;
                int spotTwoX = (int) (spotOneX + Math.cos(secondAngle) * hop);
                int spotTwoY = (int) (spotOneY + Math.sin(secondAngle) * hop);

                if (walkableRay(spotOne, new Position(spotTwoX, spotTwoY))) {
                    return spotOne;
                }
            }
        }

        return null;
    }

    private boolean walkableRay(Position from, Position to) {
        int maxX = game.mapWidth() * 32 - 1;
        int maxY = game.mapHeight() * 32 - 1;

        if (to.getX() < 0 || to.getY() < 0 || to.getX() > maxX || to.getY() > maxY) {
            return false;
        }

        int steps = Math.max(1, (int) Math.ceil(from.getDistance(to) / 8));

        for (int s = 1; s <= steps; s++) {
            int sx = from.getX() + (to.getX() - from.getX()) * s / steps;
            int sy = from.getY() + (to.getY() - from.getY()) * s / steps;

            if (!game.isWalkable(new Position(sx, sy).toWalkPosition())) {
                return false;
            }
        }

        for (Unit nearbyUnit : game.getUnitsInRadius(to, 16)) {
            if (nearbyUnit.getType().isBuilding() && !nearbyUnit.isLifted()) {
                return false;
            }
        }

        return true;
    }

    private int weaponRange() {
        WeaponType weaponType = unit.getType().groundWeapon();

        if (this.game.self().getUpgradeLevel(UpgradeType.U_238_Shells) > 0) {
            return weaponType.maxRange() + UPGRADE_RANGE;
        }
        else {
            return weaponType.maxRange();
        }
    }
}
