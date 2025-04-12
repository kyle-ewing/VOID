package information;

import bwapi.Game;
import bwapi.Unit;
import bwem.Base;

import java.util.HashSet;

public class EnemyInformation {
    private HashSet<EnemyUnits> enemyUnits = new HashSet<>();
    private BaseInfo baseInfo;
    private Game game;
    private Unit startingEnemyBase = null;
    private Base enemyNatural = null;

    public EnemyInformation(BaseInfo baseInfo, Game game) {
        this.baseInfo = baseInfo;
        this.game = game;
    }

    private boolean previouslyDiscovered(Unit unit) {
        for (EnemyUnits enemyUnit : enemyUnits) {
            if (enemyUnit.getEnemyID() == unit.getID()) {
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
    }

    public void onUnitDiscover(Unit unit) {
        if (startingEnemyBase == null) {
            if(unit.getType().isResourceDepot()) {
                startingEnemyBase = unit;
            }
        }

        if(!previouslyDiscovered(unit)) {
            addEnemyUnit(unit);
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
}
