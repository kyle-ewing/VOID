import debug.Painters;
import information.BaseInfo;
import information.EnemyInformation;
import information.Scouting;
import macro.ProductionManager;
import macro.ResourceManager;
import bwapi.*;
import bwem.BWEM;
import macro.UnitManager;

public class Bot extends DefaultBWListener {
    private BWClient bwClient;
    private Game game;
    private BWEM bwem;
    private Player player;
    private BaseInfo baseInfo;
    private EnemyInformation enemyInformation;
    private ResourceManager resourceManager;
    private ProductionManager productionManager;
    private UnitManager unitManager;
    private Scouting scouting;

    //Debug painters
    private Painters painters;

    @Override
    public void onStart() {
        game = bwClient.getGame();
        player = game.self();

        bwem = new BWEM(game);
        bwem.initialize();
        game.setLocalSpeed(5);
        game.enableFlag(Flag.UserInput);

        baseInfo = new BaseInfo(bwem, game);
        enemyInformation = new EnemyInformation(baseInfo, game);
        resourceManager = new ResourceManager(baseInfo, player);
        productionManager = new ProductionManager(game, player, resourceManager, baseInfo);
        unitManager = new UnitManager(enemyInformation, baseInfo, game);
        scouting = new Scouting(bwem, player, resourceManager, baseInfo, enemyInformation);

        //Debug painters
        painters = new Painters(game, bwem, resourceManager);


    }

    @Override
    public void onFrame() {
        game.drawTextScreen(5,15, "Frame: " + game.getFrameCount());
        enemyInformation.onFrame();
        resourceManager.onFrame();
        productionManager.onFrame();
        unitManager.onFrame();
        scouting.onFrame();
        painters.onFrame();
        baseInfo.onFrame();
    }




    @Override
    public void onUnitCreate(Unit unit) {
        if(unit.getPlayer() != game.self()) {
            return;
        }
        productionManager.onUnitCreate(unit);
    }

    @Override
    public void onUnitComplete(Unit unit) {
        if(unit.getPlayer() != game.self()) {
            return;
        }

        if(unit.getType() == UnitType.Terran_SCV) {
            resourceManager.onUnitComplete(unit);
        }
        productionManager.onUnitComplete(unit);

        if(unit.getType() != UnitType.Terran_SCV && !unit.getType().isBuilding()) {
            unitManager.onUnitComplete(unit);
        }


    }

    @Override
    public void onUnitDestroy(Unit unit) {


        if(unit.getType() == UnitType.Terran_SCV) {
            resourceManager.onUnitDestroy(unit);
        }

        enemyInformation.onUnitDestroy(unit);

        if(unit.getPlayer() == game.self()) {
            if(unit.getType() != UnitType.Terran_SCV && !unit.getType().isBuilding()) {
                unitManager.onUnitDestroy(unit);
            }

            productionManager.onUnitDestroy(unit);
        }
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

    public static void main(String[] args) {
        Bot bot = new Bot();
        bot.bwClient = new BWClient(bot);
        bot.bwClient.startGame();
    }
}