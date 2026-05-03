package unitgroups.units;

import java.util.HashSet;
import java.util.Random;

import bwapi.Game;
import bwapi.Position;
import bwapi.TechType;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwapi.WeaponType;
import information.MapInfo;
import information.enemy.EnemyInformation;
import information.enemy.EnemyUnits;

public class SiegeTank extends CombatUnits {
    private EnemyInformation enemyInformation;
    private MapInfo mapInfo;
    private UnitType defaultMode = UnitType.Terran_Siege_Tank_Tank_Mode;
    private HashSet<EnemyUnits> enemyUnits;
    private HashSet<TilePosition> mainEdgeTiles = new HashSet<>();
    private HashSet<TilePosition> combinedTankTiles = new HashSet<>();
    private HashSet<TilePosition> backupSiegeTiles = new HashSet<>();
    private HashSet<TilePosition> ccExclusionTiles = new HashSet<>();
    private TilePosition siegeTile = null;
    private boolean foundSiegeTile = false;
    private boolean wasNaturalOwned = false;
    private int unsiegeClock = 0;

    private static final int SIEGE_RANGE = 384;

    public SiegeTank(Game game, EnemyInformation enemyInformation, Unit unit) {
        super(game, unit);
        this.enemyInformation = enemyInformation;
        this.enemyUnits = enemyInformation.getEnemyUnits();
        mapInfo = enemyInformation.getBaseInfo();
        mainEdgeTiles = mapInfo.getMainCliffEdge();
        combinedTankTiles = mapInfo.getCombinedTankTiles();
        backupSiegeTiles = mapInfo.getBackupMainSiegeTiles();
        ccExclusionTiles = mapInfo.getCcExclusionTiles();
    }

    @Override
    public void attack() {
        if (enemyUnit == null) {
            return;
        }

        EnemyUnits target = enemyUnit;
        if (!priorityTargetExists) {
            EnemyUnits preferred = null;
            if (preferred != null) {
                target = preferred;
            }
        }

        siegeLogic();

        if (unit.getDistance(target.getEnemyPosition()) > SIEGE_RANGE || !target.getEnemyUnit().isVisible()) {
            unit.attack(target.getEnemyPosition());
        }
        else {
            unit.attack(target.getEnemyUnit());
        }
    }

    @Override
    public void retreat() {
        if (enemyUnit == null) {
            return;
        }


        if (enemyUnit.getEnemyUnit().getDistance(unit) > 128) {
            super.setUnitStatus(UnitStatus.RALLY);
            return;
        }

        if (super.getRallyPoint().toPosition().getApproxDistance(unit.getPosition()) < 128) {
            super.setUnitStatus(UnitStatus.RALLY);
            return;
        }

        if (dtUndetected && super.getRallyPoint() != null) {
            unit.move(super.getRallyPoint().toPosition());
            return;
        }

        if (kiteThreshold()) {
            if (unit.getGroundWeaponCooldown() == 0) {
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
        if (enemyUnit == null) {
            setUnitStatus(UnitStatus.RALLY);
            return;
        }

        EnemyUnits target = enemyUnit;
        if (!priorityTargetExists) {
            EnemyUnits preferred = null;
            if (preferred != null) {
                target = preferred;
            }
        }

        if (!inBase) {
            setUnitStatus(UnitStatus.RETREAT);
            return;
        }

        if (!enemyInBase) {
            setUnitStatus(UnitStatus.RALLY);
            return;
        }

        if (kiteThreshold()) {
            int maxRange = weaponRange();
            Position kitePos = getKitePos(maxRange);

            if (unit.getGroundWeaponCooldown() == 0) {
                unit.attack(target.getEnemyPosition());
                return;
            }


            unit.move(kitePos);
            return;
        }

        siegeLogic();

        if (!isSieged() && getUnitType() == UnitType.Terran_Siege_Tank_Siege_Mode) {
            return;
        }

        if (unit.getDistance(target.getEnemyPosition()) > SIEGE_RANGE) {
            unit.attack(target.getEnemyPosition());
        }
        else {
            unit.attack(target.getEnemyUnit());
        }

    }

    @Override
    public void rally() {
        if (rallyPoint == null) {
            return;
        }

        if (isSieged()) {
            super.setUnitType(UnitType.Terran_Siege_Tank_Tank_Mode);
            unit.unsiege();
        }

        if (super.getRallyPoint().getApproxDistance(unit.getTilePosition()) < 128 && enemyUnit != null ) {
            super.setUnitStatus(UnitStatus.DEFEND);
            return;
        }

        if (!super.enemyInBase && canSiege()) {
            super.setUnitStatus(UnitStatus.SIEGEDEF);
        }

        unit.attack(rallyPoint.toPosition());

    }

    @Override
    public void sallyOut() {
        if (enemyUnit == null) {
            return;
        }

        EnemyUnits target = enemyUnit;
        if (!priorityTargetExists) {
            EnemyUnits preferred = null;
            if (preferred != null) {
                target = preferred;
            }
        }

        if (enemyInBase) {
            super.setUnitStatus(UnitStatus.DEFEND);
            return;
        }

        siegeLogic();

        if(unit.getDistance(target.getEnemyPosition()) > SIEGE_RANGE) {
            unit.attack(target.getEnemyPosition());
        }
        else {
            unit.attack(target.getEnemyUnit());
        }

    }

    @Override
    public void regroup() {
        if (regroupPosition == null) {
            return;
        }

        if (!game.isWalkable(regroupPosition.toWalkPosition())) {
            regroupStuckCounter = 0;
            lastRegroupCheckPosition = null;
            setUnitStatus(UnitStatus.ATTACK);
            return;
        }

        if (enemyUnit != null && enemyUnit.getEnemyUnit().getDistance(unit) < 200) {
            regroupStuckCounter = 0;
            lastRegroupCheckPosition = null;
            setUnitStatus(UnitStatus.ATTACK);
            return;
        }

        if (unit.getPosition().getDistance(regroupPosition) < 225) {
            regroupStuckCounter = 0;
            lastRegroupCheckPosition = null;
            setUnitStatus(UnitStatus.ATTACK);
            return;
        }

        regroupStuckCheckTimer++;
        if (regroupStuckCheckTimer >= STUCK_CHECK_INTERVAL) {
            regroupStuckCheckTimer = 0;
            if (lastRegroupCheckPosition != null
                    && unit.getPosition().getApproxDistance(lastRegroupCheckPosition) < 16) {
                regroupStuckCounter++;
            } else {
                regroupStuckCounter = 0;
            }
            lastRegroupCheckPosition = unit.getPosition();
        }

        if (regroupStuckCounter >= STUCK_THRESHOLD) {
            regroupStuckCounter = 0;
            int dx = regroupPosition.getX() - unit.getPosition().getX();
            int dy = regroupPosition.getY() - unit.getPosition().getY();
            double length = Math.sqrt(dx * dx + dy * dy);
            if (length > 0) {
                int perpX = (int) (-dy * 100 / length);
                int perpY = (int) (dx * 100 / length);
                int moveX = Math.min(Math.max(unit.getPosition().getX() + perpX, 0), game.mapWidth() * 32);
                int moveY = Math.min(Math.max(unit.getPosition().getY() + perpY, 0), game.mapHeight() * 32);
                unit.move(new Position(moveX, moveY));
            }
            return;
        }

        siegeLogic();

        unit.move(regroupPosition);
    }

    public void siegeDef() {
        boolean naturalOwned = mapInfo.isNaturalOwned();
        if (naturalOwned && !wasNaturalOwned) {
            siegeTile = null;
            foundSiegeTile = false;
            wasNaturalOwned = true;
        }

        if (siegeTile == null) {
            setSiegeTile();
        }

        if (foundSiegeTile) {
            if (game.getFrameCount() % 24 != 0) {
                return;
            }

            if (isSieged()) {
                boolean enemyInRange = enemyUnit != null
                        && !enemyUnit.getEnemyType().isWorker()
                        && unit.getDistance(enemyUnit.getEnemyUnit()) < SIEGE_RANGE;
                boolean atSiegeTile = unit.getDistance(siegeTile.toPosition()) <= 64;
                if (!enemyInRange && !atSiegeTile) {
                    super.setUnitType(UnitType.Terran_Siege_Tank_Tank_Mode);
                    unit.unsiege();
                }
                return;
            }

            if (enemyUnit != null && canSiege()) {
                double dist = unit.getDistance(enemyUnit.getEnemyUnit());
                if (!enemyUnit.getEnemyType().isWorker() && dist < SIEGE_RANGE - 32 && dist > 64) {
                    super.setUnitType(UnitType.Terran_Siege_Tank_Siege_Mode);
                    unit.siege();
                    return;
                }
            }

            if (unit.getDistance(siegeTile.toPosition()) > 64) {
                unit.move(siegeTile.toPosition());
            }
            else {
                if (!isSieged() && canSiege()) {
                    super.setUnitType(UnitType.Terran_Siege_Tank_Siege_Mode);
                    unit.siege();
                }
            }
        }

        siegeLogic();
    }

    private void setSiegeTile() {
        if (!combinedTankTiles.isEmpty() && mapInfo.isNaturalOwned()) {
            pickSiegeDefTile(combinedTankTiles);
        }
        else if (!mainEdgeTiles.isEmpty()) {
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
        for (TilePosition tile : tileSet) {
            if (i == index) {
                targetTile = tile;
                break;
            }
            i++;
        }
        if (targetTile != null && !ccExclusionTiles.contains(targetTile)) {
            siegeTile = targetTile;
            foundSiegeTile = true;
        }
    }

    private void siegeLogic() {
        if (enemyUnit == null) {
            return;
        }

        int distToEnemy = unit.getDistance(enemyUnit.getEnemyPosition());

        switch (super.getUnitStatus()) {
            case OBSTRUCTING:
                if (isSieged()) {
                    super.setUnitType(UnitType.Terran_Siege_Tank_Tank_Mode);
                    unit.unsiege();
                }
                return;
            case ATTACK:
                if (!isSieged() && distToEnemy < 64) {
                    super.setUnitStatus(UnitStatus.RETREAT);
                }

                if (isSieged() && distToEnemy < 64
                        && !enemyUnit.getEnemyUnit().isLifted()) {
                    super.setUnitStatus(UnitStatus.RETREAT);
                    super.setUnitType(UnitType.Terran_Siege_Tank_Tank_Mode);
                    unit.unsiege();
                }
                break;
            case DEFEND:
                if (isSieged() && distToEnemy < 64) {
                    super.setUnitStatus(UnitStatus.RETREAT);
                    super.setUnitType(UnitType.Terran_Siege_Tank_Tank_Mode);
                    unit.unsiege();
                }
                break;
            case RETREAT:
                if (isSieged()) {
                    super.setUnitType(UnitType.Terran_Siege_Tank_Tank_Mode);
                    unit.unsiege();
                }
                break;
            case SIEGEDEF:
                if (isSieged() && distToEnemy < 64) {
                    super.setUnitStatus(UnitStatus.RETREAT);
                    super.setUnitType(UnitType.Terran_Siege_Tank_Tank_Mode);
                    unit.unsiege();
                }

                if (super.enemyInBase && distToEnemy > SIEGE_RANGE) {
                    super.setUnitStatus(UnitStatus.DEFEND);
                    if (isSieged()) {
                        super.setUnitType(UnitType.Terran_Siege_Tank_Tank_Mode);
                        unit.unsiege();
                    }
                }
                break;
            case REGROUP:
                if (isSieged() && distToEnemy < 64 || distToEnemy > SIEGE_RANGE) {
                    super.setUnitType(UnitType.Terran_Siege_Tank_Tank_Mode);
                    unit.unsiege();
                }
                break;
        }

        if (isSieged() && !unit.isAttacking() || unit.getGroundWeaponCooldown() == 0) {
            unsiegeClock += 8;
        }
        else {
            unsiegeClock = 0;
        }

        if (distToEnemy < SIEGE_RANGE - 32 && !isSieged() && distToEnemy > 64
                && canSiege() && !enemyUnit.getEnemyType().isWorker() && enemyUnit.getEnemyUnit().isVisible()) {
            super.setUnitType(UnitType.Terran_Siege_Tank_Siege_Mode);
            unit.siege();
        }

        if ((distToEnemy > SIEGE_RANGE || !enemyUnit.getEnemyUnit().isVisible())
                && isSieged()
                && unsiegeClock > 144
                && !enemyUnit.getEnemyUnit().isLifted()) {
            super.setUnitType(UnitType.Terran_Siege_Tank_Tank_Mode);
            unsiegeClock = 0;
            unit.unsiege();
        }
    }

    
    private EnemyUnits findValidTarget() {
        EnemyUnits tank = null, lurker = null, staticDefense = null, other = null, building = null, worker = null;
        double tankDist = Double.MAX_VALUE, lurkerDist = Double.MAX_VALUE, staticDist = Double.MAX_VALUE;
        double otherDist = Double.MAX_VALUE, buildingDist = Double.MAX_VALUE, workerDist = Double.MAX_VALUE;

        for (EnemyUnits enemy : enemyUnits) {
            if ((enemy.getEnemyUnit().isCloaked() 
                    || enemy.getEnemyUnit().isBurrowed()) 
                    && !enemy.getEnemyUnit().isDetected()) {
                continue;
            }

            if (enemy.getEnemyPosition() == null) {
                continue;
            }

            double dist = unit.getDistance(enemy.getEnemyPosition());
            
            UnitType type = enemy.getEnemyType();

            if (type.isWorker()) {
                continue;
            }

            if (dist > SIEGE_RANGE) {
                continue;
            }
            
 
            if ((type == UnitType.Terran_Siege_Tank_Tank_Mode || type == UnitType.Terran_Siege_Tank_Siege_Mode)
                    && dist < tankDist) {
                tank = enemy;
                tankDist = dist;
            }
            else if (type == UnitType.Zerg_Lurker && dist < lurkerDist) {
                lurker = enemy;
                lurkerDist = dist;
            }
            else if (isStaticDefense(type) && dist < staticDist) {
                staticDefense = enemy;
                staticDist = dist;
            }
            else if (type.isWorker() && dist < workerDist) {
                worker = enemy;
                workerDist = dist;
            }
            else if (!type.isBuilding() && dist < otherDist) {
                other = enemy;
                otherDist = dist;
            } 
            else if (type.isBuilding() && dist < buildingDist) {
                building = enemy;
                buildingDist = dist;
            }
        }

        if (tank != null) {
            return tank;
        }

        if (lurker != null) {
            return lurker;
        }

        if (staticDefense != null) {
            return staticDefense;
        }

        if (other != null) {
            return other;
        }

        if (worker != null) {
            return worker;
        }
        return building;
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
        if (maxRange == 0) {
            return mapInfo.getStartingBase().getCenter();
        }

        Position enemyPosition = enemyUnit.getEnemyPosition();
        Position unitPosition = unit.getPosition();

        double dx = unitPosition.getX() - enemyPosition.getX();
        double dy = unitPosition.getY() - enemyPosition.getY();
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length < 1) {
            return mapInfo.getStartingBase().getCenter();
        }

        double scale = maxRange / length;
        int targetX = (int) (enemyPosition.getX() + dx * scale);
        int targetY = (int) (enemyPosition.getY() + dy * scale);

        return new Position(targetX, targetY);
    }

    private boolean isStaticDefense(UnitType unitType) {
        return unitType == UnitType.Zerg_Sunken_Colony
                || unitType == UnitType.Protoss_Photon_Cannon
                || unitType == UnitType.Terran_Bunker;
    }


    private boolean canSiege() {
        if (this.game.self().hasResearched(TechType.Tank_Siege_Mode)) {
            return true;
        }
        return false;
    }

    public boolean isSieged() {
        return unit.getType() == UnitType.Terran_Siege_Tank_Siege_Mode;
    }

    private int weaponRange() {
        WeaponType weaponType = unit.getType().groundWeapon();
        return weaponType.maxRange();
    }

    public UnitType getDefaultMode() {
        return defaultMode;
    }
}
