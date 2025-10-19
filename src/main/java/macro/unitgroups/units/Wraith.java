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
        if(enemyUnit == null) {
            return;
        }

        if(priorityTargetExists) {
            setUnitStatus(UnitStatus.HUNTING);
        }
        else {
            setUnitStatus(UnitStatus.ATTACK);
        }

        unit.attack(enemyUnit.getEnemyPosition());
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
