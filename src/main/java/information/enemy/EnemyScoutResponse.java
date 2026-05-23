package information.enemy;

import bwapi.Game;
import bwapi.Position;
import bwapi.TilePosition;
import information.GameState;
import information.MapInfo;
import unitgroups.WorkerManager;
import unitgroups.units.WorkerStatus;
import unitgroups.units.Workers;
import util.Time;

public class EnemyScoutResponse {
    private Game game;
    private GameState gameState;
    private WorkerManager workerManager;
    private MapInfo mapInfo;
    private Workers counterScout;

    public EnemyScoutResponse(Game game, GameState gameState, WorkerManager workerManager, MapInfo mapInfo) {
        this.game = game;
        this.gameState = gameState;
        this.workerManager = workerManager;
        this.mapInfo = mapInfo;
    }

    private boolean isInDefendedTiles(TilePosition tile) {
        if (tile == null) {
            return false;
        }

        if (mapInfo.getBaseTiles().contains(tile)) {
            return true;
        }

        if (mapInfo.getMinBaseTiles().contains(tile)) {
            return true;
        }

        if (mapInfo.getNaturalTiles().contains(tile) && (mapInfo.isNaturalOwned() || mapInfo.hasBunkerInNatural())) {
            return true;
        }

        return false;
    }

    private void initialScoutInBase() {
        Position baseCenter = mapInfo.getStartingBase().getCenter();
        EnemyUnits closestWorker = null;
        double closestDistance = Double.MAX_VALUE;

        for (EnemyUnits enemyUnit : gameState.getKnownEnemyUnits()) {
            if (!enemyUnit.getEnemyType().isWorker()) {
                continue;
            }

            if (enemyUnit.getEnemyPosition() == null) {
                continue;
            }

            if (!isInDefendedTiles(enemyUnit.getEnemyTilePosition())) {
                continue;
            }

            double distance = enemyUnit.getEnemyPosition().getDistance(baseCenter);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestWorker = enemyUnit;
            }
        }

        if (closestWorker != null) {
            setEnemyScout(closestWorker);
        }
    }

    private void followScout() {
        if (counterScout != null) {
            if (gameState.getEnemyScout().getEnemyPosition() != null) {
                counterScout.getUnit().attack(gameState.getEnemyScout().getEnemyUnit());
            }

        }
    }

    private void assignCounterScout() {
        if (counterScout != null) {
            return;
        }

        Position scoutPosition = gameState.getEnemyScout().getEnemyPosition();
        if (scoutPosition == null) {
            return;
        }

        Workers bestWorker = null;
        double bestDistance = Double.MAX_VALUE;

        for (Workers worker : workerManager.getWorkers()) {
            if (worker.getWorkerStatus() != WorkerStatus.MINERALS) {
                continue;
            }

            if (worker.getUnit().isCarryingMinerals()) {
                continue;
            }

            if (!mapInfo.getBaseTiles().contains(worker.getUnit().getTilePosition())) {
                continue;
            }

            double distance = worker.getUnit().getPosition().getDistance(scoutPosition);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestWorker = worker;
            }
        }

        if (bestWorker != null) {
            bestWorker.setWorkerStatus(WorkerStatus.COUNTERSCOUT);
            counterScout = bestWorker;
        }
    }

    private void clearCounterScout() {
       if (counterScout == null) {
           return;
       }

        if (gameState.getEnemyScout() != null) {
            if (gameState.getEnemyScout().getEnemyPosition() == null
                    || (!gameState.getKnownEnemyUnits().contains(gameState.getEnemyScout()) && !gameState.isEnemyInBase())) {
                counterScout.setWorkerStatus(WorkerStatus.IDLE);
                counterScout = null;
            }
        }
        else {
            counterScout.setWorkerStatus(WorkerStatus.IDLE);
            counterScout = null;
        }
    }

    private void clearEnemyScout() {
        if (gameState.getEnemyScout().getEnemyPosition() == null
                || (!gameState.getKnownEnemyUnits().contains(gameState.getEnemyScout()) && !gameState.isEnemyInBase())) {
            gameState.setEnemyScout(null);
            return;
        }

        if (!isInDefendedTiles(gameState.getEnemyScout().getEnemyTilePosition()) && new Time(game.getFrameCount()).greaterThan(new Time(3,30))) {
            gameState.setEnemyScout(null);
        }
    }

    private void setEnemyScout (EnemyUnits enemyScout) {
        gameState.setEnemyScout(enemyScout);
    }

    public void onFrame() {
        if (new Time(game.getFrameCount()).greaterThan(new Time(3,30)) && gameState.getEnemyScout() == null) {
            if (counterScout == null) {
                return;
            }

            clearCounterScout();
            return;
        }

        if (gameState.getEnemyScout() == null) {
            initialScoutInBase();
        }

        if (gameState.getEnemyScout() == null) {
            clearCounterScout();
        }


        if (gameState.getEnemyScout() != null) {
            assignCounterScout();
            followScout();
            clearEnemyScout();
        }


    }
}
