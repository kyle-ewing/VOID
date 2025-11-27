package unitgroups.units;

import bwapi.Game;
import bwapi.Unit;

public class SpiderMines extends CombatUnits {
    //Override everything as mines don't do anything

    public SpiderMines(Game game, Unit unit) {
        super(game, unit);
        unitStatus = UnitStatus.MINE;
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
