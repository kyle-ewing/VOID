package macro.buildorders;

import java.util.HashSet;

import bwapi.Race;
import information.enemy.EnemyInformation;
import macro.buildorders.protoss.OneFacFE;
import macro.buildorders.terran.GoliathFE;
import macro.buildorders.zerg.FactoryExpand;
import macro.buildorders.zerg.TwoRaxAcademy;
import macro.buildpivots.BuildPivot;
import macro.buildpivots.BunkerRush;
import macro.buildtransitions.BuildTransition;
import macro.buildtransitions.TvPBio;
import macro.buildtransitions.TvPMech;
import macro.buildtransitions.TvTMech;
import macro.buildtransitions.TvZBio;
import macro.buildtransitions.TvZMech;

public class BuildOrderManager {
    private EnemyInformation enemyInformation;
    private HashSet<BuildOrder> protossOpeners = new HashSet<>();
    private HashSet<BuildOrder> terranOpeners = new HashSet<>();
    private HashSet<BuildOrder> zergOpeners = new HashSet<>();
    private HashSet<BuildOrder> randomOpeners = new HashSet<>();
    private HashSet<BuildTransition> protossTransitions = new HashSet<>();
    private HashSet<BuildTransition> terranTransitions = new HashSet<>();
    private HashSet<BuildTransition> zergTransitions = new HashSet<>();
    private HashSet<BuildPivot> buildPivots = new HashSet<>();
    private Race enemyRace;

    public BuildOrderManager(Race enemyRace) {
        this.enemyRace = enemyRace;

        initBuildOrders();
        initBuildTransitions();
        initBuildPivots();
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
        // zergOpeners.add(new TwoRaxAcademy());
        zergOpeners.add(new FactoryExpand());

        //Random
        randomOpeners.add(new TwoRaxAcademy());

    }

    private void initBuildTransitions() {
        zergTransitions.add(new TvZBio());
        zergTransitions.add(new TvZMech());
        terranTransitions.add(new TvTMech());
        protossTransitions.add(new TvPMech());
        protossTransitions.add(new TvPBio());
    }

    private void initBuildPivots() {
        buildPivots.add(new BunkerRush());
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

    public HashSet<BuildPivot> getBuildPivots() {
        return buildPivots;
    }
}
