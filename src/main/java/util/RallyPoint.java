package util;

import bwapi.Position;
import bwem.Base;
import information.BaseInfo;
import information.GameState;
import information.enemy.enemyopeners.EnemyStrategy;
import unitgroups.units.CombatUnits;
import map.PathFinding;

import java.util.List;

public class RallyPoint {
    private PathFinding pathFinding;
    private GameState gameState;
    private BaseInfo baseInfo;
    private EnemyStrategy enemyStrategy = null;
    private Base startingBase;
    private Base naturalBase;
    private Position mainRallyPoint;
    private Position naturalRallyPoint;

    public RallyPoint(PathFinding pathFinding, GameState gameState, BaseInfo baseInfo) {
        this.pathFinding = pathFinding;
        this.gameState = gameState;
        this.baseInfo = baseInfo;

        this.startingBase = baseInfo.getStartingBase();
        this.naturalBase = baseInfo.getNaturalBase();
        setInitialRallyPoints();

    }

    public void setRallyPoint(CombatUnits combatUnit) {
        if(enemyStrategy == null || enemyStrategy.isStrategyDefended() || baseInfo.isNaturalOwned()) {
            if(baseInfo.isNaturalOwned() || baseInfo.hasBunkerInNatural()) {
                combatUnit.setRallyPoint(naturalRallyPoint.toTilePosition());
            }
            else {
                combatUnit.setRallyPoint(mainRallyPoint.toTilePosition());
            }
        }
        else {
            combatUnit.setRallyPoint(mainRallyPoint.toTilePosition());
        }

        if(gameState.getEnemyOpener() == null) {
            return;
        }

        if(enemyStrategy.isStrategyDefended()) {
            return;
        }

        switch(enemyStrategy.getStrategyName()) {
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

        if(nearestWalkable != null && nearestWalkableToEnd != null) {
            path = pathFinding.findPath(nearestWalkable, nearestWalkableToEnd);
        }
        else {
            path = pathFinding.findPath(startingPos, endPoint);
        }


        if(path == null || path.isEmpty()) {
            return endPoint;
        }

        return PositionInterpolator.interpolate(path, percentage);
    }

    private void setInitialRallyPoints() {
        mainRallyPoint = rallyPath(startingBase.getCenter(), baseInfo.getMainChoke().getCenter().toPosition(), 0.75);
        naturalRallyPoint = rallyPath(naturalBase.getCenter(), baseInfo.getNaturalChoke().getCenter().toPosition(), 0.65);
    }

    public void onFrame() {
        enemyStrategy = gameState.getEnemyOpener();
    }


}
