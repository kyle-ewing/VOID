package macro.buildorders;

import java.util.HashSet;

import bwapi.Race;
import information.enemy.EnemyInformation;
import macro.buildorders.buildtransitions.BuildTransition;
import macro.buildorders.buildtransitions.TvPMech;
import macro.buildorders.buildtransitions.TvTMech;
import macro.buildorders.buildtransitions.TvZBio;

public class BuildOrderManager {
    private EnemyInformation enemyInformation;
    private HashSet<BuildOrder> protossOpeners = new HashSet<>();
    private HashSet<BuildOrder> terranOpeners = new HashSet<>();
    private HashSet<BuildOrder> zergOpeners = new HashSet<>();
    private HashSet<BuildOrder> randomOpeners = new HashSet<>();
    private HashSet<BuildTransition> protossTransitions = new HashSet<>();
    private HashSet<BuildTransition> terranTransitions = new HashSet<>();
    private HashSet<BuildTransition> zergTransitions = new HashSet<>();
    private Race enemyRace;

    public BuildOrderManager(Race enemyRace) {
        this.enemyRace = enemyRace;

        initBuildOrders();
        initBuildTransitions();
    }

    private void initBuildOrders() {

        //TvP
        // protossOpeners.add(new TwoRaxAcademy());
        // protossOpeners.add(new TwoFac());
        protossOpeners.add(new OneFacFE());

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
        zergTransitions.add(new TvZBio());
        terranTransitions.add(new TvTMech());
        protossTransitions.add(new TvPMech());
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
        switch (enemyRace) {
            case Protoss:
                return protossTransitions;
            case Terran:
                return terranTransitions;
            case Zerg:
                return zergTransitions;
            default:
                return terranTransitions;
        }
    }

}
