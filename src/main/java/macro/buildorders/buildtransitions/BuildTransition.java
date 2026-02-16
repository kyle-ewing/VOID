package macro.buildorders.buildtransitions;

import macro.buildorders.BuildOrder;
import planner.PlannedItem;

import java.util.ArrayList;

public abstract class BuildTransition {
    public abstract BuildTransitionName getBuildTransitionName();
    public abstract ArrayList<PlannedItem> getTransitionBuild();

    // Buildings that may already be built and should not be redundant
    public ArrayList<PlannedItem> getOptionalBuildings() {
        return new ArrayList<>();
    }

    public boolean transitionsFrom(BuildOrder buildOrder) {
        return false;
    }
}
