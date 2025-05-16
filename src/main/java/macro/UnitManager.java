package macro;

import bwapi.Game;
import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;
import debug.Painters;
import information.BaseInfo;
import information.EnemyInformation;
import information.EnemyUnits;
import macro.unitgroups.CombatUnits;
import macro.unitgroups.UnitStatus;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class UnitManager {

    private EnemyInformation enemyInformation;
    private BaseInfo baseInfo;
    private Game game;
    private Painters painters;
    private HashSet<CombatUnits> combatUnits = new HashSet<>();
    private HashMap<UnitType, Integer> unitCount = new HashMap<>();

    public UnitManager(EnemyInformation enemyInformation, BaseInfo baseInfo, Game game) {
        this.enemyInformation = enemyInformation;
        this.baseInfo = baseInfo;
        this.game = game;

        painters = new Painters(game);
    }

    public void onFrame() {
        paintRanges();
        //painters.paintNaturalChoke(baseInfo.getNaturalChoke());
        int frameCount = game.getFrameCount();

        if(frameCount % 12 != 0) {
            return;
        }

        for(CombatUnits combatUnit : combatUnits) {
            if (combatUnit.getEnemyUnit() == null) {
                updateClosetEnemy(combatUnit);
            }

            if(combatUnit.getRallyPoint() == null) {
                setRallyPoint(combatUnit);
            }

            updateClosetEnemy(combatUnit);

            UnitStatus unitStatus = combatUnit.getUnitStatus();

            if(unitCount.get(UnitType.Terran_Marine) > 14 && unitStatus == UnitStatus.RALLY) {
                combatUnit.setUnitStatus(UnitStatus.ATTACK);
            }

            switch(unitStatus) {
                case ATTACK:
                    combatUnit.attack();
                    break;
                case RALLY:
                    combatUnit.rally();
                    break;
            }
        }
    }

    public void onUnitComplete(Unit unit) {
        combatUnits.add(new CombatUnits(unit));
        unitCount.put(unit.getType(), unitCount.getOrDefault(unit.getType(), 0) + 1);
    }

    public void onUnitDestroy(Unit unit) {

        Iterator<CombatUnits> iterator = combatUnits.iterator();
        while (iterator.hasNext()) {
            CombatUnits combatUnit = iterator.next();
            if (combatUnit.getUnitID() == unit.getID()) {
                iterator.remove();
                break;
            }
        }

        unitCount.put(unit.getType(), unitCount.get(unit.getType()) - 1);
    }

    public void updateClosetEnemy(CombatUnits combatUnit) {
        int closestDistance = Integer.MAX_VALUE;
        EnemyUnits closestEnemy = null;

        for (EnemyUnits enemyUnit : enemyInformation.getEnemyUnits()) {
            Position enemyPosition = enemyUnit.getEnemyPosition();
            Position unitPosition = combatUnit.getUnit().getPosition();

            //Stop units from getting stuck on outdated position info
            if(combatUnit.getUnit().getDistance(enemyPosition) < 150 && !enemyUnit.getEnemyUnit().isVisible()) {
                continue;
            }

            if(!combatUnit.getUnit().hasPath(enemyPosition)) {
                continue;
            }

            if(enemyUnit.getEnemyUnit().isCloaked() || enemyUnit.getEnemyUnit().isBurrowed() || enemyUnit.getEnemyUnit().isMorphing() || enemyUnit.getEnemyUnit().getType() == UnitType.Zerg_Overlord) {
                continue;
            }

            int distance = unitPosition.getApproxDistance(enemyPosition);

            if (distance < closestDistance) {
                closestDistance = distance;
                closestEnemy = enemyUnit;
            }
        }

        if (closestEnemy != null) {
            combatUnit.setEnemyUnit(closestEnemy);
        }
    }

    //Debug painters
    public void paintRanges() {
        for(CombatUnits combatUnit : combatUnits) {
            //painters.drawAttackRange(combatUnit.getUnit());
            painters.paintUnitStatus(combatUnit);
            painters.paintClosestEnemy(combatUnit);
            painters.paintStimStatus(combatUnit);
        }

    }

    //TODO: make this dynamic
    private void setRallyPoint(CombatUnits combatUnit) {
        combatUnit.setRallyPoint(baseInfo.getMainChoke().getCenter().toTilePosition());
    }


    public HashSet<CombatUnits> getCombatUnits() {
        return combatUnits;
    }
}