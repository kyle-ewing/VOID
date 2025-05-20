package map;

import bwapi.Game;
import bwapi.TilePosition;
import bwapi.UnitType;
import bwem.BWEM;
import bwem.Mineral;
import debug.Painters;
import information.BaseInfo;

import java.util.ArrayList;
import java.util.HashSet;

public class BuildTiles {
    private Game game;
    private BWEM bwem;
    private BaseInfo baseInfo;
    private TilePositionValidator tilePositionValidator;
    private Painters painters;
    private HashSet<TilePosition> mediumBuildTiles = new HashSet<>();
    private HashSet<TilePosition> largeBuildTiles = new HashSet<>();
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
        generateBunkerTiles();
        generateMediumTiles();
        generateLargeTiles();

    }

    private void generateLargeTiles() {
        for (TilePosition tilePosition : baseInfo.getBaseTiles()) {
            if (largeBuildTiles.size() >= 12) {
                break;
            }

            if (verifyTileLine(tilePosition, UnitType.Terran_Engineering_Bay)) {
                for (int line = 0; line < 3; line++) {
                    TilePosition currentTile = new TilePosition(tilePosition.getX(), tilePosition.getY() + (line * UnitType.Terran_Engineering_Bay.tileHeight()));

                    largeBuildTiles.add(currentTile);
                }
            }
        }
    }

    private void generateMediumTiles() {


        for (TilePosition tilePosition : baseInfo.getBaseTiles()) {
            if(mediumBuildTiles.size() >= 24) {
                break;
            }

            TilePosition currentTile = tilePosition;
            boolean validTileLine = true;

            for (int i = 0; i < 2; i++) {
                if (!verifyMediumGrid(currentTile, UnitType.Terran_Supply_Depot) || intersectsExistingBuildTiles(currentTile, UnitType.Terran_Supply_Depot)) {
                    validTileLine = false;
                    break;
                }
                currentTile = new TilePosition(currentTile.getX(), currentTile.getY() + UnitType.Terran_Supply_Depot.tileHeight());
            }

            if (validTileLine) {
                currentTile = tilePosition;
                for (int i = 0; i < 2; i++) {
                    mediumBuildTiles.add(currentTile);
                    currentTile = new TilePosition(currentTile.getX(), currentTile.getY() + UnitType.Terran_Supply_Depot.tileHeight());
                }
            }
        }
    }

    private void generateBunkerTiles() {
        int closestDistance = Integer.MAX_VALUE;

        for (TilePosition tilePosition : baseInfo.getBaseTiles()) {
            if(!tilePositionValidator.isBuildable(tilePosition, UnitType.Terran_Bunker)) {
                continue;
            }
            int distance = baseInfo.getMainChoke().getCenter().getApproxDistance(tilePosition.toWalkPosition());
            if(distance < closestDistance) {
                closestDistance = distance;
                bunkerTile = tilePosition;
            }
        }
    }

    private boolean verifyTileLine(TilePosition startTile, UnitType unitType) {
        int buildingWidth = unitType.tileWidth();
        int buildingHeight = unitType.tileHeight();

        for (int line = 0; line < 3; line++) {
            TilePosition currentTile = new TilePosition(startTile.getX(), startTile.getY() + (line * buildingHeight));

            for (int x = 0; x < buildingWidth; x++) {
                for (int y = 0; y < buildingHeight; y++) {
                    TilePosition checkTile = new TilePosition(currentTile.getX() + x, currentTile.getY() + y);

                    if (!tilePositionValidator.isBuildable(checkTile) ||
                            intersectsExistingBuildTiles(checkTile, unitType)) {
                        return false;
                    }
                }
            }

            for (int bufferX = 0; bufferX < 3; bufferX++) {
                for (int y = 0; y < buildingHeight; y++) {
                    TilePosition bufferTile = new TilePosition(currentTile.getX() + buildingWidth + bufferX, currentTile.getY() + y);

                    if (!tilePositionValidator.isWithinMap(bufferTile) ||
                            intersectsExistingBuildTiles(bufferTile, unitType)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean verifyMediumGrid(TilePosition startTile, UnitType unitType) {
        TilePosition currentTile = startTile;
        TilePosition edgeCaseTile1 = new TilePosition(currentTile.getX() -1, currentTile.getY() + unitType.tileHeight());
        TilePosition edgeCaseTile2 = new TilePosition(currentTile.getX() + unitType.tileWidth() +1, currentTile.getY() + unitType.tileHeight());

        for (int i = 0; i < 2; i++) {
            if (!tilePositionValidator.isBuildable(currentTile, unitType)) {
                return false;
            }

            if(!tilePositionValidator.isBuildable(edgeCaseTile1) && !tilePositionValidator.isBuildable(edgeCaseTile2)) {
                return false;
            }

            currentTile = new TilePosition(currentTile.getX(), currentTile.getY() + unitType.tileHeight());;
        }
        return true;
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

        if (inCCBuffer) {
            return true;
        }

        for (Mineral mineral : baseInfo.getStartingMinerals()) {
            TilePosition mineralPos = mineral.getUnit().getTilePosition();
            int mineralX = mineralPos.getX();
            int mineralY = mineralPos.getY();

            boolean inMineralBuffer = (newX >= mineralX - 2 && newX < mineralX + 1 + 2) && (newY >= mineralY - 2 && newY < mineralY + 1 + 2);
            if (inMineralBuffer) {
                return true;
            }
        }

        if (bunkerTile != null) {
            int bunkerXStart = bunkerTile.getX();
            int bunkerYStart = bunkerTile.getY();
            int bunkerXEnd = bunkerTile.getX() + UnitType.Terran_Bunker.tileWidth();
            int bunkerYEnd = bunkerTile.getY() + UnitType.Terran_Bunker.tileHeight();

            for (int x = newX; x < newX + typeWidth; x++) {
                for (int y = newY; y < newY + typeHeight; y++) {
                    if (x >= bunkerXStart && x < bunkerXEnd && y >= bunkerYStart && y < bunkerYEnd) {
                        return true;
                    }
                }
            }
        }

        for (TilePosition existingTile : largeBuildTiles) {
            int existingXStart = existingTile.getX();
            int existingYStart = existingTile.getY();
            int existingXEnd = existingXStart + UnitType.Terran_Engineering_Bay.tileWidth();
            int existingYEnd = existingYStart + UnitType.Terran_Engineering_Bay.tileHeight();

            for (int x = newX; x < newX + typeWidth; x++) {
                for (int y = newY; y < newY + typeHeight; y++) {
                    if (x >= existingXStart && x < existingXEnd && y >= existingYStart && y < existingYEnd) {
                        return true;
                    }
                }
            }

            int bufferXStart = existingXEnd;
            int bufferXEnd = existingXEnd + 3;
            int bufferYStart = existingYStart;
            int bufferYEnd = existingYEnd;

            for (int x = newX; x < newX + typeWidth; x++) {
                for (int y = newY; y < newY + typeHeight; y++) {
                    if (x >= bufferXStart && x < bufferXEnd && y >= bufferYStart && y < bufferYEnd) {
                        return true;
                    }
                }
            }
        }

        for (TilePosition existingTile : mediumBuildTiles) {
            int existingXStart = existingTile.getX();
            int existingYStart = existingTile.getY();
            int existingXEnd = existingTile.getX() + UnitType.Terran_Supply_Depot.tileWidth();
            int existingYEnd = existingTile.getY() + UnitType.Terran_Supply_Depot.tileHeight();

            for (int x = newX; x < newX + typeWidth; x++) {
                for (int y = newY; y < newY + typeHeight; y++) {
                    if (x >= existingXStart && x < existingXEnd && y >= existingYStart && y < existingYEnd) {
                        return true;
                    }
                }
            }
        }
        return false;
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

    public void onFrame() {
        painters.paintPaintBunkerTile(bunkerTile);
        painters.paintLargeBuildTiles(largeBuildTiles);
        painters.paintMediumBuildTiles(mediumBuildTiles);
        painters.paintAvailableBuildTiles(largeBuildTiles, 0, "Production");
        painters.paintAvailableBuildTiles(mediumBuildTiles, 15, "Medium");

    }
}
