import bwapi.BWClient;
import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Player;
import bwapi.Unit;
import bwapi.UnitType;
import debug.Painters;
import information.GameState;
import information.MapInfo;
import information.Scouting;
import information.enemy.EnemyInformation;
import macro.ProductionManager;
import map.bwemwrappers.GameMap;
import unitgroups.UnitManager;
import unitgroups.WorkerManager;

public class Bot extends DefaultBWListener {
    private BWClient bwClient;
    private Game game;
    private GameMap gameMap;
    private Player player;
    private GameState gameState;
    private MapInfo mapInfo;
    private EnemyInformation enemyInformation;
    private WorkerManager workerManager;
    private ProductionManager productionManager;
    private UnitManager unitManager;
    private Scouting scouting;

    //Debug
    private Painters painters;

    @Override
    public void onStart() {
        game = bwClient.getGame();
        player = game.self();

        gameMap = new GameMap(game);
        mapInfo = new MapInfo(game, gameMap);
        gameState = new GameState(game, mapInfo);
        enemyInformation = new EnemyInformation(mapInfo, game, gameState);
        workerManager = new WorkerManager(mapInfo, player, game, gameState, enemyInformation);
        productionManager = new ProductionManager(game, player, mapInfo, gameState);
        scouting = new Scouting(game, mapInfo, gameState);
        unitManager = new UnitManager(enemyInformation, gameState, mapInfo, game, scouting);

        painters = new Painters(game, gameState, gameState.getConfig(), scouting, unitManager.getRallyPoint());
    }

    @Override
    public void onFrame() {
        gameState.onFrame();
        enemyInformation.onFrame();
        workerManager.onFrame();
        productionManager.onFrame();
        unitManager.onFrame();
        scouting.onFrame();
        painters.onFrame();
    }




    @Override
    public void onUnitCreate(Unit unit) {
        if (unit.getPlayer() != game.self()) {
            return;
        }
        mapInfo.onUnitCreate(unit);
        productionManager.onUnitCreate(unit);
        workerManager.onUnitCreate(unit);
    }

    @Override
    public void onUnitComplete(Unit unit) {
        if (unit.getPlayer() != game.self()) {
            return;
        }

        productionManager.onUnitComplete(unit);
        workerManager.onUnitComplete(unit);
        unitManager.onUnitComplete(unit);
    }

    @Override
    public void onUnitDestroy(Unit unit) {
        workerManager.onUnitDestroy(unit);
        enemyInformation.onUnitDestroy(unit);

        if (unit.getPlayer() == game.self()) {
            if (unit.getType() != UnitType.Terran_SCV) {
                unitManager.onUnitDestroy(unit);
            }

            productionManager.onUnitDestroy(unit);
        }

        if (unit.getType() == UnitType.Terran_Command_Center || unit.getType().isMineralField()) {
            mapInfo.onUnitDestroy(unit);
            gameMap.onUnitDestroyed(unit);
        }

        scouting.onEnemyDestroy(unit);
    }

    @Override
    public void onUnitDiscover(Unit unit) {
        if (unit.getPlayer() == game.self()) {
            return;
        }

        enemyInformation.onUnitDiscover(unit);
    }

    @Override
    public void onUnitShow(Unit unit) {
        enemyInformation.onUnitShow(unit);
    }

    @Override
    public void onUnitMorph(Unit unit) {
        if (unit.getPlayer() != game.self()) {
            return;
        }
        productionManager.onUnitMorph(unit);
    }

    @Override
    public void onUnitRenegade(Unit unit) {
        enemyInformation.onUnitRenegade(unit);
    }

    @Override
    public void onEnd(boolean isWinner) {
        painters.onEnd();
    }

    public static void main(String[] args) {
        Bot bot = new Bot();
        bot.bwClient = new BWClient(bot);
        bot.bwClient.startGame();
    }
}