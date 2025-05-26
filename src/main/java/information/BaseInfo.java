package information;

import bwapi.*;
import bwem.*;
import debug.Painters;
import map.PathFinding;

import java.util.ArrayList;
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
    private ArrayList<TilePosition> baseTiles = new ArrayList<>();

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
        setNaturalBase();
        setStartingBaseTiles();
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

    public ArrayList<TilePosition> getTilesForBase(Base base) {
        ArrayList<TilePosition> tiles = new ArrayList<>();
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

    private void setStartingMineralPatches() {
        for(Mineral mineral : startingBase.getMinerals()) {
            startingMinerals.add(mineral);
        }
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

                //Exclude islands
                if(distance == 0) {
                    continue;
                }

                //Exclude bases with no geysers
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

    public ChokePoint getMainChoke() {
        ChokePoint closestChokePoint = null;
        int closestDistance = Integer.MAX_VALUE;

        for (ChokePoint chokePoint : bwem.getMap().getChokePoints()) {
            int distance = startingBase.getLocation().getApproxDistance(chokePoint.getCenter().toTilePosition());
            if (distance < closestDistance) {
                closestChokePoint = chokePoint;
                closestDistance = distance;
            }
        }
        return closestChokePoint;
    }

    public ChokePoint getNaturalChoke() {
        ChokePoint closestChokePoint = null;
        int closestDistance = Integer.MAX_VALUE;

        for (ChokePoint chokePoint : bwem.getMap().getChokePoints()) {
            int distance = naturalBase.getLocation().getApproxDistance(chokePoint.getCenter().toTilePosition());
            if (distance < closestDistance) {
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

    public Base getStartingBase() {
        return startingBase;
    }

    public Base getNaturalBase() {
        return naturalBase;
    }

    public ArrayList<TilePosition> getBaseTiles() {
        return baseTiles;
    }

    public HashSet<Base> getMapBases() {
        return mapBases;
    }

    //onFrame used for debug painters
    public void onFrame() {
        painters.paintAllChokes();
        painters.paintNatural(naturalBase);
        //painters.paintBasePosition(mapBases);
        //painters.paintTilePositions(pathTest);
        //painters.paintTiles(baseTiles);
    }
}
