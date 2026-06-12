package map.bwemwrappers;

import java.util.ArrayList;
import java.util.HashSet;

import bwapi.Position;
import bwapi.TilePosition;

public class Area {
    private bwem.Area bwemArea;
    private int id;
    private TilePosition topLeft;
    private TilePosition bottomRight;
    private Position top;
    private int highestAltitude;
    private int lowGroundPercentage;
    private int highGroundPercentage;
    private int veryHighGroundPercentage;
    private GroundHeight groundHeight;
    private HashSet<TilePosition> tiles = new HashSet<>();
    private ArrayList<ChokePoint> chokes = new ArrayList<>();
    private ArrayList<Area> neighbors = new ArrayList<>();
    private ArrayList<Base> bases = new ArrayList<>();
    private ArrayList<Mineral> minerals = new ArrayList<>();
    private ArrayList<Geyser> geysers = new ArrayList<>();
    private boolean synthetic = false;
    private boolean startingArea = false;
    private boolean naturalArea = false;
    private boolean occupied = false;

    public Area(bwem.Area bwemArea) {
        this.bwemArea = bwemArea;
        id = bwemArea.getId().intValue();
        topLeft = bwemArea.getTopLeft();
        bottomRight = bwemArea.getBottomRight();
        top = bwemArea.getTop().toPosition();
        highestAltitude = bwemArea.getHighestAltitude().intValue();
        lowGroundPercentage = bwemArea.getLowGroundPercentage();
        highGroundPercentage = bwemArea.getHighGroundPercentage();
        veryHighGroundPercentage = bwemArea.getVeryHighGroundPercentage();
    }

    // Creates a synthetic area
    public Area(int id, bwem.Area parentBwemArea, GroundHeight groundHeight, HashSet<TilePosition> tiles) {
        this.id = id;
        this.bwemArea = parentBwemArea;
        this.groundHeight = groundHeight;
        this.tiles = tiles;
        synthetic = true;

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        long sumX = 0;
        long sumY = 0;

        for (TilePosition tile : tiles) {
            minX = Math.min(minX, tile.getX());
            minY = Math.min(minY, tile.getY());
            maxX = Math.max(maxX, tile.getX());
            maxY = Math.max(maxY, tile.getY());
            sumX += tile.getX();
            sumY += tile.getY();
        }

        topLeft = new TilePosition(minX, minY);
        bottomRight = new TilePosition(maxX, maxY);

        double centroidX = sumX / (double) tiles.size();
        double centroidY = sumY / (double) tiles.size();
        TilePosition centroidTile = null;
        double closestDistance = Double.MAX_VALUE;

        for (TilePosition tile : tiles) {
            double offsetX = tile.getX() - centroidX;
            double offsetY = tile.getY() - centroidY;
            double distance = offsetX * offsetX + offsetY * offsetY;

            if (distance < closestDistance) {
                closestDistance = distance;
                centroidTile = tile;
            }
        }

        top = centroidTile.toPosition();

        if (parentBwemArea != null) {
            highestAltitude = parentBwemArea.getHighestAltitude().intValue();
        }

        if (groundHeight == GroundHeight.LOW_GROUND) {
            lowGroundPercentage = 100;
        }
        else if (groundHeight == GroundHeight.HIGH_GROUND) {
            highGroundPercentage = 100;
        }
        else {
            veryHighGroundPercentage = 100;
        }
    }

    public int getId() {
        return id;
    }

    public TilePosition getTopLeft() {
        return topLeft;
    }

    public TilePosition getBottomRight() {
        return bottomRight;
    }

    public Position getTop() {
        return top;
    }

    public int getHighestAltitude() {
        return highestAltitude;
    }

    public int getLowGroundPercentage() {
        return lowGroundPercentage;
    }

    public int getHighGroundPercentage() {
        return highGroundPercentage;
    }

    public int getVeryHighGroundPercentage() {
        return veryHighGroundPercentage;
    }

    public GroundHeight getGroundHeight() {
        return groundHeight;
    }

    public void setGroundHeight(GroundHeight groundHeight) {
        this.groundHeight = groundHeight;
    }

    public HashSet<TilePosition> getTiles() {
        return tiles;
    }

    public ArrayList<ChokePoint> getChokes() {
        return chokes;
    }

    public ArrayList<Area> getNeighbors() {
        return neighbors;
    }

    public ArrayList<Base> getBases() {
        return bases;
    }

    public ArrayList<Mineral> getMinerals() {
        return minerals;
    }

    public ArrayList<Geyser> getGeysers() {
        return geysers;
    }

    public boolean isSynthetic() {
        return synthetic;
    }

    public boolean isStartingArea() {
        return startingArea;
    }

    public void setStartingArea(boolean startingArea) {
        this.startingArea = startingArea;
    }

    public boolean isNaturalArea() {
        return naturalArea;
    }

    public void setNaturalArea(boolean naturalArea) {
        this.naturalArea = naturalArea;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    public bwem.Area getBwemArea() {
        return bwemArea;
    }
}
