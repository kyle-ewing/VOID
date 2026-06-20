package information;

import bwapi.Game;
import bwapi.Player;
import bwapi.Position;
import bwapi.Race;
import bwapi.TechType;
import bwapi.Unit;
import bwapi.UnitType;
import map.bwemwrappers.Base;
import map.bwemwrappers.Geyser;
import map.bwemwrappers.Mineral;
import information.enemy.enemyopeners.EnemyStrategyName;
import unitgroups.units.CombatUnits;
import unitgroups.units.WorkerStatus;
import unitgroups.units.Workers;
import util.Time;

//TODO: move scouting into gamestate
public class Scouting {
    private Game game;
    private GameState gameState;
    private MapInfo mapInfo;
    private Player player;

    private Workers scout;
    private Workers secondScout = null;
    private Base scoutTargetBase = null;
    private int scoutRadius = 200;
    private int positionCount = 8;
    private int currentPositionIndex = 0;
    private int secondScoutPositionIndex = 0;
    private int scoutingAttempts = 0;
    private boolean completedScout = false;
    private boolean attemptsMaxed = false;
    private boolean mainScanned = false;
    private boolean reversed = false;
    private boolean secondScoutReversed = false;
    private boolean secondScoutSent = false;
    private boolean enemyBaseLocated = false;
    private boolean secondScoutFoundEnemy = false;
    private boolean naturalScanned = false;

    private Time time;

    public Scouting(Game game, MapInfo mapInfo, GameState gameState) {
        this.game = game;
        this.mapInfo = mapInfo;
        this.gameState = gameState;
        this.player = game.self();

        this.time = new Time(game.getFrameCount());
    }

    public void sendScout() {
        if (scoutingAttempts >= 2) {
            attemptsMaxed = true;
            return;
        }

        if (scout == null) {
            selectScout();
        }

        if (scout == null) {
            return;
        }

        Base closest = null;
        int closestDistance = Integer.MAX_VALUE;

        for (Base base : mapInfo.getStartingBases()) {
            if (mapInfo.isExplored(base)) {
                continue;
            }

            int distance = scout.getUnit().getDistance(base.getCenter());
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = base;
            }
        }

        if (closest != null) {
            scoutTargetBase = closest;
            scout.getUnit().move(closest.getCenter());
        }
    }


    private void selectScout() {
        for (Workers scv: gameState.getWorkers()) {
            if (scv.getWorkerStatus() == WorkerStatus.MINERALS) {
                scoutingAttempts++;
                scout = scv;
                scv.setWorkerStatus(WorkerStatus.SCOUTING);
                break;
            }
        }
    }

    private boolean isOpenSideIndex(int index) {
        Base enemyMain = mapInfo.getEnemyMain();
        if (enemyMain == null) {
            return true;
        }

        if (enemyMain.getMinerals().isEmpty() && enemyMain.getGeysers().isEmpty()) {
            return true;
        }

        double resourceSumX = 0;
        double resourceSumY = 0;
        int resourceCount = 0;

        for (Mineral mineral : enemyMain.getMinerals()) {
            resourceSumX += mineral.getCenter().getX();
            resourceSumY += mineral.getCenter().getY();
            resourceCount++;
        }

        for (Geyser geyser : enemyMain.getGeysers()) {
            resourceSumX += geyser.getCenter().getX();
            resourceSumY += geyser.getCenter().getY();
            resourceCount++;
        }

        double mineralDirectionX = (resourceSumX / resourceCount) - enemyMain.getCenter().getX();
        double mineralDirectionY = (resourceSumY / resourceCount) - enemyMain.getCenter().getY();

        double angle = (Math.PI * 2 * index) / positionCount;
        double offsetX = Math.cos(angle);
        double offsetY = Math.sin(angle);
        double dot = (offsetX * mineralDirectionX) + (offsetY * mineralDirectionY);

        return dot <= 0;
    }

    private int nextPerimeterIndex(int currentIndex, boolean directionReversed) {
        int step = 1;
        if (directionReversed) {
            step = -1;
        }

        int nextIndex = (currentIndex + step + positionCount) % positionCount;

        for (int attempt = 0; attempt < positionCount; attempt++) {
            if (isOpenSideIndex(nextIndex)) {
                return nextIndex;
            }

            nextIndex = (nextIndex + step + positionCount) % positionCount;
        }

        return nextIndex;
    }

    private void scoutEnemyPerimeter() {
        if (scout == null) {
            return;
        }

        if (scout.getEnemyUnit() != null) {
            return;
        }

        if (scout.getUnit().isIdle()) {
            scout.setIdleClock(scout.getIdleClock() + 1);
        }

        if (scout.getIdleClock() >= 48) {
            reversed = !reversed;
            scout.setIdleClock(0);
        }

        Position enemyBasePos = gameState.getStartingEnemyBase().getEnemyPosition();

        if (!isOpenSideIndex(currentPositionIndex)) {
            currentPositionIndex = nextPerimeterIndex(currentPositionIndex, reversed);
        }

        double angle = (Math.PI * 2 * currentPositionIndex) / positionCount;

        int x = (int) (enemyBasePos.getX() + scoutRadius * Math.cos(angle));
        int y = (int) (enemyBasePos.getY() + scoutRadius * Math.sin(angle));

        Position targetPosition = new Position(x, y);

        if (scout.getUnit().getDistance(targetPosition) < 90) {
            currentPositionIndex = nextPerimeterIndex(currentPositionIndex, reversed);
            angle = (Math.PI * 2 * currentPositionIndex) / positionCount;
            x = (int) (enemyBasePos.getX() + scoutRadius * Math.cos(angle));
            y = (int) (enemyBasePos.getY() + scoutRadius * Math.sin(angle));
            targetPosition = new Position(x, y);
        }

        scout.getUnit().rightClick(targetPosition);
    }

    private void scoutEnemyPerimeterSecond() {
        if (secondScout == null) {
            return;
        }

        if (secondScout.getEnemyUnit() != null) {
            return;
        }

        if (secondScout.getUnit().isIdle()) {
            secondScout.setIdleClock(secondScout.getIdleClock() + 1);
        }

        if (secondScout.getIdleClock() >= 48) {
            secondScoutReversed = !secondScoutReversed;
            secondScout.setIdleClock(0);
        }

        Position enemyBasePos = gameState.getStartingEnemyBase().getEnemyPosition();

        if (!isOpenSideIndex(secondScoutPositionIndex)) {
            secondScoutPositionIndex = nextPerimeterIndex(secondScoutPositionIndex, secondScoutReversed);
        }

        double angle = (Math.PI * 2 * secondScoutPositionIndex) / positionCount;

        int x = (int) (enemyBasePos.getX() + scoutRadius * Math.cos(angle));
        int y = (int) (enemyBasePos.getY() + scoutRadius * Math.sin(angle));

        Position targetPosition = new Position(x, y);

        if (secondScout.getUnit().getDistance(targetPosition) < 90) {
            secondScoutPositionIndex = nextPerimeterIndex(secondScoutPositionIndex, secondScoutReversed);
            angle = (Math.PI * 2 * secondScoutPositionIndex) / positionCount;
            x = (int) (enemyBasePos.getX() + scoutRadius * Math.cos(angle));
            y = (int) (enemyBasePos.getY() + scoutRadius * Math.sin(angle));
            targetPosition = new Position(x, y);
        }

        secondScout.getUnit().rightClick(targetPosition);
    }

    private void sendSecondScout() {
        if (secondScout == null) {
            for (Workers scv : gameState.getWorkers()) {
                if (scv.getWorkerStatus() == WorkerStatus.MINERALS) {
                    secondScout = scv;
                    scv.setWorkerStatus(WorkerStatus.SCOUTING);
                    break;
                }
            }
        }

        if (secondScout == null) {
            return;
        }

        for (Base base : mapInfo.getStartingBases()) {
            if (mapInfo.isExplored(base)) {
                continue;
            }
            if (base == scoutTargetBase) {
                continue;
            }
            secondScout.getUnit().move(base.getCenter());
            return;
        }

        if (scoutTargetBase != null && scout != null
                && secondScout.getUnit().getDistance(scoutTargetBase.getCenter()) < scout.getUnit().getDistance(scoutTargetBase.getCenter())) {
            Workers oldScout = scout;
            scout = secondScout;
            secondScout = null;
            oldScout.setWorkerStatus(WorkerStatus.MINERALS);
            scout.getUnit().move(scoutTargetBase.getCenter());
            secondScoutSent = false;
            return;
        }

        returnSecondScoutHome();
    }

    private void returnFirstScoutHome() {
        if (scout == null) {
            return;
        }
        scout.setWorkerStatus(WorkerStatus.MINERALS);
        scout = null;
        scoutTargetBase = null;
    }

    private void returnSecondScoutHome() {
        if (secondScout == null) {
            return;
        }
        secondScout.setWorkerStatus(WorkerStatus.MINERALS);
        secondScout = null;
    }

    private void locateEnemyBase() {
        if (enemyBaseLocated) {
            return;
        }

        enemyBaseLocated = true;

        if (!secondScoutSent) {
            return;
        }

        if (scout == null && secondScout != null) {
            secondScoutFoundEnemy = true;
            return;
        }

        if (secondScout == null) {
            return;
        }

        Position enemyPos = gameState.getStartingEnemyBase().getEnemyPosition();
        secondScoutFoundEnemy = secondScout.getUnit().getDistance(enemyPos) < scout.getUnit().getDistance(enemyPos);
    }

    private void scanEnemyBase(Position enemyBasePos) {
        if (enemyBasePos == null) {
            return;
        }

        if (gameState.getCombatUnits().stream().filter(cu -> cu.getUnitType() == UnitType.Terran_Comsat_Station).findFirst().orElse(null) == null) {
            return;
        }

        scanBase(enemyBasePos);
    }

    private void scanRemainingMains() {
        for (Base startingBase : mapInfo.getStartingBases()) {
            if (startingBase == mapInfo.getStartingBase()) {
                continue;
            }

            if (!mapInfo.isExplored(startingBase)) {
                scanBase(startingBase.getCenter());
            }
        }
    }

    private void scanBase(Position basePosition) {
        CombatUnits scanner = gameState.getCombatUnits().stream().filter(cu -> cu.getUnitType() == UnitType.Terran_Comsat_Station).findFirst().orElse(null);

        if (scanner == null || !scanner.getUnit().isCompleted()) {
            return;
        }

        if (scanner.getUnit().getEnergy() < 50) {
            return;
        }

        if (mapInfo.getEnemyMain() != null && mapInfo.getEnemyMain().getCenter().getDistance(scanner.getUnit().getPosition()) < 100) {
            mainScanned = true;
        }
        else if (mapInfo.getEnemyNatural() != null && mapInfo.getEnemyNatural().getCenter().getDistance(scanner.getUnit().getPosition()) < 100) {
            naturalScanned = true;
        }
        scanner.getUnit().useTech(TechType.Scanner_Sweep, basePosition);
    }

    public void onFrame() {
        time = new Time(game.getFrameCount());

        if (player.supplyUsed() / 2 >= gameState.getStartingOpener().getScoutSupply() && gameState.getStartingEnemyBase() == null) {
            sendScout();
        }

        if (gameState.getStartingEnemyBase() != null
                && time.greaterThan(new Time(2, 45))
                && time.lessThanOrEqual(new Time(5, 0))
                && gameState.getEnemyOpener() == null
                && scout == null) {
            sendScout();
        }

        if (!secondScoutSent
                && mapInfo.getStartingBases().size() == 2
                && scout != null
                && gameState.getStartingEnemyBase() == null) {
            secondScoutSent = true;
        }

        if (!secondScoutSent
                && mapInfo.getStartingBases().size() == 3
                && ((game.enemy().getRace() == Race.Protoss && gameState.getEnemyOpener() == null)
                    || (gameState.getEnemyOpener() != null && gameState.getEnemyOpener().getStrategyName() == EnemyStrategyName.GASSTEAL))
                && gameState.getStartingEnemyBase() == null
                && mapInfo.getStartingBases().stream().anyMatch(b -> mapInfo.isExplored(b))) {
            secondScoutSent = true;
        }

        if (secondScoutSent && gameState.getStartingEnemyBase() == null) {
            sendSecondScout();
        }

        if (gameState.getStartingEnemyBase() != null) {
            locateEnemyBase();

            if (secondScoutSent && secondScoutFoundEnemy) {
                returnFirstScoutHome();
                scoutEnemyPerimeterSecond();
            }
            else if (secondScoutSent && !secondScoutFoundEnemy) {
                returnSecondScoutHome();
                scoutEnemyPerimeter();
            }
            else {
                scoutEnemyPerimeter();
            }
            completedScout = true;
        }

        if (gameState.getEnemyOpener() != null) {
            completedScout = true;
        }

        if (gameState.getEnemyOpener() == null 
                && gameState.getStartingEnemyBase() != null
                && !mainScanned && time.greaterThan(new Time(5,0))) {
            scanEnemyBase(gameState.getStartingEnemyBase().getEnemyPosition());
        }

        if (gameState.getStartingEnemyBase() == null && time.greaterThan(new Time(5,0))) {
            scanRemainingMains();
        }

        if (!naturalScanned
            && mapInfo.getEnemyNatural() != null
            && time.greaterThan(new Time(9, 0))) {
            scanEnemyBase(mapInfo.getEnemyNatural().getCenter());
        }
    }

    public void onEnemyDestroy(Unit unit) {
        if (scout != null && unit.getID() == scout.getUnit().getID()) {
            scout = null;
        }

        if (secondScout != null && unit.getID() == secondScout.getUnit().getID()) {
            secondScout = null;
        }
    }

    public boolean isCompletedScout() {
        return completedScout;
    }

    public boolean attemptsMaxed() {
        return attemptsMaxed;
    }

    public Workers getScout() {
        return scout;
    }
}
