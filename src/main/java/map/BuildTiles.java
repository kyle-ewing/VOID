package map;

import bwapi.Game;
import bwapi.TilePosition;
import bwapi.UnitType;
import bwem.BWEM;
import debug.Painters;
import information.BaseInfo;

import java.util.ArrayList;
import java.util.List;

public class BuildTiles {
    private Game game;
    private BWEM bwem;
    private BaseInfo baseInfo;
    private Painters painters;
    private TilePositionValidator tilePositionValidator;
    private ArrayList<TilePosition> buildTiles = new ArrayList<>();
    private int searchOffSet = 0;

    public BuildTiles(Game game, BWEM bwem, BaseInfo baseInfo) {
        this.game = game;
        this.bwem = bwem;
        this.baseInfo = baseInfo;

        tilePositionValidator = new TilePositionValidator(game);
        painters = new Painters(game, bwem);

        init();
    }

    private void init() {
        generateBuildTiles();
    }

//    private void generateBuildTiles() {
//        TilePosition testTile = game.getBuildLocation(UnitType.Terran_Barracks, baseInfo.getStartingBase().getLocation(), 32 + searchOffSet);
//
//        if(verifyTileLine(testTile, UnitType.Terran_Barracks)) {
//            for(int y = 0; y < 4; y++) {
//                    buildTiles.add(testTile);
//                    testTile = new TilePosition(testTile.getX(), testTile.getY() + UnitType.Terran_Barracks.tileSize().getY());
//            }
//        }
//        else {
//            searchOffSet += 3;
//            generateBuildTiles();
//
//        }
//    }

    private void generateBuildTiles() {
        for(TilePosition tilePosition : baseInfo.getBaseTiles()) {
            if(verifyTileLine(tilePosition, UnitType.Terran_Barracks)) {
                buildTiles.add(tilePosition);
            }
        }
    }

    private boolean verifyTileLine(TilePosition tilePosition, UnitType unitType) {
        int lineCount = 0;

        while(lineCount < 4) {
            if(!tilePositionValidator.isBuildable(tilePosition, unitType)) {
                return false;
            }
            tilePosition = new TilePosition(tilePosition.getX(), tilePosition.getY() + unitType.tileSize().getY());
            lineCount++;
        }
        return true;
    }

    public void onFrame() {
       // painters.paintBuildTiles(buildTiles);
    }
}
