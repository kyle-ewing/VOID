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
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwem.Base;
import bwem.ChokePoint;
import bwem.Geyser;
import bwem.Mineral;
import information.GameState;
import information.MapInfo;
import util.PositionInterpolator;

public class BuildTiles {
    private Game game;
    private GameState gameState;
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
    private TilePosition closeBunkerTile;
    private TilePosition mainChokeTurret;
    private TilePosition naturalChokeTurret;
    private TilePosition mainBaseCCTile;
    private Base startingBase;
    private boolean naturalTilesGenerated = false;
    private boolean minOnlyTilesGenerated = false;

    public BuildTiles(Game game, MapInfo mapInfo, GameState gameState) {
        this.game = game;
        this.mapInfo = mapInfo;
        this.gameState = gameState;

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
        //generateChokeTurretTiles();
        generateMainBaseCCTile();
        generateLargeTiles(frontBaseTiles);
        generateMediumTiles(backBaseTiles);
        generateTurretTiles();
    }

    private void regenerateBuildTiles() {
        if (!naturalTilesGenerated && mediumBuildTiles.isEmpty() || mediumBuildTiles.size() == 1) {
            chokeExclusionZone(mapInfo.getNaturalBase());
            //generateMediumTiles(frontBaseTiles);
            generateMediumTiles(mapInfo.getNaturalTiles());
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
                addBarracksStackNoGap(tile.getX(), tile.getY(), UnitType.Terran_Barracks);
            }
        }

        //Second pass largeBuildTilesNoGap with no gap no stack
        if (largeBuildTilesNoGap.size() < 4) {
            int barWidth = UnitType.Terran_Barracks.tileWidth();
            int barHeight = UnitType.Terran_Barracks.tileHeight();

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

                boolean overlapsWithGapTiles = false;
                for (TilePosition gapTile : largeBuildTilesNoGap) {
                    int gapFootprintEndX = gapTile.getX() + barWidth;
                    int gapFootprintEndY = gapTile.getY() + barHeight;

                    if (!(tile.getX() + barWidth <= gapTile.getX() ||
                            tile.getX() >= gapFootprintEndX ||
                            tile.getY() + barHeight <= gapTile.getY() ||
                            tile.getY() >= gapFootprintEndY)) {
                        overlapsWithGapTiles = true;
                        break;
                    }
                }

                if (overlapsWithGapTiles) {
                    continue;
                }

                boolean overlapsWithNoGapTiles = false;
                for (TilePosition noGapTile : largeBuildTiles) {
                    int noGapFootprintEndX = noGapTile.getX() + barWidth + 3;
                    int noGapFootprintEndY = noGapTile.getY() + barHeight;

                    if (!(tile.getX() + barWidth <= noGapTile.getX() ||
                            tile.getX() >= noGapFootprintEndX ||
                            tile.getY() + barHeight <= noGapTile.getY() ||
                            tile.getY() >= noGapFootprintEndY)) {
                        overlapsWithNoGapTiles = true;
                        break;
                    }
                }

                if (!overlapsWithNoGapTiles) {
                    if (!hasAdjacentBuildableColumn(tile, barWidth, barHeight)) {
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

                int adjacentY = stackY + UnitType.Terran_Barracks.tileHeight();
                TilePosition adjacentPos = new TilePosition(stackX, adjacentY);

                if (baseTiles.contains(adjacentPos) && isValidBarracksStack(adjacentPos, 3)) {
                    addBarracksStack(stackX, stackY, UnitType.Terran_Barracks);
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

                int adjacentX = stackX + UnitType.Terran_Barracks.tileWidth() + 2;
                TilePosition adjacentPos = new TilePosition(adjacentX, stackY);

                if (frontBaseTiles.contains(adjacentPos) && isValidBarracksStack(adjacentPos, 2)) {
                    addBarracksStack(stackX, stackY, UnitType.Terran_Barracks);
                }
            }
        }

        //Third pass largeBuildTiles with 3 tile gap no stack
        if (largeBuildTiles.size() < 6) {
            int barWidth = UnitType.Terran_Barracks.tileWidth();
            int barHeight = UnitType.Terran_Barracks.tileHeight();

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

                boolean overlapsWithGapTiles = false;
                for (TilePosition gapTile : largeBuildTiles) {
                    int gapFootprintEndX = gapTile.getX() + barWidth + 3;
                    int gapFootprintEndY = gapTile.getY() + barHeight;

                    if (!(tile.getX() + barWidth <= gapTile.getX() ||
                            tile.getX() >= gapFootprintEndX ||
                            tile.getY() + barHeight <= gapTile.getY() ||
                            tile.getY() >= gapFootprintEndY)) {
                        overlapsWithGapTiles = true;
                        break;
                    }
                }

                if (overlapsWithGapTiles) {
                    continue;
                }

                boolean overlapsWithNoGapTiles = false;
                for (TilePosition noGapTile : largeBuildTilesNoGap) {
                    int noGapFootprintEndX = noGapTile.getX() + barWidth + 3;
                    int noGapFootprintEndY = noGapTile.getY() + barHeight;

                    if (!(tile.getX() + barWidth <= noGapTile.getX() ||
                            tile.getX() >= noGapFootprintEndX ||
                            tile.getY() + barHeight <= noGapTile.getY() ||
                            tile.getY() >= noGapFootprintEndY)) {
                        overlapsWithNoGapTiles = true;
                        break;
                    }
                }

                if (!overlapsWithNoGapTiles) {
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
                addBarracksStackNoGap(tile.getX(), tile.getY(), UnitType.Terran_Barracks);
            }
        }

        //4th pass largeBuildTilesNoGap with no gap no stack
        if (largeBuildTilesNoGap.size() < 6) {
            int barWidth = UnitType.Terran_Barracks.tileWidth();
            int barHeight = UnitType.Terran_Barracks.tileHeight();

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

                boolean overlapsWithGapTiles = false;
                for (TilePosition gapTile : largeBuildTilesNoGap) {
                    int gapFootprintEndX = gapTile.getX() + barWidth;
                    int gapFootprintEndY = gapTile.getY() + barHeight;

                    if (!(tile.getX() + barWidth <= gapTile.getX() ||
                            tile.getX() >= gapFootprintEndX ||
                            tile.getY() + barHeight <= gapTile.getY() ||
                            tile.getY() >= gapFootprintEndY)) {
                        overlapsWithGapTiles = true;
                        break;
                    }
                }

                if (overlapsWithGapTiles) {
                    continue;
                }

                boolean overlapsWithNoGapTiles = false;
                for (TilePosition noGapTile : largeBuildTiles) {
                    int noGapFootprintEndX = noGapTile.getX() + barWidth + 3;
                    int noGapFootprintEndY = noGapTile.getY() + barHeight;

                    if (!(tile.getX() + barWidth <= noGapTile.getX() ||
                            tile.getX() >= noGapFootprintEndX ||
                            tile.getY() + barHeight <= noGapTile.getY() ||
                            tile.getY() >= noGapFootprintEndY)) {
                        overlapsWithNoGapTiles = true;
                        break;
                    }
                }

                if (!overlapsWithNoGapTiles) {
                    if (!hasAdjacentBuildableColumn(tile, barWidth, barHeight)) {
                        continue;
                    }
                    largeBuildTilesNoGap.add(tile);
                }
            }
        }
    }

    private boolean hasAdjacentBuildableColumn(TilePosition tile, int barWidth, int barHeight) {
        int x = tile.getX();
        int y = tile.getY();

        for (int dy = 0; dy < barHeight; dy++) {
            TilePosition leftTile = new TilePosition(x - 1, y + dy);
            if (tilePositionValidator.isWithinMap(leftTile) && tilePositionValidator.isBuildable(leftTile)) {
                return true;
            }
        }

        for (int dy = 0; dy < barHeight; dy++) {
            TilePosition rightTile = new TilePosition(x + barWidth, y + dy);
            if (tilePositionValidator.isWithinMap(rightTile) && tilePositionValidator.isBuildable(rightTile)) {
                return true;
            }
        }

        return false;
    }

    private boolean isValidBarracksStack(TilePosition topTile, int gap) {
        int barHeight = UnitType.Terran_Barracks.tileHeight();
        TilePosition bottomTile = new TilePosition(topTile.getX(), topTile.getY() + barHeight);

        return isValidBarracksPosition(topTile, gap) && isValidBarracksPosition(bottomTile, gap);
    }

    private boolean isValidBarracksPosition(TilePosition tile, int gap) {
        int barWidth = UnitType.Terran_Barracks.tileWidth();
        int barHeight = UnitType.Terran_Barracks.tileHeight();

        if (intersectsExistingBuildTiles(tile, UnitType.Terran_Barracks, gap)) {
            return false;
        }

        for (int x = 0; x < barWidth; x++) {
            for (int y = 0; y < barHeight; y++) {
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
            for (int y = 0; y < barHeight; y++) {
                TilePosition bufferTile = new TilePosition(tile.getX() + barWidth + bufferX, tile.getY() + y);

                if (!tilePositionValidator.isWithinMap(bufferTile) || intersectsExclusionZones(bufferTile) || !tilePositionValidator.isWalkable(bufferTile)) {
                    return false;
                }
            }
        }

        //Check tile gap below stack
        for (int x = 0; x < barWidth; x++) {
            TilePosition gapTile = new TilePosition(tile.getX() + x, tile.getY() - 1);

            if (intersectsExclusionZones(gapTile) || !tilePositionValidator.isWalkable(gapTile) || intersectsExistingBuildTiles(gapTile, UnitType.Terran_Barracks, 0)) {
                return false;
            }
        }

        return !intersectsNeutralBuildings(tile, barWidth, barHeight);
    }

    private void addBarracksStack(int x, int y, UnitType barracks) {
        int barHeight = barracks.tileHeight();
        largeBuildTiles.add(new TilePosition(x, y));
        largeBuildTiles.add(new TilePosition(x, y + barHeight));
    }

    private void addBarracksStackNoGap(int x, int y, UnitType barracksType) {
        int barHeight = barracksType.tileHeight();
        largeBuildTilesNoGap.add(new TilePosition(x, y));
        largeBuildTilesNoGap.add(new TilePosition(x, y + barHeight));
    }

    private void generateMediumTiles(HashSet<TilePosition> baseTiles) {
        UnitType depotType = UnitType.Terran_Supply_Depot;
        int depotWidth = depotType.tileWidth();
        int depotHeight = depotType.tileHeight();

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

            TilePosition topRight = new TilePosition(tile.getX() + depotWidth, tile.getY());
            TilePosition bottomLeft = new TilePosition(tile.getX(), tile.getY() + depotHeight);
            TilePosition bottomRight = new TilePosition(tile.getX() + depotWidth, tile.getY() + depotHeight);

            if (!baseTiles.contains(topRight) || !baseTiles.contains(bottomLeft) || !baseTiles.contains(bottomRight)) {
                continue;
            }

            if (usedTiles.contains(topRight) || usedTiles.contains(bottomLeft) || usedTiles.contains(bottomRight)) {
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

                for (int y = 0; y < 2 * depotHeight; y++) {
                    usedTiles.add(new TilePosition(tile.getX() + 2 * depotWidth, tile.getY() + y));
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

            TilePosition rightTile = new TilePosition(tile.getX() + depotWidth, tile.getY());

            if (!baseTiles.contains(tile) || !baseTiles.contains(rightTile)) {
                continue;
            }

            if (usedTiles.contains(rightTile)) {
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

                for (int y = 0; y < depotHeight; y++) {
                    usedTiles.add(new TilePosition(tile.getX() + 2 * depotWidth, tile.getY() + y));
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

            TilePosition bottomTile = new TilePosition(tile.getX(), tile.getY() + depotHeight);

            if (!baseTiles.contains(tile) || !baseTiles.contains(bottomTile)) {
                continue;
            }

            if (usedTiles.contains(bottomTile)) {
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

                for (int y = 0; y < 2 * depotHeight; y++) {
                    usedTiles.add(new TilePosition(tile.getX() + depotWidth, tile.getY() + y));
                }
            }
        }
    }

    private boolean validMediumTileHorizontalPair(TilePosition topLeft, UnitType depotType) {
        int depotWidth = depotType.tileWidth();
        int depotHeight = depotType.tileHeight();

        TilePosition rightTile = new TilePosition(topLeft.getX() + depotWidth, topLeft.getY());

        if (!validMediumTilePositionNoGap(topLeft, depotType)) {
            return false;
        }

        if (!validMediumTilePositionNoGap(rightTile, depotType)) {
            return false;
        }

        for (int y = 0; y < depotHeight; y++) {
            TilePosition gapTile = new TilePosition(topLeft.getX() + 2 * depotWidth, topLeft.getY() + y);

            if (intersectsExclusionZones(gapTile) || !tilePositionValidator.isWalkable(gapTile) || intersectsExistingBuildTiles(gapTile, depotType, 0)) {
                return false;
            }
        }

        return true;
    }

    private boolean validMediumTileVerticalStack(TilePosition topTile, UnitType depotType) {
        int depotWidth = depotType.tileWidth();
        int depotHeight = depotType.tileHeight();

        TilePosition bottomTile = new TilePosition(topTile.getX(), topTile.getY() + depotHeight);

        if (!validMediumTilePositionNoGap(topTile, depotType)) {
            return false;
        }

        if (!validMediumTilePositionNoGap(bottomTile, depotType)) {
            return false;
        }

        for (int y = 0; y < 2 * depotHeight; y++) {
            TilePosition gapTile = new TilePosition(topTile.getX() + depotWidth, topTile.getY() + y);

            if (intersectsExclusionZones(gapTile) || !tilePositionValidator.isWalkable(gapTile) || intersectsExistingBuildTiles(gapTile, depotType, 0)) {
                return false;
            }
        }

        return true;
    }

    private boolean validMediumTileSquare(TilePosition topLeft, UnitType depotType) {
        int depotWidth = depotType.tileWidth();
        int depotHeight = depotType.tileHeight();

        TilePosition topRight = new TilePosition(topLeft.getX() + depotWidth, topLeft.getY());
        TilePosition bottomLeft = new TilePosition(topLeft.getX(), topLeft.getY() + depotHeight);
        TilePosition bottomRight = new TilePosition(topLeft.getX() + depotWidth, topLeft.getY() + depotHeight);

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

        for (int y = 0; y < 2 * depotHeight; y++) {
            TilePosition gapTile = new TilePosition(topLeft.getX() + 2 * depotWidth, topLeft.getY() + y);

            if (intersectsExclusionZones(gapTile) || !tilePositionValidator.isWalkable(gapTile) || intersectsExistingBuildTiles(gapTile, depotType, 0)) {
                return false;
            }
        }

        return true;
    }

    private boolean validMediumTilePositionNoGap(TilePosition tile, UnitType depot) {
        int depotWidth = depot.tileWidth();
        int depotHeight = depot.tileHeight();

        for (int x = 0; x < depotWidth; x++) {
            for (int y = 0; y < depotHeight; y++) {
                TilePosition checkTile = new TilePosition(tile.getX() + x, tile.getY() + y);

                if (intersectsExclusionZones(checkTile) || !tilePositionValidator.isBuildable(checkTile)
                        || intersectsExistingBuildTiles(checkTile, depot, 0)) {
                    return false;
                }
            }
        }

        return !intersectsNeutralBuildings(tile, depotWidth, depotHeight);
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
        int bunkerWidth = UnitType.Terran_Bunker.tileWidth();
        int bunkerHeight = UnitType.Terran_Bunker.tileHeight();

        int midX = finalMidPoint.getX();
        int midY = finalMidPoint.getY();

        for (int x = midX - searchRadius; x <= midX + searchRadius; x++) {
            for (int y = midY - searchRadius; y <= midY + searchRadius; y++) {
                TilePosition testPos = new TilePosition(x, y);
                boolean validLocation = true;

                for (int bx = x; bx < x + bunkerWidth; bx++) {
                    for (int by = y; by < y + bunkerHeight; by++) {
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

    private void generateChokeBunkerTiles() {
        ChokePoint mainChoke = mapInfo.getMainChoke();

        if (mainChoke != null) {
            TilePosition chokeTile = mainChoke.getCenter().toTilePosition();
            TilePosition baseTile = mapInfo.getStartingBase().getLocation();
            double percent = 0.90;

            //temp solution fix later
            if (mapInfo.getMinOnlyBase() != null) {
                ChokePoint otherChoke = null;

                for (ChokePoint cp : mapInfo.getMinOnlyBase().getArea().getChokePoints()) {
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
            TilePosition chokeTile = naturalChoke.getCenter().toTilePosition();
            TilePosition baseTile = mapInfo.getNaturalBase().getLocation();
            TilePosition closerNatural = PositionInterpolator.interpolate(chokeTile, baseTile, 0.10);
            TilePosition naturalBunker = findValidTileNear(closerNatural, UnitType.Terran_Bunker);



            if (naturalBunker != null) {
                naturalChokeBunker = naturalBunker;
                naturalBunkerEbayPosition = computeEbayInFrontOfBunker(naturalBunker, chokeTile);
            }
        }
    }

    private TilePosition computeEbayInFrontOfBunker(TilePosition bunkerTile, TilePosition chokeTile) {
        int dx = chokeTile.getX() - bunkerTile.getX();
        int dy = chokeTile.getY() - bunkerTile.getY();

        int ebayW = UnitType.Terran_Engineering_Bay.tileWidth();
        int ebayH = UnitType.Terran_Engineering_Bay.tileHeight();
        int bunkerW = UnitType.Terran_Bunker.tileWidth();
        int bunkerH = UnitType.Terran_Bunker.tileHeight();

        int searchRange = 6;

        if (Math.abs(dx) >= Math.abs(dy)) {
            int fixedX;
            if (dx > 0) {
                fixedX = bunkerTile.getX() + bunkerW;
            }
            else {
                fixedX = bunkerTile.getX() - ebayW;
            }

            for (int offset = 0; offset <= searchRange; offset++) {
                for (int sign = -1; sign <= 1; sign += 2) {
                    int testY = bunkerTile.getY() + offset * sign;
                    TilePosition candidate = new TilePosition(fixedX, testY);
                    if (tilePositionValidator.isBuildable(candidate, UnitType.Terran_Engineering_Bay) && !intersectsExclusionZones(candidate)) {
                        return candidate;
                    }
                }
            }
        }
        else {
            int fixedY;
            if (dy > 0) {
                fixedY = bunkerTile.getY() + bunkerH;
            }
            else {
                fixedY = bunkerTile.getY() - ebayH;
            }

            for (int offset = 0; offset <= searchRange; offset++) {
                for (int sign = -1; sign <= 1; sign += 2) {
                    int testX = bunkerTile.getX() + offset * sign;
                    TilePosition candidate = new TilePosition(testX, fixedY);
                    if (tilePositionValidator.isBuildable(candidate, UnitType.Terran_Engineering_Bay) && !intersectsExclusionZones(candidate)) {
                        return candidate;
                    }
                }
            }
        }

        return null;
    }

    private TilePosition findValidTileNear(TilePosition center, UnitType unitType) {
        int searchRadius = 6;

        for (int r = 0; r <= searchRadius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    if (Math.abs(dx) != r && Math.abs(dy) != r) continue;
                    TilePosition test = new TilePosition(center.getX() + dx, center.getY() + dy);
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
        int bunkerWidth = UnitType.Terran_Bunker.tileWidth();
        int bunkerHeight = UnitType.Terran_Bunker.tileHeight();
        int turretWidth = UnitType.Terran_Missile_Turret.tileWidth();
        int turretHeight = UnitType.Terran_Missile_Turret.tileHeight();

        int bx = bunkerTile.getX();
        int by = bunkerTile.getY();

        List<TilePosition> adjacentPositions = new ArrayList<>();

        for (int y = by - turretHeight + 1; y <= by + bunkerHeight - 1; y++) {
            adjacentPositions.add(new TilePosition(bx - turretWidth, y));
        }

        for (int y = by - turretHeight + 1; y <= by + bunkerHeight - 1; y++) {
            adjacentPositions.add(new TilePosition(bx + bunkerWidth, y));
        }

        for (int x = bx - turretWidth + 1; x <= bx + bunkerWidth - 1; x++) {
            adjacentPositions.add(new TilePosition(x, by - turretHeight));
        }

        for (int x = bx - turretWidth + 1; x <= bx + bunkerWidth - 1; x++) {
            adjacentPositions.add(new TilePosition(x, by + bunkerHeight));
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
        int ccWidth = UnitType.Terran_Command_Center.tileWidth();
        int ccHeight = UnitType.Terran_Command_Center.tileHeight();
        HashSet<TilePosition> baseTiles = mapInfo.getBaseTiles();
        TilePosition naturalLocation = mapInfo.getNaturalBase().getLocation();

        TilePosition bestTile = null;
        int bestDistance = Integer.MAX_VALUE;

        for (TilePosition candidate : baseTiles) {
            boolean valid = true;

            for (int x = 0; x < ccWidth && valid; x++) {
                for (int y = 0; y < ccHeight && valid; y++) {
                    TilePosition footprintTile = new TilePosition(candidate.getX() + x, candidate.getY() + y);
                    if (!baseTiles.contains(footprintTile) || intersectsExclusionZones(footprintTile) || !tilePositionValidator.isBuildable(footprintTile)) {
                        valid = false;
                    }
                }
            }

            if (!valid) {
                continue;
            }

            if (intersectsNeutralBuildings(candidate, ccWidth, ccHeight)) {
                continue;
            }

            if (intersectsExistingBuildTiles(candidate, UnitType.Terran_Command_Center, 0)) {
                continue;
            }

            if (tooCloseToNaturalResources(candidate, ccWidth, ccHeight)) {
                continue;
            }

            boolean isNearCliffEdge = false;

            for (int x = 0; x < ccWidth && !isNearCliffEdge; x++) {
                if (!tilePositionValidator.isWalkable(new TilePosition(candidate.getX() + x, candidate.getY() - 1))) {
                    isNearCliffEdge = true;
                }
                if (!tilePositionValidator.isWalkable(new TilePosition(candidate.getX() + x, candidate.getY() + ccHeight))) {
                    isNearCliffEdge = true;
                }
            }

            for (int y = 0; y < ccHeight && !isNearCliffEdge; y++) {
                if (!tilePositionValidator.isWalkable(new TilePosition(candidate.getX() - 1, candidate.getY() + y))) {
                    isNearCliffEdge = true;
                }
                if (!tilePositionValidator.isWalkable(new TilePosition(candidate.getX() + ccWidth, candidate.getY() + y))) {
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
            int ccWidth = UnitType.Terran_Command_Center.tileWidth();
            int ccHeight = UnitType.Terran_Command_Center.tileHeight();
            int turretWidth = UnitType.Terran_Missile_Turret.tileWidth();
            int turretHeight = UnitType.Terran_Missile_Turret.tileHeight();

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

            double ccCenterX = ccX + ccWidth / 2.0;
            double ccCenterY = ccY + ccHeight / 2.0;

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

                        if (!isMineralLineTurretValid(tx, ty, baseTiles, base, ccX, ccY, ccWidth, ccHeight, turretWidth, turretHeight)) {
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

    private boolean isMineralLineTurretValid(int tx, int ty, HashSet<TilePosition> baseTiles, Base base, int ccX, int ccY, int ccWidth, int ccHeight, int turretWidth, int turretHeight) {
        TilePosition turretTile = new TilePosition(tx, ty);

        if (!tilePositionValidator.isBuildable(turretTile, UnitType.Terran_Missile_Turret)) {
            return false;
        }

        for (int dx = 0; dx < turretWidth; dx++) {
            for (int dy = 0; dy < turretHeight; dy++) {
                TilePosition footprintTile = new TilePosition(tx + dx, ty + dy);
                if (!baseTiles.contains(footprintTile)) {
                    return false;
                }
                if (geyserExlusionTiles.contains(footprintTile) || chokeExclusionTiles.contains(footprintTile)) {
                    return false;
                }
            }
        }

        if (rectanglesIntersect(tx, ty, tx + turretWidth, ty + turretHeight, ccX, ccY, ccX + ccWidth + 3, ccY + ccHeight)) {
            return false;
        }

        for (Mineral mineral : base.getMinerals()) {
            TilePosition patch = mineral.getUnit().getTilePosition();
            int mineralWidth = mineral.getUnit().getType().tileWidth();
            int mineralHeight = mineral.getUnit().getType().tileHeight();
            if (rectanglesIntersect(tx, ty, tx + turretWidth, ty + turretHeight, patch.getX(), patch.getY(), patch.getX() + mineralWidth, patch.getY() + mineralHeight)) {
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


    private boolean intersectsExistingBuildTiles(TilePosition newTilePosition, UnitType unitType, int gap) {
        int newX = newTilePosition.getX();
        int newY = newTilePosition.getY();
        int endX = newX + unitType.tileWidth() + gap;
        int endY = newY + unitType.tileHeight();

        TilePosition ccPosition = mapInfo.getStartingBase().getLocation();
        int ccX = ccPosition.getX();
        int ccY = ccPosition.getY();
        int ccWidth = UnitType.Terran_Command_Center.tileWidth();
        int ccHeight = UnitType.Terran_Command_Center.tileHeight();

        boolean inCCBuffer = (newX >= ccX - 2 && newX < ccX + ccWidth + 2) && (newY >= ccY - 2 && newY < ccY + ccHeight + 2);

        if (inCCBuffer) {
            return true;
        }

        if (mainBaseCCTile != null) {
            int ccTileX = mainBaseCCTile.getX();
            int ccTileY = mainBaseCCTile.getY();
            int ccXEnd = ccTileX + UnitType.Terran_Command_Center.tileWidth();
            int ccYEnd = ccTileY + UnitType.Terran_Command_Center.tileHeight();

            if (rectanglesIntersect(newX, newY, endX, endY, ccTileX, ccTileY, ccXEnd, ccYEnd)) {
                return true;
            }
        }

        if (closeBunkerTile != null) {
            int bunkerXStart = closeBunkerTile.getX();
            int bunkerYStart = closeBunkerTile.getY();
            int bunkerXEnd = closeBunkerTile.getX() + UnitType.Terran_Bunker.tileWidth();
            int bunkerYEnd = closeBunkerTile.getY() + UnitType.Terran_Bunker.tileHeight();

            if (rectanglesIntersect(newX, newY, endX, endY, bunkerXStart, bunkerYStart, bunkerXEnd, bunkerYEnd)) {
                return true;
            }
        }

        if (mainChokeBunker != null) {
            int mainChokeBunkerX = mainChokeBunker.getX();
            int mainChokeBunkerY = mainChokeBunker.getY();
            int mainChokeXEnd = mainChokeBunkerX + UnitType.Terran_Bunker.tileWidth();
            int mainChokeYEnd = mainChokeBunkerY + UnitType.Terran_Bunker.tileHeight();

            if (rectanglesIntersect(newX, newY, endX, endY, mainChokeBunkerX, mainChokeBunkerY, mainChokeXEnd, mainChokeYEnd)) {
                return true;
            }
        }

        if (mainChokeTurret != null) {
            int mainChokeTurretX = mainChokeTurret.getX();
            int mainChokeTurretY = mainChokeTurret.getY();
            int mainChokeTurretXEnd = mainChokeTurretX + UnitType.Terran_Missile_Turret.tileWidth();
            int mainChokeTurretYEnd = mainChokeTurretY + UnitType.Terran_Missile_Turret.tileHeight();

            if (rectanglesIntersect(newX, newY, endX, endY, mainChokeTurretX, mainChokeTurretY, mainChokeTurretXEnd, mainChokeTurretYEnd)) {
                return true;
            }
        }

        for (TilePosition existingTile : largeBuildTiles) {
            int existingXStart = existingTile.getX();
            int existingYStart = existingTile.getY();
            int existingXEnd = existingXStart + UnitType.Terran_Engineering_Bay.tileWidth();
            int existingYEnd = existingYStart + UnitType.Terran_Engineering_Bay.tileHeight();

            if (rectanglesIntersect(newX, newY, endX, endY, existingXStart, existingYStart, existingXEnd + 3, existingYEnd)) {
                return true;
            }
        }

        for (TilePosition existingTile : largeBuildTilesNoGap) {
            int existingXStart = existingTile.getX();
            int existingYStart = existingTile.getY();
            int existingXEnd = existingXStart + UnitType.Terran_Engineering_Bay.tileWidth();
            int existingYEnd = existingYStart + UnitType.Terran_Engineering_Bay.tileHeight();

            if (rectanglesIntersect(newX, newY, endX, endY, existingXStart, existingYStart, existingXEnd, existingYEnd)) {
                return true;
            }
        }

        for (TilePosition existingTile : mediumBuildTiles) {
            int existingXStart = existingTile.getX();
            int existingYStart = existingTile.getY();
            int existingXEnd = existingTile.getX() + UnitType.Terran_Supply_Depot.tileWidth();
            int existingYEnd = existingTile.getY() + UnitType.Terran_Supply_Depot.tileHeight();

            if (rectanglesIntersect(newX, newY, endX, endY, existingXStart, existingYStart, existingXEnd, existingYEnd)) {
                return true;
            }
        }

        for (TilePosition existingTile : mineralLineTurrets.values()) {
            if (existingTile == null) {
                continue;
            }

            int existingXStart = existingTile.getX();
            int existingYStart = existingTile.getY();
            int existingXEnd = existingTile.getX() + UnitType.Terran_Missile_Turret.tileWidth();
            int existingYEnd = existingTile.getY() + UnitType.Terran_Missile_Turret.tileHeight();

            if (rectanglesIntersect(newX, newY, endX, endY, existingXStart, existingYStart, existingXEnd, existingYEnd)) {
                return true;
            }
        }

        for (TilePosition existingTile : mainTurrets) {
            int existingXStart = existingTile.getX();
            int existingYStart = existingTile.getY();
            int existingXEnd = existingTile.getX() + UnitType.Terran_Missile_Turret.tileWidth();
            int existingYEnd = existingTile.getY() + UnitType.Terran_Missile_Turret.tileHeight();

            if (rectanglesIntersect(newX, newY, endX, endY, existingXStart, existingYStart, existingXEnd, existingYEnd)) {
                return true;
            }
        }

        //Check against existing buildings
        for (Unit building : game.self().getUnits()) {
            if (!building.getType().isBuilding()) {
                continue;
            }

            TilePosition buildingTile = building.getTilePosition();
            int buildingX = buildingTile.getX();
            int buildingY = buildingTile.getY();
            int buildingEndX = buildingX + building.getType().tileWidth();
            int buildingEndY = buildingY + building.getType().tileHeight();

            if (rectanglesIntersect(newX, newY, endX, endY, buildingX, buildingY, buildingEndX, buildingEndY)) {
                return true;
            }
        }

        return false;
    }


    private boolean tooCloseToNaturalResources(TilePosition candidate, int ccWidth, int ccHeight) {
        int minGap = 3;
        Base natural = mapInfo.getNaturalBase();

        int ccX1 = candidate.getX();
        int ccY1 = candidate.getY();
        int ccX2 = ccX1 + ccWidth;
        int ccY2 = ccY1 + ccHeight;

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
        int ccX_end = ccX + UnitType.Terran_Command_Center.tileWidth();
        int ccHeight = UnitType.Terran_Command_Center.tileHeight();

        for (int x = ccX; x < ccX_end + 3; x++) {
            for (int y = ccY; y < ccY + ccHeight; y++) {
                ccExclusionTiles.add(new TilePosition(x, y));
            }
        }
    }

    private void chokeExclusionZone(Base base) {
        int exclusionRadius = 3;

        for (ChokePoint choke : base.getArea().getChokePoints()) {
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

        int barWidth = UnitType.Terran_Barracks.tileWidth();
        int barHeight = UnitType.Terran_Barracks.tileHeight();

        for (TilePosition noGapTile : largeBuildTilesNoGap) {
            int tileX = noGapTile.getX();
            int tileY = noGapTile.getY();

            if (rectanglesIntersect(addonX1, addonY1, addonX2, addonY2, tileX, tileY, tileX + barWidth, tileY + barHeight)) {
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

    public TilePosition getNaturalChokeBunker() {
        return naturalChokeBunker;
    }

    public TilePosition getNaturalBunkerEbayPosition() {
        return naturalBunkerEbayPosition;
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

        for (TilePosition tilePosition : largeBuildTiles) {
            if (unit.getTilePosition().equals(tilePosition)) {
                largeBuildTiles.removeIf(tile -> tile.equals(tilePosition));
                break;
            }
        }

        for (TilePosition tilePosition : largeBuildTilesNoGap) {
            if (unit.getTilePosition().equals(tilePosition)) {
                largeBuildTilesNoGap.removeIf(tile -> tile.equals(tilePosition));
                break;
            }
        }

        for (TilePosition tilePosition : mediumBuildTiles) {
            if (unit.getTilePosition().equals(tilePosition)) {
                mediumBuildTiles.removeIf(tile -> tile.equals(tilePosition));
                break;
            }
        }
    }
}
