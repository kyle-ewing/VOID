package information;

import bwapi.*;
import bwem.*;
import debug.Painters;
import map.PathFinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class BaseInfo {
    private BWEM bwem;
    private Game game;
    private PathFinding pathFinding;
    private Base startingBase;
    private Base naturalBase;
    private ChokePoint chokePoint;
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
    private HashMap<Base, TilePosition> geyserTiles = new HashMap<>();
    private ArrayList<Base> orderedExpansions = new ArrayList<>();


    private Painters painters;

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
        setNaturalBase();
        setStartingBaseTiles();
        setNaturalBaseTiles();
        setOrderedExpansions();
        setGeyserTiles();
        setChokePoints();
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
                List<Position> path = pathFinding.findPath(startingBase.getLocation().toPosition(), base.getLocation().toPosition());
                int distance = path.size();

                if(distance == 0) {
                    continue;
                }

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
            List<Position> path = pathFinding.findPath(startingBase.getLocation().toPosition(), base.getLocation().toPosition());
            int distance = path.size();

            if(distance == 0 || base.getGeysers().isEmpty() || base == startingBase) {
                continue;
            }

            if(orderedExpansions.isEmpty()) {
                orderedExpansions.add(base);
            }
            else {
                for(int i = 0; i < orderedExpansions.size(); i++) {
                    Base currentBase = orderedExpansions.get(i);
                    List<Position> currentPath = pathFinding.findPath(startingBase.getLocation().toPosition(), currentBase.getLocation().toPosition());
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

    public ChokePoint getMainChoke() {
        if(startingBase == null || naturalBase == null) {
            return null;
        }

        List<Position> path = pathFinding.findPath(startingBase.getLocation().toPosition(), naturalBase.getLocation().toPosition()
        );

        if(path.isEmpty()) {
            return null;
        }

        ChokePoint closestChokePoint = null;
        int minDistance = Integer.MAX_VALUE;

        for(ChokePoint chokePoint : bwem.getMap().getChokePoints()) {
            Position chokePos = chokePoint.getCenter().toPosition();

            for(Position pathPos : path) {
                int distance = chokePos.getApproxDistance(pathPos);
                if(distance < minDistance) {
                    minDistance = distance;
                    closestChokePoint = chokePoint;
                }
            }
        }
        return closestChokePoint;
    }

    public ChokePoint getNaturalChoke() {
        ChokePoint closestChokePoint = null;
        int closestDistance = Integer.MAX_VALUE;

        for(ChokePoint chokePoint : bwem.getMap().getChokePoints()) {
            if(chokePoint == getMainChoke()) {
                continue;
            }

            int distance = naturalBase.getLocation().getApproxDistance(chokePoint.getCenter().toTilePosition());

            if(distance < closestDistance) {
                closestChokePoint = chokePoint;
                closestDistance = distance;
            }
        }
        return closestChokePoint;
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

    //onFrame used for debug painters
    public void onFrame() {
        painters.paintAllChokes();
        painters.paintNatural(naturalBase);
        //painters.paintBasePosition(mapBases);
        //painters.paintTilePositions(pathTest);
        //painters.paintTiles(baseTiles);
//        painters.paintExpansionOrdering(orderedExpansions);
        //painters.paintMainBufferZone(startingBase);
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
    }
}
