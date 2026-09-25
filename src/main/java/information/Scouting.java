package information;

import java.util.ArrayList;
import java.util.HashSet;

import bwapi.Game;
import bwapi.Player;
import bwapi.Position;
import bwapi.TechType;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import information.enemy.EnemyUnits;
import map.bwemwrappers.Base;
import map.bwemwrappers.Geyser;
import map.bwemwrappers.Mineral;
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
    private int scoutRadius = 250;
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

    private EnemyUnits headingUnit = null;
    private Position headingStart = null;
    private int headingStartFrame = 0;
    private Base inferredEnemyMain = null;
    private ArrayList<Position> enemyMainPerimeter = new ArrayList<>();

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

        if (inferencePending()) {
            scoutTargetBase = inferredEnemyMain;
            scout.getUnit().move(inferredEnemyMain.getCenter());
            return;
        }

        if (mapInfo.getStartingBases().size() == 3) {
            Base diagonalBase = getDiagonalBase();

            Base furthestAdjacent = null;
            int furthestGroundDistance = Integer.MIN_VALUE;

            for (Base base : mapInfo.getStartingBases()) {
                if (base == diagonalBase) {
                    continue;
                }

                int groundDistance = base.getGroundDistanceFromMain();
                if (groundDistance > furthestGroundDistance) {
                    furthestGroundDistance = groundDistance;
                    furthestAdjacent = base;
                }
            }

            if (furthestAdjacent != null && !mapInfo.isExplored(furthestAdjacent)) {
                scoutTargetBase = furthestAdjacent;
                scout.getUnit().move(furthestAdjacent.getCenter());
                return;
            }

            if (diagonalBase != null && !mapInfo.isExplored(diagonalBase)) {
                scoutTargetBase = diagonalBase;
                scout.getUnit().move(diagonalBase.getCenter());
                return;
            }
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

    private Position enemyMainLoopTarget(Workers scoutWorker, boolean directionReversed) {
        Base enemyMain = mapInfo.getEnemyMain();
        if (enemyMain == null || enemyMain.getArea() == null) {
            return null;
        }

        HashSet<TilePosition> enemyMainTiles = mapInfo.getBaseTilesAllBases().get(enemyMain);
        if (enemyMainTiles == null || !enemyMainTiles.contains(scoutWorker.getUnit().getTilePosition())) {
            return null;
        }

        boolean enemyNearby = false;
        for (EnemyUnits enemyUnit : gameState.getKnownEnemyUnits()) {
            if (enemyUnit.getEnemyType().isBuilding() || !enemyUnit.getEnemyType().canAttack() || !enemyUnit.getEnemyUnit().isVisible()) {
                continue;
            }

            if (scoutWorker.getUnit().getDistance(enemyUnit.getEnemyUnit()) <= 64) {
                enemyNearby = true;
                break;
            }
        }

        if (!enemyNearby) {
            return null;
        }

        if (enemyMainPerimeter.isEmpty()) {
            Position origin = enemyMain.getArea().getTop();

            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 * i) / 16;
                double stepX = Math.cos(angle) * 16;
                double stepY = Math.sin(angle) * 16;

                int steps = 0;
                while (enemyMainTiles.contains(new Position((int) (origin.getX() + stepX * steps), (int) (origin.getY() + stepY * steps)).toTilePosition())) {
                    steps++;
                }

                for (int inset = steps - 4; inset >= 0; inset--) {
                    TilePosition candidateTile = new Position((int) (origin.getX() + stepX * inset), (int) (origin.getY() + stepY * inset)).toTilePosition();

                    if (!enemyMainTiles.contains(candidateTile) || !mapInfo.getPathFinding().getTilePositionValidator().isWalkable(candidateTile)) {
                        continue;
                    }

                    Position waypoint = new Position(candidateTile.toPosition().getX() + 16, candidateTile.toPosition().getY() + 16);
                    if (!enemyMainPerimeter.contains(waypoint)) {
                        enemyMainPerimeter.add(waypoint);
                    }
                    break;
                }
            }
        }

        if (enemyMainPerimeter.isEmpty()) {
            return null;
        }

        int nearestIndex = 0;
        int nearestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < enemyMainPerimeter.size(); i++) {
            int distance = scoutWorker.getUnit().getDistance(enemyMainPerimeter.get(i));
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestIndex = i;
            }
        }

        int step = 1;
        if (directionReversed) {
            step = -1;
        }

        int perimeterSize = enemyMainPerimeter.size();
        return enemyMainPerimeter.get((nearestIndex + step + perimeterSize) % perimeterSize);
    }

    private void scoutEnemyPerimeter() {
        if (scout == null) {
            return;
        }

        if (scout.getUnit().isIdle()) {
            scout.setIdleClock(scout.getIdleClock() + 1);
        }

        if (scout.getIdleClock() >= 48) {
            reversed = !reversed;
            scout.setIdleClock(0);
        }

        Position loopTarget = enemyMainLoopTarget(scout, reversed);
        if (loopTarget != null) {
            scout.getUnit().rightClick(loopTarget);
            return;
        }

        if (scout.getEnemyUnit() != null) {
            return;
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

        if (secondScout.getUnit().isIdle()) {
            secondScout.setIdleClock(secondScout.getIdleClock() + 1);
        }

        if (secondScout.getIdleClock() >= 48) {
            secondScoutReversed = !secondScoutReversed;
            secondScout.setIdleClock(0);
        }

        Position loopTarget = enemyMainLoopTarget(secondScout, secondScoutReversed);
        if (loopTarget != null) {
            secondScout.getUnit().rightClick(loopTarget);
            return;
        }

        if (secondScout.getEnemyUnit() != null) {
            return;
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

    private Base getDiagonalBase() {
        Position oppositePosition = new Position((game.mapWidth() * 32) - mapInfo.getStartingBase().getCenter().getX(), (game.mapHeight() * 32) - mapInfo.getStartingBase().getCenter().getY());

        Base diagonalBase = null;
        int diagonalDistance = Integer.MAX_VALUE;

        for (Base base : mapInfo.getStartingBases()) {
            int distance = base.getCenter().getApproxDistance(oppositePosition);
            if (distance < diagonalDistance) {
                diagonalDistance = distance;
                diagonalBase = base;
            }
        }

        return diagonalBase;
    }

    private void sendSecondScout() {
        Base diagonalBase = null;
        if (mapInfo.getStartingBases().size() == 3) {
            diagonalBase = getDiagonalBase();
        }

        Base nearestRemaining = null;
        int nearestGroundDistance = Integer.MAX_VALUE;

        for (Base base : mapInfo.getStartingBases()) {
            if (mapInfo.isExplored(base)) {
                continue;
            }
            if (base == scoutTargetBase) {
                continue;
            }
            if (base == diagonalBase) {
                continue;
            }

            int groundDistance = base.getGroundDistanceFromMain();
            if (groundDistance < nearestGroundDistance) {
                nearestGroundDistance = groundDistance;
                nearestRemaining = base;
            }
        }

        if (nearestRemaining == null && secondScout == null) {
            return;
        }

        if (secondScout == null && !inferencePending()) {
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

        if (inferencePending() && scout != null
                && secondScout.getUnit().getDistance(inferredEnemyMain.getCenter()) < scout.getUnit().getDistance(inferredEnemyMain.getCenter())) {
            Workers oldScout = scout;
            scout = secondScout;
            secondScout = null;
            oldScout.setWorkerStatus(WorkerStatus.MINERALS);
            scoutTargetBase = inferredEnemyMain;
            scout.getUnit().move(inferredEnemyMain.getCenter());
            return;
        }

        if (nearestRemaining != null) {
            secondScout.getUnit().move(nearestRemaining.getCenter());
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
        CombatUnits scanner = gameState.getCombatUnits().stream()
                .filter(cu -> cu.getUnitType() == UnitType.Terran_Comsat_Station
                        && cu.getUnit().isCompleted()
                        && cu.getUnit().getEnergy() >= 50)
                .findFirst()
                .orElse(null);

        if (scanner == null) {
            return;
        }

        if (!scanner.getUnit().useTech(TechType.Scanner_Sweep, basePosition)) {
            return;
        }

        if (mapInfo.getEnemyMain() != null && mapInfo.getEnemyMain().getCenter().getDistance(basePosition) < 100) {
            mainScanned = true;
        }
        else if (mapInfo.getEnemyNatural() != null && mapInfo.getEnemyNatural().getCenter().getDistance(basePosition) < 100) {
            naturalScanned = true;
        }
    }

    private boolean inferencePending() {
        return inferredEnemyMain != null
                && !mapInfo.isExplored(inferredEnemyMain)
                && gameState.getStartingEnemyBase() == null;
    }

    private double rayAngle(Position origin, double rayX, double rayY, Position target) {
        double toTargetX = target.getX() - origin.getX();
        double toTargetY = target.getY() - origin.getY();
        double cross = Math.abs((rayX * toTargetY) - (rayY * toTargetX));
        double dot = (rayX * toTargetX) + (rayY * toTargetY);
        return Math.atan2(cross, dot);
    }

    private Base closestBaseOnRay(Position origin, double rayX, double rayY) {
        Base bestBase = null;
        double bestAngle = Math.PI / 2;

        for (Base base : mapInfo.getStartingBases()) {
            if (mapInfo.isExplored(base)) {
                continue;
            }

            double angle = rayAngle(origin, rayX, rayY, base.getCenter());
            if (angle < bestAngle) {
                bestAngle = angle;
                bestBase = base;
            }
        }

        return bestBase;
    }

    private void sampleEnemyHeading() {
        if (inferredEnemyMain != null || gameState.getStartingEnemyBase() != null) {
            return;
        }

        Base naturalInferredMain = mapInfo.getEnemyMain();
        if (naturalInferredMain != null
                && mapInfo.getStartingBases().contains(naturalInferredMain)
                && !mapInfo.isExplored(naturalInferredMain)) {
            inferredEnemyMain = naturalInferredMain;
            headingUnit = null;
            return;
        }

        int frameCount = game.getFrameCount();

        if (headingUnit == null) {
            for (EnemyUnits enemyUnit : gameState.getKnownEnemyUnits()) {
                if (!enemyUnit.getEnemyUnit().isVisible() || enemyUnit.getEnemyPosition() == null) {
                    continue;
                }

                TilePosition enemyTile = enemyUnit.getEnemyTilePosition();

                boolean overlordSighting = enemyUnit.getEnemyType() == UnitType.Zerg_Overlord
                        && time.lessThanOrEqual(new Time(2, 45))
                        && enemyUnit.getEnemyPosition().getDistance(mapInfo.getStartingBase().getCenter()) <= 800;

                boolean workerSighting = enemyUnit.getEnemyType().isWorker()
                        && time.lessThanOrEqual(new Time(2, 30))
                        && !mapInfo.getBaseTiles().contains(enemyTile)
                        && !mapInfo.getNaturalTiles().contains(enemyTile)
                        && mapInfo.getStartingBases().stream().noneMatch(base -> mapInfo.getBaseTilesAllBases().get(base) != null
                                && mapInfo.getBaseTilesAllBases().get(base).contains(enemyTile));

                if (!overlordSighting && !workerSighting) {
                    continue;
                }

                headingUnit = enemyUnit;
                headingStart = enemyUnit.getEnemyPosition();
                headingStartFrame = frameCount;
                return;
            }
            return;
        }

        boolean overlordHeading = headingUnit.getEnemyType() == UnitType.Zerg_Overlord;
        int elapsedFrames = frameCount - headingStartFrame;

        if (headingUnit.getEnemyUnit().isVisible()
                && ((overlordHeading && elapsedFrames < 72) || (!overlordHeading && elapsedFrames < 48))) {
            return;
        }

        Position headingEnd = headingUnit.getEnemyPosition();
        headingUnit = null;

        if (headingEnd == null) {
            return;
        }

        double displacement = headingStart.getDistance(headingEnd);
        if ((overlordHeading && displacement < 25) || (!overlordHeading && displacement < 50)) {
            return;
        }

        double headingX = headingEnd.getX() - headingStart.getX();
        double headingY = headingEnd.getY() - headingStart.getY();

        Base backwardBase = closestBaseOnRay(headingStart, -headingX, -headingY);
        double ownMainAngle = rayAngle(headingStart, -headingX, -headingY, mapInfo.getStartingBase().getCenter());

        if (ownMainAngle < Math.PI / 2
                && (backwardBase == null || ownMainAngle < rayAngle(headingStart, -headingX, -headingY, backwardBase.getCenter()))) {
            inferredEnemyMain = closestBaseOnRay(headingStart, headingX, headingY);
            return;
        }

        inferredEnemyMain = backwardBase;
    }

    public void onFrame() {
        time = new Time(game.getFrameCount());

        sampleEnemyHeading();

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
            scoutTargetBase = null;
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
