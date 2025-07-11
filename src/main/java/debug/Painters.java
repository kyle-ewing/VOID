package debug;

import bwapi.*;
import bwem.BWEM;
import bwem.Base;
import bwem.ChokePoint;
import macro.ResourceManager;
import macro.unitgroups.CombatUnits;
import macro.unitgroups.UnitStatus;
import macro.unitgroups.Workers;

import java.util.HashSet;
import java.util.List;

public class Painters {
    Game game;
    BWEM bwem;
    ResourceManager resourceManager;

    public Painters(Game game, BWEM bwem, ResourceManager resourceManager) {
        this.game = game;
        this.bwem = bwem;
        this.resourceManager = resourceManager;
    }

    public Painters(Game game) {
        this.game = game;
    }

    public Painters(Game game, BWEM bwem) {
        this.game = game;
        this.bwem = bwem;
    }

    private void paintCircle(Unit unit, int radius, Color color) {
        if(unit == null) {
            return;
        }
        game.drawCircleMap(unit.getPosition(), radius, color);
    }

    private void paintCircle(Unit unit, Color color) {
        if(unit == null) {
            return;
        }
        game.drawCircleMap(unit.getPosition(), 8, color);
    }

    private void paintCircle(Position position, int radius, Color color) {
        if(position == null) {
            return;
        }
        game.drawCircleMap(position, radius, color);
    }

    private void paintWorker() {
        for(Workers worker : resourceManager.getWorkers()) {
            switch (worker.getWorkerStatus()) {
                case MINERALS:
                    paintCircle(worker.getUnit(), 8, Color.Blue);
                    continue;
                case SCOUTING:
                    paintCircle(worker.getUnit(), 8, Color.Orange);
                    continue;
                case IDLE:
                    paintCircle(worker.getUnit(), 8, Color.White);
                    continue;
                case MOVING_TO_BUILD:
                    paintCircle(worker.getUnit(), 8, Color.Purple);
                    continue;
                case BUILDING:
                    paintCircle(worker.getUnit(), 8, Color.Yellow);
                    continue;
                case GAS:
                    paintCircle(worker.getUnit(), 8, Color.Green);
                    continue;
                case ATTACKING:
                    paintCircle(worker.getUnit(), 8, Color.Red);
                    continue;
                case REPAIRING:
                    paintCircle(worker.getUnit(), 8, Color.Grey);
                    continue;
                case DEFEND:
                    paintCircle(worker.getUnit(), 8, Color.Teal);
            }
        }
    }

    public void paintBuildTile(TilePosition tilePosition, UnitType unitType, Color color) {
        game.drawBoxMap(tilePosition.toPosition(), tilePosition.toPosition().add(new Position(unitType.width() + 2, unitType.height() + 2)), color);
    }

//    public void drawAttackLine(HashSet<CombatUnits> combatUnits) {
//        for(CombatUnits unit : combatUnits) {
//            if(unit.getUnit().getTarget() != null) {
//                game.drawLineMap(unit.getUnit().getPosition(), unit.getUnit().getTarget().getPosition(), Color.Red);
//            }
//        }
//    }

    public void drawAttackRange(Unit unit) {
        if(unit == null) {
            return;
        }
        game.drawCircleMap(unit.getPosition(), unit.getType().groundWeapon().maxRange(), Color.Cyan);
    }

    public void paintNatural(Base base) {
        game.drawCircleMap(base.getCenter(), 40, Color.Green);
        game.drawTextMap(base.getCenter(), "Natural");
    }

    public void paintNaturalChoke(ChokePoint chokePoint) {
        game.drawCircleMap(chokePoint.getCenter().toPosition(), 40, Color.Yellow);
    }

    public void paintAllChokes() {
        for(ChokePoint chokePoint : bwem.getMap().getChokePoints()) {
            game.drawCircleMap(chokePoint.getCenter().toPosition(), 25, Color.White);
        }
    }

    public void paintTilePositions(List<Position> position) {
        for(Position pos : position) {
            game.drawCircleMap(pos, 2, Color.White, true);
        }

    }

    public void paintBasePosition(HashSet<Base> bases) {
        for(Base base : bases) {
            game.drawTextMap(base.getCenter(), String.valueOf(base.getCenter()));
        }
    }

    public void paintUnitStatus(CombatUnits unit) {

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
        }
    }

    public void paintStimStatus(CombatUnits unit) {
        if(unit.getUnit().isStimmed()) {
            game.drawTextMap(unit.getUnit().getPosition(), "STIMMED" );
        }
    }

    public void paintWorkerText() {
        for(Workers worker : resourceManager.getWorkers()) {
            game.drawTextMap(worker.getUnit().getPosition(), worker.getWorkerStatus().toString());
        }
    }

    public void paintClosestEnemy(CombatUnits unit) {
        if(unit.getEnemyUnit() != null && unit.getEnemyUnit().getEnemyPosition() != null) {
            game.drawLineMap(unit.getUnit().getPosition(), unit.getEnemyUnit().getEnemyPosition(), Color.Yellow);
        }

    }

    public void paintScoutPath(Unit unit) {
        game.drawLineMap(unit.getPosition(), unit.getTargetPosition(), Color.Cyan);
    }

    public void paintScoutPoints(int x, int y) {
        game.drawCircleMap(x, y, 25, Color.Orange, true);
    }

    public void paintLargeBuildTiles(HashSet<TilePosition> buildTiles) {
        for(TilePosition tilePosition : buildTiles) {
            game.drawTextMap(tilePosition.toPosition(), String.valueOf(tilePosition));
            Position start = tilePosition.toPosition();
            Position end = new Position(
                start.getX() + UnitType.Terran_Barracks.tileWidth() * 32,
                start.getY() + UnitType.Terran_Barracks.tileHeight() * 32
            );
            game.drawBoxMap(start, end, Color.Green);
        }
    }

    public void paintMediumBuildTiles(HashSet<TilePosition> buildTiles, Color color) {
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

    public void paintPaintBunkerTile(TilePosition tilePosition) {
        game.drawTextMap(tilePosition.toPosition(), String.valueOf(tilePosition));
        Position start = tilePosition.toPosition();
        Position end = new Position(
            start.getX() + UnitType.Terran_Bunker.tileWidth() * 32,
            start.getY() + UnitType.Terran_Bunker.tileHeight() * 32
        );
        game.drawBoxMap(start, end, Color.Red);
    }

    public void paintMissileTile(TilePosition tilePosition) {
        game.drawTextMap(tilePosition.toPosition(), String.valueOf(tilePosition));
        Position start = tilePosition.toPosition();
        Position end = new Position(
            start.getX() + UnitType.Terran_Missile_Turret.tileWidth() * 32,
            start.getY() + UnitType.Terran_Missile_Turret.tileHeight() * 32
        );
        game.drawBoxMap(start, end, Color.Teal);
    }

    public void paintTiles(HashSet<TilePosition> tiles) {
        for(TilePosition tile : tiles) {
            game.drawBoxMap(tile.toPosition(), tile.toPosition().add(new Position(32, 32)), Color.White);
        }
    }

    public void paintAvailableBuildTiles(HashSet<TilePosition> buildTiles, int offset, String tileType) {
        game.drawTextScreen(5,30 + offset,   tileType +" Tiles Available: " + buildTiles.size());
    }

    public void paintExpansionOrdering(List<Base> orderedExpansions) {
        for(int i = 0; i < orderedExpansions.size(); i++) {
            game.drawTextMap(orderedExpansions.get(i).getCenter(), "Expansion: " + i);
            game.drawCircleMap(orderedExpansions.get(i).getCenter(), 40, Color.Purple);
        }
    }

    public void paintCombatScouts(CombatUnits unit) {
        if(unit.getUnitStatus() == UnitStatus.SCOUT) {
            game.drawLineMap(unit.getUnit().getPosition(), unit.getUnit().getTargetPosition(), Color.Purple);
        }
    }

    public void paintMainBufferZone(Base base) {
            game.drawCircleMap(base.getCenter(), 800, Color.Yellow);
    }

    public void paintTileZone(HashSet<TilePosition> buildTiles, Color color) {
        for(TilePosition tile : buildTiles) {
            game.drawBoxMap(tile.toPosition(), tile.toPosition().add(new Position(32, 32)), color);
        }
    }

    public void paintMedicTarget(CombatUnits unit) {
        if(unit.getUnitType() != UnitType.Terran_Medic) {
            return;
        }

        if(unit.getFriendlyUnit() != null) {
            game.drawLineMap(unit.getUnit().getPosition(), unit.getFriendlyUnit().getPosition(), Color.Green);
        }
        //game.drawCircleMap(unit.getUnit().getPosition(), 200, Color.Green);
    }



    public void onFrame() {
        paintWorker();
        paintWorkerText();
    }
}