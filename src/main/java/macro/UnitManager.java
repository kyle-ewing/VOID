package macro;

import bwapi.Game;
import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;
import bwem.Base;
import debug.Painters;
import information.BaseInfo;
import information.EnemyInformation;
import information.EnemyUnits;
import information.Scouting;
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
    private Scouting scouting;
    private HashSet<CombatUnits> combatUnits = new HashSet<>();
    private HashMap<UnitType, Integer> unitCount = new HashMap<>();
    private HashMap<Base, CombatUnits> designatedScouts = new HashMap<>();
    private int bunkerLoad = 0;
    private Unit bunker = null;
    private int scouts = 0;


    public UnitManager(EnemyInformation enemyInformation, BaseInfo baseInfo, Game game, Scouting scouting) {
        this.enemyInformation = enemyInformation;
        this.baseInfo = baseInfo;
        this.game = game;
        this.scouting = scouting;

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
            if(combatUnit.getRallyPoint() == null) {
                setRallyPoint(combatUnit);
            }


            UnitStatus unitStatus = combatUnit.getUnitStatus();

            if(unitCount.get(UnitType.Terran_Marine) > 14 && (unitStatus == UnitStatus.RALLY || unitStatus == UnitStatus.LOAD)) {
                if(bunker != null) {
                    unLoadBunker();
                }

                combatUnit.setUnitStatus(UnitStatus.ATTACK);
            }

            if((unitStatus == UnitStatus.RALLY || unitStatus == UnitStatus.DEFEND) && bunker != null && bunkerLoad < 4) {
                combatUnit.setUnitStatus(UnitStatus.LOAD);
                bunkerLoad++;
            }

            if(scouting.isCompletedScout() && !enemyInformation.isEnemyBuildingDiscovered() && combatUnit.getUnitType() == UnitType.Terran_Marine && scouts < baseInfo.getMapBases().size()) {
                combatUnit.setUnitStatus(UnitStatus.SCOUT);
                assignScouts(combatUnit);
                scouts++;
            }

//            unitStatus = combatUnit.getUnitStatus();

            switch(unitStatus) {
                case ATTACK:
                    updateClosetEnemy(combatUnit, Integer.MAX_VALUE);
                    combatUnit.attack();
                    break;
                case RALLY:
                    updateClosetEnemy(combatUnit, 500);
                    combatUnit.rally();
                    break;
                case LOAD:
                    loadBunker(combatUnit);
                    break;
                case DEFEND:
                    updateClosetEnemy(combatUnit, 500);
                    combatUnit.defend();
                    break;
                case SCOUT:
                    scoutBases();
                    break;
            }
        }
    }

    public void updateClosetEnemy(CombatUnits combatUnit, int range) {
        int closestDistance = range;
        EnemyUnits closestEnemy = null;

        for (EnemyUnits enemyUnit : enemyInformation.getEnemyUnits()) {
            Position enemyPosition = enemyUnit.getEnemyPosition();
            Position unitPosition = combatUnit.getUnit().getPosition();

            //Stop units from getting stuck on outdated position info
            if(combatUnit.getUnit().getDistance(enemyPosition) < 250 && !enemyUnit.getEnemyUnit().isVisible()) {
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

        if(closestEnemy == null) {
            combatUnit.setEnemyUnit(null);
        }
    }

    public void loadBunker(CombatUnits combatUnit) {
        if(!combatUnit.isInBunker() && combatUnit.getUnit().getType() == UnitType.Terran_Marine) {
            combatUnit.getUnit().load(bunker);
            combatUnit.setInBunker(true);
        }
    }

    public void unLoadBunker() {
        bunker.unloadAll();
        bunkerLoad = 0;
    }

    private void assignScouts(CombatUnits combatUnit) {
        for(Base base : baseInfo.getMapBases()) {

            if(designatedScouts.containsKey(base)) {
                continue;
            }

            designatedScouts.put(base, combatUnit);
            break;

        }
    }

    private void unassignScout(CombatUnits combatUnit) {
        for(Base base : designatedScouts.keySet()) {
            if(designatedScouts.get(base).getUnitID() == combatUnit.getUnitID()) {
                designatedScouts.remove(base);
                scouts--;
                break;
            }
        }
    }

    private void scoutBases() {
        for(Base base : designatedScouts.keySet()) {
            CombatUnits scout = designatedScouts.get(base);

            scout.getUnit().attack(base.getCenter());

            if(enemyInformation.isEnemyBuildingDiscovered()) {
                scout.setUnitStatus(UnitStatus.RALLY);
            }
        }

        if(enemyInformation.isEnemyBuildingDiscovered()) {
            scouts = 0;
            designatedScouts.clear();
        }
    }


    public void onUnitComplete(Unit unit) {
        if(unit.getType() == UnitType.Terran_Bunker) {
            bunker = unit;
            return;
        }

        combatUnits.add(new CombatUnits(unit));
        unitCount.put(unit.getType(), unitCount.getOrDefault(unit.getType(), 0) + 1);
    }

    public void onUnitDestroy(Unit unit) {
        if(unit.getType() == UnitType.Terran_Bunker) {
            bunker = null;
            return;
        }

        Iterator<CombatUnits> iterator = combatUnits.iterator();
        while (iterator.hasNext()) {
            CombatUnits combatUnit = iterator.next();
            if (combatUnit.getUnitID() == unit.getID()) {
                if(combatUnit.isInBunker()) {
                    bunkerLoad--;
                }

                if(combatUnit.getUnitStatus() == UnitStatus.SCOUT) {
                    unassignScout(combatUnit);
                }

                iterator.remove();
                break;
            }
        }

        unitCount.put(unit.getType(), unitCount.get(unit.getType()) - 1);
    }

    //Debug painters
    public void paintRanges() {
        for(CombatUnits combatUnit : combatUnits) {
            //painters.drawAttackRange(combatUnit.getUnit());
            painters.paintUnitStatus(combatUnit);
            painters.paintClosestEnemy(combatUnit);
            painters.paintStimStatus(combatUnit);
            painters.paintCombatScouts(combatUnit);
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