package information.enemy;

import bwapi.Game;
import bwapi.Unit;
import bwapi.UnitType;
import bwem.Base;
import information.BaseInfo;
import information.enemy.enemyopeners.EnemyStrategy;
import information.enemy.enemytechunits.EnemyTechUnits;
import util.Time;

import java.util.HashSet;

public class EnemyInformation {
    private HashSet<EnemyUnits> enemyUnits = new HashSet<>();
    private HashSet<EnemyTechUnits> enemyTechUnits = new HashSet<>();
    private HashSet<UnitType> enemyTechunitResponse = new HashSet<>();
    private BaseInfo baseInfo;
    private Game game;
    private EnemyUnits startingEnemyBase = null;
    private Base enemyNatural = null;
    private EnemyStrategyManager enemyStrategyManager;
    private EnemyStrategy enemyOpener;
    private boolean enemyBuildingDiscovered = false;

    public EnemyInformation(BaseInfo baseInfo, Game game) {
        this.baseInfo = baseInfo;
        this.game = game;
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

    public boolean enemyInBase() {
        for(EnemyUnits enemyUnit : enemyUnits) {
            if(!enemyUnit.getEnemyType().canAttack()) {
                continue;
            }

            if(baseInfo.getBaseTiles().contains(enemyUnit.getEnemyUnit().getTilePosition())) {
                return true;
            }
            else if(baseInfo.getNaturalTiles().contains(enemyUnit.getEnemyUnit().getTilePosition())) {
                return true;
            }
        }
        return false;
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
                enemyTechunitResponse.add(enemyTechUnit.getResponseUnitType());
            }
            else if(!enemyTechUnit.isEnemyTechUnit(enemyUnits) && enemyTechUnits.contains(enemyTechUnit)) {
                enemyTechUnits.remove(enemyTechUnit);
                enemyTechunitResponse.remove(enemyTechUnit.getResponseUnitType());
            }
        }
    }

    public void onFrame() {
        Time currentTime = new Time(game.getFrameCount());

        for(EnemyUnits enemyUnit : enemyUnits) {
            if(enemyUnit.getEnemyUnit().isVisible()) {
                if(enemyUnit.getEnemyUnit().getType() != enemyUnit.getEnemyType()) {
                    updateUnitType(enemyUnit);
                }
                enemyUnit.setEnemyPosition(enemyUnit.getEnemyUnit().getPosition());
            }
        }

        for(EnemyStrategy enemyStrategy : enemyStrategyManager.getEnemyStrategies()) {
            if(enemyStrategy.isEnemyStrategy(enemyUnits, currentTime) && enemyOpener == null) {
                enemyOpener = enemyStrategy;
                game.sendText("Potential enemy opener detected: " + enemyStrategy.getStrategyName());
                break;
            }
        }

        checkTechUnits();

        if(enemyOpener != null) {
            game.drawTextScreen(5, 60, "Enemy Opener: " + enemyOpener.getStrategyName());
        }
        else {
            game.drawTextScreen(5, 60, "Enemy Opener: Unknown");
        }
    }

    public void onUnitDiscover(Unit unit) {
        if(!previouslyDiscovered(unit)) {
            addEnemyUnit(unit);

            if(unit.getType().isBuilding()) {
                enemyBuildingDiscovered = true;
            }
        }

        if (startingEnemyBase == null) {
            if(unit.getType().isResourceDepot()) {
                for(EnemyUnits enemyUnit : enemyUnits) {
                    if(enemyUnit.getEnemyType().isResourceDepot() && enemyUnit.getEnemyID() == unit.getID()) {
                        startingEnemyBase = enemyUnit;
                    }
                }
            }
        }

    }

    public void onUnitShow(Unit unit) {
//        while(enemyUnits.iterator().hasNext()) {
//            if(enemyUnits.iterator().next().getEnemyID() == unit.getID()) {
//                enemyUnits.iterator().next().setEnemyPosition(unit.getTilePosition());
//            }
//        }
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

        if(!checkForBuildings() && unit.getType().isBuilding()) {
            enemyBuildingDiscovered = false;
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

    public EnemyUnits getStartingEnemyBase() {
        return startingEnemyBase;
    }

    public EnemyStrategy getEnemyOpener() {
        return enemyOpener;
    }

    public boolean isEnemyBuildingDiscovered() {
        return enemyBuildingDiscovered;
    }

    public BaseInfo getBaseInfo() {
        return baseInfo;
    }

    public HashSet<EnemyTechUnits> getEnemyTechUnits() {
        return enemyTechUnits;
    }

    public HashSet<UnitType> getTechUnitResponse() {
        return enemyTechunitResponse;
    }
}
