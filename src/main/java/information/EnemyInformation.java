package information;

import bwapi.Game;
import bwapi.Unit;
import bwem.Base;
import information.enemyopeners.EnemyStrategy;

import java.util.HashSet;

public class EnemyInformation {
    private HashSet<EnemyUnits> enemyUnits = new HashSet<>();
    private BaseInfo baseInfo;
    private Game game;
    private Unit startingEnemyBase = null;
    private Base enemyNatural = null;
    private EnemyStrategyManager enemyStrategyManager;
    private EnemyStrategy enemyOpener;
    private boolean enemyBuildingDiscovered = false;

    public EnemyInformation(BaseInfo baseInfo, Game game) {
        this.baseInfo = baseInfo;
        this.game = game;
        enemyStrategyManager = new EnemyStrategyManager();
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
            if (enemyUnit.getEnemyUnit().getType().isBuilding() && enemyUnit.getEnemyUnit().exists()) {
                return true;
            }
        }
        return false;
    }

    public void onFrame() {
        for(EnemyUnits enemyUnit : enemyUnits) {
            if(enemyUnit.getEnemyUnit().isVisible()) {
                if(enemyUnit.getEnemyUnit().getType() != enemyUnit.getEnemyType()) {
                    updateUnitType(enemyUnit);
                }
                enemyUnit.setEnemyPosition(enemyUnit.getEnemyUnit().getPosition());
            }
        }

        for(EnemyStrategy enemyStrategy : enemyStrategyManager.getEnemyStrategies()) {
            if(enemyStrategy.isEnemyStrategy(enemyUnits, game.getFrameCount())) {
                enemyOpener = enemyStrategy;
                break;
            }
        }

        if(enemyOpener != null) {
            game.drawTextScreen(5, 60, "Enemy Opener: " + enemyOpener.getStrategyName());
        }
        else {
            game.drawTextScreen(5, 60, "Enemy Opener: Unknown");
        }
    }

    public void onUnitDiscover(Unit unit) {
        if (startingEnemyBase == null) {
            if(unit.getType().isResourceDepot()) {
                startingEnemyBase = unit;
            }
        }

        if(!previouslyDiscovered(unit)) {
            addEnemyUnit(unit);

            if(unit.getType().isBuilding()) {
                enemyBuildingDiscovered = true;
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
            if (enemyUnit.getEnemyID() == unit.getID()) {
                enemyUnits.remove(enemyUnit);
                break;
            }
        }

        if(!checkForBuildings() && unit.getType().isBuilding()) {
            enemyBuildingDiscovered = false;
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

    public Unit getStartingEnemyBase() {
        return startingEnemyBase;
    }

    public EnemyStrategy getEnemyOpener() {
        return enemyOpener;
    }

    public boolean isEnemyBuildingDiscovered() {
        return enemyBuildingDiscovered;
    }
}
