package debug;

import bwapi.*;
import bwem.Base;
import bwem.ChokePoint;
import config.Config;
import information.GameState;
import information.Scouting;
import information.enemy.EnemyUnits;
import unitgroups.units.CombatUnits;
import unitgroups.units.UnitStatus;
import unitgroups.units.Workers;
import planner.PlannedItem;
import util.Time;

import java.util.*;

public class Painters {
    Game game;
    GameState gameState;
    Config config;
    Scouting scouting;


    public Painters(Game game, GameState gameState, Config config, Scouting scouting) {
        this.game = game;
        this.gameState = gameState;
        this.config = config;
        this.scouting = scouting;

        gameSpeed();
    }

    private void gameSpeed() {
        if(config.gameSpeed) {
            game.setLocalSpeed(5);
            game.enableFlag(Flag.UserInput);
        }
    }

    public void onFrame() {
        if(config.debugHud) {
            drawHud();
        }

        if(config.debugProductionQueue) {
            paintProductionQueueReadout(gameState.getProductionQueue());
        }

        if(config.debugCombatUnits) {
            for(CombatUnits unit : gameState.getCombatUnits()) {
                paintUnitStatus(unit);
                paintClosestEnemy(unit);
                paintMedicTarget(unit);
                paintCombatScouts(unit);
                paintStimStatus(unit);
            }
        }

        if(config.debugWorkers) {
            paintWorker(gameState.getWorkers());
            paintWorkerText(gameState.getWorkers());
        }

        if(config.debugEnemyUnits) {
            enemyRangePainter();
            paintThreatUnitsToScreen();
            paintEnemyBuildingsToScreen();
        }

        if(config.debugDetailedUnitInfo) {
            paintCombatUnitValues();
        }

        if(config.debugBuildTiles) {
            paintLargeBuildTiles(gameState.getBuildTiles().getLargeBuildTiles(), Color.Green);
            paintLargeBuildTiles(gameState.getBuildTiles().getLargeBuildTilesNoGap(), Color.Yellow);
            paintMediumBuildTiles(gameState.getBuildTiles().getMediumBuildTiles(), Color.Blue);
        }

        if(config.debugBunkerTiles) {
            paintPaintBunkerTile(gameState.getBunkerPosition());
            paintPaintBunkerTile(gameState.getBuildTiles().getCloseBunkerTile());
        }

        if(config.debugTurretTiles) {
            paintMissileTile(gameState.getBuildTiles().getMainChokeTurret());
            paintMissileTile(gameState.getBuildTiles().getNaturalChokeTurret());
            paintMineralLineTurrets(gameState.getBuildTiles().getMineralLineTurrets());
            paintMainTurrets(gameState.getBuildTiles().getMainTurrets());
        }

        if(config.debugBases) {
            paintNatural(gameState.getBaseInfo().getNaturalBase());
            paintExpansionOrdering(gameState.getBaseInfo().getOrderedExpansions());
            paintExpansionDistances(gameState.getBaseInfo().getOrderedExpansions(), gameState.getBaseInfo().getStartingBase());
//            paintdistanceFromCC(gameState.getBaseInfo().getStartingBase(), 700, Color.Red);
//            paintdistanceFromCC(gameState.getBaseInfo().getNaturalBase(), 400, Color.Red);
        }

        if(config.debugChokes) {
            paintNaturalChoke(gameState.getBaseInfo().getNaturalChoke());
        }

        if(config.debugBaseTiles) {
            paintTileZone(gameState.getBuildTiles().getFrontBaseTiles(), Color.Purple);
            paintTileZone(gameState.getBuildTiles().getBackBaseTiles(), Color.Orange);
            paintTileZone(gameState.getBaseInfo().getNaturalTiles(),  Color.Teal);
            paintTileZone(gameState.getBaseInfo().getMinBaseTiles(), Color.Grey);
        }

        if(config.debugCCExclusionZone) {
            paintTileZone(gameState.getBuildTiles().getCcExclusionTiles(), Color.Red);
        }
        if(config.debugMineralExclusionZone) {
            paintTileZone(gameState.getBuildTiles().getMineralExlusionTiles(), Color.Blue);
        }
        if(config.debugGeyserExclusionZone) {
            paintTileZone(gameState.getBuildTiles().getGeyserExlusionTiles(), Color.Green);
        }

        if(config.debugScout) {
            paintRescoutCriteria();
            if(scouting.getScout() != null)
                paintScoutPath(scouting.getScout().getUnit());
        }
    }


    private void drawHud() {
        game.drawTextScreen(5,15, "Time: " + new Time(game.getFrameCount()) + " Frame: " + game.getFrameCount());

        paintAvailableBuildTiles(gameState.getBuildTiles().getLargeBuildTiles(), gameState.getBuildTiles().getLargeBuildTilesNoGap(), 0, "Production" );
        paintAvailableBuildTiles(gameState.getBuildTiles().getMediumBuildTiles(), 15, "Medium" );

        if (gameState.getEnemyOpener() != null) {
            game.drawTextScreen(5, 60, "Enemy Opener: " + gameState.getEnemyOpener().getStrategyName());
        } else {
            game.drawTextScreen(5, 60, "Enemy Opener: Unknown");
        }
    }

    //Unit Painters
    private void paintUnitStatus(CombatUnits unit) {
        switch(unit.getUnitStatus()) {
            case RALLY:
                game.drawCircleMap(unit.getUnit().getPosition(), 3, Color.White, true);
                break;
            case ATTACK:
                game.drawCircleMap(unit.getUnit().getPosition(), 3, Color.Red, true);
                break;
            case LOAD:
                game.drawCircleMap(unit.getUnit().getPosition(), 3, Color.Blue, true);
                break;
            case DEFEND:
                game.drawCircleMap(unit.getUnit().getPosition(), 3, Color.Purple, true);
                break;
            case SCOUT:
                game.drawCircleMap(unit.getUnit().getPosition(), 3, Color.Orange, true);
                break;
            case RETREAT:
                game.drawCircleMap(unit.getUnit().getPosition(), 3, Color.Teal, true);
                break;
            case OBSTRUCTING:
                game.drawCircleMap(unit.getUnit().getPosition(), 3, Color.Grey, true);
                break;
            case SIEGEDEF:
                game.drawCircleMap(unit.getUnit().getPosition(), 3, Color.Yellow, true);
                break;
            case HUNTING:
                game.drawCircleMap(unit.getUnit().getPosition(), 3, Color.Green, true);
                break;
            case AVOID:
                game.drawCircleMap(unit.getUnit().getPosition(), 3, Color.Black, true);
                break;
        }
    }

    private void paintClosestEnemy(CombatUnits unit) {
        if(unit.getEnemyUnit() != null && unit.getEnemyUnit().getEnemyPosition() != null) {
            game.drawLineMap(unit.getUnit().getPosition(), unit.getEnemyUnit().getEnemyPosition(), Color.Yellow);
        }

    }

    private void paintAttackRange(CombatUnits unit) {
        game.drawCircleMap(unit.getUnit().getPosition(), unit.getUnitType().groundWeapon().maxRange(), Color.Cyan);
    }

    private void paintStimStatus(CombatUnits unit) {
        if(unit.getUnit().isStimmed()) {
            game.drawTextMap(unit.getUnit().getPosition(), "STIMMED" );
        }
    }

    private void paintCombatScouts(CombatUnits unit) {
        if(unit.getUnitStatus() == UnitStatus.SCOUT) {
            game.drawLineMap(unit.getUnit().getPosition(), unit.getUnit().getTargetPosition(), Color.Purple);
        }
    }

    private void paintMedicTarget(CombatUnits unit) {
        if(unit.getUnitType() != UnitType.Terran_Medic) {
            return;
        }

        if(unit.getFriendlyUnit() != null) {
            game.drawLineMap(unit.getUnit().getPosition(), unit.getFriendlyUnit().getUnit().getPosition(), Color.Green);
        }
    }

    private void paintCombatUnitValues() {
        //Set this to track specific unit types and grab one unit to see all values
        UnitType desiredUnitToTrack = UnitType.Terran_Marine;
        CombatUnits trackedUnit = null;

        for(CombatUnits unit : gameState.getCombatUnits()) {
            if(unit.getUnitType() == desiredUnitToTrack) {
                trackedUnit = unit;
                break;
            }
        }

        if(trackedUnit == null) {
            return;
        }

        int yOffset = 200;
        int lineHeight = 12;
        int xStart = 480;

        game.setTextSize(Text.Size.Small);

        //Comment out unneeded values
        game.drawTextScreen(xStart, yOffset, "=== Combat Unit Debug ===");
        yOffset += lineHeight;

        game.drawTextScreen(xStart, yOffset, "Unit ID: " + trackedUnit.getUnitID());
        yOffset += lineHeight;

        game.drawTextScreen(xStart, yOffset, "Unit Type: " + trackedUnit.getUnitType());
        yOffset += lineHeight;

        game.drawTextScreen(xStart, yOffset, "Status: " + trackedUnit.getUnitStatus());
        yOffset += lineHeight;

        game.drawTextScreen(xStart, yOffset, "Rally Point: " + trackedUnit.getRallyPoint());
        yOffset += lineHeight;

//        game.drawTextScreen(xStart, yOffset, "Target Range: " + trackedUnit.getTargetRange());
//        yOffset += lineHeight;

//        game.drawTextScreen(xStart, yOffset, "Reset Clock: " + trackedUnit.getResetClock());
//        yOffset += lineHeight;

//        game.drawTextScreen(xStart, yOffset, "In Bunker: " + trackedUnit.isInBunker());
//        yOffset += lineHeight;

//        game.drawTextScreen(xStart, yOffset, "Enemy In Base: " + trackedUnit.isEnemyInBase());
//        yOffset += lineHeight;

        game.drawTextScreen(xStart, yOffset, "In Range Of Threat: " + trackedUnit.isInRangeOfThreat());
        yOffset += lineHeight;

//        game.drawTextScreen(xStart, yOffset, "Natural Rally Set: " + trackedUnit.isNaturalRallySet());
//        yOffset += lineHeight;

//        game.drawTextScreen(xStart, yOffset, "Has Tank Support: " + trackedUnit.hasTankSupport());
//        yOffset += lineHeight;

//        game.drawTextScreen(xStart, yOffset, "Priority Target Exists: " + trackedUnit.isPriorityTargetExists());
//        yOffset += lineHeight;

//        game.drawTextScreen(xStart, yOffset, "In Base: " + trackedUnit.isInBase());
//        yOffset += lineHeight;

//        game.drawTextScreen(xStart, yOffset, "Has Static Status: " + trackedUnit.hasStaticStatus());
//        yOffset += lineHeight;

//        game.drawTextScreen(xStart, yOffset, "Ignore Priority Target: " + trackedUnit.ignoreCurrentPriorityTarget());
//        yOffset += lineHeight;
//
//        game.drawTextScreen(xStart, yOffset, "Priority Target Lock: " + trackedUnit.priorityTargetLock());
//        yOffset += lineHeight;

        game.drawTextScreen(xStart, yOffset, "Enemy Unit: " + (trackedUnit.getEnemyUnit() != null ? trackedUnit.getEnemyUnit().getEnemyType() : "null"));
        yOffset += lineHeight;

//        game.drawTextScreen(xStart, yOffset, "Priority Enemy: " + (trackedUnit.getPriorityEnemyUnit() != null ? trackedUnit.getPriorityEnemyUnit().getEnemyType() : "null"));
//        yOffset += lineHeight;

        if(trackedUnit.getUnit().getOrderTarget() != null) {
            game.drawTextScreen(xStart, yOffset, "Target Unit: " + trackedUnit.getUnit().getOrderTarget().getType());
            yOffset += lineHeight;
        }

//        game.drawTextScreen(xStart, yOffset, "Friendly Unit: " + (trackedUnit.getFriendlyUnit() != null ? trackedUnit.getFriendlyUnit().getUnitID() : "null"));

        game.setTextSize(Text.Size.Default);

    }

    private void enemyRangePainter() {
        for(EnemyUnits enemyUnit : gameState.getKnownEnemyUnits()) {
            //Change condition to paint specific unit types
            if(enemyUnit.getEnemyType() != UnitType.Zerg_Lurker && enemyUnit.getEnemyType() != UnitType.Terran_Bunker && enemyUnit.getEnemyType() != UnitType.Protoss_Photon_Cannon) {
                continue;
            }

            if(enemyUnit.getEnemyPosition() != null) {
                UnitType enemyType = enemyUnit.getEnemyType();
                if(enemyType.groundWeapon() != null) {
                    int range = enemyType.groundWeapon().maxRange();
                    game.drawCircleMap(enemyUnit.getEnemyPosition(), range, Color.Red);

                    //Optional threat range
                    int threatRange = enemyType.groundWeapon().maxRange() + 150;

                    if(enemyUnit.getEnemyType() == UnitType.Terran_Bunker) {
                        threatRange = UnitType.Terran_Marine.groundWeapon().maxRange() + 200;
                    }
                    game.drawCircleMap(enemyUnit.getEnemyPosition(), threatRange, Color.Orange);

                }
            }
        }
    }

    public void paintThreatUnitsToScreen() {
        int yOffset = 200;
        int lineHeight = 12;
        int xStart = 350;

        game.setTextSize(Text.Size.Small);

        for(EnemyUnits enemyUnit : gameState.getKnownValidThreats()) {
            if(enemyUnit.getEnemyPosition() == null) {
                continue;
            }

            game.drawTextScreen(xStart, yOffset, "Threat Unit: " + enemyUnit.getEnemyType() + " at " + enemyUnit.getEnemyPosition());
            yOffset += lineHeight;
            game.drawCircleMap(enemyUnit.getEnemyPosition(), 5, Color.Red, true);
        }

        game.setTextSize(Text.Size.Default);
    }

    public void paintEnemyBuildingsToScreen() {
        int yOffset = 150;
        int lineHeight = 12;
        int xStart = 5;

        game.setTextSize(Text.Size.Small);

        for(EnemyUnits enemyUnit : gameState.getKnownEnemyUnits()) {
            if(!enemyUnit.getEnemyType().isBuilding()) {
                continue;
            }

            if(enemyUnit.getEnemyPosition() == null) {
                game.drawTextScreen(xStart, yOffset, "Unknown Enemy Unit: " + enemyUnit.getEnemyType());
                yOffset += lineHeight;
                continue;
            }

            game.drawTextScreen(xStart, yOffset, "Known Enemy Building: " + enemyUnit.getEnemyType() + " at " + enemyUnit.getEnemyPosition());
            yOffset += lineHeight;
            game.drawCircleMap(enemyUnit.getEnemyPosition(), 5, Color.Blue, true);

        }

        game.setTextSize(Text.Size.Default);
    }


    //Worker Painters
    private void paintWorker(HashSet<Workers> workers) {
        for(Workers worker : workers) {
            switch (worker.getWorkerStatus()) {
                case MINERALS:
                    paintCircle(worker.getUnit(), 8, Color.Blue);
                    break;
                case SCOUTING:
                    paintCircle(worker.getUnit(), 8, Color.Orange);
                    break;
                case IDLE:
                    paintCircle(worker.getUnit(), 8, Color.White);
                    break;
                case MOVING_TO_BUILD:
                    paintCircle(worker.getUnit(), 8, Color.Purple);
                    break;
                case BUILDING:
                    paintCircle(worker.getUnit(), 8, Color.Yellow);
                    break;
                case GAS:
                    paintCircle(worker.getUnit(), 8, Color.Green);
                    break;
                case ATTACKING:
                    paintCircle(worker.getUnit(), 8, Color.Red);
                    break;
                case REPAIRING:
                    paintCircle(worker.getUnit(), 8, Color.Grey);
                    break;
                case DEFEND:
                    paintCircle(worker.getUnit(), 8, Color.Teal);
                    break;
                case STUCK:
                    paintCircle(worker.getUnit(), 8, Color.Black);
                    break;
            }
        }
    }

    private void paintWorkerText(HashSet<Workers> workers) {
        for(Workers worker : workers) {
            game.drawTextMap(worker.getUnit().getPosition(), worker.getWorkerStatus().toString());
        }
    }

    //BuildTile Painters
    private void paintAvailableBuildTiles(HashSet<TilePosition> buildTiles, int offset, String tileType) {
        game.drawTextScreen(5,30 + offset,   tileType +" Tiles Available: " + buildTiles.size());
    }

    private void paintAvailableBuildTiles(HashSet<TilePosition> buildTiles, HashSet<TilePosition> buildTiles2, int offset, String tileType) {
        int size = buildTiles.size() + buildTiles2.size();
        game.drawTextScreen(5,30 + offset,   tileType +" Tiles Available: " + size);
    }

    private void paintBuildTile(TilePosition tilePosition, UnitType unitType, Color color) {
        game.drawBoxMap(tilePosition.toPosition(), tilePosition.toPosition().add(new Position(unitType.width() + 2, unitType.height() + 2)), color);
    }

    private void paintLargeBuildTiles(HashSet<TilePosition> buildTiles, Color color) {
        for(TilePosition tilePosition : buildTiles) {
            game.drawTextMap(tilePosition.toPosition(), String.valueOf(tilePosition));
            Position start = tilePosition.toPosition();
            Position end = new Position(
                    start.getX() + UnitType.Terran_Barracks.tileWidth() * 32,
                    start.getY() + UnitType.Terran_Barracks.tileHeight() * 32
            );
            game.drawBoxMap(start, end, color);
        }
    }

    private void paintMediumBuildTiles(HashSet<TilePosition> buildTiles, Color color) {
        for(TilePosition tilePosition : buildTiles) {
            game.drawTextMap(tilePosition.toPosition(), String.valueOf(tilePosition));
            Position depotStart = tilePosition.toPosition();
            Position depotEnd = new Position(
                    depotStart.getX() + UnitType.Terran_Supply_Depot.tileWidth() * 32,
                    depotStart.getY() + UnitType.Terran_Supply_Depot.tileHeight() * 32
            );
            game.drawBoxMap(depotStart, depotEnd, color);
        }
    }

    private void paintPaintBunkerTile(TilePosition tilePosition) {
        if(tilePosition == null) {
            return;
        }

        game.drawTextMap(tilePosition.toPosition(), String.valueOf(tilePosition));
        Position start = tilePosition.toPosition();
        Position end = new Position(
                start.getX() + UnitType.Terran_Bunker.tileWidth() * 32,
                start.getY() + UnitType.Terran_Bunker.tileHeight() * 32
        );
        game.drawBoxMap(start, end, Color.Red);
    }

    private void paintMissileTile(TilePosition tilePosition) {
        if(tilePosition == null) {
            return;
        }

        game.drawTextMap(tilePosition.toPosition(), String.valueOf(tilePosition));
        Position start = tilePosition.toPosition();
        Position end = new Position(
                start.getX() + UnitType.Terran_Missile_Turret.tileWidth() * 32,
                start.getY() + UnitType.Terran_Missile_Turret.tileHeight() * 32
        );
        game.drawBoxMap(start, end, Color.Teal);
    }

    private void paintMineralLineTurrets(HashMap<Base, TilePosition> mineralLineTurrets) {
        for(Map.Entry<Base, TilePosition> entry : mineralLineTurrets.entrySet()) {
            TilePosition tilePosition = entry.getValue();

            game.drawTextMap(tilePosition.toPosition(), String.valueOf(tilePosition));
            Position start = tilePosition.toPosition();
            Position end = new Position(
                    start.getX() + UnitType.Terran_Missile_Turret.tileWidth() * 32,
                    start.getY() + UnitType.Terran_Missile_Turret.tileHeight() * 32
            );
            game.drawBoxMap(start, end, Color.Orange);
        }
    }

    private void paintMainTurrets(HashSet<TilePosition> turretTiles) {
        for(TilePosition turretTile : turretTiles) {

            game.drawTextMap(turretTile.toPosition(), String.valueOf(turretTile));
            Position start = turretTile.toPosition();
            Position end = new Position(
                    start.getX() + UnitType.Terran_Missile_Turret.tileWidth() * 32,
                    start.getY() + UnitType.Terran_Missile_Turret.tileHeight() * 32
            );
            game.drawBoxMap(start, end, Color.Orange);
        }
    }

    //Map Painters
    private void paintNatural(Base base) {
        game.drawCircleMap(base.getCenter(), 40, Color.Green);
        game.drawTextMap(base.getCenter().getX(), base.getCenter().getY()  -10, "\u0007 Natural");
    }

    private void paintNaturalChoke(ChokePoint chokePoint) {
        game.drawCircleMap(chokePoint.getCenter().toPosition(), 40, Color.Yellow);
    }

    private void paintBasePosition(HashSet<Base> bases) {
        for(Base base : bases) {
            game.drawTextMap(base.getCenter(), String.valueOf(base.getCenter()));
        }
    }

    private void paintExpansionOrdering(List<Base> orderedExpansions) {
        for(int i = 0; i < orderedExpansions.size(); i++) {
            game.drawTextMap(orderedExpansions.get(i).getCenter(), "Expansion: " + i);
            game.drawCircleMap(orderedExpansions.get(i).getCenter(), 40, Color.Purple);
        }
    }

    private void paintExpansionDistances(List<Base> orderedExpansions, Base startingBase) {
        if(orderedExpansions.size() < 2) {
            return;
        }

        for(Base expansion : orderedExpansions) {
            if(expansion == null) {
                continue;
            }

            int distance = expansion.getCenter().getApproxDistance(startingBase.getCenter());
            game.drawTextMap(expansion.getCenter().getX(), expansion.getCenter().getY() + 10, "Distance from main: " + distance);
        }
    }

    private void paintTileZone(HashSet<TilePosition> buildTiles, Color color) {
        for(TilePosition tile : buildTiles) {
            game.drawBoxMap(tile.toPosition(), tile.toPosition().add(new Position(32, 32)), color);
        }
    }

    private void paintdistanceFromCC(Base base, int distance, Color color) {
        if(base == null) {
            return;
        }
        game.drawCircleMap(base.getCenter(), distance, color);
    }

    //Generic Painters
    private void paintCircle(Unit unit, int radius, Color color) {
        if(unit == null) {
            return;
        }
        game.drawCircleMap(unit.getPosition(), radius, color);
    }

    private void paintCircle(Position position, int radius, Color color) {
        if(position == null) {
            return;
        }
        game.drawCircleMap(position, radius, color);
    }

    //Scouting Painters
    private void paintScoutPath(Unit unit) {
        if(unit == null) {
            return;
        }

        game.drawLineMap(unit.getPosition(), unit.getTargetPosition(), Color.Cyan);
    }

    private void paintRescoutCriteria() {
        int yOffset = 15;
        int lineHeight = 12;
        int xStart = 175;

        game.setTextSize(Text.Size.Small);

        game.drawTextScreen(xStart, yOffset, "=== Rescout Criteria ===");
        yOffset += lineHeight;
        game.drawTextScreen(xStart, yOffset, "Scouting Completed: " + scouting.isCompletedScout());
        yOffset += lineHeight;
        game.drawTextScreen(xStart, yOffset, "Scouting Attempts: " + scouting.attemptsMaxed());
        yOffset += lineHeight;
        game.drawTextScreen(xStart, yOffset, "Enemy Buildings Known: " + gameState.isEnemyBuildingDiscovered());

        game.setTextSize(Text.Size.Default);
    }

    private void paintScoutPoints(int x, int y) {
        game.drawCircleMap(x, y, 25, Color.Orange, true);
    }

    //Production Painters
    private void paintProductionQueueReadout(PriorityQueue<PlannedItem> productionQueue) {
        List<PlannedItem> safeQueue = new ArrayList<>(productionQueue);
        Collections.sort(safeQueue, Comparator.comparingInt(PlannedItem::getSupply));
        int readoutAmount = 0;
        game.setTextSize(Text.Size.Small);
        for(PlannedItem pi : safeQueue) {
            if(readoutAmount < 10) {

                if(pi.getUnitType() != null) {
                    game.drawTextScreen(350, 20 + (15 * readoutAmount), "\u0004 Supply: " + pi.getSupply() + " " + "Type: " + pi.getUnitType().toString() + " " + pi.getPlannedItemStatus().toString() + " " + pi.getPriority());
                    readoutAmount++;
                }
                else if(pi.getUpgradeType() != null) {
                    game.drawTextScreen(350, 20 + (15 * readoutAmount), "\u000E Supply: " + pi.getSupply() + " " + "Type: " + pi.getUpgradeType().toString() + " " + pi.getPlannedItemStatus().toString() + " " + pi.getPriority());
                    readoutAmount++;
                }
                else if(pi.getTechUpgrade() != null) {
                    game.drawTextScreen(350, 20 + (15 * readoutAmount), "\u000E Supply: " + pi.getSupply() + " " + "Type: " + pi.getTechUpgrade().toString() + " " + pi.getPlannedItemStatus().toString() + " " + pi.getPriority());
                    readoutAmount++;
                }


            }
        }
        game.setTextSize(Text.Size.Default);
    }
}