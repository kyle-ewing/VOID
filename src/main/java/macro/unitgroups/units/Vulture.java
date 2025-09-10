package macro.unitgroups.units;

import bwapi.*;
import bwem.Base;
import information.EnemyInformation;
import macro.unitgroups.CombatUnits;
import macro.unitgroups.UnitStatus;
import java.util.List;

public class Vulture extends CombatUnits {
    private EnemyInformation enemyInformation;
    private List<Position> minePositions;
    private Position currentMinePos = null;
    private int mineCount = 3;
    private int pulseCheck = 0;
    private boolean layingMines = false;

    public Vulture(Game game, EnemyInformation enemyInformation, Unit unit) {
        super(game, unit);
        this.enemyInformation = enemyInformation;
        unitStatus = UnitStatus.ATTACK;
        calculateMinePositions();
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

        if(minimnumThreshold(1.05)) {
            unit.patrol(kiteTo());
        }
        else if(minimnumThreshold(0.5)) {
            unit.move(kiteTo());
        }
        else {
            unit.move(enemyUnit.getEnemyPosition());
        }

        if(game.self().hasResearched(TechType.Spider_Mines) && unit.getSpiderMineCount() > 0 && !unit.isAttacking()) {
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

        for(Base base : enemyInformation.getBaseInfo().getPotentialMinePaths().getPathLists().keySet()) {
            if(enemyInformation.getStartingEnemyBase().getEnemyPosition().equals(base.getCenter())) {
               minePositions = enemyInformation.getBaseInfo().getPotentialMinePaths().getPathLists().get(base);
            }
        }
    }

    public void layMinesAtChokepoints() {
        for(Position pos : minePositions) {
            if(unit.getDistance(pos) < 100) {

                for(int validMinePositionAttempt = 0; validMinePositionAttempt < 10; validMinePositionAttempt++) {
                    java.util.Random random = new java.util.Random();
                    int randX = pos.getX() - 200 + random.nextInt(400);
                    int randY = pos.getY() - 200 + random.nextInt(400);
                    Position testPos = new Position(randX, randY);

                    if(enemyInformation.getBaseInfo().getPathFinding().getTilePositionValidator().isWalkable(testPos.toTilePosition())) {
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
}
