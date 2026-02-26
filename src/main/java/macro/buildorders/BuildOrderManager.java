package macro.buildorders;

import bwapi.Race;
import information.enemy.EnemyInformation;
import macro.buildorders.buildtransitions.BuildTransition;
import macro.buildorders.buildtransitions.TvTMech;
import macro.buildorders.buildtransitions.TvZBio;

import java.util.HashSet;

public class BuildOrderManager {
    private EnemyInformation enemyInformation;
    private HashSet<BuildOrder> protossOpeners = new HashSet<>();
    private HashSet<BuildOrder> terranOpeners = new HashSet<>();
    private HashSet<BuildOrder> zergOpeners = new HashSet<>();
    private HashSet<BuildOrder> randomOpeners = new HashSet<>();
    private HashSet<BuildTransition> buildTransitions = new HashSet<>();
    private Race enemyRace;

    public BuildOrderManager(Race enemyRace) {
        this.enemyRace = enemyRace;

        initBuildOrders();
        initBuildTransitions();
    }

    private void initBuildOrders() {

        //TvP
        protossOpeners.add(new TwoRaxAcademy());
        //protossOpeners.add(new TwoFac());

        //TvT
        //terranOpeners.add(new TwoRaxAcademy());
        //terranOpeners.add(new OneFacFE());
        terranOpeners.add(new GoliathFE());

        //TvZ
        zergOpeners.add(new TwoRaxAcademy());

        //Random
        randomOpeners.add(new TwoRaxAcademy());

    }

    private void initBuildTransitions() {
        buildTransitions.add(new TvZBio());
        buildTransitions.add(new TvTMech());
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

    public HashSet<BuildTransition> getBuildTransitions() {
        return buildTransitions;
    }

}
