package util;

import bwapi.TilePosition;

public class PositionInterpolator {
    public static TilePosition interpolate(TilePosition start, TilePosition target, double percent) {
        int dx = target.getX() - start.getX();
        int dy = target.getY() - start.getY();
        int newX = start.getX() + (int)Math.round(dx * percent);
        int newY = start.getY() + (int)Math.round(dy * percent);
        return new TilePosition(newX, newY);
    }
}
