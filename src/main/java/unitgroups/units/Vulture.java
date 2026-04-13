package unitgroups.units;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import bwapi.Game;
import bwapi.Position;
import bwapi.TechType;
import bwapi.Unit;
import bwapi.UnitType;
import bwapi.UpgradeType;
import bwapi.WeaponType;
import bwem.Base;
import bwem.ChokePoint;
import information.MapInfo;
import information.enemy.EnemyInformation;
import information.enemy.EnemyUnits;
import util.Time;

public class Vulture extends CombatUnits {
    private EnemyInformation enemyInformation;
    private MapInfo mapInfo;
    private HashSet<EnemyUnits> enemyUnits;
    private List<Position> minePositions = new ArrayList<>();
    private Position currentMinePos = null;
    private int mineCount = 3;
    private int pulseCheck = 0;
    private int mineTimer = 0;
    private boolean layingMines = false;
    private boolean recentlyMined = false;
    private boolean lobotomyOverride = false;

    private final int FULL_MINE_CYCLE = new Time(0,30).getFrames();
    private final int ALLOWED_MINE_CYCLE = new Time(0,17).getFrames();
    private int mineCycle;

    public Vulture(Game game, EnemyInformation enemyInformation, Unit unit) {
        super(game, unit);
        this.enemyInformation = enemyInformation;
        this.enemyUnits = enemyInformation.getEnemyUnits();
        mapInfo = enemyInformation.getBaseInfo();
        unitStatus = UnitStatus.POKE;
        mineCycle = game.getFrameCount();
        calculateMinePositions();
    }

    @Override
    public void rally() {
        if (rallyPoint == null) {
            return;
        }

        unit.move(rallyPoint.toPosition());
        setUnitStatus(UnitStatus.ATTACK);
    }

    @Override
    public void attack() {
        if (enemyUnit == null) {
            return;
        }

        if (mineCount > unit.getSpiderMineCount()) {
            mineCount = unit.getSpiderMineCount();
            recentlyMined = true;
            layingMines = false;
        }

        if (pulseCheck > 64) {
            pulseCheck = 0;
            layingMines = false;
        }

        if (layingMines) {
            if (unit.isStuck()) {
                layingMines = false;
                recentlyMined = true;
                pulseCheck = 0;
                return;
            }

            pulseCheck += 8;
            return;
        }

        if (isOutRanged() && !hasTankSupport && !lobotomyOverride) {
            unitStatus = UnitStatus.RETREAT;
            return;
        }

        if (enemyInformation.outRangingUnitNearby(enemyUnit, unit.getType(), unit.getType().groundWeapon().maxRange() - 32) && !hasTankSupport  && !lobotomyOverride) {
            unitStatus = UnitStatus.RETREAT;
            return;
        }

        if (hasTankSupport) {
            unit.attack(enemyUnit.getEnemyUnit());
        }
        else {
            attackMove();
        }

        if (!game.self().hasResearched(TechType.Spider_Mines) || unit.getSpiderMineCount() == 0) {
            return;
        }

        layMinesOnEnemy();

        if (allowMineLaying()) {
            layMinesAtChokepoints();
            layMinesAnyWhere();
        }
    }

    @Override
    public void retreat() {
        if (enemyUnit == null || rallyPoint == null) {
            return;
        }

        if (unit.getDistance(rallyPoint.toPosition()) < 100) {
            setUnitStatus(UnitStatus.DEFEND);
        }

        if (!inRangeOfThreat && isOutRanged() || dtUndetected) {
            unit.move(rallyPoint.toPosition());
        }

        if (inBase && isOutRanged()) {
            setUnitStatus(UnitStatus.DEFEND);
        }


        if (enemyInformation.outRangingUnitNearby(enemyUnit, unit.getType(), unit.getType().groundWeapon().maxRange() - 32) && !hasTankSupport) {
            unit.move(rallyPoint.toPosition());
        }

        if (!inRangeOfThreat && (!isOutRanged() || hasTankSupport)) {
            setUnitStatus(UnitStatus.ATTACK);
        }

        if (game.self().hasResearched(TechType.Spider_Mines) && unit.getSpiderMineCount() > 0 && !layingMines && !recentlyMined) {
            if (!mapInfo.getBaseTiles().contains(unit.getPosition().toTilePosition())
                    && !mapInfo.getNaturalTiles().contains(unit.getPosition().toTilePosition())
                    && unit.getDistance(enemyUnit.getEnemyPosition()) > 300) {
                unit.useTech(TechType.Spider_Mines, unit.getPosition());
                layingMines = true;
            }
        }
    }

    @Override
    public void defend() {
        if (rallyPoint == null) {
            return;
        }

        if (enemyUnit == null) {
            setUnitStatus(UnitStatus.ATTACK);
            return;
        }

        if (isOutRanged() && inBase) {
            attackMove();

            if (hasTankSupport) {
                setUnitStatus(UnitStatus.ATTACK);
            }

            return;
        }

        if (!enemyInBase && inBase) {
            setUnitStatus(UnitStatus.ATTACK);
            return;
        }

        if (!isOutRanged()) {
            setUnitStatus(UnitStatus.ATTACK);
            return;
        }

        attackMove();
    }

    //TODO: make unique later
    @Override
    public void poke() {
        attack();
    }

    private Position kiteTo(int distance) {
        Position enemyPos = enemyUnit.getEnemyPosition();
        Position unitPos = unit.getPosition();
        int dx = unitPos.getX() - enemyPos.getX();
        int dy = unitPos.getY() - enemyPos.getY();

        double moveX = unitPos.getX() + (dx * distance / Math.max(1, unitPos.getDistance(enemyPos)));
        double moveY = unitPos.getY() + (dy * distance / Math.max(1, unitPos.getDistance(enemyPos)));

        moveX = Math.min(Math.max(moveX, 0), game.mapWidth() * 32);
        moveY = Math.min(Math.max(moveY, 0), game.mapHeight() * 32);
        return new Position((int) moveX, (int) moveY);
    }

    private void attackMove() {
        Position enemyPos = enemyUnit.getEnemyPosition();
        Position unitPos = unit.getPosition();
        double distToEnemy = unitPos.getDistance(enemyPos);

        if (distToEnemy < 64 && rallyPoint != null) {
            unit.move(rallyPoint.toPosition());
            return;
        }

        Position kitePos = getKitePosition();
        if (kitePos != null) {
            unit.move(kitePos);
            return;
        }

        unit.attack(enemyUnit.getEnemyPosition());

        boolean onCooldown = unit.getGroundWeaponCooldown() > 0;
        boolean inAttackAnimation = unit.isAttackFrame() || unit.isStartingAttack();

        if (distToEnemy > weaponRange() + 32 && !inRangeNextTick(distToEnemy)) {
            unit.move(approachTo(enemyPos));
        }
        else if (onCooldown || inAttackAnimation) {
            unit.move(kiteAwayFrom(enemyPos, weaponRange() + 64));
        }
        else {
            unit.patrol(enemyUnit.getEnemyPosition());
        }
    }

    private Position approachTo(Position targetPos) {
        Position unitPos = unit.getPosition();
        double dx = unitPos.getX() - targetPos.getX();
        double dy = unitPos.getY() - targetPos.getY();
        double dist = Math.max(1, unitPos.getDistance(targetPos));

        double moveX = targetPos.getX() + (dx * (weaponRange() - 32) / dist);
        double moveY = targetPos.getY() + (dy * (weaponRange() - 32) / dist);

        moveX = Math.min(Math.max(moveX, 0), game.mapWidth() * 32);
        moveY = Math.min(Math.max(moveY, 0), game.mapHeight() * 32);
        return new Position((int) moveX, (int) moveY);
    }

    private boolean inRangeNextTick(double distToEnemy) {
        double vultureSpeed = unit.getType().topSpeed();
        if (game.self().getUpgradeLevel(UpgradeType.Ion_Thrusters) > 0) {
            vultureSpeed *= 1.5;
        }
        double closingSpeed = (vultureSpeed + enemyUnit.getEnemyType().topSpeed()) * 8;
        return distToEnemy - closingSpeed <= weaponRange();
    }

    private Position kiteAwayFrom(Position enemyPos, int distance) {
        Position unitPos = unit.getPosition();
        int dx = unitPos.getX() - enemyPos.getX();
        int dy = unitPos.getY() - enemyPos.getY();
        double dist = Math.max(1, unitPos.getDistance(enemyPos));

        double moveX = unitPos.getX() + (dx * distance / dist);
        double moveY = unitPos.getY() + (dy * distance / dist);

        moveX = Math.min(Math.max(moveX, 0), game.mapWidth() * 32);
        moveY = Math.min(Math.max(moveY, 0), game.mapHeight() * 32);
        return new Position((int) moveX, (int) moveY);
    }

    private Position getKitePosition() {
        Position unitPos = unit.getPosition();
        double sumDx = 0;
        double sumDy = 0;
        boolean anyThreat = false;

        for (EnemyUnits enemy : enemyUnits) {
            int range = getGroundThreatRange(enemy);

            if (range == 0) {
                continue;
            }

            if (enemy.getEnemyPosition() == null) {
                continue;
            }

            UnitType type = enemy.getEnemyType();
            boolean staticThreat = type == UnitType.Protoss_Photon_Cannon
                    || type == UnitType.Zerg_Sunken_Colony
                    || type == UnitType.Terran_Bunker;

            if (type.isBuilding() && enemy.getEnemyUnit().isVisible()) {
                if (type == UnitType.Terran_Bunker) {
                    if (!enemy.getEnemyUnit().isCompleted()) {
                        continue;
                    }
                }
                else if (!enemy.getEnemyUnit().isCompleted() || enemy.getEnemyUnit().isMorphing()
                        || !enemy.getEnemyUnit().isPowered()) {
                    continue;
                }
            }

            double threatDist = unitPos.getDistance(enemy.getEnemyPosition());

            int safeDistance;
            if (staticThreat) {
                safeDistance = range + 160;
            }
            else {
                safeDistance = range + 96;
            }

            if (threatDist < safeDistance) {
                anyThreat = true;
                double dx = unitPos.getX() - enemy.getEnemyPosition().getX();
                double dy = unitPos.getY() - enemy.getEnemyPosition().getY();
                double weight = (safeDistance - threatDist) / safeDistance;
                sumDx += (dx / Math.max(1, threatDist)) * weight;
                sumDy += (dy / Math.max(1, threatDist)) * weight;
            }
        }

        if (!anyThreat) {
            return null;
        }

        double len = Math.sqrt(sumDx * sumDx + sumDy * sumDy);
        if (len < 0.001) {
            return null;
        }

        int maxX = game.mapWidth() * 32 - 1;
        int maxY = game.mapHeight() * 32 - 1;

        double rawX = unitPos.getX() + (sumDx / len) * 320;
        double rawY = unitPos.getY() + (sumDy / len) * 320;

        boolean xOutOfBounds = rawX < 0 || rawX > maxX;
        boolean yOutOfBounds = rawY < 0 || rawY > maxY;

        double moveX;
        double moveY;

        if (xOutOfBounds && yOutOfBounds) {
            return null;
        }
        else if (xOutOfBounds) {
            moveX = unitPos.getX();
            moveY = Math.min(Math.max(unitPos.getY() + Math.signum(sumDy) * 320, 0), maxY);
        }
        else if (yOutOfBounds) {
            moveX = Math.min(Math.max(unitPos.getX() + Math.signum(sumDx) * 320, 0), maxX);
            moveY = unitPos.getY();
        }
        else {
            moveX = Math.min(Math.max(rawX, 0), maxX);
            moveY = Math.min(Math.max(rawY, 0), maxY);
        }

        return new Position((int) moveX, (int) moveY);
    }

    private int getGroundThreatRange(EnemyUnits enemy) {
        UnitType type = enemy.getEnemyType();
        if (type == UnitType.Terran_Bunker) {
            return UnitType.Terran_Marine.groundWeapon().maxRange();
        }
        if (type.groundWeapon() != WeaponType.None) {
            return type.groundWeapon().maxRange();
        }
        return 0;
    }

    private int weaponRange() {
        WeaponType weaponType = unit.getType().groundWeapon();
            return weaponType.maxRange();
    }

    private void calculateMinePositions() {
        if (enemyInformation.getStartingEnemyBase() == null) {
            return;
        }

        if (enemyInformation.getStartingEnemyBase().getEnemyPosition() == null) {
            return;
        }

        ChokePoint mainChoke = mapInfo.getMainChoke();
        ChokePoint naturalChoke = mapInfo.getNaturalChoke();

        if (mainChoke == null || naturalChoke == null) {
            return;
        }

        for (Base base : mapInfo.getAllBasePaths().getChokePathLists().keySet()) {
            if (enemyInformation.getStartingEnemyBase().getEnemyPosition().equals(base.getCenter())) {
                List<Position> allPositions = mapInfo.getAllBasePaths().getChokePathLists().get(base);

                for (Position position : allPositions) {
                    boolean nearMainChoke = position.getDistance(mainChoke.getCenter().toPosition()) < 175;
                    boolean nearNaturalChoke = position.getDistance(naturalChoke.getCenter().toPosition()) < 175;

                    if (!nearMainChoke && !nearNaturalChoke) {
                        minePositions.add(position);
                    }
                }
            }
        }
    }

    private boolean isOutRanged() {
        if (enemyUnit != null) {
            if (enemyUnit.getEnemyType().isBuilding() || enemyUnit.getEnemyType() == UnitType.Terran_Marine) {
                return false;
            }
            if (enemyUnit.getEnemyType().groundWeapon().maxRange() + 32 >= weaponRange()) {
                return true;
            }
        }
        return false;
    }

    private void layMinesAtChokepoints() {
        if (minePositions.isEmpty()) {
            calculateMinePositions();
            return;
        }

        for (Position pos : minePositions) {
            if (unit.getDistance(pos) < 250) {
                for (int validMinePositionAttempt = 0; validMinePositionAttempt < 10; validMinePositionAttempt++) {
                    Random random = new Random();
                    int randX = pos.getX() - 200 + random.nextInt(400);
                    int randY = pos.getY() - 200 + random.nextInt(400);
                    Position testPos = new Position(randX, randY);

                    if (mapInfo.getPathFinding().getTilePositionValidator().isWalkable(testPos.toTilePosition())) {
                        currentMinePos = testPos;
                        break;
                    }

                    validMinePositionAttempt++;
                }

                if (currentMinePos != null) {
                    unit.useTech(TechType.Spider_Mines, currentMinePos);
                    layingMines = true;
                    currentMinePos = null;
                }
            }
        }
    }

    private void layMinesAnyWhere() {
        if (mapInfo.getBaseTiles().contains(unit.getPosition().toTilePosition())
                || mapInfo.getNaturalTiles().contains(unit.getPosition().toTilePosition())
                || mapInfo.getMinBaseTiles().contains(unit.getPosition().toTilePosition())) {
            return;
        }

        if (enemyUnit != null && unit.getDistance(enemyUnit.getEnemyPosition()) < 200) {
            return;
        }

        if (layingMines) {
            return;
        }

        unit.useTech(TechType.Spider_Mines, unit.getPosition());
        layingMines = true;
    }

    private void layMinesOnEnemy() {
        if (enemyUnit.getEnemyType().isBuilding()) {
            return;
        }

        if (mineTimer >= 192) {
            mineTimer = 0;
            recentlyMined = false;
        }

        if (recentlyMined) {
            mineTimer += 8;
            return;
        }

        if (unit.getDistance(enemyUnit.getEnemyPosition()) < 200) {
            if (mapInfo.getBaseTiles().contains(unit.getPosition().toTilePosition())
                    || mapInfo.getNaturalTiles().contains(unit.getPosition().toTilePosition())) {
                return;
            }
            if (hasTankSupport && enemyUnit.getEnemyType() != UnitType.Terran_Siege_Tank_Siege_Mode) {
                Position minePos = unit.getPosition();
                unit.useTech(TechType.Spider_Mines, minePos);
                layingMines = true;
            }
            else if (enemyUnit.getEnemyType().groundWeapon().maxRange() <= 64) {
                Position minePos = kiteTo(128);
                unit.move(minePos);
                unit.useTech(TechType.Spider_Mines, minePos);
                layingMines = true;
            }
            else if (enemyUnit.getEnemyType() == UnitType.Terran_Siege_Tank_Siege_Mode) {
                Position enemyPos = enemyUnit.getEnemyPosition();
                Position unitPos = unit.getPosition();

                int dx = enemyPos.getX() - unitPos.getX();
                int dy = enemyPos.getY() - unitPos.getY();
                double distance = unitPos.getDistance(enemyPos);

                double moveX = unitPos.getX() + (dx * 10 / Math.max(1, distance));
                double moveY = unitPos.getY() + (dy * 10 / Math.max(1, distance));

                Position minePos = new Position((int) moveX, (int) moveY);

                unit.move(minePos);
                unit.useTech(TechType.Spider_Mines, minePos);
                layingMines = true;
            }
            else {
                unit.useTech(TechType.Spider_Mines, unit.getPosition());
                layingMines = true;
            }
        }

    }

    private boolean allowMineLaying() {
        if (recentlyMined) {
            return false;
        }

        int currentFrame = game.getFrameCount();
        int idOffset = (10 + (unit.getID() % 21)) * 24;
        if ((currentFrame - mineCycle + idOffset) % FULL_MINE_CYCLE < ALLOWED_MINE_CYCLE) {
            return true;
        }
        return false;
    }

    public boolean isLobotomyOverride() {
        return lobotomyOverride;
    }

    public void setLobotomyOverride(boolean lobotomyOverride) {
        this.lobotomyOverride = lobotomyOverride;
    }
}
