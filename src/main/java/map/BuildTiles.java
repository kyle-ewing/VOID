package map;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import bwapi.Game;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import map.bwemwrappers.Area;
import map.bwemwrappers.Base;
import map.bwemwrappers.ChokePoint;
import map.bwemwrappers.Geyser;
import map.bwemwrappers.Mineral;
import information.MapInfo;
import util.PositionInterpolator;

public class BuildTiles {
    private Game game;
    private MapInfo mapInfo;
    private TilePositionValidator tilePositionValidator;
    private HashSet<TilePosition> mediumBuildTiles = new HashSet<>();
    private HashSet<TilePosition> largeBuildTiles = new HashSet<>();
    private HashSet<TilePosition> largeBuildTilesNoGap = new HashSet<>();
    private HashSet<TilePosition> mineralExlusionTiles = new HashSet<>();
    private HashSet<TilePosition> mineralAboveGapTiles = new HashSet<>();
    private HashSet<TilePosition> geyserExlusionTiles = new HashSet<>();
    private HashSet<TilePosition> ccExclusionTiles = new HashSet<>();
    private HashSet<TilePosition> chokeExclusionTiles = new HashSet<>();
    private HashSet<TilePosition> frontBaseTiles = new HashSet<>();
    private HashSet<TilePosition> backBaseTiles = new HashSet<>();
    private HashSet<TilePosition> mainTurrets = new HashSet<>();
    private HashMap<Base, TilePosition> mineralLineTurrets = new HashMap<>();
    private Queue<Base> pendingBaseTiles = new ArrayDeque<>();
    private TilePosition mainChokeBunker;
    private TilePosition naturalChokeBunker;
    private TilePosition naturalBunkerEbayPosition;
    private TilePosition naturalBunkerBarracksPosition;
    private TilePosition naturalBunkerDepotPosition;
    private TilePosition closeBunkerTile;
    private TilePosition proxyBunkerTile;
    private TilePosition mainChokeTurret;
    private TilePosition naturalChokeTurret;
    private TilePosition mainBaseCCTile;
    private Base startingBase;
    private boolean naturalTilesGenerated = false;
    private boolean minOnlyTilesGenerated = false;

    private static final int BARRACKS_WIDTH = UnitType.Terran_Barracks.tileWidth();
    private static final int BARRACKS_HEIGHT = UnitType.Terran_Barracks.tileHeight();
    private static final int DEPOT_WIDTH = UnitType.Terran_Supply_Depot.tileWidth();
    private static final int DEPOT_HEIGHT = UnitType.Terran_Supply_Depot.tileHeight();
    private static final int BUNKER_WIDTH = UnitType.Terran_Bunker.tileWidth();
    private static final int BUNKER_HEIGHT = UnitType.Terran_Bunker.tileHeight();
    private static final int TURRET_WIDTH = UnitType.Terran_Missile_Turret.tileWidth();
    private static final int TURRET_HEIGHT = UnitType.Terran_Missile_Turret.tileHeight();
    private static final int CC_WIDTH = UnitType.Terran_Command_Center.tileWidth();
    private static final int CC_HEIGHT = UnitType.Terran_Command_Center.tileHeight();
    private static final int EBAY_WIDTH = UnitType.Terran_Engineering_Bay.tileWidth();
    private static final int EBAY_HEIGHT = UnitType.Terran_Engineering_Bay.tileHeight();

    public BuildTiles(Game game, MapInfo mapInfo) {
        this.game = game;
        this.mapInfo = mapInfo;

        tilePositionValidator = new TilePositionValidator(game);

        startingBase = mapInfo.getStartingBase();
        generateBuildTiles();
    }

    private void generateBuildTiles() {
        mineralExclusionZone(startingBase);
        mineralAboveGap(startingBase);
        geyserExclusionZone(startingBase);
        ccExclusionZone(startingBase);
        mineralExclusionZone(mapInfo.getNaturalBase());
        geyserExclusionZone(mapInfo.getNaturalBase());
        ccExclusionZone(mapInfo.getNaturalBase());
        if (mapInfo.getMinOnlyBase() != null) {
            mineralExclusionZone(mapInfo.getMinOnlyBase());
            geyserExclusionZone(mapInfo.getMinOnlyBase());
            ccExclusionZone(mapInfo.getMinOnlyBase());
        }
        chokeExclusionZone(startingBase);
        generateFrontBaseTiles();
        generateBackBaseTiles();
        generateChokeBunkerTiles();
        generateCloseBunkerTile();
        mainChokeTurret = generateChokeTurretTile(mainChokeBunker, startingBase);
        naturalChokeTurret = generateChokeTurretTile(naturalChokeBunker, mapInfo.getNaturalBase());
        generateMainBaseCCTile();
        generateLargeTiles(frontBaseTiles);
        generateMediumTiles(backBaseTiles);
        generateTurretTiles();
    }

    private void regenerateBuildTiles() {
        if (!naturalTilesGenerated && mediumBuildTiles.isEmpty() || mediumBuildTiles.size() == 1) {
            chokeExclusionZone(mapInfo.getNaturalBase());
            //generateMediumTiles(frontBaseTiles);
            //generateMediumTiles(mapInfo.getNaturalTiles());
            naturalTilesGenerated = true;
        }

        if (!mapInfo.getMinBaseTiles().isEmpty() && mapInfo.getMinOnlyBase() != null && !minOnlyTilesGenerated) {
            generateLargeTiles(mapInfo.getMinBaseTiles());
            minOnlyTilesGenerated = true;
        }
        else if (mapInfo.getMinBaseTiles().isEmpty()) {
            minOnlyTilesGenerated = true;
        }

        if (!pendingBaseTiles.isEmpty()) {
            if (largeBuildTiles.isEmpty() || mediumBuildTiles.isEmpty()) {
                Base pendingBase = pendingBaseTiles.peek();
                mineralExclusionZone(pendingBase);
                geyserExclusionZone(pendingBase);
                ccExclusionZone(pendingBase);
                chokeExclusionZone(pendingBase);
                HashSet<TilePosition> newBaseTiles = mapInfo.getTilesForBase(pendingBase);

                if (mediumBuildTiles.isEmpty()) {
                    generateMediumTiles(newBaseTiles);
                }

                if (largeBuildTiles.isEmpty()) {
                    generateLargeTiles(newBaseTiles);
                }

                pendingBaseTiles.poll();
            }

        }
    }

    //TODO: I hate all of this
    // Final pass still not generating enough on some maps
    private void generateLargeTiles(HashSet<TilePosition> baseTiles) {
        TilePosition ccPos = mapInfo.getStartingBase().getLocation();

        //Sort tiles to favor closer to CC
        List<TilePosition> ccOrderedTiles = new ArrayList<>(baseTiles);
        TilePosition chokeCenter = mapInfo.getMainChoke().getCenter().toTilePosition();
        Comparator<TilePosition> comparator = Comparator.comparingInt(TilePosition::getY);

        if (ccPos.getX() <= chokeCenter.getX()) {
            comparator = comparator.thenComparingInt(TilePosition::getX);
        }
        else {
            comparator = comparator.thenComparing(Comparator.comparingInt(TilePosition::getX).reversed());
        }

        ccOrderedTiles.sort(comparator);

        //First pass largeBuildTilesNoGap with no gap stacks
        for (TilePosition tile : ccOrderedTiles) {
            if (largeBuildTilesNoGap.size() >= 4) {
                break;
            }

            if (backBaseTiles.contains(tile)) {
                continue;
            }

            if (isValidBarracksStack(tile, 0) && !largeBuildTilesNoGap.contains(tile) && !largeBuildTiles.contains(tile)) {
                addBarracksStack(tile.getX(), tile.getY(), largeBuildTilesNoGap);
            }
        }

        //Second pass largeBuildTilesNoGap with no gap no stack
        if (largeBuildTilesNoGap.size() < 4) {
            for (TilePosition tile : ccOrderedTiles) {
                if (largeBuildTilesNoGap.size() >= 4) {
                    break;
                }

                if (backBaseTiles.contains(tile)) {
                    continue;
                }

                if (!isValidBarracksPosition(tile, 0)) {
                    continue;
                }

                if (overlapsAnyBarracksFootprint(tile, largeBuildTilesNoGap, 0)) {
                    continue;
                }

                if (!overlapsAnyBarracksFootprint(tile, largeBuildTiles, 3)) {
                    if (!hasAdjacentBuildableColumn(tile)) {
                        continue;
                    }
                    largeBuildTilesNoGap.add(tile);
                }
            }
        }

        //Re-sort tiles Y increasing then X increasing
        List<TilePosition> sortedFrontTiles = new ArrayList<>(baseTiles);

        sortedFrontTiles.sort(
                Comparator.comparingInt(TilePosition::getY)
                        .thenComparingInt(TilePosition::getX)
        );

        //First pass largeBuildTiles with 3 tile gap stacks
        for (TilePosition tile : sortedFrontTiles) {
            if (largeBuildTiles.size() >= 10) {
                break;
            }

            if (backBaseTiles.contains(tile)) {
                continue;
            }

            if (isValidBarracksStack(tile, 3) && !largeBuildTiles.contains(tile)) {
                int stackX = tile.getX();
                int stackY = tile.getY();

                int adjacentY = stackY + BARRACKS_HEIGHT;
                TilePosition adjacentPos = new TilePosition(stackX, adjacentY);

                if (baseTiles.contains(adjacentPos) && isValidBarracksStack(adjacentPos, 3)) {
                    addBarracksStack(stackX, stackY, largeBuildTiles);
                }
            }
        }

        //Second pass largeBuildTiles with 2 tile gap stacks (may cause blockage?)
        for (TilePosition tile : sortedFrontTiles) {
            if (largeBuildTiles.size() >= 12) {
                break;
            }

            if (backBaseTiles.contains(tile)) {
                continue;
            }

            if (isValidBarracksStack(tile, 2) && !largeBuildTiles.contains(tile)) {
                int stackX = tile.getX();
                int stackY = tile.getY();

                int adjacentX = stackX + BARRACKS_WIDTH + 2;
                TilePosition adjacentPos = new TilePosition(adjacentX, stackY);

                if (frontBaseTiles.contains(adjacentPos) && isValidBarracksStack(adjacentPos, 2)) {
                    addBarracksStack(stackX, stackY, largeBuildTiles);
                }
            }
        }

        //Third pass largeBuildTiles with 3 tile gap no stack
        if (largeBuildTiles.size() < 6) {
            for (TilePosition tile : sortedFrontTiles) {
                if (largeBuildTiles.size() >= 8) {
                    break;
                }

                if (backBaseTiles.contains(tile)) {
                    continue;
                }

                if (!isValidBarracksPosition(tile, 3)) {
                    continue;
                }

                if (overlapsAnyBarracksFootprint(tile, largeBuildTiles, 3)) {
                    continue;
                }

                if (!overlapsAnyBarracksFootprint(tile, largeBuildTilesNoGap, 3)) {
                    largeBuildTiles.add(tile);
                }
            }
        }

        //3rd pass largeBuildTilesNoGap with no gap stacks (Fill in more barracks tiles)
        for (TilePosition tile : sortedFrontTiles) {
            if (largeBuildTilesNoGap.size() >= 6) {
                break;
            }

            if (backBaseTiles.contains(tile)) {
                continue;
            }

            if (isValidBarracksStack(tile, 0) && !largeBuildTilesNoGap.contains(tile) && !largeBuildTiles.contains(tile)) {
                addBarracksStack(tile.getX(), tile.getY(), largeBuildTilesNoGap);
            }
        }

        //4th pass largeBuildTilesNoGap with no gap no stack
        if (largeBuildTilesNoGap.size() < 6) {
            for (TilePosition tile : sortedFrontTiles) {
                if (largeBuildTilesNoGap.size() >= 6) {
                    break;
                }

                if (backBaseTiles.contains(tile)) {
                    continue;
                }

                if (!isValidBarracksPosition(tile, 0)) {
                    continue;
                }

                if (overlapsAnyBarracksFootprint(tile, largeBuildTilesNoGap, 0)) {
                    continue;
                }

                if (!overlapsAnyBarracksFootprint(tile, largeBuildTiles, 3)) {
                    if (!hasAdjacentBuildableColumn(tile)) {
                        continue;
                    }
                    largeBuildTilesNoGap.add(tile);
                }
            }
        }
    }

    private boolean overlapsAnyBarracksFootprint(TilePosition tile, Set<TilePosition> existing, int xPad) {
        for (TilePosition existingTile : existing) {
            int endX = existingTile.getX() + BARRACKS_WIDTH + xPad;
            int endY = existingTile.getY() + BARRACKS_HEIGHT;

            if (!(tile.getX() + BARRACKS_WIDTH <= existingTile.getX() ||
                    tile.getX() >= endX ||
                    tile.getY() + BARRACKS_HEIGHT <= existingTile.getY() ||
                    tile.getY() >= endY)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasAdjacentBuildableColumn(TilePosition tile) {
        int x = tile.getX();
        int y = tile.getY();

        for (int dy = 0; dy < BARRACKS_HEIGHT; dy++) {
            TilePosition leftTile = new TilePosition(x - 1, y + dy);
            if (tilePositionValidator.isWithinMap(leftTile) && tilePositionValidator.isBuildable(leftTile)) {
                return true;
            }
        }

        for (int dy = 0; dy < BARRACKS_HEIGHT; dy++) {
            TilePosition rightTile = new TilePosition(x + BARRACKS_WIDTH, y + dy);
            if (tilePositionValidator.isWithinMap(rightTile) && tilePositionValidator.isBuildable(rightTile)) {
                return true;
            }
        }

        return false;
    }

    private boolean isValidBarracksStack(TilePosition topTile, int gap) {
        TilePosition bottomTile = new TilePosition(topTile.getX(), topTile.getY() + BARRACKS_HEIGHT);

        return isValidBarracksPosition(topTile, gap) && isValidBarracksPosition(bottomTile, gap);
    }

    private boolean isValidBarracksPosition(TilePosition tile, int gap) {
        if (intersectsExistingBuildTiles(tile, UnitType.Terran_Barracks, gap)) {
            return false;
        }

        for (int x = 0; x < BARRACKS_WIDTH; x++) {
            for (int y = 0; y < BARRACKS_HEIGHT; y++) {
                TilePosition checkTile = new TilePosition(tile.getX() + x, tile.getY() + y);

                if (backBaseTiles.contains(tile)) {
                    return false;
                }

                if (intersectsExclusionZones(checkTile) || !tilePositionValidator.isBuildable(checkTile)) {
                    return false;
                }
            }
        }

        for (int bufferX = 0; bufferX < gap; bufferX++) {
            for (int y = 0; y < BARRACKS_HEIGHT; y++) {
                TilePosition bufferTile = new TilePosition(tile.getX() + BARRACKS_WIDTH + bufferX, tile.getY() + y);

                if (!tilePositionValidator.isWithinMap(bufferTile) || intersectsExclusionZones(bufferTile) || !tilePositionValidator.isWalkable(bufferTile)) {
                    return false;
                }
            }
        }

        //Check tile gap below stack
        for (int x = 0; x < BARRACKS_WIDTH; x++) {
            TilePosition gapTile = new TilePosition(tile.getX() + x, tile.getY() - 1);

            if (intersectsExclusionZones(gapTile) || !tilePositionValidator.isWalkable(gapTile) || intersectsExistingBuildTiles(gapTile, UnitType.Terran_Barracks, 0)) {
                return false;
            }
        }

        return !intersectsNeutralBuildings(tile, BARRACKS_WIDTH, BARRACKS_HEIGHT);
    }

    private void addBarracksStack(int x, int y, Set<TilePosition> target) {
        target.add(new TilePosition(x, y));
        target.add(new TilePosition(x, y + BARRACKS_HEIGHT));
    }

    private void generateMediumTiles(HashSet<TilePosition> baseTiles) {
        UnitType depotType = UnitType.Terran_Supply_Depot;

        List<TilePosition> sortedBackTiles = new ArrayList<>(baseTiles);
        HashSet<TilePosition> usedTiles = new HashSet<>();

        sortedBackTiles.sort(
                Comparator.comparingInt(TilePosition::getY)
                        .thenComparingInt(TilePosition::getX)
        );

        for (TilePosition tile : sortedBackTiles) {
            if (mediumBuildTiles.size() >= 22) {
                break;
            }

            if (usedTiles.contains(tile)) {
                continue;
            }

            TilePosition topRight = new TilePosition(tile.getX() + DEPOT_WIDTH, tile.getY());
            TilePosition bottomLeft = new TilePosition(tile.getX(), tile.getY() + DEPOT_HEIGHT);
            TilePosition bottomRight = new TilePosition(tile.getX() + DEPOT_WIDTH, tile.getY() + DEPOT_HEIGHT);

            if (!baseTiles.contains(topRight) || !baseTiles.contains(bottomLeft) || !baseTiles.contains(bottomRight)) {
                continue;
            }

            if (usedTiles.contains(topRight) || usedTiles.contains(bottomLeft) || usedTiles.contains(bottomRight)) {
                continue;
            }

            if (adjacentToLargeBuildTile(tile, 2 * DEPOT_WIDTH, 2 * DEPOT_HEIGHT)) {
                continue;
            }

            if (intersectsExistingBuildTiles(tile, depotType, 0)) {
                continue;
            }

            if (validMediumTileSquare(tile, depotType)) {
                mediumBuildTiles.add(tile);
                mediumBuildTiles.add(topRight);
                mediumBuildTiles.add(bottomLeft);
                mediumBuildTiles.add(bottomRight);
                usedTiles.add(tile);
                usedTiles.add(topRight);
                usedTiles.add(bottomLeft);
                usedTiles.add(bottomRight);

                for (int y = 0; y < 2 * DEPOT_HEIGHT; y++) {
                    usedTiles.add(new TilePosition(tile.getX() + 2 * DEPOT_WIDTH, tile.getY() + y));
                }
            }
        }

        for (TilePosition tile : sortedBackTiles) {
            if (mediumBuildTiles.size() >= 22) {
                break;
            }

            if (usedTiles.contains(tile)) {
                continue;
            }

            TilePosition rightTile = new TilePosition(tile.getX() + DEPOT_WIDTH, tile.getY());

            if (!baseTiles.contains(tile) || !baseTiles.contains(rightTile)) {
                continue;
            }

            if (usedTiles.contains(rightTile)) {
                continue;
            }

            if (adjacentToLargeBuildTile(tile, 2 * DEPOT_WIDTH, DEPOT_HEIGHT)) {
                continue;
            }

            if (intersectsExistingBuildTiles(tile, depotType, 0)) {
                continue;
            }

            if (validMediumTileHorizontalPair(tile, depotType)) {
                mediumBuildTiles.add(tile);
                mediumBuildTiles.add(rightTile);
                usedTiles.add(tile);
                usedTiles.add(rightTile);

                for (int y = 0; y < DEPOT_HEIGHT; y++) {
                    usedTiles.add(new TilePosition(tile.getX() + 2 * DEPOT_WIDTH, tile.getY() + y));
                }
            }
        }

        for (TilePosition tile : sortedBackTiles) {
            if (mediumBuildTiles.size() >= 22) {
                break;
            }

            if (usedTiles.contains(tile)) {
                continue;
            }

            TilePosition bottomTile = new TilePosition(tile.getX(), tile.getY() + DEPOT_HEIGHT);

            if (!baseTiles.contains(tile) || !baseTiles.contains(bottomTile)) {
                continue;
            }

            if (usedTiles.contains(bottomTile)) {
                continue;
            }

            if (adjacentToLargeBuildTile(tile, DEPOT_WIDTH, 2 * DEPOT_HEIGHT)) {
                continue;
            }

            if (intersectsExistingBuildTiles(tile, depotType, 0)) {
                continue;
            }

            if (validMediumTileVerticalStack(tile, depotType)) {
                mediumBuildTiles.add(tile);
                mediumBuildTiles.add(bottomTile);
                usedTiles.add(tile);
                usedTiles.add(bottomTile);

                for (int y = 0; y < 2 * DEPOT_HEIGHT; y++) {
                    usedTiles.add(new TilePosition(tile.getX() + DEPOT_WIDTH, tile.getY() + y));
                }
            }
        }
    }

    private boolean adjacentToLargeBuildTile(TilePosition tile, int groupWidth, int groupHeight) {
        if (adjacentToLargeInSet(tile, groupWidth, groupHeight, largeBuildTiles)) {
            return true;
        }

        return adjacentToLargeInSet(tile, groupWidth, groupHeight, largeBuildTilesNoGap);
    }

    private boolean adjacentToLargeInSet(TilePosition tile, int groupWidth, int groupHeight, Set<TilePosition> largeTiles) {
        for (TilePosition lt : largeTiles) {
            boolean xOverlap = lt.getX() < tile.getX() + groupWidth && lt.getX() + BARRACKS_WIDTH > tile.getX();
            boolean directlyAbove = lt.getY() + BARRACKS_HEIGHT == tile.getY();
            boolean directlyBelow = lt.getY() == tile.getY() + groupHeight;
            if (xOverlap && (directlyAbove || directlyBelow)) {
                return true;
            }
        }

        return false;
    }

    private boolean validMediumTileHorizontalPair(TilePosition topLeft, UnitType depotType) {
        TilePosition rightTile = new TilePosition(topLeft.getX() + DEPOT_WIDTH, topLeft.getY());

        if (!validMediumTilePositionNoGap(topLeft, depotType)) {
            return false;
        }

        if (!validMediumTilePositionNoGap(rightTile, depotType)) {
            return false;
        }

        for (int y = 0; y < DEPOT_HEIGHT; y++) {
            TilePosition gapTile = new TilePosition(topLeft.getX() + 2 * DEPOT_WIDTH, topLeft.getY() + y);

            if (intersectsExclusionZones(gapTile) || !tilePositionValidator.isWalkable(gapTile)) {
                return false;
            }
        }

        return true;
    }

    private boolean validMediumTileVerticalStack(TilePosition topTile, UnitType depotType) {
        TilePosition bottomTile = new TilePosition(topTile.getX(), topTile.getY() + DEPOT_HEIGHT);

        if (!validMediumTilePositionNoGap(topTile, depotType)) {
            return false;
        }

        if (!validMediumTilePositionNoGap(bottomTile, depotType)) {
            return false;
        }

        for (int y = 0; y < 2 * DEPOT_HEIGHT; y++) {
            TilePosition gapTile = new TilePosition(topTile.getX() + DEPOT_WIDTH, topTile.getY() + y);

            if (intersectsExclusionZones(gapTile) || !tilePositionValidator.isWalkable(gapTile)) {
                return false;
            }
        }

        return true;
    }

    private boolean validMediumTileSquare(TilePosition topLeft, UnitType depotType) {
        TilePosition topRight = new TilePosition(topLeft.getX() + DEPOT_WIDTH, topLeft.getY());
        TilePosition bottomLeft = new TilePosition(topLeft.getX(), topLeft.getY() + DEPOT_HEIGHT);
        TilePosition bottomRight = new TilePosition(topLeft.getX() + DEPOT_WIDTH, topLeft.getY() + DEPOT_HEIGHT);

        if (!validMediumTilePositionNoGap(topLeft, depotType)) {
            return false;
        }
        if (!validMediumTilePositionNoGap(topRight, depotType)) {
            return false;
        }
        if (!validMediumTilePositionNoGap(bottomLeft, depotType)) {
            return false;
        }
        if (!validMediumTilePositionNoGap(bottomRight, depotType)) {
            return false;
        }

        for (int y = 0; y < 2 * DEPOT_HEIGHT; y++) {
            TilePosition gapTile = new TilePosition(topLeft.getX() + 2 * DEPOT_WIDTH, topLeft.getY() + y);

            if (intersectsExclusionZones(gapTile) || !tilePositionValidator.isWalkable(gapTile)) {
                return false;
            }
        }

        return true;
    }

    private boolean validMediumTilePositionNoGap(TilePosition tile, UnitType depot) {
        if (intersectsExistingBuildTiles(tile, depot, 0)) {
            return false;
        }

        for (int x = 0; x < DEPOT_WIDTH; x++) {
            for (int y = 0; y < DEPOT_HEIGHT; y++) {
                TilePosition checkTile = new TilePosition(tile.getX() + x, tile.getY() + y);

                if (intersectsExclusionZones(checkTile) || !tilePositionValidator.isBuildable(checkTile)) {
                    return false;
                }
            }
        }

        return !intersectsNeutralBuildings(tile, DEPOT_WIDTH, DEPOT_HEIGHT);
    }

    private void generateCloseBunkerTile() {
        TilePosition chokePos = mapInfo.getMainChoke().getCenter().toTilePosition();
        TilePosition basePos = mapInfo.getStartingBase().getLocation();

        TilePosition finalMidPoint = PositionInterpolator.interpolate(basePos, chokePos, 0.20);
        boolean geyserInPath = false;

        for (Geyser geyser : startingBase.getGeysers()) {
            TilePosition geyserTL = geyser.getTopLeft();
            int geyserCX = geyserTL.getX() + 2;
            int geyserCY = geyserTL.getY() + 1;

            int ccToChokeX = chokePos.getX() - basePos.getX();
            int ccToChokeY = chokePos.getY() - basePos.getY();
            int ccToGeyserX = geyserCX - basePos.getX();
            int ccToGeyserY = geyserCY - basePos.getY();

            double dot = ccToGeyserX * ccToChokeX + ccToGeyserY * ccToChokeY;
            double chokeDistSq = ccToChokeX * ccToChokeX + ccToChokeY * ccToChokeY;
            double geyserDistSq = ccToGeyserX * ccToGeyserX + ccToGeyserY * ccToGeyserY;

            if (dot > 0 && geyserDistSq < chokeDistSq) {
                geyserInPath = true;
                double geyserDist = Math.sqrt(geyserDistSq);
                if (geyserDist > 0) {
                    double newDist = Math.max(geyserDist - 3, 0);
                    int newX = (int)(basePos.getX() + ccToGeyserX / geyserDist * newDist);
                    int newY = (int)(basePos.getY() + ccToGeyserY / geyserDist * newDist);
                    finalMidPoint = new TilePosition(newX, newY);
                }
            }
        }

        int searchRadius = 3;
        int closestDistance = Integer.MAX_VALUE;

        int midX = finalMidPoint.getX();
        int midY = finalMidPoint.getY();

        for (int x = midX - searchRadius; x <= midX + searchRadius; x++) {
            for (int y = midY - searchRadius; y <= midY + searchRadius; y++) {
                TilePosition testPos = new TilePosition(x, y);
                boolean validLocation = true;

                for (int bx = x; bx < x + BUNKER_WIDTH; bx++) {
                    for (int by = y; by < y + BUNKER_HEIGHT; by++) {
                        TilePosition footprintTile = new TilePosition(bx, by);
                        boolean excluded;
                        if (geyserInPath) {
                            excluded = mineralExlusionTiles.contains(footprintTile)
                                    || ccExclusionTiles.contains(footprintTile)
                                    || chokeExclusionTiles.contains(footprintTile)
                                    || mineralAboveGapTiles.contains(footprintTile);
                        }
                        else {
                            excluded = intersectsExclusionZones(footprintTile);
                        }
                        if (excluded) {
                            validLocation = false;
                            break;
                        }
                    }
                    if (!validLocation) {
                        break;
                    }
                }

                if (!validLocation) continue;

                if (!tilePositionValidator.isBuildable(testPos, UnitType.Terran_Bunker)) {
                    continue;
                }

                int distToMid = testPos.getApproxDistance(finalMidPoint);
                if (distToMid < closestDistance) {
                    closestDistance = distToMid;
                    closeBunkerTile = testPos;
                }
            }
        }
    }

    public void generateProxyBunkerTile(Base enemyNatural) {
        proxyBunkerTile = null;

        if (enemyNatural == null || enemyNatural.getArea() == null) {
            return;
        }

        List<ChokePoint> enemyNaturalChokes = enemyNatural.getArea().getChokes();

        if (enemyNaturalChokes == null || enemyNaturalChokes.isEmpty()) {
            return;
        }

        Base enemyMain = mapInfo.getEnemyMain();
        if (enemyMain == null || enemyMain.getArea() == null) {
            return;
        }

        ChokePoint facingChoke = null;
        for (ChokePoint candidateChoke : enemyNaturalChokes) {
            Area otherArea = candidateChoke.getFirstArea();
            if (otherArea == enemyNatural.getArea()) {
                otherArea = candidateChoke.getSecondArea();
            }
            if (otherArea != enemyMain.getArea()) {
                facingChoke = candidateChoke;
                break;
            }
        }

        if (facingChoke == null) {
            return;
        }

        Area outsideArea;
        if (facingChoke.getFirstArea() != enemyNatural.getArea()) {
            outsideArea = facingChoke.getFirstArea();
        }
        else {
            outsideArea = facingChoke.getSecondArea();
        }

        if (outsideArea == null) {
            return;
        }

        HashSet<TilePosition> outsideTiles = mapInfo.getAreaTiles().get(outsideArea);
        HashSet<TilePosition> enemyNaturalAreaTiles = mapInfo.getAreaTiles().get(enemyNatural.getArea());

        HashSet<TilePosition> resourceTiles = new HashSet<>();
        for (Mineral mineral : enemyNatural.getMinerals()) {
            TilePosition tl = mineral.getTopLeft();
            for (int rx = 0; rx < 2; rx++) {
                resourceTiles.add(new TilePosition(tl.getX() + rx, tl.getY()));
            }
        }
        for (Geyser geyser : enemyNatural.getGeysers()) {
            TilePosition tl = geyser.getTopLeft();
            for (int rx = 0; rx < 4; rx++) {
                for (int ry = 0; ry < 2; ry++) {
                    resourceTiles.add(new TilePosition(tl.getX() + rx, tl.getY() + ry));
                }
            }
        }

        TilePosition enemyDepotTile = enemyNatural.getLocation();
        int depotMinXPx = enemyDepotTile.getX() * 32;
        int depotMinYPx = enemyDepotTile.getY() * 32;
        int depotMaxXPx = depotMinXPx + CC_WIDTH * 32;
        int depotMaxYPx = depotMinYPx + CC_HEIGHT * 32;

        TilePosition chokeTile = facingChoke.getCenter().toTilePosition();
        TilePosition depotCenterTile = new TilePosition(enemyDepotTile.getX() + CC_WIDTH / 2, enemyDepotTile.getY() + CC_HEIGHT / 2);

        int chokeDirX = chokeTile.getX() - depotCenterTile.getX();
        int chokeDirY = chokeTile.getY() - depotCenterTile.getY();

        TilePosition bestProxy = null;
        int bestDistanceToDepot = Integer.MAX_VALUE;
        long bestPerpFromLine = Long.MAX_VALUE;

        for (int dx = -14; dx <= 14; dx++) {
            for (int dy = -14; dy <= 14; dy++) {
                TilePosition candidateTile = new TilePosition(depotCenterTile.getX() + dx, depotCenterTile.getY() + dy);

                if (!tilePositionValidator.isWithinMap(candidateTile)) {
                    continue;
                }

                if (!tilePositionValidator.isBuildable(candidateTile, UnitType.Terran_Bunker)) {
                    continue;
                }

                int candidateDirX = candidateTile.getX() - depotCenterTile.getX();
                int candidateDirY = candidateTile.getY() - depotCenterTile.getY();
                int dotProduct = chokeDirX * candidateDirX + chokeDirY * candidateDirY;
                if (dotProduct <= 0) {
                    continue;
                }

                boolean footprintValid = true;
                boolean anyFootprintInNatural = false;

                for (int fx = 0; fx < BUNKER_WIDTH && footprintValid; fx++) {
                    for (int fy = 0; fy < BUNKER_HEIGHT; fy++) {
                        TilePosition footprintTile = new TilePosition(candidateTile.getX() + fx, candidateTile.getY() + fy);

                        if (!tilePositionValidator.isWithinMap(footprintTile)) {
                            footprintValid = false;
                            break;
                        }

                        if (outsideTiles != null && outsideTiles.contains(footprintTile)) {
                            footprintValid = false;
                            break;
                        }

                        if (resourceTiles.contains(footprintTile)) {
                            footprintValid = false;
                            break;
                        }

                        if (enemyNaturalAreaTiles != null && enemyNaturalAreaTiles.contains(footprintTile)) {
                            anyFootprintInNatural = true;
                        }
                    }
                }

                if (!footprintValid) {
                    continue;
                }

                if (!anyFootprintInNatural) {
                    continue;
                }

                int bunkerCenterPxX = candidateTile.getX() * 32 + BUNKER_WIDTH * 32 / 2;
                int bunkerCenterPxY = candidateTile.getY() * 32 + BUNKER_HEIGHT * 32 / 2;
                int depotCenterPxX = (depotMinXPx + depotMaxXPx) / 2;
                int depotCenterPxY = (depotMinYPx + depotMaxYPx) / 2;

                int centerDX = bunkerCenterPxX - depotCenterPxX;
                int centerDY = bunkerCenterPxY - depotCenterPxY;
                double centerToCenterPx = Math.sqrt((double) centerDX * centerDX + (double) centerDY * centerDY);

                if (centerToCenterPx <= 4 * 32) {
                    continue;
                }

                int dxC = 0;
                if (bunkerCenterPxX < depotMinXPx) {
                    dxC = depotMinXPx - bunkerCenterPxX;
                }
                else if (bunkerCenterPxX > depotMaxXPx) {
                    dxC = bunkerCenterPxX - depotMaxXPx;
                }

                int dyC = 0;
                if (bunkerCenterPxY < depotMinYPx) {
                    dyC = depotMinYPx - bunkerCenterPxY;
                }
                else if (bunkerCenterPxY > depotMaxYPx) {
                    dyC = bunkerCenterPxY - depotMaxYPx;
                }

                int bunkerCenterToDepotEdgePx = (int) Math.sqrt((double) dxC * dxC + (double) dyC * dyC);

                if (bunkerCenterToDepotEdgePx > 5 * 32) {
                    continue;
                }

                long perpFromLine = Math.abs((long) chokeDirX * candidateDirY - (long) chokeDirY * candidateDirX);
                int distToDepot = candidateTile.getApproxDistance(depotCenterTile);

                if (perpFromLine < bestPerpFromLine || (perpFromLine == bestPerpFromLine && distToDepot < bestDistanceToDepot)) {
                    bestPerpFromLine = perpFromLine;
                    bestDistanceToDepot = distToDepot;
                    bestProxy = candidateTile;
                }
            }
        }

        proxyBunkerTile = bestProxy;
    }

    private void generateChokeBunkerTiles() {
        ChokePoint mainChoke = mapInfo.getMainChoke();

        if (mainChoke != null) {
            TilePosition chokeTile = mainChoke.getCenter().toTilePosition();
            TilePosition baseTile = mapInfo.getStartingBase().getLocation();
            double percent = 0.90;

            //temp solution fix later
            if (mapInfo.getMinOnlyBase() != null) {
                ChokePoint otherChoke = null;

                for (ChokePoint cp : mapInfo.getMinOnlyBase().getArea().getChokes()) {
                    if (cp.equals(mainChoke)) {
                        continue;
                    }

                    otherChoke = cp;
                }

                if (otherChoke != null) {
                    TilePosition minOnlyLocation = mapInfo.getMinOnlyBase().getLocation();
                    TilePosition otherChokePos = otherChoke.getCenter().toTilePosition();
                    int midX = (minOnlyLocation.getX() + otherChokePos.getX()) / 2;
                    int midY = (minOnlyLocation.getY() + otherChokePos.getY()) / 2;
                    baseTile = new TilePosition(midX, midY);
                } else {
                    baseTile = mapInfo.getMinOnlyBase().getLocation();
                }
                percent = 0.80;
            }

            TilePosition closerMain = PositionInterpolator.interpolate(baseTile, chokeTile, percent);
            TilePosition mainBunker = findValidTileNear(closerMain, UnitType.Terran_Bunker);

            int minDist = Integer.MAX_VALUE;

            if (!mapInfo.getMinBaseTiles().isEmpty()) {
                for (TilePosition candidate : mapInfo.getMinBaseTiles()) {
                    if (tilePositionValidator.isBuildable(candidate, UnitType.Terran_Bunker) && !intersectsExclusionZones(candidate)
                            && !intersectsExistingBuildTiles(candidate, UnitType.Terran_Bunker, 0)) {
                        int dist = candidate.getApproxDistance(closerMain);
                        if (dist < minDist) {
                            minDist = dist;
                            mainBunker = candidate;
                        }
                    }
                }

                if (mainBunker != null && (mapInfo.getMinBaseTiles().contains(mainBunker))) {
                    mainChokeBunker = mainBunker;
                }

            }
            else {
                for (TilePosition candidate : frontBaseTiles) {
                    if (tilePositionValidator.isBuildable(candidate, UnitType.Terran_Bunker) && !intersectsExclusionZones(candidate)
                            && !intersectsExistingBuildTiles(candidate, UnitType.Terran_Bunker, 0)) {
                        int dist = candidate.getApproxDistance(closerMain);
                        if (dist < minDist) {
                            minDist = dist;
                            mainBunker = candidate;
                        }
                    }
                }

                if (mainBunker != null && (frontBaseTiles.contains(mainBunker))) {
                    mainChokeBunker = mainBunker;
                }
            }
        }

        ChokePoint naturalChoke = mapInfo.getNaturalChoke();

        if (naturalChoke != null) {
            ChokePoint secondaryNaturalChoke = mapInfo.getSecondaryNaturalChoke();
            TilePosition chokeTile;
            if (secondaryNaturalChoke == null) {
                chokeTile = naturalChoke.getCenter().toTilePosition();
            }
            else {
                TilePosition primaryChokeTile = naturalChoke.getCenter().toTilePosition();
                TilePosition secondaryChokeTile = secondaryNaturalChoke.getCenter().toTilePosition();
                Position mainChokeCenter = mapInfo.getMainChoke().getCenter();

                double primaryWeight = 0.6;
                double secondaryWeight = 0.4;
                if (secondaryChokeTile.toPosition().getApproxDistance(mainChokeCenter) < primaryChokeTile.toPosition().getApproxDistance(mainChokeCenter)) {
                    primaryWeight = 0.4;
                    secondaryWeight = 0.6;
                }

                int midX = (int) (primaryChokeTile.getX() * primaryWeight + secondaryChokeTile.getX() * secondaryWeight);
                int midY = (int) (primaryChokeTile.getY() * primaryWeight + secondaryChokeTile.getY() * secondaryWeight);
                chokeTile = new TilePosition(midX, midY);
            }

            TilePosition baseTile = mapInfo.getNaturalBase().getLocation();
            TilePosition closerNatural = PositionInterpolator.interpolate(chokeTile, baseTile, 0.10);
            TilePosition naturalBunker = findValidTileNear(closerNatural, UnitType.Terran_Bunker, mapInfo.getNaturalTiles());

            if (naturalBunker != null) {
                naturalChokeBunker = naturalBunker;
                computeNaturalBunkerWall(naturalBunker, chokeTile);
                mapInfo.setNaturalChokeEdgeFromBunker(naturalChokeBunker);
            }
        }
    }

    private void computeNaturalBunkerWall(TilePosition bunkerTile, TilePosition chokeTile) {
        naturalBunkerEbayPosition = null;
        naturalBunkerBarracksPosition = null;
        naturalBunkerDepotPosition = null;

        int dx = chokeTile.getX() - bunkerTile.getX();
        int dy = chokeTile.getY() - bunkerTile.getY();

        int bunkerX = bunkerTile.getX();
        int bunkerY = bunkerTile.getY();
        int bunkerXEnd = bunkerX + BUNKER_WIDTH;
        int bunkerYEnd = bunkerY + BUNKER_HEIGHT;

        int searchRange = 6;
        int[] frontOffsets = {0, 1, -1};

        boolean horizontalAxis = Math.abs(dx) >= Math.abs(dy);

        int barracksFrontExtent;
        if (horizontalAxis) {
            barracksFrontExtent = BARRACKS_WIDTH;
        }
        else {
            barracksFrontExtent = BARRACKS_HEIGHT;
        }

        int frontStaggerBound = barracksFrontExtent - 1;
        int[] barracksFrontOffsets = new int[frontStaggerBound * 2 + 1];
        barracksFrontOffsets[0] = 0;
        for (int magnitude = 1; magnitude <= frontStaggerBound; magnitude++) {
            barracksFrontOffsets[magnitude * 2 - 1] = magnitude;
            barracksFrontOffsets[magnitude * 2] = -magnitude;
        }

        int barracksPreferredSign;
        if (horizontalAxis) {
            int naturalDy = mapInfo.getNaturalBase().getLocation().getY() - bunkerTile.getY();
            barracksPreferredSign = signOrDefault(-naturalDy, 1);
        }
        else {
            int naturalDx = mapInfo.getNaturalBase().getLocation().getX() - bunkerTile.getX();
            barracksPreferredSign = signOrDefault(-naturalDx, 1);
        }

        int[] sideSigns = {barracksPreferredSign, -barracksPreferredSign};

        TilePosition mainBaseTile = mapInfo.getStartingBase().getLocation();
        int mainSideSign;
        if (horizontalAxis) {
            int mainPerpDelta = mainBaseTile.getY() - bunkerTile.getY();
            mainSideSign = signOrDefault(mainPerpDelta, 1);
        }
        else {
            int mainPerpDelta = mainBaseTile.getX() - bunkerTile.getX();
            mainSideSign = signOrDefault(mainPerpDelta, 1);
        }

        TilePosition bestEbay = null;
        TilePosition bestBarracks = null;
        boolean bestBothWalls = false;
        boolean bestPairNearWall = false;
        boolean bestEbayWallAdjacent = false;
        boolean bestBarracksWallAdjacent = false;
        int bestSlack = Integer.MAX_VALUE;
        int bestSplitScore = Integer.MAX_VALUE;
        int bestPerpDist = Integer.MAX_VALUE;
        int bestPairDist = Integer.MAX_VALUE;

        for (int eFrontOffset : frontOffsets) {
            for (int eSideSign : sideSigns) {
                for (int eSideMag = 0; eSideMag <= searchRange; eSideMag++) {
                    int eSideOffset = eSideMag * eSideSign;
                    if (eSideMag == 0 && eSideSign != sideSigns[0]) {
                        continue;
                    }

                    int eX;
                    int eY;

                    if (horizontalAxis) {
                        int baseFrontX;
                        if (dx > 0) {
                            baseFrontX = bunkerX + BUNKER_WIDTH;
                        }
                        else {
                            baseFrontX = bunkerX - EBAY_WIDTH;
                        }

                        int frontShift;
                        if (dx > 0) {
                            frontShift = eFrontOffset;
                        }
                        else {
                            frontShift = -eFrontOffset;
                        }

                        eX = baseFrontX + frontShift;
                        eY = bunkerY + eSideOffset;
                    }
                    else {
                        int baseFrontY;
                        if (dy > 0) {
                            baseFrontY = bunkerY + BUNKER_HEIGHT;
                        }
                        else {
                            baseFrontY = bunkerY - EBAY_HEIGHT;
                        }

                        int frontShift;
                        if (dy > 0) {
                            frontShift = eFrontOffset;
                        }
                        else {
                            frontShift = -eFrontOffset;
                        }

                        eX = bunkerX + eSideOffset;
                        eY = baseFrontY + frontShift;
                    }

                    TilePosition ebayCandidate = new TilePosition(eX, eY);

                    if (!tilePositionValidator.isBuildable(ebayCandidate, UnitType.Terran_Engineering_Bay)) {
                        continue;
                    }
                    if (intersectsExclusionZones(ebayCandidate)) {
                        continue;
                    }
                    if (intersectsExistingBuildTiles(ebayCandidate, UnitType.Terran_Engineering_Bay, 0)) {
                        continue;
                    }

                    int eXEnd = eX + EBAY_WIDTH;
                    int eYEnd = eY + EBAY_HEIGHT;

                    if (rectanglesIntersect(eX, eY, eXEnd, eYEnd, bunkerX, bunkerY, bunkerXEnd, bunkerYEnd)) {
                        continue;
                    }

                    boolean ebayBunkerAdjacent = rectanglesShareEdge(eX, eY, eXEnd, eYEnd, bunkerX, bunkerY, bunkerXEnd, bunkerYEnd);

                    WallAdjacency ebayAdj = scanFootprintWallAdjacency(eX, eY, EBAY_WIDTH, EBAY_HEIGHT, bunkerX, bunkerY, horizontalAxis, mainSideSign);
                    boolean ebayWallAdjacent = ebayAdj.wallAdjacent;
                    boolean ebayNearWall = ebayAdj.nearWall;

                    for (int bFrontOffset : barracksFrontOffsets) {
                        for (int bStackOffset : frontOffsets) {
                            int bX;
                            int bY;

                            if (horizontalAxis) {
                                int baseAnchorY;
                                if (barracksPreferredSign > 0) {
                                    baseAnchorY = eY + EBAY_HEIGHT;
                                }
                                else {
                                    baseAnchorY = eY - BARRACKS_HEIGHT;
                                }

                                int stackShift = bStackOffset * barracksPreferredSign;
                                int frontShiftB;
                                if (dx > 0) {
                                    frontShiftB = bFrontOffset;
                                }
                                else {
                                    frontShiftB = -bFrontOffset;
                                }

                                bX = eX + frontShiftB;
                                bY = baseAnchorY + stackShift;
                            }
                            else {
                                int baseAnchorX;
                                if (barracksPreferredSign > 0) {
                                    baseAnchorX = eX + EBAY_WIDTH;
                                }
                                else {
                                    baseAnchorX = eX - BARRACKS_WIDTH;
                                }

                                int stackShift = bStackOffset * barracksPreferredSign;
                                int frontShiftB;
                                if (dy > 0) {
                                    frontShiftB = bFrontOffset;
                                }
                                else {
                                    frontShiftB = -bFrontOffset;
                                }

                                bX = baseAnchorX + stackShift;
                                bY = eY + frontShiftB;
                            }

                            TilePosition barracksCandidate = new TilePosition(bX, bY);

                            if (!tilePositionValidator.isBuildable(barracksCandidate, UnitType.Terran_Barracks)) {
                                continue;
                            }
                            if (intersectsExclusionZones(barracksCandidate)) {
                                continue;
                            }
                            if (intersectsExistingBuildTiles(barracksCandidate, UnitType.Terran_Barracks, 0)) {
                                continue;
                            }

                            int bXEnd = bX + BARRACKS_WIDTH;
                            int bYEnd = bY + BARRACKS_HEIGHT;

                            if (rectanglesIntersect(eX, eY, eXEnd, eYEnd, bX, bY, bXEnd, bYEnd)) {
                                continue;
                            }
                            if (rectanglesIntersect(bX, bY, bXEnd, bYEnd, bunkerX, bunkerY, bunkerXEnd, bunkerYEnd)) {
                                continue;
                            }
                            if (!rectanglesShareEdge(eX, eY, eXEnd, eYEnd, bX, bY, bXEnd, bYEnd)) {
                                continue;
                            }

                            boolean barracksBunkerAdjacent = rectanglesShareEdge(bX, bY, bXEnd, bYEnd, bunkerX, bunkerY, bunkerXEnd, bunkerYEnd);

                            WallAdjacency barracksAdj = scanFootprintWallAdjacency(bX, bY, BARRACKS_WIDTH, BARRACKS_HEIGHT, bunkerX, bunkerY, horizontalAxis, mainSideSign);
                            boolean barracksWallAdjacent = barracksAdj.wallAdjacent;
                            boolean barracksNearWall = barracksAdj.nearWall;

                            boolean bunkerAdjacencySatisfied = ebayBunkerAdjacent || barracksBunkerAdjacent;
                            boolean wallAdjacencySatisfied = ebayWallAdjacent || barracksWallAdjacent;

                            if (!bunkerAdjacencySatisfied) {
                                continue;
                            }
                            if (!wallAdjacencySatisfied) {
                                continue;
                            }

                            int slack = Math.abs(eFrontOffset) + Math.abs(eSideOffset) + Math.abs(bFrontOffset) + Math.abs(bStackOffset);

                            boolean pairNearWall = ebayNearWall || barracksNearWall;

                            int pMin;
                            int pMax;
                            int fMin;
                            int fMax;
                            if (horizontalAxis) {
                                pMin = Math.min(eY, bY);
                                pMax = Math.max(eYEnd, bYEnd);
                                fMin = Math.min(eX, bX);
                                fMax = Math.max(eXEnd, bXEnd);
                            }
                            else {
                                pMin = Math.min(eX, bX);
                                pMax = Math.max(eXEnd, bXEnd);
                                fMin = Math.min(eY, bY);
                                fMax = Math.max(eYEnd, bYEnd);
                            }

                            boolean lowSideContacted = false;
                            boolean highSideContacted = false;
                            for (int f = fMin; f < fMax; f++) {
                                int lowTileX;
                                int lowTileY;
                                int highTileX;
                                int highTileY;
                                if (horizontalAxis) {
                                    lowTileX = f;
                                    lowTileY = pMin - 1;
                                    highTileX = f;
                                    highTileY = pMax;
                                }
                                else {
                                    lowTileX = pMin - 1;
                                    lowTileY = f;
                                    highTileX = pMax;
                                    highTileY = f;
                                }

                                if (!tilePositionValidator.isWalkable(new TilePosition(lowTileX, lowTileY))) {
                                    lowSideContacted = true;
                                }
                                if (!tilePositionValidator.isWalkable(new TilePosition(highTileX, highTileY))) {
                                    highSideContacted = true;
                                }
                            }

                            boolean bothWallsContacted = lowSideContacted && highSideContacted;

                            int splitScore;
                            boolean ebayCovers = ebayBunkerAdjacent && ebayWallAdjacent;
                            boolean barracksCovers = barracksBunkerAdjacent && barracksWallAdjacent;
                            if (ebayCovers || barracksCovers) {
                                splitScore = 1;
                            }
                            else {
                                splitScore = 0;
                            }

                            int perpDist;
                            int bunkerCenterX = bunkerX * 32 + (BUNKER_WIDTH * 32) / 2;
                            int bunkerCenterY = bunkerY * 32 + (BUNKER_HEIGHT * 32) / 2;
                            int eCenterX = eX * 32 + (EBAY_WIDTH * 32) / 2;
                            int eCenterY = eY * 32 + (EBAY_HEIGHT * 32) / 2;
                            int bCenterX = bX * 32 + (BARRACKS_WIDTH * 32) / 2;
                            int bCenterY = bY * 32 + (BARRACKS_HEIGHT * 32) / 2;

                            if (horizontalAxis) {
                                perpDist = Math.abs(eCenterY - bunkerCenterY) + Math.abs(bCenterY - bunkerCenterY);
                            }
                            else {
                                perpDist = Math.abs(eCenterX - bunkerCenterX) + Math.abs(bCenterX - bunkerCenterX);
                            }

                            int dxPair = bCenterX - bunkerCenterX;
                            int dyPair = bCenterY - bunkerCenterY;
                            int pairDist = (int) Math.sqrt(dxPair * dxPair + dyPair * dyPair);

                            boolean better = false;
                            if (bestEbay == null) {
                                better = true;
                            }
                            else if (bothWallsContacted && !bestBothWalls) {
                                better = true;
                            }
                            else if (!bothWallsContacted && bestBothWalls) {
                                better = false;
                            }
                            else if (pairNearWall && !bestPairNearWall) {
                                better = true;
                            }
                            else if (!pairNearWall && bestPairNearWall) {
                                better = false;
                            }
                            else if (slack < bestSlack) {
                                better = true;
                            }
                            else if (slack == bestSlack) {
                                if (splitScore < bestSplitScore) {
                                    better = true;
                                }
                                else if (splitScore == bestSplitScore) {
                                    if (perpDist < bestPerpDist) {
                                        better = true;
                                    }
                                    else if (perpDist == bestPerpDist) {
                                        if (pairDist < bestPairDist) {
                                            better = true;
                                        }
                                    }
                                }
                            }

                            if (better) {
                                bestBothWalls = bothWallsContacted;
                                bestPairNearWall = pairNearWall;
                                bestSlack = slack;
                                bestSplitScore = splitScore;
                                bestPerpDist = perpDist;
                                bestPairDist = pairDist;
                                bestEbay = ebayCandidate;
                                bestBarracks = barracksCandidate;
                                bestEbayWallAdjacent = ebayWallAdjacent;
                                bestBarracksWallAdjacent = barracksWallAdjacent;
                            }
                        }
                    }
                }
            }
        }

        if (bestEbay != null) {
            naturalBunkerEbayPosition = bestEbay;
        }

        if (bestBarracks != null) {
            naturalBunkerBarracksPosition = bestBarracks;
        }

        if (bestEbay != null && bestBarracks != null && !(bestEbayWallAdjacent && bestBarracksWallAdjacent)) {

            int eX = bestEbay.getX();
            int eY = bestEbay.getY();
            int eXEnd = eX + EBAY_WIDTH;
            int eYEnd = eY + EBAY_HEIGHT;

            int bX = bestBarracks.getX();
            int bY = bestBarracks.getY();
            int bXEnd = bX + BARRACKS_WIDTH;
            int bYEnd = bY + BARRACKS_HEIGHT;

            int wallToucherCenterPerp;
            if (bestEbayWallAdjacent) {
                if (horizontalAxis) {
                    wallToucherCenterPerp = (eY * 32 + (EBAY_HEIGHT * 32) / 2) - (bunkerY * 32 + (BUNKER_HEIGHT * 32) / 2);
                }
                else {
                    wallToucherCenterPerp = (eX * 32 + (EBAY_WIDTH * 32) / 2) - (bunkerX * 32 + (BUNKER_WIDTH * 32) / 2);
                }
            }
            else {
                if (horizontalAxis) {
                    wallToucherCenterPerp = (bY * 32 + (BARRACKS_HEIGHT * 32) / 2) - (bunkerY * 32 + (BUNKER_HEIGHT * 32) / 2);
                }
                else {
                    wallToucherCenterPerp = (bX * 32 + (BARRACKS_WIDTH * 32) / 2) - (bunkerX * 32 + (BUNKER_WIDTH * 32) / 2);
                }
            }

            int coveredSideSign = signOrDefault(wallToucherCenterPerp, 1);

            int depotSideSign = -coveredSideSign;

            int chokeFrontSign;
            if (horizontalAxis) {
                chokeFrontSign = signOrDefault(dx, 1);
            }
            else {
                chokeFrontSign = signOrDefault(dy, 1);
            }

            int bunkerCenterX = bunkerX * 32 + (BUNKER_WIDTH * 32) / 2;
            int bunkerCenterY = bunkerY * 32 + (BUNKER_HEIGHT * 32) / 2;

            TilePosition bestDepot = null;
            int bestSharedEdge = -1;
            int bestManhattan = Integer.MAX_VALUE;

            for (int dxOffset = -searchRange; dxOffset <= searchRange; dxOffset++) {
                for (int dyOffset = -searchRange; dyOffset <= searchRange; dyOffset++) {
                    int candX = bunkerX + dxOffset;
                    int candY = bunkerY + dyOffset;

                    TilePosition depotCandidate = new TilePosition(candX, candY);
                    int candXEnd = candX + DEPOT_WIDTH;
                    int candYEnd = candY + DEPOT_HEIGHT;

                    if (!tilePositionValidator.isBuildable(depotCandidate, UnitType.Terran_Supply_Depot)) {
                        continue;
                    }
                    if (intersectsExclusionZones(depotCandidate)) {
                        continue;
                    }
                    if (intersectsExistingBuildTiles(depotCandidate, UnitType.Terran_Supply_Depot, 0)) {
                        continue;
                    }
                    if (rectanglesIntersect(candX, candY, candXEnd, candYEnd, eX, eY, eXEnd, eYEnd)) {
                        continue;
                    }
                    if (rectanglesIntersect(candX, candY, candXEnd, candYEnd, bX, bY, bXEnd, bYEnd)) {
                        continue;
                    }
                    if (rectanglesIntersect(candX, candY, candXEnd, candYEnd, bunkerX, bunkerY, bunkerXEnd, bunkerYEnd)) {
                        continue;
                    }

                    int candCenterX = candX * 32 + (DEPOT_WIDTH * 32) / 2;
                    int candCenterY = candY * 32 + (DEPOT_HEIGHT * 32) / 2;

                    int candSideSign;
                    if (horizontalAxis) {
                        int sidePerp = candCenterY - bunkerCenterY;
                        candSideSign = signOrDefault(sidePerp, 1);
                    }
                    else {
                        int sidePerp = candCenterX - bunkerCenterX;
                        candSideSign = signOrDefault(sidePerp, 1);
                    }

                    if (candSideSign != depotSideSign) {
                        continue;
                    }

                    int candFrontSign;
                    if (horizontalAxis) {
                        int frontPerp = candCenterX - bunkerCenterX;
                        candFrontSign = signOrDefault(frontPerp, 1);
                    }
                    else {
                        int frontPerp = candCenterY - bunkerCenterY;
                        candFrontSign = signOrDefault(frontPerp, 1);
                    }

                    if (candFrontSign != chokeFrontSign) {
                        continue;
                    }

                    boolean depotWallAdjacent = false;
                    for (int i = 0; i < DEPOT_WIDTH; i++) {
                        if (!tilePositionValidator.isWalkable(new TilePosition(candX + i, candY - 1))) {
                            depotWallAdjacent = true;
                            break;
                        }
                        if (!tilePositionValidator.isWalkable(new TilePosition(candX + i, candY + DEPOT_HEIGHT))) {
                            depotWallAdjacent = true;
                            break;
                        }
                    }
                    if (!depotWallAdjacent) {
                        for (int j = 0; j < DEPOT_HEIGHT; j++) {
                            if (!tilePositionValidator.isWalkable(new TilePosition(candX - 1, candY + j))) {
                                depotWallAdjacent = true;
                                break;
                            }
                            if (!tilePositionValidator.isWalkable(new TilePosition(candX + DEPOT_WIDTH, candY + j))) {
                                depotWallAdjacent = true;
                                break;
                            }
                        }
                    }

                    if (!depotWallAdjacent) {
                        continue;
                    }

                    int sharedEdge = 0;
                    sharedEdge += sharedEdgeLength(candX, candY, candXEnd, candYEnd, eX, eY, eXEnd, eYEnd);
                    sharedEdge += sharedEdgeLength(candX, candY, candXEnd, candYEnd, bX, bY, bXEnd, bYEnd);
                    sharedEdge += sharedEdgeLength(candX, candY, candXEnd, candYEnd, bunkerX, bunkerY, bunkerXEnd, bunkerYEnd);

                    if (sharedEdge <= 0) {
                        continue;
                    }

                    int manhattan = Math.abs(candCenterX - bunkerCenterX) + Math.abs(candCenterY - bunkerCenterY);

                    boolean betterDepot = false;
                    if (bestDepot == null) {
                        betterDepot = true;
                    }
                    else if (sharedEdge > bestSharedEdge) {
                        betterDepot = true;
                    }
                    else if (sharedEdge == bestSharedEdge && manhattan < bestManhattan) {
                        betterDepot = true;
                    }

                    if (betterDepot) {
                        bestDepot = depotCandidate;
                        bestSharedEdge = sharedEdge;
                        bestManhattan = manhattan;
                    }
                }
            }

            if (bestDepot != null) {
                naturalBunkerDepotPosition = bestDepot;
            }
        }

        mapInfo.setNaturalBunkerEbayPosition(naturalBunkerEbayPosition);
        mapInfo.setNaturalBunkerBarracksPosition(naturalBunkerBarracksPosition);
        mapInfo.setNaturalBunkerDepotPosition(naturalBunkerDepotPosition);
    }

    private WallAdjacency scanFootprintWallAdjacency(int x, int y, int w, int h,
                                                     int bunkerX, int bunkerY,
                                                     boolean horizontalAxis, int mainSideSign) {
        WallAdjacency result = new WallAdjacency();

        for (int i = 0; i < w; i++) {
            checkWallTile(x + i, y - 1, bunkerX, bunkerY, horizontalAxis, mainSideSign, result);
            checkWallTile(x + i, y + h, bunkerX, bunkerY, horizontalAxis, mainSideSign, result);
        }

        for (int j = 0; j < h; j++) {
            checkWallTile(x - 1, y + j, bunkerX, bunkerY, horizontalAxis, mainSideSign, result);
            checkWallTile(x + w, y + j, bunkerX, bunkerY, horizontalAxis, mainSideSign, result);
        }

        return result;
    }

    private void checkWallTile(int tileX, int tileY, int bunkerX, int bunkerY,
                               boolean horizontalAxis, int mainSideSign, WallAdjacency result) {
        if (tilePositionValidator.isWalkable(new TilePosition(tileX, tileY))) {
            return;
        }

        result.wallAdjacent = true;

        int wallPerp;
        if (horizontalAxis) {
            wallPerp = tileY - bunkerY;
        }
        else {
            wallPerp = tileX - bunkerX;
        }

        if (wallPerp == 0 || Integer.signum(wallPerp) == mainSideSign) {
            result.nearWall = true;
        }
    }

    private boolean rectanglesShareEdge(int aX, int aY, int aXEnd, int aYEnd,
                                        int bX, int bY, int bXEnd, int bYEnd) {
        boolean verticalTouch = (aX == bXEnd) || (aXEnd == bX);
        if (verticalTouch) {
            int yOverlap = Math.min(aYEnd, bYEnd) - Math.max(aY, bY);
            if (yOverlap >= 1) {
                return true;
            }
        }

        boolean horizontalTouch = (aY == bYEnd) || (aYEnd == bY);
        if (horizontalTouch) {
            int xOverlap = Math.min(aXEnd, bXEnd) - Math.max(aX, bX);
            if (xOverlap >= 1) {
                return true;
            }
        }

        return false;
    }

    private int sharedEdgeLength(int aX, int aY, int aXEnd, int aYEnd,
                                 int bX, int bY, int bXEnd, int bYEnd) {
        int total = 0;

        if ((aX == bXEnd) || (aXEnd == bX)) {
            int yOverlap = Math.min(aYEnd, bYEnd) - Math.max(aY, bY);
            if (yOverlap >= 1) {
                total += yOverlap;
            }
        }

        if ((aY == bYEnd) || (aYEnd == bY)) {
            int xOverlap = Math.min(aXEnd, bXEnd) - Math.max(aX, bX);
            if (xOverlap >= 1) {
                total += xOverlap;
            }
        }

        return total;
    }

    private int signOrDefault(int value, int defaultSign) {
        if (value > 0) {
            return 1;
        }
        if (value < 0) {
            return -1;
        }

        return defaultSign;
    }

    private TilePosition findValidTileNear(TilePosition center, UnitType unitType) {
        return findValidTileNear(center, unitType, null);
    }

    private TilePosition findValidTileNear(TilePosition center, UnitType unitType, HashSet<TilePosition> requiredTiles) {
        int searchRadius = 6;

        for (int r = 0; r <= searchRadius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    if (Math.abs(dx) != r && Math.abs(dy) != r) continue;
                    TilePosition test = new TilePosition(center.getX() + dx, center.getY() + dy);
                    if (requiredTiles != null && !requiredTiles.contains(test)) {
                        continue;
                    }
                    if (tilePositionValidator.isBuildable(test, unitType) && !intersectsExclusionZones(test) && !intersectsExistingBuildTiles(test, unitType, 0)) {
                        return test;
                    }
                }
            }
        }
        return null;
    }

    //Force choke turrets to be as close to the bunker as possible
    private TilePosition generateChokeTurretTile(TilePosition bunkerTile, Base base) {
        int bx = bunkerTile.getX();
        int by = bunkerTile.getY();

        List<TilePosition> adjacentPositions = new ArrayList<>();

        for (int y = by - TURRET_HEIGHT + 1; y <= by + BUNKER_HEIGHT - 1; y++) {
            adjacentPositions.add(new TilePosition(bx - TURRET_WIDTH, y));
        }

        for (int y = by - TURRET_HEIGHT + 1; y <= by + BUNKER_HEIGHT - 1; y++) {
            adjacentPositions.add(new TilePosition(bx + BUNKER_WIDTH, y));
        }

        for (int x = bx - TURRET_WIDTH + 1; x <= bx + BUNKER_WIDTH - 1; x++) {
            adjacentPositions.add(new TilePosition(x, by - TURRET_HEIGHT));
        }

        for (int x = bx - TURRET_WIDTH + 1; x <= bx + BUNKER_WIDTH - 1; x++) {
            adjacentPositions.add(new TilePosition(x, by + BUNKER_HEIGHT));
        }

        TilePosition baseTile = base.getLocation();
        adjacentPositions.sort(Comparator.comparingInt(pos -> pos.getApproxDistance(baseTile)));

        for (TilePosition candidate : adjacentPositions) {
            if (tilePositionValidator.isBuildable(candidate, UnitType.Terran_Missile_Turret) &&
                    !intersectsExclusionZones(candidate) &&
                    !intersectsExistingBuildTiles(candidate, UnitType.Terran_Missile_Turret, 0)) {
                return candidate;
            }
        }

        return null;
    }

    private void generateMainBaseCCTile() {
        HashSet<TilePosition> baseTiles = mapInfo.getBaseTiles();
        TilePosition naturalLocation = mapInfo.getNaturalBase().getLocation();

        TilePosition bestTile = null;
        int bestDistance = Integer.MAX_VALUE;

        for (TilePosition candidate : baseTiles) {
            boolean valid = true;

            for (int x = 0; x < CC_WIDTH && valid; x++) {
                for (int y = 0; y < CC_HEIGHT && valid; y++) {
                    TilePosition footprintTile = new TilePosition(candidate.getX() + x, candidate.getY() + y);
                    if (!baseTiles.contains(footprintTile) || intersectsExclusionZones(footprintTile) || !tilePositionValidator.isBuildable(footprintTile)) {
                        valid = false;
                    }
                }
            }

            if (!valid) {
                continue;
            }

            if (intersectsNeutralBuildings(candidate, CC_WIDTH, CC_HEIGHT)) {
                continue;
            }

            if (intersectsExistingBuildTiles(candidate, UnitType.Terran_Command_Center, 0)) {
                continue;
            }

            if (tooCloseToNaturalResources(candidate)) {
                continue;
            }

            boolean isNearCliffEdge = false;

            for (int x = 0; x < CC_WIDTH && !isNearCliffEdge; x++) {
                if (!tilePositionValidator.isWalkable(new TilePosition(candidate.getX() + x, candidate.getY() - 1))) {
                    isNearCliffEdge = true;
                }
                if (!tilePositionValidator.isWalkable(new TilePosition(candidate.getX() + x, candidate.getY() + CC_HEIGHT))) {
                    isNearCliffEdge = true;
                }
            }

            for (int y = 0; y < CC_HEIGHT && !isNearCliffEdge; y++) {
                if (!tilePositionValidator.isWalkable(new TilePosition(candidate.getX() - 1, candidate.getY() + y))) {
                    isNearCliffEdge = true;
                }
                if (!tilePositionValidator.isWalkable(new TilePosition(candidate.getX() + CC_WIDTH, candidate.getY() + y))) {
                    isNearCliffEdge = true;
                }
            }

            if (!isNearCliffEdge) {
                continue;
            }

            int distance = candidate.getApproxDistance(naturalLocation);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestTile = candidate;
            }
        }

        mainBaseCCTile = bestTile;
    }

    private void generateTurretTiles() {
        for (Base base : mapInfo.getMapBases()) {
            if (base == null || base.getMinerals().isEmpty()) {
                continue;
            }

            TilePosition ccTile = base.getLocation();
            int ccX = ccTile.getX();
            int ccY = ccTile.getY();

            double sumX = 0;
            double sumY = 0;
            for (Mineral mineral : base.getMinerals()) {
                TilePosition patch = mineral.getUnit().getTilePosition();
                sumX += patch.getX();
                sumY += patch.getY();
            }
            int mineralCount = base.getMinerals().size();
            int mineralCentroidX = (int)(sumX / mineralCount);
            int mineralCentroidY = (int)(sumY / mineralCount);

            double ccCenterX = ccX + CC_WIDTH / 2.0;
            double ccCenterY = ccY + CC_HEIGHT / 2.0;

            double toCcX = ccCenterX - mineralCentroidX;
            double toCcY = ccCenterY - mineralCentroidY;
            double toCcLen = Math.sqrt(toCcX * toCcX + toCcY * toCcY);

            int searchOriginX = mineralCentroidX;
            int searchOriginY = mineralCentroidY;
            if (toCcLen > 0) {
                searchOriginX = (int) Math.round(mineralCentroidX + toCcX / toCcLen * 2);
                searchOriginY = (int) Math.round(mineralCentroidY + toCcY / toCcLen * 2);
            }

            HashSet<TilePosition> baseTiles = mapInfo.getTilesForBase(base);

            int searchRadius = 5;
            TilePosition bestTurret = null;
            int bestDistSq = Integer.MAX_VALUE;

            for (int r = 0; r <= searchRadius; r++) {
                for (int ddx = -r; ddx <= r; ddx++) {
                    for (int ddy = -r; ddy <= r; ddy++) {
                        if (Math.abs(ddx) != r && Math.abs(ddy) != r) {
                            continue;
                        }

                        int tx = searchOriginX + ddx;
                        int ty = searchOriginY + ddy;

                        if (!isMineralLineTurretValid(tx, ty, baseTiles, base, ccX, ccY)) {
                            continue;
                        }

                        int distSq = ddx * ddx + ddy * ddy;
                        if (distSq < bestDistSq) {
                            bestDistSq = distSq;
                            bestTurret = new TilePosition(tx, ty);
                        }
                    }
                }
            }

            if (bestTurret != null) {
                mineralLineTurrets.put(base, bestTurret);
            }
        }

        List<TilePosition> front = getValidTurretTiles(frontBaseTiles);
        List<TilePosition> back = getValidTurretTiles(backBaseTiles);

        Collections.shuffle(front);
        Collections.shuffle(back);

        int addedFront = 0;
        for (TilePosition candidate : front) {
            if (addedFront >= 3) {
                break;
            }

            if (!intersectsExistingBuildTiles(candidate, UnitType.Terran_Missile_Turret, 0)) {
                mainTurrets.add(candidate);
                addedFront++;
            }
        }

        int addedBack = 0;
        for (TilePosition candidate : back) {
            if (addedBack >= 2) {
                break;
            }

            if (!intersectsExistingBuildTiles(candidate, UnitType.Terran_Missile_Turret, 0)) {
                mainTurrets.add(candidate);
                addedBack++;
            }
        }
    }

    private boolean isMineralLineTurretValid(int tx, int ty, HashSet<TilePosition> baseTiles, Base base, int ccX, int ccY) {
        TilePosition turretTile = new TilePosition(tx, ty);

        if (!tilePositionValidator.isBuildable(turretTile, UnitType.Terran_Missile_Turret)) {
            return false;
        }

        for (int dx = 0; dx < TURRET_WIDTH; dx++) {
            for (int dy = 0; dy < TURRET_HEIGHT; dy++) {
                TilePosition footprintTile = new TilePosition(tx + dx, ty + dy);
                if (!baseTiles.contains(footprintTile)) {
                    return false;
                }
                if (geyserExlusionTiles.contains(footprintTile) || chokeExclusionTiles.contains(footprintTile)) {
                    return false;
                }
            }
        }

        if (rectanglesIntersect(tx, ty, tx + TURRET_WIDTH, ty + TURRET_HEIGHT, ccX, ccY, ccX + CC_WIDTH + 3, ccY + CC_HEIGHT)) {
            return false;
        }

        for (Mineral mineral : base.getMinerals()) {
            TilePosition patch = mineral.getUnit().getTilePosition();
            int mineralWidth = mineral.getUnit().getType().tileWidth();
            int mineralHeight = mineral.getUnit().getType().tileHeight();
            if (rectanglesIntersect(tx, ty, tx + TURRET_WIDTH, ty + TURRET_HEIGHT, patch.getX(), patch.getY(), patch.getX() + mineralWidth, patch.getY() + mineralHeight)) {
                return false;
            }
        }

        if (intersectsExistingBuildTiles(turretTile, UnitType.Terran_Missile_Turret, 0)) {
            return false;
        }

        return true;
    }

    private List<TilePosition> getValidTurretTiles(Set<TilePosition> baseTiles) {
        List<TilePosition> valid = new ArrayList<>();

        for (TilePosition tile : baseTiles) {
            int tx = tile.getX();
            int ty = tile.getY();

            boolean insideBase = true;
            for (int dx = 0; dx < 2 && insideBase; dx++) {
                for (int dy = 0; dy < 2; dy++) {
                    if (!baseTiles.contains(new TilePosition(tx + dx, ty + dy))) {
                        insideBase = false;
                        break;
                    }
                }
            }
            if (!insideBase) {
                continue;
            }

            if (!tilePositionValidator.isBuildable(tile, UnitType.Terran_Missile_Turret)) {
                continue;
            }

            if (intersectsExclusionZones(tile)) {
                continue;
            }

            if (intersectsExistingBuildTiles(tile, UnitType.Terran_Missile_Turret, 0)) {
                continue;
            }

            valid.add(tile);
        }

        return valid;
    }

    private boolean footprintOverlaps(int newX, int newY, int endX, int endY,
                                      TilePosition existing, UnitType existingType, int xPad) {
        int existingX = existing.getX();
        int existingY = existing.getY();
        int existingEndX = existingX + existingType.tileWidth() + xPad;
        int existingEndY = existingY + existingType.tileHeight();

        return rectanglesIntersect(newX, newY, endX, endY, existingX, existingY, existingEndX, existingEndY);
    }

    private boolean intersectsExistingBuildTiles(TilePosition newTilePosition, UnitType unitType, int gap) {
        int newX = newTilePosition.getX();
        int newY = newTilePosition.getY();
        int endX = newX + unitType.tileWidth() + gap;
        int endY = newY + unitType.tileHeight();

        TilePosition ccPosition = mapInfo.getStartingBase().getLocation();
        int ccX = ccPosition.getX();
        int ccY = ccPosition.getY();

        boolean inCCBuffer = (newX >= ccX - 2 && newX < ccX + CC_WIDTH + 2) && (newY >= ccY - 2 && newY < ccY + CC_HEIGHT + 2);

        if (inCCBuffer) {
            return true;
        }

        if (mainBaseCCTile != null && footprintOverlaps(newX, newY, endX, endY, mainBaseCCTile, UnitType.Terran_Command_Center, 0)) {
            return true;
        }

        if (closeBunkerTile != null && footprintOverlaps(newX, newY, endX, endY, closeBunkerTile, UnitType.Terran_Bunker, 0)) {
            return true;
        }

        if (mainChokeBunker != null && footprintOverlaps(newX, newY, endX, endY, mainChokeBunker, UnitType.Terran_Bunker, 0)) {
            return true;
        }

        if (mainChokeTurret != null && footprintOverlaps(newX, newY, endX, endY, mainChokeTurret, UnitType.Terran_Missile_Turret, 0)) {
            return true;
        }

        for (TilePosition existingTile : largeBuildTiles) {
            if (footprintOverlaps(newX, newY, endX, endY, existingTile, UnitType.Terran_Engineering_Bay, 3)) {
                return true;
            }
        }

        for (TilePosition existingTile : largeBuildTilesNoGap) {
            if (footprintOverlaps(newX, newY, endX, endY, existingTile, UnitType.Terran_Engineering_Bay, 0)) {
                return true;
            }
        }

        for (TilePosition existingTile : mediumBuildTiles) {
            if (footprintOverlaps(newX, newY, endX, endY, existingTile, UnitType.Terran_Supply_Depot, 0)) {
                return true;
            }
        }

        for (TilePosition existingTile : mineralLineTurrets.values()) {
            if (existingTile == null) {
                continue;
            }

            if (footprintOverlaps(newX, newY, endX, endY, existingTile, UnitType.Terran_Missile_Turret, 0)) {
                return true;
            }
        }

        for (TilePosition existingTile : mainTurrets) {
            if (footprintOverlaps(newX, newY, endX, endY, existingTile, UnitType.Terran_Missile_Turret, 0)) {
                return true;
            }
        }

        //Check against existing buildings
        for (Unit building : game.self().getUnits()) {
            if (!building.getType().isBuilding()) {
                continue;
            }

            if (footprintOverlaps(newX, newY, endX, endY, building.getTilePosition(), building.getType(), 0)) {
                return true;
            }
        }

        return false;
    }

    private boolean tooCloseToNaturalResources(TilePosition candidate) {
        int minGap = 3;
        Base natural = mapInfo.getNaturalBase();

        int ccX1 = candidate.getX();
        int ccY1 = candidate.getY();
        int ccX2 = ccX1 + CC_WIDTH;
        int ccY2 = ccY1 + CC_HEIGHT;

        for (Mineral mineral : natural.getMinerals()) {
            TilePosition pos = mineral.getTopLeft();
            int resX1 = pos.getX();
            int resY1 = pos.getY();
            int resX2 = resX1 + 2;
            int resY2 = resY1 + 1;

            int gapX = Math.max(0, Math.max(resX1 - ccX2, ccX1 - resX2));
            int gapY = Math.max(0, Math.max(resY1 - ccY2, ccY1 - resY2));

            if (Math.max(gapX, gapY) < minGap) {
                return true;
            }
        }

        for (Geyser geyser : natural.getGeysers()) {
            TilePosition pos = geyser.getTopLeft();
            int resX1 = pos.getX();
            int resY1 = pos.getY();
            int resX2 = resX1 + 4;
            int resY2 = resY1 + 2;

            int gapX = Math.max(0, Math.max(resX1 - ccX2, ccX1 - resX2));
            int gapY = Math.max(0, Math.max(resY1 - ccY2, ccY1 - resY2));

            if (Math.max(gapX, gapY) < minGap) {
                return true;
            }
        }

        return false;
    }

    private boolean intersectsNeutralBuildings(TilePosition tile, int width, int height) {
        for (Unit neutralBuilding : game.getNeutralUnits()) {
            if (!neutralBuilding.getType().isBuilding()) {
                continue;
            }

            if (neutralBuilding.getType() == UnitType.Resource_Vespene_Geyser || neutralBuilding.getType() == UnitType.Resource_Mineral_Field) {
                continue;
            }

            TilePosition buildingTile = neutralBuilding.getTilePosition();
            int buildingWidth = neutralBuilding.getType().tileWidth();
            int buildingHeight = neutralBuilding.getType().tileHeight();

            if (rectanglesIntersect(tile.getX(), tile.getY(), tile.getX() + width, tile.getY() + height,
                    buildingTile.getX(), buildingTile.getY(),
                    buildingTile.getX() + buildingWidth,  buildingTile.getY() + buildingHeight)) {
                return true;
            }
        }
        return false;
    }

    private void mineralAboveGap(Base base) {
        for (Mineral mineral : base.getMinerals()) {
            TilePosition mineralTile = mineral.getTopLeft();
            int mineralWidth = mineral.getUnit().getType().tileWidth();
            for (int x = mineralTile.getX(); x < mineralTile.getX() + mineralWidth; x++) {
                mineralAboveGapTiles.add(new TilePosition(x, mineralTile.getY() - 1));
            }
        }
    }

    private void mineralExclusionZone(Base base) {
        if (base.getMinerals().isEmpty()) {
            return;
        }

        TilePosition lowestXTile = null;
        TilePosition highestXTile = null;
        TilePosition lowestYTile = null;
        TilePosition highestYTile = null;
        TilePosition commandCenterTile = base.getLocation();

        for (Mineral mineral : base.getMinerals()) {
            TilePosition mineralTile = mineral.getTopLeft();

            if (lowestXTile == null || mineralTile.getX() < lowestXTile.getX()) {
                lowestXTile = mineralTile;
            }
            if (highestXTile == null || mineralTile.getX() > highestXTile.getX()) {
                highestXTile = mineralTile;
            }
            if (lowestYTile == null || mineralTile.getY() < lowestYTile.getY()) {
                lowestYTile = mineralTile;
            }
            if (highestYTile == null || mineralTile.getY() > highestYTile.getY()) {
                highestYTile = mineralTile;
            }
        }

        int boxStartX = Math.min(lowestXTile.getX(), commandCenterTile.getX() + 2);
        int boxEndX = Math.max(highestXTile.getX() + 1, commandCenterTile.getX() - 2);
        int boxStartY = Math.min(lowestYTile.getY(), commandCenterTile.getY());
        int boxEndY = Math.max(highestYTile.getY(), commandCenterTile.getY());

        for (int x = boxStartX; x <= boxEndX; x++) {
            for (int y = boxStartY; y <= boxEndY; y++) {
                mineralExlusionTiles.add(new TilePosition(x, y));
            }
        }

    }

    private void geyserExclusionZone(Base base) {
        if (base.getGeysers().isEmpty()) {
            return;
        }

        TilePosition geyserTile = null;

        for (Geyser geyser : base.getGeysers()) {
            geyserTile = geyser.getTopLeft();

            int geyserX = geyserTile.getX();
            int geyserY = geyserTile.getY();
            TilePosition commandCenterTile = base.getLocation();

            int boxStartX = Math.min(geyserX, commandCenterTile.getX());
            int boxEndX = Math.max(geyserX + 3, commandCenterTile.getX());
            int boxStartY = Math.min(geyserY, commandCenterTile.getY());
            int boxEndY = Math.max(geyserY + 2, commandCenterTile.getY());

            for (int x = boxStartX; x <= boxEndX; x++) {
                for (int y = boxStartY; y <= boxEndY; y++) {
                    geyserExlusionTiles.add(new TilePosition(x, y));
                }
            }
        }
    }

    private void ccExclusionZone(Base base) {
        TilePosition commandCenterTile = base.getLocation();
        int ccX = commandCenterTile.getX();
        int ccY = commandCenterTile.getY();

        for (int x = ccX; x < ccX + CC_WIDTH + 3; x++) {
            for (int y = ccY; y < ccY + CC_HEIGHT; y++) {
                ccExclusionTiles.add(new TilePosition(x, y));
            }
        }
    }

    private void chokeExclusionZone(Base base) {
        int exclusionRadius = 3;

        for (ChokePoint choke : base.getArea().getChokes()) {
            TilePosition chokeTile = choke.getCenter().toTilePosition();

            for (int x = chokeTile.getX() - exclusionRadius; x <= chokeTile.getX() + exclusionRadius; x++) {
                for (int y = chokeTile.getY() - exclusionRadius; y <= chokeTile.getY() + exclusionRadius; y++) {
                    TilePosition tile = new TilePosition(x, y);
                    if (tilePositionValidator.isWithinMap(tile)) {
                        chokeExclusionTiles.add(tile);
                    }
                }
            }
        }
    }

    private boolean intersectsExclusionZones(TilePosition tilePosition) {
        return geyserExlusionTiles.contains(tilePosition) || mineralExlusionTiles.contains(tilePosition)
                || ccExclusionTiles.contains(tilePosition) || chokeExclusionTiles.contains(tilePosition)
                || mineralAboveGapTiles.contains(tilePosition);
    }

    public boolean isAddonPositionBlocked(TilePosition buildingTile) {
        if (largeBuildTilesNoGap.contains(buildingTile)) {
            return true;
        }

        int buildingX = buildingTile.getX();
        int buildingY = buildingTile.getY();
        int addonX1 = buildingX + 4;
        int addonY1 = buildingY;
        int addonX2 = buildingX + 6;
        int addonY2 = buildingY + 2;

        for (TilePosition noGapTile : largeBuildTilesNoGap) {
            int tileX = noGapTile.getX();
            int tileY = noGapTile.getY();

            if (rectanglesIntersect(addonX1, addonY1, addonX2, addonY2, tileX, tileY, tileX + BARRACKS_WIDTH, tileY + BARRACKS_HEIGHT)) {
                return true;
            }
        }

        for (TilePosition mediumTile : mediumBuildTiles) {
            int tileX = mediumTile.getX();
            int tileY = mediumTile.getY();

            if (rectanglesIntersect(addonX1, addonY1, addonX2, addonY2, tileX, tileY, tileX + DEPOT_WIDTH, tileY + DEPOT_HEIGHT)) {
                return true;
            }
        }

        return false;
    }

    private void generateFrontBaseTiles() {
        HashSet<TilePosition> baseTiles = new HashSet<>(mapInfo.getBaseTiles());
        TilePosition chokePos = mapInfo.getMainChoke().getCenter().toTilePosition();
        TilePosition ccPos = mapInfo.getStartingBase().getLocation();

        int xDiff = Math.abs(chokePos.getX() - ccPos.getX());
        int yDiff = Math.abs(chokePos.getY() - ccPos.getY());

        for (TilePosition tilePosition : baseTiles) {
            if (geyserExlusionTiles.contains(tilePosition) || mineralExlusionTiles.contains(tilePosition)) {
                continue;
            }

            if (xDiff > yDiff) {
                if (chokePos.getX() < ccPos.getX() && tilePosition.getX() < ccPos.getX()) {
                    frontBaseTiles.add(tilePosition);
                } else if (chokePos.getX() > ccPos.getX() && tilePosition.getX() > ccPos.getX()) {
                    frontBaseTiles.add(tilePosition);
                }
            }
            else {
                if (chokePos.getY() < ccPos.getY() && tilePosition.getY() < ccPos.getY()) {
                    frontBaseTiles.add(tilePosition);
                }
                else if (chokePos.getY() > ccPos.getY() && tilePosition.getY() > ccPos.getY()) {
                    frontBaseTiles.add(tilePosition);
                }
            }
        }
    }

    private void generateBackBaseTiles() {
        HashSet<TilePosition> baseTiles = new HashSet<>(mapInfo.getBaseTiles());

        for (TilePosition tilePosition : baseTiles) {
            if (geyserExlusionTiles.contains(tilePosition) || mineralExlusionTiles.contains(tilePosition) || frontBaseTiles.contains(tilePosition)) {
                continue;
            }
            backBaseTiles.add(tilePosition);
        }
    }

    private boolean rectanglesIntersect(int ax1, int ay1, int ax2, int ay2, int bx1, int by1, int bx2, int by2) {
        return !(ax2 <= bx1 || ax1 >= bx2 || ay2 <= by1 || ay1 >= by2);
    }

    public HashSet<TilePosition> getMediumBuildTiles() {
        return mediumBuildTiles;
    }

    public HashSet<TilePosition> getLargeBuildTiles() {
        return largeBuildTiles;
    }

    public HashSet<TilePosition> getLargeBuildTilesNoGap() {
        return largeBuildTilesNoGap;
    }

    public TilePosition getCloseBunkerTile() {
        return closeBunkerTile;
    }

    public TilePosition getProxyBunkerTile() {
        return proxyBunkerTile;
    }

    public void setProxyBunkerTile(TilePosition tile) {
        proxyBunkerTile = tile;
    }

    public TilePosition getNaturalChokeBunker() {
        return naturalChokeBunker;
    }

    public TilePosition getNaturalBunkerEbayPosition() {
        return naturalBunkerEbayPosition;
    }

    public TilePosition getNaturalBunkerBarracksPosition() {
        return naturalBunkerBarracksPosition;
    }

    public TilePosition getNaturalBunkerDepotPosition() {
        return naturalBunkerDepotPosition;
    }

    public TilePosition getMainChokeBunker() {
        return mainChokeBunker;
    }

    public TilePosition getMainChokeTurret() {
        return mainChokeTurret;
    }

    public TilePosition getNaturalChokeTurret() {
        return naturalChokeTurret;
    }

    public HashSet<TilePosition> getBackBaseTiles() {
        return backBaseTiles;
    }

    public HashSet<TilePosition> getFrontBaseTiles() {
        return frontBaseTiles;
    }

    public HashSet<TilePosition> getCcExclusionTiles() {
        return ccExclusionTiles;
    }

    public HashSet<TilePosition> getGeyserExlusionTiles() {
        return geyserExlusionTiles;
    }

    public HashSet<TilePosition> getMineralExlusionTiles() {
        return mineralExlusionTiles;
    }

    public HashMap<Base, TilePosition> getMineralLineTurrets() {
        return mineralLineTurrets;
    }

    public HashSet<TilePosition> getMainTurrets() {
        return mainTurrets;
    }

    public TilePosition getMainBaseCCTile() {
        return mainBaseCCTile;
    }

    public void onFrame() {
            regenerateBuildTiles();
    }

    public void onUnitComplete(Unit unit) {
        if (!unit.getType().isBuilding()) {
            return;
        }

        if (unit.getType() == UnitType.Terran_Command_Center) {
            for (Base base : mapInfo.getOwnedBases()) {
                if (unit.getPosition().getApproxDistance(base.getLocation().toPosition()) < 100) {
                    if (base != mapInfo.getStartingBase() && base != mapInfo.getNaturalBase()) {
                        pendingBaseTiles.add(base);
                    }
                    break;
                }
            }
        }

        largeBuildTiles.remove(unit.getTilePosition());
        largeBuildTilesNoGap.remove(unit.getTilePosition());
        mediumBuildTiles.remove(unit.getTilePosition());
    }

    private static class WallAdjacency {
        boolean wallAdjacent;
        boolean nearWall;
    }
}
