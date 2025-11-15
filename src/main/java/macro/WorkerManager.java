package macro;

import bwapi.*;
import bwem.Base;
import bwem.Mineral;
import debug.Painters;
import information.BaseInfo;
import information.GameState;
import information.enemy.EnemyScoutResponse;
import information.enemy.EnemyUnits;
import macro.unitgroups.WorkerStatus;
import macro.unitgroups.Workers;

import java.util.HashMap;
import java.util.HashSet;

public class WorkerManager {
    private BaseInfo baseInfo;
    private Player player;
    private Game game;
    private GameState gameState;
    private Painters painters;
    private EnemyScoutResponse enemyScoutResponse;
    private HashSet<Workers> workers;
    private HashSet<Workers> defenseForce = new HashSet<>();
    private HashSet<Workers> repairForce = new HashSet<>();
    private HashMap<Unit, HashSet<Workers>> refinerySaturation = new HashMap<>();
    private HashMap<Base, HashSet<Workers>> mineralSaturation = new HashMap<>();
    private HashMap<Unit, Workers> buildingRepair = new HashMap<>();
    private boolean openerResponse = false;
    private boolean startingMineralsAssigned = false;

    public WorkerManager(BaseInfo baseInfo, Player player, Game game, GameState gameState) {
        this.baseInfo = baseInfo;
        this.player = player;
        this.gameState = gameState;
        this.game = game;

        workers = gameState.getWorkers();
        painters = new Painters(game);

        enemyScoutResponse = new EnemyScoutResponse(game, gameState, this, baseInfo);
    }

    //TODO: refactor all of this and organize with switch cases
    public void onFrame() {
        startingMineralAssignment();
        gatherGas();
        workerBuildClock();
        buildingHealthCheck();
        preemptiveBunkerRepair();
        enemyScoutResponse.onFrame();

        int frameCount = game.getFrameCount();

        if(gameState.getEnemyOpener() != null && !openerResponse) {
            enemyStrategyResponse();
            openerResponse = true;
        }

        for(Workers worker : workers) {
            if(worker.getUnit().isUnderAttack() && (worker.getWorkerStatus() != WorkerStatus.SCOUTING || worker.getWorkerStatus() != WorkerStatus.COUNTERSCOUT)) {
                if(baseInfo.getBaseTiles().contains(worker.getUnit().getTilePosition()) && actuallyThreatened()) {
                    createDefenseForce(7);
                }
            }

            if(worker.getWorkerStatus() == WorkerStatus.IDLE) {
                if(!worker.isAssignedToBase()) {
                    assignMineralSaturation(worker);
                }
                worker.setWorkerStatus(WorkerStatus.MINERALS);
            }


            if(worker.getWorkerStatus() == WorkerStatus.DEFEND) {
                ClosestUnit.findClosestUnit(worker, gameState.getKnownEnemyUnits(), 900);
                workerAttackClock(worker);

                if(frameCount % 24 != 0) {
                    return;
                }

                if(worker.getEnemyUnit() != null) {
                    worker.selfDefense();
                }

                if((worker.getAttackClock() > 300 && worker.getEnemyUnit() == null) || !enemyInBase()) {
                    worker.setWorkerStatus(WorkerStatus.IDLE);
                    worker.setAttackClock(0);
                    removeDefenseForce(worker);
                }

            }

            if(worker.getWorkerStatus() == WorkerStatus.MINERALS) {
                for(Unit building : buildingRepair.keySet()) {
                    if(buildingRepair.get(building) == null) {
                        buildingRepair.put(building, worker);
                        worker.setRepairTarget(building);
                        worker.setWorkerStatus(WorkerStatus.REPAIRING);
                    }
                }
                if(worker.getUnit().isIdle()) {
                    gatherMinerals(worker);
                }

                if(worker.getUnit().isGatheringGas()) {
                    worker.getUnit().stop();
                }

                worker.setIdleClock(0);
            }

            //TODO: this helps with false positives but still improperly tries to free up workers actually stuck
            if(worker.getWorkerStatus() == WorkerStatus.STUCK) {
                if(frameCount % 24 != 0) {
                    return;
                }

                if(worker.getUnit().isGatheringMinerals()) {
                    worker.setWorkerStatus(WorkerStatus.MINERALS);
                }

                gatherMinerals(worker);

            }

            if(worker.getWorkerStatus() == WorkerStatus.REPAIRING) {
                worker.repair(worker.getRepairTarget());
            }

            if((worker.getWorkerStatus() == WorkerStatus.BUILDING) && worker.getUnit().isIdle()) {
                worker.setIdleClock(worker.getIdleClock() + 12);

                if(worker.getIdleClock() > 300) {
                    worker.setWorkerStatus(WorkerStatus.IDLE);
                    worker.setIdleClock(0);
                }
            }

        }

        painters.paintWorker(workers);
        painters.paintWorkerText(workers);
    }

    private void gatherMinerals(Workers worker) {
        for(Base base : mineralSaturation.keySet()) {
            if(mineralSaturation.get(base).contains(worker)) {
                for(Mineral mineral : base.getMinerals()) {
                    worker.getUnit().gather(mineral.getUnit());
                    break;
                }
            }
        }

        if(!worker.isAssignedToBase()) {
            assignMineralSaturation(worker);
        }
    }

    private void gatherGas() {
        for(Workers scv : workers) {
            if(scv.getWorkerStatus() == WorkerStatus.MINERALS) {
                for(Unit geyser : refinerySaturation.keySet()) {
                    if(refinerySaturation.get(geyser).size() < 3) {
                        refinerySaturation.get(geyser).add(scv);
                        scv.setWorkerStatus(WorkerStatus.GAS);
                        scv.getUnit().gather(geyser);
                    }
                }
            }
        }
    }

    private void assignMineralSaturation(Workers worker) {
        for(Base base : baseInfo.getOwnedBases()) {
            if(mineralSaturation.get(base).size() < 24) {
                mineralSaturation.get(base).add(worker);
                worker.setAssignedToBase(true);
                break;
            }
        }
    }

    private void removeMineralSaturation(Workers worker) {
        for(Base base : mineralSaturation.keySet()) {
            if(mineralSaturation.get(base).contains(worker)) {
                mineralSaturation.get(base).remove(worker);
                break;
            }
        }
    }

    private void startingMineralAssignment() {
        if(startingMineralsAssigned) {
            return;
        }

        Base mainBase = baseInfo.getStartingBase();

        HashSet<Unit> minerals = new HashSet<>(mainBase.getMinerals().stream()
                .map(Mineral::getUnit)
                .collect(java.util.stream.Collectors.toList()));

        for(Workers worker : workers) {
            if(!minerals.isEmpty()) {
                Unit mineralPatch = minerals.iterator().next();
                worker.getUnit().gather(mineralPatch);
                minerals.remove(mineralPatch);
            }
        }
        startingMineralsAssigned = true;
    }

    //
    private void workerBuildClock() {
        for(Workers worker : workers) {
            if(worker.getWorkerStatus() == WorkerStatus.MOVING_TO_BUILD) {
                worker.stuckCheck();
                worker.setBuildFrameCount(worker.getBuildFrameCount() + 1);
            }
            else {
                worker.setLastFrameChecked(0);
                worker.setBuildFrameCount(0);
            }

        }
    }

    private void createDefenseForce(int defenseSize) {
        for(Workers worker : workers) {
            if(worker.getWorkerStatus() == WorkerStatus.MINERALS && defenseForce.size() < defenseSize) {
                defenseForce.add(worker);
                worker.setWorkerStatus(WorkerStatus.DEFEND);
            }

            if(defenseForce.size() >= defenseSize) {
                return;
            }
        }
    }

    private void removeDefenseForce(Workers worker) {
        worker.setWorkerStatus(WorkerStatus.IDLE);
        defenseForce.remove(workers);
    }

    //Avoid false positives from a single worker attacking a scv (Stone check)
    private boolean actuallyThreatened() {
        int enemyWorkerCount = 0;
        for(EnemyUnits enemyUnit : gameState.getKnownEnemyUnits()) {
            if(enemyUnit.getEnemyType().isWorker()) {
                if(enemyUnit.getEnemyPosition() == null) {
                    continue;
                }

                if(baseInfo.getBaseTiles().contains(enemyUnit.getEnemyUnit().getTilePosition())) {
                    enemyWorkerCount++;
                    continue;
                }
            }
            if(baseInfo.getBaseTiles().contains(enemyUnit.getEnemyUnit().getTilePosition())) {
                return true;
            }
        }
        return enemyWorkerCount > 1;
    }

    private boolean enemyInBase() {
        for(EnemyUnits enemyUnit : gameState.getKnownEnemyUnits()) {
            if(enemyUnit.getEnemyPosition() == null) {
                continue;
            }

            TilePosition enemyTile = enemyUnit.getEnemyPosition().toTilePosition();
            if (baseInfo.getBaseTiles().contains(enemyTile)) {
                return true;
            }
        }
        return false;
    }

    private void enemyStrategyResponse() {
        switch(gameState.getEnemyOpener().getStrategyName()) {
            case "Cannon Rush":
                createDefenseForce(6);
                break;
        }
    }

    private void buildingHealthCheck() {
        for(Unit building : player.getUnits()) {
            if(building.getType().isBuilding() && building.getHitPoints() < building.getType().maxHitPoints() && building.isCompleted()) {
                if(!buildingRepair.containsKey(building)) {
                    buildingRepair.put(building, null);
                }
            }

            if(building.getType().isBuilding() && building.getHitPoints() >= building.getType().maxHitPoints() && building.isCompleted()) {
                if(buildingRepair.containsKey(building)) {
                    buildingRepair.remove(building);
                }
            }
        }
    }

    private void repairTargetDestroyed(Unit building) {
        if(buildingRepair.containsKey(building)) {
            if(buildingRepair.get(building) != null) {
                Workers worker = buildingRepair.get(building);
                worker.setWorkerStatus(WorkerStatus.IDLE);
                worker.setRepairTarget(null);
            }
        }

        if(building.getType() == UnitType.Terran_Bunker) {
            for(Workers worker : repairForce) {
                worker.setWorkerStatus(WorkerStatus.IDLE);
                worker.setRepairTarget(null);
                worker.setPreemptiveRepair(false);
            }
            repairForce.clear();
        }
    }

    private void preemptiveBunkerRepair() {
        for(Unit bunker : player.getUnits()) {
            if(bunker.getType() == UnitType.Terran_Bunker && bunker.isCompleted()) {
                if(enemyInRange()) {
                    for(Workers worker : workers) {
                        if(worker.getWorkerStatus() == WorkerStatus.MINERALS && repairForce.size() < 3) {
                            worker.setWorkerStatus(WorkerStatus.REPAIRING);
                            worker.setRepairTarget(bunker);
                            worker.setPreemptiveRepair(true);
                            repairForce.add(worker);
                        }
                        else if(repairForce.size() > 2) {
                            break;
                        }
                    }
                }
                else if(!enemyInRange()) {
                    for(Workers worker : repairForce) {
                        worker.setWorkerStatus(WorkerStatus.IDLE);
                        worker.setRepairTarget(null);
                        worker.setPreemptiveRepair(false);
                    }
                    repairForce.clear();

                }
            }
        }
    }

    private boolean enemyInRange() {
        Unit mainBunker = null;
        for(Unit bunker : player.getUnits()) {
            if(bunker.getType() == UnitType.Terran_Bunker && bunker.isCompleted()) {
                mainBunker = bunker;
                break;
            }
        }

        for(EnemyUnits enemyUnit : gameState.getKnownEnemyUnits()) {
            if(enemyUnit.getEnemyPosition() == null) {
                continue;
            }

            if(enemyUnit.getEnemyType() == UnitType.Zerg_Overlord) {
                continue;
            }

            if(enemyUnit.getEnemyType().isWorker()) {
                continue;
            }

            if(enemyUnit.getEnemyUnit().getType().isFlyer() || !enemyUnit.getEnemyUnit().isDetected()) {
                continue;
            }

            if(!enemyUnit.getEnemyUnit().exists()) {
                continue;
            }

            if(mainBunker == null) {
                return false;
            }

            if(enemyUnit.getEnemyPosition().getApproxDistance(mainBunker.getPosition()) < 400) {
                return true;
            }

        }
        return false;
    }

    private void workerAttackClock(Workers worker) {
        worker.setAttackClock(worker.getAttackClock() + 1);
    }

    public void onUnitComplete(Unit unit) {
        if(unit.getType() == UnitType.Terran_SCV) {
            workers.add(new Workers(game, unit));
        }

        if(unit.getType() == UnitType.Terran_Refinery) {
            refinerySaturation.put(unit, new HashSet<>());
        }

        if(unit.getType() == UnitType.Terran_Command_Center) {
            for(Base base : baseInfo.getOwnedBases()) {
                if(unit.getPosition().getApproxDistance(base.getCenter()) < 100) {
                    mineralSaturation.put(base, new HashSet<>());
                }
            }
        }

    }

    public void onUnitDestroy(Unit unit) {
        if(unit.getPlayer() != player) {
            return;
        }

        if(unit.getType().isBuilding()) {
            repairTargetDestroyed(unit);
        }

        if(unit.getType() != UnitType.Terran_SCV && unit.getType() != UnitType.Terran_Refinery) {
            return;
        }

        if(unit.getType() == UnitType.Terran_Refinery) {
            for(Workers worker : refinerySaturation.get(unit)) {
                worker.setWorkerStatus(WorkerStatus.IDLE);
            }

            refinerySaturation.remove(unit);
            return;
        }

        for(Workers worker : workers) {
            if(worker.getUnit() == unit) {

                for(Unit building : buildingRepair.keySet()) {
                    if(buildingRepair.get(building) == worker) {
                        buildingRepair.put(building, null);
                    }
                }

                removeMineralSaturation(worker);
                workers.remove(worker);

                break;
            }
        }
    }

    public HashSet<Workers> getWorkers() {
        return workers;
    }
}
