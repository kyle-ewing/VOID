package macro;

import bwapi.*;
import bwem.Base;
import debug.Painters;
import information.BaseInfo;
import information.EnemyInformation;
import information.EnemyUnits;
import information.Scouting;
import macro.unitgroups.CombatUnits;
import macro.unitgroups.UnitStatus;
import util.PositionInterpolator;
import util.Time;

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
    private int scouts = 0;
    private int rallyClock = 0;
    private Unit bunker = null;
    private boolean bunkerunLoaded = false;
    private boolean beingAllInned = false;
    private boolean defendedAllIn = false;


    public UnitManager(EnemyInformation enemyInformation, BaseInfo baseInfo, Game game, Scouting scouting) {
        this.enemyInformation = enemyInformation;
        this.baseInfo = baseInfo;
        this.game = game;
        this.scouting = scouting;

        painters = new Painters(game);
        initUnitCounts();
    }

    public void onFrame() {
        paintRanges();
        enemyOpenerResponse();
        //painters.paintNaturalChoke(baseInfo.getNaturalChoke());
        int frameCount = game.getFrameCount();

        if(enemyInformation.getEnemyOpener() != null && beingAllInned && !defendedAllIn) {
            rallyClock++;
        }

        if(new Time(rallyClock).greaterThan(new Time(1, 0))) {
            if(!enemyInformation.enemysInMain()) {
                defendedAllIn = true;
                rallyClock = 0;
            }
            else {
                rallyClock = 0;
            }
        }

        if(frameCount % 12 != 0) {
            return;
        }

        for(CombatUnits combatUnit : combatUnits) {
            if(combatUnit.getUnitType() == UnitType.Spell_Scanner_Sweep) {
                continue;
            }

            if(combatUnit.getRallyPoint() == null && (combatUnit.getUnitStatus() != UnitStatus.ADDON)) {
                setRallyPoint(combatUnit);
            }

            UnitStatus unitStatus = combatUnit.getUnitStatus();

            if((unitCount.get(UnitType.Terran_Marine) > 18 || unitCount.get(UnitType.Terran_Siege_Tank_Tank_Mode) > 2) && (unitStatus == UnitStatus.RALLY || unitStatus == UnitStatus.LOAD)) {
                if(bunker != null) {
                    unLoadBunker(combatUnit);
                }

                combatUnit.setUnitStatus(UnitStatus.ATTACK);
            }

            if((unitStatus == UnitStatus.RALLY || unitStatus == UnitStatus.DEFEND)) {
                combatUnit.setResetClock(combatUnit.getResetClock() + 12);

                if(new Time(combatUnit.getResetClock()).greaterThan(new Time(0, 30))) {
                    setRallyPoint(combatUnit);
                    combatUnit.setResetClock(0);
                }

                if(combatUnit.getUnitType() == UnitType.Terran_Marine && !combatUnit.isInBunker() && (bunker != null && bunkerLoad < 4)) {
                    combatUnit.setUnitStatus(UnitStatus.LOAD);
                }
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
                    if(enemyInBase()) {
                        updateClosetEnemy(combatUnit, 900);
                    }
                    else {
                        updateClosetEnemy(combatUnit, 300);
                    }
                    combatUnit.rally();
                    break;
                case LOAD:
                    loadBunker(combatUnit);
                    break;
                case DEFEND:
                    if(enemyInBase()) {
                        updateClosetEnemy(combatUnit, 900);
                    }
                    else {
                        updateClosetEnemy(combatUnit, 250);
                    }
                    combatUnit.defend();
                    break;
                case SCOUT:
                    scoutBases();
                    break;
                case ADDON:
                    scanInvisibleUnits(combatUnit);
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
            if(combatUnit.getUnit().getDistance(enemyPosition) < 250 && !enemyUnit.getEnemyUnit().exists()) {
                enemyUnit.setEnemyPosition(null);
                continue;
            }

            if(!combatUnit.getUnit().hasPath(enemyPosition)) {
                continue;
            }

            if(!combatUnit.getUnit().getType().airWeapon().targetsAir() && enemyUnit.getEnemyUnit().getType().isFlyer()) {
                continue;
            }

            if(enemyUnit.getEnemyUnit().isCloaked() || enemyUnit.getEnemyUnit().isBurrowed() || enemyUnit.getEnemyUnit().isMorphing() || enemyUnit.getEnemyUnit().getType() == UnitType.Zerg_Overlord) {
                continue;
            }

            if(enemyPosition == null) {
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

    private boolean enemyInBase() {
        for(EnemyUnits enemyUnit : enemyInformation.getEnemyUnits()) {
            if(enemyUnit.getEnemyPosition() == null) {
                continue;
            }

            if(baseInfo.getStartingBase().getLocation().getApproxDistance(enemyUnit.getEnemyPosition().toTilePosition()) < 800) {
                return true;
            }
        }
        return false;
    }

    public void loadBunker(CombatUnits combatUnit) {
        combatUnit.getUnit().load(bunker);

        if(!combatUnit.isInBunker()) {
            bunkerLoad++;
        }

        combatUnit.setInBunker(true);
    }

    public void unLoadBunker(CombatUnits combatUnit) {
        bunker.unloadAll();
        bunkerLoad = 0;
        combatUnit.setInBunker(false);
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

    private void enemyOpenerResponse() {
        if(enemyInformation.getEnemyOpener() == null) {
            return;
        }

        switch(enemyInformation.getEnemyOpener().getStrategyName()) {
            case "Cannon Rush":
                this.beingAllInned = true;
                break;
            case "Four Pool":
                this.beingAllInned = true;
                break;
        }
    }

    //Reorganize when more enemy openers are added to be more condensed
    private void setRallyPoint(CombatUnits combatUnit) {
        if(enemyInformation.getEnemyOpener() == null || defendedAllIn) {
            if((baseInfo.getOwnedBases().contains(baseInfo.getNaturalBase()))) {
                combatUnit.setRallyPoint(PositionInterpolator.interpolate(baseInfo.getNaturalBase().getLocation(), baseInfo.getNaturalChoke().getCenter().toTilePosition(), 0.8));
            }
            else {
                combatUnit.setRallyPoint(PositionInterpolator.interpolate(baseInfo.getStartingBase().getLocation(), baseInfo.getMainChoke().getCenter().toTilePosition(), 0.8));
            }
        }
        else if(enemyInformation.getEnemyOpener().getStrategyName().equals("Four Pool")) {
            combatUnit.setRallyPoint(baseInfo.getStartingBase().getCenter().toTilePosition());
        }
        else if(enemyInformation.getEnemyOpener().getStrategyName().equals("Cannon Rush")) {
            combatUnit.setRallyPoint(baseInfo.getStartingBase().getCenter().toTilePosition());
        }
    }

    private void scanInvisibleUnits(CombatUnits combatUnit) {
        if(combatUnit.getUnitType() != UnitType.Terran_Comsat_Station) {
            return;
        }

        if(isActiveScanNearUnit()) {
            return;
        }

        for(EnemyUnits enemyUnit : enemyInformation.getEnemyUnits()) {
            if(enemyUnit.getEnemyType() == UnitType.Protoss_Observer) {
                continue;
            }

            if((enemyUnit.getEnemyUnit().isCloaked() || enemyUnit.getEnemyUnit().isBurrowed()) && enemyUnit.getEnemyUnit().isVisible()) {
                combatUnit.getUnit().useTech(TechType.Scanner_Sweep, enemyUnit.getEnemyPosition());
            }
        }
    }

    private boolean isActiveScanNearUnit() {
        for(CombatUnits scanUnit : combatUnits) {
            if(scanUnit.getUnitStatus() == UnitStatus.SCAN) {
                for(EnemyUnits enemyUnit : enemyInformation.getEnemyUnits()) {
                    if((enemyUnit.getEnemyUnit().isCloaked() || enemyUnit.getEnemyUnit().isBurrowed()) && enemyUnit.getEnemyUnit().isVisible()) {
                        int distance = scanUnit.getUnit().getPosition().getApproxDistance(enemyUnit.getEnemyPosition());
                        if (distance <= 300) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void initUnitCounts()  {
        for(UnitType unitType : UnitType.values()) {
            if(unitType.getRace().toString().equals("Terran") && !unitType.isCritter() && !unitType.isHero() && !unitType.isBeacon() && !unitType.isSpecialBuilding()) {
                unitCount.put(unitType, 0);
            }
        }
    }


    public void onUnitComplete(Unit unit) {
        if(unit.getType() == UnitType.Terran_Bunker) {
            bunker = unit;
            return;
        }

        if(unit.getType() == UnitType.Terran_Comsat_Station) {
            combatUnits.add(new CombatUnits(unit, UnitStatus.ADDON));
            return;
        }
        else if(unit.getType() == UnitType.Spell_Scanner_Sweep) {
            combatUnits.add(new CombatUnits(unit, UnitStatus.SCAN));
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

        if(unit.getType().isBuilding() && unit.getType() != UnitType.Terran_Comsat_Station) {
            return;
        }

        Iterator<CombatUnits> iterator = combatUnits.iterator();
        while (iterator.hasNext()) {
            CombatUnits combatUnit = iterator.next();
            if (combatUnit.getUnitID() == unit.getID()) {
                if(combatUnit.getUnitStatus() == UnitStatus.LOAD) {
                    bunkerLoad--;
                }

                if(combatUnit.getUnitStatus() == UnitStatus.SCOUT) {
                    unassignScout(combatUnit);
                }

                iterator.remove();
                break;
            }
        }

        if(unitCount.containsKey(unit.getType())) {
            unitCount.put(unit.getType(), unitCount.get(unit.getType()) - 1);
        }
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

    public HashSet<CombatUnits> getCombatUnits() {
        return combatUnits;
    }
}