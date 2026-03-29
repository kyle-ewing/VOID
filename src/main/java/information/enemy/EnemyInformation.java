package information.enemy;

import java.util.HashSet;
import java.util.List;

import bwapi.Game;
import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;
import bwem.Base;
import information.GameState;
import information.MapInfo;
import information.enemy.enemyopeners.EnemyStrategy;
import information.enemy.enemytechbuildings.EnemyTechBuilding;
import information.enemy.enemytechunits.EnemyTechUnits;
import util.Time;

public class EnemyInformation {
    private HashSet<EnemyUnits> enemyUnits;
    private HashSet<EnemyUnits> validThreats;
    private HashSet<EnemyTechUnits> enemyTechUnits;
    private HashSet<UnitType> techunitResponse;
    private MapInfo mapInfo;
    private Game game;
    private GameState gameState;
    private EnemyUnits startingEnemyBase;
    private EnemyStrategyManager enemyStrategyManager;
    private EnemyStrategy enemyOpener;
    private int openerDefenseTimer = 0;
    private static final int OPENER_DEFENSE_TIME = 4320;

    public EnemyInformation(MapInfo mapInfo, Game game, GameState gameState) {
        this.mapInfo = mapInfo;
        this.game = game;
        this.gameState = gameState;

        enemyUnits = gameState.getKnownEnemyUnits();
        validThreats = gameState.getKnownValidThreats();
        enemyTechUnits = gameState.getKnownEnemyTechUnits();
        techunitResponse = gameState.getTechUnitResponse();
        startingEnemyBase = gameState.getStartingEnemyBase();

        enemyStrategyManager = new EnemyStrategyManager(mapInfo);
    }

    private boolean previouslyDiscovered(Unit unit) {
        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyID() == unit.getID()) {
                return true;
            }
        }
        return false;
    }

    private boolean checkForBuildings() {
        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyType().isBuilding()) {
                return true;
            }
        }
        return false;
    }

    private Base setEnemyNatural() {
        if (mapInfo.getEnemyMain() == null) {
            return null;
        }

        Base enemyMain = mapInfo.getEnemyMain();
        Base closestBase = null;

        int shortestPathLength = Integer.MAX_VALUE;

        for (Base base : mapInfo.getMapBases()) {
            if (base.equals(enemyMain)) {
                continue;
            }

            if (base.getGeysers().isEmpty()) {
                continue;
            }

            List<Position> path = mapInfo.getPathFinding().findPath(
                    enemyMain.getLocation().toPosition(),
                    base.getLocation().toPosition()
            );

            if (path != null && !path.isEmpty() && path.size() < shortestPathLength) {
                shortestPathLength = path.size();
                closestBase = base;
            }
        }

        return closestBase;
    }

    private void enemyInBase() {
        for (EnemyUnits enemyUnit : enemyUnits) {
            if (!enemyUnit.getEnemyType().canAttack() && enemyUnit.getEnemyType() != UnitType.Zerg_Extractor) {
                continue;
            }

            Position enemyPos = enemyUnit.getEnemyUnit().getPosition();

            if (mapInfo.getBaseTiles().contains(enemyUnit.getEnemyUnit().getTilePosition())) {
                gameState.setEnemyInBase(true);
                return;
            }
            else if (mapInfo.getMinBaseTiles().contains(enemyUnit.getEnemyUnit().getTilePosition())) {
                gameState.setEnemyInBase(true);
                return;
            }
            else if (mapInfo.getNaturalTiles().contains(enemyUnit.getEnemyUnit().getTilePosition()) && (mapInfo.isNaturalOwned() || mapInfo.hasBunkerInNatural())) {
                gameState.setEnemyInBase(true);
                return;
            }
            else if (enemyUnit.getEnemyType().isFlyer()) {
                int mainChokeLeash = 700;
                int naturalChokeLeash = 250;

                if (mapInfo.getMainChoke() != null) {
                     mainChokeLeash = (int) mapInfo.getMainChoke().getCenter().toPosition().getDistance(mapInfo.getStartingBase().getCenter()) + 32;
                }


                if (mapInfo.getNaturalChoke() != null ) {
                    naturalChokeLeash = (int) mapInfo.getNaturalChoke().getCenter().toPosition().getDistance(mapInfo.getNaturalBase().getCenter()) + 32;
                }

                boolean inMainBase = enemyPos.getDistance(mapInfo.getStartingBase().getCenter()) < mainChokeLeash;
                boolean inNaturalBase = enemyPos.getDistance(mapInfo.getStartingBase().getCenter()) < naturalChokeLeash;

                if (inMainBase || (mapInfo.getNaturalBase() != null && inNaturalBase && mapInfo.isNaturalOwned())) {
                    gameState.setEnemyInBase(true);
                    return;
                }
            }
        }
        gameState.setEnemyInBase(false);
    }

    private void enemyInNatural() {
        if (mapInfo.getNaturalBase() == null) {
            return;
        }

        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyPosition() == null) {
                continue;
            }

            if (!enemyUnit.getEnemyType().canAttack() && !enemyUnit.getEnemyType().isBuilding()) {
                continue;
            }

            Position enemyPos = enemyUnit.getEnemyUnit().getPosition();

            if (mapInfo.getNaturalTiles().contains(enemyUnit.getEnemyTilePosition())) {
                gameState.setEnemyInNatural(true);
                return;
            }
            else if (enemyUnit.getEnemyType().isFlyer()) {
                if (enemyPos.getDistance(mapInfo.getNaturalBase().getCenter()) < 400 && mapInfo.isNaturalOwned()) {
                    gameState.setEnemyInNatural(true);
                    gameState.setEnemyFlyerInBase(true);
                    return;
                }
            }
        }
        gameState.setEnemyInNatural(false);
        gameState.setEnemyFlyerInBase(false);
    }

    public boolean hasType(UnitType unitType) {
        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyType() == unitType) {
                return true;
            }
        }
        return false;
    }

    public boolean outRangingUnitNearby(EnemyUnits enemyUnit, UnitType friendlyUnitType, int range) {
        for (EnemyUnits outRangingUnit : enemyUnits) {
            if (outRangingUnit.getEnemyID() == enemyUnit.getEnemyID()) {
                continue;
            }

            if (!outRangingUnit.getEnemyType().canAttack() || outRangingUnit.getEnemyType().isBuilding() || outRangingUnit.getEnemyType() == UnitType.Terran_Marine) {
                continue;
            }

            if (outRangingUnit.getEnemyType().groundWeapon().maxRange() + 32 >= friendlyUnitType.groundWeapon().maxRange()) {
                if (outRangingUnit.getEnemyUnit().getDistance(enemyUnit.getEnemyUnit().getPosition()) <= range) {
                    return true;
                }
            }
        }
        return false;
    }

    private void checkTechUnits() {
        for (EnemyTechUnits enemyTechUnit : enemyStrategyManager.getEnemyTechUnits()) {
            if (enemyTechUnit.isEnemyTechUnit(enemyUnits) && !enemyTechUnits.contains(enemyTechUnit)) {
                if (!enemyTechUnit.hasTriggeredResponse()) {
                    enemyTechUnit.techBuildingResponse();
                    enemyTechUnit.techUpgradeResponse();
                    enemyTechUnit.setTriggeredResponse(true);
                }

                enemyTechUnits.add(enemyTechUnit);
                techunitResponse.add(enemyTechUnit.getResponseUnitType());
            }
            else if (!enemyTechUnit.isEnemyTechUnit(enemyUnits) && enemyTechUnits.contains(enemyTechUnit)) {
                enemyTechUnits.remove(enemyTechUnit);
                techunitResponse.remove(enemyTechUnit.getResponseUnitType());
            }
        }
    }

    private void checkTechBuildings() {
        for (EnemyTechBuilding enemyBuilding : enemyStrategyManager.getEnemyBuildings()) {
            if (!enemyBuilding.hasTriggeredResponse()) {
                enemyBuilding.friendlyBuildingResponse();
                enemyBuilding.setTriggeredResponse(true);
            }
        }
    }

    private boolean isThreat(Unit unit) {
        return unit.getType() == UnitType.Zerg_Lurker ||
                unit.getType() == UnitType.Zerg_Sunken_Colony && !unit.isMorphing() ||
                unit.getType() == UnitType.Protoss_Photon_Cannon && unit.isPowered() ||
                unit.getType() == UnitType.Terran_Bunker;
    }

    //TODO: add time limit in enemy strategy to change check times depending on strategy
    private void checkOpenerDefense(Time currentTime) {
        if (enemyOpener != null && !enemyOpener.isStrategyDefended()) {
            openerDefenseTimer++;

            switch (enemyOpener.getStrategyName()) {
                case "Dark Templar":
                    if (currentTime.greaterThan(new Time(6,0))) {
                        enemyOpener.setDefendedStrategy(true);
                        return;
                    }
                    break;
                case "Two Fac Tank":
                    if (currentTime.greaterThan(new Time(7,0))) {
                        enemyOpener.setDefendedStrategy(true);
                        return;
                    }
                    break;
                default:
                    if (!gameState.isEnemyInBase() && currentTime.greaterThan(new Time(5,0))) {
                        enemyOpener.setDefendedStrategy(true);
                        return;
                    }
                    break;
            }
        }
    }

    public boolean beingSieged() {
        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyType() == UnitType.Terran_Siege_Tank_Siege_Mode) {
                if (mapInfo.hasBunkerInNatural()) {
                    if (enemyUnit.getEnemyUnit().getDistance(gameState.getBunkerPosition().toPosition()) < 450) {
                        return true;
                    }
                }
                else {
                    if (enemyUnit.getEnemyUnit().getDistance(gameState.getBunkerPosition().toPosition()) < 800) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public int getEnemySupplyInRange(Unit bunker) {
        int supply = 0;
        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyPosition() == null) {
                continue;
            }

            if (enemyUnit.getEnemyType() == UnitType.Zerg_Overlord) {
                continue;
            }

            if (enemyUnit.getEnemyType().isWorker()) {
                continue;
            }

            if (enemyUnit.getEnemyUnit().getType().isFlyer() || !enemyUnit.getEnemyUnit().isDetected()) {
                continue;
            }

            if (!enemyUnit.getEnemyUnit().exists()) {
                continue;
            }
            
            if (enemyUnit.getEnemyPosition().getApproxDistance(bunker.getPosition()) < 400) {
                supply += enemyUnit.getEnemyType().supplyRequired();
            }
        }
        return supply;
    }

    public void onFrame() {
        Time currentTime = new Time(game.getFrameCount());
        enemyInBase();
        enemyInNatural();
        checkOpenerDefense(currentTime);
        checkTechUnits();
        checkTechBuildings();
        gameState.setBeingSieged(beingSieged());

        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyUnit().isVisible()) {
                if (enemyUnit.getEnemyUnit().getType() != enemyUnit.getEnemyType()) {
                    updateUnitType(enemyUnit);
                }
                enemyUnit.setEnemyPosition(enemyUnit.getEnemyUnit().getPosition());
                enemyUnit.setEnemyTilePosition(enemyUnit.getEnemyUnit().getTilePosition());
                enemyUnit.setBurrowed(enemyUnit.getEnemyUnit().isBurrowed());
            }

            if (enemyUnit.getEnemyType() == UnitType.Spell_Scanner_Sweep) {
                enemyUnit.setSweepTimer();
            }

            //Remove irradiated units that die out of view
            if (enemyUnit.getEnemyUnit().isIrradiated()) {
                enemyUnit.setIrradiateTimer();
            }
        }
        //Edge case where buildings aren't removed on death but position is null from units walking over it
        enemyUnits.removeIf(eu -> eu.getEnemyType().isBuilding()
                && eu.getEnemyPosition() == null
                 && !eu.getEnemyUnit().canLift());

        enemyUnits.removeIf(eu -> eu.getIrradiateTimer() > 240 || eu.getSweepTimer() > 224);
        validThreats.removeIf(eu -> eu.getIrradiateTimer() > 240);

        if (enemyOpener != null) {
            return;
        }

        for (EnemyStrategy enemyStrategy : enemyStrategyManager.getEnemyStrategies()) {
            if (enemyStrategy.isEnemyStrategy(enemyUnits, currentTime) && enemyOpener == null) {
                enemyOpener = enemyStrategy;
                gameState.setEnemyOpener(enemyOpener);
                game.sendText("Potential enemy opener detected: " + enemyStrategy.getStrategyName());
                break;
            }
        }
    }

    public void onUnitDiscover(Unit unit) {
        if (unit.getPlayer() == game.neutral() && unit.getType() != UnitType.Spell_Scanner_Sweep) {
            return;
        }

        if (!previouslyDiscovered(unit)) {
            addEnemyUnit(unit);

            if (unit.getType().isBuilding()) {
                gameState.setEnemyBuildingDiscovered(true);
            }
        }

        if (gameState.getStartingEnemyBase() == null) {
            if (unit.getType().isResourceDepot()) {
                for (EnemyUnits enemyUnit : enemyUnits) {
                    if (enemyUnit.getEnemyType().isResourceDepot() && enemyUnit.getEnemyID() == unit.getID()
                    && mapInfo.getStartingBases().stream().anyMatch(base -> base.getLocation().getDistance(unit.getTilePosition()) < 10)) {
                        gameState.setStartingEnemyBase(enemyUnit);
                        mapInfo.setEnemyMain(mapInfo.getStartingBases().stream().filter(base -> base.getLocation().getDistance(unit.getTilePosition()) < 10).findAny().orElse(null));
                        mapInfo.setEnemyNatural(setEnemyNatural());
                        break;
                    }
                }
            }
        }

        if (isThreat(unit)) {
            for (EnemyUnits threat : validThreats) {
                if (threat.getEnemyID() == unit.getID()) {
                    return;
                }
            }

            for (EnemyUnits enemyUnit : enemyUnits) {
                if (enemyUnit.getEnemyID() == unit.getID()) {
                    validThreats.add(enemyUnit);
                    break;
                }
            }

        }

    }

    public void onUnitShow(Unit unit) {
        if (isThreat(unit)) {
            for (EnemyUnits threat : validThreats) {
                if (threat.getEnemyID() == unit.getID()) {
                    return;
                }
            }
            for (EnemyUnits enemyUnit : enemyUnits) {
                if (enemyUnit.getEnemyID() == unit.getID()) {
                    validThreats.add(enemyUnit);
                }
            }
        }
    }

    public void onUnitDestroy(Unit unit) {
        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyOpener != null && enemyOpener.getPriorityEnemyUnit() != null) {
                if (enemyOpener.getPriorityEnemyUnit().getEnemyID() == unit.getID()) {
                    enemyOpener.setPriorityEnemyUnit(null);
                }
            }

            if (enemyUnit.getEnemyID() == unit.getID()) {
                enemyUnits.remove(enemyUnit);
                break;
            }
        }

        for (EnemyUnits threat : validThreats) {
            if (threat.getEnemyID() == unit.getID()) {
                validThreats.remove(threat);
                break;
            }
        }

        if (!checkForBuildings() && unit.getType().isBuilding()) {
            gameState.setEnemyBuildingDiscovered(false);
        }
    }

    public void onUnitRenegade(Unit unit) {
        if (unit.getPlayer() != game.self() && game.enemy() ==  unit.getPlayer() && !previouslyDiscovered(unit)) {
            if (unit.getType() == UnitType.Zerg_Extractor || unit.getType() == UnitType.Terran_Refinery || unit.getType() == UnitType.Protoss_Assimilator) {
                addEnemyUnit(unit);
            }
        }
    }

    private void updateUnitType(EnemyUnits enemyUnit) {
        enemyUnit.setEnemyType(enemyUnit.getEnemyUnit().getType());
    }



    private void addEnemyUnit(Unit unit) {
        enemyUnits.add(new EnemyUnits(unit.getID(), unit));
    }

    public HashSet<EnemyUnits> getEnemyUnits() {
        return enemyUnits;
    }

    //Fix later
    public EnemyUnits getStartingEnemyBase() {
        return gameState.getStartingEnemyBase();
    }

    public MapInfo getBaseInfo() {
        return mapInfo;
    }

    public HashSet<EnemyTechUnits> getEnemyTechUnits() {
        return enemyTechUnits;
    }

    public HashSet<UnitType> getTechUnitResponse() {
        return techunitResponse;
    }
}
