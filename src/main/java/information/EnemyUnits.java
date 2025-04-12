package information;

import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;

public class EnemyUnits {
    private int enemyID;
    private Unit enemyUnit;
    private UnitType enemyType;
    private Position enemyPosition;

    public EnemyUnits(int enemyID, Unit enemyUnit) {
        this.enemyID = enemyID;
        this.enemyUnit = enemyUnit;
        this.enemyType = enemyUnit.getType();
        this.enemyPosition = enemyUnit.getPosition();
    }

    public int getEnemyID() {
        return enemyID;
    }

    public UnitType getEnemyType() {
        return enemyType;
    }

    public void setEnemyType(UnitType enemyType) {
        this.enemyType = enemyType;
    }

    public Position getEnemyPosition() {
        return enemyPosition;
    }

    public void setEnemyPosition(Position enemyPosition) {
        this.enemyPosition = enemyPosition;
    }

    public Unit getEnemyUnit() {
        return enemyUnit;
    }
}
