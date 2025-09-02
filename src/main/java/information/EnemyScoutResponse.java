package information;

import bwapi.Game;
import macro.ResourceManager;
import macro.unitgroups.WorkerStatus;
import macro.unitgroups.Workers;
import util.Time;

public class EnemyScoutResponse {
    private Game game;
    private EnemyInformation enemyInformation;
    private ResourceManager resourceManager;
    private BaseInfo baseInfo;
    private EnemyUnits enemyScout;
    private Workers counterScout;
    private boolean initiallyScouted = false;

    public EnemyScoutResponse(Game game, EnemyInformation enemyInformation, ResourceManager resourceManager, BaseInfo baseInfo) {
        this.game = game;
        this.enemyInformation = enemyInformation;
        this.resourceManager = resourceManager;
        this.baseInfo = baseInfo;
    }

    private void initialScoutInBase() {
        for(EnemyUnits enemyUnit : enemyInformation.getEnemyUnits()) {
            if(enemyUnit.getEnemyType().isWorker() && enemyInformation.enemysInMain()) {
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
            for(Workers worker : resourceManager.getWorkers()) {
                if(!worker.getUnit().isCarryingMinerals() && worker.getWorkerStatus() == WorkerStatus.MINERALS) {
                    worker.setWorkerStatus(WorkerStatus.COUNTERSCOUT);
                    counterScout = worker;
                    return;
                }
            }
        }
    }

    private void clearCounterScout() {
        if(counterScout != null) {
            if(!enemyScout.getEnemyUnit().exists()) {
                counterScout.setWorkerStatus(WorkerStatus.IDLE);
                counterScout = null;
            }
        }
    }

    private void setEnemyScout (EnemyUnits enemyScout) {
        this.enemyScout = enemyScout;
    }

    public void onFrame() {
        if(new Time(game.getFrameCount()).greaterThan(new Time(3,30))) {
            clearCounterScout();
            return;
        }

        if(!initiallyScouted) {
            initialScoutInBase();
        }

        if(enemyScout != null) {
            assignCounterScout();
            followScout();
        }


    }
}
