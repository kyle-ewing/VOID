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
            super.setUnitStatus(UnitStatus.RALLY);
            return;
        }

        unit.attack(friendlyUnit.getPosition());
    }

    @Override
    public void rally() {
        if(rallyPoint == null) {
            return;
        }

        unit.attack(rallyPoint.toPosition());
    }


}
