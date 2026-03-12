package map;

import bwapi.Position;
import bwem.Base;
import bwem.ChokePoint;
import information.MapInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AllBasePaths {
    private MapInfo mapInfo;
    private HashMap<Base, List<Position>> basePathLists = new HashMap<>();
    private HashMap<Base, List<Position>> chokePathLists = new HashMap<>();

    public AllBasePaths(MapInfo mapInfo) {
        this.mapInfo = mapInfo;

        calculateBasePaths();
        calculateChokePaths();
    }

    private void calculateBasePaths() {
        Position startingBasePos = mapInfo.getStartingBase().getCenter();

        for(Base base : mapInfo.getMapBases()) {
            if(mapInfo.getStartingBase().equals(base)) {
                continue;
            }

            Position nearestWalkable = mapInfo.getPathFinding().findNearestWalkable(base.getCenter());
            List<Position> path = mapInfo.getPathFinding().findPath(startingBasePos, nearestWalkable);

            if(path == null || path.isEmpty()) {
                continue;
            }

            basePathLists.put(base, path);
        }
    }

    private void calculateChokePaths() {
        Position startingBasePos = mapInfo.getStartingBase().getCenter();

        for(Base base : mapInfo.getMapBases()) {
            if(mapInfo.getStartingBase().equals(base)
            || mapInfo.getMinBaseTiles().contains(base.getLocation())) {
                continue;
            }

            Position nearestWalkable = mapInfo.getPathFinding().findNearestWalkable(base.getCenter());
            List<Position> path = mapInfo.getPathFinding().findPath(startingBasePos, nearestWalkable);

            if(path == null || path.isEmpty()) {
                continue;
            }

            for(ChokePoint choke : mapInfo.getChokePoints()) {
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
