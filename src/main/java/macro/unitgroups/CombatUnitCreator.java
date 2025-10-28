package macro.unitgroups;

import bwapi.Game;
import bwapi.Unit;
import information.enemy.EnemyInformation;
import macro.unitgroups.units.*;

public class CombatUnitCreator {
    private Game game;
    private EnemyInformation enemyInformation;

    public CombatUnitCreator(Game game, EnemyInformation enemyInformation) {
        this.game = game;
        this.enemyInformation = enemyInformation;
    }

    public CombatUnits createCombatUnit(Unit unit) {
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
                return new Vulture(game, enemyInformation, unit);
            case Terran_Siege_Tank_Tank_Mode:
                return new SiegeTank(game, enemyInformation, unit);
            case Terran_Goliath:
                return new Goliath(game, unit);
            case Terran_Battlecruiser:
                return new Battlecruiser(game, unit);
            case Terran_Wraith:
                return new Wraith(game, enemyInformation, unit);
            case Terran_Valkyrie:
                return new Valkyrie(game, unit);
            case Terran_Science_Vessel:
                return new ScienceVessel(game, unit);
            case Terran_Comsat_Station:
                return new Comsat(game, unit);
            case Spell_Scanner_Sweep:
                return new Scan(game, unit);
            case Terran_Vulture_Spider_Mine:
                return new SpiderMines(game, unit);
            case Terran_SCV:
                return new Workers(game, unit);
            default:
                return new CombatUnits(game, unit);
        }
    }
}
