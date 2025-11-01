package macro;

import bwapi.Position;
import bwem.Base;
import information.BaseInfo;
import information.enemy.EnemyInformation;
import information.enemy.enemyopeners.EnemyStrategy;
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
    private Position mainRallyPoint;
    private Position naturalRallyPoint;

    public RallyPoint(PathFinding pathFinding, EnemyInformation enemyInformation, BaseInfo baseInfo) {
        this.pathFinding = pathFinding;
        this.enemyInformation = enemyInformation;
        this.baseInfo = baseInfo;

        this.startingBase = baseInfo.getStartingBase();
        setInitialRallyPoints();

    }

    public void setRallyPoint(CombatUnits combatUnit) {
        if(enemyInformation.getEnemyOpener() == null || enemyStrategy.isStrategyDefended()) {
            if(baseInfo.isNaturalOwned() || baseInfo.hasBunkerInNatural()) {
                combatUnit.setRallyPoint(naturalRallyPoint.toTilePosition());
            }
            else {
                combatUnit.setRallyPoint(mainRallyPoint.toTilePosition());
            }
        }

        if(enemyInformation.getEnemyOpener() == null) {
            return;
        }

        switch(enemyStrategy.getStrategyName()) {
            case "Four Pool":
                combatUnit.setRallyPoint(baseInfo.getStartingBase().getCenter().toTilePosition());
                break;
            case "Gas Steal":
                combatUnit.setRallyPoint(mainRallyPoint.toTilePosition());
                break;
            case "Cannon Rush":
                combatUnit.setRallyPoint(mainRallyPoint.toTilePosition());
                break;
            case "Dark Templar":
                combatUnit.setRallyPoint(mainRallyPoint.toTilePosition());
                break;
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

    private void setInitialRallyPoints() {
        mainRallyPoint = rallyPath(startingBase.getCenter(), baseInfo.getMainChoke().getCenter().toPosition(), 0.75);
        naturalRallyPoint = rallyPath(startingBase.getCenter(), baseInfo.getNaturalChoke().getCenter().toPosition(), 0.75);
    }

    public void onFrame() {
        enemyStrategy = enemyInformation.getEnemyOpener();
    }


}
