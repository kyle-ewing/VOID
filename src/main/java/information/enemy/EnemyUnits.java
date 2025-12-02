package information.enemy;

import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;

public class EnemyUnits {
    private int enemyID;
    private int irradiateTimer = 0;
    private Unit enemyUnit;
    private UnitType enemyType;
    private Position enemyPosition;
    private TilePosition enemyTilePosition;
    private boolean wasBurrowed = false;

    public EnemyUnits(int enemyID, Unit enemyUnit) {
        this.enemyID = enemyID;
        this.enemyUnit = enemyUnit;
        this.enemyType = enemyUnit.getType();
        this.enemyPosition = enemyUnit.getPosition();
        this.enemyTilePosition = enemyUnit.getTilePosition();
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

    public TilePosition getEnemyTilePosition() {
        return enemyTilePosition;
    }

    public void setEnemyTilePosition(TilePosition enemyTilePosition) {
        this.enemyTilePosition = enemyTilePosition;
    }

    public Unit getEnemyUnit() {
        return enemyUnit;
    }

    public boolean wasBurrowed() {
        return wasBurrowed;
    }

    public void setBurrowed(boolean wasBurrowed) {
        this.wasBurrowed = wasBurrowed;
    }

    public int getIrradiateTimer() {
        return irradiateTimer;
    }

    public void setIrradiateTimer() {
        this.irradiateTimer = irradiateTimer + 1;
    }
}
