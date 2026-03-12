import debug.Painters;
import information.MapInfo;
import information.GameState;
import information.enemy.EnemyInformation;
import information.Scouting;
import macro.ProductionManager;
import unitgroups.WorkerManager;
import bwapi.*;
import bwem.BWEM;
import unitgroups.UnitManager;

public class Bot extends DefaultBWListener {
    private BWClient bwClient;
    private Game game;
    private BWEM bwem;
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

        bwem = new BWEM(game);
        bwem.initialize();

        mapInfo = new MapInfo(bwem, game);
        gameState = new GameState(game, bwem, mapInfo);
        enemyInformation = new EnemyInformation(mapInfo, game, gameState);
        workerManager = new WorkerManager(mapInfo, player, game, gameState);
        productionManager = new ProductionManager(game, player, mapInfo, gameState);
        scouting = new Scouting(game, mapInfo, gameState);
        unitManager = new UnitManager(enemyInformation, gameState, mapInfo, game, scouting);

        painters = new Painters(game, gameState, gameState.getConfig(), scouting);
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
        if(unit.getPlayer() != game.self()) {
            return;
        }
        mapInfo.onUnitCreate(unit);
        productionManager.onUnitCreate(unit);
        workerManager.onUnitCreate(unit);
    }

    @Override
    public void onUnitComplete(Unit unit) {
        if(unit.getPlayer() != game.self()) {
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

        if(unit.getPlayer() == game.self()) {
            if(unit.getType() != UnitType.Terran_SCV) {
                unitManager.onUnitDestroy(unit);
            }

            productionManager.onUnitDestroy(unit);
        }

        if(unit.getType() == UnitType.Terran_Command_Center || unit.getType() == UnitType.Resource_Mineral_Field) {
            mapInfo.onUnitDestroy(unit);
        }

        scouting.onEnemyDestroy(unit);
    }

    @Override
    public void onUnitDiscover(Unit unit) {
        if(unit.getPlayer() == game.self()) {
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
        if(unit.getPlayer() != game.self()) {
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