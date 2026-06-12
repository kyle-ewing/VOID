package map.bwemwrappers;

import java.util.ArrayList;
import java.util.List;

import bwapi.Position;
import bwapi.WalkPosition;

public class ChokePoint {
    private bwem.ChokePoint bwemChoke;
    private Position center;
    private Area firstArea;
    private Area secondArea;
    private List<WalkPosition> geometry = new ArrayList<>();
    private boolean blocked;
    private Neutral blockingNeutral;
    private boolean pseudo;
    private boolean synthetic = false;
    private boolean heightTransition = false;
    private int width = 0;
    private Position end1;
    private Position end2;

    public ChokePoint(bwem.ChokePoint bwemChoke) {
        this.bwemChoke = bwemChoke;
        center = bwemChoke.getCenter().toPosition();
        geometry = new ArrayList<>(bwemChoke.getGeometry());
        blocked = bwemChoke.isBlocked();
        pseudo = bwemChoke.isPseudo();
    }

    public Area getOtherArea(Area area) {
        if (area == firstArea) {
            return secondArea;
        }
        if (area == secondArea) {
            return firstArea;
        }
        return null;
    }

    public Position getCenter() {
        return center;
    }

    public Area getFirstArea() {
        return firstArea;
    }

    public void setFirstArea(Area firstArea) {
        this.firstArea = firstArea;
    }

    public Area getSecondArea() {
        return secondArea;
    }

    public void setSecondArea(Area secondArea) {
        this.secondArea = secondArea;
    }

    public List<WalkPosition> getGeometry() {
        return geometry;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public Neutral getBlockingNeutral() {
        return blockingNeutral;
    }

    public void setBlockingNeutral(Neutral blockingNeutral) {
        this.blockingNeutral = blockingNeutral;
    }

    public boolean isPseudo() {
        return pseudo;
    }

    public boolean isSynthetic() {
        return synthetic;
    }

    public boolean isHeightTransition() {
        return heightTransition;
    }

    public void setHeightTransition(boolean heightTransition) {
        this.heightTransition = heightTransition;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public Position getEnd1() {
        return end1;
    }

    public void setEnd1(Position end1) {
        this.end1 = end1;
    }

    public Position getEnd2() {
        return end2;
    }

    public void setEnd2(Position end2) {
        this.end2 = end2;
    }

    public bwem.ChokePoint getBwemChoke() {
        return bwemChoke;
    }
}
