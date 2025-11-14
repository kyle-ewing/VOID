package information;

import bwapi.Game;
import bwapi.Player;
import bwapi.UnitType;
import bwem.BWEM;
import information.enemy.EnemyUnits;
import information.enemy.enemyopeners.EnemyStrategy;
import information.enemy.enemytechunits.EnemyTechUnits;
import util.Time;

import java.util.HashSet;

public class GameState {
    private Game game;
    private BWEM bwem;
    private Player player;
    private EnemyStrategy enemyOpener = null;

    private EnemyUnits startingEnemyBase = null;
    private boolean enemyInBase = false;
    private boolean enemyBuildingDiscovered = false;

    private HashSet<EnemyUnits> knownEnemyUnits = new HashSet<>();
    private HashSet<EnemyTechUnits> knownEnemyTechUnits = new HashSet<>();
    private HashSet<UnitType> techUnitResponse = new HashSet<>();

    public GameState(Game game, BWEM bwem) {
        this.game = game;
        this.bwem = bwem;

        player = game.self();
    }

    public void onFrame() {
        drawToScreen();

    }

    private void drawToScreen() {
        game.drawTextScreen(5,15, "Time: " + new Time(game.getFrameCount()) + " Frame: " + game.getFrameCount());

        if (enemyOpener != null) {
            game.drawTextScreen(5, 60, "Enemy Opener: " + enemyOpener.getStrategyName());
        } else {
            game.drawTextScreen(5, 60, "Enemy Opener: Unknown");
        }
    }

    public HashSet<EnemyUnits> getKnownEnemyUnits() {
        return knownEnemyUnits;
    }

    public HashSet<UnitType> getTechUnitResponse() {
        return techUnitResponse;
    }

    public HashSet<EnemyTechUnits> getKnownEnemyTechUnits() {
        return knownEnemyTechUnits;
    }

    public EnemyUnits getStartingEnemyBase() {
        return startingEnemyBase;
    }

    public boolean isEnemyBuildingDiscovered() {
        return enemyBuildingDiscovered;
    }

    public void setEnemyBuildingDiscovered(boolean enemyBuildingDiscovered) {
        this.enemyBuildingDiscovered = enemyBuildingDiscovered;
    }

    public void setStartingEnemyBase(EnemyUnits startingEnemyBase) {
        this.startingEnemyBase = startingEnemyBase;
    }

    public void setEnemyOpener(EnemyStrategy enemyOpener) {
        this.enemyOpener = enemyOpener;
    }

    public EnemyStrategy getEnemyOpener() {
        return enemyOpener;
    }

    public boolean isEnemyInBase() {
        return enemyInBase;
    }

    public void setEnemyInBase(boolean enemyInBase) {
        this.enemyInBase = enemyInBase;
    }
}
