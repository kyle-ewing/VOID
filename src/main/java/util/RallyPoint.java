package util;

import java.util.List;

import bwapi.Position;
import bwem.Base;
import information.GameState;
import information.MapInfo;
import information.enemy.enemyopeners.EnemyStrategy;
import map.PathFinding;
import unitgroups.units.CombatUnits;

public class RallyPoint {
    private PathFinding pathFinding;
    private GameState gameState;
    private MapInfo mapInfo;
    private EnemyStrategy enemyStrategy = null;
    private Base startingBase;
    private Base naturalBase;
    private Position mainRallyPoint;
    private Position naturalRallyPoint;

    public RallyPoint(PathFinding pathFinding, GameState gameState, MapInfo mapInfo) {
        this.pathFinding = pathFinding;
        this.gameState = gameState;
        this.mapInfo = mapInfo;

        this.startingBase = mapInfo.getStartingBase();
        this.naturalBase = mapInfo.getNaturalBase();
        setInitialRallyPoints();

    }

    public void setRallyPoint(CombatUnits combatUnit) {
        if (enemyStrategy == null || mapInfo.isNaturalOwned() || mapInfo.hasBunkerInNatural()) {
            if (mapInfo.hasBunkerInNatural() || mapInfo.isNaturalOwned()) {
                combatUnit.setRallyPoint(naturalRallyPoint.toTilePosition());
            }
            else {
                combatUnit.setRallyPoint(mainRallyPoint.toTilePosition());
            }
        }
        else {
            combatUnit.setRallyPoint(mainRallyPoint.toTilePosition());
        }

        if (gameState.getEnemyOpener() == null) {
            return;
        }

        if (enemyStrategy.isStrategyDefended()) {
            return;
        }

        switch (enemyStrategy.getStrategyName()) {
            case "Four Pool":
                combatUnit.setRallyPoint(startingBase.getCenter().toTilePosition());
                break;
            case "Gas Steal":
                combatUnit.setRallyPoint(naturalBase.getCenter().toTilePosition());
                break;
        }

    }

    //Percentage is how far along the path to go (0.0 = start, 1.0 = end)
    private Position rallyPath(Position startingPos, Position endPoint, double percentage) {
        Position nearestWalkable = pathFinding.findNearestWalkable(startingPos);
        Position nearestWalkableToEnd = pathFinding.findNearestWalkable(endPoint);
        List<Position> path;

        if (nearestWalkable != null && nearestWalkableToEnd != null) {
            path = pathFinding.findPath(nearestWalkable, nearestWalkableToEnd);
        }
        else {
            path = pathFinding.findPath(startingPos, endPoint);
        }


        if (path == null || path.isEmpty()) {
            return endPoint;
        }

        return PositionInterpolator.interpolate(path, percentage);
    }

    private void setInitialRallyPoints() {
        mainRallyPoint = rallyPath(startingBase.getCenter(), mapInfo.getMainChoke().getCenter().toPosition(), 0.62);
        naturalRallyPoint = rallyPath(naturalBase.getCenter(), mapInfo.getNaturalChoke().getCenter().toPosition(), 0.62);
    }

    public void onFrame() {
        enemyStrategy = gameState.getEnemyOpener();
    }


}
