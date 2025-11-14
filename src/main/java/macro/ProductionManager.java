package macro;

import bwapi.*;
import bwem.BWEM;
import bwem.Base;
import debug.Painters;
import information.BaseInfo;
import information.GameState;
import information.enemy.EnemyInformation;
import information.enemy.enemytechunits.EnemyTechUnits;
import macro.buildorders.*;
import macro.unitgroups.WorkerStatus;
import macro.unitgroups.Workers;
import map.BuildTiles;
import map.TilePositionValidator;
import planner.BuildComparator;
import planner.PlannedItem;
import planner.PlannedItemStatus;
import planner.PlannedItemType;

import java.util.*;

public class ProductionManager {
    private Game game;
    private GameState gameState;
    private Player player;
    private Race enemyRace;
    private ResourceManager resourceManager;
    private BaseInfo baseInfo;
    private Painters painters;
    private TilePositionValidator tilePositionValidator;
    private BuildTiles buildTiles;
    private HashMap<UnitType, Integer> unitTypeCount = new HashMap<>();
    private HashSet<Unit> productionBuildings = new HashSet<>();
    private HashSet<Unit> allBuildings = new HashSet<>();
    private HashSet<TilePosition> reservedTurretPositions = new HashSet<>();
    private ArrayList<BuildOrder> openerNames = new ArrayList<>();
    private PriorityQueue<PlannedItem> productionQueue = new PriorityQueue<>(new BuildComparator());
    private BuildOrder startingOpener;
    private Unit newestCompletedBuilding = null;
    private TilePosition bunkerPosition = null;
    private boolean openerResponse = false;
    private boolean priorityStop = false;



    public ProductionManager(Game game, Player player, ResourceManager resourceManager, BaseInfo baseInfo, GameState gameState) {
        this.game = game;
        this.player = player;
        this.resourceManager = resourceManager;
        this.baseInfo = baseInfo;
        this.gameState = gameState;

        tilePositionValidator = new TilePositionValidator(game);
        buildTiles = new BuildTiles(game, baseInfo);

        painters = new Painters(game);

        initialize();
    }

    public void initialize() {
        enemyRace = game.enemy().getRace();
        getOpenerNames();
        appendBuildOrder(enemyRace);
        initUnitCounts();
    }

    //TODO: move to it's own class
    private PriorityQueue<PlannedItem> appendBuildOrder(Race enemyRace) {
        if(enemyRace.toString().equals("Zerg")) {
            for(BuildOrder buildOrder : openerNames) {
                if(buildOrder.getBuildOrderName() == BuildOrderName.TWORAXACADEMY) {
                    bunkerPosition = buildTiles.getMainChokeBunker();
                    productionQueue.addAll(buildOrder.getBuildOrder());
                    startingOpener = buildOrder;
                }
            }
        }
        else if(enemyRace.toString().equals("Terran")) {
            for(BuildOrder buildOrder : openerNames) {
                if(buildOrder.getBuildOrderName() == BuildOrderName.TWORAXACADEMY) {
                    bunkerPosition = buildTiles.getMainChokeBunker();
                    productionQueue.addAll(buildOrder.getBuildOrder());
                    startingOpener = buildOrder;
                }
            }
        }
        else if(enemyRace.toString().equals("Protoss")) {
            for(BuildOrder buildOrder : openerNames) {
                if(buildOrder.getBuildOrderName() == BuildOrderName.TWORAXACADEMY) {
                    bunkerPosition = buildTiles.getMainChokeBunker();
                    productionQueue.addAll(buildOrder.getBuildOrder());
                    startingOpener = buildOrder;
                }
            }
        }
        else {
            for(BuildOrder buildOrder : openerNames) {
                if(buildOrder.getBuildOrderName() == BuildOrderName.TWORAXACADEMY) {
                    bunkerPosition = buildTiles.getMainChokeBunker();
                    productionQueue.addAll(buildOrder.getBuildOrder());
                    startingOpener = buildOrder;
                }
            }
        }
        return productionQueue;
    }

    private void buildingProduction() {
        boolean hasHighPriorityBuilding = hasHigherPriorityBuilding();
        Workers worker = null;

        for (PlannedItem pi : productionQueue) {
            if(pi.getPriority() == 1 && pi.getPlannedItemStatus() == PlannedItemStatus.NOT_STARTED && pi.getPlannedItemType() == PlannedItemType.BUILDING && meetsRequirements(pi.getUnitType()) && pi.getSupply() <= player.supplyUsed() / 2) {
                priorityStop = true;
            }

            //Throttle gas units but allow mineral only if enough is banked (Vessels very gas heavy and only unit worth prioritizing)
            if(pi.getPriority() == 1 && pi.getPlannedItemStatus() == PlannedItemStatus.NOT_STARTED
                    && pi.getPlannedItemType() == PlannedItemType.UNIT && meetsRequirements(pi.getUnitType())
                    && pi.getSupply() > player.supplyUsed() / 2) {
                if(resourceManager.getAvailableMinerals() >= pi.getUnitType().mineralPrice() && resourceManager.getAvailableGas() < pi.getUnitType().gasPrice()) {
                    priorityStop = true;
                }
            }

            if(priorityStop && pi.getPriority() != 1 && pi.getPlannedItemType() == PlannedItemType.BUILDING && pi.getPlannedItemStatus() == PlannedItemStatus.NOT_STARTED) {
                continue;
            }

            if (hasHighPriorityBuilding && pi.getPlannedItemType() == PlannedItemType.UNIT && pi.getPlannedItemStatus() == PlannedItemStatus.NOT_STARTED) {
                continue;
            }

            switch (pi.getPlannedItemStatus()) {
                case NOT_STARTED:
                    if(pi.getSupply() <= player.supplyUsed() / 2) {

                        if(pi.getPlannedItemType() == PlannedItemType.UPGRADE) {
                            if(!canBeResearched(pi.getTechBuilding()) || !isResearching(pi.getTechBuilding())) {
                                continue;
                            }

                            if(pi.getTechUpgrade() != null) {
                                if(resourceManager.getAvailableMinerals() < pi.getTechUpgrade().mineralPrice() || resourceManager.getAvailableGas() < pi.getTechUpgrade().gasPrice()) {
                                    continue;
                                }
                                researchTech(pi.getTechUpgrade());
                                pi.setPlannedItemStatus(PlannedItemStatus.IN_PROGRESS);
                            }
                            else if(pi.getUpgradeType() != null) {
                                if(resourceManager.getAvailableMinerals() < pi.getUpgradeType().mineralPrice() || resourceManager.getAvailableGas() < pi.getUpgradeType().gasPrice()) {
                                    continue;
                                }
                                researchUpgrade(pi.getUpgradeType());
                                pi.setPlannedItemStatus(PlannedItemStatus.IN_PROGRESS);
                            }
                            continue;
                        }

                        if(resourceManager.getAvailableMinerals() >= pi.getUnitType().mineralPrice() && resourceManager.getAvailableGas() >= pi.getUnitType().gasPrice()) {

                            if(pi.getPlannedItemType() == PlannedItemType.UNIT) {
                                for(Unit productionBuilding : productionBuildings) {
                                    if(productionBuilding.canTrain(pi.getUnitType()) && !productionBuilding.isTraining()) {
                                        productionBuilding.train(pi.getUnitType());
                                        pi.setProductionBuilding(productionBuilding);
                                        pi.setPlannedItemStatus(PlannedItemStatus.IN_PROGRESS);
                                        break;
                                    }
                                }
                            }
                            else if(pi.getPlannedItemType() == PlannedItemType.BUILDING) {
                                if(pi.getBuildPosition() == null) {
                                    if (pi.getUnitType() == UnitType.Terran_Refinery) {
                                        setRefineryPosition(pi);
                                    }
                                    else if (pi.getUnitType() == UnitType.Terran_Command_Center) {
                                        setCommandCenterPosition(pi);
                                    }
                                    else {
                                        setBuildingPosition(pi);
                                    }

                                    //Skip over if out of tiles
                                    if(pi.getBuildPosition() == null) {
                                        continue;
                                    }
                                }

                                if(pi.getBuildPosition() != null) {
                                    worker = resourceManager.getClosestWorker(pi.getBuildPosition().toPosition());
                                    pi.setAssignedBuilder(worker);
                                }

                                if(pi.getAssignedBuilder() != null) {
                                    if(worker.getWorkerStatus() == WorkerStatus.MINERALS && worker.getUnit().canBuild(pi.getUnitType())) {
                                        resourceManager.reserveResources(pi.getUnitType());
                                        worker.setBuildingPosition(pi.getBuildPosition().toPosition());
                                        worker.getUnit().move(pi.getBuildPosition().toPosition());
                                        worker.getUnit().build(pi.getUnitType(), pi.getBuildPosition());
                                        pi.setPlannedItemStatus(PlannedItemStatus.SCV_ASSIGNED);

                                        worker.setWorkerStatus(WorkerStatus.MOVING_TO_BUILD);
                                    }
                                }
                            }
                            else if(pi.getPlannedItemType() == PlannedItemType.ADDON) {
                                for(Unit productionBuilding : productionBuildings) {
                                    if(productionBuilding.canBuildAddon(pi.getUnitType()) && !productionBuilding.isTraining() && productionBuilding.getAddon() == null) {
                                        if(productionQueue.stream().anyMatch(plannedItem -> plannedItem.getAddOnParent() == productionBuilding)) {
                                            continue;
                                        }

                                        productionBuilding.buildAddon(pi.getUnitType());
                                        pi.setAddOnParent(productionBuilding);
                                        pi.setPlannedItemStatus(PlannedItemStatus.IN_PROGRESS);
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    //check requirements again in case tiles run out before building starts
                    if((pi.getPriority() == 1 && pi.getPlannedItemStatus() != PlannedItemStatus.NOT_STARTED) || (pi.getPlannedItemType() == PlannedItemType.BUILDING && !meetsRequirements(pi.getUnitType()))) {
                        priorityStop = false;
                    }

                    break;

                case SCV_ASSIGNED:
                    worker = pi.getAssignedBuilder();
                    if(worker == pi.getAssignedBuilder() && worker.getWorkerStatus() == WorkerStatus.MOVING_TO_BUILD) {

                        worker.pulseCheck();

                        if(worker.getUnit().getDistance(pi.getBuildPosition().toPosition()) < 96) {
                            worker.getUnit().build(pi.getUnitType(), pi.getBuildPosition());
                        }



                        if(worker.getBuildFrameCount() > 600) {
                            worker.setWorkerStatus(WorkerStatus.IDLE);
                            worker.getUnit().stop();
                            resourceManager.unreserveResources(pi.getUnitType());
                            pi.setPlannedItemStatus(PlannedItemStatus.NOT_STARTED);
                        }
                    }

                    if(worker.getWorkerStatus() == WorkerStatus.STUCK) {
                        resourceManager.unreserveResources(pi.getUnitType());
                        pi.setPlannedItemStatus(PlannedItemStatus.NOT_STARTED);
                    }

                    if(!worker.getUnit().exists()) {
                        pi.setPlannedItemStatus(PlannedItemStatus.NOT_STARTED);
                    }

                    if(buildingInProduction(pi.getBuildPosition(), pi.getUnitType())) {
                        resourceManager.unreserveResources(pi.getUnitType());
                        pi.setPlannedItemStatus(PlannedItemStatus.IN_PROGRESS);

                        if(worker.getUnitID() == pi.getAssignedBuilder().getUnitID() ) {
                            worker.setWorkerStatus(WorkerStatus.BUILDING);
                        }
                    }
                    break;

                case IN_PROGRESS:
                    if(pi.getPlannedItemType() == PlannedItemType.BUILDING) {
                        if(pi.getAssignedBuilder() != null) {
                            worker = pi.getAssignedBuilder();
                        }

                        for(Unit building : allBuildings) {
                            if(building.getType() == pi.getUnitType() && building.getTilePosition().equals(pi.getBuildPosition()) && building.isCompleted()) {
                                pi.setPlannedItemStatus(PlannedItemStatus.COMPLETE);

                                if (worker.getWorkerStatus() == WorkerStatus.BUILDING) {
                                    worker.setWorkerStatus(WorkerStatus.IDLE);
                                }

                                break;
                            }
                        }

                        boolean builderHasDied = true;
                        for(Workers workers : resourceManager.getWorkers()) {
                            if(workers == pi.getAssignedBuilder()) {
                                builderHasDied = false;
                                break;
                            }
                        }

                        if(builderHasDied) {
                            for(Workers newWorker : resourceManager.getWorkers()) {
                                if(newWorker.getWorkerStatus() == WorkerStatus.MINERALS) {
                                    pi.setAssignedBuilder(newWorker);
                                    newWorker.setWorkerStatus(WorkerStatus.MOVING_TO_BUILD);
                                    break;
                                }
                            }
                        }

                        for(Unit building : player.getUnits()) {
                            if(!building.isCompleted() && building.getType() == pi.getUnitType() && !building.isBeingConstructed()) {
                                pi.getAssignedBuilder().getUnit().rightClick(building);
                                break;
                            }

                            if(building.isBeingConstructed() && pi.getAssignedBuilder().getUnit().isConstructing()) {
                                if(worker.getUnit().getID() == pi.getAssignedBuilder().getUnitID()) {
                                    worker.setWorkerStatus(WorkerStatus.BUILDING);
                                    break;
                                }

                            }
                        }
                    }

                    if(pi.getPlannedItemType() == PlannedItemType.UPGRADE) {
                        if(pi.getUpgradeType() != null) {
                            if(game.self().getUpgradeLevel(pi.getUpgradeType()) == pi.getUpgradeLevel()) {
                                pi.setPlannedItemStatus(PlannedItemStatus.COMPLETE);
                            }

                            if(!isUpgrading(pi.getUpgradeType()) && game.self().getUpgradeLevel(pi.getUpgradeType()) < pi.getUpgradeLevel()) {
                                pi.setPlannedItemStatus(PlannedItemStatus.NOT_STARTED);
                            }
                        }
                        else if(pi.getTechUpgrade() != null) {
                            if(game.self().hasResearched(pi.getTechUpgrade())) {
                                pi.setPlannedItemStatus(PlannedItemStatus.COMPLETE);
                            }

                            if(!isResearching(pi.getTechUpgrade()) && !game.self().hasResearched(pi.getTechUpgrade())) {
                                pi.setPlannedItemStatus(PlannedItemStatus.NOT_STARTED);
                            }
                        }
                    }

                    if(pi.getPlannedItemType() == PlannedItemType.ADDON) {
                        if(pi.getAddOnParent() == null || pi.getAddOnParent().getAddon() == null) {
                            continue;
                        }

                        if(pi.getAddOnParent() != null && pi.getAddOnParent().getAddon().isCompleted()) {
                            pi.setPlannedItemStatus(PlannedItemStatus.COMPLETE);
                            break;
                        }
                    }

                    for(Unit productionBuilding : productionBuildings) {
                        if(pi.getProductionBuilding() == productionBuilding && !productionBuilding.isTraining()) {
                            pi.setPlannedItemStatus(PlannedItemStatus.COMPLETE);
                            break;
                        }
                    }
                    break;

                case COMPLETE:
                    break;
            }
        }

        productionQueue.removeIf(pi -> pi.getPlannedItemStatus() == PlannedItemStatus.COMPLETE);
    }

    private void addToQueue(UnitType unitType, PlannedItemType plannedItemType, int priority) {
        productionQueue.add(new PlannedItem(unitType, 0, PlannedItemStatus.NOT_STARTED, plannedItemType, priority));
    }

    private void addAddOn(UnitType unitType, int priority) {
        productionQueue.add(new PlannedItem(unitType, 0, PlannedItemStatus.NOT_STARTED, PlannedItemType.ADDON, priority));
    }

    private boolean hasHigherPriorityBuilding() {
        for (PlannedItem pi : productionQueue) {
            if ((pi.getPlannedItemType() == PlannedItemType.BUILDING) && pi.getPlannedItemStatus() == PlannedItemStatus.NOT_STARTED && pi.getSupply() <= player.supplyUsed() / 2 && meetsRequirements(pi.getUnitType()) && pi.getPriority() < 3) {
                return true;
            }
            else if(pi.getPlannedItemType() == PlannedItemType.UPGRADE && pi.getPlannedItemStatus() == PlannedItemStatus.NOT_STARTED && pi.getSupply() <= player.supplyUsed() / 2 && isResearching(pi.getTechBuilding())) {
                return true;
            }
        }
        return false;
    }


    private void addUnitProduction() {


        switch(startingOpener.getBuildOrderName()) {
            case EIGHTRAX:
                for(Unit productionBuilding : productionBuildings) {
                    if(productionBuilding.getType() == UnitType.Terran_Barracks && !productionBuilding.isTraining() && resourceManager.getAvailableMinerals() >= 50) {
                        productionBuilding.train(UnitType.Terran_Marine);
                    }
                }
                break;
            case TWORAX:
                for(Unit productionBuilding : productionBuildings) {
                    if(productionBuilding.getType() == UnitType.Terran_Barracks && !productionBuilding.isTraining() && resourceManager.getAvailableMinerals() >= 50) {
                        productionBuilding.train(UnitType.Terran_Marine);
                    }
                }
                break;
            case TWORAXACADEMY:
                for(Unit productionBuilding : productionBuildings) {
                    if(!gameState.getTechUnitResponse().isEmpty()) {
                        for(UnitType unitType: gameState.getTechUnitResponse()) {
                            if(isCurrentlyTraining(productionBuilding, unitType.whatBuilds().getKey())) {
                                if(isRecruitable(UnitType.Terran_Science_Vessel, 1) && !hasUnitInQueue(unitType)) {
                                    if(unitType == UnitType.Terran_Science_Vessel) {
                                        if(unitTypeCount.get(unitType) < 2) {
                                            addToQueue(unitType, PlannedItemType.UNIT, 1);
                                        }
                                    }
                                    else {
                                        addToQueue(unitType, PlannedItemType.UNIT, 2);
                                    }
                                }
                            }
                        }
                    }

                    if (isCurrentlyTraining(productionBuilding, UnitType.Terran_Barracks)) {
                        if(productionBuilding.canTrain(UnitType.Terran_Medic) && isRecruitable(UnitType.Terran_Medic) && unitTypeCount.get(UnitType.Terran_Medic) < 3 && !hasUnitInQueue(UnitType.Terran_Medic)) {
                                addToQueue(UnitType.Terran_Medic, PlannedItemType.UNIT,2);
                        }
                        else if(isRecruitable(UnitType.Terran_Marine) && !hasUnitInQueue(UnitType.Terran_Marine)) {
                            addToQueue(UnitType.Terran_Marine, PlannedItemType.UNIT,3);
                        }
                    }

                    if(isCurrentlyTraining(productionBuilding, UnitType.Terran_Factory)) {
                        if (isRecruitable(UnitType.Terran_Siege_Tank_Tank_Mode) && unitTypeCount.get(UnitType.Terran_Siege_Tank_Tank_Mode) < 7 && !hasUnitInQueue(UnitType.Terran_Siege_Tank_Tank_Mode)) {
                            addToQueue(UnitType.Terran_Siege_Tank_Tank_Mode, PlannedItemType.UNIT, 2);
                        }
                    }

                }
                //TODO: remove when transitions are added
                if(resourceManager.getAvailableMinerals() > 500 && !buildTiles.getLargeBuildTiles().isEmpty() && unitTypeCount.get(UnitType.Terran_Barracks) < 6 && !hasUnitInQueue(UnitType.Terran_Barracks)) {
                    addToQueue(UnitType.Terran_Barracks, PlannedItemType.BUILDING, 3);
                }
                break;
            case ONERAXFE:
                for(Unit productionBuilding : productionBuildings) {
                    if(isCurrentlyTraining(productionBuilding, UnitType.Terran_Barracks)) {
                        if (isRecruitable(UnitType.Terran_Marine)) {
                            addToQueue(UnitType.Terran_Marine, PlannedItemType.UNIT, 3);
                        }
                    }
                }
                break;
            case TWOFAC:
                for(Unit productionBuilding : productionBuildings) {
                    if(!gameState.getTechUnitResponse().isEmpty()) {
                        for(UnitType unitType: gameState.getTechUnitResponse()) {
                            if(isCurrentlyTraining(productionBuilding, unitType.whatBuilds().getKey())) {
                                if(isRecruitable(unitType) && !hasUnitInQueue(unitType)) {
                                    addToQueue(unitType, PlannedItemType.UNIT, 2);
                                }
                            }
                        }
                    }

                    if(isCurrentlyTraining(productionBuilding, UnitType.Terran_Barracks) && unitTypeCount.get(UnitType.Terran_Factory) < 1) {
                        if (isRecruitable(UnitType.Terran_Marine) && !hasUnitInQueue(UnitType.Terran_Marine)) {
                            addToQueue(UnitType.Terran_Marine, PlannedItemType.UNIT, 3);
                        }
                    }

                    if(isCurrentlyTraining(productionBuilding, UnitType.Terran_Factory)) {
                        if (isRecruitable(UnitType.Terran_Siege_Tank_Tank_Mode) && unitTypeCount.get(UnitType.Terran_Siege_Tank_Tank_Mode) < 4 && !hasUnitInQueue(UnitType.Terran_Siege_Tank_Tank_Mode)) {
                            addToQueue(UnitType.Terran_Siege_Tank_Tank_Mode, PlannedItemType.UNIT, 2);
                        }
                        else if(isRecruitable(UnitType.Terran_Vulture) && !hasUnitInQueue(UnitType.Terran_Vulture)) {
                            addToQueue(UnitType.Terran_Vulture, PlannedItemType.UNIT, 3);

                        }
                    }

                    if(resourceManager.getAvailableMinerals() > 500 && unitTypeCount.get(UnitType.Terran_Factory) < 4 && !hasUnitInQueue(UnitType.Terran_Factory)) {
                        addProductionBuilding(UnitType.Terran_Factory, 3);
                    }
                }
                break;

        }
    }

    //Unplanned depot additions to the queue
    private void addSupplyDepot() {
        if(!isDepotInQueue()) {

            int usedSupply = game.self().supplyUsed() / 2;
            int totalSupply = game.self().supplyTotal() / 2;
            int freeSupply = totalSupply - usedSupply;

            if(freeSupply <= 4 && buildTiles.getMediumBuildTiles().size() >= 2) {
                addToQueue(UnitType.Terran_Supply_Depot, PlannedItemType.BUILDING, 1);
                addToQueue(UnitType.Terran_Supply_Depot, PlannedItemType.BUILDING, 1);
            }

            if(freeSupply <= 4 && buildTiles.getMediumBuildTiles().size() == 1) {
                addToQueue(UnitType.Terran_Supply_Depot, PlannedItemType.BUILDING, 1);
            }
        }
    }

    private void addProductionBuilding(UnitType unitType, int priority) {
        if(buildTiles.getLargeBuildTiles().isEmpty()) {
            return;
        }

        int currentlyBuilding = (int) productionQueue.stream().filter(pi -> pi.getUnitType() != null && pi.getUnitType().tileHeight() == 3 && pi.getUnitType().tileWidth() == 4 && (pi.getPlannedItemStatus() == PlannedItemStatus.SCV_ASSIGNED || pi.getPlannedItemStatus() == PlannedItemStatus.IN_PROGRESS)).count();

        if(buildTiles.getLargeBuildTiles().size() == currentlyBuilding) {
            return;
        }

        addToQueue(unitType, PlannedItemType.BUILDING, priority);
    }

    private void scvProduction() {
        int ownedBases = baseInfo.getOwnedBases().size();
        int workerCap = 24 * ownedBases;

        if(unitTypeCount.get(UnitType.Terran_SCV) < workerCap) {
            boolean scvInQueue = productionQueue.stream()
                    .anyMatch(pi -> pi.getUnitType() == UnitType.Terran_SCV);

            if (!scvInQueue) {
                for(int i = 0; i < ownedBases; i++) {
                    addToQueue(UnitType.Terran_SCV, PlannedItemType.UNIT, 4);
                }
            }
        }
    }

    private boolean isDepotInQueue() {
        if(buildTiles.getMediumBuildTiles().isEmpty()) {
            return true;
        }

        for(PlannedItem pi : productionQueue) {
            if(pi.getUnitType() == UnitType.Terran_Supply_Depot) {
                return true;
            }

//            if(pi.getUnitType() == UnitType.Terran_Command_Center) {
//                return true;
//            }
        }
        return false;
    }

    //this might break if a natural has no geyser
    private void setRefineryPosition(PlannedItem pi) {
        for(Base base : baseInfo.getOwnedBases()) {
            if(!baseInfo.getGeyserTiles().containsKey(base)) {
                continue;
            }

            if(baseInfo.getUsedGeysers().contains(baseInfo.getGeyserTiles().get(base))) {
                continue;
            }

            baseInfo.getUsedGeysers().add(baseInfo.getGeyserTiles().get(base));
            pi.setBuildPosition(baseInfo.getGeyserTiles().get(base));
        }
    }

    private void setBuildingPosition(PlannedItem pi) {
        TilePosition cloestBuildTile = null;
        int distanceFromSCV = Integer.MAX_VALUE;

        if(pi.getUnitType().tileHeight() == 3 && pi.getUnitType().tileWidth() == 4) {
            if(buildTiles.getLargeBuildTiles().isEmpty() && pi.getUnitType().canBuildAddon()) {
                return;
            }
            if(buildTiles.getLargeBuildTiles().isEmpty() && buildTiles.getLargeBuildTilesNoGap().isEmpty()) {
                return;
            }

            if(pi.getUnitType().canBuildAddon() || buildTiles.getLargeBuildTilesNoGap().isEmpty()) {
                for(TilePosition tilePosition : buildTiles.getLargeBuildTiles()) {
                    if(tileTaken(tilePosition)) {
                        continue;
                    }

                    int distance = tilePosition.getApproxDistance(baseInfo.getStartingBase().getLocation());

                    if(distance < distanceFromSCV) {
                        distanceFromSCV = distance;
                        cloestBuildTile = tilePosition;
                    }
                }
            }
            else {
                for(TilePosition tilePosition : buildTiles.getLargeBuildTilesNoGap()) {
                    if(tileTaken(tilePosition)) {
                        continue;
                    }

                    int distance = tilePosition.getApproxDistance(baseInfo.getStartingBase().getLocation());

                    if(distance < distanceFromSCV) {
                        distanceFromSCV = distance;
                        cloestBuildTile = tilePosition;
                    }
                }
            }
            pi.setBuildPosition(cloestBuildTile);

        }
        else if(pi.getUnitType().tileHeight() == 2 && pi.getUnitType().tileWidth() == 3) {
            if(buildTiles.getMediumBuildTiles().isEmpty()) {
                return;
            }

            if(pi.getUnitType() == UnitType.Terran_Bunker) {
                if(buildTiles.getCloseBunkerTile() == null) {
                    return;
                }

                if(gameState.getEnemyOpener() != null) {
                    if(gameState.getEnemyOpener().getStrategyName().equals("Four Pool")) {
                        pi.setBuildPosition(buildTiles.getCloseBunkerTile());
                        return;
                    }
                }

                if(bunkerPosition != null) {
                    pi.setBuildPosition(bunkerPosition);
                }

                return;
            }

            for(TilePosition tilePosition : buildTiles.getMediumBuildTiles()) {
                if(tileTaken(tilePosition)) {
                    continue;
                }

                int distance = tilePosition.getApproxDistance(baseInfo.getStartingBase().getLocation());

                if(distance < distanceFromSCV) {
                    distanceFromSCV = distance;
                    cloestBuildTile = tilePosition;
                }
            }
            pi.setBuildPosition(cloestBuildTile);
        }
        else {
            if(buildTiles.getMainChokeTurret() != null && !reservedTurretPositions.contains(buildTiles.getMainChokeTurret())) {
                reservedTurretPositions.add(buildTiles.getMainChokeTurret());
                pi.setBuildPosition(buildTiles.getMainChokeTurret());
                return;
            }

            if(buildTiles.getNaturalChokeTurret() != null && !reservedTurretPositions.contains(buildTiles.getNaturalChokeTurret())) {
                reservedTurretPositions.add(buildTiles.getNaturalChokeTurret());
                pi.setBuildPosition(buildTiles.getNaturalChokeTurret());
            }
        }
    }

    private void setCommandCenterPosition(PlannedItem pi) {
        for(Base base : baseInfo.getOrderedExpansions()) {
            if(!tilePositionValidator.isBuildable(base.getLocation(), UnitType.Terran_Command_Center)) {
                continue;
            }


            TilePosition tilePosition = base.getLocation();

            pi.setBuildPosition(tilePosition);
            baseInfo.getOrderedExpansions().removeIf(base1 -> base1.getLocation() == tilePosition);
            break;
        }

    }

    private void openerResponse() {
        openerResponse = true;

        Map<UnitType, Integer> buildingCounts = new HashMap<>();
        for(UnitType building : gameState.getEnemyOpener().getBuildingResponse()) {
            buildingCounts.merge(building, 1, Integer::sum);
        }

        productionQueue.removeIf(pi -> buildingCounts.containsKey(pi.getUnitType()));

        for(UnitType building : gameState.getEnemyOpener().getBuildingResponse()) {
            if(unitTypeCount.get(building) == 0) {
                if(building.isAddon()) {
                    addToQueue(building, PlannedItemType.ADDON, 1);
                }
                else {
                    addToQueue(building, PlannedItemType.BUILDING, 1);
                }
            }

        }
    }

    private void enemyTechResponse() {
        if(gameState.getKnownEnemyTechUnits().isEmpty()) {
            return;
        }

        for(EnemyTechUnits techUnit : gameState.getKnownEnemyTechUnits()) {
            if(techUnit.getFriendlyBuildingResponse().isEmpty()) {
                continue;
            }


            for(UnitType buildingResponse : techUnit.getFriendlyBuildingResponse()) {
                boolean existingBuilding = false;
                for(Unit building : allBuildings) {
                    if(building.getType() == buildingResponse) {
                        existingBuilding = true;
                        break;
                    }
                }

                for(PlannedItem pi : productionQueue) {
                    if(pi.getUnitType() == buildingResponse) {
                        existingBuilding = true;
                        break;
                    }
                }

                if(!existingBuilding) {
                    if(buildingResponse.isAddon()) {
                        addToQueue(buildingResponse, PlannedItemType.ADDON, 1);
                    }
                    else {
                        addToQueue(buildingResponse, PlannedItemType.BUILDING, 1);
                    }
                }

            }

            if(techUnit.getFriendlyUpgradeResponse().isEmpty()) {
                continue;
            }

            for(PlannedItem upgradeResponse : techUnit.getFriendlyUpgradeResponse()) {
                boolean existingUpgrade = false;

                for(PlannedItem pi : productionQueue) {
                    if(pi.getTechUpgrade() != null && upgradeResponse.getTechUpgrade() != null) {
                        if(pi.getTechUpgrade() == upgradeResponse.getTechUpgrade()) {
                            existingUpgrade = true;
                            break;
                        }
                    }

                    if(pi.getUpgradeType() != null && upgradeResponse.getUpgradeType() != null) {
                        if(pi.getUpgradeType() == upgradeResponse.getUpgradeType()) {
                            existingUpgrade = true;
                            break;
                        }
                    }
                }

                if(upgradeResponse.getTechUpgrade() != null) {
                    if(game.self().hasResearched(upgradeResponse.getTechUpgrade())) {
                        existingUpgrade = true;
                    }
                }

                if(upgradeResponse.getUpgradeType() != null) {
                    if(game.self().getUpgradeLevel(upgradeResponse.getUpgradeType()) >= upgradeResponse.getUpgradeLevel()) {
                        existingUpgrade = true;
                    }
                }

                if(!existingUpgrade) {
                    productionQueue.add(upgradeResponse);
                }
            }

        }
    }

    //Track number of buildings to check for building requirements
    private void addUnitTypeCount(Unit unit) {
        unitTypeCount.put(unit.getType(), unitTypeCount.get(unit.getType()) + 1);
    }

    private void initUnitCounts()  {
        for(UnitType unitType : UnitType.values()) {
            if(unitType.getRace().toString().equals("Terran") && !unitType.isCritter() && !unitType.isHero() && !unitType.isBeacon() && !unitType.isSpecialBuilding()) {
                unitTypeCount.put(unitType, 0);
            }
        }
    }

    //rename this to something less confusing (its checking for the opposite)
    private boolean isCurrentlyTraining(Unit unit, UnitType unitType) {
        return unit.getType() == unitType && !unit.isTraining();
    }

    //TODO: replace sections of buildprodduction with this
    private boolean isRecruitable(UnitType unitType) {
        int usedSupply = game.self().supplyUsed() / 2;
        int totalSupply = game.self().supplyTotal() / 2;
        int freeSupply = totalSupply - usedSupply;

        if(resourceManager.getAvailableMinerals() >= unitType.mineralPrice() && resourceManager.getAvailableGas() >= unitType.gasPrice() && freeSupply >= unitType.supplyRequired() / 2) {
            return true;
        }
        return false;
    }

    private boolean isRecruitable(UnitType unitType, int priority) {
        int usedSupply = game.self().supplyUsed() / 2;
        int totalSupply = game.self().supplyTotal() / 2;
        int freeSupply = totalSupply - usedSupply;

        if(resourceManager.getAvailableMinerals() >= unitType.mineralPrice() && resourceManager.getAvailableGas() >= unitType.gasPrice() && freeSupply >= unitType.supplyRequired() / 2) {
            return true;
        }

        if(priority == 1 && freeSupply >= unitType.supplyRequired() / 2 && resourceManager.getAvailableGas() <= unitType.gasPrice()) {
            return true;
        }

        return false;
    }

    private boolean isUpgrading(UpgradeType upgradeType) {
        for(Unit researchBuilding : allBuildings) {
            if(researchBuilding.getAddon() != null && researchBuilding.getAddon().getUpgrade() == upgradeType) {
                return true;
            }

            if(researchBuilding.getUpgrade() == upgradeType) {
                return true;
            }
        }
        return false;
    }

    private boolean isResearching(TechType techType) {
        for(Unit researchBuilding : allBuildings) {
            if(researchBuilding.getAddon() != null && researchBuilding.getAddon().getTech() == techType) {
                return true;
            }

            if(researchBuilding.getTech() == techType) {
                return true;
            }
        }
        return false;
    }

    private void researchUpgrade(UpgradeType upgradeType) {
        if(resourceManager.getAvailableMinerals() >= upgradeType.mineralPrice() && resourceManager.getAvailableGas() >= upgradeType.gasPrice()) {
            for(Unit researchBuilding : allBuildings) {
                if(researchBuilding.canUpgrade(upgradeType) && !researchBuilding.isUpgrading()) {
                    researchBuilding.upgrade(upgradeType);
                    break;
                }
            }
        }
    }

    private void researchTech(TechType techType) {
        if(resourceManager.getAvailableMinerals() >= techType.mineralPrice() && resourceManager.getAvailableGas() >= techType.gasPrice()) {
            for(Unit researchBuilding : allBuildings) {
                if(researchBuilding.canResearch(techType) && !researchBuilding.isUpgrading()) {
                    researchBuilding.research(techType);
                    break;
                }
            }
        }

    }

    private boolean isResearching(UnitType unitType) {
        int availableBuildings = 0;
        for(Unit researchBuilding : allBuildings) {
            if(researchBuilding.getType() == unitType && researchBuilding.isCompleted() && !(researchBuilding.isResearching() || researchBuilding.isUpgrading())) {
                availableBuildings++;
            }
        }
        return availableBuildings > 0;
    }

    private boolean buildingInProduction(TilePosition tilePosition, UnitType unitType) {
        if(tilePosition == null) {
            return false;
        }

        for(Unit building : allBuildings) {
            if (building.getType() == unitType && building.getTilePosition().getX() == tilePosition.getX() && building.getTilePosition().getY() == tilePosition.getY()) {
                return true;
            }
        }
        return false;
    }

    private void resetUnitInProduction(Unit destroyedBuilding) {
        for(PlannedItem pi : productionQueue) {
            if(pi.getPlannedItemStatus() == PlannedItemStatus.IN_PROGRESS && pi.getPlannedItemType() == PlannedItemType.UNIT && pi.getProductionBuilding() == destroyedBuilding) {
                pi.setPlannedItemStatus(PlannedItemStatus.NOT_STARTED);
                pi.setProductionBuilding(null);
            }
        }
    }

    private void removeUnitTypeCount(Unit unit) {
            unitTypeCount.put(unit.getType(), unitTypeCount.get(unit.getType()) - 1);
    }

    private void removeUnitTypeCount(UnitType unit) {
        unitTypeCount.put(unit, unitTypeCount.get(unit) - 1);
    }

    private void removeBuilding(Unit unit) {
        allBuildings.remove(unit);
        productionBuildings.remove(unit);
    }

    private void resetBuilding(Unit unit) {
        for(PlannedItem pi : productionQueue) {
            if(pi.getPlannedItemStatus() == PlannedItemStatus.IN_PROGRESS && pi.getUnitType() == unit.getType()) {
                for(Workers worker : resourceManager.getWorkers()) {
                    if(!worker.getUnit().exists()) {
                        continue;
                    }

                    if(worker.getUnit() == null) {
                        continue;
                    }

                    if(worker.getUnit().getID() == pi.getAssignedBuilder().getUnitID()) {
                        worker.setWorkerStatus(WorkerStatus.IDLE);
                        break;
                    }
                }
                pi.setPlannedItemStatus(PlannedItemStatus.NOT_STARTED);
                pi.setAssignedBuilder(null);
                pi.setBuildPosition(null);
                pi.setPriority(1);
                break;
            }
        }
    }

    private boolean meetsRequirements(UnitType unitType) {
        Map<UnitType, Integer> requiredUnits = unitType.requiredUnits();

        if(requiredUnits.isEmpty()) {
            return true;
        }

        if(requiredUnits.size() == 1 && requiredUnits.containsKey(UnitType.Terran_SCV)) {
            return true;
        }

        if(unitType.isBuilding()) {
            if(unitType.tileHeight() == 3 && unitType.tileWidth() == 4 && buildTiles.getLargeBuildTiles().isEmpty()) {
                return false;
            }
            if(unitType.tileHeight() == 2 && unitType.tileWidth() == 3 && buildTiles.getMediumBuildTiles().isEmpty()) {
                return false;
            }
        }

        if(unitType.gasPrice() > 0 && unitTypeCount.get(UnitType.Terran_Refinery) == 0) {
            return false;
        }

        for(Map.Entry<UnitType, Integer> requirement : requiredUnits.entrySet()) {
            UnitType requiredUnit = requirement.getKey();
            int requiredCount = requirement.getValue();

            if(!requiredUnit.isBuilding()) {
                continue;
            }

            if(unitTypeCount.get(requiredUnit) < requiredCount) {
                return false;
            }
        }

        return true;
    }

    private boolean tileTaken(TilePosition tilePosition) {
        for(PlannedItem pi : productionQueue) {
            if(pi.getBuildPosition() == null) {
                continue;
            }

            if(pi.getBuildPosition() == tilePosition) {
                return true;
            }
        }
        return false;
    }

    private boolean canBeResearched(UnitType unitType) {
        if(unitTypeCount.get(unitType) > 0) {
            return true;
        }

        return false;
    }

    private boolean hasUnitInQueue(UnitType unitType) {
        return productionQueue.stream().anyMatch(pi -> pi.getUnitType() == unitType && pi.getPlannedItemStatus() == PlannedItemStatus.NOT_STARTED);
    }

    private void getOpenerNames() {
        openerNames.add(new EightRax());
        openerNames.add(new TwoRax());
        openerNames.add(new TwoRaxAcademy());
        openerNames.add(new OneRaxFE());
        openerNames.add(new TwoFac());
    }

    public void onFrame() {
        scvProduction();
        buildingProduction();
        addSupplyDepot();
        addUnitProduction();

        if(gameState.getEnemyOpener() != null && !openerResponse) {
            openerResponse();
        }

        enemyTechResponse();
        buildTiles.onFrame();
        painters.paintProductionQueueReadout(productionQueue);
    }

    //TODO: get rid of this why am i checking before it's done
    public void onUnitCreate(Unit unit) {
        if(unit.getType().isBuilding()) {
            allBuildings.add(unit);
        }
    }

    public void onUnitComplete(Unit unit) {
        addUnitTypeCount(unit);

        if(unit.canTrain()) {
            productionBuildings.add(unit);
        }
        if(unit.getType().isBuilding()) {
            newestCompletedBuilding = unit;
        }
        buildTiles.onUnitComplete(unit);
    }

    public void onUnitDestroy(Unit unit) {
        removeUnitTypeCount(unit);
        removeBuilding(unit);

        if(unit.getType().isBuilding()) {
            if(unit.getType() == UnitType.Terran_Bunker || unit.getType() == UnitType.Terran_Refinery) {
                return;
            }

            if(unit.getType() == UnitType.Terran_Missile_Turret) {
                reservedTurretPositions.remove(unit.getTilePosition());
            }

            if(!unit.isCompleted()) {
                resetBuilding(unit);
            }
            else if(unit.getType().isAddon()){
                addToQueue(unit.getType(), PlannedItemType.ADDON, 1);
            }
            else {
                addToQueue(unit.getType(), PlannedItemType.BUILDING, 1);
            }

            resetUnitInProduction(unit);

            if(unit.getType().tileHeight() == 3 && unit.getType().tileWidth() == 4) {
                if(unit.getType() == UnitType.Terran_Command_Center) {
                    baseInfo.readdExpansion(unit);
                    return;
                }

                buildTiles.getLargeBuildTiles().add(unit.getTilePosition());
            }
            else if(unit.getType().tileHeight() == 2 && unit.getType().tileWidth() == 3) {
                buildTiles.getMediumBuildTiles().add(unit.getTilePosition());
            }
        }

    }

    public void onUnitMorph(Unit unit) {
        if(unit.getType().isBuilding()) {
            allBuildings.add(unit);
        }

        if(unit.getType() == UnitType.Terran_Siege_Tank_Siege_Mode) {
            removeUnitTypeCount(UnitType.Terran_Siege_Tank_Tank_Mode);
        }
        else if(unit.getType() == UnitType.Terran_Siege_Tank_Tank_Mode) {
            removeUnitTypeCount(UnitType.Terran_Siege_Tank_Siege_Mode);
        }
    }

}