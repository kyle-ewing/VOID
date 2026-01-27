package map;

import bwapi.Position;
import bwem.Base;
import bwem.ChokePoint;
import information.BaseInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AllBasePaths {
    private BaseInfo baseInfo;
    private HashMap<Base, List<Position>> basePathLists = new HashMap<>();
    private HashMap<Base, List<Position>> chokePathLists = new HashMap<>();

    public AllBasePaths(BaseInfo baseInfo) {
        this.baseInfo = baseInfo;

        calculateBasePaths();
        calculateChokePaths();
    }

    private void calculateBasePaths() {
        Position startingBasePos = baseInfo.getStartingBase().getCenter();

        for(Base base : baseInfo.getMapBases()) {
            if(baseInfo.getStartingBase().equals(base)) {
                continue;
            }

            Position nearestWalkable = baseInfo.getPathFinding().findNearestWalkable(base.getCenter());
            List<Position> path = baseInfo.getPathFinding().findPath(startingBasePos, nearestWalkable);

            if(path == null || path.isEmpty()) {
                continue;
            }

            basePathLists.put(base, path);
        }
    }

    private void calculateChokePaths() {
        Position startingBasePos = baseInfo.getStartingBase().getCenter();

        for(Base base : baseInfo.getMapBases()) {
            if(baseInfo.getStartingBase().equals(base)) {
                continue;
            }

            Position nearestWalkable = baseInfo.getPathFinding().findNearestWalkable(base.getCenter());
            List<Position> path = baseInfo.getPathFinding().findPath(startingBasePos, nearestWalkable);

            if(path == null || path.isEmpty()) {
                continue;
            }

            for(ChokePoint choke : baseInfo.getChokePoints()) {
                Position chokePos = choke.getCenter().toPosition();
                for(Position pathPos : path) {
                    if(chokePos.getDistance(pathPos) < 175) {
                        if(!chokePathLists.containsKey(base)) {
                            chokePathLists.put(base, new ArrayList<>());
                        }
                        chokePathLists.get(base).add(chokePos);
                    }
                }
            }
        }
    }

    public HashMap<Base, List<Position>> getBasePathLists() {
        return basePathLists;
    }

    public HashMap<Base, List<Position>> getChokePathLists() {
        return chokePathLists;
    }
}
