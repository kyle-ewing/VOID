package map.bwemwrappers;

import bwapi.Position;
import bwapi.Unit;

public class Mineral extends Neutral {
    private bwem.Mineral bwemMineral;
    private int initialAmount;
    private int lastKnownResources;
    private boolean destroyed = false;

    public Mineral(bwem.Mineral bwemMineral) {
        super(bwemMineral);
        this.bwemMineral = bwemMineral;
        initialAmount = bwemMineral.getInitialAmount();
        if (initialAmount > 0) {
            lastKnownResources = initialAmount;
        }
        else {
            lastKnownResources = 1500;
        }
    }

    public Position getPosition() {
        return getCenter();
    }

    public int getResources() {
        if (destroyed) {
            return 0;
        }
        Unit unit = getUnit();
        if (unit.isVisible()) {
            lastKnownResources = unit.getResources();
        }
        return lastKnownResources;
    }

    public void markDestroyed() {
        destroyed = true;
        lastKnownResources = 0;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public int getInitialAmount() {
        return initialAmount;
    }

    public bwem.Mineral getBwemMineral() {
        return bwemMineral;
    }
}
