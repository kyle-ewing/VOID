package information;

import bwapi.Game;
import bwapi.Player;
import bwapi.Position;
import bwem.BWEM;
import bwem.Base;
import debug.Painters;
import macro.ResourceManager;
import macro.unitgroups.WorkerStatus;
import macro.unitgroups.Workers;

public class Scouting {
    private BWEM bwem;
    private Game game;
    private ResourceManager resourceManager;
    private BaseInfo baseInfo;
    private Painters painters;
    private Player player;
    private EnemyInformation enemyInformation;

    private Workers scout;
    private int updateFrame = 24;
    private int scoutRadius = 320;
    private int positionCount = 8;
    private int currentPositionIndex = 0;

    public Scouting(BWEM bwem, Game game, Player player, ResourceManager resourceManager, BaseInfo baseInfo, EnemyInformation enemyInformation) {
        this.bwem = bwem;
        this.game = game;
        this.player = player;
        this.resourceManager = resourceManager;
        this.baseInfo = baseInfo;
        this.enemyInformation = enemyInformation;

        this.painters = new Painters(game, bwem, resourceManager);
    }

    public void sendScout() {
        if(scout == null) {
            selectScout();
        }

        for(Base startingBase : baseInfo.getStartingBases()) {
            if(startingBase == baseInfo.getStartingBase()) {
                continue;
            }

            if(!baseInfo.isExplored(startingBase)) {
                scout.getUnit().move(startingBase.getCenter());
            }
        }
    }


    private void selectScout() {
        for(Workers scv: resourceManager.getWorkers()) {
            if(scv.getWorkerStatus() == WorkerStatus.MINERALS) {
                scout = scv;
                scv.setWorkerStatus(WorkerStatus.SCOUTING);
                break;
            }
        }
    }

    public void scoutEnemyPerimeter() {
        if(game.getFrameCount() % updateFrame != 0) {
            return;
        }

        Position enemyBasePos = enemyInformation.getStartingEnemyBase().getPosition();
        double angle = (Math.PI * 2 * currentPositionIndex) / positionCount;

        int x = (int) (enemyBasePos.getX() + scoutRadius * Math.cos(angle));
        int y = (int) (enemyBasePos.getY() + scoutRadius * Math.sin(angle));

        scout.getUnit().rightClick(new Position(x, y));
        currentPositionIndex = (currentPositionIndex + 1) % positionCount;
    }

    public void onFrame() {
        if(player.supplyUsed() / 2 >= 10 && enemyInformation.getStartingEnemyBase() == null) {
            sendScout();
        }

        if(enemyInformation.getStartingEnemyBase() != null) {
            scoutEnemyPerimeter();
        }

        if(scout != null) {
            painters.paintScoutPath(scout.getUnit());
        }
    }
}
