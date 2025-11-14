package macro;

import bwapi.Position;
import bwem.Base;
import information.BaseInfo;
import information.GameState;
import information.enemy.EnemyInformation;
import information.enemy.enemyopeners.EnemyStrategy;
import macro.unitgroups.CombatUnits;
import map.PathFinding;
import util.PositionInterpolator;
import java.util.List;

public class RallyPoint {
    private PathFinding pathFinding;
    private GameState gameState;
    private BaseInfo baseInfo;
    private EnemyStrategy enemyStrategy = null;
    private Base startingBase;
    private Position mainRallyPoint;
    private Position naturalRallyPoint;

    public RallyPoint(PathFinding pathFinding, GameState gameState, BaseInfo baseInfo) {
        this.pathFinding = pathFinding;
        this.gameState = gameState;
        this.baseInfo = baseInfo;

        this.startingBase = baseInfo.getStartingBase();
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

        if(gameState.getEnemyOpener() == null) {
            return;
        }

        if(enemyStrategy.isStrategyDefended()) {
            return;
        }

        switch(enemyStrategy.getStrategyName()) {
            case "Four Pool":
                combatUnit.setRallyPoint(baseInfo.getStartingBase().getCenter().toTilePosition());
                break;
            default:
                combatUnit.setRallyPoint(mainRallyPoint.toTilePosition());
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
        naturalRallyPoint = rallyPath(startingBase.getCenter(), baseInfo.getNaturalChoke().getCenter().toPosition(), 0.88);
    }

    public void onFrame() {
        enemyStrategy = gameState.getEnemyOpener();
    }


}
