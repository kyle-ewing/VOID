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
import util.Time;

public class SquadManager {
    private Game game;
    private GameState gamestate;
    private EnemyInformation enemyInformation;
    private List<Squad> squads = new ArrayList<>();
    private HashMap<UnitType, Integer> compositionLimits = new HashMap<>();
    private float enemyArmySupply = 0;
    private int runbySquadCooldown = 0;
    private boolean runbySquadActive = false;

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
        if (unit.getUnitStatus() == UnitStatus.MINE) {
            return;
        }

        if (unit.getUnitType() == UnitType.Terran_Science_Vessel) {
            assignVesselSquad(unit);
            return;
        }

        if (unit.getUnitType().isFlyer()) {
            //implement air squad later
            return;
        }

        UnitType type = unit.getUnitType();
        int limit = compositionLimits.getOrDefault(type, Integer.MAX_VALUE);

        for (Squad squad : squads) {
            if (squad.isRunbySquad()) {
                continue;
            }
            if (squad.getCountOf(type) < limit) {
                squad.addToSquad(unit);
                return;
            }
        }

        Squad newSquad = new Squad(game);
        newSquad.addToSquad(unit);
        squads.add(newSquad);
    }

    public Squad getSquadOfUnit(CombatUnits unit) {
        for (Squad squad : squads) {
            if (squad.getSquadUnits().contains(unit)) {
                return squad;
            }
        }
        return null;
    }

    public void transferUnit(CombatUnits unit, Squad fromSquad, Squad toSquad) {
        fromSquad.removeFromSquad(unit);
        toSquad.addToSquad(unit);
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

    private void manageVesselSquads() {
        for (CombatUnits unit : gamestate.getCombatUnits()) {
            if (unit.getUnitType() != UnitType.Terran_Science_Vessel) {
                continue;
            }

            assignVesselSquad(unit);
        }
    }

    private void assignVesselSquad(CombatUnits vessel) {
        Squad currentSquad = getSquadOfUnit(vessel);

        if (currentSquad != null && isValidVesselSquad(currentSquad)) {
            return;
        }

        Squad target = findValidVesselSquad();

        if (target != null) {
            if (currentSquad != null) {
                transferUnit(vessel, currentSquad, target);
                return;
            }
            target.addToSquad(vessel);
            return;
        }

        if (currentSquad != null && currentSquad.groundUnitCount() > 0) {
            return;
        }

        Squad fallback = findGroundSquad();

        if (fallback != null) {
            if (currentSquad != null) {
                transferUnit(vessel, currentSquad, fallback);
                return;
            }
            fallback.addToSquad(vessel);
            return;
        }

        if (currentSquad != null) {
            currentSquad.removeFromSquad(vessel);
        }
    }

    private boolean isValidVesselSquad(Squad squad) {
        if (squad.isRunbySquad()) {
            return false;
        }

        if (squad.groundUnitCount() == 0) {
            return false;
        }

        return squad.scienceVesselCount() <= 2;
    }

    private Squad findValidVesselSquad() {
        for (Squad squad : squads) {
            if (squad.isRunbySquad()) {
                continue;
            }

            if (squad.groundUnitCount() == 0) {
                continue;
            }

            if (squad.scienceVesselCount() >= 2) {
                continue;
            }

            return squad;
        }
        return null;
    }

    private Squad findGroundSquad() {
        for (Squad squad : squads) {
            if (squad.isRunbySquad()) {
                continue;
            }

            if (squad.groundUnitCount() == 0) {
                continue;
            }

            return squad;
        }
        return null;
    }

    private void createRunbySquad() {
        Squad runbySquad = new Squad(game, true);
        int addedUnits = 0;
        for (Squad squad : squads) {
            if (squad.isRunbySquad()) {
                continue;
            }

            if (addedUnits >= 4) {
                break;
            }
            for (CombatUnits unit : new ArrayList<>(squad.getSquadUnits())) {
                if (addedUnits >= 4) {
                    break;
                }
                if (unit.getUnitType() == UnitType.Terran_Vulture) {
                    transferUnit(unit, squad, runbySquad);
                    addedUnits++;
                }
            }
        }

        if (addedUnits > 0) {
            squads.add(runbySquad);
        }
    }

    private void initCompositionLimits() {
        compositionLimits.put(UnitType.Terran_Marine, 20);
        compositionLimits.put(UnitType.Terran_Firebat, 8);
        compositionLimits.put(UnitType.Terran_Medic, 6);
        compositionLimits.put(UnitType.Terran_Siege_Tank_Tank_Mode, 8);
        compositionLimits.put(UnitType.Terran_Vulture, 12);
        compositionLimits.put(UnitType.Terran_Goliath, 12);
    }

    public void onFrame() {
        enemyArmySupply = enemyInformation.getEnemyArmySupply();

        manageVesselSquads();

        for (Squad squad : squads) {
            if (enemyArmySupply > 8) {
                squad.setEnemyArmyExists(true);
            } 
            else {
                squad.setEnemyArmyExists(false);
            }

            squad.onFrame();
        }

        if (new Time(game.getFrameCount()).greaterThan(new Time(10,30))
                && gamestate.getUnitTypeCount().getOrDefault(UnitType.Terran_Vulture, 0) >= 8
                && squads.stream().filter(s -> s.isRunbySquad()).count() == 0
                && enemyInformation.getEnemyUnits().stream().filter(eu -> eu.getEnemyType().isResourceDepot()).count() >= 2
                && !runbySquadActive) {
            createRunbySquad();
            runbySquadActive = true;
        }

        if (runbySquadActive) {
            runbySquadCooldown++;

            if (new Time(runbySquadCooldown).greaterThan(new Time(2,0))) {
                runbySquadActive = false;
                runbySquadCooldown = 0;
            }
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
