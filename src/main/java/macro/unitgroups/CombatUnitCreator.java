package macro.unitgroups;

import bwapi.Game;
import bwapi.Unit;
import macro.unitgroups.units.*;

public class CombatUnitCreator {
    private Game game;

    public CombatUnitCreator(Game game) {
        this.game = game;
    }

    public CombatUnits createCombatUnit(Unit unit, UnitStatus unitStatus) {
        switch (unit.getType()) {
            case Terran_Marine:
                return new Marine(game, unit);
            case Terran_Medic:
                return new Medic(game, unit);
            case Terran_Firebat:
                return new Firebat(game, unit);
            case Terran_Ghost:
                return new Ghost(game, unit);
            case Terran_Vulture:
                return new Vulture(game, unit);
            case Terran_Siege_Tank_Tank_Mode:
                return new SiegeTank(game, unit);
            case Terran_Goliath:
                return new Goliath(game, unit);
            case Terran_Battlecruiser:
                return new Battlecruiser(game, unit);
            case Terran_Wraith:
                return new Wraith(game, unit);
            case Terran_Valkyrie:
                return new Valkyrie(game, unit);
            case Terran_Science_Vessel:
                return new ScienceVessel(game, unit);
            case Terran_Comsat_Station:
                return new Comsat(game, unit, unitStatus);
            case Spell_Scanner_Sweep:
                return new Scan(game, unit, unitStatus);
            default:
                return new CombatUnits(game, unit);
        }
    }
}
