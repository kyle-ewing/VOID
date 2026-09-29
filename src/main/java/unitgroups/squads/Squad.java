package unitgroups.squads;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import bwapi.Game;
import bwapi.Position;
import bwapi.UnitType;
import map.bwemwrappers.ChokePoint;
import unitgroups.units.CombatUnits;
import unitgroups.units.UnitStatus;

public class Squad {
    private Game game;
    private Position regroupPosition;
    private HashSet<CombatUnits> squadUnits = new HashSet<>();
    private HashMap<Integer, UnitType> squadComposition = new HashMap<>();
    private ArrayList<ChokePoint> narrowChokes;
    private boolean enemyArmyExists = false;
    private boolean isRunbySquad = false;

    private static final double SMOOTHING_ALPHA = 0.85;
    private static final int CLUSTER_RADIUS = 300;
    private static final int TANK_WEIGHT = 4;
    private static final int TANK_THRESHOLD = 2;
    private static final int LEAD_DISTANCE = 200;

    public Squad(Game game, ArrayList<ChokePoint> narrowChokes) {
        this.game = game;
        this.narrowChokes = narrowChokes;
    }

    public Squad(Game game, ArrayList<ChokePoint> narrowChokes, boolean isRunbySquad) {
        this.game = game;
        this.narrowChokes = narrowChokes;
        this.isRunbySquad = isRunbySquad;
    }

    private void updateRegroupPosition() {
        if (squadUnits.size() == 0) {
            regroupPosition = new Position(0, 0);
            return;
        }

        List<CombatUnits> units = new ArrayList<>(squadUnits);
        boolean tankBias = siegeTankCount() >= TANK_THRESHOLD;

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

            if (unit.getUnitStatus() != UnitStatus.ATTACK && unit.getUnitStatus() != UnitStatus.REGROUP) {
                continue;
            }

            if (regroupPathNearNarrowChoke(unit)) {
                if (unit.getUnitStatus() == UnitStatus.REGROUP) {
                    unit.setUnitStatus(UnitStatus.ATTACK);
                }
                unit.setForcedRegroup(false);
                continue;
            }

            boolean isolated = false;
            if (unit.getUnit().getDistance(regroupPosition) > 500) {
                isolated = true;
                for (CombatUnits other : squadUnits) {
                    if (other == unit) {
                        continue;
                    }

                    if (isScienceVessel(other)) {
                        continue;
                    }

                    if (unit.getUnit().getDistance(other.getUnit().getPosition()) <= 200) {
                        isolated = false;
                        break;
                    }
                }
            }

            if (isolated && !unit.enemyInWeaponRange(64) && !regroupMovesTowardEnemy(unit)) {
                unit.setUnitStatus(UnitStatus.REGROUP);
                unit.setForcedRegroup(true);
                continue;
            }

            unit.setForcedRegroup(false);

            if (unit.getUnitStatus() == UnitStatus.REGROUP) {
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

            if (regroupMovesTowardEnemy(unit)) {
                continue;
            }

            unit.setUnitStatus(UnitStatus.REGROUP);
        }
    }

    private boolean regroupPathNearNarrowChoke(CombatUnits unit) {
        Position unitPosition = unit.getUnit().getPosition();
        double segmentX = regroupPosition.getX() - unitPosition.getX();
        double segmentY = regroupPosition.getY() - unitPosition.getY();
        double segmentLengthSquared = segmentX * segmentX + segmentY * segmentY;

        for (ChokePoint choke : narrowChokes) {
            Position chokeCenter = choke.getCenter();
            double toChokeX = chokeCenter.getX() - unitPosition.getX();
            double toChokeY = chokeCenter.getY() - unitPosition.getY();

            double t = 0;
            if (segmentLengthSquared > 0) {
                t = (toChokeX * segmentX + toChokeY * segmentY) / segmentLengthSquared;
                t = Math.max(0, Math.min(1, t));
            }

            double nearestX = unitPosition.getX() + segmentX * t;
            double nearestY = unitPosition.getY() + segmentY * t;
            double dx = chokeCenter.getX() - nearestX;
            double dy = chokeCenter.getY() - nearestY;
            double radius = choke.getWidth() / 2.0 + 192;

            if (dx * dx + dy * dy < radius * radius) {
                return true;
            }
        }

        return false;
    }

    private boolean regroupMovesTowardEnemy(CombatUnits unit) {
        if (unit.getEnemyUnit() == null || unit.getEnemyUnit().getEnemyPosition() == null) {
            return false;
        }

        Position enemyPosition = unit.getEnemyUnit().getEnemyPosition();
        return unit.getUnit().getDistance(enemyPosition) >= regroupPosition.getDistance(enemyPosition);
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
