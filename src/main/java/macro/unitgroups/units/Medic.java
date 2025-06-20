package macro.unitgroups.units;

import bwapi.Game;
import bwapi.Unit;
import macro.unitgroups.CombatUnits;

import java.util.HashSet;

public class Medic extends CombatUnits {
    public Medic(Game game, Unit unit) {
        super(game, unit);
    }

    @Override
    public void attack() {
        if(friendlyUnit == null) {
            super.rally();
        }

        unit.attack(friendlyUnit);
    }

    @Override
    public void rally() {
        if(rallyPoint == null) {
            return;
        }

        unit.move(rallyPoint.toPosition());
    }


}
