package map.bwemwrappers;

import bwapi.Unit;

public class Geyser extends Neutral {
    private bwem.Geyser bwemGeyser;
    private int initialAmount;
    private int lastKnownResources;
    private Base base;
    private boolean taken = false;
    private Unit refinery;

    public Geyser(bwem.Geyser bwemGeyser) {
        super(bwemGeyser);
        this.bwemGeyser = bwemGeyser;
        initialAmount = bwemGeyser.getInitialAmount();
        lastKnownResources = initialAmount;
    }

    public int getResources() {
        Unit unit = getUnit();
        if (unit.isVisible()) {
            lastKnownResources = unit.getResources();
        }
        return lastKnownResources;
    }

    public int getInitialAmount() {
        return initialAmount;
    }

    public Base getBase() {
        return base;
    }

    public void setBase(Base base) {
        this.base = base;
    }

    public boolean isTaken() {
        return taken;
    }

    public void setTaken(boolean taken) {
        this.taken = taken;
    }

    public Unit getRefinery() {
        return refinery;
    }

    public void setRefinery(Unit refinery) {
        this.refinery = refinery;
    }

    public bwem.Geyser getBwemGeyser() {
        return bwemGeyser;
    }
}
