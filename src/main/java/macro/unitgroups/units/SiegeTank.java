package macro.unitgroups.units;

import bwapi.*;
import macro.unitgroups.CombatUnits;
import macro.unitgroups.UnitStatus;

public class SiegeTank extends CombatUnits {
    private static final int SIEGE_RANGE = 384;

    public SiegeTank(Game game, Unit unit) {
        super(game, unit);
    }

    @Override
    public void attack() {
        if(enemyUnit == null) {
            return;
        }

        siegeLogic();
        unit.attack(enemyUnit.getEnemyPosition());
    }

    @Override
    public void retreat() {
        if(enemyUnit.getEnemyUnit().getDistance(unit) > 128) {
            super.setUnitStatus(UnitStatus.ATTACK);
            return;
        }

        if(super.getRallyPoint().toPosition().getApproxDistance(unit.getPosition()) < 128) {
            super.setUnitStatus(UnitStatus.RALLY);
            return;
        }

        if(kiteThreshold()) {
            if(unit.getGroundWeaponCooldown() == 0) {
                unit.attack(super.rallyPoint.toPosition());
                return;
            }

            unit.move(super.rallyPoint.toPosition());
            return;
        }

        unit.attack(super.rallyPoint.toPosition());
    }

    @Override
    public void defend() {
        if(enemyUnit == null) {
            setUnitStatus(UnitStatus.RALLY);
            return;
        }

        if(kiteThreshold()) {
            if(unit.getGroundWeaponCooldown() == 0) {
                unit.attack(super.rallyPoint.toPosition());
                return;
            }

            int maxRange = weaponRange();
            Position kitePos = getKitePos(maxRange);
            unit.move(kitePos);
            return;
        }

        siegeLogic();
    }

    @Override
    public void rally() {
        if(rallyPoint == null) {
            return;
        }

        if(isSieged()) {
            super.setUnitType(UnitType.Terran_Siege_Tank_Tank_Mode);
            unit.unsiege();
        }

        if(super.getRallyPoint().getApproxDistance(unit.getTilePosition()) < 128 && enemyUnit != null ) {
            super.setUnitStatus(UnitStatus.DEFEND);
            return;
        }

        unit.attack(rallyPoint.toPosition());

    }

    private void siegeLogic() {

        switch(super.getUnitStatus()) {
            case ATTACK:
                if(!isSieged() && enemyUnit.getEnemyUnit().getDistance(unit) < 128) {
                    super.setUnitStatus(UnitStatus.RETREAT);
                }

                if(isSieged() && enemyUnit.getEnemyUnit().getDistance(unit) < 128) {
                    super.setUnitStatus(UnitStatus.RETREAT);
                    super.setUnitType(UnitType.Terran_Siege_Tank_Tank_Mode);
                    unit.unsiege();
                }
                break;
            case DEFEND:
                if(isSieged() && enemyUnit.getEnemyUnit().getDistance(unit) < 128) {
                    super.setUnitType(UnitType.Terran_Siege_Tank_Tank_Mode);
                    unit.unsiege();
                }
                break;
            case RETREAT:
                if(isSieged()) {
                    super.setUnitType(UnitType.Terran_Siege_Tank_Tank_Mode);
                    unit.unsiege();
                }
                break;
        }

        if(enemyUnit.getEnemyUnit().getDistance(unit) < SIEGE_RANGE && !isSieged() && enemyUnit.getEnemyUnit().getDistance(unit) > 128 && canSiege()) {
            super.setUnitType(UnitType.Terran_Siege_Tank_Siege_Mode);
            unit.siege();
        }

        if(enemyUnit.getEnemyUnit().getDistance(unit) > SIEGE_RANGE && isSieged()) {
            super.setUnitType(UnitType.Terran_Siege_Tank_Tank_Mode);
            unit.unsiege();
        }
    }

    private boolean kiteThreshold() {
        int maxRange = weaponRange();
        double kiteThreshold = maxRange * 0.2;
        Position enemyPosition = enemyUnit.getEnemyPosition();
        Position unitPosition = unit.getPosition();
        double distanceToEnemy = unitPosition.getDistance(enemyPosition);

        return distanceToEnemy < kiteThreshold;
    }

    private Position getKitePos(int maxRange) {
        Position enemyPosition = enemyUnit.getEnemyPosition();
        Position unitPosition = unit.getPosition();

        double dx = unitPosition.getX() - enemyPosition.getX();
        double dy = unitPosition.getY() - enemyPosition.getY();
        double length = Math.sqrt(dx * dx + dy * dy);

        double scale = maxRange / length;
        int targetX = (int) (enemyPosition.getX() + dx * scale);
        int targetY = (int) (enemyPosition.getY() + dy * scale);

        Position kitePos = new Position(targetX, targetY);
        return kitePos;
    }


    private boolean canSiege() {
        if(this.game.self().hasResearched(TechType.Tank_Siege_Mode)) {
            return true;
        }
        return false;
    }

    private boolean isSieged() {
        if(unit.getType() == UnitType.Terran_Siege_Tank_Siege_Mode) {
            return true;
        }
        return false;
    }

    private int weaponRange() {
        WeaponType weaponType = unit.getType().groundWeapon();
        return weaponType.maxRange();
    }
}
