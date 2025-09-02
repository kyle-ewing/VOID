package macro;

import bwapi.*;
import bwem.Base;
import bwem.Mineral;
import information.BaseInfo;
import information.EnemyInformation;
import information.EnemyScoutResponse;
import information.EnemyUnits;
import macro.unitgroups.WorkerStatus;
import macro.unitgroups.Workers;

import java.util.HashMap;
import java.util.HashSet;

public class ResourceManager {
    private BaseInfo baseInfo;
    private Player player;
    private Game game;
    private EnemyInformation enemyInformation;
    private EnemyScoutResponse enemyScoutResponse;
    private HashSet<Workers> workers = new HashSet<>();
    private HashSet<Workers> defenseForce = new HashSet<>();
    private HashSet<Workers> repairForce = new HashSet<>();
    private HashMap<Unit, HashSet<Workers>> refinerySaturation = new HashMap<>();
    private HashMap<Base, HashSet<Workers>> mineralSaturation = new HashMap<>();
    private HashMap<Unit, Workers> buildingRepair = new HashMap<>();
    private int reservedMinerals = 0;
    private int reservedGas = 0;
    private int availableMinerals = 0;
    private int availableGas = 0;
    private boolean isReserved = false;
    private boolean openerResponse = false;
    private boolean startingMineralsAssigned = false;

    public ResourceManager(BaseInfo baseInfo, Player player, Game game, EnemyInformation enemyInformation) {
        this.baseInfo = baseInfo;
        this.player = player;
        this.enemyInformation = enemyInformation;
        this.game = game;

        enemyScoutResponse = new EnemyScoutResponse(game, enemyInformation, this, baseInfo);
    }

    //TODO: refactor all of this and organize with switch cases
    public void onFrame() {
        startingMineralAssignment();
        setAvailableMinerals(availableMinerals);
        setAvailableGas(availableGas);
        gatherGas();
        workerBuildClock();
        buildingHealthCheck();
        preemptiveBunkerRepair();
        enemyScoutResponse.onFrame();

        int frameCount = game.getFrameCount();

        if(enemyInformation.getEnemyOpener() != null && !openerResponse) {
            enemyStrategyResponse();
            openerResponse = true;
        }

        for(Workers worker : workers) {
            if(worker.getUnit().isUnderAttack() && (worker.getWorkerStatus() != WorkerStatus.SCOUTING || worker.getWorkerStatus() != WorkerStatus.COUNTERSCOUT)) {
                if(baseInfo.getBaseTiles().contains(worker.getUnit().getTilePosition())) {
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
                updateClosetEnemy(worker);
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
    }

    private void gatherMinerals(Workers worker) {
        for(Base base : mineralSaturation.keySet()) {
            if(mineralSaturation.get(base).contains(worker)) {
                for(Mineral mineral : base.getMinerals()) {
                    worker.getUnit().gather(mineral.getUnit());
                    worker.setWorkerStatus(WorkerStatus.MINERALS);
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

    public  void reserveResources(UnitType unitType) {
        reservedMinerals += unitType.mineralPrice();
        reservedGas += unitType.gasPrice();
        availableMinerals = player.minerals() - reservedMinerals;
        availableGas = player.gas() - reservedGas;
        isReserved = true;
    }

    public void unreserveResources(UnitType unitType) {
        reservedMinerals -= unitType.mineralPrice();
        reservedGas -= unitType.gasPrice();
        availableMinerals = player.minerals() - reservedMinerals;
        availableGas = player.gas() - reservedGas;
        isReserved = false;
    }

    //
    private void workerBuildClock() {
        for(Workers worker : workers) {
            if(worker.getWorkerStatus() == WorkerStatus.MOVING_TO_BUILD) {
                worker.setBuildFrameCount(worker.getBuildFrameCount() + 1);
            }
            else {
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

    public void updateClosetEnemy(Workers worker) {
        int closestDistance = 1000;
        EnemyUnits closestEnemy = null;

        for (EnemyUnits enemyUnit : enemyInformation.getEnemyUnits()) {
            Position enemyPosition = enemyUnit.getEnemyPosition();
            Position unitPosition = worker.getUnit().getPosition();

            //Stop units from getting stuck on outdated position info
            if(worker.getUnit().getDistance(enemyPosition) < 250 && !enemyUnit.getEnemyUnit().isVisible()) {
                continue;
            }

            if(!worker.getUnit().hasPath(enemyPosition)) {
                continue;
            }

            if(enemyUnit.getEnemyUnit().getType().isFlyer()) {
                continue;
            }

            int distance = unitPosition.getApproxDistance(enemyPosition);

            if (distance < closestDistance) {
                closestDistance = distance;
                closestEnemy = enemyUnit;
            }
        }

        if (closestEnemy != null) {
            worker.setEnemyUnit(closestEnemy);
        }
        else {
            worker.setEnemyUnit(null);
        }
    }

    private boolean enemyInBase() {
        for(EnemyUnits enemyUnit : enemyInformation.getEnemyUnits()) {
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
        switch(enemyInformation.getEnemyOpener().getStrategyName()) {
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

        for(EnemyUnits enemyUnit : enemyInformation.getEnemyUnits()) {
            if(enemyUnit.getEnemyPosition() == null) {
                continue;
            }

            if(enemyUnit.getEnemyType() == UnitType.Zerg_Overlord) {
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
            workers.add(new Workers(unit, WorkerStatus.IDLE));
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

    public int getReservedMinerals() {
        return reservedMinerals;
    }

    public void setReservedMinerals(int reservedMinerals) {
        this.reservedMinerals = reservedMinerals;
    }

    public int getReservedGas() {
        return reservedGas;
    }

    public void setReservedGas(int reservedGas) {
        this.reservedGas = reservedGas;
    }

    public int getAvailableMinerals() {
        return availableMinerals;
    }

    private void setAvailableMinerals(int availableMinerals) {
        this.availableMinerals = player.minerals() - reservedMinerals;
    }

    public int getAvailableGas() {
        return availableGas;
    }

    public void setAvailableGas(int availableGas) {
        this.availableGas = player.gas() - reservedGas;
    }

    public boolean isReserved() {
        return isReserved;
    }

    public void setReserved(boolean reserved) {
        isReserved = reserved;
    }

    public HashSet<Workers> getWorkers() {
        return workers;
    }

    public HashSet<Workers> getDefenseForce() {
        return defenseForce;
    }
}
