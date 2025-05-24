package macro;

import bwapi.*;
import bwem.BWEM;
import debug.Painters;
import information.BaseInfo;
import information.EnemyInformation;
import macro.buildorders.*;
import macro.unitgroups.WorkerStatus;
import macro.unitgroups.Workers;
import map.BuildTiles;
import map.TilePositionValidator;
import planner.BuildComparator;
import planner.PlannedItem;
import planner.PlannedItemStatus;
import planner.PlannedItemType;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;

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
                if(buildOrder.getBuildOrderName() == BuildOrderName.TWORAXACADEMY) {
                    productionQueue.addAll(buildOrder.getBuildOrder());
                    startingOpener = buildOrder;
                }
            }
        }
        else {
            //no random build order
        }
        return productionQueue;
    }

    private void buildingProduction() {
        for (PlannedItem pi : productionQueue) {
            if(pi.getPriority() == 1 && pi.getPlannedItemStatus() == PlannedItemStatus.NOT_STARTED) {
                priorityStop = true;
            }

            if(priorityStop && pi.getPriority() != 1 && pi.getPlannedItemType() == PlannedItemType.BUILDING) {
                continue;
            }

            switch (pi.getPlannedItemStatus()) {
                case NOT_STARTED:
                    if (pi.getSupply() <= player.supplyUsed() / 2) {

                        if(pi.getPlannedItemType() == PlannedItemType.UPGRADE) {
                            if(pi.getTechUpgrade() != null) {
                                researchTech(pi.getTechUpgrade());
                            }
                            else if(pi.getUpgradeType() != null) {
                                researchUpgrade(pi.getUpgradeType());
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
                            else if (pi.getPlannedItemType() == PlannedItemType.BUILDING) {
                                for (Workers worker : resourceManager.getWorkers()) {
                                    if (worker.getUnit() == pi.getAssignedBuilder()) {
                                        continue;
                                    }

                                    if (worker.getWorkerStatus() == WorkerStatus.MINERALS && worker.getUnit().canBuild(pi.getUnitType())) {
                                        pi.setAssignedBuilder(worker.getUnit());
                                        if (pi.getBuildPosition() == null) {

                                            if (pi.getUnitType() == UnitType.Terran_Refinery) {
                                                setRefineryPosition(pi);
                                            }

                                            setBuildingPosition(pi);

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

                            if(worker.getBuildFrameCount() > 300) {
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

                    if(pi.getPlannedItemType() == PlannedItemType.UPGRADE) {
                        if(game.self().hasResearched(pi.getTechUpgrade())) {
                            pi.setPlannedItemStatus(PlannedItemStatus.COMPLETE);
                        }
                        else if(game.self().getUpgradeLevel(pi.getUpgradeType()) == 1) {
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


    private void addUnitProduction() {
        switch(startingOpener.getBuildOrderName()) {
            case EIGHTRAX:
                for(Unit productionBuilding : productionBuildings) {
                    if(productionBuilding.getType() == UnitType.Terran_Barracks && !productionBuilding.isTraining() && resourceManager.getAvailableMinerals() >= 50) {
                        productionBuilding.train(UnitType.Terran_Marine);
                    }
                }
            case TWORAX:
                for(Unit productionBuilding : productionBuildings) {
                    if(productionBuilding.getType() == UnitType.Terran_Barracks && !productionBuilding.isTraining() && resourceManager.getAvailableMinerals() >= 50) {
                        productionBuilding.train(UnitType.Terran_Marine);
                    }
                }
            case TWORAXACADEMY:
                for(Unit productionBuilding : productionBuildings) {
                    if (isCurrentlyTraining(productionBuilding, UnitType.Terran_Barracks)) {
                        if(productionBuilding.canTrain(UnitType.Terran_Medic) && isRecruitable(UnitType.Terran_Medic) && unitTypeCount.get(UnitType.Terran_Medic) < 3) {
                                addToQueue(UnitType.Terran_Medic, PlannedItemType.UNIT,2);
                        }
                        else if(isRecruitable(UnitType.Terran_Marine)) {
                            addToQueue(UnitType.Terran_Marine, PlannedItemType.UNIT,3);
                        }
                    }
                }

                if(resourceManager.getAvailableMinerals() > 400) {
                    addToQueue(UnitType.Terran_Barracks, PlannedItemType.BUILDING, 3);
                }
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
        if(unitTypeCount.get(UnitType.Terran_SCV) < 24) {
            boolean scvInQueue = productionQueue.stream()
                    .anyMatch(pi -> pi.getUnitType() == UnitType.Terran_SCV);

            if (!scvInQueue) {
                addToQueue(UnitType.Terran_SCV, PlannedItemType.UNIT,3);
            }
        }
    }

    private boolean isDepotInQueue() {
        for(PlannedItem pi : productionQueue) {
            if(pi.getUnitType() == UnitType.Terran_Supply_Depot) {
                return true;
            }
        }
        return false;
    }

    //this might break if a natural has no geyser
    private void setRefineryPosition(PlannedItem pi) {
        if(baseInfo.getStartingBase().getGeysers().get(0).getUnit().getType() == UnitType.Resource_Vespene_Geyser) {
            pi.setBuildPosition(baseInfo.getStartingBase().getGeysers().get(0).getUnit().getTilePosition());
        }

//        if(baseInfo.getNaturalBase().getGeysers().get(0).getUnit().getType() == UnitType.Resource_Vespene_Geyser) {
//            pi.setBuildPosition(baseInfo.getNaturalBase().getGeysers().get(0).getUnit().getTilePosition());
//        }
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
            if(buildTiles.getLargeBuildTiles().isEmpty()) {
                return;
            }

            if(pi.getUnitType() == UnitType.Terran_Bunker) {
                if(buildTiles.getBunkerTile() == null) {
                    return;
                }
                pi.setBuildPosition(buildTiles.getBunkerTile());
                buildTiles.updateRemainingTiles(pi.getBuildPosition());
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

    private void openerResponse() {
        openerResponse = true;
        for(UnitType building : enemyInformation.getEnemyOpener().getBuildingResponse()) {
            System.out.println("Adding building to queue: " + building);
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
                if(researchBuilding.canUpgrade(upgradeType)) {
                    researchBuilding.upgrade(upgradeType);
                }
            }
        }
    }

    private void researchTech(TechType techType) {
        if(resourceManager.getAvailableMinerals() >= techType.mineralPrice() && resourceManager.getAvailableGas() >= techType.gasPrice()) {
            for(Unit researchBuilding : allBuildings) {
                if(researchBuilding.canResearch(techType)) {
                    researchBuilding.research(techType);
                }
            }
        }

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

    private void removeBuilding(Unit unit) {
        allBuildings.remove(unit);
    }

    private void getOpenerNames() {
        openerNames.add(new EightRax());
        openerNames.add(new TwoRax());
        openerNames.add(new TwoRaxAcademy());
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
    }

    public void onUnitMorph(Unit unit) {
        if(unit.getType().isBuilding()) {
            allBuildings.add(unit);
        }
    }

}