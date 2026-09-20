package unitgroups.units;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import bwapi.Game;
import bwapi.Order;
import bwapi.Position;
import bwapi.TechType;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitCommand;
import bwapi.UnitCommandType;
import bwapi.UnitType;
import bwapi.UpgradeType;
import bwapi.WeaponType;
import information.MapInfo;
import information.enemy.EnemyInformation;
import information.enemy.EnemyUnits;
import map.bwemwrappers.Base;
import map.bwemwrappers.ChokePoint;
import map.bwemwrappers.Mineral;
import util.Time;

public class Vulture extends CombatUnits {
    private EnemyInformation enemyInformation;
    private MapInfo mapInfo;
    private HashSet<EnemyUnits> enemyUnits;
    private List<Position> minePositions = new ArrayList<>();
    private Position currentMinePos = null;
    private int mineCount = 3;
    private int pulseCheck = 0;
    private int mineTimer = 0;
    private boolean layingMines = false;
    private boolean miningExpansion = false;
    private boolean approachingStagingBase = false;
    private boolean recentlyMined = false;
    private boolean detouringMine = false;
    private boolean miningStagingComplete = false;
    private int dodgeSideSign = 0;
    private Base targetedEnemyExpansion = null;
    private HashSet<Base> visitedExpansions = new HashSet<>();
    private boolean runbyStagingComplete = false;
    private boolean lobotomyOverride = false;
    private int lastAttackStartFrame = -100;
    private Position lastMoveTarget = null;
    private int lastMoveFrame = 0;
    private int kiteFailFrames = 0;
    private Position wallStuckCheckPos = null;
    private int wallStuckTimer = 0;
    private int wallStuckRetreatTimer = 0;

    private final int FULL_MINE_CYCLE = new Time(0,30).getFrames();
    private final int ALLOWED_MINE_CYCLE = new Time(0,20).getFrames();
    private int mineCycle;

    public Vulture(Game game, EnemyInformation enemyInformation, Unit unit) {
        super(game, unit);
        this.enemyInformation = enemyInformation;
        this.enemyUnits = enemyInformation.getEnemyUnits();
        mapInfo = enemyInformation.getBaseInfo();
        unitStatus = UnitStatus.POKE;
        mineCycle = game.getFrameCount();
        calculateMinePositions();
    }

    @Override
    public void onFrame() {
        if (mineTimer >= 144) {
            mineTimer = 0;
            recentlyMined = false;
        }

        if (recentlyMined) {
            mineTimer += 8;
        }

        if (unitStatus != UnitStatus.POKE && unitStatus != UnitStatus.ATTACK && unitStatus != UnitStatus.RUNBY && unitStatus != UnitStatus.DEFEND) {
            wallStuckTimer = 0;
            wallStuckCheckPos = null;
            wallStuckRetreatTimer = 0;
            return;
        }

        if (wallStuckRetreatTimer > 0) {
            wallStuckRetreatTimer--;
            return;
        }

        wallStuckTimer++;
        if (wallStuckTimer < 48) {
            return;
        }

        wallStuckTimer = 0;

        if (wallStuckCheckPos != null
                && unit.getPosition().getApproxDistance(wallStuckCheckPos) < 16) {
            wallStuckRetreatTimer = 12;
        }

        wallStuckCheckPos = unit.getPosition();
    }

    @Override
    public void rally() {
        if (rallyPoint == null) {
            return;
        }

        unit.move(rallyPoint.toPosition());
        setUnitStatus(UnitStatus.ATTACK);
    }

    @Override
    public void microOnFrame() {
        if (unit.isStartingAttack()) {
            lastAttackStartFrame = game.getFrameCount();
        }

        if (unitStatus != UnitStatus.POKE && unitStatus != UnitStatus.ATTACK) {
            return;
        }

        if (enemyUnit == null || enemyUnit.getEnemyPosition() == null) {
            return;
        }

        retargetFromBuilding();

        if (hasTankSupport || layingMines || wallStuckRetreatTimer > 0) {
            return;
        }

        if (unit.getLastCommandFrame() >= game.getFrameCount()) {
            return;
        }

        if (game.getFrameCount() + game.getRemainingLatencyFrames() < lastAttackStartFrame + 4) {
            return;
        }

        if (unit.isAttackFrame()) {
            return;
        }

        Position kitePos = getKitePosition();
        if (kitePos != null) {
            issueMove(kitePos);
            return;
        }

        movingShot();
    }

    private void movingShot() {
        Unit target = enemyUnit.getEnemyUnit();
        Position enemyPos = enemyUnit.getEnemyPosition();

        if (target == null || !target.isVisible()) {
            unit.attack(enemyPos);
            return;
        }

        double distance = unit.getDistance(target);
        int myRange = weaponRange();

        int cooldown = unit.getGroundWeaponCooldown();
        if (unit.isStartingAttack()) {
            cooldown = unit.getType().groundWeapon().damageCooldown();
        }

        if (cooldown == 0 && distance <= myRange) {
            issueAttack(target);
            return;
        }

        double mySpeed = game.self().topSpeed(unit.getType());
        double haltDistance = (mySpeed * mySpeed) / (2.0 * acceleration());
        int fleeDistance = (int) Math.max(haltDistance * 2.0, mySpeed * 30.0);

        if (enemyUnit.getEnemyType().isBuilding()) {
            int threatRange = getGroundThreatRange(enemyUnit);
            if (threatRange <= myRange || distance > threatRange + fleeMargin()) {
                issueAttack(target);
                return;
            }

            Position buildingFleePos = kiteAwayFrom(enemyPos, fleeDistance);
            if (buildingFleePos != null) {
                kiteFailFrames = 0;
                issueMove(buildingFleePos);
                return;
            }

            kiteFailFrames++;

            if (kiteFailFrames > 24 && rallyPoint != null) {
                issueMove(rallyPoint.toPosition());
                return;
            }

            issueAttack(target);
            return;
        }

        if (enemyUnit.getEnemyType().isWorker() && distance <= 64) {
            Position workerFleePos = kiteAwayFrom(enemyPos, fleeDistance);
            if (workerFleePos != null) {
                issueMove(workerFleePos);
                return;
            }
        }

        if (enemyUnit.getEnemyType().isWorker() || targetFacingAway(enemyUnit)) {
            if (!target.isMoving() || distance <= myRange) {
                issueAttack(target);
                return;
            }

            int lead = (int) Math.min(Math.max((distance - myRange) / Math.max(1.0, mySpeed), 0.0), 12.0);
            issueMove(new Position(
                    (int) (enemyPos.getX() + target.getVelocityX() * lead),
                    (int) (enemyPos.getY() + target.getVelocityY() * lead)));
            return;
        }

        if (framesToEnterWeaponRange(distance) + 2 * game.getRemainingLatencyFrames() >= cooldown) {
            issueAttack(target);
            return;
        }

        if (mySpeed <= enemyTopSpeed(enemyUnit.getEnemyType())
                && enemyGroundRange(enemyUnit) >= myRange) {
            issueAttack(target);
            return;
        }

        Position fleePos = kiteAwayFrom(enemyPos, fleeDistance);

        if (fleePos == null) {
            kiteFailFrames++;

            if (kiteFailFrames > 24 && rallyPoint != null) {
                issueMove(rallyPoint.toPosition());
                return;
            }

            issueAttack(target);
            return;
        }

        kiteFailFrames = 0;
        issueMove(fleePos);
    }

    private void issueAttack(Unit target) {
        if (unit.getOrderTarget() == target) {
            return;
        }

        unit.attack(target);
    }

    private boolean retargetFromBuilding() {
        Unit orderTarget = unit.getOrderTarget();
        boolean orderOnBuilding = orderTarget != null && orderTarget.getType().isBuilding();

        if (!enemyUnit.getEnemyType().isBuilding() && !orderOnBuilding) {
            return false;
        }

        EnemyUnits closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (EnemyUnits enemy : enemyUnits) {
            if (enemy.getEnemyType().isBuilding()) {
                continue;
            }
            if (enemy.getEnemyType().isFlyer() || !enemy.getEnemyType().canAttack()) {
                continue;
            }
            if (enemy.getEnemyType() == UnitType.Terran_Vulture_Spider_Mine
                    || enemy.getEnemyType() == UnitType.Protoss_Scarab) {
                continue;
            }
            if (enemy.getEnemyPosition() == null) {
                continue;
            }
            if (enemy.getEnemyUnit() == null || !enemy.getEnemyUnit().isVisible()) {
                continue;
            }
            if (!unit.canAttackUnit(enemy.getEnemyUnit())) {
                continue;
            }

            double distance = unit.getDistance(enemy.getEnemyUnit());
            if (distance > 500) {
                continue;
            }
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = enemy;
            }
        }

        if (closest == null) {
            return false;
        }

        setEnemyUnit(closest);
        issueAttack(closest.getEnemyUnit());
        return true;
    }

    private boolean targetFacingAway(EnemyUnits enemy) {
        Unit target = enemy.getEnemyUnit();
        if (target == null || !target.isVisible()) {
            return false;
        }

        Position enemyPos = enemy.getEnemyPosition();
        double dx = unit.getPosition().getX() - enemyPos.getX();
        double dy = unit.getPosition().getY() - enemyPos.getY();
        double length = Math.max(1.0, Math.sqrt(dx * dx + dy * dy));

        return (Math.cos(target.getAngle()) * dx + Math.sin(target.getAngle()) * dy) / length < 0;
    }

    private int framesToEnterWeaponRange(double distance) {
        double gap = Math.max(0.0, distance - weaponRange());
        return (int) Math.round(gap / Math.max(1.0, game.self().topSpeed(unit.getType())));
    }

    private void issueMove(Position destination) {
        double mySpeed = game.self().topSpeed(unit.getType());
        double brakingDistance = (mySpeed * mySpeed) / acceleration();

        if (lastMoveTarget != null
                && unit.getOrder() == Order.Move
                && game.getFrameCount() - lastMoveFrame < 6
                && lastMoveTarget.getApproxDistance(destination) < 32
                && unit.getPosition().getDistance(lastMoveTarget) > brakingDistance
                && unit.isMoving()
                && !unit.isStuck()) {
            return;
        }

        unit.move(destination);
        lastMoveTarget = destination;
        lastMoveFrame = game.getFrameCount();
    }

    @Override
    public void attack() {
        if (enemyUnit == null || enemyUnit.getEnemyPosition() == null) {
            if (rallyPoint != null) {
                unit.move(rallyPoint.toPosition());
            }
            return;
        }

        retargetFromBuilding();

        if (mineCount > unit.getSpiderMineCount()) {
            mineCount = unit.getSpiderMineCount();
            recentlyMined = true;
            layingMines = false;
        }

        if (pulseCheck > 32) {
            pulseCheck = 0;
            layingMines = false;
        }

        if (layingMines) {
            if (unit.isStuck()) {
                layingMines = false;
                recentlyMined = true;
                pulseCheck = 0;
                return;
            }

            pulseCheck += 8;
            return;
        }

        if ((isOutRanged() || enemyInformation.outRangingUnitNearby(enemyUnit, unit.getType(), unit.getType().groundWeapon().maxRange() + 32))
                && !hasTankSupport
                && !lobotomyOverride
                && unit.getDistance(enemyUnit.getEnemyPosition()) < enemyGroundRange(enemyUnit) + 160) {
            unitStatus = UnitStatus.RETREAT;
            return;
        }

        if (enemyInformation.outRangingUnitNearby(enemyUnit, unit.getType(), unit.getType().groundWeapon().maxRange() + 32) 
                && !hasTankSupport  
                && !lobotomyOverride) {
            unitStatus = UnitStatus.RETREAT;
            return;
        }

        if (hasTankSupport) {
            unit.attack(enemyUnit.getEnemyUnit());
        }
        else if (wallStuckRetreatTimer > 0 && rallyPoint != null) {
            unit.move(rallyPoint.toPosition());
        }

        if (!game.self().hasResearched(TechType.Spider_Mines) || unit.getSpiderMineCount() == 0) {
            return;
        }

        layMinesOnEnemy();

        if (allowMineLaying()) {
            layMinesAtChokepoints();
            layMinesAnyWhere();
        }
    }

    @Override
    public void retreat() {
        if (enemyUnit == null || rallyPoint == null) {
            return;
        }

        if (unit.getDistance(rallyPoint.toPosition()) < 100) {
            setUnitStatus(UnitStatus.DEFEND);
        }

        boolean outRangingNearby = enemyInformation.outRangingUnitNearby(enemyUnit, unit.getType(), unit.getType().groundWeapon().maxRange() + 32);

        if (((isOutRanged() || outRangingNearby) && unit.getDistance(enemyUnit.getEnemyPosition()) > enemyGroundRange(enemyUnit) + 256)
                || (!inRangeOfThreat && (!isOutRanged() || hasTankSupport) && !outRangingNearby)) {
            setUnitStatus(UnitStatus.POKE);
            return;
        }

        unit.move(rallyPoint.toPosition());

        if (inBase && isOutRanged()) {
            setUnitStatus(UnitStatus.DEFEND);
        }

        if (game.self().hasResearched(TechType.Spider_Mines) && unit.getSpiderMineCount() > 0 && !layingMines && !recentlyMined) {
            if (!mapInfo.getBaseTiles().contains(unit.getPosition().toTilePosition())
                    && !mapInfo.getNaturalTiles().contains(unit.getPosition().toTilePosition())
                    && unit.getDistance(enemyUnit.getEnemyPosition()) > 300
                    && !isOnNaturalBunkerWall(unit.getPosition())) {
                unit.useTech(TechType.Spider_Mines, unit.getPosition());
                layingMines = true;
            }
        }
    }

    @Override
    public void defend() {
        if (enemyUnit == null) {
            setUnitStatus(UnitStatus.POKE);
            return;
        }

        if (retargetFromBuilding()) {
            return;
        }

        if (unit.isStartingAttack() || unit.isAttackFrame()) {
            return;
        }

        EnemyUnits closestMelee = null;
        double closestMeleeDist = Double.MAX_VALUE;
        for (EnemyUnits e : enemyUnits) {
            if (e.getEnemyPosition() == null) {
                continue;
            }
            if (enemyGroundRange(e) >= 32) {
                continue;
            }
            double d = unit.getDistance(e.getEnemyPosition());
            if (d < closestMeleeDist) {
                closestMeleeDist = d;
                closestMelee = e;
            }
        }

        if (closestMelee != null && closestMeleeDist <= 96 && unit.getGroundWeaponCooldown() > 0) {
            Position fleePos = kiteAwayFrom(closestMelee.getEnemyPosition(), weaponRange());
            if (fleePos != null) {
                unit.move(fleePos);
                return;
            }
        }

        unit.attack(enemyUnit.getEnemyUnit());
    }

    @Override
    public void poke() {
        if (enemyUnit == null || enemyUnit.getEnemyPosition() == null) {
            if (mapInfo.getEnemyMain() != null) {
                unit.attack(mapInfo.getEnemyMain().getCenter());
            }

            return;
        }

        if (retargetFromBuilding()) {
            return;
        }

        if (hasTankSupport && enemyUnit.getEnemyUnit() != null && enemyUnit.getEnemyUnit().isVisible()) {
            issueAttack(enemyUnit.getEnemyUnit());
        }

        if (mineCount > unit.getSpiderMineCount()) {
            mineCount = unit.getSpiderMineCount();
            recentlyMined = true;
            layingMines = false;
            miningExpansion = false;
            approachingStagingBase = false;
            targetedEnemyExpansion = null;
            detouringMine = false;
            miningStagingComplete = false;
        }

        if (pulseCheck > 32) {
            pulseCheck = 0;
            layingMines = false;
            miningExpansion = false;
            approachingStagingBase = false;
            targetedEnemyExpansion = null;
            detouringMine = false;
            miningStagingComplete = false;
        }

        if (layingMines) {
            if (unit.isStuck()) {
                layingMines = false;
                miningExpansion = false;
                approachingStagingBase = false;
                targetedEnemyExpansion = null;
                recentlyMined = true;
                pulseCheck = 0;
                detouringMine = false;
                miningStagingComplete = false;
                return;
            }

            if (!miningExpansion) {
                pulseCheck += 8;
            }
            else if (targetedEnemyExpansion != null && unit.getDistance(targetedEnemyExpansion.getCenter()) < 96) {
                pulseCheck += 8;
            }
            else if (unit.isIdle()) {
                pulseCheck += 16;
            }

            if (miningExpansion && targetedEnemyExpansion != null) {
                detouringMine = false;
                Position destPos = targetedEnemyExpansion.getCenter();
                double distToDest = unit.getPosition().getDistance(destPos);

                if (dodgeToward(destPos)) {
                    detouringMine = true;
                    miningStagingComplete = true;
                    return;
                }

                if (distToDest <= 96) {
                    UnitCommand lastCmd = unit.getLastCommand();
                    if (lastCmd == null || lastCmd.getType() != UnitCommandType.Use_Tech_Position) {
                        unit.useTech(TechType.Spider_Mines, destPos);
                    }
                }
            }

            return;
        }

        if (wallStuckRetreatTimer > 0 && rallyPoint != null) {
            unit.move(rallyPoint.toPosition());
            return;
        }

        if (isOutRanged() && !hasTankSupport && !lobotomyOverride
                && unit.getDistance(enemyUnit.getEnemyPosition()) < enemyGroundRange(enemyUnit) + 128) {
            unitStatus = UnitStatus.RETREAT;
            return;
        }

        if (enemyInformation.outRangingUnitNearby(enemyUnit, unit.getType(), weaponRange() + 32)
                && !targetFacingAway(enemyUnit)
                && !hasTankSupport
                && !lobotomyOverride
                && unit.getDistance(enemyUnit.getEnemyPosition()) < weaponRange() + 160) {
            unitStatus = UnitStatus.RETREAT;
            return;
        }

        if (!game.self().hasResearched(TechType.Spider_Mines) || unit.getSpiderMineCount() == 0) {
            return;
        }

        layMinesOnEnemy();

        if (allowMineLaying()) {
            layMinesAtChokepoints();
            layMinesAnyWhere();
        }
    }

    @Override
    public void runby() {
        if (wallStuckRetreatTimer > 0 && rallyPoint != null) {
            unit.move(rallyPoint.toPosition());
            return;
        }

        ArrayList<Base> enemyExpansions = mapInfo.scoredBestEnemyExpansion(enemyUnits);
        Base enemyNatural = mapInfo.getEnemyNatural();
        Base enemyMain = mapInfo.getEnemyMain();

        List<Base> enemyOwnedBeyondNatural = new ArrayList<>();
        for (Base base : mapInfo.getMapBases()) {
            if (base == enemyNatural || base == enemyMain) {
                continue;
            }
            if (!enemyDepotNearBase(base)) {
                continue;
            }
            enemyOwnedBeyondNatural.add(base);
        }

        Position vulturePos = unit.getPosition();

        List<Base> targets = new ArrayList<>();
        if (!enemyOwnedBeyondNatural.isEmpty()) {
            enemyOwnedBeyondNatural.sort((a, b) -> Double.compare(
                    vulturePos.getDistance(a.getCenter()),
                    vulturePos.getDistance(b.getCenter())));
            targets.addAll(enemyOwnedBeyondNatural);
        }

        List<Base> scoredSorted = new ArrayList<>(enemyExpansions);
        scoredSorted.sort((a, b) -> Double.compare(
                vulturePos.getDistance(a.getCenter()),
                vulturePos.getDistance(b.getCenter())));
        for (Base scored : scoredSorted) {
            if (!targets.contains(scored)) {
                targets.add(scored);
            }
        }

        for (Base expansion : targets) {
            if (visitedExpansions.contains(expansion)) {
                continue;
            }

            targetedEnemyExpansion = expansion;

            ArrayList<Base> ordered = mapInfo.getOrderedExpansions();
            if (!ordered.isEmpty() && unit.getDistance(ordered.get(0).getCenter()) < 160) {
                approachingStagingBase = false;
                runbyStagingComplete = true;
            }
            else if (!runbyStagingComplete && !ordered.isEmpty()
                    && unit.getDistance(ordered.get(0).getCenter()) < unit.getDistance(expansion.getCenter())) {
                Position stagingPos = ordered.get(0).getCenter();
                approachingStagingBase = true;
                if (unit.getDistance(stagingPos) > 500 && dodgeToward(stagingPos)) {
                    return;
                }
                unit.move(stagingPos);
                return;
            }

            EnemyUnits depot = findDepotNearBase(expansion);
            if (depot != null) {
                runbyStagingComplete = true;
                Position attackPos = runbyAttackPos(expansion, depot);
                if (unit.getDistance(attackPos) >= 150) {
                    if (unit.getDistance(attackPos) > 500 && dodgeToward(attackPos)) {
                        return;
                    }
                    unit.move(attackPos);
                    return;
                }
                attackBaseTarget(depot, expansion);
                return;
            }

            if (unit.getDistance(expansion.getCenter()) >= 200) {
                runbyStagingComplete = true;
                Position expansionPos = expansion.getCenter();
                if (unit.getDistance(expansionPos) > 500 && dodgeToward(expansionPos)) {
                    return;
                }
                unit.move(expansionPos);
                return;
            }

            runbyStagingComplete = true;
            visitedExpansions.add(expansion);
        }

        if (enemyNatural != null) {
            targetedEnemyExpansion = enemyNatural;
            runbyStagingComplete = true;

            EnemyUnits naturalDepot = findDepotNearBase(enemyNatural);
            if (naturalDepot != null) {
                Position attackPos = runbyAttackPos(enemyNatural, naturalDepot);
                if (unit.getDistance(attackPos) >= 150) {
                    unit.move(attackPos);
                    return;
                }
                attackBaseTarget(naturalDepot, enemyNatural);
                return;
            }

            if (unit.getDistance(enemyNatural.getCenter()) >= 200) {
                unit.move(enemyNatural.getCenter());
                return;
            }
        }

        if (enemyMain != null) {
            targetedEnemyExpansion = enemyMain;
            runbyStagingComplete = true;

            EnemyUnits mainDepot = findDepotNearBase(enemyMain);
            if (mainDepot != null) {
                Position attackPos = runbyAttackPos(enemyMain, mainDepot);
                if (unit.getDistance(attackPos) >= 150) {
                    unit.move(attackPos);
                    return;
                }
                attackBaseTarget(mainDepot, enemyMain);
                return;
            }

            unit.attack(enemyMain.getCenter());
            return;
        }

        if (rallyPoint != null) {
            unit.move(rallyPoint.toPosition());
        }
    }

    private Position kiteTo(int distance) {
        Position enemyPos = enemyUnit.getEnemyPosition();
        Position unitPos = unit.getPosition();
        int dx = unitPos.getX() - enemyPos.getX();
        int dy = unitPos.getY() - enemyPos.getY();

        double moveX = unitPos.getX() + (dx * distance / Math.max(1, unitPos.getDistance(enemyPos)));
        double moveY = unitPos.getY() + (dy * distance / Math.max(1, unitPos.getDistance(enemyPos)));

        moveX = Math.min(Math.max(moveX, 0), game.mapWidth() * 32);
        moveY = Math.min(Math.max(moveY, 0), game.mapHeight() * 32);
        return new Position((int) moveX, (int) moveY);
    }

    private boolean dodgeToward(Position destPos) {
        Position vulturePos = unit.getPosition();
        double distToDest = vulturePos.getDistance(destPos);
        double hazardRadius = 256.0;

        double avoidDx = 0.0;
        double avoidDy = 0.0;
        boolean threatNearby = false;

        for (EnemyUnits enemy : enemyUnits) {
            int range = getGroundThreatRange(enemy);
            if (range == 0) {
                continue;
            }
            if (enemy.getEnemyPosition() == null) {
                continue;
            }

            UnitType type = enemy.getEnemyType();

            if (type.isWorker()) {
                continue;
            }

            if (type.isBuilding() && enemy.getEnemyUnit() != null && enemy.getEnemyUnit().isVisible()) {
                if (type == UnitType.Terran_Bunker) {
                    if (!enemy.getEnemyUnit().isCompleted()) {
                        continue;
                    }
                }
                else if (!enemy.getEnemyUnit().isCompleted() || enemy.getEnemyUnit().isMorphing()
                        || !enemy.getEnemyUnit().isPowered()) {
                    continue;
                }
            }

            double threatDist = vulturePos.getDistance(enemy.getEnemyPosition());
            double safeDistance = range + hazardRadius;

            if (threatDist < safeDistance) {
                threatNearby = true;
                double dx = vulturePos.getX() - enemy.getEnemyPosition().getX();
                double dy = vulturePos.getY() - enemy.getEnemyPosition().getY();
                double weight = (safeDistance - threatDist) / safeDistance;
                avoidDx += (dx / Math.max(1.0, threatDist)) * weight;
                avoidDy += (dy / Math.max(1.0, threatDist)) * weight;
            }
        }

        if (!threatNearby || distToDest <= 96) {
            dodgeSideSign = 0;
            return false;
        }

        double avoidLen = Math.sqrt(avoidDx * avoidDx + avoidDy * avoidDy);
        if (avoidLen < 0.001) {
            return false;
        }

        double avoidNx = avoidDx / avoidLen;
        double avoidNy = avoidDy / avoidLen;

        double destDx = destPos.getX() - vulturePos.getX();
        double destDy = destPos.getY() - vulturePos.getY();
        double destLen = Math.sqrt(destDx * destDx + destDy * destDy);

        if (destLen < 0.001) {
            return false;
        }

        double destNx = destDx / destLen;
        double destNy = destDy / destLen;

        double perpX = -destNy;
        double perpY = destNx;
        if (dodgeSideSign == 0) {
            double sideDot = perpX * avoidNx + perpY * avoidNy;
            dodgeSideSign = 1;
            if (sideDot < 0) {
                dodgeSideSign = -1;
            }
        }
        if (dodgeSideSign < 0) {
            perpX = -perpX;
            perpY = -perpY;
        }

        double stepLen = 160.0;
        double forwardWeight = 0.6;
        double sideWeight = 1.0;
        double rawX = vulturePos.getX() + (destNx * forwardWeight + perpX * sideWeight) * stepLen;
        double rawY = vulturePos.getY() + (destNy * forwardWeight + perpY * sideWeight) * stepLen;

        int maxX = game.mapWidth() * 32 - 1;
        int maxY = game.mapHeight() * 32 - 1;
        int clampedX = (int) Math.min(Math.max(rawX, 0), maxX);
        int clampedY = (int) Math.min(Math.max(rawY, 0), maxY);

        Position detourWaypoint = new Position(clampedX, clampedY);

        if (!game.isWalkable(detourWaypoint.toWalkPosition())) {
            return false;
        }

        int sampleSteps = 8;
        for (int s = 1; s <= sampleSteps; s++) {
            int sx = vulturePos.getX() + (detourWaypoint.getX() - vulturePos.getX()) * s / sampleSteps;
            int sy = vulturePos.getY() + (detourWaypoint.getY() - vulturePos.getY()) * s / sampleSteps;
            if (!game.isWalkable(new Position(sx, sy).toWalkPosition())) {
                return false;
            }
        }

        unit.move(detourWaypoint);
        return true;
    }

    private Position kiteAwayFrom(Position enemyPos, int distance) {
        Position unitPos = unit.getPosition();
        int dx = unitPos.getX() - enemyPos.getX();
        int dy = unitPos.getY() - enemyPos.getY();
        double dist = Math.max(1, unitPos.getDistance(enemyPos));

        double dirX = dx / dist;
        double dirY = dy / dist;

        int maxX = game.mapWidth() * 32 - 1;
        int maxY = game.mapHeight() * 32 - 1;

        double baseAngle = Math.atan2(dirY, dirX);
        double[] offsets = {0, 0.5236, -0.5236, 1.0472, -1.0472, 1.5708, -1.5708,
                2.0944, -2.0944, 2.618, -2.618, 3.1416};

        for (double offset : offsets) {
            double angle = baseAngle + offset;
            double rx = Math.cos(angle);
            double ry = Math.sin(angle);

            for (int d = distance; d >= 32; d -= 32) {
                int tx = (int) Math.min(Math.max(unitPos.getX() + rx * d, 0), maxX);
                int ty = (int) Math.min(Math.max(unitPos.getY() + ry * d, 0), maxY);
                Position candidate = new Position(tx, ty);

                if (!walkableRay(unitPos, candidate)) {
                    continue;
                }

                return candidate;
            }
        }

        return null;
    }

    private boolean walkableRay(Position from, Position to) {
        int sampleSteps = 8;
        for (int s = 1; s <= sampleSteps; s++) {
            int sx = from.getX() + (to.getX() - from.getX()) * s / sampleSteps;
            int sy = from.getY() + (to.getY() - from.getY()) * s / sampleSteps;
            if (!game.isWalkable(new Position(sx, sy).toWalkPosition())) {
                return false;
            }
        }
        return true;
    }

    private Position getKitePosition() {
        Position unitPos = unit.getPosition();
        double sumDx = 0;
        double sumDy = 0;
        double staticSumDx = 0;
        double staticSumDy = 0;
        int staticThreatCount = 0;
        boolean anyThreat = false;
        boolean ignoreStaticDefense = unitStatus == UnitStatus.ATTACK && enemyInformation.staticDefenseCount() <= 2;

        for (EnemyUnits enemy : enemyUnits) {
            int range = getGroundThreatRange(enemy);

            if (range == 0) {
                continue;
            }

            if (enemy.getEnemyPosition() == null) {
                continue;
            }

            UnitType type = enemy.getEnemyType();

            if (type.isWorker()) {
                continue;
            }

            boolean staticThreat = type == UnitType.Protoss_Photon_Cannon
                    || type == UnitType.Zerg_Sunken_Colony
                    || type == UnitType.Terran_Bunker;


            if (type.isBuilding() && enemy.getEnemyUnit() != null && enemy.getEnemyUnit().isVisible()) {
                if (type == UnitType.Terran_Bunker) {
                    if (!enemy.getEnemyUnit().isCompleted()) {
                        continue;
                    }
                }
                else if (!enemy.getEnemyUnit().isCompleted() || enemy.getEnemyUnit().isMorphing()
                        || !enemy.getEnemyUnit().isPowered()) {
                    continue;
                }
            }

            double threatDist = unitPos.getDistance(enemy.getEnemyPosition());

            int safeDistance;
            if (staticThreat) {
                safeDistance = range + 160;
            }
            else if (range > weaponRange()) {
                safeDistance = range + 192;
            }
            else if (range <= 32) {
                safeDistance = range + 64;
            }
            else {
                safeDistance = 64;
            }

            if (threatDist >= safeDistance) {
                continue;
            }

            double dx = unitPos.getX() - enemy.getEnemyPosition().getX();
            double dy = unitPos.getY() - enemy.getEnemyPosition().getY();
            double weight = (safeDistance - threatDist) / safeDistance;
            double contribX = (dx / Math.max(1, threatDist)) * weight;
            double contribY = (dy / Math.max(1, threatDist)) * weight;

            if (staticThreat) {
                if (ignoreStaticDefense
                        && (type == UnitType.Zerg_Sunken_Colony || type == UnitType.Protoss_Photon_Cannon)) {
                    continue;
                }

                staticThreatCount++;
                staticSumDx += contribX;
                staticSumDy += contribY;
                continue;
            }

            anyThreat = true;
            sumDx += contribX;
            sumDy += contribY;
        }

        if (staticThreatCount > 1 || (staticThreatCount > 0 && unitStatus == UnitStatus.POKE)) {
            anyThreat = true;
            sumDx += staticSumDx;
            sumDy += staticSumDy;
        }

        if (!anyThreat) {
            return null;
        }

        double len = Math.sqrt(sumDx * sumDx + sumDy * sumDy);
        if (len < 0.001) {
            return null;
        }

        int maxX = game.mapWidth() * 32 - 1;
        int maxY = game.mapHeight() * 32 - 1;

        for (int projection = 320; projection >= 64; projection -= 64) {
            double rawX = unitPos.getX() + (sumDx / len) * projection;
            double rawY = unitPos.getY() + (sumDy / len) * projection;

            if (rawX < 0 || rawX > maxX || rawY < 0 || rawY > maxY) {
                continue;
            }

            Position kitePos = new Position((int) rawX, (int) rawY);
            if (!walkableRay(unitPos, kitePos)) {
                continue;
            }

            return kitePos;
        }

        return null;
    }

    private int getGroundThreatRange(EnemyUnits enemy) {
        UnitType type = enemy.getEnemyType();
        if (type == UnitType.Terran_Bunker) {
            if (enemyInformation.getEnemyUpgrades().hasRangeUpgrade(UnitType.Terran_Marine)) {
                return 160;
            }
            return UnitType.Terran_Marine.groundWeapon().maxRange();
        }
        if (type.groundWeapon() != WeaponType.None) {
            return enemyGroundRange(enemy);
        }
        return 0;
    }

    private int enemyGroundRange(EnemyUnits e) {
        UnitType type = e.getEnemyType();
        if (!enemyInformation.getEnemyUpgrades().hasRangeUpgrade(type)) {
            return type.groundWeapon().maxRange();
        }
        if (type == UnitType.Protoss_Dragoon) {
            return 192;
        }
        if (type == UnitType.Terran_Marine) {
            return 160;
        }
        if (type == UnitType.Zerg_Hydralisk) {
            return 160;
        }
        return type.groundWeapon().maxRange();
    }

    private double enemyTopSpeed(UnitType type) {
        double speed = type.topSpeed();
        if (!enemyInformation.getEnemyUpgrades().hasSpeedUpgrade(type)) {
            return speed;
        }
        return speed * 1.5;
    }

    private int fleeMargin() {
        double mySpeed = game.self().topSpeed(unit.getType());
        double framesToFlee = (mySpeed / acceleration()) + 4 + game.getRemainingLatencyFrames() + 1;
        return (int) (0.5 * mySpeed * framesToFlee);
    }

    private double acceleration() {
        double acceleration = unit.getType().acceleration() / 256.0;
        if (game.self().getUpgradeLevel(UpgradeType.Ion_Thrusters) > 0) {
            acceleration *= 2.0;
        }
        return acceleration;
    }

    private int weaponRange() {
        WeaponType weaponType = unit.getType().groundWeapon();
            return weaponType.maxRange();
    }

    private void calculateMinePositions() {
        if (enemyInformation.getStartingEnemyBase() == null) {
            return;
        }

        if (enemyInformation.getStartingEnemyBase().getEnemyPosition() == null) {
            return;
        }

        ChokePoint mainChoke = mapInfo.getMainChoke();
        ChokePoint naturalChoke = mapInfo.getNaturalChoke();

        if (mainChoke == null || naturalChoke == null) {
            return;
        }

        for (Base base : mapInfo.getAllBasePaths().getChokePathLists().keySet()) {
            if (enemyInformation.getStartingEnemyBase().getEnemyPosition().equals(base.getCenter())) {
                List<Position> allPositions = mapInfo.getAllBasePaths().getChokePathLists().get(base);

                for (Position position : allPositions) {
                    boolean nearMainChoke = position.getDistance(mainChoke.getCenter()) < 175;
                    boolean nearNaturalChoke = position.getDistance(naturalChoke.getCenter()) < 175;

                    if (!nearMainChoke && !nearNaturalChoke) {
                        minePositions.add(position);
                    }
                }
            }
        }
    }

    private boolean isOutRanged() {
        if (enemyUnit != null) {
            if (enemyUnit.getEnemyType().isBuilding() || enemyUnit.getEnemyType() == UnitType.Terran_Marine) {
                return false;
            }
            if (enemyGroundRange(enemyUnit) + 32 >= weaponRange()) {
                return true;
            }
        }
        return false;
    }

    private void layMinesAtChokepoints() {
        if (minePositions.isEmpty()) {
            calculateMinePositions();
            return;
        }

        for (Position pos : minePositions) {
            if (unit.getDistance(pos) < 250) {
                for (int validMinePositionAttempt = 0; validMinePositionAttempt < 10; validMinePositionAttempt++) {
                    Random random = new Random();
                    int randX = pos.getX() - 200 + random.nextInt(400);
                    int randY = pos.getY() - 200 + random.nextInt(400);
                    Position testPos = new Position(randX, randY);

                    if (mapInfo.getPathFinding().getTilePositionValidator().isWalkable(testPos.toTilePosition())) {
                        currentMinePos = testPos;
                        break;
                    }

                    validMinePositionAttempt++;
                }

                if (currentMinePos != null && !isOnNaturalBunkerWall(currentMinePos)) {
                    unit.useTech(TechType.Spider_Mines, currentMinePos);
                    layingMines = true;
                    currentMinePos = null;
                }
            }
        }
    }

    private void layMinesAnyWhere() {
        if (mapInfo.getBaseTiles().contains(unit.getPosition().toTilePosition())
                || mapInfo.getNaturalTiles().contains(unit.getPosition().toTilePosition())
                || mapInfo.getMinBaseTiles().contains(unit.getPosition().toTilePosition())) {
            return;
        }

        if (enemyUnit != null && unit.getDistance(enemyUnit.getEnemyPosition()) < 200 && enemyUnit.getEnemyType().isBuilding()) {
            return;
        }

        if (layingMines) {
            return;
        }

        if (isOnNaturalBunkerWall(unit.getPosition())) {
            return;
        }

        unit.useTech(TechType.Spider_Mines, unit.getPosition());
        layingMines = true;
    }

    private void layMinesOnEnemy() {
        if (enemyUnit.getEnemyType().isBuilding()) {
            return;
        }

        if (recentlyMined) {
            return;
        }

        if (unit.getDistance(enemyUnit.getEnemyPosition()) < 200) {
            if (mapInfo.getBaseTiles().contains(unit.getPosition().toTilePosition())
                    || mapInfo.getNaturalTiles().contains(unit.getPosition().toTilePosition())) {
                return;
            }
            if (hasTankSupport && enemyUnit.getEnemyType() != UnitType.Terran_Siege_Tank_Siege_Mode) {
                Position minePos = unit.getPosition();
                if (!isOnNaturalBunkerWall(minePos)) {
                    unit.useTech(TechType.Spider_Mines, minePos);
                    layingMines = true;
                }
            }
            else if (enemyGroundRange(enemyUnit) <= 64) {
                Position minePos = kiteTo(256);
                if (!isOnNaturalBunkerWall(minePos)) {
                    unit.useTech(TechType.Spider_Mines, minePos);
                    layingMines = true;
                }
            }
            else if (enemyUnit.getEnemyType() == UnitType.Terran_Siege_Tank_Siege_Mode) {
                Position enemyPos = enemyUnit.getEnemyPosition();
                Position unitPos = unit.getPosition();

                int dx = enemyPos.getX() - unitPos.getX();
                int dy = enemyPos.getY() - unitPos.getY();
                double distance = unitPos.getDistance(enemyPos);

                double moveX = unitPos.getX() + (dx * 10 / Math.max(1, distance));
                double moveY = unitPos.getY() + (dy * 10 / Math.max(1, distance));

                Position minePos = new Position((int) moveX, (int) moveY);

                unit.move(minePos);
                if (!isOnNaturalBunkerWall(minePos)) {
                    unit.useTech(TechType.Spider_Mines, minePos);
                    layingMines = true;
                }
            }
            else {
                if (!isOnNaturalBunkerWall(unit.getPosition())) {
                    unit.useTech(TechType.Spider_Mines, unit.getPosition());
                    layingMines = true;
                }
            }
        }

    }

    private boolean isOnNaturalBunkerWall(Position pos) {
        TilePosition tile = pos.toTilePosition();
        TilePosition ebayTile = mapInfo.getNaturalBunkerEbayPosition();
        if (ebayTile != null
                && tile.getX() >= ebayTile.getX() && tile.getX() < ebayTile.getX() + UnitType.Terran_Engineering_Bay.tileWidth()
                && tile.getY() >= ebayTile.getY() && tile.getY() < ebayTile.getY() + UnitType.Terran_Engineering_Bay.tileHeight()) {
            return true;
        }
        TilePosition barracksTile = mapInfo.getNaturalBunkerBarracksPosition();
        if (barracksTile != null
                && tile.getX() >= barracksTile.getX() && tile.getX() < barracksTile.getX() + UnitType.Terran_Barracks.tileWidth()
                && tile.getY() >= barracksTile.getY() && tile.getY() < barracksTile.getY() + UnitType.Terran_Barracks.tileHeight()) {
            return true;
        }
        return false;
    }

    private boolean allowMineLaying() {
        if (recentlyMined) {
            return false;
        }

        int currentFrame = game.getFrameCount();
        int idOffset = (10 + (unit.getID() % 21)) * 24;
        if ((currentFrame - mineCycle + idOffset) % FULL_MINE_CYCLE < ALLOWED_MINE_CYCLE) {
            return true;
        }
        return false;
    }

    public void mineEnemyExpansions(HashMap<Base, CombatUnits> claimedBases) {
        if (enemyUnit != null && enemyUnit.getEnemyPosition() != null) {
            if (unit.getDistance(enemyUnit.getEnemyPosition()) < 200) {
                return;
            }
        }

        if (miningExpansion && targetedEnemyExpansion != null && enemyDepotNearBase(targetedEnemyExpansion)) {
            miningExpansion = false;
            layingMines = false;
            approachingStagingBase = false;
            targetedEnemyExpansion = null;
            detouringMine = false;
            miningStagingComplete = false;
        }

        if (approachingStagingBase && targetedEnemyExpansion != null) {
            ArrayList<Base> ordered = mapInfo.getOrderedExpansions();
            if (unit.getDistance(targetedEnemyExpansion.getCenter()) <= 800) {
                approachingStagingBase = false;
                miningStagingComplete = true;
                unit.useTech(TechType.Spider_Mines, targetedEnemyExpansion.getCenter());
                return;
            }
            if (!ordered.isEmpty() && unit.getDistance(ordered.get(0).getCenter()) < 96) {
                approachingStagingBase = false;
                miningStagingComplete = true;
                unit.useTech(TechType.Spider_Mines, targetedEnemyExpansion.getCenter());
            }
            return;
        }

        if (miningExpansion) {
            if (detouringMine) {
                return;
            }
            UnitCommand lastCmd = unit.getLastCommand();
            if (lastCmd != null && lastCmd.getType() == UnitCommandType.Use_Tech_Position) {
                return;
            }
            miningExpansion = false;
            layingMines = false;
            targetedEnemyExpansion = null;
        }

        ArrayList<Base> enemyExpansions = mapInfo.scoredBestEnemyExpansion(enemyUnits);

        for (Base expansion : enemyExpansions) {
            if (claimedBases.containsKey(expansion) && claimedBases.get(expansion) != this) {
                continue;
            }

            boolean alreadyMined = false;
            for (Unit friendlyMine : game.self().getUnits()) {
                if (friendlyMine.getType() == UnitType.Terran_Vulture_Spider_Mine) {
                    if (friendlyMine.getDistance(expansion.getCenter()) < 64) {
                        alreadyMined = true;
                        break;
                    }
                }
            }

            if (enemyDepotNearBase(expansion)) {
                continue;
            }

            if (!alreadyMined) {
                ArrayList<Base> ordered = mapInfo.getOrderedExpansions();
                targetedEnemyExpansion = expansion;
                claimedBases.put(expansion, this);
                layingMines = true;
                miningExpansion = true;

                if (!miningStagingComplete && !ordered.isEmpty() && unit.getDistance(ordered.get(0).getCenter()) < unit.getDistance(expansion.getCenter())) {
                    unit.move(ordered.get(0).getCenter());
                    approachingStagingBase = true;
                }
                else {
                    unit.useTech(TechType.Spider_Mines, expansion.getCenter());
                }
                break;
            }
        }
    }

    public void resetExpansionMining() {
        miningExpansion = false;
        layingMines = false;
        approachingStagingBase = false;
        targetedEnemyExpansion = null;
        detouringMine = false;
        miningStagingComplete = false;
    }

    private void attackBaseTarget(EnemyUnits depot, Base base) {
        if (enemyUnit != null && enemyUnit.getEnemyUnit() != null) {
            unit.attack(enemyUnit.getEnemyUnit());
            return;
        }
        if (depot != null && depot.getEnemyUnit() != null) {
            unit.attack(depot.getEnemyUnit());
            return;
        }
        unit.attack(base.getCenter());
    }

    private boolean enemyDepotNearBase(Base base) {
        return findDepotNearBase(base) != null;
    }

    private EnemyUnits findDepotNearBase(Base base) {
        for (EnemyUnits eu : enemyUnits) {
            if (!eu.getEnemyType().isResourceDepot()) {
                continue;
            }
            if (eu.getEnemyPosition() == null) {
                continue;
            }
            if (eu.getEnemyPosition().getDistance(base.getCenter()) < 256) {
                return eu;
            }
        }
        return null;
    }

    private Position runbyAttackPos(Base base, EnemyUnits depot) {
        Position depotPos = depot.getEnemyPosition();
        if (depotPos == null) {
            depotPos = base.getCenter();
        }

        List<Mineral> patches = mapInfo.getBasePatches(base);
        int sumX = 0;
        int sumY = 0;
        int count = 0;
        for (Mineral patch : patches) {
            Position p = patch.getPosition();
            if (p == null) {
                continue;
            }
            sumX += p.getX();
            sumY += p.getY();
            count++;
        }

        if (count == 0) {
            return depotPos;
        }

        Position mineralCentroid = new Position(sumX / count, sumY / count);
        return new Position(
                (depotPos.getX() + mineralCentroid.getX()) / 2,
                (depotPos.getY() + mineralCentroid.getY()) / 2);
    }

    public boolean isLobotomyOverride() {
        return lobotomyOverride;
    }

    public void setLobotomyOverride(boolean lobotomyOverride) {
        this.lobotomyOverride = lobotomyOverride;
    }

    public boolean isMiningExpansion() {
        return miningExpansion;
    }
}
