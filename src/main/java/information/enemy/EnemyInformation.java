package information.enemy;

import bwapi.Game;
import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;
import bwem.Base;
import information.BaseInfo;
import information.GameState;
import information.enemy.enemyopeners.EnemyStrategy;
import information.enemy.enemytechunits.EnemyTechUnits;
import planner.PlannedItemStatus;
import util.Time;

import java.util.HashSet;

public class EnemyInformation {
    private HashSet<EnemyUnits> enemyUnits;
    private HashSet<EnemyUnits> validThreats;
    private HashSet<EnemyTechUnits> enemyTechUnits;
    private HashSet<UnitType> techunitResponse;
    private BaseInfo baseInfo;
    private Game game;
    private GameState gameState;
    private EnemyUnits startingEnemyBase;
    private Base enemyNatural = null;
    private EnemyStrategyManager enemyStrategyManager;
    private EnemyStrategy enemyOpener;
    private int openerDefenseTimer = 0;
    private static final int OPENER_DEFENSE_TIME = 6480;

    public EnemyInformation(BaseInfo baseInfo, Game game, GameState gameState) {
        this.baseInfo = baseInfo;
        this.game = game;
        this.gameState = gameState;

        enemyUnits = gameState.getKnownEnemyUnits();
        validThreats = gameState.getKnownValidThreats();
        enemyTechUnits = gameState.getKnownEnemyTechUnits();
        techunitResponse = gameState.getTechUnitResponse();
        startingEnemyBase = gameState.getStartingEnemyBase();

        enemyStrategyManager = new EnemyStrategyManager(baseInfo);
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

    private void enemyInBase() {
        for(EnemyUnits enemyUnit : enemyUnits) {
            if(!enemyUnit.getEnemyType().canAttack()) {
                continue;
            }

            Position enemyPos = enemyUnit.getEnemyUnit().getPosition();

            if(baseInfo.getBaseTiles().contains(enemyUnit.getEnemyUnit().getTilePosition())) {
                gameState.setEnemyInBase(true);
                return;
            }
            else if(baseInfo.getMinBaseTiles().contains(enemyUnit.getEnemyUnit().getTilePosition())) {
                gameState.setEnemyInBase(true);
                return;
            }
            else if(baseInfo.getNaturalTiles().contains(enemyUnit.getEnemyUnit().getTilePosition())) {
                gameState.setEnemyInBase(true);
                return;
            }
            else if(enemyUnit.getEnemyType().isFlyer()) {
                if(enemyPos.getDistance(baseInfo.getStartingBase().getCenter()) < 700 || (baseInfo.getNaturalBase() != null && (enemyPos.getDistance(baseInfo.getNaturalBase().getCenter()) < 400 && baseInfo.isNaturalOwned()))) {
                    gameState.setEnemyInBase(true);
                    return;
                }

                gameState.setEnemyInBase(true);
                return;
            }
        }
        gameState.setEnemyInBase(false);
    }

    private void enemyInNatural() {
        if(baseInfo.getNaturalBase() == null) {
            return;
        }

        for(EnemyUnits enemyUnit : enemyUnits) {
            if(enemyUnit.getEnemyPosition() == null) {
                continue;
            }

            if(!enemyUnit.getEnemyType().canAttack() && !enemyUnit.getEnemyType().isBuilding()) {
                continue;
            }

            Position enemyPos = enemyUnit.getEnemyUnit().getPosition();

            if(baseInfo.getNaturalTiles().contains(enemyUnit.getEnemyTilePosition())) {
                gameState.setEnemyInNatural(true);
                return;
            }
            else if(enemyUnit.getEnemyType().isFlyer()) {
                if(enemyPos.getDistance(baseInfo.getNaturalBase().getCenter()) < 400 && baseInfo.isNaturalOwned()) {
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
        for(EnemyUnits outRangingUnit : enemyUnits) {
            if(outRangingUnit.getEnemyID() == enemyUnit.getEnemyID()) {
                continue;
            }

            if(!outRangingUnit.getEnemyType().canAttack() || outRangingUnit.getEnemyType().isBuilding()) {
                continue;
            }

            if(outRangingUnit.getEnemyType().groundWeapon().maxRange() + 32 >= friendlyUnitType.groundWeapon().maxRange()) {
                if(outRangingUnit.getEnemyUnit().getDistance(enemyUnit.getEnemyUnit().getPosition()) <= range) {
                    return true;
                }
            }
        }
        return false;
    }

    private void checkTechUnits() {
        for(EnemyTechUnits enemyTechUnit : enemyStrategyManager.getEnemyTechUnits()) {
            if(enemyTechUnit.isEnemyTechUnit(enemyUnits) && !enemyTechUnits.contains(enemyTechUnit)) {
                if(!enemyTechUnit.hasTriggeredResponse()) {
                    enemyTechUnit.techBuildingResponse();
                    enemyTechUnit.techUpgradeResponse();
                    enemyTechUnit.setTriggeredResponse(true);
                }

                enemyTechUnits.add(enemyTechUnit);
                techunitResponse.add(enemyTechUnit.getResponseUnitType());
            }
            else if(!enemyTechUnit.isEnemyTechUnit(enemyUnits) && enemyTechUnits.contains(enemyTechUnit)) {
                enemyTechUnits.remove(enemyTechUnit);
                techunitResponse.remove(enemyTechUnit.getResponseUnitType());
            }
        }
    }

    private boolean isThreat(Unit unit) {
        return unit.getType() == UnitType.Zerg_Lurker ||
                unit.getType() == UnitType.Zerg_Sunken_Colony && !unit.isMorphing() ||
                unit.getType() == UnitType.Protoss_Photon_Cannon && unit.isPowered();
    }

    //TODO: add time limit in enemy strategy to change check times depending on strategy
    private void checkOpenerDefense(Time currentTime) {
        if (enemyOpener != null && !enemyOpener.isStrategyDefended()) {
            openerDefenseTimer++;

            //add specific strategy checks later
            if (openerDefenseTimer >= OPENER_DEFENSE_TIME) {
                switch(enemyOpener.getStrategyName()) {
                    case "Dark Templar":
                        if(!hasType(UnitType.Protoss_Dark_Templar)) {
                            enemyOpener.setDefendedStrategy(true);
                            return;
                        }
                    default:
                        if(!gameState.isEnemyInBase()) {
                            enemyOpener.setDefendedStrategy(true);
                            return;
                        }
                        break;
                }
                //Check again in 2 minutes
                openerDefenseTimer = 1440;
            }
        }


    }

    public void onFrame() {
        Time currentTime = new Time(game.getFrameCount());
        enemyInBase();
        enemyInNatural();

        for(EnemyUnits enemyUnit : enemyUnits) {
            if(enemyUnit.getEnemyUnit().isVisible()) {
                if(enemyUnit.getEnemyUnit().getType() != enemyUnit.getEnemyType()) {
                    updateUnitType(enemyUnit);
                }
                enemyUnit.setEnemyPosition(enemyUnit.getEnemyUnit().getPosition());
                enemyUnit.setEnemyTilePosition(enemyUnit.getEnemyUnit().getTilePosition());
                enemyUnit.setBurrowed(enemyUnit.getEnemyUnit().isBurrowed());
            }

            //Remove irradiated units that die out of view
            if(enemyUnit.getEnemyUnit().isIrradiated()) {
                enemyUnit.setIrradiateTimer();
            }
        }

        enemyUnits.removeIf(eu -> eu.getIrradiateTimer() > 240);
        validThreats.removeIf(eu -> eu.getIrradiateTimer() > 240);

        for(EnemyStrategy enemyStrategy : enemyStrategyManager.getEnemyStrategies()) {
            if(enemyStrategy.isEnemyStrategy(enemyUnits, currentTime) && enemyOpener == null) {
                enemyOpener = enemyStrategy;
                gameState.setEnemyOpener(enemyOpener);
                game.sendText("Potential enemy opener detected: " + enemyStrategy.getStrategyName());
                break;
            }
        }

        checkOpenerDefense(currentTime);
        checkTechUnits();
    }

    public void onUnitDiscover(Unit unit) {
        if(!previouslyDiscovered(unit)) {
            addEnemyUnit(unit);

            if(unit.getType().isBuilding()) {
                gameState.setEnemyBuildingDiscovered(true);
            }
        }

        if (gameState.getStartingEnemyBase() == null) {
            if(unit.getType().isResourceDepot()) {
                for(EnemyUnits enemyUnit : enemyUnits) {
                    if(enemyUnit.getEnemyType().isResourceDepot() && enemyUnit.getEnemyID() == unit.getID()) {
                        gameState.setStartingEnemyBase(enemyUnit);
                        break;
                    }
                }
            }
        }

        if(isThreat(unit)) {
            for(EnemyUnits threat : validThreats) {
                if(threat.getEnemyID() == unit.getID()) {
                    return;
                }
            }

            for(EnemyUnits enemyUnit : enemyUnits) {
                if(enemyUnit.getEnemyID() == unit.getID()) {
                    validThreats.add(enemyUnit);
                    break;
                }
            }

        }

    }

    public void onUnitShow(Unit unit) {
        if(isThreat(unit)) {
            for(EnemyUnits threat : validThreats) {
                if(threat.getEnemyID() == unit.getID()) {
                    return;
                }
            }
            for(EnemyUnits enemyUnit : enemyUnits) {
                if(enemyUnit.getEnemyID() == unit.getID()) {
                    validThreats.add(enemyUnit);
                }
            }
        }
    }

    public void onUnitDestroy(Unit unit) {
        for (EnemyUnits enemyUnit : enemyUnits) {
            if(enemyOpener != null && enemyOpener.getPriorityEnemyUnit() != null) {
                if(enemyOpener.getPriorityEnemyUnit().getEnemyID() == unit.getID()) {
                    enemyOpener.setPriorityEnemyUnit(null);
                }
            }

            if (enemyUnit.getEnemyID() == unit.getID()) {
                enemyUnits.remove(enemyUnit);
                break;
            }
        }

        for(EnemyUnits threat : validThreats) {
            if(threat.getEnemyID() == unit.getID()) {
                validThreats.remove(threat);
                break;
            }
        }

        if(!checkForBuildings() && unit.getType().isBuilding()) {
            gameState.setEnemyBuildingDiscovered(false);
        }
    }

    public void onUnitRenegade(Unit unit) {
        if(unit.getPlayer() != game.self() && game.enemy() ==  unit.getPlayer() && !previouslyDiscovered(unit)) {
            if(unit.getType() == UnitType.Zerg_Extractor || unit.getType() == UnitType.Terran_Refinery || unit.getType() == UnitType.Protoss_Assimilator) {
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

    public BaseInfo getBaseInfo() {
        return baseInfo;
    }

    public HashSet<EnemyTechUnits> getEnemyTechUnits() {
        return enemyTechUnits;
    }

    public HashSet<UnitType> getTechUnitResponse() {
        return techunitResponse;
    }
}
