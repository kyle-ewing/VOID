package macro.unitgroups.units;

import bwapi.Game;
import bwapi.Unit;
import macro.unitgroups.CombatUnits;
import macro.unitgroups.UnitStatus;

public class SpiderMines extends CombatUnits {
    //Override everything as mines don't do anything

    public SpiderMines(Game game, Unit unit) {
        super(game, unit);
        unitStatus = UnitStatus.MINE;
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
