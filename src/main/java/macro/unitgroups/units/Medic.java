package macro.unitgroups.units;

import bwapi.Game;
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

        unit.attack(friendlyUnit.getPosition());
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

        unit.attack(friendlyUnit.getPosition());
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


}
