package map;

import bwapi.Game;
import bwapi.TilePosition;
import bwapi.UnitType;
import bwem.BWEM;
import bwem.Mineral;
import debug.Painters;
import information.BaseInfo;

import java.util.ArrayList;

public class BuildTiles {
    private Game game;
    private BWEM bwem;
    private BaseInfo baseInfo;
    private TilePositionValidator tilePositionValidator;
    private Painters painters;
    private ArrayList<TilePosition> mediumBuildTiles = new ArrayList<>();
    private ArrayList<TilePosition> largeBuildTiles = new ArrayList<>();

    public BuildTiles(Game game, BWEM bwem, BaseInfo baseInfo) {
        this.game = game;
        this.bwem = bwem;
        this.baseInfo = baseInfo;

        tilePositionValidator = new TilePositionValidator(game);
        painters = new Painters(game, bwem);

        generateBuildTiles();
    }

    private void generateBuildTiles() {
        generateLargeTiles();
        generateMediumTiles();
    }

    private void generateLargeTiles() {
        for (TilePosition tilePosition : baseInfo.getBaseTiles()) {
            //Cap large build size at 12
            if (largeBuildTiles.size() >= 10) {
                break;
            }

            if (verifyTileLine(tilePosition, UnitType.Terran_Engineering_Bay) && !intersectsExistingBuildTiles(tilePosition, UnitType.Terran_Engineering_Bay)) {
                TilePosition currentTile = tilePosition;
                for (int i = 0; i < 5; i++) {
                    largeBuildTiles.add(currentTile);
                    currentTile = new TilePosition(currentTile.getX(), currentTile.getY() + UnitType.Terran_Engineering_Bay.tileHeight());
                }
            }
        }
    }

    private void generateMediumTiles() {
        for (TilePosition tilePosition : baseInfo.getBaseTiles()) {
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
                    System.out.println(UnitType.Terran_Supply_Depot.tileHeight());
                }
            }
        }
    }

    private boolean verifyMediumGrid(TilePosition startTile, UnitType unitType) {
        TilePosition currentTile = startTile;

        for (int i = 0; i < 2; i++) {
            if (!tilePositionValidator.isBuildable(currentTile, unitType)) {
                return false;
            }

            currentTile = new TilePosition(currentTile.getX(), currentTile.getY() + unitType.tileHeight());;
        }
        return true;
    }

    private boolean verifyTileLine(TilePosition startTile, UnitType unitType) {
        TilePosition currentTile = startTile;

        for (int i = 0; i < 5; i++) {
            if (!tilePositionValidator.isBuildable(currentTile, unitType)) {
                return false;
            }

            TilePosition nextTile = new TilePosition(currentTile.getX(), currentTile.getY() + unitType.tileHeight());

            if (i < 4) {
                if (!tilePositionValidator.isWithinMap(nextTile)) {
                    return false;
                }
                currentTile = nextTile;
            }
        }
        return true;
    }

    private boolean intersectsExistingBuildTiles(TilePosition newTilePosition, UnitType unitType) {
        int newX = newTilePosition.getX();
        int newY = newTilePosition.getY();
        int typeWidth = unitType.tileWidth();
        int typeHeight = unitType.tileHeight() * 4;

        // No build buffer around starting CC
        TilePosition ccPosition = baseInfo.getStartingBase().getLocation();
        int ccX = ccPosition.getX();
        int ccY = ccPosition.getY();
        int ccWidth = UnitType.Terran_Command_Center.tileWidth();
        int ccHeight = UnitType.Terran_Command_Center.tileHeight();

        boolean inCCBuffer = (newX >= ccX - 2 && newX < ccX + ccWidth + 2) && (newY >= ccY - 2 && newY < ccY + ccHeight + 2);

        if (inCCBuffer) {
            return true;
        }

        // No build buffer around mineral patches
        for (Mineral mineral : baseInfo.getStartingMinerals()) {
            TilePosition mineralPos = mineral.getUnit().getTilePosition();
            int mineralX = mineralPos.getX();
            int mineralY = mineralPos.getY();

            boolean inMineralBuffer = (newX >= mineralX - 2 && newX < mineralX + 1 + 2) && (newY >= mineralY - 2 && newY < mineralY + 1 + 2);
            if (inMineralBuffer) {
                return true;
            }
        }

        for (TilePosition existingTile : largeBuildTiles) {
            int existingX = existingTile.getX();
            int existingY = existingTile.getY();

            int expandedNewX = newX - 3;
            int expandedWidth = typeWidth + 4;

            boolean xOverlap = (expandedNewX < existingX + typeWidth) && (existingX < expandedNewX + expandedWidth);
            boolean yOverlap = (newY < existingY + typeHeight) && (existingY < newY + typeHeight);

            if (xOverlap && yOverlap) {
                return true;
            }
        }
        
        for (TilePosition existingTile : mediumBuildTiles) {
            int existingX = existingTile.getX();
            int existingY = existingTile.getY();

            boolean xOverlap = (newX < existingX + typeWidth) && (existingX < newX + typeWidth);
            boolean yOverlap = (newY < existingY + typeHeight) && (existingY < newY + typeHeight);

            if (xOverlap && yOverlap) {
                return true;
            }
        }

        if (unitType == UnitType.Terran_Engineering_Bay) {
            return !tilePositionValidator.isWithinMap(new TilePosition(newX - 2, newY)) || !tilePositionValidator.isWithinMap(new TilePosition(newX + typeWidth + 1, newY));
        }

        return false;
    }

    public ArrayList<TilePosition> getMediumBuildTiles() {
        return mediumBuildTiles;
    }

    public ArrayList<TilePosition> getLargeBuildTiles() {
        return largeBuildTiles;
    }

    public void onFrame() {
        painters.paintLargeBuildTiles(largeBuildTiles);
        painters.paintMediumBuildTiles(mediumBuildTiles);
        painters.paintAvailableBuildTiles(largeBuildTiles, 0, "Production");
        painters.paintAvailableBuildTiles(mediumBuildTiles, 15, "Medium");
    }
}
