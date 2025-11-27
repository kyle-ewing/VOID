import debug.Painters;
import information.BaseInfo;
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
    private BaseInfo baseInfo;
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

//        game.setLocalSpeed(5);
//        game.enableFlag(Flag.UserInput);

        baseInfo = new BaseInfo(bwem, game);
        gameState = new GameState(game, bwem, baseInfo);
        enemyInformation = new EnemyInformation(baseInfo, game, gameState);
        workerManager = new WorkerManager(baseInfo, player, game, gameState);
        productionManager = new ProductionManager(game, player, baseInfo, gameState);
        scouting = new Scouting(game, baseInfo, gameState);
        unitManager = new UnitManager(enemyInformation, gameState, baseInfo, game, scouting);

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
        baseInfo.onUnitCreate(unit);
        productionManager.onUnitCreate(unit);
    }

    @Override
    public void onUnitComplete(Unit unit) {
        if(unit.getPlayer() != game.self()) {
            return;
        }

        if(unit.getType() == UnitType.Terran_Command_Center) {
            baseInfo.onUnitComplete(unit);
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

        if(unit.getType() == UnitType.Terran_Command_Center) {
            baseInfo.onUnitDestroy(unit);
        }

        scouting.onEnemyDestroy(unit);
    }

    @Override
    public void onUnitDiscover(Unit unit) {
        if(unit.getPlayer() == game.self() || unit.getPlayer() == game.neutral()) {
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

    public static void main(String[] args) {
        Bot bot = new Bot();
        bot.bwClient = new BWClient(bot);
        bot.bwClient.startGame();
    }
}