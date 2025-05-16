package map;

import bwapi.Game;
import bwapi.TilePosition;
import bwapi.UnitType;

public class TilePositionValidator {
    private final Game game;

    public TilePositionValidator(Game game) {
        this.game = game;
    }

    public boolean isWithinMap(TilePosition tp) {
        if (tp == null) {
            return false;
        }
        int mapWidth = game.mapWidth();
        int mapHeight = game.mapHeight();
        return tp.getX() >= 0 && tp.getX() < mapWidth &&
                tp.getY() >= 0 && tp.getY() < mapHeight;
    }

    public boolean isWalkable(TilePosition tp) {
        if (!isWithinMap(tp)) return false;

        int startWalkX = tp.getX() * 4;
        int startWalkY = tp.getY() * 4;

        for (int dx = 0; dx < 4; dx++) {
            for (int dy = 0; dy < 4; dy++) {
                int walkX = startWalkX + dx;
                int walkY = startWalkY + dy;
                if (!game.isWalkable(walkX, walkY)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isBuildable(TilePosition tp) {
        if (!isWithinMap(tp)) {
            return false;
        }
        return game.isBuildable(tp);
    }

    public boolean isBuildable(TilePosition tp, UnitType buildingType) {
        if (!isWithinMap(tp)) {
            return false;
        }

        for (int x = 0; x < buildingType.tileWidth(); x++) {
            for (int y = 0; y < buildingType.tileHeight(); y++) {
                TilePosition buildTile = new TilePosition(tp.getX() + x, tp.getY() + y);
                if (!isWithinMap(buildTile) || !game.isBuildable(buildTile, true)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isValid(TilePosition tp) {
        return isWalkable(tp);
    }
}