package unitgroups.units;

import java.util.HashSet;

import bwapi.Game;
import bwapi.Position;
import bwapi.TechType;
import bwapi.Unit;
import bwapi.UnitType;
import bwapi.UpgradeType;
import bwapi.WeaponType;
import information.MapInfo;
import information.enemy.EnemyInformation;
import information.enemy.EnemyUnits;
import util.Time;

public class Wraith extends CombatUnits {
    private EnemyInformation enemyInformation;
    private MapInfo mapInfo;
    private HashSet<EnemyUnits> enemyUnits;
    private int decloakTimer = 0;
    private int weaponRange = unit.getType().airWeapon().maxRange();

    private static final int SCAN_RANGE = 400;
    private static final int SCAN_BUFFER = 96;

    public Wraith(Game game, EnemyInformation enemyInformation, Unit unit) {
        super(game, unit);
        this.enemyInformation = enemyInformation;
        this.enemyUnits = enemyInformation.getEnemyUnits();

        mapInfo = enemyInformation.getBaseInfo();
        unitStatus = UnitStatus.HUNTING;
        priorityTargets.add(UnitType.Protoss_Shuttle);
        priorityTargets.add(UnitType.Protoss_Probe);
        priorityTargets.add(UnitType.Zerg_Drone);
        priorityTargets.add(UnitType.Zerg_Guardian);
        priorityTargets.add(UnitType.Zerg_Queen);
        priorityTargets.add(UnitType.Terran_Dropship);
    }

    @Override
    public void attack() {
        int frameCount = game.getFrameCount();

        if (frameCount % 4 != 0) {
            return;
        }

        if (enemyUnit == null) {
            return;
        }

        if (enemyUnit.getEnemyPosition() == null) {
            return;
        }

        if (priorityTargetExists) {
            setUnitStatus(UnitStatus.HUNTING);
        }
        else {
            setUnitStatus(UnitStatus.ATTACK);
        }

        if (game.self().hasResearched(TechType.Cloaking_Field)) {
            if (inUnitAntiAirRange() && unit.getEnergy() > 50) {
                unit.cloak();
            }
            else {
                if (unit.isCloaked()) {
                    decloakTimer += 4;

                    if (decloakTimer >= 192) {
                        unit.decloak();
                        decloakTimer = 0;
                    }
                }
            }
        }

        attackMove();
    }

    @Override
    public void hunting() {
        if (priorityEnemyUnit != null) {
            attack();
        }
        else {
            setUnitStatus(UnitStatus.ATTACK);
        }
    }

    private void attackMove() {
        EnemyUnits target = enemyUnit;
        if (!priorityTargetExists) {
            EnemyUnits preferred = findValidTarget();
            if (preferred != null) {
                target = preferred;
            }
        }

        if (target.getEnemyPosition() == null) {
            return;
        }
        
        Position enemyPos = target.getEnemyPosition();
        Position unitPos = unit.getPosition();
        double distToEnemy = unitPos.getDistance(enemyPos);

        if (!unit.isCloaked() || unit.isDetected()) {
            Position kitePos = getKitePosition();

            if (kitePos != null) {
                unit.move(kitePos);
                return;
            }
        }

        boolean onCooldown = unit.getAirWeaponCooldown() > 0;
        boolean inAttackAnimation = unit.isAttackFrame() || unit.isStartingAttack();

        if (distToEnemy > weaponRange + 64) {
            unit.move(approachTo(enemyPos));
        } 
        else if (onCooldown || inAttackAnimation || unit.isPatrolling()) {
            unit.move(kiteAwayFrom(enemyPos, weaponRange + 64));
        } 
        else {
            unit.patrol(enemyPos);
        }
    }

    private EnemyUnits findValidTarget() {
        EnemyUnits wraith = null, tank = null, scv = null, other = null, building = null;
        double wraithDist = Double.MAX_VALUE, tankDist = Double.MAX_VALUE, scvDist = Double.MAX_VALUE;
        double otherDist = Double.MAX_VALUE, buildingDist = Double.MAX_VALUE;

        boolean workerPosKnown = enemyUnits.stream().anyMatch(eu -> eu.getEnemyPosition() != null && eu.getEnemyType().isWorker());
        boolean isSeen = !unit.isCloaked() && unit.isDetected();

        for (EnemyUnits enemy : enemyUnits) {
            if ((enemy.getEnemyUnit().isCloaked() 
                    || enemy.getEnemyUnit().isBurrowed()) 
                    && !enemy.getEnemyUnit().isDetected()) {
                continue;
            }

            if (enemy.getEnemyPosition() == null) {
                continue;
            }

            if (enemy.getEnemyType() == UnitType.Terran_Medic || (enemy.getEnemyType() == UnitType.Terran_Marine && isSeen)) {
                continue;
            }

            double dist = unit.getDistance(enemy.getEnemyPosition());
            
            UnitType type = enemy.getEnemyType();

            if (workerPosKnown && type.isBuilding()) {
                continue;
            }

            if (dist > 400) {
                continue;
            }

            if (type == UnitType.Terran_Wraith && dist < wraithDist) {
                wraith = enemy;
                wraithDist = dist;
            } 
            else if ((type == UnitType.Terran_Siege_Tank_Tank_Mode || type == UnitType.Terran_Siege_Tank_Siege_Mode)
                    && dist < tankDist) {
                tank = enemy;
                tankDist = dist;
            } 
            else if (type == UnitType.Terran_SCV && dist < scvDist) {
                scv = enemy;
                scvDist = dist;
            } 
            else if (!type.isBuilding() && dist < otherDist) {
                other = enemy;
                otherDist = dist;
            } 
            else if (type.isBuilding() && dist < buildingDist) {
                building = enemy;
                buildingDist = dist;
            }
        }

        if (wraith != null) {
            return wraith;
        }

        if (tank != null) {
            return tank;
        }

        if (scv != null) {
            return scv;
        }

        if (other != null) {
            return other;
        }
        return building;
    }

    private Position approachTo(Position targetPos) {
        Position unitPos = unit.getPosition();
        double dx = unitPos.getX() - targetPos.getX();
        double dy = unitPos.getY() - targetPos.getY();
        double dist = Math.max(1, unitPos.getDistance(targetPos));

        double moveX = targetPos.getX() + (dx * (weaponRange - 32) / dist);
        double moveY = targetPos.getY() + (dy * (weaponRange - 32) / dist);

        moveX = Math.min(Math.max(moveX, 0), game.mapWidth() * 32);
        moveY = Math.min(Math.max(moveY, 0), game.mapHeight() * 32);
        return new Position((int) moveX, (int) moveY);
    }

    private Position kiteAwayFrom(Position enemyPos, int distance) {
        Position unitPos = unit.getPosition();
        int dx = unitPos.getX() - enemyPos.getX();
        int dy = unitPos.getY() - enemyPos.getY();
        double dist = Math.max(1, unitPos.getDistance(enemyPos));

        double moveX = unitPos.getX() + (dx * distance / dist);
        double moveY = unitPos.getY() + (dy * distance / dist);

        moveX = Math.min(Math.max(moveX, 0), game.mapWidth() * 32);
        moveY = Math.min(Math.max(moveY, 0), game.mapHeight() * 32);
        return new Position((int) moveX, (int) moveY);
    }

    //Units only no static D
    private boolean inUnitAntiAirRange() {
        for (EnemyUnits enemy : enemyUnits) {
            if (enemy.getEnemyType().isBuilding()) {
                continue;
            }

            int range = getAntiAirRange(enemy);

            if (range == 0) {
                continue;
            }

            if (unit.getDistance(enemy.getEnemyPosition()) < range + 64) {
                return true;
            }
        }
        return false;
    }

    private Position getKitePosition() {
        Position unitPos = unit.getPosition();
        double sumDx = 0;
        double sumDy = 0;
        boolean anyThreat = false;
        boolean cloakedSafe = unit.isCloaked() && !unit.isDetected();

        for (EnemyUnits enemy : enemyUnits) {
            if (enemy.getEnemyType() == UnitType.Spell_Scanner_Sweep) {
                if (enemy.getEnemyPosition() == null) {
                    continue;
                }

                double scanDist = unitPos.getDistance(enemy.getEnemyPosition());
                int scanSafeDistance = SCAN_RANGE + SCAN_BUFFER;

                if (scanDist < scanSafeDistance) {
                    anyThreat = true;
                    double dx = unitPos.getX() - enemy.getEnemyPosition().getX();
                    double dy = unitPos.getY() - enemy.getEnemyPosition().getY();
                    double weight = (scanSafeDistance - scanDist) / (double) scanSafeDistance;
                    sumDx += (dx / Math.max(1, scanDist)) * weight;
                    sumDy += (dy / Math.max(1, scanDist)) * weight;
                }
                continue;
            }

            int range = getAntiAirRange(enemy);

            if (range == 0) {
                continue;
            }

            if (enemy.getEnemyPosition() == null) {
                continue;
            }

            UnitType type = enemy.getEnemyType();
            boolean staticAA = type == UnitType.Terran_Missile_Turret
                    || type == UnitType.Zerg_Spore_Colony
                    || type == UnitType.Protoss_Photon_Cannon;;

            if (!type.isBuilding() && cloakedSafe) {
                continue;
            }

            if (type.isBuilding() && !staticAA && cloakedSafe) {
                continue;
            }

            if (type.isBuilding() && enemy.getEnemyUnit().isVisible()) {
                if (!enemy.getEnemyUnit().isCompleted() || enemy.getEnemyUnit().isMorphing()
                        || !enemy.getEnemyUnit().isPowered()) {
                    continue;
                }
            }

            double threatDist = unitPos.getDistance(enemy.getEnemyPosition());
            
            int safeDistance;
            if (staticAA) {
                safeDistance = range + 160;
            } 
            else {
                safeDistance = range + 96;
            }

            if (threatDist < safeDistance) {
                anyThreat = true;
                double dx = unitPos.getX() - enemy.getEnemyPosition().getX();
                double dy = unitPos.getY() - enemy.getEnemyPosition().getY();
                double weight = (safeDistance - threatDist) / safeDistance;
                sumDx += (dx / Math.max(1, threatDist)) * weight;
                sumDy += (dy / Math.max(1, threatDist)) * weight;
            }
        }

        if (!anyThreat) {
            return null;
        }

        double len = Math.sqrt(sumDx * sumDx + sumDy * sumDy);
        if (len < 0.001) {
            return null;
        }

        //Avoid positions that are out of bounds

        int maxX = game.mapWidth() * 32 - 1;
        int maxY = game.mapHeight() * 32 - 1;

        double rawX = unitPos.getX() + (sumDx / len) * 320;
        double rawY = unitPos.getY() + (sumDy / len) * 320;

        boolean xOutOfBounds = rawX < 0 || rawX > maxX;
        boolean yOutOfBounds = rawY < 0 || rawY > maxY;

        double moveX;
        double moveY;

        if (xOutOfBounds && yOutOfBounds) {
            return null;
        } 
        else if (xOutOfBounds) {
            moveX = unitPos.getX();
            moveY = Math.min(Math.max(unitPos.getY() + Math.signum(sumDy) * 320, 0), maxY);
        } 
        else if (yOutOfBounds) {
            moveX = Math.min(Math.max(unitPos.getX() + Math.signum(sumDx) * 320, 0), maxX);
            moveY = unitPos.getY();
        } 
        else {
            moveX = Math.min(Math.max(rawX, 0), maxX);
            moveY = Math.min(Math.max(rawY, 0), maxY);
        }

        return new Position((int) moveX, (int) moveY);
    }

    private int getAntiAirRange(EnemyUnits enemy) {
        if (enemy.getEnemyType().airWeapon() != WeaponType.None) {
            return enemy.getEnemyType().airWeapon().maxRange();
        }

        if (enemy.getEnemyType().groundWeapon().targetsAir()) {
            return enemy.getEnemyType().groundWeapon().maxRange() + 32;
        }
        return 0;
    }
}
