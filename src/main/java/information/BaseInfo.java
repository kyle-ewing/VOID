package information;

import bwapi.*;
import bwem.*;
import debug.Painters;
import map.PathFinding;
import map.AllBasePaths;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class BaseInfo {
    private BWEM bwem;
    private Game game;
    private PathFinding pathFinding;
    private AllBasePaths allBasePaths;
    private Base startingBase;
    private Base naturalBase;
    private ChokePoint mainChokePoint;
    private ChokePoint naturalChokePoint;
    private Unit initalCC = null;
    private HashSet<Base> mapBases = new HashSet<>();
    private HashSet<Base> startingBases = new HashSet<>();
    private HashSet<Mineral> startingMinerals = new HashSet<>();
    private HashSet<Geyser> startingGeysers = new HashSet<>();
    private HashSet<TilePosition> baseTiles = new HashSet<>();
    private HashSet<TilePosition> naturalTiles = new HashSet<>();
    private HashSet<Base> ownedBases = new HashSet<>();
    private HashSet<ChokePoint> chokePoints = new HashSet<>();
    private HashSet<TilePosition> usedGeysers = new HashSet<>();
    private HashSet<TilePosition> mainCliffEdge = new HashSet<>();
    private HashSet<TilePosition> naturalChokeEdge = new HashSet<>();
    private HashSet<TilePosition> combinedTankTiles = new HashSet<>();
    private HashSet<TilePosition> backupMainSiegeTiles = new HashSet<>();
    private HashMap<Base, TilePosition> geyserTiles = new HashMap<>();
    private HashMap<Base, List<Position>> allPathsMap;
    private ArrayList<Base> orderedExpansions = new ArrayList<>();
    private boolean naturalOwned = false;

    private Painters painters;

    //TODO: save paths so operation only needs to be calculated once
    public BaseInfo(BWEM bwem, Game game) {
        this.bwem = bwem;
        this.game = game;

        pathFinding = new PathFinding(bwem, game);
        painters = new Painters(game, bwem);

        init();
    }

    public void init() {

        for(Unit unit : game.getAllUnits()) {
            if(unit.getType() == UnitType.Terran_Command_Center) {
                initalCC = unit;
            }
        }

        addAllBases();
        setStartingBase();
        addStartingBases();
        setStartingMineralPatches();
        setStartingGeysers();
        setChokePoints();

        allBasePaths = new AllBasePaths(this);
        allPathsMap = new HashMap<>(allBasePaths.getPathLists());

        setNaturalBase();
        setMainChoke();
        setNaturalChoke();
        setStartingBaseTiles();
        setNaturalBaseTiles();
        setOrderedExpansions();
        setGeyserTiles();
        setMainCliffEdge();
        setNaturalChokeEdge();
        combineTankTiles();
        backupMainSiegeTiles();
    }

    private void addAllBases() {
        for(Base base : bwem.getMap().getBases()) {
            mapBases.add(base);
        }
    }

    private void addStartingBases() {
        for(Base base : mapBases) {
            if(base.getLocation() == startingBase.getLocation()) {
                continue;
            }
            if(base.isStartingLocation()) {
                startingBases.add(base);
            }
        }
    }

    public boolean isExplored(Base base) {
        if(game.isExplored(base.getLocation())) {
            return true;
        }

        return false;
    }

    private void setGeyserTiles() {
        for(Base base : mapBases) {
            if(base.getGeysers().isEmpty()) {
                continue;
            }

            for(Geyser geyser : base.getGeysers()) {
                geyserTiles.put(base, geyser.getUnit().getTilePosition());
            }
        }
    }

    public HashSet<TilePosition> getTilesForBase(Base base) {
        HashSet<TilePosition> tiles = new HashSet<>();
        Area baseArea = base.getArea();

        int mapWidth = game.mapWidth();
        int mapHeight = game.mapHeight();

        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                TilePosition tile = new TilePosition(x, y);
                Area tileArea = bwem.getMap().getArea(tile);
                if (tileArea != null && tileArea.getId() == baseArea.getId()) {
                    tiles.add(tile);
                }
            }
        }

        return tiles;
    }

    private void setStartingBaseTiles() {
       baseTiles = getTilesForBase(startingBase);
    }

    private void setNaturalBaseTiles() {
        naturalTiles = getTilesForBase(naturalBase);
    }

    private void setStartingMineralPatches() {
        for(Mineral mineral : startingBase.getMinerals()) {
            startingMinerals.add(mineral);
        }
    }

    private void setStartingGeysers() {
        for(Geyser geyser : startingBase.getGeysers()) {
            startingGeysers.add(geyser);
        }
    }

    private void setChokePoints() {
        chokePoints.addAll(bwem.getMap().getChokePoints());
    }

    private void setStartingBase() {
        Base closestStartingBase = null;
        int closestDistance = Integer.MAX_VALUE;

        for(Base base : mapBases) {
            if(base.isStartingLocation()) {
                int distance = initalCC.getDistance(base.getLocation().toPosition());
                if(closestDistance > distance) {
                    closestStartingBase = base;
                    closestDistance = distance;
                    startingBase = closestStartingBase;
                }
            }
        }
    }

    private void setNaturalBase() {
        Base closestBase = null;
        int closestDistance = Integer.MAX_VALUE;

        for(Base base : mapBases) {
            if(base != startingBase) {
                List<Position> path = allPathsMap.get(base);

                if(path == null || path.isEmpty()) {
                    continue;
                }

                int distance = path.size();



                if(base.getGeysers().isEmpty()) {
                    continue;
                }

                if(distance < closestDistance) {
                    //pathTest = path;
                    closestBase = base;
                    closestDistance = distance;
                }
            }
        }
                naturalBase = closestBase;
    }

    private void setOrderedExpansions() {
        for(Base base : mapBases) {
            if(base == startingBase) {
                continue;
            }

            List<Position> path = allPathsMap.get(base);

            if(path == null || path.isEmpty()) {
                continue;
            }

            int distance = path.size();

            if(base.getGeysers().isEmpty()) {
                continue;
            }

            if(orderedExpansions.isEmpty()) {
                orderedExpansions.add(base);
            }
            else {
                for(int i = 0; i < orderedExpansions.size(); i++) {
                    Base currentBase = orderedExpansions.get(i);
                    List<Position> currentPath = allPathsMap.get(currentBase);
                    int currentDistance = currentPath.size();

                    if(currentDistance > distance) {
                        orderedExpansions.add(i, base);
                        break;
                    }

                    if(i == orderedExpansions.size() - 1) {
                        orderedExpansions.add(base);
                        break;
                    }
                }
            }
        }
    }

    public void readdExpansion(Unit unit) {
        Base closestBase = null;
        int closestDistance = Integer.MAX_VALUE;

        for(Base base : mapBases) {
            int distance = unit.getPosition().getApproxDistance(base.getLocation().toPosition());
            if(distance < closestDistance) {
                closestBase = base;
                closestDistance = distance;
            }
        }

        if(closestBase == null) {
            return;
        }

        if(!orderedExpansions.contains(closestBase)) {
            orderedExpansions.add(0, closestBase);
        }
    }

    private void setMainCliffEdge() {
        ChokePoint mainChoke = getMainChoke();
        ChokePoint naturalChoke = getNaturalChoke();
        HashSet<TilePosition> actualCliffEdge = new HashSet<>();

        if(mainChoke == null || naturalChoke == null) {
            return;
        }

        for(TilePosition tile : baseTiles) {
            int distanceToMainChoke = mainChoke.getCenter().toPosition().getApproxDistance(tile.toPosition());
            int distanceToNaturalChoke = naturalChoke.getCenter().toPosition().getApproxDistance(tile.toPosition());

            if(distanceToMainChoke < 160 || distanceToMainChoke > 256 || distanceToNaturalChoke > 400) {
                continue;
            }

            boolean isCliffEdge = false;
            for(int dx = -1; dx <= 1 && !isCliffEdge; dx++) {
                for(int dy = -1; dy <= 1 && !isCliffEdge; dy++) {
                    if(dx == 0 && dy == 0) {
                        continue;
                    }

                    TilePosition adj = new TilePosition(tile.getX() + dx, tile.getY() + dy);
                    if(!baseTiles.contains(adj)) {
                        isCliffEdge = true;
                    }
                }
            }

            if(isCliffEdge) {
                actualCliffEdge.add(tile);
            }
        }

        for (TilePosition edgeTile : actualCliffEdge) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue;
                    TilePosition adj = new TilePosition(edgeTile.getX() + dx, edgeTile.getY() + dy);
                    if (baseTiles.contains(adj) && !mainCliffEdge.contains(adj)) {
                        mainCliffEdge.add(adj);
                    }
                }
            }
        }

    }

    private void setNaturalChokeEdge() {
        ChokePoint naturalChoke = getNaturalChoke();

        if (naturalChoke == null || naturalTiles.isEmpty()) {
            return;
        }

        Position chokeCenter = naturalChoke.getCenter().toPosition();
        int minDistance = 96;
        int maxDistance = 188;

        for(TilePosition tile : naturalTiles) {
            int distanceToChoke = chokeCenter.getApproxDistance(tile.toPosition());

            if(distanceToChoke >= minDistance && distanceToChoke <= maxDistance) {
                if(pathFinding.getTilePositionValidator().isWalkable(tile)) {
                    naturalChokeEdge.add(tile);
                }
            }
        }
    }

    private void combineTankTiles() {
        combinedTankTiles.addAll(mainCliffEdge);
        combinedTankTiles.addAll(naturalChokeEdge);
    }

    //TODO: pull repeated code into own method
    private void backupMainSiegeTiles() {
        ChokePoint mainChoke = getMainChoke();
        ChokePoint naturalChoke = getNaturalChoke();
        HashSet<TilePosition> actualCliffEdge = new HashSet<>();

        if(mainChoke == null || naturalChoke == null) {
            return;
        }

        for(TilePosition tile : baseTiles) {
            int distanceToMainChoke = mainChoke.getCenter().toPosition().getApproxDistance(tile.toPosition());
            int distanceToNaturalChoke = naturalChoke.getCenter().toPosition().getApproxDistance(tile.toPosition());

            if(distanceToMainChoke < 160 || distanceToMainChoke > 256) {
                continue;
            }

            boolean isCliffEdge = false;
            for(int dx = -1; dx <= 1 && !isCliffEdge; dx++) {
                for(int dy = -1; dy <= 1 && !isCliffEdge; dy++) {
                    if(dx == 0 && dy == 0) {
                        continue;
                    }

                    TilePosition adj = new TilePosition(tile.getX() + dx, tile.getY() + dy);
                    if(!baseTiles.contains(adj)) {
                        isCliffEdge = true;
                    }
                }
            }

            if(isCliffEdge) {
                actualCliffEdge.add(tile);
            }
        }

        for (TilePosition edgeTile : actualCliffEdge) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue;
                    TilePosition adj = new TilePosition(edgeTile.getX() + dx, edgeTile.getY() + dy);
                    if (baseTiles.contains(adj) && !backupMainSiegeTiles.contains(adj)) {
                        backupMainSiegeTiles.add(adj);
                    }
                }
            }
        }

    }

    public ChokePoint getMainChoke() {
        return mainChokePoint;
    }

    private void setMainChoke() {
        if(startingBase == null || naturalBase == null) {
            return;
        }

        List<Position> path = pathFinding.findPath(startingBase.getLocation().toPosition(), naturalBase.getLocation().toPosition()
        );

        if(path.isEmpty()) {
            return;
        }

        mainChokePoint = null;
        int minDistance = Integer.MAX_VALUE;

        for(ChokePoint chokePoint : bwem.getMap().getChokePoints()) {
            Position chokePos = chokePoint.getCenter().toPosition();

            for(Position pathPos : path) {
                int distance = chokePos.getApproxDistance(pathPos);
                if(distance < minDistance) {
                    minDistance = distance;
                    mainChokePoint = chokePoint;
                }
            }
        }
    }

    public boolean hasBunkerInNatural() {
        for(Unit building : game.self().getUnits()) {
            if(!building.getType().isBuilding()) {
                continue;
            }

            if(building.getType() == UnitType.Terran_Bunker && getNaturalTiles().contains(building.getTilePosition())) {
                return true;
            }
        }
        return false;
    }

    public ChokePoint getNaturalChoke() {
        return  naturalChokePoint;
    }

    public void setNaturalChoke() {
        naturalChokePoint = null;
        int closestDistance = Integer.MAX_VALUE;

        for(ChokePoint chokePoint : bwem.getMap().getChokePoints()) {
            if(chokePoint == getMainChoke()) {
                continue;
            }

            int distance = naturalBase.getLocation().getApproxDistance(chokePoint.getCenter().toTilePosition());

            if(distance < closestDistance) {
                naturalChokePoint = chokePoint;
                closestDistance = distance;
            }
        }
    }

    public HashSet<Base> getStartingBases() {
        return startingBases;
    }

    public HashSet<Mineral> getStartingMinerals() {
        return startingMinerals;
    }

    public HashSet<Geyser> getStartingGeysers() {
        return startingGeysers;
    }

    public Base getStartingBase() {
        return startingBase;
    }

    public Base getNaturalBase() {
        return naturalBase;
    }

    public HashSet<TilePosition> getBaseTiles() {
        return baseTiles;
    }

    public HashSet<TilePosition> getNaturalTiles() {
        return naturalTiles;
    }

    public HashSet<Base> getMapBases() {
        return mapBases;
    }

    public ArrayList<Base> getOrderedExpansions() {
        return orderedExpansions;
    }

    public HashSet<Base> getOwnedBases() {
        return ownedBases;
    }

    public HashMap<Base, TilePosition> getGeyserTiles() {
        return geyserTiles;
    }

    public HashSet<TilePosition> getUsedGeysers() {
        return usedGeysers;
    }

    public PathFinding getPathFinding() {
        return pathFinding;
    }

    public HashSet<ChokePoint> getChokePoints() {
        return chokePoints;
    }

    public AllBasePaths getAllBasePaths() {
        return allBasePaths;
    }

    public HashSet<TilePosition> getMainCliffEdge() {
        return mainCliffEdge;
    }

    public HashSet<TilePosition> getCombinedTankTiles() {
        return combinedTankTiles;
    }

    public HashSet<TilePosition> getBackupMainSiegeTiles() {
        return backupMainSiegeTiles;
    }

    public boolean isNaturalOwned() {
        return naturalOwned;
    }

    //onFrame used for debug painters
    public void onFrame() {
        painters.paintAllChokes();
        painters.paintNatural(naturalBase);
//        painters.paintTiles(mainCliffEdge);
//        painters.paintTiles(naturalChokeEdge);
        //painters.paintBasePosition(mapBases);
        //painters.paintTilePositions(pathTest);
        //painters.paintTiles(baseTiles);
//        painters.paintExpansionOrdering(orderedExpansions);
        //painters.paintMainBufferZone(startingBase);
    }

    public void onUnitCreate(Unit unit) {
        if(unit.getType() != UnitType.Terran_Command_Center) {
            return;
        }

        if(unit.getDistance(naturalBase.getCenter()) < 100) {
            baseTiles.addAll(naturalTiles);
            naturalOwned = true;
        }
    }

    public void onUnitComplete(Unit unit) {
        for(Base base : mapBases) {
            if(unit.getPosition().getApproxDistance(base.getLocation().toPosition()) < 100) {
                ownedBases.add(base);
                break;
            }
        }
    }

    public void onUnitDestroy(Unit unit) {
        for(Base base : ownedBases) {
            if(unit.getPosition().getApproxDistance(base.getLocation().toPosition()) < 100) {
                ownedBases.remove(base);
                break;
            }
        }

        if(unit.getDistance(naturalBase.getCenter()) < 100) {
            baseTiles.removeAll(naturalTiles);
            naturalOwned = false;
        }

    }
}
