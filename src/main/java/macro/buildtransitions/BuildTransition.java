package macro.buildtransitions;

import java.util.ArrayList;

import macro.buildorders.BuildType;
import planner.PlannedItem;

public abstract class BuildTransition {
    public abstract BuildTransitionName getBuildTransitionName();
    public abstract ArrayList<PlannedItem> getTransitionBuild();

    // Buildings that may already be built and should not be redundant
    public ArrayList<PlannedItem> getOptionalBuildings() {
        return new ArrayList<>();
    }

    public boolean transitionsFrom(BuildType buildType) {
        return false;
    }
}
