package unitgroups.units;

import java.util.HashSet;

import bwapi.Game;
import bwapi.Position;
import bwapi.TechType;
import bwapi.Unit;
import bwapi.UnitType;
import information.enemy.EnemyInformation;
import information.enemy.EnemyUnits;

public class ScienceVessel extends CombatUnits {
    private HashSet<EnemyUnits> enemyUnits;
    private int irradiateClock = 0;
    private boolean irradiating = false;


    public ScienceVessel(Game game, EnemyInformation enemyInformation, Unit unit) {
        super(game, unit);
        this.enemyUnits = enemyInformation.getEnemyUnits();

        priorityTargets.add(UnitType.Zerg_Defiler);
        priorityTargets.add(UnitType.Zerg_Mutalisk);
        priorityTargets.add(UnitType.Zerg_Lurker);
        priorityTargets.add(UnitType.Zerg_Guardian);
        priorityTargets.add(UnitType.Zerg_Queen);
    }

    public void rally() {
        if (irradiating) {
            irradiateClock += 8;

            if (irradiateClock >= 64) {
                irradiating = false;
                irradiateClock = 0;
            }
        }



        if (priorityEnemyUnit != null && priorityEnemyUnit.getEnemyUnit().isIrradiated()) {
            irradiateClock = 0;
            irradiating = false;
            setPriorityTargetLock(false);
            setIgnoreCurrentPriorityTarget(true);
        }
        else if (priorityEnemyUnit != null && !priorityEnemyUnit.getEnemyUnit().isIrradiated()) {
            setIgnoreCurrentPriorityTarget(false);
        }


        if (game.self().hasResearched(TechType.Irradiate) && unit.getEnergy() >= 75) {
            if (priorityEnemyUnit != null && unit.getDistance(priorityEnemyUnit.getEnemyUnit()) <= 550 && !ignoreCurrentPriorityTarget) {
                irradiate();
                return;
            }
        }

        if (irradiating) {
            return;
        }

        if (nearbyAirThreat()) {
            avoidAirThreat();
            return;
        }

        if (rallyPoint == null) {
            return;
        }

        if (friendlyUnit == null) {
            unit.move(rallyPoint.toPosition());
            return;
        }

        unit.move(friendlyUnit.getUnit().getPosition());

    }

    public void attack() {
        if (irradiating) {
            irradiateClock += 8;

            if (irradiateClock >= 64) {
                irradiating = false;
                irradiateClock = 0;
            }
        }

        if (priorityEnemyUnit == null) {
            irradiating = false;
            setPriorityTargetLock(false);
            setIgnoreCurrentPriorityTarget(false);
        }

        if (priorityEnemyUnit != null && priorityEnemyUnit.getEnemyUnit().isIrradiated()) {
            irradiateClock = 0;
            irradiating = false;
            setPriorityTargetLock(false);
            setIgnoreCurrentPriorityTarget(true);
        }
        else if (priorityEnemyUnit != null && !priorityEnemyUnit.getEnemyUnit().isIrradiated()) {
            setIgnoreCurrentPriorityTarget(false);
        }


        if (game.self().hasResearched(TechType.Irradiate) && unit.getEnergy() >= 75) {
            if (priorityEnemyUnit != null && unit.getDistance(priorityEnemyUnit.getEnemyUnit()) <= 550 && !ignoreCurrentPriorityTarget) {
                irradiate();
                return;
            }
        }

        if (irradiating) {
            return;
        }

        if (nearbyAirThreat()) {
            avoidAirThreat();
            return;
        }

        if (friendlyUnit == null) {
            super.setTargetRange(200);
            super.setUnitStatus(UnitStatus.RETREAT);
            return;
        }

        unit.move(friendlyUnit.getUnit().getPosition());
    }

    public void defend() {
        if (nearbyAirThreat()) {
            avoidAirThreat();
            return;
        }

        unit.move(friendlyUnit.getUnit().getPosition());
    }

    public void retreat() {
        if (nearbyAirThreat()) {
            avoidAirThreat();
            return;
        }

        if (rallyPoint == null) {
            return;
        }

        if (friendlyUnit != null) {
            super.setUnitStatus(UnitStatus.ATTACK);
            return;
        }

        unit.move(rallyPoint.toPosition());
    }

    private void irradiate() {
        irradiating = true;
        setPriorityTargetLock(true);
        unit.useTech(TechType.Irradiate, priorityEnemyUnit.getEnemyUnit());
    }

    private boolean nearbyAirThreat() {
        for (EnemyUnits enemy : enemyUnits) {
            if (enemy.getEnemyPosition() == null) {
                continue;
            }

            if (!enemy.getEnemyType().isFlyer() || !enemy.getEnemyType().airWeapon().targetsAir() || !enemy.getEnemyUnit().exists()) {
                continue;
            }

            int radius = 200;
            if (enemy.getEnemyType() == UnitType.Zerg_Scourge) {
                radius = 280;
            }

            if (unit.getDistance(enemy.getEnemyPosition()) < radius) {
                return true;
            }
        }
        return false;
    }

    private void avoidAirThreat() {
        Position unitPos = unit.getPosition();
        double sumDx = 0;
        double sumDy = 0;
        boolean anyThreat = false;

        for (EnemyUnits enemy : enemyUnits) {
            if (enemy.getEnemyPosition() == null) {
                continue;
            }

            if (!enemy.getEnemyType().isFlyer() || !enemy.getEnemyType().airWeapon().targetsAir() || !enemy.getEnemyUnit().exists()) {
                continue;
            }

            int radius = 200;
            if (enemy.getEnemyType() == UnitType.Zerg_Scourge) {
                radius = 280;
            }

            double threatDist = unitPos.getDistance(enemy.getEnemyPosition());
            if (threatDist >= radius) {
                continue;
            }

            anyThreat = true;
            double dx = unitPos.getX() - enemy.getEnemyPosition().getX();
            double dy = unitPos.getY() - enemy.getEnemyPosition().getY();
            double weight = (radius - threatDist) / radius;
            sumDx += (dx / Math.max(1, threatDist)) * weight;
            sumDy += (dy / Math.max(1, threatDist)) * weight;
        }

        if (!anyThreat) {
            return;
        }

        double len = Math.sqrt(sumDx * sumDx + sumDy * sumDy);
        if (len < 0.001) {
            return;
        }

        int maxX = game.mapWidth() * 32 - 1;
        int maxY = game.mapHeight() * 32 - 1;

        double moveX = Math.min(Math.max(unitPos.getX() + (sumDx / len) * 320, 0), maxX);
        double moveY = Math.min(Math.max(unitPos.getY() + (sumDy / len) * 320, 0), maxY);

        unit.move(new Position((int) moveX, (int) moveY));
    }

}
