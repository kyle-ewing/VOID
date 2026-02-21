package unitgroups.units;

import bwapi.Game;
import bwapi.Unit;
import bwapi.UnitType;
import information.BaseInfo;
import information.enemy.EnemyInformation;
import util.Time;

public class Wraith extends CombatUnits {
    private EnemyInformation enemyInformation;
    private BaseInfo baseInfo;

    public Wraith(Game game, EnemyInformation enemyInformation, Unit unit) {
        super(game, unit);
        this.enemyInformation = enemyInformation;
        baseInfo = enemyInformation.getBaseInfo();
        unitStatus = UnitStatus.HUNTING;
        priorityTargets.add(UnitType.Protoss_Shuttle);
        priorityTargets.add(UnitType.Protoss_Probe);
        priorityTargets.add(UnitType.Zerg_Drone);
        priorityTargets.add(UnitType.Zerg_Guardian);
        priorityTargets.add(UnitType.Zerg_Queen);
        priorityTargets.add(UnitType.Terran_Dropship);
        priorityTargets.add(UnitType.Terran_SCV);
    }

    @Override
    public void attack() {
        int frameCount = game.getFrameCount();

        if(frameCount % 12 != 0) {
            return;
        }

        if(enemyUnit == null) {
            return;
        }

        if(enemyUnit.getEnemyPosition() == null) {
            return;
        }

        if(priorityTargetExists) {
            setUnitStatus(UnitStatus.HUNTING);
        }
        else {
            setUnitStatus(UnitStatus.ATTACK);
        }

        if(unit.getDistance(enemyUnit.getEnemyPosition()) > 128) {
            unit.move(enemyUnit.getEnemyPosition());
        }
        else {
            unit.attack(enemyUnit.getEnemyUnit());
        }
    }

    @Override
    public void hunting() {
        if(priorityEnemyUnit != null) {
            attack();
        }
        else {
            setUnitStatus(UnitStatus.ATTACK);
        }
    }
}
