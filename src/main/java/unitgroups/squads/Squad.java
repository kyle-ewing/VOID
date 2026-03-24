package unitgroups.squads;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import bwapi.Position;
import bwapi.UnitType;
import unitgroups.units.CombatUnits;
import unitgroups.units.UnitStatus;

public class Squad {
    private Position regroupPosition;
    private HashSet<CombatUnits> squadUnits = new HashSet<>();
    private HashMap<Integer, UnitType> squadComposition = new HashMap<>();

    private void updateRegroupPosition() {
        int x = 0;
        int y = 0;

        for (CombatUnits unit : squadUnits) {
            x += unit.getUnit().getPosition().getX();
            y += unit.getUnit().getPosition().getY();
        }

        if (squadUnits.size() == 0) {
            regroupPosition = new Position(0, 0);
            return;
        }

        regroupPosition = new Position(x / squadUnits.size(), y / squadUnits.size());
    }

    private void checkRegroup() {
        for (CombatUnits unit : squadUnits) {
            if (unit.getUnitStatus() != UnitStatus.ATTACK) {
                continue; 
            }

            if (unit.getUnit().getPosition().getDistance(regroupPosition) > 200) {
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
}
