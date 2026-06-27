package unitgroups.squads;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import bwapi.Game;
import bwapi.Position;
import bwapi.UnitType;
import unitgroups.units.CombatUnits;
import unitgroups.units.UnitStatus;

public class Squad {
    private Game game;
    private Position regroupPosition;
    private HashSet<CombatUnits> squadUnits = new HashSet<>();
    private HashMap<Integer, UnitType> squadComposition = new HashMap<>();
    private boolean enemyArmyExists = false;
    private boolean isRunbySquad = false;

    private static final double SMOOTHING_ALPHA = 0.85;
    private static final int CLUSTER_RADIUS = 300;
    private static final int TANK_WEIGHT = 4;
    private static final int TANK_THRESHOLD = 2;
    private static final int LEAD_DISTANCE = 200;

    public Squad(Game game) {
        this.game = game;
    }

    public Squad(Game game, boolean isRunbySquad) {
        this.game = game;
        this.isRunbySquad = isRunbySquad;
    }

    private void updateRegroupPosition() {
        if (squadUnits.size() == 0) {
            regroupPosition = new Position(0, 0);
            return;
        }

        List<CombatUnits> units = new ArrayList<>(squadUnits);
        boolean tankBias = siegeTankCount() >= TANK_THRESHOLD;
        boolean medicBias = siegeTankCount() == 0 && medicCount() >= 1;

        CombatUnits anchor = units.get(0);
        int maxNeighbors = -1;

        for (CombatUnits candidate : units) {
            if (candidate.isInBunker()) {
                continue;
            }

            if (isScienceVessel(candidate)) {
                continue;
            }

            if (tankBias && !isSiegeTank(candidate)) {
                continue;
            }

            if (medicBias && !isMedic(candidate)) {
                continue;
            }

            Position cp = candidate.getUnit().getPosition();
            int neighbors = 0;
            for (CombatUnits other : units) {
                if (cp.getApproxDistance(other.getUnit().getPosition()) <= CLUSTER_RADIUS) {
                    neighbors++;
                }
            }
            if (neighbors > maxNeighbors) {
                maxNeighbors = neighbors;
                anchor = candidate;
            }
        }

        Position anchorPos = anchor.getUnit().getPosition();
        int cx = 0;
        int cy = 0;
        int count = 0;

        for (CombatUnits unit : units) {
            if (isScienceVessel(unit)) {
                continue;
            }

            Position pos = unit.getUnit().getPosition();
            if (pos.getApproxDistance(anchorPos) <= CLUSTER_RADIUS) {
                int weight = 1;

                if (tankBias && isSiegeTank(unit)) {
                    weight = TANK_WEIGHT;
                }

                if (medicBias && isMedic(unit)) {
                    weight = TANK_WEIGHT;
                }
                cx += pos.getX() * weight;
                cy += pos.getY() * weight;
                count += weight;
            }
        }

        if (count == 0) {
            regroupPosition = new Position(0, 0);
            return;
        }

        Position clusterCenter = new Position(cx / count, cy / count);

        if (regroupPosition == null) {
            regroupPosition = clusterCenter;
            return;
        }

        int smoothX = (int) (regroupPosition.getX() * SMOOTHING_ALPHA + clusterCenter.getX() * (1 - SMOOTHING_ALPHA));
        int smoothY = (int) (regroupPosition.getY() * SMOOTHING_ALPHA + clusterCenter.getY() * (1 - SMOOTHING_ALPHA));
        regroupPosition = new Position(smoothX, smoothY);
    }

    private boolean isSiegeTank(CombatUnits unit) {
        return unit.getUnitType() == UnitType.Terran_Siege_Tank_Tank_Mode
                || unit.getUnitType() == UnitType.Terran_Siege_Tank_Siege_Mode;
    }

    private boolean isMedic(CombatUnits unit) {
        return unit.getUnitType() == UnitType.Terran_Medic;
    }

    private boolean isScienceVessel(CombatUnits unit) {
        return unit.getUnitType() == UnitType.Terran_Science_Vessel;
    }

    public int scienceVesselCount() {
        int count = 0;
        for (CombatUnits unit : squadUnits) {
            if (isScienceVessel(unit)) {
                count++;
            }
        }
        return count;
    }

    public int groundUnitCount() {
        int count = 0;
        for (CombatUnits unit : squadUnits) {
            if (!unit.getUnitType().isFlyer()) {
                count++;
            }
        }
        return count;
    }

    public int siegeTankCount() {
        int count = 0;
        for (CombatUnits unit : squadUnits) {
            if (isSiegeTank(unit)) {
                count++;
            }
        }
        return count;
    }

    public int medicCount() {
        int count = 0;
        for (CombatUnits unit : squadUnits) {
            if (isMedic(unit)) {
                count++;
            }
        }
        return count;
    }

    private void checkRegroup() {
        if (isRunbySquad) {
            return;
        }

        if (squadUnits.size() < 4 || !enemyArmyExists) {
            return;
        }

        if (!game.isWalkable(regroupPosition.toWalkPosition())) {
            return;
        }

        for (CombatUnits unit : squadUnits) {
            if (isScienceVessel(unit)) {
                continue;
            }

            if (unit.getUnitStatus() != UnitStatus.ATTACK) {
                continue;
            }

            if (unit.enemyInWeaponRange(64)) {
                continue;
            }

            if (unit.getEnemyUnit() == null || unit.getEnemyUnit().getEnemyPosition() == null) {
                continue;
            }

            if (unit.getUnit().getDistance(regroupPosition) <= LEAD_DISTANCE) {
                continue;
            }

            Position enemyPosition = unit.getEnemyUnit().getEnemyPosition();
            if (unit.getUnit().getDistance(enemyPosition) >= regroupPosition.getDistance(enemyPosition)) {
                continue;
            }

            unit.setUnitStatus(UnitStatus.REGROUP);
        }
    }

    public void addToSquad(CombatUnits unit) {
        squadUnits.add(unit);
        squadComposition.put(unit.getUnitID(), unit.getUnitType());
    }

    public void removeFromSquad(CombatUnits unit) {
        squadUnits.remove(unit);
        squadComposition.remove(unit.getUnitID());
    }

    public void onFrame() {
        updateRegroupPosition();

        for (CombatUnits unit : squadUnits) {
            unit.setInRunbySquad(isRunbySquad);

            if (isRunbySquad && unit.getUnitStatus() != UnitStatus.REGROUP) {
                unit.setUnitStatus(UnitStatus.RUNBY);
            }
        }

        checkRegroup();
    }

    public Position getRegroupPosition() {
        return regroupPosition;
    }

    public void setRegroupPosition(Position regroupPosition) {
        this.regroupPosition = regroupPosition;
    }

    public HashSet<CombatUnits> getSquadUnits() {
        return squadUnits;
    }

    public void setSquadUnits(HashSet<CombatUnits> squadUnits) {
        this.squadUnits = squadUnits;
    }

    public int getCountOf(UnitType type) {
        int count = 0;
        for (UnitType t : squadComposition.values()) {
            if (t == type) {
                count++;
            }
        }
        return count;
    }

    public HashMap<Integer, UnitType> getSquadComposition() {
        return squadComposition;
    }

    public void setSquadComposition(HashMap<Integer, UnitType> squadComposition) {
        this.squadComposition = squadComposition;
    }

    public boolean enemyArmyExists() {
        return enemyArmyExists;
    }

    public void setEnemyArmyExists(boolean enemyArmyExists) {
        this.enemyArmyExists = enemyArmyExists;
    }

    public boolean isRunbySquad() {
        return isRunbySquad;
    }
    
}
