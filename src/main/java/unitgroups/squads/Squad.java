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

    private static final double SMOOTHING_ALPHA = 0.85;
    private static final int CLUSTER_RADIUS = 300;

    public Squad(Game game) {
        this.game = game;
    }

    private void updateRegroupPosition() {
        if (squadUnits.size() == 0) {
            regroupPosition = new Position(0, 0);
            return;
        }

        List<CombatUnits> units = new ArrayList<>(squadUnits);

        CombatUnits anchor = units.get(0);
        int maxNeighbors = -1;

        for (CombatUnits candidate : units) {
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
            Position pos = unit.getUnit().getPosition();
            if (pos.getApproxDistance(anchorPos) <= CLUSTER_RADIUS) {
                cx += pos.getX();
                cy += pos.getY();
                count++;
            }
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

    private void checkRegroup() {
        if (squadUnits.size() < 4) {
            return;
        }

        for (CombatUnits unit : squadUnits) {
            if (unit.getUnitStatus() != UnitStatus.ATTACK) {
                continue; 
            }

            if (unit.getEnemyUnit() != null && unit.getEnemyUnit().getEnemyPosition() != null && unit.getUnit().getPosition().getDistance(unit.getEnemyUnit().getEnemyPosition()) < 150) {
                continue; 
            }

            if (unit.getUnit().getPosition().getDistance(regroupPosition) > 250 && game.isWalkable(regroupPosition.toWalkPosition())) {
                unit.setUnitStatus(UnitStatus.REGROUP);
            }
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

    
}
