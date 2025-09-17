package macro;

import bwapi.Position;
import bwem.Base;
import information.BaseInfo;
import information.EnemyInformation;
import information.enemyopeners.EnemyStrategy;
import macro.unitgroups.CombatUnits;
import map.PathFinding;
import util.PositionInterpolator;
import java.util.List;

public class RallyPoint {
    private PathFinding pathFinding;
    private EnemyInformation enemyInformation;
    private BaseInfo baseInfo;
    private EnemyStrategy enemyStrategy = null;
    private Base startingBase;

    public RallyPoint(PathFinding pathFinding, EnemyInformation enemyInformation, BaseInfo baseInfo) {
        this.pathFinding = pathFinding;
        this.enemyInformation = enemyInformation;
        this.baseInfo = baseInfo;

        this.startingBase = baseInfo.getStartingBase();

    }

    public void setRallyPoint(CombatUnits combatUnit) {
        if(enemyInformation.getEnemyOpener() == null || enemyStrategy.isStrategyDefended()) {
            if(baseInfo.isNaturalOwned()) {
                combatUnit.setRallyPoint(rallyPath(baseInfo.getNaturalBase().getCenter(), baseInfo.getNaturalChoke().getCenter().toPosition(), 0.80).toTilePosition());
            }
            else {
                combatUnit.setRallyPoint(rallyPath(startingBase.getCenter(), baseInfo.getMainChoke().getCenter().toPosition(), 0.75).toTilePosition());
            }
        }
        else if(enemyInformation.getEnemyOpener().getStrategyName().equals("Four Pool")) {
            combatUnit.setRallyPoint(baseInfo.getStartingBase().getCenter().toTilePosition());
        }
    }

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

    public void onFrame() {
        enemyStrategy = enemyInformation.getEnemyOpener();
    }


}
