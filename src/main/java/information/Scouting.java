package information;

import bwapi.Game;
import bwapi.Player;
import bwapi.Position;
import bwapi.TechType;
import bwapi.Unit;
import bwapi.UnitType;
import bwem.Base;
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
    private int scoutRadius = 200;
    private int positionCount = 8;
    private int currentPositionIndex = 0;
    private int scoutingAttempts = 0;
    private boolean completedScout = false;
    private boolean attemptsMaxed = false;
    private boolean mainScanned = false;
    private boolean reversed = false;

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

        for (Base startingBase : mapInfo.getStartingBases()) {
            if (startingBase == mapInfo.getStartingBase()) {
                continue;
            }

            if (!mapInfo.isExplored(startingBase)) {
                scout.getUnit().move(startingBase.getCenter());
            }
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

    private void scoutEnemyPerimeter() {
        if (scout == null) {
            return;
        }

        if (scout.getUnit().isAttacking()) {
            scout.getUnit().stop();
        }

        if (scout.getUnit().isIdle()) {
            scout.setIdleClock(scout.getIdleClock() + 1);
        }
        // else {
        //     scout.setIdleClock(0);
        // }

        if (scout.getIdleClock() >= 48) {
            reversed = !reversed;
            scout.setIdleClock(0);
        }

        Position enemyBasePos = gameState.getStartingEnemyBase().getEnemyPosition();
        double angle = (Math.PI * 2 * currentPositionIndex) / positionCount;

        int x = (int) (enemyBasePos.getX() + scoutRadius * Math.cos(angle));
        int y = (int) (enemyBasePos.getY() + scoutRadius * Math.sin(angle));

        Position targetPosition = new Position(x, y);

        if (scout.getUnit().getDistance(targetPosition) < 90) {
            if (reversed) {
                currentPositionIndex = (currentPositionIndex - 1 + positionCount) % positionCount;
            }
            else {
                currentPositionIndex = (currentPositionIndex + 1) % positionCount;
            }
            angle = (Math.PI * 2 * currentPositionIndex) / positionCount;
            x = (int) (enemyBasePos.getX() + scoutRadius * Math.cos(angle));
            y = (int) (enemyBasePos.getY() + scoutRadius * Math.sin(angle));
            targetPosition = new Position(x, y);
        }

        scout.getUnit().rightClick(targetPosition);
    }

    private void scanEnemyBase() {
        if (gameState.getStartingEnemyBase() == null) {
            return;
        }

        if (gameState.getCombatUnits().stream().filter(cu -> cu.getUnitType() == UnitType.Terran_Comsat_Station).findFirst().orElse(null) == null) {
            return;
        }

        System.out.println("Scanning enemy base at " + time);

        scanBase(gameState.getStartingEnemyBase().getEnemyPosition());
        mainScanned = true;
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

        if (gameState.getStartingEnemyBase() != null) {
            scoutEnemyPerimeter();
            completedScout = true;
        }

        if (gameState.getEnemyOpener() != null) {
            completedScout = true;
        }

        if (gameState.getEnemyOpener() == null && !mainScanned && time.greaterThan(new Time(5,0))) {
            scanEnemyBase();
        }
        
        if (gameState.getStartingEnemyBase() == null && time.greaterThan(new Time(5,0))) {
            scanRemainingMains();
        }
    }

    public void onEnemyDestroy(Unit unit) {
        if (scout == null) {
            return;
        }

        if (unit.getID() == scout.getUnit().getID()) {
            scout = null;
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
