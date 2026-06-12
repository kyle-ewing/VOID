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
