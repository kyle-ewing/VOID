package map.bwemwrappers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
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
    private HashMap<bwem.Area, ArrayList<Area>> subAreasByParent = new HashMap<>();
    private HashMap<Base, Base> naturalsByStartingBase = new HashMap<>();
    private Area[][] areaByTile;
    private boolean[][] walkableByTile;
    private GroundHeight[][] heightByTile;
    private Base startingBase;
    private int nextSyntheticAreaId;

    public GameMap(Game game) {
        this.game = game;

        bwem = new BWEM(game);
        bwem.initialize();

        init();
    }

    private void init() {
        setGrids();
        pathFinding = new PathFinding(walkableByTile, game);
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
        splitAreas();
        cutSnakingAreas();
        rehomeChokes();
        rebuildAreaWiring();
        rehomeBasesAndResources();
        mergeSmallAreasOutsideNatural();
        validateGraph();
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

                int height = game.getGroundHeight(tile) / 2;
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
                naturalsByStartingBase.put(startingLocationBase, natural);
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

    private void splitAreas() {
        HashSet<Area> exemptAreas = new HashSet<>();

        for (Area area : areas) {
            if (area.isStartingArea() || area.isNaturalArea()) {
                exemptAreas.add(area);
            }
        }

        exemptAreas.addAll(minOnlyExemptAreas());

        nextSyntheticAreaId = 0;
        for (Area area : areas) {
            if (area.getId() > nextSyntheticAreaId) {
                nextSyntheticAreaId = area.getId();
            }
        }
        nextSyntheticAreaId++;

        for (Area area : new ArrayList<>(areas)) {
            if (area.getTiles().isEmpty()) {
                continue;
            }

            GroundHeight uniformHeight = null;
            boolean uniform = true;

            for (TilePosition tile : area.getTiles()) {
                GroundHeight tileHeight = getGroundHeight(tile);

                if (uniformHeight == null) {
                    uniformHeight = tileHeight;
                }
                else if (tileHeight != uniformHeight) {
                    uniform = false;
                    break;
                }
            }

            if (uniform) {
                area.setGroundHeight(uniformHeight);
                continue;
            }

            if (exemptAreas.contains(area)) {
                area.setGroundHeight(dominantHeight(area));
                continue;
            }

            ArrayList<HashSet<TilePosition>> components = new ArrayList<>();
            ArrayList<GroundHeight> componentHeights = new ArrayList<>();
            floodFillByHeight(area, components, componentHeights);
            mergeSmallComponents(components, componentHeights);

            if (components.size() == 1) {
                area.setGroundHeight(componentHeights.get(0));
                continue;
            }

            ArrayList<Area> subAreas = new ArrayList<>();

            for (int i = 0; i < components.size(); i++) {
                Area subArea = new Area(nextSyntheticAreaId, area.getBwemArea(), componentHeights.get(i), components.get(i));
                nextSyntheticAreaId++;
                subAreas.add(subArea);

                for (TilePosition tile : components.get(i)) {
                    areaByTile[tile.getX()][tile.getY()] = subArea;
                }
            }

            areas.remove(area);
            areas.addAll(subAreas);
            subAreasByParent.put(area.getBwemArea(), subAreas);

            Area largestSubArea = subAreas.get(0);
            for (Area subArea : subAreas) {
                if (subArea.getTiles().size() > largestSubArea.getTiles().size()) {
                    largestSubArea = subArea;
                }
            }
            areasByBwemArea.put(area.getBwemArea(), largestSubArea);

            createFrontierChokes(subAreas);
        }
    }

    private GroundHeight dominantHeight(Area area) {
        int lowCount = 0;
        int highCount = 0;
        int veryHighCount = 0;

        for (TilePosition tile : area.getTiles()) {
            GroundHeight tileHeight = getGroundHeight(tile);

            if (tileHeight == GroundHeight.LOW_GROUND) {
                lowCount++;
            }
            else if (tileHeight == GroundHeight.HIGH_GROUND) {
                highCount++;
            }
            else {
                veryHighCount++;
            }
        }

        if (lowCount >= highCount && lowCount >= veryHighCount) {
            return GroundHeight.LOW_GROUND;
        }

        if (highCount >= veryHighCount) {
            return GroundHeight.HIGH_GROUND;
        }

        return GroundHeight.VERY_HIGH_GROUND;
    }

    private HashSet<Area> minOnlyExemptAreas() {
        HashSet<Area> exempt = new HashSet<>();

        for (Base startingLocationBase : naturalsByStartingBase.keySet()) {
            Base natural = naturalsByStartingBase.get(startingLocationBase);
            List<Position> path = pathFinding.findPath(startingLocationBase.getLocation().toPosition(), natural.getLocation().toPosition());

            if (path == null || path.isEmpty()) {
                continue;
            }

            ChokePoint mainChoke = null;
            int minDistance = Integer.MAX_VALUE;

            for (ChokePoint choke : chokes) {
                for (Position pathPos : path) {
                    int distance = choke.getCenter().getApproxDistance(pathPos);

                    if (distance < minDistance) {
                        minDistance = distance;
                        mainChoke = choke;
                    }
                }
            }

            if (mainChoke == null) {
                continue;
            }

            Area firstArea = mainChoke.getFirstArea();
            Area secondArea = mainChoke.getSecondArea();

            if (firstArea == null || secondArea == null) {
                continue;
            }

            if (firstArea.getBases().contains(startingLocationBase) || secondArea.getBases().contains(startingLocationBase)) {
                continue;
            }

            for (Base areaBase : firstArea.getBases()) {
                if (areaBase != natural) {
                    exempt.add(firstArea);
                }
            }

            for (Base areaBase : secondArea.getBases()) {
                if (areaBase != natural) {
                    exempt.add(secondArea);
                }
            }
        }

        return exempt;
    }

    private void floodFillByHeight(Area area, ArrayList<HashSet<TilePosition>> components, ArrayList<GroundHeight> componentHeights) {
        HashSet<TilePosition> remaining = new HashSet<>(area.getTiles());
        int[][] offsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        while (!remaining.isEmpty()) {
            TilePosition seed = remaining.iterator().next();
            GroundHeight seedHeight = getGroundHeight(seed);
            HashSet<TilePosition> component = new HashSet<>();
            Deque<TilePosition> queue = new ArrayDeque<>();

            remaining.remove(seed);
            component.add(seed);
            queue.add(seed);

            while (!queue.isEmpty()) {
                TilePosition current = queue.poll();

                for (int[] offset : offsets) {
                    TilePosition neighborTile = new TilePosition(current.getX() + offset[0], current.getY() + offset[1]);

                    if (!remaining.contains(neighborTile)) {
                        continue;
                    }

                    if (getGroundHeight(neighborTile) != seedHeight) {
                        continue;
                    }

                    remaining.remove(neighborTile);
                    component.add(neighborTile);
                    queue.add(neighborTile);
                }
            }

            components.add(component);
            componentHeights.add(seedHeight);
        }
    }

    private void mergeSmallComponents(ArrayList<HashSet<TilePosition>> components, ArrayList<GroundHeight> componentHeights) {
        boolean merged = true;

        while (merged && components.size() > 1) {
            merged = false;

            for (int i = 0; i < components.size(); i++) {
                if (components.get(i).size() >= 70) {
                    continue;
                }

                int largestNeighbor = -1;
                int largestNeighborSize = -1;

                for (int j = 0; j < components.size(); j++) {
                    if (j == i) {
                        continue;
                    }

                    if (!componentsAdjacent(components.get(i), components.get(j))) {
                        continue;
                    }

                    if (components.get(j).size() > largestNeighborSize) {
                        largestNeighborSize = components.get(j).size();
                        largestNeighbor = j;
                    }
                }

                if (largestNeighbor < 0) {
                    continue;
                }

                components.get(largestNeighbor).addAll(components.get(i));
                components.remove(i);
                componentHeights.remove(i);
                merged = true;
                break;
            }
        }
    }

    private boolean componentsAdjacent(HashSet<TilePosition> first, HashSet<TilePosition> second) {
        HashSet<TilePosition> smaller;
        HashSet<TilePosition> larger;

        if (first.size() <= second.size()) {
            smaller = first;
            larger = second;
        }
        else {
            smaller = second;
            larger = first;
        }

        int[][] offsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        for (TilePosition tile : smaller) {
            for (int[] offset : offsets) {
                if (larger.contains(new TilePosition(tile.getX() + offset[0], tile.getY() + offset[1]))) {
                    return true;
                }
            }
        }

        return false;
    }

    private void createFrontierChokes(ArrayList<Area> targetAreas) {
        HashSet<Area> targetSet = new HashSet<>(targetAreas);
        HashMap<Long, ArrayList<int[]>> edgesByPair = new HashMap<>();
        HashMap<Long, Area[]> areasByPair = new HashMap<>();
        int[][] offsets = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};

        for (Area target : targetAreas) {
            for (TilePosition tile : target.getTiles()) {
                for (int[] offset : offsets) {
                    TilePosition neighborTile = new TilePosition(tile.getX() + offset[0], tile.getY() + offset[1]);
                    Area neighbor = getArea(neighborTile);

                    if (neighbor == null || neighbor == target) {
                        continue;
                    }

                    if (neighbor.getBwemArea() != target.getBwemArea()) {
                        continue;
                    }

                    boolean forward = offset[0] > 0 || offset[1] > 0;

                    if (!forward && targetSet.contains(neighbor)) {
                        continue;
                    }

                    int[] edge;
                    if (forward) {
                        edge = new int[]{tile.getX(), tile.getY(), neighborTile.getX(), neighborTile.getY()};
                    }
                    else {
                        edge = new int[]{neighborTile.getX(), neighborTile.getY(), tile.getX(), tile.getY()};
                    }

                    Area lowArea;
                    Area highArea;
                    if (target.getId() < neighbor.getId()) {
                        lowArea = target;
                        highArea = neighbor;
                    }
                    else {
                        lowArea = neighbor;
                        highArea = target;
                    }

                    long pairKey = ((long) lowArea.getId() << 32) | highArea.getId();
                    edgesByPair.computeIfAbsent(pairKey, k -> new ArrayList<>()).add(edge);
                    areasByPair.put(pairKey, new Area[]{lowArea, highArea});
                }
            }
        }

        for (Long pairKey : edgesByPair.keySet()) {
            Area[] pairAreas = areasByPair.get(pairKey);
            ArrayList<int[]> edges = edgesByPair.get(pairKey);
            boolean[] assigned = new boolean[edges.size()];

            for (int i = 0; i < edges.size(); i++) {
                if (assigned[i]) {
                    continue;
                }

                ArrayList<int[]> segment = new ArrayList<>();
                Deque<Integer> queue = new ArrayDeque<>();
                assigned[i] = true;
                queue.add(i);

                while (!queue.isEmpty()) {
                    int current = queue.poll();
                    segment.add(edges.get(current));

                    for (int j = 0; j < edges.size(); j++) {
                        if (assigned[j]) {
                            continue;
                        }

                        if (edgesTouch(edges.get(current), edges.get(j))) {
                            assigned[j] = true;
                            queue.add(j);
                        }
                    }
                }

                buildChokeFromSegment(segment, pairAreas[0], pairAreas[1]);
            }
        }
    }

    private boolean edgesTouch(int[] firstEdge, int[] secondEdge) {
        int[][] firstTiles = {{firstEdge[0], firstEdge[1]}, {firstEdge[2], firstEdge[3]}};
        int[][] secondTiles = {{secondEdge[0], secondEdge[1]}, {secondEdge[2], secondEdge[3]}};

        for (int[] firstTile : firstTiles) {
            for (int[] secondTile : secondTiles) {
                if (Math.abs(firstTile[0] - secondTile[0]) <= 1 && Math.abs(firstTile[1] - secondTile[1]) <= 1) {
                    return true;
                }
            }
        }

        return false;
    }

    private void buildChokeFromSegment(ArrayList<int[]> segment, Area firstArea, Area secondArea) {
        ArrayList<WalkPosition> geometry = new ArrayList<>();

        for (int[] edge : segment) {
            boolean horizontalAdjacency = edge[2] - edge[0] == 1;

            for (int i = 0; i < 4; i++) {
                if (horizontalAdjacency) {
                    geometry.add(new WalkPosition(edge[2] * 4, edge[1] * 4 + i));
                }
                else {
                    geometry.add(new WalkPosition(edge[0] * 4 + i, edge[3] * 4));
                }
            }
        }

        if (geometry.isEmpty()) {
            return;
        }

        WalkPosition firstExtreme = geometry.get(0);
        WalkPosition secondExtreme = geometry.get(0);
        int longestSpan = -1;

        for (WalkPosition first : geometry) {
            for (WalkPosition second : geometry) {
                int span = first.toPosition().getApproxDistance(second.toPosition());

                if (span > longestSpan) {
                    longestSpan = span;
                    firstExtreme = first;
                    secondExtreme = second;
                }
            }
        }

        Position bestEnd1 = marchToUnwalkable(firstExtreme, secondExtreme);
        Position bestEnd2 = marchToUnwalkable(secondExtreme, firstExtreme);
        int bestWidth = (int) bestEnd1.getDistance(bestEnd2);

        Position midpoint = new Position((bestEnd1.getX() + bestEnd2.getX()) / 2, (bestEnd1.getY() + bestEnd2.getY()) / 2);
        WalkPosition centerWalk = geometry.get(0);
        int closestDistance = Integer.MAX_VALUE;

        for (WalkPosition boundaryWalk : geometry) {
            int distance = boundaryWalk.toPosition().getApproxDistance(midpoint);

            if (distance < closestDistance) {
                closestDistance = distance;
                centerWalk = boundaryWalk;
            }
        }

        Position center = centerWalk.toPosition();

        for (ChokePoint existing : chokes) {
            if (existing.getBwemChoke() == null) {
                continue;
            }

            for (WalkPosition boundaryWalk : geometry) {
                if (existing.getCenter().getApproxDistance(boundaryWalk.toPosition()) < 64) {
                    return;
                }
            }
        }

        ChokePoint choke = new ChokePoint(center, firstArea, secondArea, geometry, bestEnd1, bestEnd2, bestWidth);

        if (firstArea.getGroundHeight() != null && firstArea.getGroundHeight() == secondArea.getGroundHeight()) {
            choke.setHeightTransition(false);
        }

        chokes.add(choke);
        firstArea.getChokes().add(choke);
        secondArea.getChokes().add(choke);

        if (!firstArea.getNeighbors().contains(secondArea)) {
            firstArea.getNeighbors().add(secondArea);
        }
        if (!secondArea.getNeighbors().contains(firstArea)) {
            secondArea.getNeighbors().add(firstArea);
        }
    }

    private void cutSnakingAreas() {
        HashSet<ChokePoint> uncuttable = new HashSet<>();
        boolean cut = true;

        while (cut) {
            cut = false;

            for (ChokePoint choke : new ArrayList<>(chokes)) {
                if (!choke.isSynthetic() || uncuttable.contains(choke)) {
                    continue;
                }

                if (choke.getWidth() <= 1100) {
                    continue;
                }

                Area thinSide = null;
                if (isThinStrip(choke.getFirstArea(), choke)) {
                    thinSide = choke.getFirstArea();
                }
                else if (isThinStrip(choke.getSecondArea(), choke)) {
                    thinSide = choke.getSecondArea();
                }

                if (thinSide == null) {
                    uncuttable.add(choke);
                    continue;
                }

                if (cutAreaInHalf(thinSide, choke)) {
                    cut = true;
                    break;
                }

                uncuttable.add(choke);
            }
        }

        for (ChokePoint choke : new ArrayList<>(chokes)) {
            if (!choke.isSynthetic()) {
                continue;
            }

            if (choke.getWidth() <= 1100) {
                continue;
            }

            chokes.remove(choke);

            Area firstArea = choke.getFirstArea();
            Area secondArea = choke.getSecondArea();

            if (firstArea != null) {
                firstArea.getChokes().remove(choke);
            }
            if (secondArea != null) {
                secondArea.getChokes().remove(choke);
            }

            if (firstArea == null || secondArea == null) {
                continue;
            }

            boolean stillConnected = false;
            for (ChokePoint remaining : firstArea.getChokes()) {
                if (remaining.getOtherArea(firstArea) == secondArea) {
                    stillConnected = true;
                    break;
                }
            }

            if (!stillConnected) {
                firstArea.getNeighbors().remove(secondArea);
                secondArea.getNeighbors().remove(firstArea);
            }
        }
    }

    private boolean isThinStrip(Area area, ChokePoint divide) {
        if (area == null) {
            return false;
        }

        HashSet<TilePosition> seeds = new HashSet<>();

        for (WalkPosition boundaryWalk : divide.getGeometry()) {
            TilePosition geometryTile = boundaryWalk.toTilePosition();

            for (int offsetX = -1; offsetX <= 1; offsetX++) {
                for (int offsetY = -1; offsetY <= 1; offsetY++) {
                    TilePosition candidateTile = new TilePosition(geometryTile.getX() + offsetX, geometryTile.getY() + offsetY);

                    if (area.getTiles().contains(candidateTile)) {
                        seeds.add(candidateTile);
                    }
                }
            }
        }

        if (seeds.isEmpty()) {
            return false;
        }

        HashMap<TilePosition, Integer> depths = new HashMap<>();
        Deque<TilePosition> queue = new ArrayDeque<>(seeds);

        for (TilePosition seed : seeds) {
            depths.put(seed, 1);
        }

        int[][] offsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        while (!queue.isEmpty()) {
            TilePosition current = queue.poll();
            int depth = depths.get(current);

            if (depth >= 4) {
                continue;
            }

            for (int[] offset : offsets) {
                TilePosition neighborTile = new TilePosition(current.getX() + offset[0], current.getY() + offset[1]);

                if (depths.containsKey(neighborTile) || !area.getTiles().contains(neighborTile)) {
                    continue;
                }

                depths.put(neighborTile, depth + 1);
                queue.add(neighborTile);
            }
        }

        return depths.size() == area.getTiles().size();
    }

    private boolean cutAreaInHalf(Area snake, ChokePoint divide) {
        TilePosition firstSeed = null;
        TilePosition secondSeed = null;
        int firstSeedDistance = Integer.MAX_VALUE;
        int secondSeedDistance = Integer.MAX_VALUE;

        for (TilePosition tile : snake.getTiles()) {
            Position tilePosition = tile.toPosition();
            int distanceToEnd1 = tilePosition.getApproxDistance(divide.getEnd1());
            int distanceToEnd2 = tilePosition.getApproxDistance(divide.getEnd2());

            if (distanceToEnd1 < firstSeedDistance) {
                firstSeedDistance = distanceToEnd1;
                firstSeed = tile;
            }

            if (distanceToEnd2 < secondSeedDistance) {
                secondSeedDistance = distanceToEnd2;
                secondSeed = tile;
            }
        }

        if (firstSeed == null || secondSeed == null || firstSeed.equals(secondSeed)) {
            return false;
        }

        HashMap<TilePosition, Integer> labels = new HashMap<>();
        Deque<TilePosition> queue = new ArrayDeque<>();
        labels.put(firstSeed, 0);
        labels.put(secondSeed, 1);
        queue.add(firstSeed);
        queue.add(secondSeed);

        int[][] offsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        while (!queue.isEmpty()) {
            TilePosition current = queue.poll();
            int label = labels.get(current);

            for (int[] offset : offsets) {
                TilePosition neighborTile = new TilePosition(current.getX() + offset[0], current.getY() + offset[1]);

                if (labels.containsKey(neighborTile) || !snake.getTiles().contains(neighborTile)) {
                    continue;
                }

                labels.put(neighborTile, label);
                queue.add(neighborTile);
            }
        }

        HashSet<TilePosition> firstHalf = new HashSet<>();
        HashSet<TilePosition> secondHalf = new HashSet<>();

        for (TilePosition tile : snake.getTiles()) {
            Integer label = labels.get(tile);

            if (label == null || label == 0) {
                firstHalf.add(tile);
            }
            else {
                secondHalf.add(tile);
            }
        }

        if (firstHalf.isEmpty() || secondHalf.isEmpty()) {
            return false;
        }

        Area firstHalfArea = new Area(nextSyntheticAreaId, snake.getBwemArea(), snake.getGroundHeight(), firstHalf);
        nextSyntheticAreaId++;
        Area secondHalfArea = new Area(nextSyntheticAreaId, snake.getBwemArea(), snake.getGroundHeight(), secondHalf);
        nextSyntheticAreaId++;

        for (TilePosition tile : firstHalf) {
            areaByTile[tile.getX()][tile.getY()] = firstHalfArea;
        }
        for (TilePosition tile : secondHalf) {
            areaByTile[tile.getX()][tile.getY()] = secondHalfArea;
        }

        areas.remove(snake);
        areas.add(firstHalfArea);
        areas.add(secondHalfArea);

        ArrayList<Area> siblings = subAreasByParent.get(snake.getBwemArea());

        if (siblings != null) {
            siblings.remove(snake);
            siblings.add(firstHalfArea);
            siblings.add(secondHalfArea);

            Area largestSubArea = siblings.get(0);
            for (Area sibling : siblings) {
                if (sibling.getTiles().size() > largestSubArea.getTiles().size()) {
                    largestSubArea = sibling;
                }
            }
            areasByBwemArea.put(snake.getBwemArea(), largestSubArea);
        }

        for (ChokePoint attached : new ArrayList<>(chokes)) {
            if (attached.getFirstArea() != snake && attached.getSecondArea() != snake) {
                continue;
            }

            chokes.remove(attached);
            Area other = attached.getOtherArea(snake);

            if (other != null) {
                other.getChokes().remove(attached);
                other.getNeighbors().remove(snake);
            }
        }

        ArrayList<Area> halves = new ArrayList<>();
        halves.add(firstHalfArea);
        halves.add(secondHalfArea);
        createFrontierChokes(halves);

        return true;
    }

    private void rehomeChokes() {
        for (ChokePoint choke : chokes) {
            bwem.ChokePoint bwemChoke = choke.getBwemChoke();

            if (bwemChoke == null) {
                continue;
            }

            bwem.Area bwemFirst = bwemChoke.getAreas().getFirst();
            bwem.Area bwemSecond = bwemChoke.getAreas().getSecond();

            if (bwemFirst != null) {
                choke.setFirstArea(resolveChokeSide(bwemChoke, bwemFirst));
            }
            if (bwemSecond != null) {
                choke.setSecondArea(resolveChokeSide(bwemChoke, bwemSecond));
            }

            Area firstArea = choke.getFirstArea();
            Area secondArea = choke.getSecondArea();

            if (firstArea == null || secondArea == null) {
                continue;
            }

            if (firstArea.getGroundHeight() != null && secondArea.getGroundHeight() != null) {
                choke.setHeightTransition(firstArea.getGroundHeight() != secondArea.getGroundHeight());
            }
        }
    }

    private Area resolveChokeSide(bwem.ChokePoint bwemChoke, bwem.Area bwemSide) {
        if (!subAreasByParent.containsKey(bwemSide)) {
            return areasByBwemArea.get(bwemSide);
        }

        ArrayList<Area> subAreas = subAreasByParent.get(bwemSide);
        TilePosition sideTile = bwemChoke.getNodePositionInArea(Node.MIDDLE, bwemSide).toTilePosition();
        Area resolved = getArea(sideTile);

        if (resolved != null && subAreas.contains(resolved)) {
            return resolved;
        }

        Position sidePosition = sideTile.toPosition();
        Area nearest = null;
        int closestDistance = Integer.MAX_VALUE;

        for (Area subArea : subAreas) {
            for (TilePosition tile : subArea.getTiles()) {
                int distance = tile.toPosition().getApproxDistance(sidePosition);

                if (distance < closestDistance) {
                    closestDistance = distance;
                    nearest = subArea;
                }
            }
        }

        return nearest;
    }

    private void rebuildAreaWiring() {
        for (Area area : areas) {
            area.getChokes().clear();
            area.getNeighbors().clear();
        }

        for (ChokePoint choke : chokes) {
            Area firstArea = choke.getFirstArea();
            Area secondArea = choke.getSecondArea();

            if (firstArea == null || secondArea == null || firstArea == secondArea) {
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

    private void rehomeBasesAndResources() {
        for (Area area : areas) {
            area.getBases().clear();
            area.getMinerals().clear();
            area.getGeysers().clear();
        }

        for (Base base : bases) {
            Area area = getArea(base.getLocation());

            if (area == null) {
                area = getNearestArea(base.getLocation());
            }

            base.setArea(area);

            if (area == null) {
                continue;
            }

            area.getBases().add(base);

            if (base.isStartingLocation()) {
                area.setStartingArea(true);
            }
            if (base.isNatural()) {
                area.setNaturalArea(true);
            }
        }

        for (Mineral mineral : minerals) {
            Area area = getArea(mineral.getTopLeft());

            if (area == null) {
                continue;
            }

            area.getMinerals().add(mineral);
        }

        for (Geyser geyser : geysers) {
            Area area = getArea(geyser.getTopLeft());

            if (area == null && geyser.getBase() != null) {
                area = geyser.getBase().getArea();
            }

            if (area == null) {
                continue;
            }

            area.getGeysers().add(geyser);
        }
    }

    private void mergeSmallAreasOutsideNatural() {
        boolean merged = true;

        while (merged) {
            merged = false;

            Area smallestCandidate = null;
            Area mergeTarget = null;
            Area candidateNatural = null;

            for (Base startingLocationBase : naturalsByStartingBase.keySet()) {
                Base natural = naturalsByStartingBase.get(startingLocationBase);
                Area naturalArea = natural.getArea();

                if (naturalArea == null) {
                    continue;
                }

                for (Area candidate : naturalArea.getNeighbors()) {
                    if (candidate == naturalArea) {
                        continue;
                    }

                    if (candidate.isStartingArea() || candidate.isNaturalArea()) {
                        continue;
                    }

                    if (!candidate.getBases().isEmpty()) {
                        continue;
                    }

                    if (candidate.getTiles().size() >= 200) {
                        continue;
                    }

                    if (smallestCandidate != null && candidate.getTiles().size() >= smallestCandidate.getTiles().size()) {
                        continue;
                    }

                    Area target = largestNonNaturalNeighbor(candidate, naturalArea);

                    if (target == null) {
                        continue;
                    }

                    smallestCandidate = candidate;
                    mergeTarget = target;
                    candidateNatural = naturalArea;
                }
            }

            if (smallestCandidate == null) {
                continue;
            }

            absorbArea(smallestCandidate, mergeTarget);
            rebuildAreaWiring();
            merged = true;
        }
    }

    private Area largestNonNaturalNeighbor(Area candidate, Area naturalArea) {
        Area target = null;
        int largestSize = -1;

        for (Area neighbor : candidate.getNeighbors()) {
            if (neighbor == candidate || neighbor == naturalArea) {
                continue;
            }

            if (neighbor.isNaturalArea()) {
                continue;
            }

            if (neighbor.getBases().isEmpty() && neighbor.getTiles().size() > 600) {
                continue;
            }

            if (neighbor.getTiles().size() > largestSize) {
                largestSize = neighbor.getTiles().size();
                target = neighbor;
            }
        }

        return target;
    }

    private void absorbArea(Area candidate, Area target) {
        for (TilePosition tile : candidate.getTiles()) {
            areaByTile[tile.getX()][tile.getY()] = target;
        }

        target.getTiles().addAll(candidate.getTiles());
        target.getMinerals().addAll(candidate.getMinerals());
        target.getGeysers().addAll(candidate.getGeysers());
        target.getBases().addAll(candidate.getBases());

        for (Base base : candidate.getBases()) {
            base.setArea(target);
        }

        for (ChokePoint choke : new ArrayList<>(chokes)) {
            if (choke.getFirstArea() != candidate && choke.getSecondArea() != candidate) {
                continue;
            }

            Area other = choke.getOtherArea(candidate);

            if (other == target) {
                chokes.remove(choke);
                continue;
            }

            if (choke.getFirstArea() == candidate) {
                choke.setFirstArea(target);
            }
            else {
                choke.setSecondArea(target);
            }
        }

        areas.remove(candidate);

        ArrayList<Area> siblings = subAreasByParent.get(candidate.getBwemArea());

        if (siblings == null) {
            if (areasByBwemArea.get(candidate.getBwemArea()) == candidate) {
                areasByBwemArea.put(candidate.getBwemArea(), target);
            }
            return;
        }

        siblings.remove(candidate);

        if (siblings.isEmpty()) {
            subAreasByParent.remove(candidate.getBwemArea());
            if (areasByBwemArea.get(candidate.getBwemArea()) == candidate) {
                areasByBwemArea.put(candidate.getBwemArea(), target);
            }
            return;
        }

        if (areasByBwemArea.get(candidate.getBwemArea()) != candidate) {
            return;
        }

        Area largestSubArea = siblings.get(0);
        for (Area sibling : siblings) {
            if (sibling.getTiles().size() > largestSubArea.getTiles().size()) {
                largestSubArea = sibling;
            }
        }
        areasByBwemArea.put(candidate.getBwemArea(), largestSubArea);
    }

    private void validateGraph() {
        int tilesWithoutArea = 0;
        int mapWidth = game.mapWidth();
        int mapHeight = game.mapHeight();

        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                if (walkableByTile[x][y] && areaByTile[x][y] == null) {
                    tilesWithoutArea++;
                }
            }
        }

        HashSet<Area> liveAreas = new HashSet<>(areas);
        int chokesWithDeadAreas = 0;

        for (ChokePoint choke : chokes) {
            if (choke.getFirstArea() == null || choke.getSecondArea() == null) {
                chokesWithDeadAreas++;
                continue;
            }

            if (!liveAreas.contains(choke.getFirstArea()) || !liveAreas.contains(choke.getSecondArea())) {
                chokesWithDeadAreas++;
            }
        }

        HashMap<Area, Integer> componentIds = new HashMap<>();
        int componentId = 0;

        for (Area area : areas) {
            if (componentIds.containsKey(area)) {
                continue;
            }

            Deque<Area> queue = new ArrayDeque<>();
            componentIds.put(area, componentId);
            queue.add(area);

            while (!queue.isEmpty()) {
                Area current = queue.poll();

                for (ChokePoint choke : current.getChokes()) {
                    if (choke.isBlocked()) {
                        continue;
                    }

                    Area neighbor = choke.getOtherArea(current);

                    if (neighbor == null || componentIds.containsKey(neighbor)) {
                        continue;
                    }

                    componentIds.put(neighbor, componentId);
                    queue.add(neighbor);
                }
            }

            componentId++;
        }

        int accessibilityMismatches = 0;

        for (Area first : areas) {
            for (Area second : areas) {
                if (first == second || first.isSynthetic() || second.isSynthetic()) {
                    continue;
                }

                boolean bwemAccessible = first.getBwemArea().isAccessibleFrom(second.getBwemArea());
                boolean wrapperAccessible = componentIds.get(first).equals(componentIds.get(second));

                if (bwemAccessible != wrapperAccessible) {
                    accessibilityMismatches++;
                }
            }
        }

        if (tilesWithoutArea > 0) {
            System.out.println("GameMap: " + tilesWithoutArea + " walkable tiles without an area");
        }
        if (chokesWithDeadAreas > 0) {
            System.out.println("GameMap: " + chokesWithDeadAreas + " chokes referencing dead or missing areas");
        }
        if (accessibilityMismatches > 0) {
            System.out.println("GameMap: " + accessibilityMismatches + " area pairs disagree with BWEM accessibility");
        }
    }

    public List<Area> areaPath(Area from, Area to) {
        if (from == null || to == null) {
            return null;
        }

        if (from == to) {
            ArrayList<Area> single = new ArrayList<>();
            single.add(from);
            return single;
        }

        HashMap<Area, Area> parents = new HashMap<>();
        HashSet<Area> visited = new HashSet<>();
        Deque<Area> queue = new ArrayDeque<>();

        visited.add(from);
        queue.add(from);

        while (!queue.isEmpty()) {
            Area current = queue.poll();

            if (current == to) {
                break;
            }

            for (ChokePoint choke : current.getChokes()) {
                if (choke.isBlocked()) {
                    continue;
                }

                Area neighbor = choke.getOtherArea(current);

                if (neighbor == null || visited.contains(neighbor)) {
                    continue;
                }

                visited.add(neighbor);
                parents.put(neighbor, current);
                queue.add(neighbor);
            }
        }

        if (!visited.contains(to)) {
            return null;
        }

        ArrayList<Area> reversed = new ArrayList<>();
        Area step = to;

        while (step != null) {
            reversed.add(step);
            step = parents.get(step);
        }

        ArrayList<Area> path = new ArrayList<>();
        for (int i = reversed.size() - 1; i >= 0; i--) {
            path.add(reversed.get(i));
        }

        return path;
    }

    public List<ChokePoint> chokePath(Area from, Area to) {
        List<Area> path = areaPath(from, to);

        if (path == null) {
            return null;
        }

        ArrayList<ChokePoint> chokeList = new ArrayList<>();

        if (path.size() < 2) {
            return chokeList;
        }

        Position previousPoint = from.getTop();

        for (int i = 0; i < path.size() - 1; i++) {
            Area current = path.get(i);
            Area next = path.get(i + 1);

            ChokePoint best = null;
            int bestDistance = Integer.MAX_VALUE;

            for (ChokePoint choke : current.getChokes()) {
                if (choke.isBlocked()) {
                    continue;
                }

                if (choke.getOtherArea(current) != next) {
                    continue;
                }

                int distance = 0;
                if (previousPoint != null) {
                    distance = choke.getCenter().getApproxDistance(previousPoint);
                }

                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = choke;
                }
            }

            if (best == null) {
                return null;
            }

            chokeList.add(best);
            previousPoint = best.getCenter();
        }

        return chokeList;
    }

    public void onUnitDestroyed(Unit unit) {
        if (!unit.getType().isMineralField()) {
            return;
        }

        try {
            bwem.getMap().onUnitDestroyed(unit);
        }
        catch (IllegalStateException e) {
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
        Area direct = getArea(tile);

        if (direct != null) {
            return direct;
        }

        int maxRadius = Math.max(game.mapWidth(), game.mapHeight());

        for (int radius = 1; radius < maxRadius; radius++) {
            for (int offsetX = -radius; offsetX <= radius; offsetX++) {
                for (int offsetY = -radius; offsetY <= radius; offsetY++) {
                    if (Math.abs(offsetX) != radius && Math.abs(offsetY) != radius) {
                        continue;
                    }

                    Area candidate = getArea(new TilePosition(tile.getX() + offsetX, tile.getY() + offsetY));

                    if (candidate != null) {
                        return candidate;
                    }
                }
            }
        }

        return null;
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
