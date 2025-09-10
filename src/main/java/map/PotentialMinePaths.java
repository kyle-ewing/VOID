package map;

import bwapi.Position;
import bwem.Base;
import bwem.ChokePoint;
import information.BaseInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PotentialMinePaths {
    private BaseInfo baseInfo;
    private HashMap<Base, List<Position>> pathLists = new HashMap<>();

    public PotentialMinePaths(BaseInfo baseInfo) {
        this.baseInfo = baseInfo;

        calculateAllPaths();
    }

    private void calculateAllPaths() {
        Position startingBasePos = baseInfo.getStartingBase().getCenter();

        for(Base base : baseInfo.getMapBases()) {
            if(baseInfo.getStartingBase().equals(base)) {
                continue;
            }

            Position nearestWalkable = baseInfo.getPathFinding().findNearestWalkable(base.getCenter());
            List<Position> path = baseInfo.getPathFinding().findPath(startingBasePos, nearestWalkable);

            for(ChokePoint choke : baseInfo.getChokePoints()) {
                Position chokePos = choke.getCenter().toPosition();
                for(Position pathPos : path) {
                    if(chokePos.getDistance(pathPos) < 175) {
                        if(choke != baseInfo.getMainChoke() && choke != baseInfo.getNaturalChoke()) {
                            if(!pathLists.containsKey(base)) {
                                pathLists.put(base, new ArrayList<>());
                            }
                            pathLists.get(base).add(chokePos);
                        }
                    }
                }
            }
        }
    }

    public HashMap<Base, List<Position>> getPathLists() {
        return pathLists;
    }
}
