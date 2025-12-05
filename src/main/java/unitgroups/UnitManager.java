package unitgroups;

import bwapi.*;
import bwem.Base;
import information.BaseInfo;
import information.GameState;
import information.enemy.EnemyInformation;
import information.enemy.EnemyUnits;
import information.Scouting;
import unitgroups.units.*;
import map.PathFinding;
import util.ClosestUnit;
import util.RallyPoint;
import util.Time;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class UnitManager {

    private EnemyInformation enemyInformation;
    private BaseInfo baseInfo;
    private Game game;
    private GameState gameState;
    private CombatUnitCreator combatUnitCreator;
    private Scouting scouting;
    private RallyPoint rallyPoint;
    private PathFinding pathFinding;
    private HashSet<CombatUnits> combatUnits;
    private HashMap<UnitType, Integer> unitCount;
    private HashMap<Base, CombatUnits> designatedScouts = new HashMap<>();
    private EnemyUnits priorityTarget = null;
    private int bunkerLoad = 0;
    private int scouts = 0;
    private int rallyClock = 0;
    private Unit bunker = null;
    private boolean beingAllInned = false;
    private boolean defendedAllIn = false;


    public UnitManager(EnemyInformation enemyInformation, GameState gameState, BaseInfo baseInfo, Game game, Scouting scouting) {
        this.enemyInformation = enemyInformation;
        this.gameState = gameState;
        this.baseInfo = baseInfo;
        this.game = game;
        this.scouting = scouting;
        this.combatUnitCreator = new CombatUnitCreator(game, enemyInformation);
        this.pathFinding = baseInfo.getPathFinding();
        this.rallyPoint = new RallyPoint(pathFinding, gameState, baseInfo);

        combatUnits = gameState.getCombatUnits();
        unitCount = gameState.getUnitTypeCount();
    }

    public void onFrame() {
        enemyOpenerResponse();
        rallyPoint.onFrame();
        int frameCount = game.getFrameCount();

        if(gameState.getEnemyOpener() != null && beingAllInned && !defendedAllIn) {
            rallyClock++;
        }

        if(new Time(rallyClock).greaterThan(new Time(1, 0))) {
            if(!gameState.isEnemyInBase()) {
                defendedAllIn = true;
                rallyClock = 0;
            }
            else {
                rallyClock = 0;
            }
        }

        if(frameCount % 8 != 0) {
            return;
        }

        //TODO: this is all horrible
        for(CombatUnits combatUnit : combatUnits) {

            //Skip over units who don't move or need to be controlled
            if(combatUnit.hasStaticStatus()) {
                continue;
            }

            combatUnit.onFrame();
            inRangeOfThreat(combatUnit);

            //Set rally point if not set
            if(combatUnit.getRallyPoint() == null || (baseInfo.getNaturalBase() != null && !combatUnit.isNaturalRallySet())) {
                if(baseInfo.getOwnedBases().contains(baseInfo.getNaturalBase())) {
                    combatUnit.setNaturalRallySet(true);
                }

                rallyPoint.setRallyPoint(combatUnit);
            }

            switch (combatUnit.getUnitType()) {
                case Terran_Marine:
                    bunkerStatus(combatUnit);
                    break;
                case Terran_Medic:
                    ClosestUnit.findClosestFriendlyUnit(combatUnit, combatUnits, UnitType.Terran_Marine);
                    break;
                case Terran_Science_Vessel:
                    ClosestUnit.findClosestFriendlyUnit(combatUnit, combatUnits, UnitType.Terran_Marine);
                    ClosestUnit.priorityTargets(combatUnit, combatUnit.getPriorityTargets(), gameState.getKnownEnemyUnits(), Integer.MAX_VALUE);
                    break;
            }

            combatUnit.setInBase(baseInfo.getBaseTiles().contains(combatUnit.getUnit().getTilePosition()));

            UnitStatus unitStatus = combatUnit.getUnitStatus();

            if(priorityTarget != null) {
                combatUnit.setPriorityEnemyUnit(priorityTarget);

                if(!priorityTargetExists()) {
                    priorityTarget = null;
                    combatUnit.setPriorityEnemyUnit(null);
                }
            }

            if(moveOutConditionsMet(gameState.getOpenerMoveOutCondition())) {
                if(unitStatus == UnitStatus.RALLY || unitStatus == UnitStatus.LOAD || unitStatus == UnitStatus.SIEGEDEF) {
                    if(bunker != null) {
                        unLoadBunker(combatUnit);
                    }

                    if(combatUnit instanceof SiegeTank) {
                        if(((SiegeTank) combatUnit).isSieged()) {
                            combatUnit.getUnit().unsiege();
                        }
                    }

                    if(combatUnit.getUnitType() == UnitType.Terran_Vulture) {
                        ((Vulture) combatUnit).setLobotomyOverride(true);
                    }

                    combatUnit.setUnitStatus(UnitStatus.ATTACK);
                }
            }
            else {
                if(combatUnit.getUnitType() == UnitType.Terran_Vulture) {
                    ((Vulture) combatUnit).setLobotomyOverride(false);
                }
            }

            if((scouting.isCompletedScout() || scouting.isAttemptsMaxed()) && !gameState.isEnemyBuildingDiscovered() && (combatUnit.getUnitType() == UnitType.Terran_Marine || combatUnit.getUnitType() == UnitType.Terran_Vulture ) && scouts < baseInfo.getMapBases().size()) {
                combatUnit.setUnitStatus(UnitStatus.SCOUT);
                assignScouts(combatUnit);
                scouts++;
            }

            if(hasTankSupport(combatUnit)) {
                combatUnit.setHasTankSupport(true);
            }
            else {
                combatUnit.setHasTankSupport(false);
            }

//            unitStatus = combatUnit.getUnitStatus();

            switch(unitStatus) {
                case ATTACK:
                    ClosestUnit.findClosestUnit(combatUnit, gameState.getKnownEnemyUnits(), Integer.MAX_VALUE);

                    if(combatUnit.getUnitType() == UnitType.Terran_Marine) {
                        if(combatUnit.isInRangeOfThreat() && typeOfThreat(combatUnit) == UnitType.Zerg_Lurker) {
                            avoidThreat(combatUnit);
                            combatUnit.setUnitStatus(UnitStatus.RETREAT);
                            continue;
                        }
                        else if(combatUnit.isInRangeOfThreat() && combatUnit.hasTankSupport()) {
                            avoidThreat(combatUnit);
                            combatUnit.setUnitStatus(UnitStatus.RETREAT);
                            continue;
                        }
                        combatUnit.attack();
                        break;
                    }

                    if(combatUnit.isInRangeOfThreat()) {
                        avoidThreat(combatUnit);
                        combatUnit.setUnitStatus(UnitStatus.RETREAT);
                        break;
                    }

                    combatUnit.attack();
                    break;
                case RALLY:
                    //Avoid units who can't shoot up trying to defend against air
                    if(gameState.isEnemyInBase() && gameState.enemyFlyerInBase() && combatUnit.getUnitType().airWeapon().targetsAir()) {
                        combatUnit.setEnemyInBase(true);
                        ClosestUnit.findClosestUnit(combatUnit, gameState.getKnownEnemyUnits(), 900);
                    }
                    else if(gameState.isEnemyInBase() && !gameState.enemyFlyerInBase()) {
                        combatUnit.setEnemyInBase(true);
                        ClosestUnit.findClosestUnit(combatUnit, gameState.getKnownEnemyUnits(), 900);
                    }
                    else if(priorityTarget != null) {
                        combatUnit.setEnemyInBase(true);
                        combatUnit.setEnemyUnit(priorityTarget);
                    }
                    else {
                        combatUnit.setEnemyInBase(false);
                        ClosestUnit.findClosestUnit(combatUnit, gameState.getKnownEnemyUnits(), 150);
                    }

                    if(obstructingBuild(combatUnit)) {
                        break;
                    }

                    if(combatUnit.isInRangeOfThreat()) {
                        avoidThreat(combatUnit);
                        combatUnit.setUnitStatus(UnitStatus.RETREAT);
                        break;
                    }

                    rallyClockReset(combatUnit);
                    combatUnit.rally();
                    break;
                case LOAD:
                    loadBunker(combatUnit);
                    break;
                case DEFEND:
                    if(gameState.isEnemyInBase()) {
                        combatUnit.setEnemyInBase(true);
                        ClosestUnit.findClosestUnit(combatUnit, gameState.getKnownEnemyUnits(), 1000);
                    }
                    else if(priorityTarget != null) {
                        combatUnit.setEnemyInBase(true);
                        combatUnit.setEnemyUnit(priorityTarget);
                    }
                    else {
                        combatUnit.setEnemyInBase(false);
                        ClosestUnit.findClosestUnit(combatUnit, gameState.getKnownEnemyUnits(), 250);
                    }

                    if(obstructingBuild(combatUnit)) {
                        break;
                    }

                    if(combatUnit.isInRangeOfThreat()) {
                        avoidThreat(combatUnit);
                        combatUnit.setUnitStatus(UnitStatus.RETREAT);
                        break;
                    }

                    rallyClockReset(combatUnit);
                    combatUnit.defend();
                    break;
                case RETREAT:
                    if(!combatUnit.isInRangeOfThreat()) {
                        ClosestUnit.findClosestUnit(combatUnit, gameState.getKnownEnemyUnits(), Integer.MAX_VALUE);
                    }
                    else {
                        avoidThreat(combatUnit);
                    }

                    combatUnit.retreat();
                    break;
                case SCOUT:
                    scoutBases();
                    break;
                case ADDON:
                    scanInvisibleUnits(combatUnit);
                    break;
                case OBSTRUCTING:
                    if(!obstructingBuild(combatUnit)) {
                        combatUnit.setUnitStatus(UnitStatus.RALLY);
                    }

                    moveFromObstruction(combatUnit);
                    break;
                case SIEGEDEF:
                    ((SiegeTank) combatUnit).siegeDef();

                    if(gameState.isEnemyInBase()) {
                        combatUnit.setEnemyInBase(true);
                        ClosestUnit.findClosestUnit(combatUnit, gameState.getKnownEnemyUnits(), 900);
                    }
                    break;
                case HUNTING:
                    ClosestUnit.priorityTargets(combatUnit, combatUnit.getPriorityTargets(), gameState.getKnownEnemyUnits(), Integer.MAX_VALUE);
                    combatUnit.hunting();
            }
        }
    }

    private void loadBunker(CombatUnits combatUnit) {
        combatUnit.getUnit().load(bunker);

        if(!combatUnit.isInBunker()) {
            bunkerLoad++;
        }

        combatUnit.setInBunker(true);
    }

    private void unLoadBunker(CombatUnits combatUnit) {
        bunker.unloadAll();
        bunkerLoad = 0;
        combatUnit.setInBunker(false);
    }

    private void bunkerStatus(CombatUnits combatUnit) {
        if(!combatUnit.isInBunker() && (bunker != null && bunkerLoad < 4 && priorityTarget == null) && combatUnit.getUnitStatus() != UnitStatus.ATTACK) {
            combatUnit.setUnitStatus(UnitStatus.LOAD);
        }
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

            if(gameState.isEnemyBuildingDiscovered()) {
                scout.setUnitStatus(UnitStatus.RALLY);
            }
        }

        if(gameState.isEnemyBuildingDiscovered()) {
            scouts = 0;
            designatedScouts.clear();
        }
    }

    private void rallyClockReset(CombatUnits combatUnit) {
        combatUnit.setResetClock(combatUnit.getResetClock() + 12);

        if(new Time(combatUnit.getResetClock()).greaterThan(new Time(0, 30))) {
            rallyPoint.setRallyPoint(combatUnit);
            combatUnit.setResetClock(0);
        }
    }

    private void enemyOpenerResponse() {
        if(gameState.getEnemyOpener() == null) {
            return;
        }

        switch(gameState.getEnemyOpener().getStrategyName()) {
            case "Cannon Rush":
                this.beingAllInned = true;
                break;
            case "Four Pool":
                this.beingAllInned = true;
                break;
            case "Gas Steal":
                //Not an all in but needs response
                this.beingAllInned = true;
                priorityTarget = gameState.getEnemyOpener().getPriorityEnemyUnit();
                break;
            case "Dark Templar":
                break;
        }
    }

    //TODO: move to comsat class
    private void scanInvisibleUnits(CombatUnits combatUnit) {
        if(combatUnit.getUnitType() != UnitType.Terran_Comsat_Station) {
            return;
        }

        if(isActiveScanNearUnit()) {
            return;
        }

        for(EnemyUnits enemyUnit : gameState.getKnownEnemyUnits()) {
            if(enemyUnit.getEnemyType() == UnitType.Protoss_Observer) {
                continue;
            }

            if((enemyUnit.getEnemyUnit().isCloaked() || enemyUnit.getEnemyUnit().isBurrowed()) && enemyUnit.getEnemyUnit().isVisible() && friendlyUnitInRange()) {
                combatUnit.getUnit().useTech(TechType.Scanner_Sweep, enemyUnit.getEnemyPosition());
            }
        }
    }

    private boolean isActiveScanNearUnit() {
        for(CombatUnits scanUnit : combatUnits) {
            if(scanUnit.getUnitStatus() == UnitStatus.SCAN) {
                for(EnemyUnits enemyUnit : gameState.getKnownEnemyUnits()) {
                    if((enemyUnit.getEnemyUnit().isCloaked() || enemyUnit.getEnemyUnit().isBurrowed()) && enemyUnit.getEnemyUnit().isVisible() && !enemyUnit.getEnemyUnit().isDetected()) {
                        int distance = scanUnit.getUnit().getPosition().getApproxDistance(enemyUnit.getEnemyPosition());
                        if (distance <= 300) {
                            return true;
                        }
                    }
                }
            }

            //Give a tolerance just outside of detection range to not waste scans
            if(scanUnit.getUnitType().isDetector()) {
                for(EnemyUnits enemyUnit : gameState.getKnownEnemyUnits()) {
                    if((enemyUnit.getEnemyUnit().isCloaked() || enemyUnit.getEnemyUnit().isBurrowed()) && enemyUnit.getEnemyUnit().isVisible()) {
                        if(enemyUnit.getEnemyUnit().getDistance(scanUnit.getUnit().getPosition()) <= scanUnit.getUnitType().sightRange()) {
                            if(scanUnit.getUnitType() == UnitType.Terran_Missile_Turret && enemyUnit.getEnemyUnit().getDistance(scanUnit.getUnit().getPosition()) > 224) {
                                continue;
                            }

                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean friendlyUnitInRange() {
        for(CombatUnits friendlyUnit : combatUnits) {
            if(friendlyUnit.getUnitStatus() == UnitStatus.WORKER || friendlyUnit.getUnitStatus() == UnitStatus.BUILDING
            || friendlyUnit.getUnitStatus() == UnitStatus.MINE || friendlyUnit.getUnitStatus() == UnitStatus.SCAN || friendlyUnit.getUnitStatus() == UnitStatus.ADDON) {
                continue;
            }

            if(friendlyUnit.getUnitType() == UnitType.Terran_Medic) {
                continue;
            }

            if(friendlyUnit.getUnit().getPosition() == null) {
                continue;
            }

            for(EnemyUnits enemyUnit : gameState.getKnownEnemyUnits()) {
                if(enemyUnit.getEnemyPosition() == null) {
                    continue;
                }

                if(friendlyUnit.getUnit().getPosition().getApproxDistance(enemyUnit.getEnemyPosition()) < friendlyUnit.getUnitType().groundWeapon().maxRange()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean obstructingBuild(CombatUnits combatUnit) {
        for(Unit unit : game.self().getUnits()) {
            if(!unit.getType().isWorker()) {
                continue;
            }

            if(combatUnit.getRallyPoint() == null) {
                continue;
            }

            if(!unit.isMoving() && !unit.isConstructing() && unit.getPosition().getApproxDistance(combatUnit.getUnit().getPosition()) < 100 && combatUnit.getUnit().getPosition().getApproxDistance(combatUnit.getRallyPoint().toPosition()) < 100) {
                combatUnit.setUnitStatus(UnitStatus.OBSTRUCTING);
                return true;
            }
        }
        return false;
    }

    private void moveFromObstruction(CombatUnits combatUnit) {
        Unit worker = null;
        for(Unit unit : game.self().getUnits()) {
            if(!unit.getType().isWorker()) {
                continue;
            }

            if(!unit.isMoving() && !unit.isConstructing() &&
                    unit.getPosition().getApproxDistance(combatUnit.getUnit().getPosition()) < 100) {
                worker = unit;
                break;
            }
        }

        if(worker == null) {
            return;
        }

        Position workerPos = worker.getPosition();
        Position unitPos = combatUnit.getUnit().getPosition();
        int dx = unitPos.getX() - workerPos.getX();
        int dy = unitPos.getY() - workerPos.getY();

        int moveX = unitPos.getX() + (dx * 100 / Math.max(1, unitPos.getApproxDistance(workerPos)));
        int moveY = unitPos.getY() + (dy * 100 / Math.max(1, unitPos.getApproxDistance(workerPos)));

        moveX = Math.min(Math.max(moveX, 0), game.mapWidth() * 32);
        moveY = Math.min(Math.max(moveY, 0), game.mapHeight() * 32);

        Position movePos = new Position(moveX, moveY);
        combatUnit.getUnit().attack(movePos);
    }

    private void avoidThreat(CombatUnits combatUnit) {
        EnemyUnits closestThreat = ClosestUnit.findClosestEnemyUnit(combatUnit, gameState.getKnownValidThreats(), Integer.MAX_VALUE);
        if(closestThreat == null || closestThreat.getEnemyPosition() == null) {
            return;
        }

        Position unitPos = combatUnit.getUnit().getPosition();
        Position threatPos = closestThreat.getEnemyPosition();

        int dx = unitPos.getX() - threatPos.getX();
        int dy = unitPos.getY() - threatPos.getY();

        int moveX = unitPos.getX() + (dx * 200 / Math.max(1, unitPos.getApproxDistance(threatPos)));
        int moveY = unitPos.getY() + (dy * 200 / Math.max(1, unitPos.getApproxDistance(threatPos)));

        moveX = Math.min(Math.max(moveX, 0), game.mapWidth() * 32);
        moveY = Math.min(Math.max(moveY, 0), game.mapHeight() * 32);

        Position movePos = new Position(moveX, moveY);
        combatUnit.getUnit().move(movePos);
    }

    private void inRangeOfThreat(CombatUnits combatUnit) {
        if(gameState.getKnownValidThreats().isEmpty()) {
            combatUnit.setInRangeOfThreat(false);
            return;
        }

        boolean inRange = false;

        for(EnemyUnits enemyUnit : gameState.getKnownValidThreats()) {
            if(enemyUnit.getEnemyPosition() == null) {
                continue;
            }

            if(combatUnit.getUnit().isFlying() && !enemyUnit.getEnemyType().airWeapon().targetsAir()) {
                continue;
            }

            if(combatUnit.getUnit().getPosition().getApproxDistance(enemyUnit.getEnemyPosition()) < enemyUnit.getEnemyType().groundWeapon().maxRange() + 125) {
                //Check if it's an active threat
                if(enemyUnit.getEnemyType() == UnitType.Zerg_Lurker && !enemyUnit.getEnemyUnit().isBurrowed()) {
                    continue;
                }
                else if(enemyUnit.getEnemyType().isBuilding() && (enemyUnit.getEnemyUnit().isMorphing() || !enemyUnit.getEnemyUnit().isPowered())) {
                    continue;
                }

                inRange = true;
            }
        }

        combatUnit.setInRangeOfThreat(inRange);
    }

    private UnitType typeOfThreat(CombatUnits combatUnit) {
        for(EnemyUnits enemyUnit : gameState.getKnownEnemyUnits()) {
            if(enemyUnit.getEnemyPosition() == null) {
                continue;
            }

            boolean isThreat = (enemyUnit.getEnemyType() == UnitType.Zerg_Lurker && enemyUnit.getEnemyUnit().isBurrowed() && !enemyUnit.getEnemyUnit().isDetected());

            if(!isThreat) {
                continue;
            }

            if(combatUnit.getUnit().getPosition().getApproxDistance(enemyUnit.getEnemyPosition()) < enemyUnit.getEnemyType().groundWeapon().maxRange() + 100) {
                return enemyUnit.getEnemyType();
            }
        }
        return null;
    }

    private boolean hasTankSupport(CombatUnits combatUnit) {
        if(unitCount.get(UnitType.Terran_Siege_Tank_Tank_Mode) > 0 || unitCount.get(UnitType.Terran_Siege_Tank_Siege_Mode) > 0) {
            ClosestUnit.findClosestFriendlyUnit(combatUnit, combatUnits, UnitType.Terran_Siege_Tank_Tank_Mode);
            if(combatUnit.getFriendlyUnit() != null && combatUnit.getUnit().getDistance(combatUnit.getFriendlyUnit().getUnit()) < 300) {
                return true;
            }

            for(CombatUnits unit : combatUnits) {
                if((unit.getUnitType() == UnitType.Terran_Siege_Tank_Tank_Mode || unit.getUnitType() == UnitType.Terran_Siege_Tank_Siege_Mode)
                        && unit.getEnemyUnit() != null && !unit.isInBase()) {
                    int distanceToEnemy = unit.getUnit().getDistance(unit.getEnemyUnit().getEnemyPosition());
                    if(distanceToEnemy < 400) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isExistingUnit(Unit unit) {
        for(CombatUnits combatUnit : combatUnits) {
            if(combatUnit.getUnitID() == unit.getID()) {
                return true;
            }
        }
        return false;
    }

    private boolean moveOutConditionsMet(HashMap<UnitType, Integer> openerMoveOutCondition) {
        for(UnitType unitType : openerMoveOutCondition.keySet()) {
            int requiredCount = openerMoveOutCondition.get(unitType);

            //Handle both tanks modes
            if(unitType == UnitType.Terran_Siege_Tank_Tank_Mode || unitType == UnitType.Terran_Siege_Tank_Siege_Mode) {
                int tankModeCount = unitCount.getOrDefault(UnitType.Terran_Siege_Tank_Tank_Mode, 0);
                int siegeModeCount = unitCount.getOrDefault(UnitType.Terran_Siege_Tank_Siege_Mode, 0);
                int totalTanks = tankModeCount + siegeModeCount;

                if(totalTanks < requiredCount) {
                    return false;
                }
            }
            else {
                if(!unitCount.containsKey(unitType) || unitCount.get(unitType) < requiredCount) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean priorityTargetExists() {
        return gameState.getKnownEnemyUnits().contains(priorityTarget);
    }

    public void onUnitComplete(Unit unit) {
        if(unit.getType() == UnitType.Terran_Bunker) {
            bunker = unit;
            return;
        }

        if(unit.getType() == UnitType.Terran_Comsat_Station) {
            CombatUnits combatUnit = combatUnitCreator.createCombatUnit(unit);
            combatUnits.add(combatUnit);
            return;
        }
        else if(unit.getType() == UnitType.Spell_Scanner_Sweep) {
            CombatUnits combatUnit = combatUnitCreator.createCombatUnit(unit);
            combatUnits.add(combatUnit);
            return;
        }

        if(isExistingUnit(unit)) {
            return;
        }

        if(unit.getType().isBuilding()) {
            if(!unit.canLift() && unit.getType() != UnitType.Terran_Missile_Turret) {
                return;
            }

            CombatUnits combatUnit = new CombatUnits(game, unit, UnitStatus.BUILDING);
            combatUnits.add(combatUnit);
            return;
        }

        CombatUnits combatUnit = combatUnitCreator.createCombatUnit(unit);
        combatUnits.add(combatUnit);
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
    }
}