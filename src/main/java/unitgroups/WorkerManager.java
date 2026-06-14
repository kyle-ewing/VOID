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
import information.GameState;
import information.MapInfo;
import information.enemy.EnemyInformation;
import information.enemy.EnemyScoutResponse;
import information.enemy.EnemyUnits;
import information.enemy.enemyopeners.EnemyStrategyName;
import map.bwemwrappers.Base;
import map.bwemwrappers.Mineral;
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
    private HashSet<Workers> pulledScvs = new HashSet<>();
    private HashMap<Unit, HashSet<Workers>> refinerySaturation = new HashMap<>();
    private HashMap<Base, HashSet<Workers>> mineralSaturation = new HashMap<>();
    private HashMap<Unit, Workers> buildingRepair = new HashMap<>();
    private boolean openerResponse = false;
    private boolean initialMineralAssignmentDone = false;
    private boolean mineralBlockersCleared = false;
    private boolean scvsPulled = false;
    private int pullFrame = 0;

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
        releasePulledScvs();
        clearNaturalDefenseForce();
        enemyScoutResponse.onFrame();
        transferDepletedBaseWorkers();
        updateDepletingBaseFlag();

        int frameCount = game.getFrameCount();

        if (gameState.getEnemyOpener() != null && new Time(frameCount).lessThanOrEqual(new Time(4,30))) {
            enemyStrategyResponse();
            openerResponse = true;
        }

        if ((gameState.hasTransitioned() || new Time(game.getFrameCount()).greaterThan(new Time(8, 0))) && !mineralBlockersCleared) {
            removeMineralBlockers();
        }

        for (Workers worker : workers) {
            if (new Time(frameCount).lessThanOrEqual(new Time(6,0))) {
                if (worker.getUnit().isUnderAttack() && (worker.getWorkerStatus() != WorkerStatus.SCOUTING || worker.getWorkerStatus() != WorkerStatus.COUNTERSCOUT)) {
                    //Stop worker defense after the early game
                    if (mapInfo.getBaseTiles().contains(worker.getUnit().getTilePosition()) && actuallyThreatened() && !hasCompletedCannonInBase() && !hasBunkerInMain()) {
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
                        && gameState.getEnemyOpener().getStrategyName() == EnemyStrategyName.GASSTEAL
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
                            removeMineralSaturation(worker);
                            worker.setAssignedToBase(false);
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
                    ClosestUnit.findClosestUnit(worker, gameState.getKnownEnemyUnits(), 1100);
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
                case ATTACKING:
                    if (frameCount % 8 != 0) {
                        break;
                    }

                    Base enemyNatural = mapInfo.getEnemyRushTargetBase(gameState.getKnownEnemyUnits());
                    HashSet<TilePosition> enemyNaturalTiles = null;
                    if (enemyNatural != null) {
                        enemyNaturalTiles = mapInfo.getBaseTilesAllBases().get(enemyNatural);
                    }

                    Unit naturalBunker = null;
                    if (enemyNaturalTiles != null) {
                        for (Unit building : gameState.getAllBuildings()) {
                            if (building.getType() == UnitType.Terran_Bunker
                                    && enemyNaturalTiles.contains(building.getTilePosition())) {
                                naturalBunker = building;
                                break;
                            }
                        }
                    }

                    Unit attackDamaged = null;
                    if (pulledScvs.contains(worker) && naturalBunker != null
                            && naturalBunker.getHitPoints() < naturalBunker.getType().maxHitPoints()) {
                        attackDamaged = naturalBunker;
                    }
                    else {
                        attackDamaged = findNearbyDamagedBuilding(worker, 300);
                    }

                    if (attackDamaged != null) {
                        worker.repair(attackDamaged);
                        break;
                    }

                    if (naturalBunker != null) {
                        if (worker.getUnit().getDistance(naturalBunker.getPosition()) > 150) {
                            worker.getUnit().move(naturalBunker.getPosition());
                            break;
                        }
                    }

                    HashSet<EnemyUnits> attackCandidates = gameState.getKnownEnemyUnits();
                    if (gameState.getEnemyScout() != null) {
                        attackCandidates = new HashSet<>(attackCandidates);
                        attackCandidates.remove(gameState.getEnemyScout());
                    }
                    ClosestUnit.findClosestUnit(worker, attackCandidates, 160);
                    if (worker.getEnemyUnit() != null) {
                        worker.selfDefense();
                        break;
                    }

                    if (enemyNatural != null) {
                        Position enemyNaturalPos = enemyNatural.getCenter();
                        if (worker.getUnit().getDistance(enemyNaturalPos) > 64) {
                            worker.getUnit().move(enemyNaturalPos);
                        }
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

                    if (frameCount % 24 != 0) {
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
                scv = ClosestUnit.findClosestWorker(geyser.getPosition(), workers, mapInfo.getPathFinding(), false);

                if (scv == null) {
                    continue;
                }

                if (scv.getWorkerStatus() == WorkerStatus.ATTACKING) {
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
        Base closestBase = null;
        double closestDistance = Double.MAX_VALUE;

        for (Base base : mapInfo.getOwnedBases()) {
            if (mapInfo.getDepletedBases().contains(base)) {
                continue;
            }
            int capacity = 24;
            if (mapInfo.getHalfTransferredBases().contains(base)) {
                capacity = 12;
            }
            HashSet<Workers> saturation = mineralSaturation.computeIfAbsent(base, k -> new HashSet<>());
            if (saturation.size() >= capacity) {
                continue;
            }
            double distance = worker.getUnit().getDistance(base.getCenter());
            if (distance < closestDistance) {
                closestDistance = distance;
                closestBase = base;
            }
        }

        if (closestBase != null) {
            mineralSaturation.get(closestBase).add(worker);
            worker.setAssignedToBase(true);
            if (!closestBase.getMinerals().isEmpty()) {
                worker.getUnit().gather(closestBase.getMinerals().get(0).getUnit());
            }
            return;
        }

        Base nextExpansion = mapInfo.scoredBestExpansion(gameState.getStartingOpener().getBuildOrderName(), gameState.getKnownEnemyUnits());
        if (nextExpansion != null && !nextExpansion.getMinerals().isEmpty()) {
            mineralSaturation.computeIfAbsent(nextExpansion, k -> new HashSet<>()).add(worker);
            worker.setAssignedToBase(true);
            worker.getUnit().gather(nextExpansion.getMinerals().get(0).getUnit());
        }
    }

    private int baseMineralResources(Base base) {
        int total = 0;
        for (Mineral patch : mapInfo.getBasePatches(base)) {
            total += patch.getResources();
        }
        return total;
    }

    private int basePatchCount(Base base) {
        int count = 0;
        for (Mineral patch : mapInfo.getBasePatches(base)) {
            if (patch.getResources() > 0) {
                count++;
            }
        }
        return count;
    }

    private void transferDepletedBaseWorkers() {
        for (Base base : mineralSaturation.keySet()) {
            if (mapInfo.getDepletedBases().contains(base)) {
                continue;
            }

            HashSet<Workers> assigned = mineralSaturation.get(base);

            if (baseMineralResources(base) == 0) {
                mapInfo.getDepletedBases().add(base);
                for (Workers worker : new HashSet<>(assigned)) {
                    if (worker.getWorkerStatus() == WorkerStatus.GAS) {
                        continue;
                    }
                    assigned.remove(worker);
                    worker.setAssignedToBase(false);
                    worker.setWorkerStatus(WorkerStatus.IDLE);
                }
                continue;
            }

            if (mapInfo.getHalfTransferredBases().contains(base)) {
                continue;
            }

            Integer originalPatches = mapInfo.getOriginalPatchCounts().get(base);
            if (originalPatches == null || originalPatches == 0) {
                continue;
            }

            if (basePatchCount(base) * 2 > originalPatches) {
                continue;
            }

            mapInfo.getHalfTransferredBases().add(base);

            int mineralWorkerCount = 0;
            for (Workers w : assigned) {
                if (w.getWorkerStatus() != WorkerStatus.GAS) {
                    mineralWorkerCount++;
                }
            }

            int toTransfer = mineralWorkerCount / 2;
            int transferred = 0;

            for (Workers worker : new HashSet<>(assigned)) {
                if (transferred >= toTransfer) {
                    break;
                }
                if (worker.getWorkerStatus() == WorkerStatus.GAS) {
                    continue;
                }
                assigned.remove(worker);
                worker.setAssignedToBase(false);
                worker.setWorkerStatus(WorkerStatus.IDLE);
                transferred++;
            }
        }
    }

    private void updateDepletingBaseFlag() {
        HashMap<Base, Integer> originalCounts = mapInfo.getOriginalMineralCounts();
        for (Base base : mapInfo.getOwnedBases()) {
            if (mapInfo.getDepletedBases().contains(base)) {
                continue;
            }
            if (mapInfo.getDepletionCountedBases().contains(base)) {
                continue;
            }
            Integer original = originalCounts.get(base);
            if (original == null || original == 0) {
                continue;
            }
            int current = baseMineralResources(base);
            if (current > 0 && current * 3 <= original) {
                mapInfo.getDepletionCountedBases().add(base);
                gameState.setHasDepletingBase(true);
                return;
            }
        }
        gameState.setHasDepletingBase(false);
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
                worker.setNearTargetFrameCount(0);
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

    private void createPulledScvs(int count) {
        if (scvsPulled) {
            return;
        }

        if (pulledScvs.isEmpty()) {
            pullFrame = game.getFrameCount();
        }

        for (Workers worker : workers) {
            if (worker.getWorkerStatus() == WorkerStatus.MINERALS && pulledScvs.size() < count) {
                pulledScvs.add(worker);
                removeMineralSaturation(worker);
                worker.setAssignedToBase(false);
                worker.setWorkerStatus(WorkerStatus.ATTACKING);
            }

            if (pulledScvs.size() >= count) {
                scvsPulled = true;
                return;
            }
        }
    }

    private void releasePulledScvs() {
        if (pulledScvs.isEmpty()) {
            return;
        }

        boolean rushGate = new Time(game.getFrameCount() - pullFrame).lessThanOrEqual(new Time(2, 0))
                && gameState.getSelectedPivot() != null && gameState.getSelectedPivot().isRushActive();

        Iterator<Workers> iterator = pulledScvs.iterator();
        while (iterator.hasNext()) {
            Workers worker = iterator.next();

            if (worker.getWorkerStatus() != WorkerStatus.ATTACKING) {
                iterator.remove();
                continue;
            }

            if (worker.getUnit().getHitPoints() < 10) {
                worker.setWorkerStatus(WorkerStatus.IDLE);
                iterator.remove();
                continue;
            }

            if (rushGate) {
                continue;
            }

            boolean enemyNearby = false;
            for (EnemyUnits enemyUnit : gameState.getKnownEnemyUnits()) {
                if (enemyUnit.getEnemyPosition() == null) {
                    continue;
                }
                if (enemyUnit.getEnemyUnit().getDistance(worker.getUnit()) < 600) {
                    enemyNearby = true;
                    break;
                }
            }

            if (enemyNearby) {
                continue;
            }

            if (findNearbyDamagedBuilding(worker, 300) != null) {
                continue;
            }

            worker.setWorkerStatus(WorkerStatus.IDLE);
            iterator.remove();
        }
    }

    private boolean hasBunker() {
        for (Unit building : gameState.getAllBuildings()) {
            if (building.getType() == UnitType.Terran_Bunker && building.isCompleted()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasBunkerInMain() {
        for (Unit building : gameState.getAllBuildings()) {
            if (building.getType() == UnitType.Terran_Bunker && building.isCompleted()
                    && mapInfo.getBaseTiles().contains(building.getTilePosition())) {
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
                worker.setAssignedToBase(false);
                worker.setAttackClock(0);
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
            case CANNONRUSH:
                if (!hasCompletedCannonInBase() && !hasBunkerInMain() && enemyInBase()) {
                    if (workers.size() > 12) {
                        createDefenseForce(6);
                    }
                    else {
                        createDefenseForce(3);
                    }
                }
                break;
            case FOURRAX:
                if (gameState.getKnownEnemyUnits().stream().anyMatch(unit -> unit.getEnemyType() == UnitType.Terran_Marine && unit.getEnemyPosition() != null
                        && mapInfo.getBaseTiles().contains(unit.getEnemyPosition().toTilePosition()))) {
                    createDefenseForce(3);
                }
                break;
            case SCVRUSH:
                if (gameState.isEnemyInBase()) {
                    createDefenseForce(3);
                }
                break;
            case BUNKERRUSH:
                if (gameState.isEnemyInNatural()) {
                    createDefenseForce(3);
                }
                break;
            case NEXUSFIRST:
                createPulledScvs(6);
                break;
            default:
                break;
        }
    }

    private void buildingHealthCheck() {
        for (Unit building : player.getUnits()) {
            if (building.getType().isBuilding() && building.getHitPoints() < building.getType().maxHitPoints() && building.isCompleted()) {
                if (building.getType() == UnitType.Terran_Bunker
                        && !mapInfo.getBaseTiles().contains(building.getTilePosition())
                        && !mapInfo.getNaturalTiles().contains(building.getTilePosition())) {
                    continue;
                }

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
        if (!pulledScvs.isEmpty()) {
            return;
        }

        Unit bunker = null;

        for (Unit unit : gameState.getAllBuildings()) {
            if (unit.getType() != UnitType.Terran_Bunker) {
                continue;
            }

            if (mapInfo.hasBunkerInNatural() && mapInfo.getNaturalTiles().contains(unit.getTilePosition())) {
                bunker = unit;
                break;
            }
            else if (mapInfo.getBaseTiles().contains(unit.getTilePosition())) {
                bunker = unit;
            }
        }

        if (bunker == null) {
            return;
        }

        if (bunker.getLoadedUnits().isEmpty()) {
            if (!enemyInRange(400)) {
                for (Workers worker : repairForce) {
                    worker.setWorkerStatus(WorkerStatus.IDLE);
                    worker.setRepairTarget(null);
                    worker.setPreemptiveRepair(false);
                }
                repairForce.clear();
            }
            return;
        }

        if (bunker.isCompleted()) {
            Position baseCenter = mapInfo.getStartingBase().getCenter();
            if (gameState.isNaturalCCCompleted()) {
                baseCenter = mapInfo.getNaturalBase().getCenter();
            }

            Integer openerSize = openerRepairForceSize(bunker);

            if (openerSize != null) {
                createRepairForce(bunker, openerSize);
            }
            else if (enemyInRange(200) && enemyInformation.getEnemySupplyInRange(bunker) >= 10) {
                createRepairForce(bunker, 6);
            }
            else if (enemyInRange(400) && enemyInformation.getEnemySupplyInRange(bunker) >= 6) {
                createRepairForce(bunker, 4);
            } 
            else if (bunker.getDistance(baseCenter) > 750
                    && new Time(game.getFrameCount()).greaterThan(new Time(5, 0))
                    && new Time(game.getFrameCount()).lessThanOrEqual(new Time(8, 0))) {
                if (enemyInformation.getNonWorkerEnemySupply() >= 8) {
                    createRepairForce(bunker, 5);
                }
                else {
                    createRepairForce(bunker, 4);
                }
            }
            else if (bunker.getDistance(baseCenter) > 550
                    && new Time(game.getFrameCount()).greaterThan(new Time(5, 0))
                    && new Time(game.getFrameCount()).lessThanOrEqual(new Time(8,0))) {
                createRepairForce(bunker, 2);
            }
            else if (bunker.getDistance(baseCenter) > 400
                    && new Time(game.getFrameCount()).greaterThan(new Time(4,20))
                    && new Time(game.getFrameCount()).lessThanOrEqual(new Time(8,0))) {
                createRepairForce(bunker, 1);
            }
            else if (!enemyInRange(400)) {
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

        Workers repairWorker = ClosestUnit.findClosestWorker(bunker.getPosition(), availableWorkers, mapInfo.getPathFinding(), false);

        if (repairWorker != null) {
            removeMineralSaturation(repairWorker);
            repairWorker.setAssignedToBase(false);
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

    private Integer openerRepairForceSize(Unit bunker) {
        if (gameState.getEnemyOpener() == null || new Time(game.getFrameCount()).greaterThan(new Time(8, 0))) {
            return null;
        }

        switch (gameState.getEnemyOpener().getStrategyName()) {
            case TWOGATE:
                if (new Time(game.getFrameCount()).greaterThan(new Time(3, 10)) && gameState.getKnownEnemyUnits().stream()
                        .anyMatch(unit -> unit.getEnemyType() == UnitType.Protoss_Zealot && unit.getEnemyPosition() != null
                        && unit.getEnemyUnit().getDistance(bunker) < 500)) {
                    return 3;
                }

                if (new Time(game.getFrameCount()).lessThanOrEqual(new Time(6, 0))) {
                    return 3;
                }
            case DTRUSH:
                if (new Time(game.getFrameCount()).greaterThan(new Time(5, 0))) {
                    return 3;
                }
                return 0;
            case FOURPOOL:
                return 2;
            case SHUTTLERUSH:
                return 0;
            default:
                return null;
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

    private boolean enemyInRange(int range) {
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

            if (enemyUnit.getEnemyPosition().getApproxDistance(mainBunker.getPosition()) < range) {
                return true;
            }

        }
        return false;
    }

    private void workerAttackClock(Workers worker) {
        worker.setAttackClock(worker.getAttackClock() + 1);
    }

    private void scoutAttack(Workers worker) {
        if (worker.getEnemyUnit() != null && worker.getEnemyUnit().getEnemyType().isWorker() && worker.getEnemyUnit().getEnemyUnit().exists()) {
            worker.selfDefense();
            return;
        }

        HashSet<EnemyUnits> enemyWorkers = new HashSet<>();
        for (EnemyUnits enemy : gameState.getKnownEnemyUnits()) {
            if (enemy.getEnemyType().isWorker()) {
                enemyWorkers.add(enemy);
            }
        }

        worker.setEnemyUnit(ClosestUnit.findClosestEnemyUnit(worker, enemyWorkers, 600));
    }

    private boolean gasImbalance() {
        if (gameState.getEnemyOpener() != null) {
            if (gameState.getEnemyOpener().getStrategyName() == EnemyStrategyName.SCVRUSH && gameState.isEnemyInBase() && workers.size() < 13
                    && gameState.getResourceTracking().getAvailableMinerals() > 300) {
                return true;
            }
        }

        if (workers.size() < 60 && gameState.getResourceTracking().getAvailableGas() > 1800) {
            return true;
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
                mineralSaturation.computeIfAbsent(base, k -> new HashSet<>());
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
                pulledScvs.remove(worker);

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
