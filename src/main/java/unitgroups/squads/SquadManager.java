package unitgroups.squads;

import bwapi.Game;
import information.GameState;
import unitgroups.units.CombatUnits;

public class SquadManager {
    private Game game;
    private GameState gamestate;
    private Squad mainSquad = new Squad();

    public SquadManager(Game game, GameState gamestate) {
        this.game = game;
        this.gamestate = gamestate;
    }

    public void addUnitToSquad(CombatUnits unit) {
        mainSquad.addToSquad(unit);
    }

    public void removeUnitFromSquad(CombatUnits unit) {
        mainSquad.removeFromSquad(unit);
    }

    public void onFrame() {
        mainSquad.onFrame();
    }

    public void onUnitComplete(CombatUnits unit) {
        mainSquad.addToSquad(unit);
    }

    public void onUnitDestroy(CombatUnits unit) {
        mainSquad.removeFromSquad(unit);
    }
}
