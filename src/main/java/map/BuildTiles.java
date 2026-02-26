package map;

import bwapi.*;
import bwem.Base;
import bwem.ChokePoint;
import bwem.Geyser;
import bwem.Mineral;
import information.BaseInfo;
import information.GameState;
import util.PositionInterpolator;

import java.util.*;

public class BuildTiles {
    private Game game;
    private GameState gameState;
    private BaseInfo baseInfo;
    private TilePositionValidator tilePositionValidator;
    private HashSet<TilePosition> mediumBuildTiles = new HashSet<>();
    private HashSet<TilePosition> largeBuildTiles = new HashSet<>();
    private HashSet<TilePosition> largeBuildTilesNoGap = new HashSet<>();
    private HashSet<TilePosition> mineralExlusionTiles = new HashSet<>();
    private HashSet<TilePosition> geyserExlusionTiles = new HashSet<>();
    private HashSet<TilePosition> ccExclusionTiles = new HashSet<>();
    private HashSet<TilePosition> chokeExclusionTiles = new HashSet<>();
    private HashSet<TilePosition> frontBaseTiles = new HashSet<>();
    private HashSet<TilePosition> backBaseTiles = new HashSet<>();
    private HashSet<TilePosition> mainTurrets = new HashSet<>();
    private HashMap<Base, TilePosition> mineralLineTurrets = new HashMap<>();
    private TilePosition mainChokeBunker;
    private TilePosition naturalChokeBunker;
    private TilePosition closeBunkerTile;
    private TilePosition mainChokeTurret;
    private TilePosition naturalChokeTurret;
    private Base startingBase;
    private boolean naturalTilesGenerated = false;
    private boolean minOnlyTilesGenerated = false;

    public BuildTiles(Game game, BaseInfo baseInfo, GameState gameState) {
        this.game = game;
        this.baseInfo = baseInfo;
        this.gameState = gameState;

        tilePositionValidator = new TilePositionValidator(game);

        startingBase = baseInfo.getStartingBase();
        generateBuildTiles();
    }

    private void generateBuildTiles() {
        mineralExclusionZone(startingBase);
        geyserExclusionZone(startingBase);
        ccExclusionZone(startingBase);
        mineralExclusionZone(baseInfo.getNaturalBase());
        geyserExclusionZone(baseInfo.getNaturalBase());
        ccExclusionZone(baseInfo.getNaturalBase());
        chokeExclusionZone(startingBase);
        generateFrontBaseTiles();
        generateBackBaseTiles();
        generateChokeBunkerTiles();
        generateCloseBunkerTile();
        mainChokeTurret = generateChokeTurretTile(mainChokeBunker, startingBase);
        naturalChokeTurret = generateChokeTurretTile(naturalChokeBunker, baseInfo.getNaturalBase());
        //generateChokeTurretTiles();
        generateLargeTiles(frontBaseTiles);
        generateMediumTiles(backBaseTiles);
        generateTurretTiles();
    }

    private void regenerateBuildTiles() {
        if(!naturalTilesGenerated && mediumBuildTiles.isEmpty() || mediumBuildTiles.size() == 1) {
            chokeExclusionZone(baseInfo.getNaturalBase());
            //generateMediumTiles(frontBaseTiles);
            generateMediumTiles(baseInfo.getNaturalTiles());
            naturalTilesGenerated = true;
        }

        if(!baseInfo.getMinBaseTiles().isEmpty() && baseInfo.getMinOnlyBase() != null && !minOnlyTilesGenerated) {
            chokeExclusionTiles.clear();
            mineralExclusionZone(baseInfo.getMinOnlyBase());
            geyserExclusionZone(baseInfo.getMinOnlyBase());
            ccExclusionZone(baseInfo.getMinOnlyBase());
            generateLargeTiles(baseInfo.getMinBaseTiles());
            minOnlyTilesGenerated = true;
        }
        else if(baseInfo.getMinBaseTiles().isEmpty()) {
            minOnlyTilesGenerated = true;
        }
    }

    //TODO: I hate all of this
    // Final pass still not generating enough on some maps
    private void generateLargeTiles(HashSet<TilePosition> baseTiles) {
        TilePosition ccPos = baseInfo.getStartingBase().getLocation();

        //Sort tiles to favor closer to CC
        List<TilePosition> ccOrderedTiles = new ArrayList<>(baseTiles);
        TilePosition chokeCenter = baseInfo.getMainChoke().getCenter().toTilePosition();
        Comparator<TilePosition> comparator = Comparator.comparingInt(TilePosition::getY);

        if(ccPos.getX() <= chokeCenter.getX()) {
            comparator = comparator.thenComparingInt(TilePosition::getX);
        }
        else {
            comparator = comparator.thenComparing(Comparator.comparingInt(TilePosition::getX).reversed());
        }

        ccOrderedTiles.sort(comparator);

        //First pass largeBuildTilesNoGap with no gap stacks
        for(TilePosition tile : ccOrderedTiles) {
            if(largeBuildTilesNoGap.size() >= 4) {
                break;
            }

            if(backBaseTiles.contains(tile)) {
                continue;
            }

            if(isValidBarracksStack(tile, 0) && !largeBuildTilesNoGap.contains(tile) && !largeBuildTiles.contains(tile)) {
                addBarracksStackNoGap(tile.getX(), tile.getY(), UnitType.Terran_Barracks);
            }
        }

        //Second pass largeBuildTilesNoGap with no gap no stack
        if(largeBuildTilesNoGap.size() < 4) {
            int barWidth = UnitType.Terran_Barracks.tileWidth();
            int barHeight = UnitType.Terran_Barracks.tileHeight();

            for(TilePosition tile : ccOrderedTiles) {
                if(largeBuildTilesNoGap.size() >= 4) {
                    break;
                }

                if(backBaseTiles.contains(tile)) {
                    continue;
                }

                if(!isValidBarracksPosition(tile, 0)) {
                    continue;
                }

                boolean overlapsWithGapTiles = false;
                for(TilePosition gapTile : largeBuildTilesNoGap) {
                    int gapFootprintEndX = gapTile.getX() + barWidth;
                    int gapFootprintEndY = gapTile.getY() + barHeight;

                    if(!(tile.getX() + barWidth <= gapTile.getX() ||
                            tile.getX() >= gapFootprintEndX ||
                            tile.getY() + barHeight <= gapTile.getY() ||
                            tile.getY() >= gapFootprintEndY)) {
                        overlapsWithGapTiles = true;
                        break;
                    }
                }

                if(overlapsWithGapTiles) {
                    continue;
                }

                boolean overlapsWithNoGapTiles = false;
                for(TilePosition noGapTile : largeBuildTiles) {
                    int noGapFootprintEndX = noGapTile.getX() + barWidth + 3;
                    int noGapFootprintEndY = noGapTile.getY() + barHeight;

                    if(!(tile.getX() + barWidth <= noGapTile.getX() ||
                            tile.getX() >= noGapFootprintEndX ||
                            tile.getY() + barHeight <= noGapTile.getY() ||
                            tile.getY() >= noGapFootprintEndY)) {
                        overlapsWithNoGapTiles = true;
                        break;
                    }
                }

                if(!overlapsWithNoGapTiles) {
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
        for(TilePosition tile : sortedFrontTiles) {
            if(largeBuildTiles.size() >= 10) {
                break;
            }

            if(backBaseTiles.contains(tile)) {
                continue;
            }

            if(isValidBarracksStack(tile, 3) && !largeBuildTiles.contains(tile)) {
                int stackX = tile.getX();
                int stackY = tile.getY();

                int adjacentY = stackY + UnitType.Terran_Barracks.tileHeight();
                TilePosition adjacentPos = new TilePosition(stackX, adjacentY);

                if(baseTiles.contains(adjacentPos) && isValidBarracksStack(adjacentPos, 3)) {
                    addBarracksStack(stackX, stackY, UnitType.Terran_Barracks);
                }
            }
        }

        //Second pass largeBuildTiles with 2 tile gap stacks (may cause blockage?)
        for(TilePosition tile : sortedFrontTiles) {
            if(largeBuildTiles.size() >= 12) {
                break;
            }

            if(backBaseTiles.contains(tile)) {
                continue;
            }

            if(isValidBarracksStack(tile, 2) && !largeBuildTiles.contains(tile)) {
                int stackX = tile.getX();
                int stackY = tile.getY();

                int adjacentX = stackX + UnitType.Terran_Barracks.tileWidth() + 2;
                TilePosition adjacentPos = new TilePosition(adjacentX, stackY);

                if(frontBaseTiles.contains(adjacentPos) && isValidBarracksStack(adjacentPos, 2)) {
                    addBarracksStack(stackX, stackY, UnitType.Terran_Barracks);
                }
            }
        }

        //Third pass largeBuildTiles with 3 tile gap no stack
        if(largeBuildTiles.size() < 6) {
            int barWidth = UnitType.Terran_Barracks.tileWidth();
            int barHeight = UnitType.Terran_Barracks.tileHeight();

            for(TilePosition tile : sortedFrontTiles) {
                if(largeBuildTiles.size() >= 8) {
                    break;
                }

                if(backBaseTiles.contains(tile)) {
                    continue;
                }

                if(!isValidBarracksPosition(tile, 3)) {
                    continue;
                }

                boolean overlapsWithGapTiles = false;
                for(TilePosition gapTile : largeBuildTiles) {
                    int gapFootprintEndX = gapTile.getX() + barWidth + 3;
                    int gapFootprintEndY = gapTile.getY() + barHeight;

                    if(!(tile.getX() + barWidth <= gapTile.getX() ||
                            tile.getX() >= gapFootprintEndX ||
                            tile.getY() + barHeight <= gapTile.getY() ||
                            tile.getY() >= gapFootprintEndY)) {
                        overlapsWithGapTiles = true;
                        break;
                    }
                }

                if(overlapsWithGapTiles) {
                    continue;
                }

                boolean overlapsWithNoGapTiles = false;
                for(TilePosition noGapTile : largeBuildTilesNoGap) {
                    int noGapFootprintEndX = noGapTile.getX() + barWidth + 3;
                    int noGapFootprintEndY = noGapTile.getY() + barHeight;

                    if(!(tile.getX() + barWidth <= noGapTile.getX() ||
                            tile.getX() >= noGapFootprintEndX ||
                            tile.getY() + barHeight <= noGapTile.getY() ||
                            tile.getY() >= noGapFootprintEndY)) {
                        overlapsWithNoGapTiles = true;
                        break;
                    }
                }

                if(!overlapsWithNoGapTiles) {
                    largeBuildTiles.add(tile);
                }
            }
        }

        //3rd pass largeBuildTilesNoGap with no gap stacks (Fill in more barracks tiles)
        for(TilePosition tile : sortedFrontTiles) {
            if(largeBuildTilesNoGap.size() >= 6) {
                break;
            }

            if(backBaseTiles.contains(tile)) {
                continue;
            }

            if(isValidBarracksStack(tile, 0) && !largeBuildTilesNoGap.contains(tile) && !largeBuildTiles.contains(tile)) {
                addBarracksStackNoGap(tile.getX(), tile.getY(), UnitType.Terran_Barracks);
            }
        }

        //4th pass largeBuildTilesNoGap with no gap no stack
        if(largeBuildTilesNoGap.size() < 6) {
            int barWidth = UnitType.Terran_Barracks.tileWidth();
            int barHeight = UnitType.Terran_Barracks.tileHeight();

            for(TilePosition tile : sortedFrontTiles) {
                if(largeBuildTilesNoGap.size() >= 6) {
                    break;
                }

                if(backBaseTiles.contains(tile)) {
                    continue;
                }

                if(!isValidBarracksPosition(tile, 0)) {
                    continue;
                }

                boolean overlapsWithGapTiles = false;
                for(TilePosition gapTile : largeBuildTilesNoGap) {
                    int gapFootprintEndX = gapTile.getX() + barWidth;
                    int gapFootprintEndY = gapTile.getY() + barHeight;

                    if(!(tile.getX() + barWidth <= gapTile.getX() ||
                            tile.getX() >= gapFootprintEndX ||
                            tile.getY() + barHeight <= gapTile.getY() ||
                            tile.getY() >= gapFootprintEndY)) {
                        overlapsWithGapTiles = true;
                        break;
                    }
                }

                if(overlapsWithGapTiles) {
                    continue;
                }

                boolean overlapsWithNoGapTiles = false;
                for(TilePosition noGapTile : largeBuildTiles) {
                    int noGapFootprintEndX = noGapTile.getX() + barWidth + 3;
                    int noGapFootprintEndY = noGapTile.getY() + barHeight;

                    if(!(tile.getX() + barWidth <= noGapTile.getX() ||
                            tile.getX() >= noGapFootprintEndX ||
                            tile.getY() + barHeight <= noGapTile.getY() ||
                            tile.getY() >= noGapFootprintEndY)) {
                        overlapsWithNoGapTiles = true;
                        break;
                    }
                }

                if(!overlapsWithNoGapTiles) {
                    largeBuildTilesNoGap.add(tile);
                }
            }
        }
    }

    private boolean isValidBarracksStack(TilePosition topTile, int gap) {
        int barHeight = UnitType.Terran_Barracks.tileHeight();
        TilePosition bottomTile = new TilePosition(topTile.getX(), topTile.getY() + barHeight);

        return isValidBarracksPosition(topTile, gap) && isValidBarracksPosition(bottomTile, gap);
    }

    private boolean isValidBarracksPosition(TilePosition tile, int gap) {
        int barWidth = UnitType.Terran_Barracks.tileWidth();
        int barHeight = UnitType.Terran_Barracks.tileHeight();

        if(intersectsExistingBuildTiles(tile, UnitType.Terran_Barracks, gap)) {
            return false;
        }

        for(int x = 0; x < barWidth; x++) {
            for(int y = 0; y < barHeight; y++) {
                TilePosition checkTile = new TilePosition(tile.getX() + x, tile.getY() + y);

                if(backBaseTiles.contains(tile)) {
                    return false;
                }

                if(intersectsExclusionZones(checkTile) || !tilePositionValidator.isBuildable(checkTile)) {
                    return false;
                }
            }
        }

        for(int bufferX = 0; bufferX < gap; bufferX++) {
            for(int y = 0; y < barHeight; y++) {
                TilePosition bufferTile = new TilePosition(tile.getX() + barWidth + bufferX, tile.getY() + y);

                if(!tilePositionValidator.isWithinMap(bufferTile) || intersectsExclusionZones(bufferTile) || !tilePositionValidator.isWalkable(bufferTile)) {
                    return false;
                }
            }
        }

        //Check tile gap below stack
        for(int x = 0; x < barWidth; x++) {
            TilePosition gapTile = new TilePosition(tile.getX() + x, tile.getY() - 1);

            if(intersectsExclusionZones(gapTile) || !tilePositionValidator.isWalkable(gapTile) || intersectsExistingBuildTiles(gapTile, UnitType.Terran_Barracks, 0)) {
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

        List<TilePosition> sortedBackTiles = new ArrayList<>(baseTiles);
        HashSet<TilePosition> usedTiles = new HashSet<>();

        sortedBackTiles.sort(
                Comparator.comparingInt(TilePosition::getY)
                        .thenComparingInt(TilePosition::getX)
        );

        for(TilePosition tile : sortedBackTiles) {
            if(mediumBuildTiles.size() >= 22) {
                break;
            }

            if(usedTiles.contains(tile)) {
                continue;
            }

            TilePosition bottomTile = new TilePosition(tile.getX(), tile.getY() + depotType.tileHeight());

            if(usedTiles.contains(bottomTile) || !baseTiles.contains(bottomTile)) {
                continue;
            }

            if(intersectsExistingBuildTiles(tile, depotType, 0)) {
                continue;
            }

            if(validMediumTileStack(tile, depotType)) {
                mediumBuildTiles.add(tile);
                mediumBuildTiles.add(bottomTile);
                usedTiles.add(tile);
                usedTiles.add(bottomTile);

                for(int y = 0; y < 2 * depotType.tileHeight(); y++) {
                    usedTiles.add(new TilePosition(tile.getX() + depotType.tileWidth(), tile.getY() + y));
                }
            }
        }
    }



    //Try to generate two medium tiles on top of each other
    private boolean validMediumTileStack(TilePosition topTile, UnitType depotType) {
        int depotHeight = depotType.tileHeight();
        TilePosition bottomTile = new TilePosition(topTile.getX(), topTile.getY() + depotHeight);

        return validMediumTilePosition(topTile, depotType) && validMediumTilePosition(bottomTile, depotType);
    }

    private boolean validMediumTilePosition(TilePosition tile, UnitType depot) {
        int depotWidth = depot.tileWidth();
        int depotHeight = depot.tileHeight();

        for(int x = 0; x < depotWidth; x++) {
            for(int y = 0; y < depotHeight; y++) {
                TilePosition checkTile = new TilePosition(tile.getX() + x, tile.getY() + y);

                if(intersectsExclusionZones(checkTile) || !tilePositionValidator.isBuildable(checkTile)
                        || intersectsExistingBuildTiles(checkTile, depot, 0)) {
                    return false;
                }
            }
        }

        //TODO: Check for a gap in any direction
        //Check tile gap to the right
        for(int y = 0; y < depotHeight; y++) {
            TilePosition gapTile = new TilePosition(tile.getX() + depotWidth, tile.getY() + y);

            if(intersectsExclusionZones(gapTile) || !tilePositionValidator.isWalkable(gapTile) || intersectsExistingBuildTiles(gapTile, depot, 0)) {
                return false;
            }
        }

        return !intersectsNeutralBuildings(tile, depotWidth, depotHeight);
    }

    private void generateCloseBunkerTile() {
        TilePosition chokePos = baseInfo.getMainChoke().getCenter().toTilePosition();
        TilePosition basePos = baseInfo.getStartingBase().getLocation();

        int firstMidX = (chokePos.getX() + basePos.getX()) / 2;
        int firstMidY = (chokePos.getY() + basePos.getY()) / 2;
        int finalMidX = (firstMidX + basePos.getX()) / 2;
        int finalMidY = (firstMidY + basePos.getY()) / 2;
        TilePosition finalMidPoint = new TilePosition(finalMidX, finalMidY);

        int searchRadius = 4;
        int closestDistance = Integer.MAX_VALUE;
        int bunkerWidth = UnitType.Terran_Bunker.tileWidth();
        int bunkerHeight = UnitType.Terran_Bunker.tileHeight();

        for(int x = finalMidX - searchRadius; x <= finalMidX + searchRadius; x++) {
            for(int y = finalMidY - searchRadius; y <= finalMidY + searchRadius; y++) {
                TilePosition testPos = new TilePosition(x, y);
                boolean validLocation = true;

                for(int bx = x; bx < x + bunkerWidth; bx++) {
                    for(int by = y; by < y + bunkerHeight; by++) {
                        TilePosition footprintTile = new TilePosition(bx, by);
                        if(intersectsExclusionZones(footprintTile)) {
                            validLocation = false;
                            break;
                        }
                    }
                    if(!validLocation) {
                        break;
                    }
                }

                if(!validLocation) continue;

                if(!tilePositionValidator.isBuildable(testPos, UnitType.Terran_Bunker)) {
                    continue;
                }

                int distToMid = testPos.getApproxDistance(finalMidPoint);
                if(distToMid < closestDistance) {
                    closestDistance = distToMid;
                    closeBunkerTile = testPos;
                }
            }
        }
    }

    private void generateChokeBunkerTiles() {
        ChokePoint mainChoke = baseInfo.getMainChoke();

        if(mainChoke != null) {
            TilePosition chokeTile = mainChoke.getCenter().toTilePosition();
            TilePosition baseTile = baseInfo.getStartingBase().getLocation();
            double percent = 0.90;

            //temp solution fix later
            if(baseInfo.getMinOnlyBase() != null) {
                ChokePoint otherChoke = null;

                for(ChokePoint cp : baseInfo.getMinOnlyBase().getArea().getChokePoints()) {
                    if(cp.equals(mainChoke)) {
                        continue;
                    }

                    otherChoke = cp;
                }

                if(otherChoke != null) {
                    TilePosition minOnlyLocation = baseInfo.getMinOnlyBase().getLocation();
                    TilePosition otherChokePos = otherChoke.getCenter().toTilePosition();
                    int midX = (minOnlyLocation.getX() + otherChokePos.getX()) / 2;
                    int midY = (minOnlyLocation.getY() + otherChokePos.getY()) / 2;
                    baseTile = new TilePosition(midX, midY);
                } else {
                    baseTile = baseInfo.getMinOnlyBase().getLocation();
                }
                percent = 0.80;
            }

            TilePosition closerMain = PositionInterpolator.interpolate(baseTile, chokeTile, percent);
            TilePosition mainBunker = findValidTileNear(closerMain, UnitType.Terran_Bunker);



            int minDist = Integer.MAX_VALUE;

            if(!baseInfo.getMinBaseTiles().isEmpty()) {
                for(TilePosition candidate : baseInfo.getMinBaseTiles()) {
                    if (tilePositionValidator.isBuildable(candidate, UnitType.Terran_Bunker) && !intersectsExclusionZones(candidate)
                            && !intersectsExistingBuildTiles(candidate, UnitType.Terran_Bunker, 0)) {
                        int dist = candidate.getApproxDistance(closerMain);
                        if(dist < minDist) {
                            minDist = dist;
                            mainBunker = candidate;
                        }
                    }
                }

                if(mainBunker != null && (baseInfo.getMinBaseTiles().contains(mainBunker))) {
                    mainChokeBunker = mainBunker;
                }

            }
            else {
                for(TilePosition candidate : frontBaseTiles) {
                    if (tilePositionValidator.isBuildable(candidate, UnitType.Terran_Bunker) && !intersectsExclusionZones(candidate)
                            && !intersectsExistingBuildTiles(candidate, UnitType.Terran_Bunker, 0)) {
                        int dist = candidate.getApproxDistance(closerMain);
                        if(dist < minDist) {
                            minDist = dist;
                            mainBunker = candidate;
                        }
                    }
                }

                if(mainBunker != null && (frontBaseTiles.contains(mainBunker))) {
                    mainChokeBunker = mainBunker;
                }
            }
        }

        ChokePoint naturalChoke = baseInfo.getNaturalChoke();

        if(naturalChoke != null) {
            TilePosition chokeTile = naturalChoke.getCenter().toTilePosition();
            TilePosition baseTile = baseInfo.getNaturalBase().getLocation();
            TilePosition closerNatural = PositionInterpolator.interpolate(chokeTile, baseTile, 0.10);
            TilePosition naturalBunker = findValidTileNear(closerNatural, UnitType.Terran_Bunker);



            if (naturalBunker != null) {
                naturalChokeBunker = naturalBunker;
            }
        }
    }

    private TilePosition findValidTileNear(TilePosition center, UnitType unitType) {
        int searchRadius = 6;

        for(int r = 0; r <= searchRadius; r++) {
            for(int dx = -r; dx <= r; dx++) {
                for(int dy = -r; dy <= r; dy++) {
                    if(Math.abs(dx) != r && Math.abs(dy) != r) continue;
                    TilePosition test = new TilePosition(center.getX() + dx, center.getY() + dy);
                    if(tilePositionValidator.isBuildable(test, unitType) && !intersectsExclusionZones(test) && !intersectsExistingBuildTiles(test, unitType, 0)) {
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

        for(int y = by - turretHeight + 1; y <= by + bunkerHeight - 1; y++) {
            adjacentPositions.add(new TilePosition(bx - turretWidth, y));
        }

        for(int y = by - turretHeight + 1; y <= by + bunkerHeight - 1; y++) {
            adjacentPositions.add(new TilePosition(bx + bunkerWidth, y));
        }

        for(int x = bx - turretWidth + 1; x <= bx + bunkerWidth - 1; x++) {
            adjacentPositions.add(new TilePosition(x, by - turretHeight));
        }

        for(int x = bx - turretWidth + 1; x <= bx + bunkerWidth - 1; x++) {
            adjacentPositions.add(new TilePosition(x, by + bunkerHeight));
        }

        TilePosition baseTile = base.getLocation();
        adjacentPositions.sort(Comparator.comparingInt(pos -> pos.getApproxDistance(baseTile)));

        for(TilePosition candidate : adjacentPositions) {
            if(tilePositionValidator.isBuildable(candidate, UnitType.Terran_Missile_Turret) &&
                    !intersectsExclusionZones(candidate) &&
                    !intersectsExistingBuildTiles(candidate, UnitType.Terran_Missile_Turret, 0)) {
                return candidate;
            }
        }

        return null;
    }

    private void generateTurretTiles() {
        for(Base base : baseInfo.getMapBases()) {
            if(base == null || base.getMinerals().isEmpty()) {
                continue;
            }

            //Currently grabs first patch instead of center patch
            TilePosition mineralPatch = base.getMinerals().iterator().next().getUnit().getTilePosition();
            TilePosition ccTile = base.getLocation();

            int midX = (ccTile.getX() + mineralPatch.getX()) / 2;
            int midY = (ccTile.getY() + mineralPatch.getY()) / 2;
            TilePosition midPoint = new TilePosition(midX, midY);

            HashSet<TilePosition> baseTiles = baseInfo.getTilesForBase(base);

            int searchRadius = 6;
            TilePosition bestTurret = null;

            int ccX = ccTile.getX();
            int ccY = ccTile.getY();
            int ccXEnd = ccX + UnitType.Terran_Command_Center.tileWidth() + 2;
            int ccYEnd = ccY + UnitType.Terran_Command_Center.tileHeight();

            boolean found = false;

            for(int r = 0; r <= searchRadius && !found; r++) {
                for(int dx = -r; dx <= r && !found; dx++) {
                    for(int dy = -r; dy <= r; dy++) {
                        if(Math.abs(dx) != r && Math.abs(dy) != r) {
                            continue;
                        }

                        int tx = midPoint.getX() + dx;
                        int ty = midPoint.getY() + dy;
                        TilePosition test = new TilePosition(tx, ty);

                        if(!baseTiles.contains(test)) {
                            continue;
                        }

                        if(!tilePositionValidator.isBuildable(test, UnitType.Terran_Missile_Turret) || !tilePositionValidator.isWalkable(test)) {
                            continue;
                        }

                        if(intersectsExclusionZones(test)) {
                            continue;
                        }

                        int turretWidth = UnitType.Terran_Missile_Turret.tileWidth();
                        int turretHeight = UnitType.Terran_Missile_Turret.tileHeight();

                        boolean overlapsRightBuffer = rectanglesIntersect(tx, ty, tx + turretWidth, ty + turretHeight, ccX, ccY, ccXEnd, ccYEnd);

                        if(overlapsRightBuffer || intersectsExistingBuildTiles(test, UnitType.Terran_Missile_Turret, 0) || intersectsExistingBuildTiles(test, UnitType.Terran_Missile_Turret, 0)) {
                            continue;
                        }

                        bestTurret = test;
                        found = true;
                        break;
                    }
                }
            }

            if(bestTurret != null) {
                mineralLineTurrets.put(base, bestTurret);
            }
        }

        List<TilePosition> front = getValidTurretTiles(frontBaseTiles);
        List<TilePosition> back = getValidTurretTiles(backBaseTiles);

        Collections.shuffle(front);
        Collections.shuffle(back);

        int addedFront = 0;
        for(TilePosition candidate : front) {
            if(addedFront >= 3) {
                break;
            }

            if(!intersectsExistingBuildTiles(candidate, UnitType.Terran_Missile_Turret, 0)) {
                mainTurrets.add(candidate);
                addedFront++;
            }
        }

        int addedBack = 0;
        for(TilePosition candidate : back) {
            if(addedBack >= 2) {
                break;
            }

            if(!intersectsExistingBuildTiles(candidate, UnitType.Terran_Missile_Turret, 0)) {
                mainTurrets.add(candidate);
                addedBack++;
            }
        }
    }

    private List<TilePosition> getValidTurretTiles(Set<TilePosition> baseTiles) {
        List<TilePosition> valid = new ArrayList<>();

        for(TilePosition tile : baseTiles) {
            int tx = tile.getX();
            int ty = tile.getY();

            boolean insideBase = true;
            for(int dx = 0; dx < 2 && insideBase; dx++) {
                for(int dy = 0; dy < 2; dy++) {
                    if(!baseTiles.contains(new TilePosition(tx + dx, ty + dy))) {
                        insideBase = false;
                        break;
                    }
                }
            }
            if(!insideBase) {
                continue;
            }

            if(!tilePositionValidator.isBuildable(tile, UnitType.Terran_Missile_Turret)) {
                continue;
            }

            if(intersectsExclusionZones(tile)) {
                continue;
            }

            if(intersectsExistingBuildTiles(tile, UnitType.Terran_Missile_Turret, 0)) {
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

        TilePosition ccPosition = baseInfo.getStartingBase().getLocation();
        int ccX = ccPosition.getX();
        int ccY = ccPosition.getY();
        int ccWidth = UnitType.Terran_Command_Center.tileWidth();
        int ccHeight = UnitType.Terran_Command_Center.tileHeight();

        boolean inCCBuffer = (newX >= ccX - 2 && newX < ccX + ccWidth + 2) && (newY >= ccY - 2 && newY < ccY + ccHeight + 2);

        if(inCCBuffer) {
            return true;
        }

        if(closeBunkerTile != null) {
            int bunkerXStart = closeBunkerTile.getX();
            int bunkerYStart = closeBunkerTile.getY();
            int bunkerXEnd = closeBunkerTile.getX() + UnitType.Terran_Bunker.tileWidth();
            int bunkerYEnd = closeBunkerTile.getY() + UnitType.Terran_Bunker.tileHeight();

            if(rectanglesIntersect(newX, newY, endX, endY, bunkerXStart, bunkerYStart, bunkerXEnd, bunkerYEnd)) {
                return true;
            }
        }

        if(mainChokeBunker != null) {
            int mainChokeBunkerX = mainChokeBunker.getX();
            int mainChokeBunkerY = mainChokeBunker.getY();
            int mainChokeXEnd = mainChokeBunkerX + UnitType.Terran_Bunker.tileWidth();
            int mainChokeYEnd = mainChokeBunkerY + UnitType.Terran_Bunker.tileHeight();

            if(rectanglesIntersect(newX, newY, endX, endY, mainChokeBunkerX, mainChokeBunkerY, mainChokeXEnd, mainChokeYEnd)) {
                return true;
            }
        }

        if(mainChokeTurret != null) {
            int mainChokeTurretX = mainChokeTurret.getX();
            int mainChokeTurretY = mainChokeTurret.getY();
            int mainChokeTurretXEnd = mainChokeTurretX + UnitType.Terran_Missile_Turret.tileWidth();
            int mainChokeTurretYEnd = mainChokeTurretY + UnitType.Terran_Missile_Turret.tileHeight();

            if(rectanglesIntersect(newX, newY, endX, endY, mainChokeTurretX, mainChokeTurretY, mainChokeTurretXEnd, mainChokeTurretYEnd)) {
                return true;
            }
        }

        for(TilePosition existingTile : largeBuildTiles) {
            int existingXStart = existingTile.getX();
            int existingYStart = existingTile.getY();
            int existingXEnd = existingXStart + UnitType.Terran_Engineering_Bay.tileWidth();
            int existingYEnd = existingYStart + UnitType.Terran_Engineering_Bay.tileHeight();

            if(rectanglesIntersect(newX, newY, endX, endY, existingXStart, existingYStart, existingXEnd + 3, existingYEnd)) {
                return true;
            }
        }

        for(TilePosition existingTile : largeBuildTilesNoGap) {
            int existingXStart = existingTile.getX();
            int existingYStart = existingTile.getY();
            int existingXEnd = existingXStart + UnitType.Terran_Engineering_Bay.tileWidth();
            int existingYEnd = existingYStart + UnitType.Terran_Engineering_Bay.tileHeight();

            if(rectanglesIntersect(newX, newY, endX, endY, existingXStart, existingYStart, existingXEnd, existingYEnd)) {
                return true;
            }
        }

        for(TilePosition existingTile : mediumBuildTiles) {
            int existingXStart = existingTile.getX();
            int existingYStart = existingTile.getY();
            int existingXEnd = existingTile.getX() + UnitType.Terran_Supply_Depot.tileWidth();
            int existingYEnd = existingTile.getY() + UnitType.Terran_Supply_Depot.tileHeight();

            if(rectanglesIntersect(newX, newY, endX, endY, existingXStart, existingYStart, existingXEnd, existingYEnd)) {
                return true;
            }
        }

        for(TilePosition existingTile : mineralLineTurrets.values()) {
            if(existingTile == null) {
                continue;
            }

            int existingXStart = existingTile.getX();
            int existingYStart = existingTile.getY();
            int existingXEnd = existingTile.getX() + UnitType.Terran_Missile_Turret.tileWidth();
            int existingYEnd = existingTile.getY() + UnitType.Terran_Missile_Turret.tileHeight();

            if(rectanglesIntersect(newX, newY, endX, endY, existingXStart, existingYStart, existingXEnd, existingYEnd)) {
                return true;
            }
        }

        for(TilePosition existingTile : mainTurrets) {
            int existingXStart = existingTile.getX();
            int existingYStart = existingTile.getY();
            int existingXEnd = existingTile.getX() + UnitType.Terran_Missile_Turret.tileWidth();
            int existingYEnd = existingTile.getY() + UnitType.Terran_Missile_Turret.tileHeight();

            if(rectanglesIntersect(newX, newY, endX, endY, existingXStart, existingYStart, existingXEnd, existingYEnd)) {
                return true;
            }
        }

        //Check against existing buildings
        for(Unit building : game.self().getUnits()) {
            if(!building.getType().isBuilding()) {
                continue;
            }

            TilePosition buildingTile = building.getTilePosition();
            int buildingX = buildingTile.getX();
            int buildingY = buildingTile.getY();
            int buildingEndX = buildingX + building.getType().tileWidth();
            int buildingEndY = buildingY + building.getType().tileHeight();

            if(rectanglesIntersect(newX, newY, endX, endY, buildingX, buildingY, buildingEndX, buildingEndY)) {
                return true;
            }
        }

        return false;
    }


    private boolean intersectsNeutralBuildings(TilePosition tile, int width, int height) {
        for(Unit neutralBuilding : game.getNeutralUnits()) {
            if(!neutralBuilding.getType().isBuilding()) {
                continue;
            }

            if(neutralBuilding.getType() == UnitType.Resource_Vespene_Geyser || neutralBuilding.getType() == UnitType.Resource_Mineral_Field) {
                continue;
            }

            TilePosition buildingTile = neutralBuilding.getTilePosition();
            int buildingWidth = neutralBuilding.getType().tileWidth();
            int buildingHeight = neutralBuilding.getType().tileHeight();

            if(rectanglesIntersect(tile.getX(), tile.getY(), tile.getX() + width, tile.getY() + height,
                    buildingTile.getX(), buildingTile.getY(),
                    buildingTile.getX() + buildingWidth,  buildingTile.getY() + buildingHeight)) {
                return true;
            }
        }
        return false;
    }

    private void mineralExclusionZone(Base base) {
        TilePosition lowestXTile = null;
        TilePosition highestXTile = null;
        TilePosition lowestYTile = null;
        TilePosition highestYTile = null;
        TilePosition commandCenterTile = base.getLocation();

        for(Mineral mineral : base.getMinerals()) {
            TilePosition mineralTile = mineral.getTopLeft();

            if(lowestXTile == null || mineralTile.getX() < lowestXTile.getX()) {
                lowestXTile = mineralTile;
            }
            if(highestXTile == null || mineralTile.getX() > highestXTile.getX()) {
                highestXTile = mineralTile;
            }
            if(lowestYTile == null || mineralTile.getY() < lowestYTile.getY()) {
                lowestYTile = mineralTile;
            }
            if(highestYTile == null || mineralTile.getY() > highestYTile.getY()) {
                highestYTile = mineralTile;
            }
        }

        int boxStartX = Math.min(lowestXTile.getX(), commandCenterTile.getX() + 2);
        int boxEndX = Math.max(highestXTile.getX() + 1, commandCenterTile.getX() - 2);
        int boxStartY = Math.min(lowestYTile.getY(), commandCenterTile.getY());
        int boxEndY = Math.max(highestYTile.getY(), commandCenterTile.getY());

        for(int x = boxStartX; x <= boxEndX; x++) {
            for(int y = boxStartY; y <= boxEndY; y++) {
                mineralExlusionTiles.add(new TilePosition(x, y));
            }
        }

    }

    private void geyserExclusionZone(Base base) {
        if(base.getGeysers().isEmpty()) {
            return;
        }

        TilePosition geyserTile = null;

        for(Geyser geyser : base.getGeysers()) {
            geyserTile = geyser.getTopLeft();

            int geyserX = geyserTile.getX();
            int geyserY = geyserTile.getY();
            TilePosition commandCenterTile = base.getLocation();

            int boxStartX = Math.min(geyserX, commandCenterTile.getX());
            int boxEndX = Math.max(geyserX + 3, commandCenterTile.getX());
            int boxStartY = Math.min(geyserY, commandCenterTile.getY());
            int boxEndY = Math.max(geyserY + 2, commandCenterTile.getY());

            for(int x = boxStartX; x <= boxEndX; x++) {
                for(int y = boxStartY; y <= boxEndY; y++) {
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

        for(int x = ccX; x < ccX_end + 3; x++) {
            for(int y = ccY; y < ccY + ccHeight; y++) {
                ccExclusionTiles.add(new TilePosition(x, y));
            }
        }
    }

    private void chokeExclusionZone(Base base) {
        int exclusionRadius = 3;

        for(ChokePoint choke : base.getArea().getChokePoints()) {
            TilePosition chokeTile = choke.getCenter().toTilePosition();

            for(int x = chokeTile.getX() - exclusionRadius; x <= chokeTile.getX() + exclusionRadius; x++) {
                for(int y = chokeTile.getY() - exclusionRadius; y <= chokeTile.getY() + exclusionRadius; y++) {
                    TilePosition tile = new TilePosition(x, y);
                    if(tilePositionValidator.isWithinMap(tile)) {
                        chokeExclusionTiles.add(tile);
                    }
                }
            }
        }
    }

    private boolean intersectsExclusionZones(TilePosition tilePosition) {
        return geyserExlusionTiles.contains(tilePosition) || mineralExlusionTiles.contains(tilePosition)
                || ccExclusionTiles.contains(tilePosition) || chokeExclusionTiles.contains(tilePosition);
    }

    public boolean isAddonPositionBlocked(TilePosition buildingTile) {
        int addonX = buildingTile.getX() + 4;
        for(int dy = 0; dy < 3; dy++) {
            TilePosition addonTile = new TilePosition(addonX, buildingTile.getY() + dy);
            if(largeBuildTilesNoGap.contains(addonTile)) {
                return true;
            }
        }
        return false;
    }

    private void generateFrontBaseTiles() {
        HashSet<TilePosition> baseTiles = new HashSet<>(baseInfo.getBaseTiles());
        TilePosition chokePos = baseInfo.getMainChoke().getCenter().toTilePosition();
        TilePosition ccPos = baseInfo.getStartingBase().getLocation();

        int xDiff = Math.abs(chokePos.getX() - ccPos.getX());
        int yDiff = Math.abs(chokePos.getY() - ccPos.getY());

        for(TilePosition tilePosition : baseTiles) {
            if(geyserExlusionTiles.contains(tilePosition) || mineralExlusionTiles.contains(tilePosition)) {
                continue;
            }

            if(xDiff > yDiff) {
                if (chokePos.getX() < ccPos.getX() && tilePosition.getX() < ccPos.getX()) {
                    frontBaseTiles.add(tilePosition);
                } else if (chokePos.getX() > ccPos.getX() && tilePosition.getX() > ccPos.getX()) {
                    frontBaseTiles.add(tilePosition);
                }
            }
            else {
                if(chokePos.getY() < ccPos.getY() && tilePosition.getY() < ccPos.getY()) {
                    frontBaseTiles.add(tilePosition);
                }
                else if(chokePos.getY() > ccPos.getY() && tilePosition.getY() > ccPos.getY()) {
                    frontBaseTiles.add(tilePosition);
                }
            }
        }
    }

    private void generateBackBaseTiles() {
        HashSet<TilePosition> baseTiles = new HashSet<>(baseInfo.getBaseTiles());

        for(TilePosition tilePosition : baseTiles) {
            if(geyserExlusionTiles.contains(tilePosition) || mineralExlusionTiles.contains(tilePosition) || frontBaseTiles.contains(tilePosition)) {
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

    public void onFrame() {
            regenerateBuildTiles();
    }

    public void onUnitComplete(Unit unit) {
        if(!unit.getType().isBuilding()) {
            return;
        }

        for(TilePosition tilePosition : largeBuildTiles) {
            if(unit.getTilePosition().equals(tilePosition)) {
                largeBuildTiles.removeIf(tile -> tile.equals(tilePosition));
                break;
            }
        }

        for(TilePosition tilePosition : largeBuildTilesNoGap) {
            if(unit.getTilePosition().equals(tilePosition)) {
                largeBuildTilesNoGap.removeIf(tile -> tile.equals(tilePosition));
                break;
            }
        }

        for(TilePosition tilePosition : mediumBuildTiles) {
            if(unit.getTilePosition().equals(tilePosition)) {
                mediumBuildTiles.removeIf(tile -> tile.equals(tilePosition));
                break;
            }
        }
    }
}
