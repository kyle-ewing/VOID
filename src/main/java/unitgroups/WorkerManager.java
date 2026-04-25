package unitgroups;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import bwapi.Game;
import bwapi.Player;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwem.Base;
import bwem.Mineral;
import information.GameState;
import information.MapInfo;
import information.enemy.EnemyInformation;
import information.enemy.EnemyScoutResponse;
import information.enemy.EnemyUnits;
import unitgroups.units.WorkerStatus;
import unitgroups.units.Workers;
import util.ClosestUnit;
import util.Time;

public class WorkerManager {
    private MapInfo mapInfo;
    private Player player;
    private Game game;
    private GameState gameState;
    private EnemyInformation enemyInformation;
    private EnemyScoutResponse enemyScoutResponse;
    private HashSet<Workers> workers;
    private HashSet<Workers> defenseForce = new HashSet<>();
    private HashSet<Workers> repairForce = new HashSet<>();
    private HashMap<Unit, HashSet<Workers>> refinerySaturation = new HashMap<>();
    private HashMap<Base, HashSet<Workers>> mineralSaturation = new HashMap<>();
    private HashMap<Unit, Workers> buildingRepair = new HashMap<>();
    private boolean openerResponse = false;
    private boolean initialMineralAssignmentDone = false;
    private boolean mineralBlockersCleared = false;

    public WorkerManager(MapInfo mapInfo, Player player, Game game, GameState gameState, EnemyInformation enemyInformation) {
        this.mapInfo = mapInfo;
        this.player = player;
        this.gameState = gameState;
        this.game = game;
        this.enemyInformation = enemyInformation;

        workers = gameState.getWorkers();

        enemyScoutResponse = new EnemyScoutResponse(game, gameState, this, mapInfo);

    }

    public void onFrame() {
        gatherGas();
        workerBuildClock();
        buildingHealthCheck();
        preemptiveBunkerRepair();
        clearNaturalDefenseForce();
        enemyScoutResponse.onFrame();

        int frameCount = game.getFrameCount();

        if (gameState.getEnemyOpener() != null && new Time(frameCount).lessThanOrEqual(new Time(4,30))) {
            enemyStrategyResponse();
            openerResponse = true;
        }

        if (gameState.hasTransitioned() && !mineralBlockersCleared) {
            removeMineralBlockers();
        }

        for (Workers worker : workers) {
            if (new Time(frameCount).lessThanOrEqual(new Time(6,0))) {
                if (worker.getUnit().isUnderAttack() && (worker.getWorkerStatus() != WorkerStatus.SCOUTING || worker.getWorkerStatus() != WorkerStatus.COUNTERSCOUT)) {
                    //Stop worker defense after the early game
                    if (mapInfo.getBaseTiles().contains(worker.getUnit().getTilePosition()) && actuallyThreatened() && !hasCompletedCannonInBase() && !hasBunker()) {
                        if (workers.size() > 12) {
                            createDefenseForce(6);
                        }
                        else {
                            createDefenseForce(4);
                        }

                    }
                }

                //TODO: move to enemystratresponse
                if (gameState.getEnemyOpener() != null
                        && gameState.getEnemyOpener().getStrategyName().equals("Gas Steal")
                        && gameState.isEnemyInBase()) {
                    if (new Time(game.getFrameCount()).lessThanOrEqual(new Time(2, 5))) {
                        createDefenseForce(1);
                    }
                    else if (new Time(game.getFrameCount()).lessThanOrEqual(new Time(3, 0))) {
                        createDefenseForce(4);
                    }
                }
            }

            switch (worker.getWorkerStatus()) {
                case MINERALS:
                    for (Unit building : buildingRepair.keySet()) {
                        if (buildingRepair.get(building) == null) {
                            buildingRepair.put(building, worker);
                            worker.setRepairTarget(building);
                            worker.setWorkerStatus(WorkerStatus.REPAIRING);
                        }
                    }

                    if (worker.getUnit().isGatheringGas() && !worker.getUnit().isCarryingGas()) {
                        worker.getUnit().stop();
                    }
                    else if (worker.getUnit().isIdle() || worker.getUnit().isAttacking() || !worker.getUnit().isGatheringMinerals()) {
                        gatherMinerals(worker);
                    }

                    worker.setIdleClock(0);
                    break;
                case GAS:
                    break;
                case IDLE:
                    if (!worker.isAssignedToBase()) {
                        assignMineralSaturation(worker);
                    }
                    worker.setWorkerStatus(WorkerStatus.MINERALS);
                    break;
                case DEFEND:
                    ClosestUnit.findClosestUnit(worker, gameState.getKnownEnemyUnits(), 900);
                    workerAttackClock(worker);

                    if (frameCount % 24 != 0) {
                        break;
                    }

                    if (worker.getEnemyUnit() != null) {
                        TilePosition enemyTile = worker.getEnemyUnit().getEnemyPosition().toTilePosition();
                        if (mapInfo.getBaseTiles().contains(enemyTile) || mapInfo.getNaturalTiles().contains(enemyTile)) {
                            worker.selfDefense();
                        }
                    }

                    if ((worker.getAttackClock() > 300 && worker.getEnemyUnit() == null) || !enemyInBase()
                            || (worker.getEnemyUnit() == null
                            && !mapInfo.getBaseTiles().contains(worker.getUnit().getTilePosition())
                            && !mapInfo.getNaturalTiles().contains(worker.getUnit().getTilePosition()))
                            || hasCompletedCannonInBase()) {
                        worker.setWorkerStatus(WorkerStatus.IDLE);
                        worker.setAttackClock(0);
                        worker.setAssignedToBase(false);
                        removeDefenseForce(worker);
                    }
                    break;
                case BUILDING:
                    if (worker.getUnit().isIdle()) {
                        worker.setIdleClock(worker.getIdleClock() + 12);

                        if (worker.getIdleClock() > 300) {
                            worker.setWorkerStatus(WorkerStatus.IDLE);
                            worker.setIdleClock(0);
                        }
                    }
                    break;
                case REPAIRING:
                    if (worker.isPreemptiveRepair()) {
                        Unit nearbyDamaged = findNearbyDamagedBuilding(worker, 75);
                        if (nearbyDamaged != null) {
                            worker.repair(nearbyDamaged);
                            break;
                        }
                    }
                    worker.repair(worker.getRepairTarget());

                    if (obstructingBuild(worker)) {
                        moveFromObstruction(worker);
                    }
                    break;
                case SCOUTING:
                    if (gameState.getEnemyOpener() == null) {
                        break;
                    }

                    scoutAttack(worker);
                    break;
                case STUCK:
                    if (frameCount % 24 != 0) {
                        return;
                    }

                    if (worker.getUnit().isGatheringMinerals()) {
                        worker.setWorkerStatus(WorkerStatus.MINERALS);
                    }

                    gatherMinerals(worker);
                    break;
                case MOVING_TO_BUILD:
                    worker.pulseCheck();
                    break;
                case CLEARINGMINE:
                    Position buildPos = worker.getBuildingPosition();

                    if (buildPos == null) {
                        worker.setWorkerStatus(WorkerStatus.MOVING_TO_BUILD);
                        break;
                    }

                    Unit mineTarget = null;
                    for (EnemyUnits enemyUnit : gameState.getKnownEnemyUnits()) {
                        if (!enemyUnit.getEnemyUnit().exists() || !enemyUnit.getEnemyUnit().isDetected()) {
                            continue;
                        }

                        if (enemyUnit.getEnemyUnit().getPosition().getDistance(buildPos) < 160) {
                            mineTarget = enemyUnit.getEnemyUnit();
                            break;
                        }
                    }

                    if (mineTarget != null) {
                        worker.getUnit().attack(mineTarget);
                    }
                    else {
                        worker.setWorkerStatus(WorkerStatus.MOVING_TO_BUILD);
                    }
                    break;
                default:
                    //do nothing
            }
        }

        if (!initialMineralAssignmentDone) {
            startingMineralAssignment();
        }

    }

    private void gatherMinerals(Workers worker) {
        for (Base base : mineralSaturation.keySet()) {
            if (mineralSaturation.get(base).contains(worker)) {
                for (Mineral mineral : base.getMinerals()) {
                    worker.getUnit().gather(mineral.getUnit());
                    break;
                }
            }
        }

        if (!worker.isAssignedToBase()) {
            assignMineralSaturation(worker);
        }
    }

    private void gatherGas() {
        if (gasImbalance()) {
            for (Unit geyser : refinerySaturation.keySet()) {
                if (refinerySaturation.get(geyser).isEmpty()) {
                    continue;
                }

                Iterator<Workers> iterator = refinerySaturation.get(geyser).iterator();
                while (iterator.hasNext()) {
                    Workers worker = iterator.next();
                    worker.setWorkerStatus(WorkerStatus.IDLE);
                    iterator.remove();
                }
            }
            return;
        }

        int gasTarget = getGasWorkerTarget();
        Workers scv;

        for (Unit geyser : refinerySaturation.keySet()) {
            int currentCount = refinerySaturation.get(geyser).size();

            if (currentCount > gasTarget) {
                Iterator<Workers> iterator = refinerySaturation.get(geyser).iterator();
                while (iterator.hasNext() && refinerySaturation.get(geyser).size() > gasTarget) {
                    Workers worker = iterator.next();

                    if (worker.getUnit().isCarryingGas()) {
                        continue;
                    }

                    worker.setWorkerStatus(WorkerStatus.IDLE);
                    iterator.remove();
                }
            }
            else if (currentCount < gasTarget) {
                scv = ClosestUnit.findClosestWorker(geyser.getPosition(), workers, mapInfo.getPathFinding());

                if (scv == null) {
                    continue;
                }

                if (scv.getUnit().isCarryingMinerals()) {
                    continue;
                }

                refinerySaturation.get(geyser).add(scv);
                scv.setWorkerStatus(WorkerStatus.GAS);
                scv.getUnit().gather(geyser);
            }
        }
    }

    private int getGasWorkerTarget() {
        return gameState.getStartingOpener().getGasWorkerTarget(player.gatheredGas(), gameState.getAllBuildings());
    }

    private void assignMineralSaturation(Workers worker) {
        for (Base base : mapInfo.getOwnedBases()) {
            HashSet<Workers> saturation = mineralSaturation.computeIfAbsent(base, k -> new HashSet<>());
            if (saturation.size() < 24) {
                saturation.add(worker);
                worker.setAssignedToBase(true);
                break;
            }
        }
    }

    private void removeMineralSaturation(Workers worker) {
        for (Base base : mineralSaturation.keySet()) {
            if (mineralSaturation.get(base).contains(worker)) {
                mineralSaturation.get(base).remove(worker);
                break;
            }
        }
    }

    //Can't be set on start because nothing is moving/set yet
    private void startingMineralAssignment() {
        if (game.getFrameCount() == 0 || game.getFrameCount() == 1) {
            return;
        }

        if (workers.isEmpty()) {
            return;
        }

        Base mainBase = mapInfo.getStartingBase();

        HashSet<Unit> minerals = mainBase.getMinerals().stream()
                .map(Mineral::getUnit).collect(java.util.stream.Collectors.toCollection(HashSet::new));

        for (Workers worker : workers) {
            if (!minerals.isEmpty()) {
                Unit mineralPatch = minerals.iterator().next();
                worker.getUnit().gather(mineralPatch);
                minerals.remove(mineralPatch);
            }
        }
        initialMineralAssignmentDone = true;
    }

    private void workerBuildClock() {
        for (Workers worker : workers) {
            if (worker.getWorkerStatus() == WorkerStatus.MOVING_TO_BUILD) {
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
        for (Workers worker : workers) {
            if (worker.getWorkerStatus() == WorkerStatus.MINERALS && defenseForce.size() < defenseSize) {
                defenseForce.add(worker);
                removeMineralSaturation(worker);
                worker.setWorkerStatus(WorkerStatus.DEFEND);
            }

            if (defenseForce.size() >= defenseSize) {
                return;
            }
        }
    }

    private void removeDefenseForce(Workers worker) {
        worker.setWorkerStatus(WorkerStatus.IDLE);
        defenseForce.remove(worker);
    }

    private boolean hasBunker() {
        for (Unit building : gameState.getAllBuildings()) {
            if (building.getType() == UnitType.Terran_Bunker && building.isCompleted()) {
                return true;
            }
        }
        return false;
    }

    private void clearNaturalDefenseForce() {
        if (defenseForce.isEmpty()) {
            return;
        }

        if (!hasBunker()) {
            return;
        }

        Iterator<Workers> iterator = defenseForce.iterator();
        while (iterator.hasNext()) {
            Workers worker = iterator.next();
            if (mapInfo.getNaturalTiles().contains(worker.getUnit().getTilePosition())
                    || mapInfo.getBaseTiles().contains(worker.getUnit().getTilePosition())) {
                worker.setWorkerStatus(WorkerStatus.IDLE);
                iterator.remove();
            }
        }
    }

    //Avoid false positives from a single worker attacking a scv (Stone check)
    private boolean actuallyThreatened() {
        int enemyWorkerCount = 0;
        for (EnemyUnits enemyUnit : gameState.getKnownEnemyUnits()) {
            //Don't trigger against lurkers or flyers
            if (enemyUnit.getEnemyUnit().getType().isFlyer() || enemyUnit.getEnemyType() == UnitType.Zerg_Lurker) {
                continue;
            }

            if (enemyUnit.getEnemyType().isWorker()) {
                if (enemyUnit.getEnemyPosition() == null) {
                    continue;
                }

                if (mapInfo.getBaseTiles().contains(enemyUnit.getEnemyUnit().getTilePosition())) {
                    enemyWorkerCount++;
                    continue;
                }
            }
            if (mapInfo.getBaseTiles().contains(enemyUnit.getEnemyUnit().getTilePosition())) {
                return true;
            }
        }
        return enemyWorkerCount > 1;
    }

    private boolean enemyInBase() {
        for (EnemyUnits enemyUnit : gameState.getKnownEnemyUnits()) {
            if (enemyUnit.getEnemyPosition() == null) {
                continue;
            }

            TilePosition enemyTile = enemyUnit.getEnemyPosition().toTilePosition();
            if (mapInfo.getBaseTiles().contains(enemyTile) || mapInfo.getNaturalTiles().contains(enemyTile)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCompletedCannonInBase() {
        for (EnemyUnits enemyUnit : gameState.getKnownEnemyUnits()) {
            if (enemyUnit.getEnemyType() != UnitType.Protoss_Photon_Cannon) {
                continue;
            }
            if (!enemyUnit.getEnemyUnit().isCompleted() || !enemyUnit.getEnemyUnit().isPowered()) {
                continue;
            }
            TilePosition cannonTile = enemyUnit.getEnemyUnit().getTilePosition();
            if (mapInfo.getBaseTiles().contains(cannonTile) || mapInfo.getNaturalTiles().contains(cannonTile)) {
                return true;
            }
        }
        return false;
    }

    private void enemyStrategyResponse() {
        switch (gameState.getEnemyOpener().getStrategyName()) {
            case "Cannon Rush":
                if (!hasCompletedCannonInBase() && !hasBunker()) {
                    if (workers.size() > 12) {
                        createDefenseForce(6);
                    }
                    else {
                        createDefenseForce(3);
                    }
                }
                break;
            case "Four Rax":
                if (gameState.getKnownEnemyUnits().stream().anyMatch(unit -> unit.getEnemyType() == UnitType.Terran_Marine && unit.getEnemyPosition() != null
                        && mapInfo.getBaseTiles().contains(unit.getEnemyPosition().toTilePosition()))) {
                    createDefenseForce(3);
                }
            case "SCV Rush":
                if (gameState.isEnemyInBase()) {
                    createDefenseForce(3);
                }
                break;
        }
    }

    private void buildingHealthCheck() {
        for (Unit building : player.getUnits()) {
            if (building.getType().isBuilding() && building.getHitPoints() < building.getType().maxHitPoints() && building.isCompleted()) {
                if (!buildingRepair.containsKey(building)) {
                    buildingRepair.put(building, null);
                }
            }

            if (building.getType().isBuilding() && building.getHitPoints() >= building.getType().maxHitPoints() && building.isCompleted()) {
                if (buildingRepair.containsKey(building)) {
                    buildingRepair.remove(building);
                }
            }
        }
    }

    private void repairTargetDestroyed(Unit building) {
        if (buildingRepair.containsKey(building)) {
            if (buildingRepair.get(building) != null) {
                Workers worker = buildingRepair.get(building);
                worker.setWorkerStatus(WorkerStatus.IDLE);
                worker.setRepairTarget(null);
            }
        }

        if (building.getType() == UnitType.Terran_Bunker) {
            for (Workers worker : repairForce) {
                worker.setWorkerStatus(WorkerStatus.IDLE);
                worker.setRepairTarget(null);
                worker.setPreemptiveRepair(false);
            }
            repairForce.clear();
        }
    }

    private void preemptiveBunkerRepair() {
        Unit bunker = null;

        for (Unit unit : gameState.getAllBuildings()) {
            if (unit.getType() != UnitType.Terran_Bunker) {
                continue;
            }

            if (mapInfo.hasBunkerInNatural() && mapInfo.getNaturalTiles().contains(unit.getTilePosition())) {
                bunker = unit;
                break;
            }
            else {
                bunker = unit;
            }
        }

        if (bunker == null) {
            return;
        }

        if (bunker.isCompleted()) {
            Position baseCenter = mapInfo.getStartingBase().getCenter();
            if (gameState.isNaturalCCCompleted()) {
                baseCenter = mapInfo.getNaturalBase().getCenter();
            }

            int openerSize = openerRepairForceSize();

            if (openerSize > 0) {
                createRepairForce(bunker, openerSize);
            }
            else if (enemyInRange() && enemyInformation.getEnemySupplyInRange(bunker) >= 6) {
                createRepairForce(bunker, 3);
            }
            else if (bunker.getDistance(baseCenter) > 650
                    && new Time(game.getFrameCount()).greaterThan(new Time(5, 10))
                    && new Time(game.getFrameCount()).lessThanOrEqual(new Time(8,0))) {
                createRepairForce(bunker, 2);
            }
            else if (bunker.getDistance(baseCenter) > 400
                    && new Time(game.getFrameCount()).greaterThan(new Time(4,30))
                    && new Time(game.getFrameCount()).lessThanOrEqual(new Time(8,0))) {
                createRepairForce(bunker, 1);
            }
            else if (!enemyInRange()) {
                for (Workers worker : repairForce) {
                    worker.setWorkerStatus(WorkerStatus.IDLE);
                    worker.setRepairTarget(null);
                    worker.setPreemptiveRepair(false);
                }
                repairForce.clear();
            }
        }
    }

    private void createRepairForce(Unit bunker, int repairSize) {
        if (repairForce.size() == repairSize || workers.size() < 8) {
            return;
        }

        if (repairForce.size() > repairSize) {
            Iterator<Workers> iterator = repairForce.iterator();
            while (iterator.hasNext() && repairForce.size() > repairSize) {
                Workers worker = iterator.next();
                worker.setWorkerStatus(WorkerStatus.IDLE);
                worker.setRepairTarget(null);
                worker.setPreemptiveRepair(false);
                iterator.remove();
            }
            return;
        }
        HashSet<Workers> availableWorkers = (HashSet<Workers>) new HashSet<>(workers).stream()
                .filter(worker -> worker.getWorkerStatus() == WorkerStatus.MINERALS).collect(Collectors.toSet());

        Workers repairWorker = ClosestUnit.findClosestWorker(bunker.getPosition(), availableWorkers, mapInfo.getPathFinding());

        if (repairWorker != null) {
            repairWorker.setWorkerStatus(WorkerStatus.REPAIRING);
            repairWorker.setRepairTarget(bunker);
            repairWorker.setPreemptiveRepair(true);
            repairForce.add(repairWorker);
        }

//        for (Workers worker : workers) {
//            if (worker.getWorkerStatus() == WorkerStatus.MINERALS && repairForce.size() < repairSize
//                    && workers.size() > 8) {
//                worker.setWorkerStatus(WorkerStatus.REPAIRING);
//                worker.setRepairTarget(bunker);
//                worker.setPreemptiveRepair(true);
//                repairForce.add(worker);
//            }
//            else if (repairForce.size() >= repairSize) {
//                break;
//            }
//        }
    }

    private int openerRepairForceSize() {
        if (gameState.getEnemyOpener() == null || new Time(game.getFrameCount()).greaterThan(new Time(8, 0))) {
            return 0;
        }

        switch (gameState.getEnemyOpener().getStrategyName()) {
            case "Two Gate":
                if (new Time(game.getFrameCount()).lessThanOrEqual(new Time(6, 0))) {
                    return 3;
                }
            case "Dark Templar":
                if (new Time(game.getFrameCount()).greaterThan(new Time(5, 0))) {
                    return 3;
                }
                return 0;  
            case "Four Pool":
                return 2;
            default:
                return 0;
        }
    }

    private Unit findNearbyDamagedBuilding(Workers worker, int range) {
        for (Unit building : gameState.getAllBuildings()) {
            if (!building.isCompleted()) {
                continue;
            }
            if (building.getHitPoints() < building.getType().maxHitPoints() && building.getDistance(worker.getUnit()) < range) {
                return building;
            }
        }
        return null;
    }

    private boolean obstructingBuild(Workers worker) {
        for (Workers scv : workers) {
            if (scv == worker) {
                continue;
            }

            if (scv.getWorkerStatus() == WorkerStatus.MOVING_TO_BUILD && scv.getUnit().getPosition().getDistance(worker.getUnit().getPosition()) < 64) {
                return true;
            }
        }
        return false;
    }

    private void moveFromObstruction(Workers worker) {
        Workers buildingWorker = workers.stream()
                .filter(w -> w.getWorkerStatus() == WorkerStatus.MOVING_TO_BUILD && w.getUnit().getPosition().getDistance(worker.getUnit().getPosition()) < 64)
                .findFirst().orElse(null);

        if (buildingWorker == null) {
            return;
        }

        Position workerPos = worker.getUnit().getPosition();
        Position builderPos = buildingWorker.getUnit().getPosition();

        int distance = Math.max(1, builderPos.getApproxDistance(workerPos));

        int moveX = workerPos.getX() + (workerPos.getX() - builderPos.getX() * 100 / distance);
        int moveY = workerPos.getY() + (workerPos.getY() - builderPos.getY() * 100 / distance);
        moveX = Math.min(Math.max(moveX, 0), game.mapWidth() * 32);
        moveY = Math.min(Math.max(moveY, 0), game.mapHeight() * 32);

        Position movePos = new Position(moveX, moveY);
        worker.getUnit().move(movePos);
    }

    private boolean enemyInRange() {
        Unit mainBunker = null;
        for (Unit bunker : player.getUnits()) {
            if (bunker.getType() == UnitType.Terran_Bunker && bunker.isCompleted()) {
                mainBunker = bunker;
                break;
            }
        }

        for (EnemyUnits enemyUnit : gameState.getKnownEnemyUnits()) {
            if (enemyUnit.getEnemyPosition() == null) {
                continue;
            }

            if (enemyUnit.getEnemyType() == UnitType.Zerg_Overlord) {
                continue;
            }

            if (enemyUnit.getEnemyType().isWorker()) {
                continue;
            }

            if (enemyUnit.getEnemyUnit().getType().isFlyer() || !enemyUnit.getEnemyUnit().isDetected()) {
                continue;
            }

            if (!enemyUnit.getEnemyUnit().exists()) {
                continue;
            }

            if (mainBunker == null) {
                return false;
            }

            if (enemyUnit.getEnemyPosition().getApproxDistance(mainBunker.getPosition()) < 400) {
                return true;
            }

        }
        return false;
    }

    private void workerAttackClock(Workers worker) {
        worker.setAttackClock(worker.getAttackClock() + 1);
    }

    private void scoutAttack(Workers worker) {
        if (worker.getEnemyUnit() != null) {
            worker.selfDefense();
        }
        else {
            ClosestUnit.findClosestEnemyUnit(worker, gameState.getKnownEnemyUnits(), 600);
        }
    }

    private boolean gasImbalance() {
        if (gameState.getEnemyOpener() != null) {
            if (gameState.getEnemyOpener().getStrategyName().equals("SCV Rush") && gameState.isEnemyInBase() && workers.size() < 13
                    && gameState.getResourceTracking().getAvailableMinerals() > 300) {
                return true;
            }
        }

        return workers.size() <= 10 && gameState.getResourceTracking().getAvailableGas() > 300 && gameState.getResourceTracking().getAvailableMinerals() < 300;
    }

    private void removeMineralBlockers() {
        if (mapInfo.getBlockingMineralFields().isEmpty()) {
            mineralBlockersCleared = true;

            if (workers.stream().anyMatch(worker -> worker.getWorkerStatus() == WorkerStatus.REMOVINGBLOCKER)) {
                for (Workers worker : workers) {
                    if (worker.getWorkerStatus() == WorkerStatus.REMOVINGBLOCKER) {
                        worker.setWorkerStatus(WorkerStatus.IDLE);
                    }
                }
            }

            return;
        }

        Workers blockRemover = workers.stream()
            .filter(worker -> worker.getWorkerStatus() == WorkerStatus.REMOVINGBLOCKER)
            .findAny().orElse(null);

        if (blockRemover == null) {
            blockRemover = workers.stream()
                .filter(worker -> worker.getWorkerStatus() == WorkerStatus.MINERALS && !worker.getUnit().isCarrying())
                .findAny().orElse(null);

            if (blockRemover != null) {
                removeMineralSaturation(blockRemover);
                blockRemover.setAssignedToBase(false);
                blockRemover.setWorkerStatus(WorkerStatus.REMOVINGBLOCKER);
            }
        }

        if (blockRemover != null && blockRemover.getUnit().isCarrying()) {
            blockRemover.setWorkerStatus(WorkerStatus.IDLE);
            System.out.println("Mineral blockers cleared");
            return;
        }

        Entry<Unit,Position> blocker = mapInfo.getBlockingMineralFields().entrySet().stream()
            .findAny().orElse(null);

        if (blockRemover != null && blocker != null) {
            if (blockRemover.getUnit().getDistance(blocker.getKey()) > 200) {
                blockRemover.getUnit().move(blocker.getValue());
            }
            else {
                if (!blockRemover.getUnit().isGatheringMinerals()) {
                    blockRemover.getUnit().rightClick(blocker.getKey());
                }

            }


        }
    }

    public void onUnitComplete(Unit unit) {
        if (unit.getType() == UnitType.Terran_SCV && workers.stream().noneMatch(w -> w.getUnit() == unit)) {
            workers.add(new Workers(game, unit));
            return;
        }

        if (unit.getType() == UnitType.Terran_Refinery) {
            refinerySaturation.put(unit, new HashSet<>());
            return;
        }

    }

    public void onUnitCreate(Unit unit) {
        if (unit.getType() != UnitType.Terran_Command_Center) {
            return;
        }
        for (Base base : mapInfo.getOwnedBases()) {
            if (unit.getPosition().getApproxDistance(base.getCenter()) < 100) {
                mineralSaturation.put(base, new HashSet<>());
            }
        }
    }

    public void onUnitDestroy(Unit unit) {
        if (unit.getPlayer() != player) {
            return;
        }

        if (unit.getType().isBuilding()) {
            repairTargetDestroyed(unit);
        }

        if (unit.getType() != UnitType.Terran_SCV && unit.getType() != UnitType.Terran_Refinery) {
            return;
        }

        if (unit.getType() == UnitType.Terran_Refinery) {
            for (Workers worker : refinerySaturation.getOrDefault(unit, new HashSet<>())) {
                worker.setWorkerStatus(WorkerStatus.IDLE);
            }
            refinerySaturation.remove(unit);
            return;
        }

        Iterator<Workers> iterator = workers.iterator();
        while (iterator.hasNext()) {
            Workers worker = iterator.next();
            if (worker.getUnit() == unit) {
                for (Unit building : buildingRepair.keySet()) {
                    if (buildingRepair.get(building) == worker) {
                        buildingRepair.put(building, null);
                    }
                }

                repairForce.remove(worker);

                for (Unit geyser : refinerySaturation.keySet()) {
                    refinerySaturation.get(geyser).remove(worker);
                }

                removeMineralSaturation(worker);
                iterator.remove();
                break;
            }
        }
    }

    public HashSet<Workers> getWorkers() {
        return workers;
    }
}
