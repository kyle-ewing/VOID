package unitgroups.units;

import bwapi.*;
import bwem.Base;
import bwem.ChokePoint;
import information.BaseInfo;
import information.enemy.EnemyInformation;
import util.Time;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Vulture extends CombatUnits {
    private EnemyInformation enemyInformation;
    private BaseInfo baseInfo;
    private List<Position> minePositions = new ArrayList<>();
    private Position currentMinePos = null;
    private int mineCount = 3;
    private int pulseCheck = 0;
    private int mineTimer = 0;
    private boolean layingMines = false;
    private boolean recentlyMined = false;
    private boolean lobotomyOverride = false;

    private final int FULL_MINE_CYCLE = new Time(0,30).getFrames();
    private final int ALLOWED_MINE_CYCLE = new Time(0,10).getFrames();
    private int mineCycle;

    public Vulture(Game game, EnemyInformation enemyInformation, Unit unit) {
        super(game, unit);
        this.enemyInformation = enemyInformation;
        baseInfo = enemyInformation.getBaseInfo();
        unitStatus = UnitStatus.ATTACK;
        mineCycle = game.getFrameCount();
        calculateMinePositions();
    }

    @Override
    public void rally() {
        if(rallyPoint == null) {
            return;
        }

        unit.move(rallyPoint.toPosition());
        setUnitStatus(UnitStatus.ATTACK);
    }

    @Override
    public void attack() {
        if(enemyUnit == null) {
            return;
        }

        if(mineCount > unit.getSpiderMineCount()) {
            mineCount = unit.getSpiderMineCount();
            recentlyMined = true;
            layingMines = false;
        }

        if(pulseCheck > 64) {
            pulseCheck = 0;
            layingMines = false;
        }

        if(layingMines) {
            if(unit.isStuck()) {
                layingMines = false;
                recentlyMined = true;
                pulseCheck = 0;
                return;
            }

            pulseCheck += 8;
            return;
        }

        if(isOutRanged() && !hasTankSupport && !lobotomyOverride) {
            unitStatus = UnitStatus.RETREAT;
            return;
        }

        if(enemyInformation.outRangingUnitNearby(enemyUnit, unit.getType(), unit.getType().groundWeapon().maxRange() - 32) && !hasTankSupport  && !lobotomyOverride) {
            unitStatus = UnitStatus.RETREAT;
            return;
        }

        attackMove();

        if(!game.self().hasResearched(TechType.Spider_Mines) || unit.getSpiderMineCount() == 0) {
            return;
        }

        layMinesOnEnemy();

        if(allowMineLaying()) {
            layMinesAtChokepoints();
        }
    }

    @Override
    public void retreat() {
        if(enemyUnit == null || rallyPoint == null) {
            return;
        }

        if(unit.getDistance(rallyPoint.toPosition()) < 100) {
            setUnitStatus(UnitStatus.DEFEND);
        }

        if(!inRangeOfThreat && isOutRanged()) {
            unit.move(rallyPoint.toPosition());
        }

        if(inBase && isOutRanged()) {
            setUnitStatus(UnitStatus.DEFEND);
        }

        if(enemyInformation.outRangingUnitNearby(enemyUnit, unit.getType(), unit.getType().groundWeapon().maxRange() - 32) && !hasTankSupport) {
            unit.move(rallyPoint.toPosition());
        }

        if(!inRangeOfThreat && (!isOutRanged() || hasTankSupport)) {
            setUnitStatus(UnitStatus.ATTACK);
        }
    }

    @Override
    public void defend() {
        if(rallyPoint == null) {
            return;
        }

        if(enemyUnit == null) {
            setUnitStatus(UnitStatus.ATTACK);
            return;
        }

        if(isOutRanged() && inBase) {
            attackMove();

            if(hasTankSupport) {
                setUnitStatus(UnitStatus.ATTACK);
            }

            return;
        }

        if(!enemyInBase && inBase) {
            setUnitStatus(UnitStatus.ATTACK);
            return;
        }

        if(!isOutRanged()) {
            setUnitStatus(UnitStatus.ATTACK);
            return;
        }

        attackMove();
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

    private boolean minimnumThreshold(double threshold) {
        double halfRange = weaponRange() * threshold;
        Position enemyPosition = enemyUnit.getEnemyPosition();
        Position unitPosition = unit.getPosition();
        double distanceToEnemy = unitPosition.getDistance(enemyPosition);

        return distanceToEnemy < halfRange;
    }

    private void attackMove() {
        if(minimnumThreshold(1.05)) {
            unit.patrol(kiteTo(weaponRange()));
        }
        else if(minimnumThreshold(0.5)) {
            unit.move(kiteTo(weaponRange()));
        }
        else {
            unit.move(enemyUnit.getEnemyPosition());
        }
    }

    private int weaponRange() {
        WeaponType weaponType = unit.getType().groundWeapon();
            return weaponType.maxRange();
    }

    private void calculateMinePositions() {
        if(enemyInformation.getStartingEnemyBase() == null) {
            return;
        }

        if(enemyInformation.getStartingEnemyBase().getEnemyPosition() == null) {
            return;
        }

        ChokePoint mainChoke = baseInfo.getMainChoke();
        ChokePoint naturalChoke = baseInfo.getNaturalChoke();

        if(mainChoke == null || naturalChoke == null) {
            return;
        }

        for(Base base : baseInfo.getAllBasePaths().getChokePathLists().keySet()) {
            if(enemyInformation.getStartingEnemyBase().getEnemyPosition().equals(base.getCenter())) {
                List<Position> allPositions = baseInfo.getAllBasePaths().getChokePathLists().get(base);

                for(Position position : allPositions) {
                    boolean nearMainChoke = position.getDistance(mainChoke.getCenter().toPosition()) < 175;
                    boolean nearNaturalChoke = position.getDistance(naturalChoke.getCenter().toPosition()) < 175;

                    if(!nearMainChoke && !nearNaturalChoke) {
                        minePositions.add(position);
                    }
                }
            }
        }
    }

    private boolean isOutRanged() {
        if(enemyUnit != null) {
            if(enemyUnit.getEnemyType().isBuilding() || enemyUnit.getEnemyType() == UnitType.Terran_Marine) {
                return false;
            }
            if(enemyUnit.getEnemyType().groundWeapon().maxRange() + 32 >= weaponRange()) {
                return true;
            }
        }
        return false;
    }

    private void layMinesAtChokepoints() {
        if(minePositions.isEmpty()) {
            calculateMinePositions();
            return;
        }

        for(Position pos : minePositions) {
            if(unit.getDistance(pos) < 250) {
                for(int validMinePositionAttempt = 0; validMinePositionAttempt < 10; validMinePositionAttempt++) {
                    Random random = new Random();
                    int randX = pos.getX() - 200 + random.nextInt(400);
                    int randY = pos.getY() - 200 + random.nextInt(400);
                    Position testPos = new Position(randX, randY);

                    if(baseInfo.getPathFinding().getTilePositionValidator().isWalkable(testPos.toTilePosition())) {
                        currentMinePos = testPos;
                        break;
                    }

                    validMinePositionAttempt++;
                }

                if(currentMinePos != null) {
                    unit.useTech(TechType.Spider_Mines, currentMinePos);
                    layingMines = true;
                    currentMinePos = null;
                }
            }
        }
    }

    private void layMinesOnEnemy() {
        if(enemyUnit.getEnemyType().isBuilding()) {
            return;
        }

        if(mineTimer >= 192) {
            mineTimer = 0;
            recentlyMined = false;
        }

        if(recentlyMined) {
            mineTimer += 8;
            return;
        }

        if(unit.getDistance(enemyUnit.getEnemyPosition()) < 200) {
            if(enemyUnit.getEnemyType().groundWeapon().maxRange() <= 64) {
                Position minePos = kiteTo(64);
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
        if(recentlyMined) {
            return false;
        }

        int currentFrame = game.getFrameCount();
        int idOffset = (10 + (unit.getID() % 21)) * 24;
        if((currentFrame - mineCycle + idOffset) % FULL_MINE_CYCLE < ALLOWED_MINE_CYCLE) {
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
