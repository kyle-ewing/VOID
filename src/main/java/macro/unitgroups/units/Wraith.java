package macro.unitgroups.units;

import bwapi.Game;
import bwapi.Unit;
import bwapi.UnitType;
import information.BaseInfo;
import information.enemy.EnemyInformation;
import macro.unitgroups.CombatUnits;
import macro.unitgroups.UnitStatus;

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
        if(enemyUnit != null) {
            attack();
        }
        else {
            setUnitStatus(UnitStatus.ATTACK);
        }
    }
}
