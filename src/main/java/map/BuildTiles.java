package map;

import bwapi.*;
import bwem.BWEM;
import bwem.Mineral;
import debug.Painters;
import information.BaseInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public class BuildTiles {
    private Game game;
    private BWEM bwem;
    private BaseInfo baseInfo;
    private TilePositionValidator tilePositionValidator;
    private Painters painters;
    private HashSet<TilePosition> mediumBuildTiles = new HashSet<>();
    private HashSet<TilePosition> largeBuildTiles = new HashSet<>();
    private HashSet<TilePosition> mineralExlusionTiles = new HashSet<>();
    private HashSet<TilePosition> geyserExlusionTiles = new HashSet<>();
    private HashSet<TilePosition> ccExclusionTiles = new HashSet<>();
    private HashSet<TilePosition> frontBaseTiles = new HashSet<>();
    private HashSet<TilePosition> backBaseTiles = new HashSet<>();
    private TilePosition bunkerTile;

    public BuildTiles(Game game, BWEM bwem, BaseInfo baseInfo) {
        this.game = game;
        this.bwem = bwem;
        this.baseInfo = baseInfo;

        tilePositionValidator = new TilePositionValidator(game);
        painters = new Painters(game, bwem);

        generateBuildTiles();
    }

    private void generateBuildTiles() {
        mineralExclusionZone();
        geyserExclusionZone();
        ccExclusionZone();
        generateFrontBaseTiles();
        generateBackBaseTiles();
        generateBunkerTiles();
        generateLargeTiles();
        generateMediumTiles();

    }

    private void generateLargeTiles() {
        int gap = 3;

        List<TilePosition> sortedFrontTiles = new ArrayList<>(frontBaseTiles);
        TilePosition chokePos = baseInfo.getMainChoke().getCenter().toTilePosition();
        TilePosition ccPos = baseInfo.getStartingBase().getLocation();
        int xDiff = Math.abs(chokePos.getX() - ccPos.getX());
        int yDiff = Math.abs(chokePos.getY() - ccPos.getY());

        if(xDiff > yDiff) {
            sortedFrontTiles.sort(Comparator.comparingInt(TilePosition::getX));
        }
        else {
            sortedFrontTiles.sort(Comparator.comparingInt(TilePosition::getY));
        }

        for(TilePosition tile : sortedFrontTiles) {
            if(largeBuildTiles.size() >= 12) {
                break;
            }

            if(backBaseTiles.contains(tile)) {
                continue;
            }

            if(isValidBarracksStack(tile, UnitType.Terran_Barracks, gap)) {
                int stackX = tile.getX();
                int stackY = tile.getY();

                int adjacentX = stackX + UnitType.Terran_Barracks.tileWidth() + gap;
                TilePosition adjacentPos = new TilePosition(adjacentX, stackY);

                if (frontBaseTiles.contains(adjacentPos) && isValidBarracksStack(adjacentPos, UnitType.Terran_Barracks, gap)) {
                    addBarracksStack(stackX, stackY, UnitType.Terran_Barracks);
                    addBarracksStack(adjacentX, stackY, UnitType.Terran_Barracks);
                }
            }
        }

        if(largeBuildTiles.size() < 12) {
            for(TilePosition tile : sortedFrontTiles) {
                if(largeBuildTiles.size() >= 12) {
                    break;
                }

                if(backBaseTiles.contains(tile)) {
                    continue;
                }

                if(isValidBarracksStack(tile, UnitType.Terran_Barracks, gap) && !largeBuildTiles.contains(tile)) {
                    addBarracksStack(tile.getX(), tile.getY(), UnitType.Terran_Barracks);
                }
            }
        }
    }

    private boolean isValidBarracksStack(TilePosition topTile, UnitType barracksType, int gap) {
        int barWidth = barracksType.tileWidth();
        int barHeight = barracksType.tileHeight();
        TilePosition bottomTile = new TilePosition(topTile.getX(), topTile.getY() + barHeight);
        TilePosition bufferRow = new TilePosition(topTile.getX(), topTile.getY() + 2 * barHeight);

        return isValidBarracksPosition(topTile, barracksType, gap) && isValidBarracksPosition(bottomTile, barracksType, gap) && isValidBufferRow(bufferRow, barWidth);
    }

    private boolean isValidBufferRow(TilePosition start, int width) {
        for(int i = 0; i < width; i++) {
            TilePosition bufferTile = new TilePosition(start.getX() + i, start.getY());

            if(!tilePositionValidator.isWithinMap(bufferTile) || intersectsMineralExclusionZone(bufferTile) || intersectsGeyserExclusionZone(bufferTile) || intersectsCCExclusionZone(bufferTile) || !tilePositionValidator.isWalkable(bufferTile)) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidBarracksPosition(TilePosition tile, UnitType barracksType, int gap) {
        int barWidth = barracksType.tileWidth();
        int barHeight = barracksType.tileHeight();

        for(int x = 0; x < barWidth; x++) {
            for(int y = 0; y < barHeight; y++) {
                TilePosition checkTile = new TilePosition(tile.getX() + x, tile.getY() + y);

                if(intersectsMineralExclusionZone(checkTile) || intersectsGeyserExclusionZone(checkTile) || intersectsCCExclusionZone(checkTile) || !tilePositionValidator.isBuildable(checkTile) || intersectsExistingBuildTiles(checkTile, barracksType)) {
                    return false;
                }
            }
        }

        for(int bufferX = 0; bufferX < gap; bufferX++) {
            for(int y = 0; y < barHeight; y++) {
                TilePosition bufferTile = new TilePosition(tile.getX() + barWidth + bufferX, tile.getY() + y);

                if(!tilePositionValidator.isWithinMap(bufferTile) || intersectsMineralExclusionZone(bufferTile) || intersectsGeyserExclusionZone(bufferTile) || intersectsCCExclusionZone(bufferTile) || !tilePositionValidator.isWalkable(bufferTile)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void addBarracksStack(int x, int y, UnitType barracksType) {
        int barHeight = barracksType.tileHeight();
        largeBuildTiles.add(new TilePosition(x, y));
        largeBuildTiles.add(new TilePosition(x, y + barHeight));
    }

    private boolean intersectsCCExclusionZone(TilePosition tile) {
        return ccExclusionTiles.contains(tile);
    }

    private void generateMediumTiles() {
        UnitType depotType = UnitType.Terran_Supply_Depot;
        int gap = 1;

        List<TilePosition> sortedBackTiles = new ArrayList<>(backBaseTiles);
        sortedBackTiles.sort(Comparator.comparingInt(TilePosition::getY).thenComparingInt(TilePosition::getX));
        HashSet<TilePosition> usedTiles = new HashSet<>();

        for(TilePosition tile : sortedBackTiles) {
            if(mediumBuildTiles.size() >= 24) {
                break;
            }

            if(usedTiles.contains(tile)) {
                continue;
            }

            TilePosition bottomTile = new TilePosition(tile.getX(), tile.getY() + depotType.tileHeight());

            if(usedTiles.contains(bottomTile) || !backBaseTiles.contains(bottomTile)) {
                continue;
            }

            if(isValidSupplyDepotStack(tile, depotType, gap)) {
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

    private boolean isValidSupplyDepotStack(TilePosition topTile, UnitType depotType, int gap) {
        int depotHeight = depotType.tileHeight();
        TilePosition bottomTile = new TilePosition(topTile.getX(), topTile.getY() + depotHeight);

        return isValidSupplyDepotPosition(topTile, depotType, gap) && isValidSupplyDepotPosition(bottomTile, depotType, gap);
    }

    private boolean isValidSupplyDepotPosition(TilePosition tile, UnitType depotType, int gap) {
        int depotWidth = depotType.tileWidth();
        int depotHeight = depotType.tileHeight();

        for(int x = 0; x < depotWidth; x++) {
            for(int y = 0; y < depotHeight; y++) {
                TilePosition checkTile = new TilePosition(tile.getX() + x, tile.getY() + y);

                if (intersectsMineralExclusionZone(checkTile) || intersectsGeyserExclusionZone(checkTile) || intersectsCCExclusionZone(checkTile) || !tilePositionValidator.isBuildable(checkTile) || intersectsExistingBuildTiles(checkTile, depotType)) {
                    return false;
                }
            }
        }

        for(int y = 0; y < depotHeight; y++) {
            TilePosition gapTile = new TilePosition(tile.getX() + depotWidth, tile.getY() + y);

            if(!tilePositionValidator.isWithinMap(gapTile) || intersectsMineralExclusionZone(gapTile) || intersectsGeyserExclusionZone(gapTile) || intersectsCCExclusionZone(gapTile) || !tilePositionValidator.isWalkable(gapTile) || isTileInPlannedBuildingFootprint(gapTile)) {
                return false;
            }
        }
        return true;
    }

    private boolean isTileInPlannedBuildingFootprint(TilePosition tile) {
        TilePosition ccPos = baseInfo.getStartingBase().getLocation();
        int ccX = ccPos.getX();
        int ccY = ccPos.getY();
        int ccWidth = UnitType.Terran_Command_Center.tileWidth();
        int ccHeight = UnitType.Terran_Command_Center.tileHeight();

        if (tile.getX() >= ccX && tile.getX() < ccX + ccWidth && tile.getY() >= ccY && tile.getY() < ccY + ccHeight) {
            return true;
        }

        if(bunkerTile != null) {
            int bunkerX = bunkerTile.getX();
            int bunkerY = bunkerTile.getY();
            int bunkerWidth = UnitType.Terran_Bunker.tileWidth();
            int bunkerHeight = UnitType.Terran_Bunker.tileHeight();

            if (tile.getX() >= bunkerX && tile.getX() < bunkerX + bunkerWidth && tile.getY() >= bunkerY && tile.getY() < bunkerY + bunkerHeight) {
                return true;
            }
        }

        for(TilePosition largeTile : largeBuildTiles) {int buildingWidth = UnitType.Terran_Barracks.tileWidth();int buildingHeight = UnitType.Terran_Barracks.tileHeight();
            if(tile.getX() >= largeTile.getX() && tile.getX() < largeTile.getX() + buildingWidth && tile.getY() >= largeTile.getY() && tile.getY() < largeTile.getY() + buildingHeight) {
                return true;
            }
        }

        for(TilePosition mediumTile : mediumBuildTiles) {
            int buildingWidth = UnitType.Terran_Supply_Depot.tileWidth();
            int buildingHeight = UnitType.Terran_Supply_Depot.tileHeight();
            if(tile.getX() >= mediumTile.getX() && tile.getX() < mediumTile.getX() + buildingWidth && tile.getY() >= mediumTile.getY() && tile.getY() < mediumTile.getY() + buildingHeight) {
                return true;
            }
        }
        return false;
    }

    private void generateBunkerTiles() {
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
                        if(intersectsMineralExclusionZone(footprintTile) || intersectsCCExclusionZone(footprintTile) || intersectsGeyserExclusionZone(footprintTile)) {
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
                    bunkerTile = testPos;
                }
            }
        }
    }

    private boolean intersectsExistingBuildTiles(TilePosition newTilePosition, UnitType unitType) {
        int newX = newTilePosition.getX();
        int newY = newTilePosition.getY();
        int typeWidth = unitType.tileWidth();
        int typeHeight = unitType.tileHeight();

        TilePosition ccPosition = baseInfo.getStartingBase().getLocation();
        int ccX = ccPosition.getX();
        int ccY = ccPosition.getY();
        int ccWidth = UnitType.Terran_Command_Center.tileWidth();
        int ccHeight = UnitType.Terran_Command_Center.tileHeight();

        boolean inCCBuffer = (newX >= ccX - 2 && newX < ccX + ccWidth + 2) && (newY >= ccY - 2 && newY < ccY + ccHeight + 2);

        if(inCCBuffer) {
            return true;
        }

        if(bunkerTile != null) {
            int bunkerXStart = bunkerTile.getX();
            int bunkerYStart = bunkerTile.getY();
            int bunkerXEnd = bunkerTile.getX() + UnitType.Terran_Bunker.tileWidth();
            int bunkerYEnd = bunkerTile.getY() + UnitType.Terran_Bunker.tileHeight();

            for(int x = newX; x < newX + typeWidth; x++) {
                for(int y = newY; y < newY + typeHeight; y++) {
                    if (x >= bunkerXStart && x < bunkerXEnd && y >= bunkerYStart && y < bunkerYEnd) {
                        return true;
                    }
                }
            }
        }

        for(TilePosition existingTile : largeBuildTiles) {
            int existingXStart = existingTile.getX();
            int existingYStart = existingTile.getY();
            int existingXEnd = existingXStart + UnitType.Terran_Engineering_Bay.tileWidth();
            int existingYEnd = existingYStart + UnitType.Terran_Engineering_Bay.tileHeight();

            for(int x = newX; x < newX + typeWidth; x++) {
                for(int y = newY; y < newY + typeHeight; y++) {
                    if(x >= existingXStart && x < existingXEnd && y >= existingYStart && y < existingYEnd) {
                        return true;
                    }
                }
            }

            int bufferXStart = existingXEnd;
            int bufferXEnd = existingXEnd + 3;
            int bufferYStart = existingYStart;
            int bufferYEnd = existingYEnd;

            for(int x = newX; x < newX + typeWidth; x++) {
                for(int y = newY; y < newY + typeHeight; y++) {
                    if(x >= bufferXStart && x < bufferXEnd && y >= bufferYStart && y < bufferYEnd) {
                        return true;
                    }
                }
            }
        }

        for(TilePosition existingTile : mediumBuildTiles) {
            int existingXStart = existingTile.getX();
            int existingYStart = existingTile.getY();
            int existingXEnd = existingTile.getX() + UnitType.Terran_Supply_Depot.tileWidth();
            int existingYEnd = existingTile.getY() + UnitType.Terran_Supply_Depot.tileHeight();

            for(int x = newX; x < newX + typeWidth; x++) {
                for(int y = newY; y < newY + typeHeight; y++) {
                    if(x >= existingXStart && x < existingXEnd && y >= existingYStart && y < existingYEnd) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void mineralExclusionZone() {
        TilePosition lowestXTile = null;
        TilePosition highestXTile = null;
        TilePosition lowestYTile = null;
        TilePosition highestYTile = null;
        TilePosition commandCenterTile = baseInfo.getStartingBase().getLocation();

        for(Mineral mineral : baseInfo.getStartingMinerals()) {
            TilePosition mineralTile = mineral.getUnit().getTilePosition();

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

        int boxStartX = Math.min(lowestXTile.getX(), commandCenterTile.getX());
        int boxEndX = Math.max(highestXTile.getX(), commandCenterTile.getX());
        int boxStartY = Math.min(lowestYTile.getY(), commandCenterTile.getY());
        int boxEndY = Math.max(highestYTile.getY(), commandCenterTile.getY());

        for(int x = boxStartX; x <= boxEndX; x++) {
            for(int y = boxStartY; y <= boxEndY; y++) {
                mineralExlusionTiles.add(new TilePosition(x, y));
            }
        }

    }

    private boolean intersectsMineralExclusionZone(TilePosition tilePosition) {
        return mineralExlusionTiles.contains(tilePosition);
    }

    private void geyserExclusionZone() {
        TilePosition geyserTile = baseInfo.getStartingGeysers().iterator().next().getUnit().getTilePosition();
        int geyserX = geyserTile.getX();
        int geyserY = geyserTile.getY();
        TilePosition commandCenterTile = baseInfo.getStartingBase().getLocation();

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

    private boolean intersectsGeyserExclusionZone(TilePosition tilePosition) {
        return geyserExlusionTiles.contains(tilePosition);
    }

    private void ccExclusionZone() {
        TilePosition commandCenterTile = baseInfo.getStartingBase().getLocation();
        int ccX = commandCenterTile.getX();
        int ccY = commandCenterTile.getY();
        int ccX_end = ccX + UnitType.Terran_Command_Center.tileWidth();
        int ccHeight = UnitType.Terran_Command_Center.tileHeight();

        for(int x = ccX_end; x < ccX_end + 3; x++) {
            for(int y = ccY; y < ccY + ccHeight; y++) {
                ccExclusionTiles.add(new TilePosition(x, y));
            }
        }
    }

    private void generateFrontBaseTiles() {
        HashSet<TilePosition> baseTiles = new HashSet(baseInfo.getBaseTiles());
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
        HashSet<TilePosition> baseTiles = new HashSet(baseInfo.getBaseTiles());

        for(TilePosition tilePosition : baseTiles) {
            if(geyserExlusionTiles.contains(tilePosition) || mineralExlusionTiles.contains(tilePosition) || frontBaseTiles.contains(tilePosition)) {
                continue;
            }
            backBaseTiles.add(tilePosition);
        }
    }


    public void updateRemainingTiles(TilePosition tilePosition) {
        largeBuildTiles.removeIf(tile -> tile.equals(tilePosition));
        mediumBuildTiles.removeIf(tile -> tile.equals(tilePosition));
    }

    public HashSet<TilePosition> getMediumBuildTiles() {
        return mediumBuildTiles;
    }

    public HashSet<TilePosition> getLargeBuildTiles() {
        return largeBuildTiles;
    }

    public TilePosition getBunkerTile() {
        return bunkerTile;
    }

    public void onFrame() {
        painters.paintPaintBunkerTile(bunkerTile);
        painters.paintAvailableBuildTiles(largeBuildTiles, 0, "Production");
        painters.paintAvailableBuildTiles(mediumBuildTiles, 15, "Medium");
        painters.paintTileZone(mineralExlusionTiles, Color.Cyan);
//        painters.paintTileZone(geyserExlusionTiles, Color.Green);
//        painters.paintTileZone(frontBaseTiles, Color.Purple);
//        painters.paintTileZone(backBaseTiles, Color.Orange);
        painters.paintTileZone(ccExclusionTiles, Color.Red);
        painters.paintLargeBuildTiles(largeBuildTiles);
        painters.paintMediumBuildTiles(mediumBuildTiles);

    }

    public void onUnitComplete(Unit unit) {
        if(!unit.getType().isBuilding()) {
            return;
        }

        for(TilePosition tilePosition : largeBuildTiles) {
            if(unit.getTilePosition().equals(tilePosition)) {
                largeBuildTiles.remove(tilePosition);
                break;
            }
        }

        for(TilePosition tilePosition : mediumBuildTiles) {
            if(unit.getTilePosition().equals(tilePosition)) {
                mediumBuildTiles.remove(tilePosition);
                break;
            }
        }
    }
}
