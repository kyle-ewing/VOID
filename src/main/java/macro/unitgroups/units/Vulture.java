package macro.unitgroups.units;

import bwapi.*;
import bwem.Base;
import bwem.ChokePoint;
import information.BaseInfo;
import information.EnemyInformation;
import macro.unitgroups.CombatUnits;
import macro.unitgroups.UnitStatus;
import util.Time;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Vulture extends CombatUnits {
    private EnemyInformation enemyInformation;
    private BaseInfo baseInfo;
    private List<Position> minePositions;
    private Position currentMinePos = null;
    private int mineCount = 3;
    private int pulseCheck = 0;
    private boolean layingMines = false;

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
    }

    @Override
    public void retreat() {
        if(enemyUnit == null || rallyPoint == null) {
            return;
        }

        if(!inRangeOfThreat && isOutRanged()) {
            unit.move(rallyPoint.toPosition());
        }


        if(!inRangeOfThreat && (!isOutRanged() || hasTankSupport)) {
            setUnitStatus(UnitStatus.ATTACK);
        }
    }

    @Override
    public void attack() {
        if(enemyUnit == null) {
            return;
        }

        if(mineCount > unit.getSpiderMineCount()) {
            mineCount = unit.getSpiderMineCount();
            layingMines = false;
        }

        if(pulseCheck > 72) {
            pulseCheck = 0;
            layingMines = false;
        }

        if(layingMines) {
            pulseCheck += 12;
            return;
        }

        if(isOutRanged() && !hasTankSupport) {
            unitStatus = UnitStatus.RETREAT;
        }

        if(minimnumThreshold(1.05)) {
            unit.patrol(kiteTo());
        }
        else if(minimnumThreshold(0.5)) {
            unit.move(kiteTo());
        }
        else {
            unit.move(enemyUnit.getEnemyPosition());
        }

        if(game.self().hasResearched(TechType.Spider_Mines) && unit.getSpiderMineCount() > 0 && allowMineLaying()) {
            layMinesAtChokepoints();
        }
    }

    private Position kiteTo() {
        Position enemyPos = enemyUnit.getEnemyPosition();
        Position unitPos = unit.getPosition();
        int dx = unitPos.getX() - enemyPos.getX();
        int dy = unitPos.getY() - enemyPos.getY();

        int patrolDistance = weaponRange();
        double moveX = unitPos.getX() + (dx * patrolDistance / Math.max(1, unitPos.getDistance(enemyPos)));
        double moveY = unitPos.getY() + (dy * patrolDistance / Math.max(1, unitPos.getDistance(enemyPos)));

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

        for(Base base : baseInfo.getAllBasePaths().getPathLists().keySet()) {
            if(enemyInformation.getStartingEnemyBase().getEnemyPosition().equals(base.getCenter())) {
                List<Position> allPositions = baseInfo.getAllBasePaths().getPathLists().get(base);
                minePositions = new ArrayList<>();

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
            if(enemyUnit.getEnemyType().isBuilding()) {
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

    private boolean allowMineLaying() {
        int currentFrame = game.getFrameCount();
        int idOffset = (10 + (unit.getID() % 21)) * 24;
        if((currentFrame - mineCycle + idOffset) % FULL_MINE_CYCLE < ALLOWED_MINE_CYCLE) {
            return true;
        }
        return false;
    }
}
