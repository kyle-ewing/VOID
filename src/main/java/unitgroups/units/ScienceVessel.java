package unitgroups.units;

import bwapi.Game;
import bwapi.TechType;
import bwapi.Unit;
import bwapi.UnitType;

public class ScienceVessel extends CombatUnits {
    private int irradiateClock = 0;
    private boolean irradiating = false;


    public ScienceVessel(Game game, Unit unit) {
        super(game, unit);

        priorityTargets.add(UnitType.Zerg_Defiler);
        priorityTargets.add(UnitType.Zerg_Mutalisk);
        priorityTargets.add(UnitType.Zerg_Lurker);
        priorityTargets.add(UnitType.Zerg_Guardian);
        priorityTargets.add(UnitType.Zerg_Queen);
    }

    public void rally() {
        if(rallyPoint == null) {
            return;
        }

        if(friendlyUnit == null) {
            unit.move(rallyPoint.toPosition());
            return;
        }

        if(irradiating) {
            irradiateClock += 8;

            if(irradiateClock >= 64) {
                irradiating = false;
                irradiateClock = 0;
            }
        }



        if(priorityEnemyUnit != null && priorityEnemyUnit.getEnemyUnit().isIrradiated()) {
            irradiateClock = 0;
            irradiating = false;
            setPriorityTargetLock(false);
            setIgnoreCurrentPriorityTarget(true);
        }
        else if(priorityEnemyUnit != null && !priorityEnemyUnit.getEnemyUnit().isIrradiated()) {
            setIgnoreCurrentPriorityTarget(false);
        }


        if(game.self().hasResearched(TechType.Irradiate) && unit.getEnergy() >= 75) {
            if(priorityEnemyUnit != null && unit.getDistance(priorityEnemyUnit.getEnemyUnit()) <= 550 && !ignoreCurrentPriorityTarget) {
                irradiate();
                return;
            }
        }

        if(irradiating) {
            return;
        }

        unit.move(friendlyUnit.getUnit().getPosition());

    }

    public void attack() {
        if(friendlyUnit == null) {
            super.setTargetRange(200);
            super.setUnitStatus(UnitStatus.RETREAT);
            return;
        }

        if(irradiating) {
            irradiateClock += 8;

            if(irradiateClock >= 64) {
                irradiating = false;
                irradiateClock = 0;
            }
        }

        if(priorityEnemyUnit == null) {
            irradiating = false;
            setPriorityTargetLock(false);
            setIgnoreCurrentPriorityTarget(false);
        }

        if(priorityEnemyUnit != null && priorityEnemyUnit.getEnemyUnit().isIrradiated()) {
            irradiateClock = 0;
            irradiating = false;
            setPriorityTargetLock(false);
            setIgnoreCurrentPriorityTarget(true);
        }
        else if(priorityEnemyUnit != null && !priorityEnemyUnit.getEnemyUnit().isIrradiated()) {
            setIgnoreCurrentPriorityTarget(false);
        }


        if(game.self().hasResearched(TechType.Irradiate) && unit.getEnergy() >= 75) {
            if(priorityEnemyUnit != null && unit.getDistance(priorityEnemyUnit.getEnemyUnit()) <= 550 && !ignoreCurrentPriorityTarget) {
                irradiate();
                return;
            }
        }

        if(irradiating) {
            return;
        }

        unit.move(friendlyUnit.getUnit().getPosition());
    }

    public void defend() {
        unit.move(friendlyUnit.getUnit().getPosition());
    }

    public void retreat() {
        if(rallyPoint == null) {
            return;
        }

        if(friendlyUnit != null) {
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

}
