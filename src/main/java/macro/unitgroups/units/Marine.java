package macro.unitgroups.units;

import bwapi.*;
import macro.unitgroups.CombatUnits;
import macro.unitgroups.UnitStatus;

public class Marine extends CombatUnits {
    private static final int UPGRADE_RANGE = 32;
    private boolean hasTankSupport = false;

    public Marine(Game game, Unit unit) {
        super(game, unit);
    }

    @Override
    public void attack() {
        if(enemyUnit == null) {
            return;
        }

        if(super.isInBunker()) {
            inBunker = false;
        }

        kite();

        if(minimnumThreshold()) {
            return;
        }

        if(!unit.isStimmed() && unit.isAttacking()) {
            unit.useTech(TechType.Stim_Packs);
        }

        if(!unit.isStartingAttack() && unit.getGroundWeaponCooldown() == 0 && !unit.isAttackFrame()) {
            unit.attack(enemyUnit.getEnemyPosition());
        }
    }

    @Override
    public void defend() {
        if(enemyUnit == null) {
            setUnitStatus(UnitStatus.RALLY);
            return;
        }

        kite();

        if(minimnumThreshold()) {
            return;
        }

        if(!unit.isStimmed() && unit.isAttacking()) {
            unit.useTech(TechType.Stim_Packs);
        }

        if(!unit.isStartingAttack() && unit.getGroundWeaponCooldown() == 0 && !unit.isAttackFrame()) {
            unit.attack(enemyUnit.getEnemyPosition());
        }

    }

    @Override
    public void retreat() {
        if(enemyUnit == null || enemyUnit.getEnemyPosition() == null) {
            return;
        }

        if(!unit.isStimmed() && unit.isAttacking()) {
            unit.useTech(TechType.Stim_Packs);
        }

        if(!unit.isStartingAttack() && unit.getGroundWeaponCooldown() == 0 && !unit.isAttackFrame()) {
            unit.attack(enemyUnit.getEnemyPosition());
        }

        if(!inRangeOfThreat) {
            setUnitStatus(UnitStatus.ATTACK);
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

    public boolean hasTankSupport() {
        return hasTankSupport;
    }

    public void setHasTankSupport(boolean hasTankSupport) {
        this.hasTankSupport = hasTankSupport;
    }
}
