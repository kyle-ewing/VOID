package unitgroups.units;

import bwapi.*;
import information.BaseInfo;
import information.enemy.EnemyInformation;

import java.util.HashSet;
import java.util.Random;

public class SiegeTank extends CombatUnits {
    private EnemyInformation enemyInformation;
    private BaseInfo baseInfo;
    private UnitType defaultMode = UnitType.Terran_Siege_Tank_Tank_Mode;
    private HashSet<TilePosition> mainEdgeTiles = new HashSet<>();
    private HashSet<TilePosition> combinedTankTiles = new HashSet<>();
    private HashSet<TilePosition> backupSiegeTiles = new HashSet<>();
    private TilePosition siegeTile = null;
    private boolean foundSiegeTile = false;

    private static final int SIEGE_RANGE = 384;

    public SiegeTank(Game game, EnemyInformation enemyInformation, Unit unit) {
        super(game, unit);
        this.enemyInformation = enemyInformation;
        baseInfo = enemyInformation.getBaseInfo();
        mainEdgeTiles = baseInfo.getMainCliffEdge();
        combinedTankTiles = baseInfo.getCombinedTankTiles();
        backupSiegeTiles = baseInfo.getBackupMainSiegeTiles();
    }

    @Override
    public void attack() {
        if(enemyUnit == null) {
            return;
        }

        siegeLogic();
        unit.attack(enemyUnit.getEnemyPosition());
    }

    @Override
    public void retreat() {
        if(enemyUnit == null) {
            return;
        }


        if(enemyUnit.getEnemyUnit().getDistance(unit) > 128) {
            super.setUnitStatus(UnitStatus.RALLY);
            return;
        }

        if(super.getRallyPoint().toPosition().getApproxDistance(unit.getPosition()) < 128) {
            super.setUnitStatus(UnitStatus.RALLY);
            return;
        }

        if(kiteThreshold()) {
            if(unit.getGroundWeaponCooldown() == 0) {
                unit.attack(super.rallyPoint.toPosition());
                return;
            }

            unit.move(super.rallyPoint.toPosition());
            return;
        }

        unit.attack(super.rallyPoint.toPosition());
    }

    @Override
    public void defend() {
        if(enemyUnit == null) {
            setUnitStatus(UnitStatus.RALLY);
            return;
        }

        if(!inBase) {
            setUnitStatus(UnitStatus.RETREAT);
            return;
        }

        if(!enemyInBase) {
            setUnitStatus(UnitStatus.RALLY);
            return;
        }

        if(kiteThreshold()) {
            int maxRange = weaponRange();
            Position kitePos = getKitePos(maxRange);

            if(unit.getGroundWeaponCooldown() == 0) {
                unit.attack(enemyUnit.getEnemyPosition());
                return;
            }


            unit.move(kitePos);
            return;
        }

        siegeLogic();
        unit.attack(enemyUnit.getEnemyPosition());
    }

    @Override
    public void rally() {
        if(rallyPoint == null) {
            return;
        }

        if(isSieged()) {
            super.setUnitType(UnitType.Terran_Siege_Tank_Tank_Mode);
            unit.unsiege();
        }

        if(super.getRallyPoint().getApproxDistance(unit.getTilePosition()) < 128 && enemyUnit != null ) {
            super.setUnitStatus(UnitStatus.DEFEND);
            return;
        }

        if(!super.enemyInBase && canSiege()) {
            super.setUnitStatus(UnitStatus.SIEGEDEF);
        }

        unit.attack(rallyPoint.toPosition());

    }

    public void siegeDef() {
        if(siegeTile == null) {
            setSiegeTile();
        }

        if(foundSiegeTile) {
            if(game.getFrameCount() % 24 != 0) {
                return;
            }

            if(unit.getDistance(siegeTile.toPosition()) > 64) {
                unit.move(siegeTile.toPosition());
            }
            else {
                if(!isSieged() && canSiege()) {
                    super.setUnitType(UnitType.Terran_Siege_Tank_Siege_Mode);
                    unit.siege();
                }
            }
        }

        siegeLogic();
    }

    private void setSiegeTile() {
        if(!combinedTankTiles.isEmpty() && baseInfo.isNaturalOwned()) {
            pickSiegeDefTile(combinedTankTiles);
        }
        else if(!mainEdgeTiles.isEmpty()) {
            pickSiegeDefTile(mainEdgeTiles);
        }
        else {
            pickSiegeDefTile(backupSiegeTiles);
        }

    }

    private void pickSiegeDefTile(HashSet<TilePosition> tileSet) {
        Random rand = new Random(unitID);

        int index = rand.nextInt(tileSet.size());
        TilePosition targetTile = null;
        int i = 0;
        for(TilePosition tile : tileSet) {
            if (i == index) {
                targetTile = tile;
                break;
            }
            i++;
        }
        if(targetTile != null) {
            siegeTile = targetTile;
            foundSiegeTile = true;
        }
    }

    private void siegeLogic() {
        if(enemyUnit == null) {
            return;
        }

        switch(super.getUnitStatus()) {
            case ATTACK:
                if(!isSieged() && enemyUnit.getEnemyUnit().getDistance(unit) < 64) {
                    super.setUnitStatus(UnitStatus.RETREAT);
                }

                if(isSieged() && enemyUnit.getEnemyUnit().getDistance(unit) < 64
                        && !enemyUnit.getEnemyUnit().isLifted()) {
                    super.setUnitStatus(UnitStatus.RETREAT);
                    super.setUnitType(UnitType.Terran_Siege_Tank_Tank_Mode);
                    unit.unsiege();
                }
                break;
            case DEFEND:
                if(isSieged() && enemyUnit.getEnemyUnit().getDistance(unit) < 64) {
                    super.setUnitStatus(UnitStatus.RETREAT);
                    super.setUnitType(UnitType.Terran_Siege_Tank_Tank_Mode);
                    unit.unsiege();
                }
                break;
            case RETREAT:
                if(isSieged()) {
                    super.setUnitType(UnitType.Terran_Siege_Tank_Tank_Mode);
                    unit.unsiege();
                }
                break;
            case SIEGEDEF:
                if(isSieged() && enemyUnit.getEnemyUnit().getDistance(unit) < 64) {
                    super.setUnitStatus(UnitStatus.RETREAT);
                    super.setUnitType(UnitType.Terran_Siege_Tank_Tank_Mode);
                    unit.unsiege();
                }

                if(isSieged() && super.enemyInBase && enemyUnit.getEnemyUnit().getDistance(unit) > SIEGE_RANGE) {
                    super.setUnitStatus(UnitStatus.DEFEND);
                    super.setUnitType(UnitType.Terran_Siege_Tank_Tank_Mode);
                    unit.unsiege();
                }
                break;
        }

        if(enemyUnit.getEnemyUnit().getDistance(unit) < SIEGE_RANGE && !isSieged() && enemyUnit.getEnemyUnit().getDistance(unit) > 64
                && canSiege() && !enemyUnit.getEnemyType().isWorker()) {
            super.setUnitType(UnitType.Terran_Siege_Tank_Siege_Mode);
            unit.siege();
        }

        if(enemyUnit.getEnemyUnit().getDistance(unit) > SIEGE_RANGE && isSieged()
                && !enemyUnit.getEnemyUnit().isLifted()) {
            super.setUnitType(UnitType.Terran_Siege_Tank_Tank_Mode);
            unit.unsiege();
        }
    }

    private boolean kiteThreshold() {
        int maxRange = weaponRange();
        double kiteThreshold = maxRange * 0.2;
        Position enemyPosition = enemyUnit.getEnemyPosition();
        Position unitPosition = unit.getPosition();
        double distanceToEnemy = unitPosition.getDistance(enemyPosition);

        return distanceToEnemy < kiteThreshold;
    }

    private Position getKitePos(int maxRange) {
        Position enemyPosition = enemyUnit.getEnemyPosition();
        Position unitPosition = unit.getPosition();

        double dx = unitPosition.getX() - enemyPosition.getX();
        double dy = unitPosition.getY() - enemyPosition.getY();
        double length = Math.sqrt(dx * dx + dy * dy);

        double scale = maxRange / length;
        int targetX = (int) (enemyPosition.getX() + dx * scale);
        int targetY = (int) (enemyPosition.getY() + dy * scale);

        Position kitePos = new Position(targetX, targetY);
        return kitePos;
    }


    private boolean canSiege() {
        if(this.game.self().hasResearched(TechType.Tank_Siege_Mode)) {
            return true;
        }
        return false;
    }

    public boolean isSieged() {
        if(unit.getType() == UnitType.Terran_Siege_Tank_Siege_Mode) {
            return true;
        }
        return false;
    }

    private int weaponRange() {
        WeaponType weaponType = unit.getType().groundWeapon();
        return weaponType.maxRange();
    }

    public UnitType getDefaultMode() {
        return defaultMode;
    }
}
