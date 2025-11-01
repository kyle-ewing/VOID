package macro.unitgroups.units;

import bwapi.Game;
import bwapi.Unit;
import macro.unitgroups.CombatUnits;
import macro.unitgroups.UnitStatus;

public class ScienceVessel extends CombatUnits {
    public ScienceVessel(Game game, Unit unit) {
        super(game, unit);
    }

    public void rally() {
        if(rallyPoint == null) {
            return;
        }

        unit.move(rallyPoint.toPosition());
    }

    public void attack() {
        if(friendlyUnit == null) {
            super.setTargetRange(200);
            super.setUnitStatus(UnitStatus.RETREAT);
            return;
        }

        unit.move(friendlyUnit.getUnit().getPosition());
    }

    public void defend() {
        unit.move(friendlyUnit.getUnit().getPosition());
    }

    public void retreat() {
        if(rallyPoint == null) {
            return;
        }

        if(friendlyUnit != null) {
            super.setUnitStatus(UnitStatus.ATTACK);
            return;
        }

        unit.move(rallyPoint.toPosition());
    }
}
