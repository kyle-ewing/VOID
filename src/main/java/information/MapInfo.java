package information;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import bwapi.Game;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwem.Area;
import bwem.BWEM;
import bwem.Base;
import bwem.ChokePoint;
import bwem.Geyser;
import bwem.Mineral;
import information.enemy.EnemyUnits;
import information.neutral.MineralPatch;
import macro.buildorders.BuildOrderName;
import map.AllBasePaths;
import map.PathFinding;

public class MapInfo {
    private static final int FLYER_BASE_TILE_BUFFER = 2;

    private BWEM bwem;
    private Game game;
    private PathFinding pathFinding;
    private AllBasePaths allBasePaths;
    private Base startingBase;
    private Base naturalBase;
    private Base minOnlyBase;
    private Base enemyMain;
    private Base enemyNatural;
    private ChokePoint mainChokePoint;
    private ChokePoint naturalChokePoint;
    private ChokePoint secondaryNaturalChokePoint;
    private Unit initalCC = null;
    private TilePosition naturalBunkerEbayPosition;
    private TilePosition naturalBunkerBarracksPosition;
    private TilePosition naturalBunkerDepotPosition;
    private ChokePoint outsideNaturalChoke;
    private HashSet<Base> mapBases = new HashSet<>();
    private HashSet<Base> startingBases = new HashSet<>();
    private HashSet<Mineral> startingMinerals = new HashSet<>();
    private HashSet<Geyser> startingGeysers = new HashSet<>();
    private HashSet<TilePosition> baseTiles = new HashSet<>();
    private HashSet<TilePosition> naturalTiles = new HashSet<>();
    private HashSet<TilePosition> minBaseTiles = new HashSet<>();
    private LinkedHashSet<Base> ownedBases = new LinkedHashSet<>();
    private HashSet<Base> depletedBases = new HashSet<>();
    private HashSet<Base> halfTransferredBases = new HashSet<>();
    private HashSet<Base> depletionCountedBases = new HashSet<>();
    private HashSet<ChokePoint> chokePoints = new HashSet<>();
    private HashSet<TilePosition> usedGeysers = new HashSet<>();
    private HashSet<TilePosition> mainCliffEdge = new HashSet<>();
    private HashSet<TilePosition> naturalChokeEdge = new HashSet<>();
    private HashSet<TilePosition> combinedTankTiles = new HashSet<>();
    private HashSet<TilePosition> outsideNaturalSiegeTiles = new HashSet<>();
    private HashSet<TilePosition> claimedSiegeTiles = new HashSet<>();
    private HashSet<TilePosition> backupMainSiegeTiles = new HashSet<>();
    private HashSet<TilePosition> ccExclusionTiles = new HashSet<>();
    private HashMap<Base, TilePosition> geyserTiles = new HashMap<>();
    private HashMap<Base, List<Position>> allPathsMap;
    private HashMap<Base, HashSet<TilePosition>> baseTilesAllBases = new HashMap<>();
    private HashMap<Area, HashSet<TilePosition>> areaTiles = new HashMap<>();
    private HashMap<Base, List<Area>> basePathAreas = new HashMap<>();
    private HashMap<Base, HashSet<TilePosition>> basePathTiles = new HashMap<>();
    private HashMap<Unit, Position> blockingMineralFields = new HashMap<>();
    private HashMap<Base, Integer> originalMineralCounts = new HashMap<>();
    private HashMap<Base, Integer> originalPatchCounts = new HashMap<>();
    private HashMap<Base, Integer> livePatchCounts = new HashMap<>();
    private HashMap<Base, List<MineralPatch>> basePatches = new HashMap<>();
    private HashMap<Base, Base> startingBaseMinOnlys = new HashMap<>();
    private HashMap<Base, ChokePoint> startingBaseMainChokes = new HashMap<>();
    private ArrayList<Base> orderedExpansions = new ArrayList<>();
    private boolean naturalOwned = false;

    public MapInfo(BWEM bwem, Game game) {
        this.bwem = bwem;
        this.game = game;

        pathFinding = new PathFinding(bwem, game);

        init();
    }

    public void init() {

        for (Unit unit : game.self().getUnits()) {
            if (unit.getType() == UnitType.Terran_Command_Center) {
                initalCC = unit;
            }
        }

        addAllBases();
        setOriginalMineralCounts();
        setStartingBase();
        addStartingBases();
        setStartingMineralPatches();
        setStartingGeysers();
        setChokePoints();

        allBasePaths = new AllBasePaths(this);
        allPathsMap = new HashMap<>(allBasePaths.getBasePathLists());

        setNaturalBase();
        setMainChoke();
        setNaturalChoke();
        setOutsideNaturalChoke();
        setStartingBaseTiles();
        setNaturalBaseTiles();
        extendNaturalTiles();
        setOrderedExpansions();
        setGeyserTiles();
        setMainCliffEdge();
        setNaturalChokeEdge();
        combineTankTiles();
        backupMainSiegeTiles();
        setCcExclusionTiles();
        setAllBaseTiles();
        setAreaTiles();
        setOutsideNaturalSiegeTiles();
        setBlockingMinerals();

        //Handle edge cases where bases are split into multiple areas
        combineBaseAreas();
        setStartingBaseMinOnlys();

        allBasePaths = new AllBasePaths(this);
        allPathsMap = new HashMap<>(allBasePaths.getBasePathLists());
    }

    private void addAllBases() {
        for (Base base : bwem.getMap().getBases()) {
            mapBases.add(base);
        }
    }

    private void addStartingBases() {
        for (Base base : mapBases) {
            if (base.getLocation() == startingBase.getLocation()) {
                continue;
            }
            if (base.isStartingLocation()) {
                startingBases.add(base);
            }
        }
    }

    public boolean isExplored(Base base) {
        if (game.isExplored(base.getLocation())) {
            return true;
        }

        return false;
    }

    private void setGeyserTiles() {
        for (Base base : mapBases) {
            if (base.getGeysers().isEmpty()) {
                continue;
            }

            for (Geyser geyser : base.getGeysers()) {
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

    //Tiles around choke added for rally jank
    private void extendNaturalTiles() {
        if (naturalChokePoint == null) {
            return;
        }

        Position chokeCenter = naturalChokePoint.getCenter().toPosition();
        int maxDistance = 96;

        int mapWidth = game.mapWidth();
        int mapHeight = game.mapHeight();

        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                TilePosition tile = new TilePosition(x, y);
                Position tilePos = tile.toPosition();

                if (chokeCenter.getApproxDistance(tilePos) <= maxDistance) {
                    naturalTiles.add(tile);
                }
            }
        }
    }

    private void setStartingBaseTiles() {
       baseTiles = getTilesForBase(startingBase);
    }

    private void setNaturalBaseTiles() {
        naturalTiles = getTilesForBase(naturalBase);
    }

    private void setStartingMineralPatches() {
        for (Mineral mineral : startingBase.getMinerals()) {
            startingMinerals.add(mineral);
        }
    }

    private void setStartingGeysers() {
        for (Geyser geyser : startingBase.getGeysers()) {
            startingGeysers.add(geyser);
        }
    }

    private void setChokePoints() {
        chokePoints.addAll(bwem.getMap().getChokePoints());
    }

    private void setStartingBase() {
        Base closestStartingBase = null;
        int closestDistance = Integer.MAX_VALUE;

        for (Base base : mapBases) {
            if (base.isStartingLocation()) {
                int distance = initalCC.getDistance(base.getLocation().toPosition());
                if (closestDistance > distance) {
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

        for (Base base : mapBases) {
            if (base != startingBase) {
                List<Position> path = allPathsMap.get(base);

                if (path == null || path.isEmpty()) {
                    continue;
                }

                int distance = path.size();



                if (base.getGeysers().isEmpty()) {
                    continue;
                }

                if (distance < closestDistance) {
                    //pathTest = path;
                    closestBase = base;
                    closestDistance = distance;
                }
            }
        }
                naturalBase = closestBase;
    }

    private void setOrderedExpansions() {
        for (Base base : mapBases) {
            if (base == startingBase || base == naturalBase) {
                continue;
            }

            List<Position> path = allPathsMap.get(base);

            if (path == null || path.isEmpty()) {
                continue;
            }

            int distance = path.size();

            if (orderedExpansions.isEmpty()) {
                orderedExpansions.add(base);
            }
            else {
                boolean inserted = false;

                for (int i = 0; i < orderedExpansions.size(); i++) {
                    Base currentBase = orderedExpansions.get(i);
                    List<Position> currentPath = allPathsMap.get(currentBase);
                    int currentDistance = currentPath.size();

                    if (distance < currentDistance) {
                        orderedExpansions.add(i, base);
                        inserted = true;
                        break;
                    }
                }

                if (!inserted) {
                    orderedExpansions.add(base);
                }
            }
        }

        if (naturalBase != null) {
            orderedExpansions.add(0, naturalBase);
        }
    }

    public void readdExpansion(Unit unit) {
        Base closestBase = null;
        int closestDistance = Integer.MAX_VALUE;

        for (Base base : mapBases) {
            int distance = unit.getPosition().getApproxDistance(base.getLocation().toPosition());
            if (distance < closestDistance) {
                closestBase = base;
                closestDistance = distance;
            }
        }

        if (closestBase == null) {
            return;
        }

        if (!orderedExpansions.contains(closestBase)) {
            orderedExpansions.add(0, closestBase);
        }
    }

    private void setMainCliffEdge() {
        ChokePoint mainChoke = getMainChoke();
        ChokePoint naturalChoke = getNaturalChoke();
        HashSet<TilePosition> actualCliffEdge = new HashSet<>();

        if (mainChoke == null || naturalChoke == null) {
            return;
        }

        for (TilePosition tile : baseTiles) {
            int distanceToMainChoke = mainChoke.getCenter().toPosition().getApproxDistance(tile.toPosition());
            int distanceToNaturalChoke = naturalChoke.getCenter().toPosition().getApproxDistance(tile.toPosition());

            if (distanceToMainChoke < 160 || distanceToMainChoke > 256 || distanceToNaturalChoke > 225) {
                continue;
            }

            boolean isCliffEdge = false;
            for (int dx = -1; dx <= 1 && !isCliffEdge; dx++) {
                for (int dy = -1; dy <= 1 && !isCliffEdge; dy++) {
                    if (dx == 0 && dy == 0) {
                        continue;
                    }

                    TilePosition adj = new TilePosition(tile.getX() + dx, tile.getY() + dy);
                    if (!baseTiles.contains(adj)) {
                        isCliffEdge = true;
                    }
                }
            }

            if (isCliffEdge) {
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

        for (TilePosition tile : naturalTiles) {
            int distanceToChoke = chokeCenter.getApproxDistance(tile.toPosition());

            if (distanceToChoke >= minDistance && distanceToChoke <= maxDistance) {
                if (pathFinding.getTilePositionValidator().isWalkable(tile)) {
                    naturalChokeEdge.add(tile);
                }
            }
        }
    }

    private void setOutsideNaturalChoke() {
        if (naturalChokePoint == null || naturalBase == null || naturalBase.getArea() == null) {
            return;
        }

        Area outsideNaturalArea;
        if (naturalChokePoint.getAreas().getFirst() != naturalBase.getArea()) {
            outsideNaturalArea = naturalChokePoint.getAreas().getFirst();
        }
        else {
            outsideNaturalArea = naturalChokePoint.getAreas().getSecond();
        }

        if (outsideNaturalArea == null) {
            return;
        }

        Position mapCenter = new Position(game.mapWidth() * 16, game.mapHeight() * 16);
        int closestDistance = Integer.MAX_VALUE;

        for (ChokePoint choke : outsideNaturalArea.getChokePoints()) {
            if (choke == naturalChokePoint) {
                continue;
            }

            if (choke.getAreas().getFirst() == null || choke.getAreas().getSecond() == null) {
                continue;
            }

            int distanceToCenter = choke.getCenter().toPosition().getApproxDistance(mapCenter);

            if (distanceToCenter < closestDistance) {
                closestDistance = distanceToCenter;
                outsideNaturalChoke = choke;
            }
        }
    }

    private void setOutsideNaturalSiegeTiles() {
        if (naturalChokePoint == null || naturalBase == null || naturalBase.getArea() == null) {
            return;
        }

        Area outsideArea;
        if (naturalChokePoint.getAreas().getFirst() != naturalBase.getArea()) {
            outsideArea = naturalChokePoint.getAreas().getFirst();
        }
        else {
            outsideArea = naturalChokePoint.getAreas().getSecond();
        }

        if (outsideArea == null) {
            return;
        }

        HashSet<TilePosition> outsideAreaTiles = areaTiles.get(outsideArea);

        if (outsideAreaTiles == null) {
            return;
        }

        int minDistance = 96;
        int maxDistance = 275;

        for (ChokePoint choke : outsideArea.getChokePoints()) {
            if (choke == naturalChokePoint) {
                continue;
            }

            if (choke.getAreas().getFirst() == null || choke.getAreas().getSecond() == null) {
                continue;
            }

            Position chokeCenter = choke.getCenter().toPosition();

            for (TilePosition tile : outsideAreaTiles) {
                int distanceToChoke = chokeCenter.getApproxDistance(tile.toPosition());

                if (distanceToChoke >= minDistance && distanceToChoke <= maxDistance) {
                    if (pathFinding.getTilePositionValidator().isWalkable(tile)) {
                        outsideNaturalSiegeTiles.add(tile);
                    }
                }
            }
        }
    }

    public HashSet<TilePosition> getSiegeDefTiles() {
        HashSet<TilePosition> result = new HashSet<>(combinedTankTiles);

        if (hasExpansionPastNatural()) {
            result.addAll(outsideNaturalSiegeTiles);
        }

        return result;
    }

    public void addClaimedSiegeTile(TilePosition tile) {
        if (tile == null) {
            return;
        }
        claimedSiegeTiles.add(tile);
    }

    public void removeClaimedSiegeTile(TilePosition tile) {
        if (tile == null) {
            return;
        }
        claimedSiegeTiles.remove(tile);
    }

    public HashSet<TilePosition> getClaimedSiegeTiles() {
        return claimedSiegeTiles;
    }

    public boolean hasExpansionPastNatural() {
        if (startingBase == null || naturalBase == null) {
            return false;
        }

        Area startingArea = startingBase.getArea();
        Area naturalArea = naturalBase.getArea();

        for (Base owned : ownedBases) {
            Area ownedArea = owned.getArea();
            if (ownedArea == null) {
                continue;
            }
            if (ownedArea != startingArea && ownedArea != naturalArea) {
                return true;
            }
        }

        return false;
    }

    public void setNaturalChokeEdgeFromBunker(TilePosition bunkerTile) {
        naturalChokeEdge.clear();

        if (naturalTiles.isEmpty() || naturalBase == null) {
            return;
        }

        Position bunkerCenter = new Position(bunkerTile.toPosition().getX() + 48, bunkerTile.toPosition().getY() + 32);
        int minDistance = 64;
        int maxDistance = 160;

        int toNatX = naturalBase.getCenter().getX() - bunkerCenter.getX();
        int toNatY = naturalBase.getCenter().getY() - bunkerCenter.getY();
        double toNatMag = Math.sqrt((double) toNatX * toNatX + (double) toNatY * toNatY);

        for (TilePosition tile : naturalTiles) {
            int tileX = tile.getX();
            int tileY = tile.getY();
            boolean onBunkerFootprint = tileX >= bunkerTile.getX() - 1 && tileX <= bunkerTile.getX() + 3
                    && tileY >= bunkerTile.getY() - 1 && tileY <= bunkerTile.getY() + 2;

            if (onBunkerFootprint) {
                continue;
            }

            int distanceToBunker = bunkerCenter.getApproxDistance(tile.toPosition());

            if (distanceToBunker >= minDistance && distanceToBunker <= maxDistance) {
                if (pathFinding.getTilePositionValidator().isWalkable(tile)) {
                    int toTileX = tile.toPosition().getX() - bunkerCenter.getX();
                    int toTileY = tile.toPosition().getY() - bunkerCenter.getY();
                    double toTileMag = Math.sqrt((double) toTileX * toTileX + (double) toTileY * toTileY);
                    double cosAngle = ((double) toNatX * toTileX + (double) toNatY * toTileY) / (toNatMag * toTileMag);

                    if (cosAngle >= -0.156) {
                        naturalChokeEdge.add(tile);
                    }
                }
            }
        }

        combinedTankTiles.clear();
        combinedTankTiles.addAll(mainCliffEdge);
        combinedTankTiles.addAll(naturalChokeEdge);
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

        if (mainChoke == null || naturalChoke == null) {
            return;
        }

        for (TilePosition tile : baseTiles) {
            int distanceToMainChoke = mainChoke.getCenter().toPosition().getApproxDistance(tile.toPosition());
            int distanceToNaturalChoke = naturalChoke.getCenter().toPosition().getApproxDistance(tile.toPosition());

            if (distanceToMainChoke < 160 || distanceToMainChoke > 256) {
                continue;
            }

            boolean isCliffEdge = false;
            for (int dx = -1; dx <= 1 && !isCliffEdge; dx++) {
                for (int dy = -1; dy <= 1 && !isCliffEdge; dy++) {
                    if (dx == 0 && dy == 0) {
                        continue;
                    }

                    TilePosition adj = new TilePosition(tile.getX() + dx, tile.getY() + dy);
                    if (!baseTiles.contains(adj)) {
                        isCliffEdge = true;
                    }
                }
            }

            if (isCliffEdge) {
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

    private void setCcExclusionTiles() {
        addCcExclusionForBase(startingBase);
        if (naturalBase != null) {
            addCcExclusionForBase(naturalBase);
        }
    }

    private void addCcExclusionForBase(Base base) {
        TilePosition cc = base.getLocation();
        int ccX = cc.getX();
        int ccY = cc.getY();
        int ccXEnd = ccX + UnitType.Terran_Command_Center.tileWidth();
        int ccHeight = UnitType.Terran_Command_Center.tileHeight();

        for (int x = ccX; x < ccXEnd + 3; x++) {
            for (int y = ccY; y < ccY + ccHeight; y++) {
                ccExclusionTiles.add(new TilePosition(x, y));
            }
        }
    }

    public ChokePoint getMainChoke() {
        return mainChokePoint;
    }

    private void setMainChoke() {
        if (startingBase == null || naturalBase == null) {
            return;
        }

        List<Position> path = pathFinding.findPath(startingBase.getLocation().toPosition(), naturalBase.getLocation().toPosition()
        );

        if (path.isEmpty()) {
            return;
        }

        mainChokePoint = null;
        int minDistance = Integer.MAX_VALUE;

        for (ChokePoint chokePoint : bwem.getMap().getChokePoints()) {
            Position chokePos = chokePoint.getCenter().toPosition();

            for (Position pathPos : path) {
                int distance = chokePos.getApproxDistance(pathPos);
                if (distance < minDistance) {
                    minDistance = distance;
                    mainChokePoint = chokePoint;
                }
            }
        }
    }

    public boolean hasBunkerInNatural() {
        for (Unit building : game.self().getUnits()) {
            if (!building.getType().isBuilding()) {
                continue;
            }

            if (building.getType() == UnitType.Terran_Bunker && getNaturalTiles().contains(building.getTilePosition())) {
                return true;
            }
        }
        return false;
    }

    public boolean isFlyerInOwnedBase(Position flyerPos) {
        if (flyerPos == null) {
            return false;
        }

        TilePosition flyerTile = flyerPos.toTilePosition();
        int mapWidth = game.mapWidth();
        int mapHeight = game.mapHeight();

        for (int dx = -FLYER_BASE_TILE_BUFFER; dx <= FLYER_BASE_TILE_BUFFER; dx++) {
            for (int dy = -FLYER_BASE_TILE_BUFFER; dy <= FLYER_BASE_TILE_BUFFER; dy++) {
                int checkX = flyerTile.getX() + dx;
                int checkY = flyerTile.getY() + dy;

                if (checkX < 0 || checkY < 0 || checkX >= mapWidth || checkY >= mapHeight) {
                    continue;
                }

                if (baseTiles.contains(new TilePosition(checkX, checkY))) {
                    return true;
                }
            }
        }

        return false;
    }

    public ChokePoint getNaturalChoke() {
        return  naturalChokePoint;
    }

    public ChokePoint getSecondaryNaturalChoke() {
        return secondaryNaturalChokePoint;
    }

    public void setNaturalChoke() {
        if (naturalBase == null) {
            return;
        }

        naturalChokePoint = null;
        secondaryNaturalChokePoint = null;
        int closestDistance = Integer.MAX_VALUE;

        List<ChokePoint> naturalChokes = naturalBase.getArea().getChokePoints();
        HashMap<ChokePoint, Double> chokeDistances = new HashMap<>();

        Base potentialEnemyBase = startingBases.iterator().next();

        //Handles maps where there are multiple chokes leading out of the natural and tries to idenifty the correct choke for the bunker/rally
        for (ChokePoint choke : naturalChokes) {
            if (choke == getMainChoke()) {
                continue;
            }

            chokeDistances.put(choke, choke.getCenter().toPosition().getDistance(potentialEnemyBase.getCenter()));
        }

        for (ChokePoint choke : chokeDistances.keySet()) {
            if (chokeDistances.get(choke) < closestDistance) {
                naturalChokePoint = choke;
                closestDistance = chokeDistances.get(choke).intValue();
            }
        }

        setSecondaryNaturalChoke();
    }

    private void setSecondaryNaturalChoke() {
        if (naturalChokePoint == null || naturalBase.getArea() == null) {
            return;
        }

        Area primaryOutsideArea;
        if (naturalChokePoint.getAreas().getFirst() != naturalBase.getArea()) {
            primaryOutsideArea = naturalChokePoint.getAreas().getFirst();
        }
        else {
            primaryOutsideArea = naturalChokePoint.getAreas().getSecond();
        }

        if (primaryOutsideArea == null) {
            return;
        }

        Position primaryChokeCenter = naturalChokePoint.getCenter().toPosition();
        Position naturalCenter = naturalBase.getCenter();

        for (ChokePoint choke : primaryOutsideArea.getChokePoints()) {
            if (choke == naturalChokePoint) {
                continue;
            }

            Position candidateChokeCenter = choke.getCenter().toPosition();
            int distToPrimary = candidateChokeCenter.getApproxDistance(primaryChokeCenter);
            int distToNatural = candidateChokeCenter.getApproxDistance(naturalCenter);


            if (distToPrimary > 320) {
                continue;
            }

            if (distToNatural > 550) {
                continue;
            }

            secondaryNaturalChokePoint = choke;
            return;
        }
    }

    private void setStartingBaseMinOnlys() {
        if (minOnlyBase != null) {
            startingBaseMinOnlys.put(startingBase, minOnlyBase);
        }

        for (Base sb : startingBases) {
            Base sbMinOnly = findMinOnlyForStartingBase(sb);
            if (sbMinOnly != null) {
                startingBaseMinOnlys.put(sb, sbMinOnly);
            }
        }
    }

    private Base findMinOnlyForStartingBase(Base sb) {
        Base sbNatural = null;
        int shortestPath = Integer.MAX_VALUE;
        for (Base other : mapBases) {
            if (other.equals(sb) || other.getGeysers().isEmpty()) {
                continue;
            }
            List<Position> path = pathFinding.findPath(sb.getLocation().toPosition(), other.getLocation().toPosition());
            if (path != null && !path.isEmpty() && path.size() < shortestPath) {
                shortestPath = path.size();
                sbNatural = other;
            }
        }
        if (sbNatural == null) {
            return null;
        }

        List<Position> pathToNatural = pathFinding.findPath(sb.getLocation().toPosition(), sbNatural.getLocation().toPosition());
        if (pathToNatural == null || pathToNatural.isEmpty()) {
            return null;
        }

        ChokePoint sbMainChoke = null;
        int minChokeDistance = Integer.MAX_VALUE;
        for (ChokePoint choke : bwem.getMap().getChokePoints()) {
            Position chokePos = choke.getCenter().toPosition();
            for (Position pathPos : pathToNatural) {
                int d = chokePos.getApproxDistance(pathPos);
                if (d < minChokeDistance) {
                    minChokeDistance = d;
                    sbMainChoke = choke;
                }
            }
        }
        if (sbMainChoke == null) {
            return null;
        }

        startingBaseMainChokes.put(sb, sbMainChoke);

        Area firstArea = sbMainChoke.getAreas().getFirst();
        Area secondArea = sbMainChoke.getAreas().getSecond();

        if (firstArea.getBases().contains(sb) || secondArea.getBases().contains(sb)) {
            return null;
        }

        if (!firstArea.getBases().isEmpty() && !firstArea.getBases().get(0).equals(sbNatural)) {
            return firstArea.getBases().get(0);
        }
        if (!secondArea.getBases().isEmpty() && !secondArea.getBases().get(0).equals(sbNatural)) {
            return secondArea.getBases().get(0);
        }
        return null;
    }

    public Base getEnemyRushTargetBase(HashSet<EnemyUnits> knownEnemyUnits) {
        if (enemyMain == null) {
            return enemyNatural;
        }
        Base candidateMinOnly = startingBaseMinOnlys.get(enemyMain);
        if (candidateMinOnly == null) {
            return enemyNatural;
        }
        for (EnemyUnits enemyUnit : knownEnemyUnits) {
            if (enemyUnit.getEnemyType().isResourceDepot()
                    && enemyUnit.getEnemyPosition() != null
                    && enemyUnit.getEnemyPosition().getDistance(candidateMinOnly.getLocation().toPosition()) < 200) {
                return candidateMinOnly;
            }
        }
        return enemyNatural;
    }

    //WIP, hacky solution for Andromeda
    private void combineBaseAreas() {
        if (naturalBase == null || mainChokePoint == null || startingBase == null) {
            return;
        }

        Area firstArea = mainChokePoint.getAreas().getFirst();
        Area secondArea = mainChokePoint.getAreas().getSecond();

        //If starting base is in either area of the main choke point ignore this (Andromeda edge case)
        if (firstArea.getBases().contains(startingBase) || secondArea.getBases().contains(startingBase)) {
            return;
        }

        if (!firstArea.getBases().isEmpty() && !firstArea.getBases().get(0).equals(naturalBase)) {
            minOnlyBase = firstArea.getBases().get(0);
            HashSet<TilePosition> firstAreaTiles = getTilesForBase(firstArea.getBases().get(0));
            minBaseTiles.addAll(firstAreaTiles);
            baseTiles.addAll(firstAreaTiles);
        }

        if (!secondArea.getBases().isEmpty() && !secondArea.getBases().get(0).equals(naturalBase)) {
            minOnlyBase = secondArea.getBases().get(0);
            HashSet<TilePosition> secondAreaTiles = getTilesForBase(secondArea.getBases().get(0));
            minBaseTiles.addAll(secondAreaTiles);
            baseTiles.addAll(secondAreaTiles);
        }
    }

    public boolean baseCloseToNatural(Base base) {
        if (base == null || naturalBase == null) {
            return false;
        }

        if (base == naturalBase) {
            return false;
        }

        Area baseArea = base.getArea();
        Area naturalArea = naturalBase.getArea();

        if (baseArea == null || naturalArea == null) {
            return false;
        }

        if (baseArea == naturalArea) {
            return false;
        }

        List<Area> path = areaBfsPath(naturalArea, baseArea);

        if (path == null) {
            return false;
        }

        return path.size() <= 3;
    }

    private List<Area> areaBfsPath(Area from, Area to) {
        if (from == null || to == null) {
            return null;
        }

        if (from == to) {
            List<Area> singleton = new ArrayList<>();
            singleton.add(from);
            return singleton;
        }

        HashMap<Area, Area> parents = new HashMap<>();
        HashSet<Area> visited = new HashSet<>();
        Deque<Area> queue = new ArrayDeque<>();

        visited.add(from);
        queue.add(from);

        boolean found = false;

        while (!queue.isEmpty() && visited.size() <= 64) {
            Area current = queue.poll();

            for (ChokePoint choke : current.getChokePoints()) {
                Area neighbor = choke.getAreas().getFirst();
                if (neighbor == current) {
                    neighbor = choke.getAreas().getSecond();
                }

                if (neighbor == null || visited.contains(neighbor)) {
                    continue;
                }

                visited.add(neighbor);
                parents.put(neighbor, current);

                if (neighbor == to) {
                    found = true;
                    break;
                }

                queue.add(neighbor);
            }

            if (found) {
                break;
            }
        }

        if (!found) {
            return null;
        }

        ArrayList<Area> reversed = new ArrayList<>();
        Area step = to;
        while (step != null) {
            reversed.add(step);
            if (step == from) {
                break;
            }
            step = parents.get(step);
        }

        ArrayList<Area> path = new ArrayList<>();
        for (int i = reversed.size() - 1; i >= 0; i--) {
            path.add(reversed.get(i));
        }

        return path;
    }

    private void addPathAreasForBase(Base newBase) {
        if (newBase == null || newBase == naturalBase || naturalBase == null) {
            return;
        }

        Area newBaseArea = newBase.getArea();
        Area naturalArea = naturalBase.getArea();

        if (newBaseArea == null || naturalArea == null) {
            return;
        }

        List<Area> path = areaBfsPath(naturalArea, newBaseArea);

        if (path == null) {
            return;
        }

        basePathAreas.put(newBase, path);

        HashSet<Area> massiveAreas = new HashSet<>();
        for (int i = 1; i <= path.size() - 2; i++) {
            Area intermediate = path.get(i);
            HashSet<TilePosition> tiles = areaTiles.get(intermediate);
            if (tiles != null && tiles.size() > 1000) {
                massiveAreas.add(intermediate);
            }
        }

        if (massiveAreas.isEmpty()) {
            for (int i = 1; i <= path.size() - 2; i++) {
                Area intermediate = path.get(i);
                HashSet<TilePosition> tiles = areaTiles.get(intermediate);
                if (tiles != null) {
                    baseTiles.addAll(tiles);
                }
            }
            return;
        }

        for (int i = 1; i <= path.size() - 2; i++) {
            Area intermediate = path.get(i);
            if (massiveAreas.contains(intermediate)) {
                continue;
            }
            HashSet<TilePosition> tiles = areaTiles.get(intermediate);
            if (tiles != null) {
                baseTiles.addAll(tiles);
            }
        }

        List<Position> tilePath = pathFinding.findPath(naturalBase.getLocation().toPosition(), newBase.getLocation().toPosition());

        if (tilePath == null || tilePath.isEmpty()) {
            return;
        }

        int mapWidth = game.mapWidth();
        int mapHeight = game.mapHeight();
        HashSet<TilePosition> radiusContribution = basePathTiles.computeIfAbsent(newBase, k -> new HashSet<>());

        for (Position p : tilePath) {
            TilePosition pathTile = p.toTilePosition();
            Area pathArea = bwem.getMap().getArea(pathTile);

            if (pathArea == null || !massiveAreas.contains(pathArea)) {
                continue;
            }

            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = -3; dy <= 3; dy++) {
                    int cx = pathTile.getX() + dx;
                    int cy = pathTile.getY() + dy;

                    if (cx < 0 || cy < 0 || cx >= mapWidth || cy >= mapHeight) {
                        continue;
                    }

                    TilePosition candidateTile = new TilePosition(cx, cy);
                    baseTiles.add(candidateTile);
                    radiusContribution.add(candidateTile);
                }
            }
        }
    }

    private void removePathAreasForBase(Base deadBase) {
        List<Area> deadPath = basePathAreas.remove(deadBase);

        if (deadPath == null) {
            return;
        }

        if (deadPath.size() <= 2) {
            return;
        }

        HashSet<Area> stillNeeded = new HashSet<>();

        for (List<Area> remainingPath : basePathAreas.values()) {
            for (int i = 1; i <= remainingPath.size() - 2; i++) {
                stillNeeded.add(remainingPath.get(i));
            }
        }

        for (Base owned : ownedBases) {
            if (owned.getArea() != null) {
                stillNeeded.add(owned.getArea());
            }
        }

        for (int i = 1; i <= deadPath.size() - 2; i++) {
            Area intermediate = deadPath.get(i);
            if (stillNeeded.contains(intermediate)) {
                continue;
            }
            HashSet<TilePosition> tiles = areaTiles.get(intermediate);
            if (tiles == null) {
                continue;
            }
            if (tiles.size() > 1000) {
                continue;
            }
            baseTiles.removeAll(tiles);
        }

        HashSet<TilePosition> deadContribution = basePathTiles.remove(deadBase);

        if (deadContribution == null || deadContribution.isEmpty()) {
            return;
        }

        HashSet<TilePosition> stillNeededTiles = new HashSet<>();

        for (HashSet<TilePosition> remaining : basePathTiles.values()) {
            stillNeededTiles.addAll(remaining);
        }

        stillNeededTiles.addAll(naturalTiles);

        for (Base owned : ownedBases) {
            if (owned == deadBase) {
                continue;
            }
            HashSet<TilePosition> ownedTiles = baseTilesAllBases.get(owned);
            if (ownedTiles != null) {
                stillNeededTiles.addAll(ownedTiles);
            }
        }

        for (TilePosition t : deadContribution) {
            if (stillNeededTiles.contains(t)) {
                continue;
            }
            baseTiles.remove(t);
        }
    }

    private void setAllBaseTiles() {

        for (Base base : mapBases) {
            if (base == startingBase || base == naturalBase || (minOnlyBase != null && base == minOnlyBase)) {
                continue;
            }

            HashSet<TilePosition> tiles = getTilesForBase(base);
            baseTilesAllBases.put(base, tiles);
        }
    }

    private void setAreaTiles() {
        int mapWidth = game.mapWidth();
        int mapHeight = game.mapHeight();

        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                TilePosition tile = new TilePosition(x, y);
                Area tileArea = bwem.getMap().getArea(tile);
                if (tileArea == null) {
                    continue;
                }
                areaTiles.computeIfAbsent(tileArea, k -> new HashSet<>()).add(tile);
            }
        }
    }

    private void setOriginalMineralCounts() {
        for (Base base : mapBases) {
            int totalResources = 0;
            List<MineralPatch> patches = new ArrayList<>();
            for (Mineral mineral : base.getMinerals()) {
                totalResources += mineral.getUnit().getResources();
                patches.add(new MineralPatch(mineral));
            }
            originalMineralCounts.put(base, totalResources);
            int patchCount = base.getMinerals().size();
            originalPatchCounts.put(base, patchCount);
            livePatchCounts.put(base, patchCount);
            basePatches.put(base, patches);
        }
    }

    private void decrementLivePatchCount(Unit mineralUnit) {

        for (Base base : mapBases) {
            for (Mineral mineral : base.getMinerals()) {
                if (mineral.getUnit().getID() == mineralUnit.getID()) {
                    livePatchCounts.put(base, Math.max(0, livePatchCounts.getOrDefault(base, 0) - 1));
                }
            }
        }
    }

    private void setBlockingMinerals() {
        for (Unit unit : game.getNeutralUnits()) {
            if (unit.getType() != UnitType.Resource_Mineral_Field) {
                continue;
            }

            List<Position> path = pathFinding.findPath(startingBase.getLocation().toPosition(), naturalBase.getLocation().toPosition());

            if (path.isEmpty()) {
                continue;
            }

            if (unit.getResources() == 0 && naturalBase.getCenter().getDistance(unit.getPosition()) < 1000) {
                blockingMineralFields.put(unit, unit.getPosition());
            }
        }
    }

    public Base scoredBestExpansion(BuildOrderName buildOrder, HashSet<EnemyUnits> knownEnemyUnits) {
        Base bestBase = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Base base : mapBases) {
            if (base == startingBase || base == naturalBase) {
                continue;
            }

            if (ownedBases.contains(base)) {
                continue;
            }

            List<Position> path = allPathsMap.get(base);
            if (path == null || path.isEmpty()) {
                continue;
            }

            boolean enemyOwned = false;
            for (EnemyUnits enemyUnit : knownEnemyUnits) {
                if (enemyUnit.getEnemyType().isResourceDepot() && enemyUnit.getEnemyPosition().getDistance(base.getLocation().toPosition()) < 200) {
                    enemyOwned = true;
                    break;
                }
            }

            if (enemyOwned) {
                continue;
            }

            double distFromMain = base.getCenter().getDistance(startingBase.getCenter());

            
            double distFromEnemy = 0.0;

            if (ownedBases.contains(naturalBase)) {
                distFromMain = base.getCenter().getDistance(naturalBase.getCenter());
            }

            if (enemyMain != null) {
                distFromEnemy = base.getCenter().getDistance(enemyMain.getCenter());
            }

            double score = distFromEnemy - distFromMain;

            if (buildOrder == BuildOrderName.GOLIATHFE && base.getGeysers().isEmpty()) {
                score -= 5000;
            }

            if (score > bestScore) {
                bestScore = score;
                bestBase = base;
            }
        }

        return bestBase;
    }

    public ArrayList<Base> scoredBestEnemyExpansion(HashSet<EnemyUnits> knownEnemyUnits) {
        ArrayList<Base> candidates = new ArrayList<>();

        if (enemyMain == null && enemyNatural == null) {
            return candidates;
        }

        boolean enemyNaturalHasDepot = false;
        if (enemyNatural != null) {
            for (EnemyUnits enemyUnit : knownEnemyUnits) {
                if (enemyUnit.getEnemyType().isResourceDepot()
                        && enemyUnit.getEnemyPosition().getDistance(enemyNatural.getLocation().toPosition()) < 200) {
                    enemyNaturalHasDepot = true;
                    break;
                }
            }
        }

        Base enemyFrontline;
        if (enemyNaturalHasDepot) {
            enemyFrontline = enemyNatural;
        }
        else if (enemyMain != null) {
            enemyFrontline = enemyMain;
        }
        else {
            enemyFrontline = enemyNatural;
        }

        for (Base base : mapBases) {
            if (base == startingBase || base == naturalBase || base == enemyMain || base == enemyNatural) {
                continue;
            }

            if (ownedBases.contains(base)) {
                continue;
            }

            List<Position> path = allPathsMap.get(base);
            if (path == null || path.isEmpty()) {
                continue;
            }

            boolean enemyOwned = false;
            for (EnemyUnits enemyUnit : knownEnemyUnits) {
                if (enemyUnit.getEnemyType().isResourceDepot()
                        && enemyUnit.getEnemyPosition().getDistance(base.getLocation().toPosition()) < 100) {
                    enemyOwned = true;
                    break;
                }
            }

            if (enemyOwned) {
                continue;
            }

            candidates.add(base);
        }

        Base playerFrontline = ownedBases.contains(naturalBase) ? naturalBase : startingBase;
        final Base finalEnemyFrontline = enemyFrontline;
        final Base finalPlayerFrontline = playerFrontline;

        candidates.sort((a, b) -> {
            double scoreA = finalPlayerFrontline.getCenter().getDistance(a.getCenter()) - finalEnemyFrontline.getCenter().getDistance(a.getCenter());
            double scoreB = finalPlayerFrontline.getCenter().getDistance(b.getCenter()) - finalEnemyFrontline.getCenter().getDistance(b.getCenter());
            return Double.compare(scoreB, scoreA);
        });

        return new ArrayList<>(candidates.subList(0, Math.min(3, candidates.size())));
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

    public TilePosition getNaturalBunkerEbayPosition() {
        return naturalBunkerEbayPosition;
    }

    public void setNaturalBunkerEbayPosition(TilePosition position) {
        naturalBunkerEbayPosition = position;
    }

    public TilePosition getNaturalBunkerBarracksPosition() {
        return naturalBunkerBarracksPosition;
    }

    public void setNaturalBunkerBarracksPosition(TilePosition position) {
        naturalBunkerBarracksPosition = position;
    }

    public TilePosition getNaturalBunkerDepotPosition() {
        return naturalBunkerDepotPosition;
    }

    public void setNaturalBunkerDepotPosition(TilePosition position) {
        naturalBunkerDepotPosition = position;
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

    public HashSet<TilePosition> getOutsideNaturalSiegeTiles() {
        return outsideNaturalSiegeTiles;
    }

    public HashSet<TilePosition> getBackupMainSiegeTiles() {
        return backupMainSiegeTiles;
    }

    public HashSet<TilePosition> getCcExclusionTiles() {
        return ccExclusionTiles;
    }

    public HashMap<Unit, Position> getBlockingMineralFields() {
        return blockingMineralFields;
    }

    public HashMap<Base, Integer> getOriginalMineralCounts() {
        return originalMineralCounts;
    }

    public HashSet<Base> getDepletedBases() {
        return depletedBases;
    }

    public HashSet<Base> getHalfTransferredBases() {
        return halfTransferredBases;
    }

    public HashSet<Base> getDepletionCountedBases() {
        return depletionCountedBases;
    }

    public HashMap<Base, Integer> getOriginalPatchCounts() {
        return originalPatchCounts;
    }

    public HashMap<Base, Integer> getLivePatchCounts() {
        return livePatchCounts;
    }

    public List<MineralPatch> getBasePatches(Base base) {
        return basePatches.getOrDefault(base, new ArrayList<>());
    }

    public void claimNatural() {
        baseTiles.addAll(naturalTiles);
        naturalOwned = true;
        ownedBases.add(naturalBase);
    }

    public boolean isNaturalOwned() {
        return naturalOwned;
    }

    public void setNaturalOwned(boolean naturalOwned) {
        this.naturalOwned = naturalOwned;
    }

    public Base getMinOnlyBase() {
        return minOnlyBase;
    }

    public HashSet<TilePosition> getMinBaseTiles() {
        return minBaseTiles;
    }

    public Base getEnemyNatural() {
        return enemyNatural;
    }

    public void setEnemyNatural(Base enemyNatural) {
        this.enemyNatural = enemyNatural;
    }

    public Base getEnemyMain() {
        return enemyMain;
    }

    public void setEnemyMain(Base enemyMain) {
        this.enemyMain = enemyMain;
    }

    public ChokePoint getStartingBaseMainChoke(Base base) {
        return startingBaseMainChokes.get(base);
    }

    public HashMap<Base, HashSet<TilePosition>> getBaseTilesAllBases() {
        return baseTilesAllBases;
    }

    public HashMap<Area, HashSet<TilePosition>> getAreaTiles() {
        return areaTiles;
    }

    public HashMap<Base, List<Area>> getBasePathAreas() {
        return basePathAreas;
    }

    public ChokePoint getOutsideNaturalChoke() {
        return outsideNaturalChoke;
    }

    public void setOutsideNaturalChoke(ChokePoint outsideNaturalChoke) {
        this.outsideNaturalChoke = outsideNaturalChoke;
    }

    public void onUnitCreate(Unit unit) {
        if (unit.getType() != UnitType.Terran_Command_Center) {
            return;
        }

        Base baseTaken = null;

        for (Base base : mapBases) {
            if (unit.getPosition().getApproxDistance(base.getLocation().toPosition()) < 100) {
                ownedBases.add(base);
                baseTaken = base;
                break;
            }
        }

        if (unit.getDistance(naturalBase.getCenter()) < 100) {
            baseTiles.addAll(naturalTiles);
            naturalOwned = true;
            return;
        }

        if (naturalOwned && baseTaken != null) {
            HashSet<TilePosition> expansionTiles = baseTilesAllBases.get(baseTaken);
            if (expansionTiles != null) {
                baseTiles.addAll(expansionTiles);
            }
            addPathAreasForBase(baseTaken);
        }
    }

    public void onUnitDestroy(Unit unit) {
        if (unit.getType() == UnitType.Terran_Command_Center) {
            Base destroyedBase = null;
            for (Base base : ownedBases) {
                if (unit.getPosition().getApproxDistance(base.getLocation().toPosition()) < 100) {
                    destroyedBase = base;
                    break;
                }
            }

            if (destroyedBase != null) {
                ownedBases.remove(destroyedBase);
            }

            if (unit.getPosition().getApproxDistance(naturalBase.getCenter()) < 100) {
                baseTiles.removeAll(naturalTiles);
                naturalOwned = false;
                return;
            }

            if (destroyedBase != null) {
                HashSet<TilePosition> expansionTiles = baseTilesAllBases.get(destroyedBase);
                if (expansionTiles != null) {
                    baseTiles.removeAll(expansionTiles);
                }
                removePathAreasForBase(destroyedBase);
            }

            return;
        }

        if (unit.getType().isMineralField()) {
            try {
                bwem.getMap().onUnitDestroyed(unit);
            }
            catch (IllegalStateException e) {
            }
            decrementLivePatchCount(unit);
            for (List<MineralPatch> patches : basePatches.values()) {
                for (MineralPatch patch : patches) {
                    if (patch.getUnit().getID() == unit.getID()) {
                        patch.markDestroyed();
                    }
                }
            }
        }

        if (blockingMineralFields.containsKey(unit)) {
            blockingMineralFields.remove(unit);
        }
    }
}
