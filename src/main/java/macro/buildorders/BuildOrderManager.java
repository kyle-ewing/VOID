package macro.buildorders;

import bwapi.Race;

import java.util.HashSet;

public class BuildOrderManager {
    private HashSet<BuildOrder> protossOpeners = new HashSet<>();
    private HashSet<BuildOrder> terranOpeners = new HashSet<>();
    private HashSet<BuildOrder> zergOpeners = new HashSet<>();
    private HashSet<BuildOrder> randomOpeners = new HashSet<>();
    private Race enemyRace;

    public BuildOrderManager(Race enemyRace) {
        this.enemyRace = enemyRace;

        initBuildOrders();
    }

    private void initBuildOrders() {

        //TvP
        protossOpeners.add(new TwoRaxAcademy());
        //protossOpeners.add(new TwoFac());

        //TvT
        //terranOpeners.add(new TwoRaxAcademy());
        terranOpeners.add(new OneFacFE());

        //TvZ
        zergOpeners.add(new TwoRaxAcademy());

        //Random
        randomOpeners.add(new TwoRaxAcademy());

    }

    public HashSet<BuildOrder> getOpenersForRace() {
        switch (enemyRace) {
            case Protoss:
                return protossOpeners;
            case Terran:
                return terranOpeners;
            case Zerg:
                return zergOpeners;
            default:
                return randomOpeners;
        }
    }

}
