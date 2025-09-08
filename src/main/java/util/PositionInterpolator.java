package util;

import bwapi.Position;
import bwapi.TilePosition;

import java.util.List;

public class PositionInterpolator {
    public static TilePosition interpolate(TilePosition start, TilePosition target, double percent) {
        int dx = target.getX() - start.getX();
        int dy = target.getY() - start.getY();
        int newX = start.getX() + (int)Math.round(dx * percent);
        int newY = start.getY() + (int)Math.round(dy * percent);
        return new TilePosition(newX, newY);
    }

    public static Position interpolate(List<Position> path, double percent) {
        if(path == null || path.size() < 2) {
            return null;
        }

        double totalLength = 0;

        for(int i = 1; i < path.size(); i++) {
            totalLength += path.get(i - 1).getDistance(path.get(i));
        }

        double targetDistance = totalLength * percent;
        double accumulated = 0.0;

        for(int i = 1; i < path.size(); i++) {
            Position prev = path.get(i - 1);
            Position curr = path.get(i);
            double segment = prev.getDistance(curr);

            if(accumulated + segment >= targetDistance) {
                double remaining = targetDistance - accumulated;
                double segmentPercent = segment == 0 ? 0 : remaining / segment;
                int x = (int) Math.round(prev.getX() + (curr.getX() - prev.getX()) * segmentPercent);
                int y = (int) Math.round(prev.getY() + (curr.getY() - prev.getY()) * segmentPercent);
                return new Position(x, y);
            }
            accumulated += segment;
        }

        return path.get(path.size() - 1);
    }
}
