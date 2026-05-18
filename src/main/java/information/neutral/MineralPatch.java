package information.neutral;

import bwapi.Position;
import bwapi.Unit;
import bwem.Mineral;

public class MineralPatch {
    private Mineral mineral;
    private Position position;
    private int lastKnownResources;
    private boolean destroyed = false;

    public MineralPatch(Mineral mineral) {
        this.mineral = mineral;
        this.position = mineral.getUnit().getPosition();
        int initial = mineral.getInitialAmount();
        if (initial > 0) {
            this.lastKnownResources = initial;
        }
        else {
            this.lastKnownResources = 1500;
        }
    }

    public Position getPosition() {
        return position;
    }

    public int getResources() {
        if (destroyed) {
            return 0;
        }
        Unit unit = mineral.getUnit();
        if (unit.isVisible()) {
            lastKnownResources = unit.getResources();
        }
        return lastKnownResources;
    }

    public void markDestroyed() {
        destroyed = true;
        lastKnownResources = 0;
    }

    public Unit getUnit() {
        return mineral.getUnit();
    }

    public Mineral getMineral() {
        return mineral;
    }
}
