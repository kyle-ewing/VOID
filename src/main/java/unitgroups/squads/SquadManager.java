package unitgroups.squads;

import bwapi.Game;
import information.GameState;
import unitgroups.units.CombatUnits;

public class SquadManager {
    private Game game;
    private GameState gamestate;
    private Squad mainSquad;

    public SquadManager(Game game, GameState gamestate) {
        this.game = game;
        this.gamestate = gamestate;
        this.mainSquad = new Squad(game);
    }

    public void updateRegroupPositions() {
        for (CombatUnits combatUnit : mainSquad.getSquadUnits()) {
            combatUnit.setRegroupPosition(mainSquad.getRegroupPosition());
        }
    }

    public void addUnitToSquad(CombatUnits unit) {
        mainSquad.addToSquad(unit);
    }

    public void removeUnitFromSquad(CombatUnits unit) {
        mainSquad.removeFromSquad(unit);
    }

    public void onFrame() {
        mainSquad.onFrame();
        updateRegroupPositions();
    }

    public void onUnitComplete(CombatUnits unit) {
        mainSquad.addToSquad(unit);
    }

    public void onUnitDestroy(CombatUnits unit) {
        mainSquad.removeFromSquad(unit);
    }
}
