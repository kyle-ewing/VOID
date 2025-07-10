package macro;

import bwapi.*;
import bwem.BWEM;
import bwem.Base;
import debug.Painters;
import information.BaseInfo;
import information.EnemyInformation;
import information.enemyopeners.EnemyStrategy;
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
    private Player player;
    private Race enemyRace;
    private ResourceManager resourceManager;
    private BaseInfo baseInfo;
    private BWEM bwem;
    private Painters painters;
    private TilePositionValidator tilePositionValidator;
    private BuildTiles buildTiles;
    private EnemyInformation enemyInformation;
    private HashMap<UnitType, Integer> unitTypeCount = new HashMap<>();
    private HashSet<Unit> productionBuildings = new HashSet<>();
    private HashSet<Unit> allBuildings = new HashSet<>();
    private ArrayList<BuildOrder> openerNames = new ArrayList<>();
    private PriorityQueue<PlannedItem> productionQueue = new PriorityQueue<>(new BuildComparator());
    private BuildOrder startingOpener;
    private Unit newestCompletedBuilding = null;
    private boolean openerResponse = false;
    private boolean priorityStop = false;



    public ProductionManager(Game game, Player player, ResourceManager resourceManager, BaseInfo baseInfo, BWEM bwem, EnemyInformation enemyInformation) {
        this.game = game;
        this.player = player;
        this.resourceManager = resourceManager;
        this.baseInfo = baseInfo;
        this.enemyInformation = enemyInformation;

        tilePositionValidator = new TilePositionValidator(game);
        buildTiles = new BuildTiles(game, bwem, baseInfo);

        painters = new Painters(game);

        initialize();
    }

    public void initialize() {
        enemyRace = game.enemy().getRace();
        getOpenerNames();
        appendBuildOrder(enemyRace);
        initUnitCounts();
    }

    private PriorityQueue<PlannedItem> appendBuildOrder(Race enemyRace) {
        if(enemyRace.toString().equals("Zerg")) {
            for(BuildOrder buildOrder : openerNames) {
                if(buildOrder.getBuildOrderName() == BuildOrderName.TWORAXACADEMY) {
                    productionQueue.addAll(buildOrder.getBuildOrder());
                    startingOpener = buildOrder;
                }
            }
        }
        else if(enemyRace.toString().equals("Terran")) {
            for(BuildOrder buildOrder : openerNames) {
                if(buildOrder.getBuildOrderName() == BuildOrderName.TWORAXACADEMY) {
                    productionQueue.addAll(buildOrder.getBuildOrder());
                    startingOpener = buildOrder;
                }
            }
        }
        else if(enemyRace.toString().equals("Protoss")) {
            for(BuildOrder buildOrder : openerNames) {
                if(buildOrder.getBuildOrderName() == BuildOrderName.TWOFAC) {
                    productionQueue.addAll(buildOrder.getBuildOrder());
                    startingOpener = buildOrder;
                }
            }
        }
        else {
            for(BuildOrder buildOrder : openerNames) {
                if(buildOrder.getBuildOrderName() == BuildOrderName.TWORAXACADEMY) {
                    productionQueue.addAll(buildOrder.getBuildOrder());
                    startingOpener = buildOrder;
                }
            }
        }
        return productionQueue;
    }

    private void buildingProduction() {
        boolean hasHighPriorityBuilding = hasHigherPriorityBuilding();

        for (PlannedItem pi : productionQueue) {
            if(pi.getPriority() == 1 && pi.getPlannedItemStatus() == PlannedItemStatus.NOT_STARTED && meetsRequirements(pi.getUnitType()) && pi.getSupply() <= player.supplyUsed() / 2) {
                priorityStop = true;
            }

            if(priorityStop && pi.getPriority() != 1 && pi.getPlannedItemType() == PlannedItemType.BUILDING) {
                continue;
            }

            if (hasHighPriorityBuilding && pi.getPlannedItemType() == PlannedItemType.UNIT && pi.getPlannedItemStatus() == PlannedItemStatus.NOT_STARTED) {
                continue;
            }

            switch (pi.getPlannedItemStatus()) {
                case NOT_STARTED:
                    if (pi.getSupply() <= player.supplyUsed() / 2) {

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

                        if (resourceManager.getAvailableMinerals() >= pi.getUnitType().mineralPrice() && resourceManager.getAvailableGas() >= pi.getUnitType().gasPrice()) {

                            if(pi.getPlannedItemType() == PlannedItemType.UNIT) {
                                for(Unit productionBuilding : productionBuildings) {
                                    if(productionBuilding.canTrain(pi.getUnitType()) && !productionBuilding.isTraining()) {
                                        productionBuilding.train(pi.getUnitType());
                                        pi.setAssignedBuilder(productionBuilding);
                                        pi.setPlannedItemStatus(PlannedItemStatus.IN_PROGRESS);
                                        break;
                                    }
                                }
                            }
                            else if(pi.getPlannedItemType() == PlannedItemType.BUILDING) {
                                for(Workers worker : resourceManager.getWorkers()) {
                                    if(worker.getUnit() == pi.getAssignedBuilder()) {
                                        continue;
                                    }

                                    if(worker.getWorkerStatus() == WorkerStatus.MINERALS && worker.getUnit().canBuild(pi.getUnitType())) {
                                        pi.setAssignedBuilder(worker.getUnit());
                                        if(pi.getBuildPosition() == null) {

                                            if (pi.getUnitType() == UnitType.Terran_Refinery) {
                                                setRefineryPosition(pi);
                                            }
                                            else if(pi.getUnitType() == UnitType.Terran_Command_Center) {
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
                                        resourceManager.reserveResources(pi.getUnitType());
                                        worker.getUnit().move(pi.getBuildPosition().toPosition());
                                        worker.getUnit().build(pi.getUnitType(), pi.getBuildPosition());
                                        pi.setPlannedItemStatus(PlannedItemStatus.SCV_ASSIGNED);

                                        worker.setWorkerStatus(WorkerStatus.MOVING_TO_BUILD);
                                        break;
                                    }
                                }
                            }
                            else if(pi.getPlannedItemType() == PlannedItemType.ADDON) {
                                for(Unit productionBuilding : productionBuildings) {
                                    if(productionBuilding.canBuildAddon(pi.getUnitType()) && !productionBuilding.isTraining() && productionBuilding.getAddon() == null) {
                                        productionBuilding.buildAddon(pi.getUnitType());

                                        pi.setAddOnParent(productionBuilding);
                                        pi.setPlannedItemStatus(PlannedItemStatus.IN_PROGRESS);

                                        break;
                                    }
                                }
                            }
                        }
                    }

                    if(pi.getPriority() == 1 && pi.getPlannedItemStatus() != PlannedItemStatus.NOT_STARTED) {
                        priorityStop = false;
                    }

                    break;

                case SCV_ASSIGNED:
                    for(Workers worker : resourceManager.getWorkers()) {
                        if(worker.getUnit().getID() == pi.getAssignedBuilder().getID() && worker.getWorkerStatus() == WorkerStatus.MOVING_TO_BUILD) {
                            if(worker.getUnit().getDistance(pi.getBuildPosition().toPosition()) < 96) {
                                worker.getUnit().build(pi.getUnitType(), pi.getBuildPosition());
                                break;
                            }

                            if(worker.getBuildFrameCount() > 600) {
                                worker.setWorkerStatus(WorkerStatus.IDLE);
                                worker.getUnit().stop();
                                resourceManager.unreserveResources(pi.getUnitType());
                                pi.setPlannedItemStatus(PlannedItemStatus.NOT_STARTED);
                                break;
                            }
                        }
                    }

                    if(buildingInProduction(pi.getBuildPosition(), pi.getUnitType())) {
                        resourceManager.unreserveResources(pi.getUnitType());
                        pi.setPlannedItemStatus(PlannedItemStatus.IN_PROGRESS);

                        for(Workers worker : resourceManager.getWorkers()) {
                            if(worker.getUnit().getID() == pi.getAssignedBuilder().getID()) {
                                worker.setWorkerStatus(WorkerStatus.BUILDING);
                                break;
                            }
                        }
                    }
                    break;

                case IN_PROGRESS:
                    if(pi.getPlannedItemType() == PlannedItemType.BUILDING) {
                        if (pi.getUnitType() == newestCompletedBuilding.getType()) {
                            if (newestCompletedBuilding.getInitialTilePosition().getX() == pi.getBuildPosition().getX() &&
                                    newestCompletedBuilding.getInitialTilePosition().getY() == pi.getBuildPosition().getY()) {
                                pi.setPlannedItemStatus(PlannedItemStatus.COMPLETE);

                                if (newestCompletedBuilding.canTrain()) {
                                    productionBuildings.add(newestCompletedBuilding);
                                }

                                for (Workers worker : resourceManager.getWorkers()) {
                                    if (worker.getWorkerStatus() == WorkerStatus.BUILDING && pi.getAssignedBuilder() == worker.getUnit()) {
                                        worker.setWorkerStatus(WorkerStatus.IDLE);
                                    }
                                }
                            }

                        }

                        boolean builderHasDied = true;
                        for(Workers workers : resourceManager.getWorkers()) {
                            if(workers.getUnit().getID() == pi.getAssignedBuilder().getID()) {
                                builderHasDied = false;
                                break;
                            }
                        }

                        if(builderHasDied) {
                            for(Workers worker : resourceManager.getWorkers()) {
                                if(worker.getWorkerStatus() == WorkerStatus.MINERALS) {
                                    pi.setAssignedBuilder(worker.getUnit());
                                    worker.setWorkerStatus(WorkerStatus.MOVING_TO_BUILD);
                                    break;
                                }
                            }
                        }

                        for(Unit building : player.getUnits()) {
                            if(!building.isCompleted() && building.getType() == pi.getUnitType() && !building.isBeingConstructed()) {
                                pi.getAssignedBuilder().rightClick(building);
                                break;
                            }

                            if(building.isBeingConstructed() && pi.getAssignedBuilder().isConstructing()) {
                                for(Workers worker : resourceManager.getWorkers()) {
                                    if(worker.getUnit().getID() == pi.getAssignedBuilder().getID()) {
                                        worker.setWorkerStatus(WorkerStatus.BUILDING);
                                        break;
                                    }
                                }
                            }
                        }
                    }


                    if(pi.getPlannedItemType() == PlannedItemType.UPGRADE) {
                        if(pi.getUpgradeType() != null) {
                            if(game.self().getUpgradeLevel(pi.getUpgradeType()) == pi.getUpgradeLevel()) {
                                pi.setPlannedItemStatus(PlannedItemStatus.COMPLETE);
                            }
                        }
                        else if(pi.getTechUpgrade() != null) {
                            if(game.self().hasResearched(pi.getTechUpgrade())) {
                                pi.setPlannedItemStatus(PlannedItemStatus.COMPLETE);
                            }
                        }
                    }

                    if(pi.getPlannedItemType() == PlannedItemType.ADDON) {
                        if(pi.getAddOnParent() == null || pi.getAddOnParent().getAddon() == null) {
                            continue;
                        }

                        if(pi.getAddOnParent() != null && pi.getAddOnParent().getAddon().isCompleted()) {
                            pi.setPlannedItemStatus(PlannedItemStatus.COMPLETE);
                        }
                    }


                    for(Unit productionBuilding : productionBuildings) {
                        if(pi.getAssignedBuilder() == productionBuilding && !productionBuilding.isTraining()) {
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
                    if (isCurrentlyTraining(productionBuilding, UnitType.Terran_Barracks)) {
                        if(productionBuilding.canTrain(UnitType.Terran_Medic) && isRecruitable(UnitType.Terran_Medic) && unitTypeCount.get(UnitType.Terran_Medic) < 3 && !hasUnitInQueue(UnitType.Terran_Medic)) {
                                addToQueue(UnitType.Terran_Medic, PlannedItemType.UNIT,2);
                        }
                        else if(isRecruitable(UnitType.Terran_Marine) && !hasUnitInQueue(UnitType.Terran_Marine)) {
                            addToQueue(UnitType.Terran_Marine, PlannedItemType.UNIT,3);
                        }
                    }

                }
                //TODO: remove when transitions are added
                if(resourceManager.getAvailableMinerals() > 400 && !buildTiles.getLargeBuildTiles().isEmpty() && unitTypeCount.get(UnitType.Terran_Barracks) < 6 && !hasUnitInQueue(UnitType.Terran_Barracks)) {
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
                    if(isCurrentlyTraining(productionBuilding, UnitType.Terran_Barracks) && unitTypeCount.get(UnitType.Terran_Factory) < 1) {
                        if (isRecruitable(UnitType.Terran_Marine) && !hasUnitInQueue(UnitType.Terran_Marine)) {
                            addToQueue(UnitType.Terran_Marine, PlannedItemType.UNIT, 3);
                        }
                    }

                    if(isCurrentlyTraining(productionBuilding, UnitType.Terran_Factory)) {
                        if (isRecruitable(UnitType.Terran_Siege_Tank_Tank_Mode) && unitTypeCount.get(UnitType.Terran_Siege_Tank_Tank_Mode) < 4 && !hasUnitInQueue(UnitType.Terran_Siege_Tank_Tank_Mode)) {
                            addToQueue(UnitType.Terran_Siege_Tank_Tank_Mode, PlannedItemType.UNIT, 2);
                        }
                        else if (isRecruitable(UnitType.Terran_Vulture) && !hasUnitInQueue(UnitType.Terran_Vulture)) {
                            addToQueue(UnitType.Terran_Vulture, PlannedItemType.UNIT, 3);
                        }

                    }

                    if(resourceManager.getAvailableMinerals() > 400 && !buildTiles.getLargeBuildTiles().isEmpty() && unitTypeCount.get(UnitType.Terran_Factory) < 3 && !hasUnitInQueue(UnitType.Terran_Factory)) {
                        addToQueue(UnitType.Terran_Factory, PlannedItemType.BUILDING, 3);
                    }
                }
                break;

        }
    }

    //Unplanned depot additions to the queue
    private void addSupplyDepot() {

        if (!isDepotInQueue()) {

            int usedSupply = game.self().supplyUsed() / 2;
            int totalSupply = game.self().supplyTotal() / 2;
            int freeSupply = totalSupply - usedSupply;

            if (freeSupply <= 4) {
                addToQueue(UnitType.Terran_Supply_Depot, PlannedItemType.BUILDING, 1);
                addToQueue(UnitType.Terran_Supply_Depot, PlannedItemType.BUILDING, 1);
            }
        }
    }

    //TODO: scale max scvs based on base count
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
        for(PlannedItem pi : productionQueue) {
            if(pi.getUnitType() == UnitType.Terran_Supply_Depot) {
                return true;
            }

            if(pi.getUnitType() == UnitType.Terran_Command_Center) {
                return true;
            }
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
            if(buildTiles.getLargeBuildTiles().isEmpty()) {
                return;
            }

            for(TilePosition tilePosition : buildTiles.getLargeBuildTiles()) {
                int distance = tilePosition.getApproxDistance(pi.getAssignedBuilder().getTilePosition());

                if(distance < distanceFromSCV) {
                    distanceFromSCV = distance;
                    cloestBuildTile = tilePosition;
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

                if(enemyInformation.getEnemyOpener() != null) {
                    if(enemyInformation.getEnemyOpener().getStrategyName().equals("Four Pool")) {
                        pi.setBuildPosition(buildTiles.getCloseBunkerTile());
                        return;
                    }
                }

                if(baseInfo.getOwnedBases().contains(baseInfo.getNaturalBase())) {
                    pi.setBuildPosition(buildTiles.getNaturalChokeBunker());
                }
                else {
                    pi.setBuildPosition(buildTiles.getMainChokeBunker());
                }
                return;
            }

            for(TilePosition tilePosition : buildTiles.getMediumBuildTiles()) {
                int distance = tilePosition.getApproxDistance(pi.getAssignedBuilder().getTilePosition());

                if(distance < distanceFromSCV) {
                    distanceFromSCV = distance;
                    cloestBuildTile = tilePosition;
                }
            }
            pi.setBuildPosition(cloestBuildTile);
        }
        else {
            //turrets and addons
        }
        buildTiles.updateRemainingTiles(pi.getBuildPosition());
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

        for(UnitType building : enemyInformation.getEnemyOpener().getBuildingResponse()) {
            productionQueue.removeIf(pi -> pi.getUnitType() == building);
            addToQueue(building, PlannedItemType.BUILDING, 1);
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

    private void removeUnitTypeCount(Unit unit) {
            unitTypeCount.put(unit.getType(), unitTypeCount.get(unit.getType()) - 1);
    }

    private void removeUnitTypeCount(UnitType unit) {
        unitTypeCount.put(unit, unitTypeCount.get(unit) - 1);
    }

    private void removeBuilding(Unit unit) {
        allBuildings.remove(unit);
    }

    private void resetBuilding(Unit unit) {
        for(PlannedItem pi : productionQueue) {
            if(pi.getPlannedItemStatus() == PlannedItemStatus.IN_PROGRESS && pi.getUnitType() == unit.getType()) {
                for(Workers worker : resourceManager.getWorkers()) {
                    if(worker.getUnit().getID() == pi.getAssignedBuilder().getID()) {
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
        if(!unitType.isBuilding()) {
            return false;
        }

        Map<UnitType, Integer> requiredUnits = unitType.requiredUnits();
        if(requiredUnits.isEmpty()) {
            return true;
        }

        for(UnitType requiredUnit : requiredUnits.keySet()) {
            if(!requiredUnit.isBuilding()) {
                continue;
            }

            if(unitTypeCount.get(requiredUnit) > 0) {
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

        if(enemyInformation.getEnemyOpener() != null && !openerResponse) {
            openerResponse();
        }
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
    }

    public void onUnitDestroy(Unit unit) {
        removeUnitTypeCount(unit);
        removeBuilding(unit);

        if(unit.getType().isBuilding() && unit.getType() != UnitType.Terran_Bunker) {
            if(!unit.isCompleted()) {
                resetBuilding(unit);
            }
            else {
                addToQueue(unit.getType(), PlannedItemType.BUILDING, 1);
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