package map.bwemwrappers;

import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;

public abstract class Neutral {
    private Unit unit;
    private Position center;
    private TilePosition topLeft;
    private TilePosition tileSize;
    private boolean blocking;

    protected Neutral(bwem.Neutral bwemNeutral) {
        unit = bwemNeutral.getUnit();
        center = bwemNeutral.getCenter();
        topLeft = bwemNeutral.getTopLeft();
        tileSize = bwemNeutral.getSize();
        blocking = bwemNeutral.isBlocking();
    }

    public Unit getUnit() {
        return unit;
    }

    public Position getCenter() {
        return center;
    }

    public TilePosition getTopLeft() {
        return topLeft;
    }

    public TilePosition getTileSize() {
        return tileSize;
    }

    public boolean isBlocking() {
        return blocking;
    }
}
