package information;

import bwapi.Game;
import bwapi.Player;
import bwapi.Position;
import bwapi.Unit;
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
    private int scoutRadius = 200;
    private int positionCount = 8;
    private int currentPositionIndex = 0;
    private boolean completedScout = false;

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
        if(scout == null) {
            return;
        }

        if (scout.getUnit().isAttacking()) {
            scout.getUnit().stop();
        }


        Position enemyBasePos = enemyInformation.getStartingEnemyBase().getPosition();
        double angle = (Math.PI * 2 * currentPositionIndex) / positionCount;

        int x = (int) (enemyBasePos.getX() + scoutRadius * Math.cos(angle));
        int y = (int) (enemyBasePos.getY() + scoutRadius * Math.sin(angle));

        Position targetPosition = new Position(x, y);

        if (scout.getUnit().getDistance(targetPosition) < 90) {
            currentPositionIndex = (currentPositionIndex + 1) % positionCount;
            angle = (Math.PI * 2 * currentPositionIndex) / positionCount;
            x = (int) (enemyBasePos.getX() + scoutRadius * Math.cos(angle));
            y = (int) (enemyBasePos.getY() + scoutRadius * Math.sin(angle));
            targetPosition = new Position(x, y);
        }

        scout.getUnit().rightClick(targetPosition);
    }

    public void onFrame() {
        if(player.supplyUsed() / 2 >= 10 && enemyInformation.getStartingEnemyBase() == null) {
            sendScout();
        }

        if(enemyInformation.getStartingEnemyBase() != null) {
            scoutEnemyPerimeter();
            completedScout = true;
        }

        if(scout != null) {
            painters.paintScoutPath(scout.getUnit());
        }
    }

    public void onEnemyDestroy(Unit unit) {
        if(scout == null) {
            return;
        }

        if(unit.getID() == scout.getUnit().getID()) {
            scout = null;
        }
    }

    public boolean isCompletedScout() {
        return completedScout;
    }
}
