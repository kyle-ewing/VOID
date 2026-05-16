package unitgroups.units;

import bwapi.Game;
import bwapi.Position;
import bwapi.Unit;

public class Building extends CombatUnits {
    private boolean inWall = false;

        public Building(Game game, Unit unit, UnitStatus unitStatus) {
            super(game, unit, unitStatus, false);
    }

    public void liftedBuildings(Position bunkerPosition, Position naturalBaseCenter) {
        if (!notNeeded || !unit.isLifted()) {
            return;
        }

        if (unit.isUnderAttack()) {
            unit.move(bunkerPosition);
            return;
        }

        double dx = bunkerPosition.x - naturalBaseCenter.x;
        double dy = bunkerPosition.y - naturalBaseCenter.y;
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length == 0) {
            return;
        }

        Position target = new Position(
                (int) (bunkerPosition.x + (dx / length) * 160),
                (int) (bunkerPosition.y + (dy / length) * 160));

        unit.move(target);
    }

    public boolean isInWall() {
        return inWall;
    }

    public void setInWall(boolean inWall) {
        this.inWall = inWall;
    }

    
}
