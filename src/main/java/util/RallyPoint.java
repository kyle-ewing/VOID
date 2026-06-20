package util;

import java.util.List;

import bwapi.Game;
import bwapi.Position;
import bwapi.TilePosition;
import information.GameState;
import information.MapInfo;
import information.enemy.enemyopeners.EnemyStrategy;
import map.PathFinding;
import map.bwemwrappers.Area;
import map.bwemwrappers.Base;
import unitgroups.units.CombatUnits;

public class RallyPoint {
    private Game game;
    private PathFinding pathFinding;
    private GameState gameState;
    private MapInfo mapInfo;
    private EnemyStrategy enemyStrategy = null;
    private Base startingBase;
    private Base naturalBase;
    private Position mainRallyPoint;
    private Position naturalRallyPoint;
    private Position lateGameRallyPoint;

    public RallyPoint(Game game, PathFinding pathFinding, GameState gameState, MapInfo mapInfo) {
        this.game = game;
        this.pathFinding = pathFinding;
        this.gameState = gameState;
        this.mapInfo = mapInfo;

        this.startingBase = mapInfo.getStartingBase();
        this.naturalBase = mapInfo.getNaturalBase();
        setInitialRallyPoints();

    }

    public void setRallyPoint(CombatUnits combatUnit) {
        if (mapInfo.hasExpansionPastNatural() && lateGameRallyPoint != null) {
            combatUnit.setRallyPoint(lateGameRallyPoint.toTilePosition());
            return;
        }
        else if (enemyStrategy == null || mapInfo.isNaturalOwned() || mapInfo.hasBunkerInNatural()) {
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
            case FOURPOOL:
            case SHUTTLERUSH:
                combatUnit.setRallyPoint(startingBase.getCenter().toTilePosition());
                break;
            case GASSTEAL:
                Base gasStealEnemyNatural = mapInfo.getEnemyNatural();
                if (gasStealEnemyNatural == null) {
                    combatUnit.setRallyPoint(new TilePosition(game.mapWidth() / 2, game.mapHeight() / 2));
                }
                else {
                    combatUnit.setRallyPoint(rallyPath(naturalBase.getCenter(), gasStealEnemyNatural.getCenter(), 0.8).toTilePosition());
                }
                break;
            case BUNKERRUSH:
                if (gameState.isEnemyInNatural()) {
                    combatUnit.setRallyPoint(naturalRallyPoint.toTilePosition());
                }
                else if (!mapInfo.isNaturalOwned() || !mapInfo.hasBunkerInNatural()){
                    combatUnit.setRallyPoint(mainRallyPoint.toTilePosition());
                }
                else {
                    combatUnit.setRallyPoint(naturalRallyPoint.toTilePosition());
                }
                break;
            case TWOFACTANK:
                combatUnit.setRallyPoint(naturalRallyPoint.toTilePosition());
                break;
            case TWOGATE:
                if (mapInfo.isNaturalOwned() || mapInfo.hasBunkerInNatural()) {
                    combatUnit.setRallyPoint(naturalRallyPoint.toTilePosition());
                }
                else {
                    combatUnit.setRallyPoint(mainRallyPoint.toTilePosition());
                }
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
        mainRallyPoint = rallyPath(startingBase.getCenter(), mapInfo.getMainChoke().getCenter(), 0.72);
        naturalRallyPoint = rallyPath(naturalBase.getCenter(), mapInfo.getNaturalChoke().getCenter(), 0.62);

        if (mapInfo.getOutsideNaturalChoke() == null) {
            return;
        }

        Area outsideArea = mapInfo.getOutsideNaturalArea();

        if (outsideArea != null && !outsideArea.getBases().isEmpty()) {
            lateGameRallyPoint = rallyPath(mapInfo.getNaturalChoke().getCenter(), outsideArea.getBases().get(0).getCenter(), 0.7);
            return;
        }

        lateGameRallyPoint = rallyPath(mapInfo.getOutsideNaturalChoke().getCenter(), mapInfo.getNaturalChoke().getCenter(), 0.6);
    }

    public void onFrame() {
        enemyStrategy = gameState.getEnemyOpener();
    }

    public Position getMainRallyPoint() {
        return mainRallyPoint;
    }

    public Position getNaturalRallyPoint() {
        return naturalRallyPoint;
    }

    public Position getLateGameRallyPoint() {
        return lateGameRallyPoint;
    }


}
