package unitgroups.units;

import bwapi.Game;
import bwapi.Unit;

public class Scan extends CombatUnits {
    public Scan(Game game, Unit unit) {
        super(game, unit);
        unitStatus = UnitStatus.SCAN;
        hasStaticStatus = true;
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
