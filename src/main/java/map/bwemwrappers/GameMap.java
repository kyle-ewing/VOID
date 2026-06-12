package map.bwemwrappers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import bwapi.Game;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.WalkPosition;
import bwem.BWEM;
import bwem.ChokePoint.Node;
import map.PathFinding;

public class GameMap {
    private Game game;
    private BWEM bwem;
    private PathFinding pathFinding;
    private ArrayList<Base> bases = new ArrayList<>();
    private ArrayList<Area> areas = new ArrayList<>();
    private ArrayList<ChokePoint> chokes = new ArrayList<>();
    private ArrayList<Mineral> minerals = new ArrayList<>();
    private ArrayList<Geyser> geysers = new ArrayList<>();
    private ArrayList<StaticBuilding> staticBuildings = new ArrayList<>();
    private HashMap<bwem.Area, Area> areasByBwemArea = new HashMap<>();
    private HashMap<bwem.Base, Base> basesByBwemBase = new HashMap<>();
    private HashMap<bwem.ChokePoint, ChokePoint> chokesByBwemChoke = new HashMap<>();
    private HashMap<Unit, Neutral> neutralsByUnit = new HashMap<>();
    private Area[][] areaByTile;
    private boolean[][] walkableByTile;
    private GroundHeight[][] heightByTile;
    private Base startingBase;

    public GameMap(BWEM bwem, Game game) {
        this.bwem = bwem;
        this.game = game;

        pathFinding = new PathFinding(bwem, game);

        init();
    }

    private void init() {
        setGrids();
        createNeutrals();
        createAreas();
        createChokePoints();
        createBases();
        connectAreas();
        setAreaTiles();
        setStartingBase();
        setPathsFromMain();
        setNaturalBases();
        markBaseAreas();
    }

    private void setGrids() {
        int mapWidth = game.mapWidth();
        int mapHeight = game.mapHeight();
        areaByTile = new Area[mapWidth][mapHeight];
        walkableByTile = new boolean[mapWidth][mapHeight];
        heightByTile = new GroundHeight[mapWidth][mapHeight];

        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                TilePosition tile = new TilePosition(x, y);
                walkableByTile[x][y] = bwem.getMap().getTile(tile).isWalkable();

                int height = game.getGroundHeight(tile);
                if (height >= 2) {
                    heightByTile[x][y] = GroundHeight.VERY_HIGH_GROUND;
                }
                else if (height == 1) {
                    heightByTile[x][y] = GroundHeight.HIGH_GROUND;
                }
                else {
                    heightByTile[x][y] = GroundHeight.LOW_GROUND;
                }
            }
        }
    }

    private void createNeutrals() {
        for (bwem.Mineral bwemMineral : bwem.getMap().getNeutralData().getMinerals()) {
            Mineral mineral = new Mineral(bwemMineral);
            minerals.add(mineral);
            neutralsByUnit.put(mineral.getUnit(), mineral);
        }

        for (bwem.Geyser bwemGeyser : bwem.getMap().getNeutralData().getGeysers()) {
            Geyser geyser = new Geyser(bwemGeyser);
            geysers.add(geyser);
            neutralsByUnit.put(geyser.getUnit(), geyser);
        }

        for (bwem.StaticBuilding bwemStaticBuilding : bwem.getMap().getNeutralData().getStaticBuildings()) {
            StaticBuilding staticBuilding = new StaticBuilding(bwemStaticBuilding);
            staticBuildings.add(staticBuilding);
            neutralsByUnit.put(staticBuilding.getUnit(), staticBuilding);
        }
    }

    private void createAreas() {
        for (bwem.Area bwemArea : bwem.getMap().getAreas()) {
            Area area = new Area(bwemArea);

            for (bwem.Mineral bwemMineral : bwemArea.getMinerals()) {
                Neutral neutral = neutralsByUnit.get(bwemMineral.getUnit());
                if (neutral != null) {
                    area.getMinerals().add((Mineral) neutral);
                }
            }

            for (bwem.Geyser bwemGeyser : bwemArea.getGeysers()) {
                Neutral neutral = neutralsByUnit.get(bwemGeyser.getUnit());
                if (neutral != null) {
                    area.getGeysers().add((Geyser) neutral);
                }
            }

            areas.add(area);
            areasByBwemArea.put(bwemArea, area);
        }
    }

    private void createChokePoints() {
        for (bwem.ChokePoint bwemChoke : bwem.getMap().getChokePoints()) {
            ChokePoint choke = new ChokePoint(bwemChoke);

            bwem.Area bwemFirst = bwemChoke.getAreas().getFirst();
            bwem.Area bwemSecond = bwemChoke.getAreas().getSecond();
            choke.setFirstArea(areasByBwemArea.get(bwemFirst));
            choke.setSecondArea(areasByBwemArea.get(bwemSecond));

            if (bwemChoke.getBlockingNeutral() != null) {
                choke.setBlockingNeutral(neutralsByUnit.get(bwemChoke.getBlockingNeutral().getUnit()));
            }

            WalkPosition end1Node = bwemChoke.getNodePosition(Node.END1);
            WalkPosition end2Node = bwemChoke.getNodePosition(Node.END2);
            Position end1 = marchToUnwalkable(end1Node, end2Node);
            Position end2 = marchToUnwalkable(end2Node, end1Node);
            choke.setEnd1(end1);
            choke.setEnd2(end2);
            choke.setWidth((int) end1.getDistance(end2));

            if (bwemFirst != null && bwemSecond != null) {
                TilePosition firstSideTile = bwemChoke.getNodePositionInArea(Node.MIDDLE, bwemFirst).toTilePosition();
                TilePosition secondSideTile = bwemChoke.getNodePositionInArea(Node.MIDDLE, bwemSecond).toTilePosition();
                GroundHeight firstHeight = getGroundHeight(firstSideTile);
                GroundHeight secondHeight = getGroundHeight(secondSideTile);

                if (firstHeight != null && secondHeight != null && firstHeight != secondHeight) {
                    choke.setHeightTransition(true);
                }
            }

            chokes.add(choke);
            chokesByBwemChoke.put(bwemChoke, choke);
        }
    }

    private Position marchToUnwalkable(WalkPosition from, WalkPosition away) {
        double directionX = from.getX() - away.getX();
        double directionY = from.getY() - away.getY();
        double magnitude = Math.sqrt(directionX * directionX + directionY * directionY);

        if (magnitude == 0) {
            return from.toPosition();
        }

        directionX = directionX / magnitude;
        directionY = directionY / magnitude;

        int walkWidth = game.mapWidth() * 4;
        int walkHeight = game.mapHeight() * 4;
        double currentX = from.getX();
        double currentY = from.getY();

        while (true) {
            currentX += directionX;
            currentY += directionY;

            int walkX = (int) Math.round(currentX);
            int walkY = (int) Math.round(currentY);

            if (walkX < 0 || walkY < 0 || walkX >= walkWidth || walkY >= walkHeight) {
                int clampedX = Math.max(0, Math.min(walkX, walkWidth - 1));
                int clampedY = Math.max(0, Math.min(walkY, walkHeight - 1));
                return new WalkPosition(clampedX, clampedY).toPosition();
            }

            if (!game.isWalkable(walkX, walkY)) {
                return new WalkPosition(walkX, walkY).toPosition();
            }
        }
    }

    private void createBases() {
        for (bwem.Base bwemBase : bwem.getMap().getBases()) {
            Base base = new Base(bwemBase);

            for (bwem.Mineral bwemMineral : bwemBase.getMinerals()) {
                Neutral neutral = neutralsByUnit.get(bwemMineral.getUnit());
                if (neutral != null) {
                    base.getMinerals().add((Mineral) neutral);
                }
            }

            for (bwem.Geyser bwemGeyser : bwemBase.getGeysers()) {
                Neutral neutral = neutralsByUnit.get(bwemGeyser.getUnit());
                if (neutral != null) {
                    Geyser geyser = (Geyser) neutral;
                    base.getGeysers().add(geyser);
                    geyser.setBase(base);
                }
            }

            for (bwem.Mineral bwemMineral : bwemBase.getBlockingMinerals()) {
                Neutral neutral = neutralsByUnit.get(bwemMineral.getUnit());
                if (neutral != null) {
                    base.getBlockingMinerals().add((Mineral) neutral);
                }
            }

            base.setLivePatchCount(base.getMinerals().size());

            if (base.getGeysers().isEmpty()) {
                base.setMinOnly(true);
            }

            Area area = areasByBwemArea.get(bwemBase.getArea());
            base.setArea(area);
            if (area != null) {
                area.getBases().add(base);
            }

            bases.add(base);
            basesByBwemBase.put(bwemBase, base);
        }
    }

    private void connectAreas() {
        for (ChokePoint choke : chokes) {
            Area firstArea = choke.getFirstArea();
            Area secondArea = choke.getSecondArea();

            if (firstArea == null || secondArea == null) {
                continue;
            }

            firstArea.getChokes().add(choke);
            secondArea.getChokes().add(choke);

            if (!firstArea.getNeighbors().contains(secondArea)) {
                firstArea.getNeighbors().add(secondArea);
            }
            if (!secondArea.getNeighbors().contains(firstArea)) {
                secondArea.getNeighbors().add(firstArea);
            }
        }
    }

    private void setAreaTiles() {
        int mapWidth = game.mapWidth();
        int mapHeight = game.mapHeight();

        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                TilePosition tile = new TilePosition(x, y);
                bwem.Area bwemArea = bwem.getMap().getArea(tile);

                if (bwemArea == null) {
                    continue;
                }

                Area area = areasByBwemArea.get(bwemArea);

                if (area == null) {
                    continue;
                }

                areaByTile[x][y] = area;
                area.getTiles().add(tile);
            }
        }
    }

    private void setStartingBase() {
        Position startPosition = game.self().getStartLocation().toPosition();
        int closestDistance = Integer.MAX_VALUE;

        for (Base base : bases) {
            if (!base.isStartingLocation()) {
                continue;
            }

            int distance = base.getLocation().toPosition().getApproxDistance(startPosition);
            if (distance < closestDistance) {
                closestDistance = distance;
                startingBase = base;
            }
        }

        if (startingBase != null) {
            startingBase.setOwnership(Ownership.SELF);
        }
    }

    private void setPathsFromMain() {
        if (startingBase == null) {
            return;
        }

        startingBase.setGroundDistanceFromMain(0);
        Position startingPosition = startingBase.getCenter();

        for (Base base : bases) {
            if (base == startingBase) {
                continue;
            }

            Position nearestWalkable = pathFinding.findNearestWalkable(base.getCenter());
            List<Position> path = pathFinding.findPath(startingPosition, nearestWalkable);

            if (path == null || path.isEmpty()) {
                continue;
            }

            base.setPathFromMain(path);
            base.setGroundDistanceFromMain(path.size());
        }
    }

    private void setNaturalBases() {
        for (Base startingLocationBase : bases) {
            if (!startingLocationBase.isStartingLocation()) {
                continue;
            }

            Base natural = null;
            int shortestPath = Integer.MAX_VALUE;

            for (Base other : bases) {
                if (other == startingLocationBase) {
                    continue;
                }

                if (other.getGeysers().isEmpty()) {
                    continue;
                }

                List<Position> path = pathFinding.findPath(startingLocationBase.getLocation().toPosition(), other.getLocation().toPosition());

                if (path == null || path.isEmpty()) {
                    continue;
                }

                if (path.size() < shortestPath) {
                    shortestPath = path.size();
                    natural = other;
                }
            }

            if (natural != null) {
                natural.setNatural(true);
            }
        }
    }

    private void markBaseAreas() {
        for (Base base : bases) {
            Area area = base.getArea();

            if (area == null) {
                continue;
            }

            if (base.isStartingLocation()) {
                area.setStartingArea(true);
            }

            if (base.isNatural()) {
                area.setNaturalArea(true);
            }
        }
    }

    public void onUnitDestroyed(Unit unit) {
        if (!unit.getType().isMineralField()) {
            return;
        }

        Neutral neutral = neutralsByUnit.get(unit);

        if (!(neutral instanceof Mineral)) {
            return;
        }

        Mineral mineral = (Mineral) neutral;
        mineral.markDestroyed();

        for (Base base : bases) {
            if (base.getMinerals().contains(mineral)) {
                base.decrementLivePatchCount();
            }
        }
    }

    public Area getArea(TilePosition tile) {
        if (tile.getX() < 0 || tile.getY() < 0 || tile.getX() >= game.mapWidth() || tile.getY() >= game.mapHeight()) {
            return null;
        }
        return areaByTile[tile.getX()][tile.getY()];
    }

    public Area getNearestArea(TilePosition tile) {
        bwem.Area bwemArea = bwem.getMap().getNearestArea(tile);

        if (bwemArea == null) {
            return null;
        }

        return areasByBwemArea.get(bwemArea);
    }

    public boolean isWalkable(TilePosition tile) {
        if (tile.getX() < 0 || tile.getY() < 0 || tile.getX() >= game.mapWidth() || tile.getY() >= game.mapHeight()) {
            return false;
        }
        return walkableByTile[tile.getX()][tile.getY()];
    }

    public GroundHeight getGroundHeight(TilePosition tile) {
        if (tile.getX() < 0 || tile.getY() < 0 || tile.getX() >= game.mapWidth() || tile.getY() >= game.mapHeight()) {
            return null;
        }
        return heightByTile[tile.getX()][tile.getY()];
    }

    public Area getArea(bwem.Area bwemArea) {
        return areasByBwemArea.get(bwemArea);
    }

    public Base getBase(bwem.Base bwemBase) {
        return basesByBwemBase.get(bwemBase);
    }

    public ChokePoint getChoke(bwem.ChokePoint bwemChoke) {
        return chokesByBwemChoke.get(bwemChoke);
    }

    public ArrayList<Base> getBases() {
        return bases;
    }

    public ArrayList<Area> getAreas() {
        return areas;
    }

    public ArrayList<ChokePoint> getChokes() {
        return chokes;
    }

    public ArrayList<Mineral> getMinerals() {
        return minerals;
    }

    public ArrayList<Geyser> getGeysers() {
        return geysers;
    }

    public ArrayList<StaticBuilding> getStaticBuildings() {
        return staticBuildings;
    }

    public Base getStartingBase() {
        return startingBase;
    }

    public PathFinding getPathFinding() {
        return pathFinding;
    }
}
