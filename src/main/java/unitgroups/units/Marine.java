package unitgroups.units;

import bwapi.*;

public class Marine extends CombatUnits {
    private static final int UPGRADE_RANGE = 32;
    private Integer badTargetID = null;

    public Marine(Game game, Unit unit) {
        super(game, unit);
    }

    @Override
    public void rally() {
        if(rallyPoint == null) {
            return;
        }

        if(priorityEnemyUnit != null) {
            setEnemyUnit(priorityEnemyUnit);
            setUnitStatus(UnitStatus.DEFEND);
        }

        if(enemyUnit != null && enemyInBase) {
            setUnitStatus(UnitStatus.DEFEND);
        }

        if(inBase) {
            unit.attack(rallyPoint.toPosition());
        }
        else {
            setUnitStatus(UnitStatus.RETREAT);
        }

    }

    @Override
    public void attack() {
        if(enemyUnit == null) {
            return;
        }

        if(super.isInBunker()) {
            inBunker = false;
        }

        attackUnit();
    }

    @Override
    public void defend() {
        if(enemyUnit == null) {
            setUnitStatus(UnitStatus.RALLY);
            return;
        }

        if(!inBase) {
            setUnitStatus(UnitStatus.RETREAT);
            return;
        }

        if(!enemyInBase) {
            setUnitStatus(UnitStatus.RALLY);
            return;
        }

        attackUnit();

    }

    @Override
    public void retreat() {
        if(enemyUnit == null || enemyUnit.getEnemyPosition() == null) {
            return;
        }

        enemyUnit = null;
        unit.move(rallyPoint.toPosition());

        if(inBase || hasTankSupport) {
            setUnitStatus(UnitStatus.RALLY);
        }
    }

    private void attackUnit() {
        kite();

        if(minimnumThreshold() && enemyUnit.getEnemyType() != UnitType.Terran_Siege_Tank_Siege_Mode) {
            return;
        }

        if(!unit.isStimmed() && unit.isAttacking()) {
            unit.useTech(TechType.Stim_Packs);
        }

        if(unit.getOrderTarget() != null && unit.getOrderTarget().getID() != enemyUnit.getEnemyID() && unit.isAttacking()) {
            if(badTargetID == null || badTargetID != unit.getTarget().getID()) {
                unit.stop();
                return;
            }
        }

        if(unit.getOrderTarget() != null && unit.getOrderTarget().getID() == enemyUnit.getEnemyID()) {
            badTargetID = null;
        }

        if(!unit.isStartingAttack() && unit.getGroundWeaponCooldown() == 0 && !unit.isAttackFrame()) {
            if(enemyUnit.getEnemyUnit().isVisible()) {
                unit.attack(enemyUnit.getEnemyUnit());
            }
            else {
                unit.attack(enemyUnit.getEnemyPosition());
            }
        }
    }

    private void kite() {
        if(enemyUnit.getEnemyType().isBuilding()) {
            return;
        }

        int maxRange = weaponRange();
        double kiteThreshold = maxRange * 0.9;
        Position enemyPosition = enemyUnit.getEnemyPosition();
        Position unitPosition = unit.getPosition();
        double distanceToEnemy = unitPosition.getDistance(enemyPosition);

        if(enemyUnit.getEnemyType() == UnitType.Terran_Siege_Tank_Siege_Mode) {
            if(distanceToEnemy > 32) {
                unit.move(enemyPosition);
            }
            return;
        }

        if(distanceToEnemy < kiteThreshold) {
            double dx = unitPosition.getX() - enemyPosition.getX();
            double dy = unitPosition.getY() - enemyPosition.getY();
            double length = Math.sqrt(dx * dx + dy * dy);

            double scale = maxRange / length;
            int targetX = (int) (enemyPosition.getX() + dx * scale);
            int targetY = (int) (enemyPosition.getY() + dy * scale);

            Position kitePos = new Position(targetX, targetY);
            unit.move(kitePos);
        }
    }

    private boolean minimnumThreshold() {
        double halfRange = weaponRange() * 0.25;
        Position enemyPosition = enemyUnit.getEnemyPosition();
        Position unitPosition = unit.getPosition();
        double distanceToEnemy = unitPosition.getDistance(enemyPosition);

        return distanceToEnemy < halfRange;
    }

    private int weaponRange() {
        WeaponType weaponType = unit.getType().groundWeapon();

        if(this.game.self().getUpgradeLevel(UpgradeType.U_238_Shells) > 0) {
            return weaponType.maxRange() + UPGRADE_RANGE;
        }
        else {
            return weaponType.maxRange();
        }
    }
}
