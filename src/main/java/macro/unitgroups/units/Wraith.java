package macro.unitgroups.units;

import bwapi.Game;
import bwapi.Unit;
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
        unitStatus = UnitStatus.ATTACK;
    }

    public void attack() {
        if(enemyUnit == null) {
            return;
        }

        unit.attack(enemyUnit.getEnemyPosition());
    }

    public void retreat() {

    }
}
