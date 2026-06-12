package map.bwemwrappers;

import java.util.ArrayList;
import java.util.List;

import bwapi.Position;
import bwapi.TilePosition;

public class Base {
    private bwem.Base bwemBase;
    private TilePosition location;
    private Position center;
    private boolean startingLocation;
    private Area area;
    private ArrayList<Mineral> minerals = new ArrayList<>();
    private ArrayList<Geyser> geysers = new ArrayList<>();
    private ArrayList<Mineral> blockingMinerals = new ArrayList<>();
    private Ownership ownership = Ownership.NEUTRAL;
    private boolean natural = false;
    private boolean minOnly = false;
    private int livePatchCount = 0;
    private List<Position> pathFromMain = new ArrayList<>();
    private int groundDistanceFromMain = Integer.MAX_VALUE;

    public Base(bwem.Base bwemBase) {
        this.bwemBase = bwemBase;
        location = bwemBase.getLocation();
        center = bwemBase.getCenter();
        startingLocation = bwemBase.isStartingLocation();
    }

    public void decrementLivePatchCount() {
        if (livePatchCount > 0) {
            livePatchCount--;
        }
    }

    public TilePosition getLocation() {
        return location;
    }

    public Position getCenter() {
        return center;
    }

    public boolean isStartingLocation() {
        return startingLocation;
    }

    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        this.area = area;
    }

    public ArrayList<Mineral> getMinerals() {
        return minerals;
    }

    public ArrayList<Geyser> getGeysers() {
        return geysers;
    }

    public ArrayList<Mineral> getBlockingMinerals() {
        return blockingMinerals;
    }

    public Ownership getOwnership() {
        return ownership;
    }

    public void setOwnership(Ownership ownership) {
        this.ownership = ownership;
    }

    public boolean isNatural() {
        return natural;
    }

    public void setNatural(boolean natural) {
        this.natural = natural;
    }

    public boolean isMinOnly() {
        return minOnly;
    }

    public void setMinOnly(boolean minOnly) {
        this.minOnly = minOnly;
    }

    public int getLivePatchCount() {
        return livePatchCount;
    }

    public void setLivePatchCount(int livePatchCount) {
        this.livePatchCount = livePatchCount;
    }

    public List<Position> getPathFromMain() {
        return pathFromMain;
    }

    public void setPathFromMain(List<Position> pathFromMain) {
        this.pathFromMain = pathFromMain;
    }

    public int getGroundDistanceFromMain() {
        return groundDistanceFromMain;
    }

    public void setGroundDistanceFromMain(int groundDistanceFromMain) {
        this.groundDistanceFromMain = groundDistanceFromMain;
    }

    public bwem.Base getBwemBase() {
        return bwemBase;
    }
}
