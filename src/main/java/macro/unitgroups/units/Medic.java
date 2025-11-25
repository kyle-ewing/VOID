package macro.unitgroups.units;

import bwapi.Game;
import bwapi.Position;
import bwapi.Unit;
import macro.unitgroups.CombatUnits;
import macro.unitgroups.UnitStatus;

public class Medic extends CombatUnits {
    public Medic(Game game, Unit unit) {
        super(game, unit);
    }

    @Override
    public void attack() {
        if(friendlyUnit == null) {
            super.setTargetRange(200);
            super.setUnitStatus(UnitStatus.RETREAT);
            return;
        }

        if(super.getTargetRange() > 200) {
            super.setTargetRange(200);
        }

        if(inBase) {
            unstick();
            return;
        }

        unit.attack(friendlyUnit.getUnit().getPosition());
    }

    @Override
    public void rally() {
        if(rallyPoint == null) {
            return;
        }

        if(enemyInBase) {
            super.setTargetRange(800);
            super.setUnitStatus(UnitStatus.DEFEND);
        }

        unit.attack(rallyPoint.toPosition());
    }

    @Override
    public void defend() {
        if(friendlyUnit == null || !enemyInBase) {
            super.setTargetRange(200);
            super.setUnitStatus(UnitStatus.RALLY);
            return;
        }

        unit.attack(friendlyUnit.getUnit().getPosition());
    }

    @Override
    public void retreat() {
        if(rallyPoint == null) {
            return;
        }

        if(friendlyUnit != null) {
            super.setUnitStatus(UnitStatus.ATTACK);
            return;
        }

        unit.attack(rallyPoint.toPosition());
    }

    //Move medics away from target to prevent blocking units from moving out
    private void unstick() {
        if (friendlyUnit == null) {
            super.setTargetRange(200);
            super.setUnitStatus(UnitStatus.RETREAT);
            return;
        }

        Unit friend = friendlyUnit.getUnit();
        Position myPos = unit.getPosition();
        Position friendPos = friend.getPosition();

        double dist = myPos.getDistance(friendPos);

        if(dist > 40) {
            unit.attack(friendPos);
            return;
        }

        if(dist < 40 && dist > 0) {
            int dx = myPos.getX() - friendPos.getX();
            int dy = myPos.getY() - friendPos.getY();

            double len = Math.sqrt(dx * dx + dy * dy);
            if(len > 0) {
                double scale = 32.0 / len;
                int newX = myPos.getX() + (int) (dx * scale);
                int newY = myPos.getY() + (int) (dy * scale);
                unit.move(new Position(newX, newY));
            }
        }
    }
}
