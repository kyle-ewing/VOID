package unitgroups.squads;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import bwapi.Game;
import bwapi.UnitType;
import information.GameState;
import information.enemy.EnemyInformation;
import unitgroups.units.CombatUnits;
import unitgroups.units.UnitStatus;

public class SquadManager {
    private Game game;
    private GameState gamestate;
    private EnemyInformation enemyInformation;
    private List<Squad> squads = new ArrayList<>();
    private HashMap<UnitType, Integer> compositionLimits = new HashMap<>();
    private float enemyArmySupply = 0;

    public SquadManager(Game game, GameState gamestate, EnemyInformation enemyInformation) {
        this.game = game;
        this.gamestate = gamestate;
        this.enemyInformation = enemyInformation;
        squads.add(new Squad(game));
        initCompositionLimits();
    }

    public void updateRegroupPositions() {
        for (Squad squad : squads) {
            for (CombatUnits combatUnit : squad.getSquadUnits()) {
                combatUnit.setRegroupPosition(squad.getRegroupPosition());
            }
        }
    }

    public void addUnitToSquad(CombatUnits unit) {
        if (unit.getUnitType().isFlyer() || unit.getUnitStatus() == UnitStatus.MINE) {
            //implement air squad later
            return;
        }

        UnitType type = unit.getUnitType();
        int limit = compositionLimits.getOrDefault(type, Integer.MAX_VALUE);

        for (Squad squad : squads) {
            if (squad.getCountOf(type) < limit) {
                squad.addToSquad(unit);
                return;
            }
        }

        Squad newSquad = new Squad(game);
        newSquad.addToSquad(unit);
        squads.add(newSquad);
    }

    public void removeUnitFromSquad(CombatUnits unit) {
        for (Squad squad : squads) {
            squad.removeFromSquad(unit);
        }
        squads.removeIf(squad -> squad.getSquadUnits().isEmpty());
        if (squads.isEmpty()) {
            squads.add(new Squad(game));
        }
    }

    private void initCompositionLimits() {
        compositionLimits.put(UnitType.Terran_Marine, 20);
        compositionLimits.put(UnitType.Terran_Firebat, 8);
        compositionLimits.put(UnitType.Terran_Medic, 6);
        compositionLimits.put(UnitType.Terran_Siege_Tank_Tank_Mode, 6);
        compositionLimits.put(UnitType.Terran_Vulture, 12);
        compositionLimits.put(UnitType.Terran_Goliath, 12);
    }

    public void onFrame() {
        enemyArmySupply = enemyInformation.getEnemyArmySupply();

        for (Squad squad : squads) {
            if (enemyArmySupply > 8) {
                squad.setEnemyArmyExists(true);
            } 
            else {
                squad.setEnemyArmyExists(false);
            }

            squad.onFrame();
        }
        updateRegroupPositions();
    }

    public void onUnitComplete(CombatUnits unit) {
        addUnitToSquad(unit);
    }

    public void onUnitDestroy(CombatUnits unit) {
        removeUnitFromSquad(unit);
    }
}
