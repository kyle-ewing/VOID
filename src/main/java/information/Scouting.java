package information;

import bwapi.Game;
import bwapi.Player;
import bwapi.Position;
import bwapi.Unit;
import bwem.BWEM;
import bwem.Base;
import debug.Painters;
import information.enemy.EnemyInformation;
import macro.ResourceManager;
import macro.unitgroups.WorkerStatus;
import macro.unitgroups.Workers;

public class Scouting {
    private BWEM bwem;
    private Game game;
    private GameState gameState;
    private ResourceManager resourceManager;
    private BaseInfo baseInfo;
    private Painters painters;
    private Player player;
    private EnemyInformation enemyInformation;

    private Workers scout;
    private int scoutRadius = 200;
    private int positionCount = 8;
    private int currentPositionIndex = 0;
    private int scoutingAttempts = 0;
    private boolean completedScout = false;
    private boolean attemptsMaxed = false;

    public Scouting(BWEM bwem, Game game, Player player, ResourceManager resourceManager, BaseInfo baseInfo, GameState gameState) {
        this.bwem = bwem;
        this.game = game;
        this.player = player;
        this.resourceManager = resourceManager;
        this.baseInfo = baseInfo;
        this.gameState = gameState;

        this.painters = new Painters(game, bwem, resourceManager);
    }

    public void sendScout() {
        if(scoutingAttempts >= 3) {
            attemptsMaxed = true;
            return;
        }

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
                scoutingAttempts++;
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


        Position enemyBasePos = gameState.getStartingEnemyBase().getEnemyPosition();
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
        if(player.supplyUsed() / 2 >= 10 && gameState.getStartingEnemyBase() == null) {
            sendScout();
        }

        if(gameState.getStartingEnemyBase() != null) {
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

    public boolean isAttemptsMaxed() {
        return attemptsMaxed;
    }
}
