package information.enemy;

import bwapi.Game;
import information.BaseInfo;
import information.GameState;
import unitgroups.WorkerManager;
import unitgroups.units.WorkerStatus;
import unitgroups.units.Workers;
import util.Time;

public class EnemyScoutResponse {
    private Game game;
    private GameState gameState;
    private WorkerManager workerManager;
    private BaseInfo baseInfo;
    private EnemyUnits enemyScout;
    private Workers counterScout;
    private boolean initiallyScouted = false;

    public EnemyScoutResponse(Game game, GameState gameState, WorkerManager workerManager, BaseInfo baseInfo) {
        this.game = game;
        this.gameState = gameState;
        this.workerManager = workerManager;
        this.baseInfo = baseInfo;
    }

    private void initialScoutInBase() {
        for(EnemyUnits enemyUnit : gameState.getKnownEnemyUnits()) {
            if(enemyUnit.getEnemyType().isWorker() && gameState.isEnemyInBase()) {
                setEnemyScout(enemyUnit);
                initiallyScouted = true;
            }
        }
    }

    private void followScout() {
        if(counterScout != null) {
            if(enemyScout.getEnemyPosition() != null) {
                counterScout.getUnit().attack(enemyScout.getEnemyUnit());
            }

        }
    }

    private void assignCounterScout() {
        if(counterScout == null) {
            for(Workers worker : workerManager.getWorkers()) {
                if(!worker.getUnit().isCarryingMinerals() && worker.getWorkerStatus() == WorkerStatus.MINERALS) {
                    worker.setWorkerStatus(WorkerStatus.COUNTERSCOUT);
                    counterScout = worker;
                    return;
                }
            }
        }
    }

    private void clearCounterScout() {
       if(counterScout == null) {
           return;
       }

        if(enemyScout != null) {
            if(!gameState.getKnownEnemyUnits().contains(enemyScout) && !gameState.isEnemyInBase()) {
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
        if(!gameState.getKnownEnemyUnits().contains(enemyScout) && !gameState.isEnemyInBase()) {
            enemyScout = null;
        }
    }

    private void setEnemyScout (EnemyUnits enemyScout) {
        this.enemyScout = enemyScout;
    }

    public void onFrame() {
        if(new Time(game.getFrameCount()).greaterThan(new Time(3,30))) {
            if(counterScout == null) {
                return;
            }

            clearCounterScout();
            return;
        }

        if(!initiallyScouted) {
            initialScoutInBase();
        }

        if(enemyScout == null && initiallyScouted) {
            clearCounterScout();;
        }


        if(enemyScout != null) {
            assignCounterScout();
            followScout();
            clearEnemyScout();
        }


    }
}
