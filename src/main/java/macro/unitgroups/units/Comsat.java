package macro.unitgroups.units;

import bwapi.Game;
import bwapi.Unit;
import macro.unitgroups.CombatUnits;
import macro.unitgroups.UnitStatus;

public class Comsat extends CombatUnits {
    public Comsat(Game game, Unit unit) {
        super(game, unit);
        unitStatus = UnitStatus.ADDON;
    }

    @Override
    public void attack() {

    }

    @Override
    public void retreat() {

    }

    @Override
    public void defend() {

    }

    @Override
    public void rally() {

    }
}
