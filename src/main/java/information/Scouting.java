package information;

import bwapi.Player;
import bwem.BWEM;
import bwem.Base;
import macro.ResourceManager;
import macro.unitgroups.WorkerStatus;
import macro.unitgroups.Workers;

public class Scouting {
    private BWEM bwem;
    private ResourceManager resourceManager;
    private BaseInfo baseInfo;
    private Player player;
    private EnemyInformation enemyInformation;
    private boolean isScouting = false;

    private Workers scout;

    public Scouting(BWEM bwem, Player player, ResourceManager resourceManager, BaseInfo baseInfo, EnemyInformation enemyInformation) {
        this.bwem = bwem;
        this.player = player;
        this.resourceManager = resourceManager;
        this.baseInfo = baseInfo;
        this.enemyInformation = enemyInformation;
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

    public void updateScouting() {
        //have scout do things when it finds enemy base
    }

    public void onFrame() {
        if(player.supplyUsed() / 2 >= 10) {
            sendScout();
        }
    }
}
