package macro.buildtransitions;

import macro.buildorders.BuildOrder;
import planner.PlannedItem;

import java.util.ArrayList;

public interface BuildTransitions {
    BuildTransitionName getBuildTransitionName();
    ArrayList<PlannedItem> getTransitionBuild();
    boolean transitionsFrom(BuildOrder buildOrder);
}
